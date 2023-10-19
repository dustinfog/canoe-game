package org.canoegame.entity;

import junit.framework.TestCase;
import org.jetbrains.annotations.NotNull;

public class CacheTest extends TestCase {

    class MockStringKey implements Key<Integer> {
        private final String value;

        public MockStringKey(String value) {
            this.value = value;
        }

        @Override
        public boolean isPrefixOf(Key<Integer> key) {
            return ((MockStringKey)key).value.startsWith(this.value);
        }

        @Override
        public int groupCode() {
            return 0;
        }

        @Override
        public int compareTo(@NotNull Key<Integer> o) {
            return this.value.compareTo(((MockStringKey)o).value);
        }

        @Override
        public int hashCode() {
            return this.value.hashCode();
        }

        public boolean equals(Object o) {
            if (!(o instanceof MockStringKey)) {
                return false;
            }
            return this.value.equals(((MockStringKey)o).value);
        }
    }

    public void testManualPrefix() {
        var cache = new Cache.ManualPrefix<Integer>();

        assertTrue(cache.add(new MockStringKey("a")));
        assertFalse(cache.add(new MockStringKey("a")));
        assertFalse(cache.add(new MockStringKey("a_b")));
        assertTrue(cache.exists(new MockStringKey("a"), true));
        assertFalse(cache.exists(new MockStringKey("b"), true));
        assertTrue(cache.exists(new MockStringKey("a_b"), true));
        assertEquals(1, cache.size());

        assertTrue(cache.add(new MockStringKey("b")));
        assertFalse(cache.add(new MockStringKey("b_a")));
        assertTrue(cache.exists(new MockStringKey("b"), true));
        assertEquals(2, cache.size());
    }

    public void testTtlPrefix() throws InterruptedException {
        var cache = new Cache.TtlPrefix<Integer>(1000);

        assertTrue(cache.add(new MockStringKey("a")));
        assertFalse(cache.add(new MockStringKey("a")));
        assertFalse(cache.add(new MockStringKey("a_b")));
        assertEquals(1, cache.size());

        assertTrue(cache.exists(new MockStringKey("a_b"), false));
        assertFalse(cache.exists(new MockStringKey("b"), false));
        Thread.sleep(1000);
        assertFalse(cache.exists(new MockStringKey("a_b"), false));
        assertEquals(0, cache.size());
    }
}