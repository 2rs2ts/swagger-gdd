package io.swagger.gdd.models;

import java.util.Arrays;

/**
 * Models the "annotations" property.
 */
public class Annotations {
    private String[] required;

    public String[] getRequired() {
        return required;
    }

    public void setRequired(String[] required) {
        this.required = required;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Annotations that = (Annotations) o;

        return Arrays.equals(getRequired(), that.getRequired());
    }

    @Override
    public int hashCode() {
        return getRequired() != null ? Arrays.hashCode(getRequired()) : 0;
    }
}
