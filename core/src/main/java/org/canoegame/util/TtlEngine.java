package org.canoegame.util;

import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class TtlEngine<V> {
    private final long ttl;
    private Node<V> head;
    private final Consumer<V> expireHandler;

    public TtlEngine(long ttl, Consumer<V> expireHandler) {
        this.ttl = ttl;
        this.expireHandler = expireHandler;
    }

    public Node<V> add(V value) {
        var node = new Node<>(this, value);
        this.touch(node);
        return node;
    }

    public void remove(@NotNull Node<V> node) {
        if (node.list != this) {
            return;
        }

        node.list = null;

        if (node == head) {
            if (node.next == node) {
                head = null;
                return;
            }

            head = node.next;
        }

        node.next.prev = node.prev;
        node.prev.next = node.next;
    }

    public void touch(@NotNull Node<V> node) {
        if (node.list != this) {
            return;
        }

        node.accessTime = System.currentTimeMillis();;
        this.moveNodeToHead(node);
    }

    private void moveNodeToHead(Node<V> node) {
        if (node == head || node.list != this) {
            return;
        }

        // 空List
        if (head == null) {
            head = node;
            head.next = head;
            head.prev = head;
            return;
        }

        // 已在列表中
        if (node.next != null) {
            node.next.prev = node.prev;
            node.prev.next = node.next;
        }

        node.next = head;
        node.prev = head.prev;
        head.prev.next = node;
        head.prev = node;
        head = node;
    }

    public void expire() {
        if (head == null) {
            return;
        }

        var tail = head.prev;
        var now = System.currentTimeMillis();
        while (tail != null && now - tail.accessTime > ttl) {
            tail.list = null;
            if (expireHandler != null) {
                expireHandler.accept(tail.value);
            }

            if (tail == head) {
                head = null;
                tail = null;
            } else {
                tail = tail.prev;
            }
        }

        if (tail != null && tail.list != null) {
            tail.next = head;
            head.prev = tail;
        }
    }

    public static class Node<V> {
        private final V value;
        private long accessTime;
        Node<V> next;
        Node<V> prev;

        TtlEngine<V> list;

        public V getValue() {
            return value;
        }

        public boolean isExpired() {
            return list == null;
        }

        private Node(TtlEngine<V> list, V value) {
            this.list = list;
            this.value = value;
        }
    }
}
