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
        var v = tasklet.localGet(this);
        if (v == null && supplier != null) {
            v = supplier.get();
            tasklet.localSet(this, v);
        }

        return v;
    }

    public void set(T value) {
        Tasklet.currentTasklet().localSet(this, value);
    }
    public void remove() {
        Tasklet.currentTasklet().localRemove(this);
    }
}
