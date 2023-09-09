package org.canoegame.schedule;

import java.util.HashMap;
import java.util.Map;

public class Tasklet {
    private static final ThreadLocal<Tasklet> current = new ThreadLocal<>();

    private final Runnable runnable;
    private final Map<Object, Object> storage;
    private final String entry;

    Tasklet(Runnable runnable) {
        this(runnable.getClass().getName(), runnable);
    }

    Tasklet(String name, Runnable runnable) {
        this.entry = name;
        this.runnable = runnable;
        storage = new HashMap<>();
    }


    public static Tasklet currentTasklet() {
        return current.get();
    }

    public String getEntry() {
        return entry;
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

    @Override
    public String toString() {
        return entry + "@" + getId();
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
