package org.canoegame.schedule;

import java.util.function.Supplier;

public class ActorLocal<T> {
    private final Supplier<? extends T> supplier;
    public ActorLocal(Supplier<? extends T> supplier) {
        this.supplier = supplier;
    }

    public ActorLocal() {
        this.supplier = null;
    }
    public T get() {
        var actor = Actor.currentActor();
        var v = actor.localGet(this);
        if (v == null && supplier != null) {
            v = supplier.get();
            actor.localSet(this, v);
        }

        return v;
    }

    public void set(T value) {
        Actor.currentActor().localSet(this, value);
    }
    public void remove() {
        Actor.currentActor().localRemove(this);
    }
}
