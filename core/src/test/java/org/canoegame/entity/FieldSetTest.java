package org.canoegame.entity;

import junit.framework.TestCase;

import java.util.Arrays;

public class FieldSetTest extends TestCase {

    public void testSize() {
        FieldSet<TestField> fieldSet = new FieldSet<>(TestField.class);
        fieldSet.add(TestField.ID);
        assertEquals(1, fieldSet.size());
        fieldSet.add(TestField.Name);
        assertEquals(2, fieldSet.size());
    }

    public void testIsEmpty() {
        FieldSet<TestField> fieldSet = new FieldSet<>(TestField.class);
        assertTrue(fieldSet.isEmpty());
        fieldSet.add(TestField.ID);
        assertFalse(fieldSet.isEmpty());
    }

    public void testIterator() {
        FieldSet<TestField> fieldSet = new FieldSet<>(TestField.class);
        fieldSet.add(TestField.ID);
        fieldSet.add(TestField.Name);

        var iterator = fieldSet.iterator();
        assertTrue(iterator.hasNext());
        assertEquals(TestField.ID, iterator.next());
        assertTrue(iterator.hasNext());
        assertEquals(TestField.Name, iterator.next());
        assertFalse(iterator.hasNext());
    }

    public void testContains() {
        FieldSet<TestField> fieldSet = new FieldSet<>(TestField.class);
        fieldSet.add(TestField.ID);
        assertTrue(fieldSet.contains(TestField.ID));
        assertFalse(fieldSet.contains(TestField.Name));
    }

    public void testAdd() {
        FieldSet<TestField> fieldSet = new FieldSet<>(TestField.class);
        assertTrue(fieldSet.add(TestField.ID));
        assertFalse(fieldSet.add(TestField.ID));
    }

    public void testRemove() {
        FieldSet<TestField> fieldSet = new FieldSet<>(TestField.class);
        fieldSet.add(TestField.ID);
        assertTrue(fieldSet.remove(TestField.ID));
        assertFalse(fieldSet.remove(TestField.ID));
    }

    public void testContainsAll() {
        FieldSet<TestField> fs1 = new FieldSet<>(TestField.class);
        fs1.add(TestField.ID);

        FieldSet<TestField> fs2 = new FieldSet<>(TestField.class);
        fs2.add(TestField.ID);
        assertTrue(fs1.containsAll(fs2));
        fs2.add(TestField.Name);
        assertFalse(fs1.containsAll(fs2));
    }

    public void testAddAll() {
        FieldSet<TestField> fs = new FieldSet<>(TestField.class);
        fs.addAll(Arrays.asList(TestField.class.getEnumConstants()));
        assertEquals(2, fs.size());
    }

    public void testRemoveAll() {
        FieldSet<TestField> fs = new FieldSet<>(TestField.class);
        fs.add(TestField.ID);
        fs.add(TestField.Name);
        fs.removeAll(Arrays.asList(TestField.ID, TestField.Name));
        assertTrue(fs.isEmpty());
    }

    public void testRetainAll() {
        FieldSet<TestField> fs = new FieldSet<>(TestField.class);
        fs.add(TestField.ID);
        fs.add(TestField.Name);
        fs.retainAll(Arrays.asList(TestField.ID));
        assertEquals(1, fs.size());
        assertEquals(TestField.ID, fs.iterator().next());
    }

    public void testClear() {
        FieldSet<TestField> fs = new FieldSet<>(TestField.class);
        fs.add(TestField.ID);
        fs.add(TestField.Name);
        fs.clear();
        assertTrue(fs.isEmpty());
    }

    public void testTestEquals() {
        FieldSet<TestField> fs1 = new FieldSet<>(TestField.class);
        fs1.add(TestField.ID);
        fs1.add(TestField.Name);

        FieldSet<TestField> fs2 = new FieldSet<>(TestField.class);
        fs2.add(TestField.ID);
        fs2.add(TestField.Name);

        assertTrue(fs1.equals(fs2));
        assertTrue(fs2.equals(fs1));
        assertTrue(fs1.equals(fs1));
        assertTrue(fs2.equals(fs2));

        fs2.remove(TestField.Name);
        assertFalse(fs1.equals(fs2));
        assertFalse(fs2.equals(fs1));
    }

    public void testTestClone() {
        FieldSet<TestField> fs1 = new FieldSet<>(TestField.class);
        fs1.add(TestField.ID);
        fs1.add(TestField.Name);

        FieldSet<TestField> fs2 = fs1.clone();
        assertTrue(fs1.equals(fs2));
        assertTrue(fs2.equals(fs1));
        assertTrue(fs1.equals(fs1));
        assertTrue(fs2.equals(fs2));

        fs2.remove(TestField.Name);
        assertFalse(fs1.equals(fs2));
        assertFalse(fs2.equals(fs1));
    }


    private enum TestField implements Field {
        ID(1),
        Name(2),
        Sex(3);

        TestField(int number) {
            this.number = number;
        }

        private final int number;
        @Override
        public int getNumber() {
            return number;
        }
    }
}