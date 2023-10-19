package org.canoegame.example;

import org.canoegame.entity.Commitment;
import org.canoegame.entity.Entity;
import org.canoegame.entity.FieldSet;
import org.canoegame.entity.Key;

public class PersonRecord<T extends PersonRecord<T>> extends Entity<T, PersonRecord.Field> {
    public PersonRecord() {
        super();
    }

    @Override
    protected Commitment createCommitment(FieldSet<Field> changes) {
        return null;
    }

    @Override
    public Key<T> getKey() {
        return null;
    }

    @Override
    protected Class<Field> getFieldClass() {
        return Field.class;
    }

    @Override
    protected void incrementRevision() {

    }

    public enum Field implements org.canoegame.entity.Field {
        NAME(1), EMAIL(2);

        private final int number;

        Field(int number) {
            this.number = number;
        }

        @Override
        public int getNumber() {
            return number;
        }
    }
}
