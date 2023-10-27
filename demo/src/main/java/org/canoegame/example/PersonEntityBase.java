package org.canoegame.example

import org.canoegame.entity.Entity;
import org.canoegame.entity.Commitment;
import org.canoegame.entity.FieldSet;
import org.canoegame.entity.Key;

abstract public class PersonEntityBase<E extends PersonEntityBase<E>> extends Entity<E, PersonEntityBase.Field> {

    @Override
    protected Commitment createCommitment(FieldSet<Field> changes) {
        return null;
    }

    public enum Field implements org.canoegame.entity.Field {
            NAME("name", 1),
            ID("id", 2),
            EMAIL("email", 3),
            HELLO("hello", 4),
        ;

        private final String fieldName;
        private final int number;

        Field(String fieldName, int number) {
            this.fieldName = fieldName;
            this.number = number;
        }

        public String getFieldName() {
            return fieldName;
        }

        @Override
        public String toString() {
            return fieldName;
        }

        @Override
        public int getNumber() {
            return number;
        }
    }

    @Override
    public Key<E> getKey() {
        return null;
    }

    @Override
    protected Class<Field> getFieldClass() {
        return Field.class;
    }

    @Override
    protected void incrementRevision() {

    }

    @Override
    public int getRevision() {
        return 0;
    }
}
