package org.canoegame.entity;

public interface Key<E> extends Comparable<Key<E>>{
    boolean isPrefixOf(Key<E> key);

    int groupCode();
}