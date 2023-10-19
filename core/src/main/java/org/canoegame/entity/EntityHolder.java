package org.canoegame.entity;

public class EntityHolder<V>  {
    private volatile V value;
    private boolean expiring;

    EntityHolder(V value, boolean expiring) {
        this.value = value;
        this.expiring = expiring;
    }
    public V get() {
        return value;
    }

    void set(V value) {
        this.value = value;
    }

    void setExpiring(boolean v) {
        expiring = v;
    }

    boolean isExpiring() {
        return expiring;
    }
}
