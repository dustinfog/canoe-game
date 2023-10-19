package org.canoegame.entity;

import java.util.EnumSet;

abstract public class Entity<E extends Entity<E, F>, F extends Enum<F>&Field> implements Cloneable{
    private volatile EntityHolder<E> holder;

    private EntityManager entityManager;

    private Lifecycle lifecycle;
    private final EnumSet<EntityFlag> flags;

    private final FieldSet<F> changes;

    public Entity() {
        lifecycle = Lifecycle.Unavailable;
        flags = EnumSet.noneOf(EntityFlag.class);
        changes = createFieldSet();
    }

    public void setEntityManager(EntityManager entityManager) {
        if (this.entityManager != null) {
            throw new IllegalStateException("EntityManager already set");
        }

        this.entityManager = entityManager;
    }

    protected void validateRead() {
        if (!entityManager.isAccessValidation()) {
            return;
        }

        var v = entityManager.getLocalCopy(getKey());
        if (v != this) {
            throw new IllegalStateException("Entity not a local copy");
        }
    }

    protected void validateWrite() {
        if (!hasFlag(EntityFlag.WRITABLE)) {
            throw new IllegalStateException("Entity not writable");
        }

        if (!entityManager.isAccessValidation()) {
            return;
        }

        var v = entityManager.getLocalCopy(getKey());
        if (v != this) {
            throw new IllegalStateException("Entity not a local copy");
        }
    }

    public E read() {
        var key = getKey();
        var entity = entityManager.getLocalCopy(key);
        if (entity != null) {
            return entity;
        }

        for (;;) {
            var v = holder.get();
            if (v != null) {
                entityManager.putLocalCopy(v);
                return v;
            }

            holder = entityManager.refer(getKey());
        }
    }

    public E copyWrite() {
        var key = getKey();
        var entity = entityManager.getLocalCopy(key);
        if (entity != null && entity.hasFlag(EntityFlag.WRITABLE)) {
            return entity;
        }

        for (;;) {
            var v = holder.get();
            if (v != null) {
                var n = v.clone();
                n.setFlag(EntityFlag.WRITABLE);
                entityManager.putLocalCopy(n);
                return n;
            }

            holder = entityManager.refer(getKey());
        }
    }

    protected void setFlag(EntityFlag flag) {
        flags.add(flag);
    }

    protected void clearFlag(EntityFlag flag) {
        flags.remove(flag);
    }

    public boolean hasFlag(EntityFlag flag) {
        return flags.contains(flag);
    }

    public void Change(F field) {
        validateWrite();
        switch (lifecycle) {
            case NEW:
                return;
            case DELETED:
                throw new IllegalStateException("Entity deleted");
            case Unavailable:
                throw new IllegalStateException("Entity unavailable");
        }

        changes.add(field);
        setFlag(EntityFlag.DIRTY);
    }

    public Lifecycle getLifecycle() {
        return lifecycle;
    }

    protected boolean delete() {
        validateWrite();
        switch (lifecycle) {
            case NEW:
                lifecycle = Lifecycle.Unavailable;
                setFlag(EntityFlag.DIRTY);
                return true;
            case NORMAL:
                lifecycle = Lifecycle.DELETED;
                setFlag(EntityFlag.DIRTY);
                return true;
            default:
                return false;
        }
    }

    public Commitment commit() {
        validateWrite();
        setFlag(EntityFlag.COMMITTING);

        var commitment =  switch (lifecycle) {
            case NEW -> createCommitment(createFieldSet().addAll());
            case NORMAL -> {
                incrementRevision();
                yield createCommitment(changes.clone());
            }
            case DELETED -> createCommitment(createFieldSet());
            default -> null;
        };

        lifecycle = switch (lifecycle) {
            case NEW -> Lifecycle.NORMAL;
            case DELETED -> Lifecycle.Unavailable;
            default -> lifecycle;
        };

        changes.clear();
        clearFlag(EntityFlag.DIRTY);
        clearFlag(EntityFlag.COMMITTING);
        return commitment;
    }

    private FieldSet<F> createFieldSet() {
        return new FieldSet<>(getFieldClass());
    }

    abstract protected Commitment createCommitment(FieldSet<F> changes);

    abstract public Key<E> getKey();
    abstract protected Class<F> getFieldClass();

    abstract protected void incrementRevision();

    @Override
    public E clone() {
        try {
            return (E) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
