package io.swagger.gdd;

import java.lang.Override;

/**
 * Valid values for
 */
public enum Label {
    LIMITED_AVAILABILITY("limited_availability"), DEPRECATED("deprecated");
    private final String label;
    private Label(final String label) {
        this.label = label;
    }
    @Override
    public String toString() {
        return label;
    }
}
