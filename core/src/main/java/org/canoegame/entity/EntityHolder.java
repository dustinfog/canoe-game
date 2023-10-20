package org.canoegame.entity;

public class EntityHolder<V>  {
    private final Key<V> key;
    private volatile V value;
    private State state = State.UNCACHED;

    EntityHolder(Key<V> key, V value) {
        this.key = key;
        this.value = value;
    }

    public V get() {
        return value;
    }

    public Key<V> getKey() {
        return key;
    }

    void set(V value) {
        this.value = value;
    }

    State getState() {
        return state;
    }

    void setState(State state) {
        this.state = state;
    }

    enum State {
        UNCACHED,
        MANUAL,
        EXPIRING,
    }
}
