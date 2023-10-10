package org.canoegame.schedule;

import java.util.function.Supplier;

public class TaskletLocal<T> {
    private final Supplier<? extends T> supplier;
    public TaskletLocal(Supplier<? extends T> supplier) {
        this.supplier = supplier;
    }

    public TaskletLocal() {
        this.supplier = null;
    }
    public T get() {
        var tasklet = Tasklet.currentTasklet();
        var v = tasklet.get(this);
        if (v == null && supplier != null) {
            v = supplier.get();
            tasklet.set(this, v);
        }

        return v;
    }

    public void set(T value) {
        Tasklet.currentTasklet().set(this, value);
    }
    public void remove() {
        Tasklet.currentTasklet().remove(this);
    }
}
