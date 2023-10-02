package org.canoegame.schedule;

import java.util.HashMap;
import java.util.Map;

public class Tasklet {
    private static final ThreadLocal<Tasklet> current = new ThreadLocal<>();

    private final Runnable runnable;
    private final Map<Object, Object> storage;
    private final String name;
    private final Actor actor;

    Tasklet(String name, Runnable runnable, Actor actor) {
        if (name == null) {
            name = runnable.getClass().getName();
        }
        this.name = name;
        this.runnable = runnable;
        this.actor = actor;
        storage = new HashMap<>();
    }

    public static Tasklet currentTasklet() {
        return current.get();
    }

    public String getName() {
        return name;
    }

    public void set(Object key, Object value) {
        if (currentTasklet() != this)
            throw new IllegalStateException("Not current tasklet");

        storage.put(key, value);
    }

    public Object get(Object key) {
        if (currentTasklet() != this)
            throw new IllegalStateException("Not current tasklet");

        return storage.get(key);
    }

    public void remove(Object key) {
        if (currentTasklet() != this)
            throw new IllegalStateException("Not current tasklet");

        storage.remove(key);
    }

    public Actor getActor() {
        return actor;
    }

    @Override
    public String toString() {
        return name + "@" + getId();
    }

    public int getId() {
        return hashCode();
    }

    void run() {
        current.set(this);

        try {
            runnable.run();
        } finally {
            current.remove();
        }
    }
}
