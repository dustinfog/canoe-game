package org.canoegame.entity;

import junit.framework.TestCase;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

public class CacheTest extends TestCase {


    static class MockEntity extends Entity<MockEntity, MockEntity.Field> {
        private final int key1;
        private final int key2;

        public int field1;

        public int revision;

        private final org.canoegame.entity.Key<MockEntity> k;

        public MockEntity(int key1, int key2) {
            this.key1 = key1;
            this.key2 = key2;
            k = key(key1, key2);
        }

        public int getKey1() {
            return key1;
        }

        public int getKey2() {
            return key2;
        }

        public void setField1(int v) {
            validateWrite();
            Change(Field.FIELD_1);
            this.field1 = v;
        }

        public int getField1() {
            validateRead();
            return field1;
        }

        public int hashCode() {
            return k.hashCode();
        }

        public boolean equals(Object o) {
            if (!(o instanceof MockEntity e)) {
                return false;
            }

            return k.equals(e.getKey());
        }

        public static org.canoegame.entity.Key<MockEntity> key(int key1, int key2) {
            return new Key(key1, key2, 2);
        }
        public static org.canoegame.entity.Key<MockEntity> prefix(int key1) {
            return new Key(key1, 0, 1);
        }
        public static org.canoegame.entity.Key<MockEntity> prefix() {
            return new Key(0, 0, 0);
        }

        private record Key(int key1, int key2, int significant) implements org.canoegame.entity.Key<MockEntity> {

            @Override
                    public boolean isPrefixOf(org.canoegame.entity.Key<MockEntity> key) {
                        if (!(key instanceof Key k)) {
                            return false;
                        }

                        if (significant == 0) {
                            return true;
                        }

                        if (significant > k.significant) {
                            return false;
                        }

                        for (var i = 0; i < significant; i++) {
                            if (compareField(k, i) != 0) {
                                return false;
                            }
                        }

                        return true;
                    }

                    @Override
                    public int groupCode() {
                        return key1;
                    }

                    public int hashCode() {
                        return key1 * 31 + key2;
                    }

                    public boolean equals(Object o) {
                        if (!(o instanceof Key k)) {
                            return false;
                        }

                        return compareTo(k) == 0;
                    }

                    @Override
                    public int compareTo(@NotNull org.canoegame.entity.Key<MockEntity> o) {
                        if (!(o instanceof Key k)) {
                            return 0;
                        }

                        for (int i = 0, minSig = Math.min(significant, k.significant); i < minSig; i++) {
                            var r = compareField(k, i);
                            if (r != 0) {
                                return r;
                            }
                        }

                        return Integer.compare(significant, k.significant);
                    }

                    private int compareField(@NotNull Key k, int index) {
                        return switch (index) {
                            case 0 -> Integer.compare(key1, k.key1);
                            default -> Integer.compare(key2, k.key2);
                        };
                    }
                }

        public enum Field implements org.canoegame.entity.Field {
            KEY_1(1),
            KEY_2(2),
            FIELD_1(3);

            private final int number;
            Field(int number) {
                this.number = number;
            }

            @Override
            public int getNumber() {
                return number;
            }
        }

        protected Commitment createCommitment(FieldSet<Field> changes) {
            return null;
        }

        @Override
        public org.canoegame.entity.Key<MockEntity> getKey() {
            return k;
        }

        @Override
        protected Class getFieldClass() {
            return Field.class;
        }

        @Override
        protected void incrementRevision() {
            revision ++;
        }

        @Override
        public int getRevision() {
            return revision;
        }
    }

    public void testPutNullIfAbsent() {
        var cache = new Cache<>(MockEntity.class, TimeUnit.SECONDS.toMillis(1), true);
        var holder = cache.putNullIfAbsent(MockEntity.key(1, 2));
        var holder1 = cache.putNullIfAbsent(MockEntity.key(1, 2));
        var holder3 = cache.get(MockEntity.key(1, 2));

        assertSame(holder, holder1);
        assertSame(holder, holder3);
    }

    public void testGetAll() throws InterruptedException {
        var cache = new Cache<>(MockEntity.class, TimeUnit.SECONDS.toMillis(1), true);

        var e1 = new MockEntity(1, 2);
        cache.putOnStore(e1, false);

        var e2 = new MockEntity(1, 3);
        cache.putOnStore(e2, false);

        var e3 = new MockEntity(1, 4);
        cache.putOnStore(e3, false);


        var all = cache.getAll(MockEntity.prefix(1));
        assertNull(all);

        cache.putPrefix(MockEntity.prefix(1));
        all = cache.getAll(MockEntity.prefix(1));
        assertSame(3, all.size());
        assertSame(e1, all.get(0).get());
        assertSame(e2, all.get(1).get());
        assertSame(e3, all.get(2).get());

        var key = MockEntity.key(1, 8);
        assertNotNull(cache.get(key));
        assertNull(cache.get(key).get());

        var e4 = new MockEntity(2, 1);
        cache.putOnStore(e4, false);
        cache.putPrefix(MockEntity.prefix(2));
        var all2 = cache.getAll(MockEntity.prefix(2));
        assertSame(1, all2.size());


        cache = new Cache<>(MockEntity.class, TimeUnit.SECONDS.toMillis(1), false);
        cache.putOnStore(e1, true);
        cache.putOnStore(e2, true);
        cache.putOnStore(e3, true);
        cache.putPrefix(MockEntity.prefix(1));
        var all3 = cache.getAll(MockEntity.prefix(1));
        assertSame(3, all3.size());
        Thread.sleep(1000);
        var all4 = cache.getAll(MockEntity.prefix(1));
        assertNull(all4);
    }

    public void testPutOnFetch() {
        var cache = new Cache<>(MockEntity.class, TimeUnit.SECONDS.toMillis(1), true);
        var e1 = new MockEntity(1, 2);
        cache.putOnStore(e1, false);
        var holder = cache.get(e1.getKey());
        assertEquals(e1, holder.get());

        var e2 = new MockEntity(1, 2);
        Exception expectedException = null;
        try {
            cache.putOnFetch(e2, true);
        } catch (Exception e) {
            expectedException = e;
        }

        if (expectedException == null) {
            fail("Expected exception not thrown");
        }
        var holder2 = cache.get(e2.getKey());
        assertSame(e1, holder2.get());
    }

    public void testPutOnStore() {
        var cache = new Cache<>(MockEntity.class, TimeUnit.SECONDS.toMillis(1), true);
        var e1 = new MockEntity(1, 2);
        cache.putOnStore(e1, false);
        var holder = cache.get(e1.getKey());
        assertEquals(e1, holder.get());

        var e2 = new MockEntity(1, 2);
        Exception expectedException = null;
        try {
            cache.putOnStore(e2, true);
        } catch (Exception e) {
            expectedException = e;
        }

        if (expectedException == null) {
            fail("Expected exception not thrown");
        }
        var holder2 = cache.get(e2.getKey());
        assertEquals(e2, holder2.get());
    }

    public void testPutOnDelete() {
        var cache = new Cache<>(MockEntity.class, TimeUnit.SECONDS.toMillis(1), true);

        var e1 = new MockEntity(1, 2);
        cache.putOnStore(e1, false);

        assertEquals(e1, cache.get(e1.getKey()).get());

        cache.putOnDelete(e1);
        assertNotNull(cache.get(e1.getKey()));
        assertNull(cache.get(e1.getKey()).get());
    }

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
        var prefix = MockEntity.prefix();

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