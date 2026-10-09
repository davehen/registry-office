package com.arhs.tools.ai.exception;

public class DuplicatePropertyException extends IllegalArgumentException {

    public enum UniqueProperty {
        EMAIL("E-mail"),
        USERNAME("Username");

        public final String label;

        UniqueProperty(String label) {
            this.label = label;
        }
    }

    private final UniqueProperty property;

    public DuplicatePropertyException(UniqueProperty property) {
        super();
        this.property = property;
    }

    public UniqueProperty getProperty() {
        return property;
    }

    @Override
    public String getMessage() {
        return property.label + " already exists";
    }
}
