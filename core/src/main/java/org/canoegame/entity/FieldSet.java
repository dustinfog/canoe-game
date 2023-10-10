package org.canoegame.entity;

import org.jetbrains.annotations.NotNull;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;

public class FieldSet<F extends Enum<F>&Field> extends AbstractSet<F> implements Cloneable {
    private final Class<F> elementType;
    private final static ConcurrentHashMap<Class<? extends Enum<?>>, Enum<?>[]> universes = new ConcurrentHashMap<>();
    private final F[] universe;
    private long elements = 0L;

    public FieldSet(Class<F> elementType) {
        this.elementType = elementType;
        universe = getUniverse(elementType);
    }

    private static <F extends Enum<F>&Field> F[] getUniverse(Class<F> elementType) {
        var ret = universes.computeIfAbsent(elementType, k -> {
            var all = elementType.getEnumConstants();
            var max = 0;
            for (F f : all) {
                if (f.getNumber() > max) {
                    max = f.getNumber();
                }
            }

            var value = new Enum[max + 1];
            for (F f : all) {
                value[f.getNumber()] = f;
            }
            return value;
        });

        return (F[]) ret;
    }


    public int size() {
        return Long.bitCount(elements);
    }

    public boolean isEmpty() {
        return elements == 0;
    }

    public @NotNull Iterator<F> iterator() {
        return new FieldSetIterator();
    }

    private class FieldSetIterator implements Iterator<F> {
        long unseen;

        long lastReturned = 0;

        FieldSetIterator() {
            unseen = elements;
        }

        public boolean hasNext() {
            return unseen != 0;
        }

        public F next() {
            if (unseen == 0)
                throw new NoSuchElementException();
            lastReturned = unseen & -unseen;
            unseen -= lastReturned;
            return universe[Long.numberOfTrailingZeros(lastReturned)];
        }

        public void remove() {
            if (lastReturned == 0)
                throw new IllegalStateException();
            elements &= ~lastReturned;
            lastReturned = 0;
        }
    }

    public boolean contains(Object e) {
        if (e == null)
            return false;
        Class<?> eClass = e.getClass();
        if (eClass != elementType && eClass.getSuperclass() != elementType)
            return false;

        return (elements & (1L << ((F) e).getNumber())) != 0;
    }

    public boolean add(F e) {
        typeCheck(e);

        long oldElements = elements;
        elements |= (1L << e.getNumber());
        return elements != oldElements;
    }

    public boolean remove(Object e) {
        if (e == null)
            return false;
        Class<?> eClass = e.getClass();
        if (eClass != elementType && eClass.getSuperclass() != elementType)
            return false;

        long oldElements = elements;
        elements &= ~(1L << ((Field) e).getNumber());
        return elements != oldElements;
    }

    public boolean containsAll(Collection<?> c) {
        if (!(c instanceof FieldSet<?> es))
            return super.containsAll(c);

        if (es.elementType != elementType)
            return es.isEmpty();

        return (es.elements & ~elements) == 0;
    }

    public FieldSet<F> addAll() {
        elements = ~0L;
        return this;
    }

    public boolean addAll(Collection<? extends F> c) {
        if (!(c instanceof FieldSet<?> es))
            return super.addAll(c);

        if (es.elementType != elementType) {
            if (es.isEmpty())
                return false;
            else
                throw new ClassCastException(
                        es.elementType + " != " + elementType);
        }

        long oldElements = elements;
        elements |= es.elements;
        return elements != oldElements;
    }

    public boolean removeAll(Collection<?> c) {
        if (!(c instanceof FieldSet<?> es))
            return super.removeAll(c);

        if (es.elementType != elementType)
            return false;

        long oldElements = elements;
        elements &= ~es.elements;
        return elements != oldElements;
    }

    public boolean retainAll(Collection<?> c) {
        if (!(c instanceof FieldSet<?> es))
            return super.retainAll(c);

        if (es.elementType != elementType) {
            boolean changed = (elements != 0);
            elements = 0;
            return changed;
        }

        long oldElements = elements;
        elements &= es.elements;
        return elements != oldElements;
    }

    public void clear() {
        elements = 0;
    }

    public FieldSet<F> clone() {
        try {
            return (FieldSet<F>) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e);
        }
    }

    public boolean equals(Object o) {
        if (!(o instanceof FieldSet<?> es))
            return super.equals(o);

        if (es.elementType != elementType)
            return elements == 0 && es.elements == 0;
        return es.elements == elements;

    }

    final void typeCheck(F e) {
        Class<?> eClass = e.getClass();
        if (eClass != elementType && eClass.getSuperclass() != elementType)
            throw new ClassCastException(eClass + " != " + elementType);
    }
}
