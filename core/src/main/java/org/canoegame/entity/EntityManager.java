package org.canoegame.entity;

import org.canoegame.schedule.TaskletLocal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EntityManager {
    private final TaskletLocal<Map<Key<?>, Entity<?, ?>>> localCopies = new TaskletLocal<>(HashMap::new);
    private boolean accessValidation = true;

    public boolean isAccessValidation() {
        return accessValidation;
    }

    public void setAccessValidation(boolean accessValidation) {
        this.accessValidation = accessValidation;
    }

    <E extends Entity<E, F>, F extends Enum<F>&Field> E getLocalCopy(Key<E> key) {
        var v = localCopies.get().get(key);
        if (v == null) {
            return null;
        }
        return (E)v;
    }
    <E extends Entity<E, F>, F extends Enum<F>&Field> void putLocalCopy(E entity) {
       localCopies.get().put(entity.getKey(), entity);
    }

    public <E extends Entity<E, F>, F extends Enum<F>&Field> E create(Class<E> clazz) {
        return null;
    }
    public <T extends Entity<T, F>, F extends Enum<F>&Field> T get(Key<T> key) {
        return null;
    }
    public <T extends Entity<T, F>, F extends Enum<F>&Field> List<T> getAll(Key<T> key) {
        return null;
    }
    public <T extends Entity<T, F>, F extends Enum<F>&Field> EntityHolder<T> refer(Key<T> key) {
        return null;
    }

}
