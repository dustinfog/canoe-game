package org.canoegame.entity;

import org.canoegame.util.TtlEngine;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Cache<E extends Entity<E, ?>> {
    private static final int CONCURRENCY_LEVEL = 10;
    private final Prefix<E> prefix;
    private final Group<E>[] groups;
    Cache(boolean useManualCache) {
        if (useManualCache) {
            this.prefix = new ManualPrefix<>();
        } else {
            this.prefix = new TtlPrefix<>(1000);
        }

        groups = new Group[CONCURRENCY_LEVEL];
    }

    public EntityHolder<E> get(Key<E> key) {
        var group = getGroup(key);
        return group.get(key);
    }

    public EntityHolder<E> getAll(Key<E> prefixKey) {
        if (prefix.exists(prefixKey, true)) {
            return null;
        }

        var group = getGroup(prefixKey);
        if (group != null) {
        }
        return null;
    }

    public EntityHolder<E> putOnFetch(Key<E> key, E value, boolean expiring) {
        var group = getGroup(key);
        return group.putOnFetch(key, value, expiring);
    }

    public EntityHolder<E> putOnStore(Key<E> key, E value, boolean expiring) {
        var group = getGroup(key);
        return group.putOnStore(key, value, expiring);
    }

    public void putOnDelete(Key<E> key) {
        var group = getGroup(key);
        group.putOnDelete(key);
    }

    private Group<E> getGroup(Key<E> key) {
        var groupCode = key.groupCode();
        if (groupCode < 0) {
            return null;
        }
        return groups[groupCode%CONCURRENCY_LEVEL];
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

        public EntityHolder<E> get(Key<E> key) {
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

                return ttl.get(key, false);
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

                switchExpiring(key, orig, expiring);
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
                switchExpiring(key, orig, expiring);
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
                    return;
                }

                orig.set(null);
                switchExpiring(key, orig, true);
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

        private void switchExpiring(Key<E> key, EntityHolder<E> holder, boolean expiring) {
            if (expiring) {
                if (holder.isExpiring()) {
                    return;
                }

                manual.remove(key);
                holder.setExpiring(true);
                ttl.put(key, holder);
            } else {
                if (!holder.isExpiring()) {
                    return;
                }

                ttl.remove(key);
                holder.setExpiring(false);
                manual.put(key, holder);
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
                holder = new EntityHolder<>(value, false);
                store.put(key, holder);
            } else {
                holder.set(value);
            }

            return holder;
        }

        public void put(Key<E> key, EntityHolder<E> holder) {
            store.put(key, holder);
        }

        public EntityHolder<E> remove(Key<E> key) {
            return store.remove(key);
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
            engine = new TtlEngine<>(ttl, (v) -> store.remove(v.get().getKey()));
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
                holder = new EntityHolder<>(value, true);
                store.put(key, engine.add(holder));
            } else {
                holder = node.getValue();
                holder.set(value);
            }

            return holder;
        }


        public void put(Key<E> key, EntityHolder<E> holder) {
            store.put(key, engine.add(holder));
        }


        public EntityHolder<E> remove(Key<E> key) {
            var node = store.remove(key);
            if (node == null) {
                return null;
            }

            engine.remove(node);
            return node.getValue();
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
