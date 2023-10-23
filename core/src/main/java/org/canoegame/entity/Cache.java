package org.canoegame.entity;

import org.canoegame.util.TtlEngine;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Cache<E extends Entity<E, ?>> {
    private static final int CONCURRENCY_LEVEL = 10;
//    public static final long DEFAULT_TTL = TimeUnit.MINUTES.toMillis(30);

    private final Prefix<E> prefix;
    private final Group<E>[] groups;

    private final Class<E> elementType;

    private final boolean canonical;

    Cache(Class<E> elementType, long ttl, boolean canonical) {
        this.elementType = elementType;
        this.canonical = canonical;

        if (canonical) {
            this.prefix = new ManualPrefix<>();
        } else {
            var prefixTtl = (int)((float)(ttl) * 0.8);
            this.prefix = new TtlPrefix<>(prefixTtl);
        }

        groups = new Group[CONCURRENCY_LEVEL];
        for (var i = 0; i < CONCURRENCY_LEVEL; i ++) {
            groups[i] = new Group<>(ttl);
        }
    }

    public EntityHolder<E> get(Key<E> key) {
        var group = getGroup(key);
        if (group == null) {
            return null;
        }

        return group.get(key, prefix.exists(key, false));
    }

    public List<EntityHolder<E>> getAll(Key<E> prefixKey) {
        if (!prefix.exists(prefixKey, true)) {
            return null;
        }

        var group = getGroup(prefixKey);
        if (group != null) {
            group.getAll(prefixKey, canonical);
        }

        List<EntityHolder<E>> all = new ArrayList<>();
        for (var g : groups) {
            all.addAll(g.getAll(prefixKey, canonical));
        }

        return all;
    }

    public void putPrefix(Key<E> key) {
        prefix.add(key);
    }

    public EntityHolder<E> putNullIfAbsent(@NotNull Key<E> key) {
        var group = getGroup(key);
        if (group == null) {
            return null;
        }

        return group.get(key, true);
    }

    public EntityHolder<E> putOnFetch(@NotNull E value, boolean expiring) {
        if (canonical && expiring) {
            throw new IllegalArgumentException("Canonical cache cannot be expiring");
        }

        this.typeCheck(value);
        var key = value.getKey();
        var group = getGroup(key);
        if (group == null) {
            return null;
        }
        return group.putOnFetch(key, value, expiring);
    }

    public EntityHolder<E> putOnStore(@NotNull E value, boolean expiring) {
        if (canonical && expiring) {
            throw new IllegalArgumentException("Canonical cache cannot be expiring");
        }

        this.typeCheck(value);
        var key = value.getKey();
        var group = getGroup(key);
        if (group == null) {
            return null;
        }
        return group.putOnStore(key, value, expiring);
    }

    public void putOnDelete(E value) {
        var key = value.getKey();
        var group = getGroup(key);
        if (group == null) {
            return;
        }
        group.putOnDelete(key);
    }

    private Group<E> getGroup(Key<E> key) {
        var groupCode = key.groupCode();
        if (groupCode < 0) {
            return null;
        }
        return groups[groupCode%CONCURRENCY_LEVEL];
    }

    final void typeCheck(E e) {
        Class<?> eClass = e.getClass();
        if (eClass != elementType && eClass.getSuperclass() != elementType)
            throw new ClassCastException(eClass + " != " + elementType);
    }

    static class Group<E extends Entity<E, ?>> {
        private final ReadWriteLock lock;
        private final Manual<E> manual;
        private final Ttl<E> ttl;


        Group(long ttl) {
            manual = new Manual<>();
            this.ttl = new Ttl<>(ttl);
            lock = new ReentrantReadWriteLock();
        }

        public EntityHolder<E> get(Key<E> key, boolean putNullIfAbsent) {
            lock.readLock().lock();
            try {
                var ret = manual.get(key);
                if (ret != null) {
                    return ret;
                }
            } finally {
                lock.readLock().unlock();
            }

            lock.writeLock().lock();
            try {
                var ret = manual.get(key);
                if (ret != null) {
                    return ret;
                }

                ret = ttl.get(key, false);
                if (ret != null) {
                    return ret;
                }

                if (putNullIfAbsent) {
                    ret = new EntityHolder<>(key, null);
                    ttl.put(ret);
                    return ret;
                }

                return null;
            } finally {
                lock.writeLock().unlock();
            }
        }

        public List<EntityHolder<E>> getAll(Key<E> prefixKey, boolean canonical) {
            if (canonical) {
                lock.readLock().lock();
                try {
                    return manual.getAllWithPrefix(prefixKey);
                } finally {
                    lock.readLock().unlock();
                }
            }

            lock.readLock().lock();
            try {
                var all = manual.getAllWithPrefix(prefixKey);
                all.addAll(ttl.getAllWithPrefix(prefixKey));
                return all;
            } finally {
                lock.readLock().unlock();
            }
        }

        public EntityHolder<E> putOnFetch(Key<E> key, E value, boolean expiring) {
            lock.writeLock().lock();
            try {
                var orig = peek(key);
                if (orig == null) {
                    return put(key, value, expiring);
                }

                return orig;
            } finally {
                lock.writeLock().unlock();
            }
        }

        public EntityHolder<E> putOnStore(Key<E> key, E value, boolean expiring) {
            lock.writeLock().lock();
            try {
                var orig = peek(key);
                if (orig == null) {
                    return put(key, value, expiring);
                }

                orig.set(value);
                switchStore(orig, expiring);
                return orig;
            } finally {
                lock.writeLock().unlock();
            }
        }

        public void putOnDelete(Key<E> key) {
            lock.writeLock().lock();
            try {
                var orig = peek(key);
                if (orig == null) {
                   ttl.put(new EntityHolder<>(key, null));
                   return;
                }

                orig.set(null);
                switchStore(orig, true);
            } finally {
                lock.writeLock().unlock();
            }
        }

        private EntityHolder<E> peek(Key<E> key) {
            var ret = manual.get(key);
            if (ret != null) {
                return ret;
            }

            return ttl.get(key, true);
        }

        private void switchStore(EntityHolder<E> holder, boolean expiring) {
            var state = holder.getState();
            if (state == EntityHolder.State.UNCACHED) {
                throw new IllegalStateException("Entity not cached");
            }

            if (expiring) {
                if (holder.getState() == EntityHolder.State.EXPIRING) {
                    return;
                }

                manual.remove(holder.getKey());
                ttl.put(holder);
            } else {
                if (holder.getState() == EntityHolder.State.MANUAL) {
                    return;
                }

                ttl.remove(holder.getKey());
                manual.put(holder);
            }
        }

        private EntityHolder<E> put(Key<E> key, E value, boolean expiring) {
            if (expiring) {
                return ttl.put(key, value);
            } else {
                return manual.put(key, value);
            }
        }
    }

    static class Manual<E extends Entity<E, ?>> {
        private final TreeMap<Key<E>, EntityHolder<E>> store = new TreeMap<>();
        public EntityHolder<E> get(Key<E> key) {
            return store.get(key);
        }

        public EntityHolder<E> put(Key<E> key, E value) {
            var holder = store.get(key);
            if (holder == null) {
                holder = new EntityHolder<>(key, value);
                put(holder);
            } else {
                holder.set(value);
            }

            return holder;
        }

        public void put(EntityHolder<E> holder) {
            var key = holder.getKey();
            holder.setState(EntityHolder.State.MANUAL);
            store.put(key, holder);
        }

        public void remove(Key<E> key) {
            var holder = store.remove(key);
            holder.setState(EntityHolder.State.UNCACHED);
        }

        public int size() {
            return store.size();
        }

        public List<EntityHolder<E>> getAllWithPrefix(Key<E> prefix) {
            List<EntityHolder<E>> all = new ArrayList<>();
            var tailMap = store.tailMap(prefix, true);
            for (var e : tailMap.entrySet()) {
                if (!prefix.isPrefixOf(e.getKey())) {
                    break;
                }

                all.add(e.getValue());
            }

            return all;
        }
    }

    static class Ttl<E extends Entity<E, ?>> {
        private final TreeMap<Key<E>, TtlEngine.Node<EntityHolder<E>>> store;
        private final TtlEngine<EntityHolder<E>> engine;

        Ttl(long ttl) {
            store = new TreeMap<>();
            engine = new TtlEngine<>(ttl, (v) -> {
                v.set(null);
                store.remove(v.getKey());
            });
        }

        public EntityHolder<E> get(Key<E> key, boolean peek) {
            if (!peek) {
                engine.expire();
            }

            var node = store.get(key);
            if (node == null) {
                return null;
            }

            if (!peek) {
                engine.touch(node);
            }
            return node.getValue();
        }

        public EntityHolder<E> put(Key<E> key, E value) {
            var node = store.get(key);
            EntityHolder<E>  holder;
            if (node == null) {
                holder = new EntityHolder<>(key, value);
                put(holder);
            } else {
                holder = node.getValue();
                holder.set(value);
            }

            return holder;
        }

        public void put(EntityHolder<E> holder) {
            var key = holder.getKey();
            holder.setState(EntityHolder.State.EXPIRING);
            store.put(key, engine.add(holder));
        }

        public void remove(Key<E> key) {
            var node = store.remove(key);
            if (node == null) {
                return;
            }

            engine.remove(node);
            node.getValue().setState(EntityHolder.State.UNCACHED);
        }

        public int size() {
            return store.size();
        }

        public List<EntityHolder<E>> getAllWithPrefix(Key<E> prefix) {
            engine.expire();
            List<EntityHolder<E>> all = new ArrayList<>();
            var tailMap = store.tailMap(prefix, true);
            for (var e : tailMap.entrySet()) {
                if (!prefix.isPrefixOf(e.getKey())) {
                    break;
                }

                var node = e.getValue();
                engine.touch(node);
                all.add(node.getValue());
            }

            return all;
        }

    }

    interface Prefix<E> {
        boolean add(Key<E> key);
        boolean exists(Key<E> key, boolean touch);
        int size();
    }
    static class ManualPrefix<E> implements Prefix<E> {
        private final ReadWriteLock lock;
        private final TreeSet<Key<E>> store;

        ManualPrefix() {
            this.lock = new ReentrantReadWriteLock();
            this.store = new TreeSet<>();
        }
        public boolean add(Key<E> key) {
            lock.writeLock().lock();
            try {
                if (exists(key, false)) {
                    return false;
                }

                var tailItr = store.tailSet(key, true).iterator();
                while (tailItr.hasNext()) {
                    var next = tailItr.next();
                    if (!key.isPrefixOf(next)) {
                        break;
                    }

                    tailItr.remove();
                }

                store.add(key);
                return true;
            } finally {
                lock.writeLock().unlock();
            }
        }

        public boolean exists(Key<E> key, boolean touch) {
            lock.readLock().lock();
            try {
                var headSet = store.headSet(key, true);
                if (headSet.isEmpty()) {
                    return false;
                }

                return headSet.last().isPrefixOf(key);
            } finally {
                lock.readLock().unlock();
            }
        }

        public int size() {
            lock.readLock().lock();
            try {
                return store.size();
            } finally {
                lock.readLock().unlock();
            }
        }
    }

    static class TtlPrefix<E> implements Prefix<E> {
        private final ReadWriteLock lock;
        private final TreeMap<Key<E>, TtlEngine.Node<Key<E>>> store;
        private final TtlEngine<Key<E>> engine;

        TtlPrefix(long ttl) {
            lock = new ReentrantReadWriteLock();
             store = new TreeMap<>();
             engine = new TtlEngine<>(ttl, store::remove);
        }
        public int size() {
            lock.readLock().lock();
            try {
                return store.size();
            } finally {
                lock.readLock().unlock();
            }
        }
        public boolean add(Key<E> key) {
            lock.writeLock().lock();
            try {
                if (exists(key, false)) {
                    return false;
                }

                var tailItr = store.tailMap(key, true).entrySet().iterator();
                while (tailItr.hasNext()) {
                    var next = tailItr.next();
                    if (!key.isPrefixOf(next.getKey())) {
                        break;
                    }

                    engine.remove(next.getValue());
                    tailItr.remove();
                }

                store.put(key, engine.add(key));
                return true;
            } finally {
                lock.writeLock().unlock();
            }

        }

        public boolean exists(Key<E> key, boolean touch) {
            lock.writeLock().lock();
            try {
                engine.expire();

                var headMap = store.headMap(key, true);
                if (headMap.isEmpty()) {
                    return false;
                }

                var theEntry = headMap.lastEntry();
                if (!theEntry.getKey().isPrefixOf(key)) {
                    return false;
                }

                if (touch) {
                    engine.touch(theEntry.getValue());
                }
                return true;
            } finally {
                lock.writeLock().unlock();
            }
        }
    }
}
