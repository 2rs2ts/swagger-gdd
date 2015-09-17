package io.swagger.gdd.models;

/**
 * Represents a schema in "schemas".
 */
public class Schema extends AbstractSchema {
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Schema that = (Schema) o;

        if (getId() != null ? !getId().equals(that.getId()) : that.getId() != null) return false;
        if (getType() != null ? !getType().equals(that.getType()) : that.getType() != null) return false;
        if (get$ref() != null ? !get$ref().equals(that.get$ref()) : that.get$ref() != null) return false;
        if (getDescription() != null ? !getDescription().equals(that.getDescription()) : that.getDescription() != null)
            return false;
        if (getLocation() != null ? !getLocation().equals(that.getLocation()) : that.getLocation() != null)
            return false;
        if (getFormat() != null ? !getFormat().equals(that.getFormat()) : that.getFormat() != null) return false;
        if (getPattern() != null ? !getPattern().equals(that.getPattern()) : that.getPattern() != null) return false;
        if (getMinimum() != null ? !getMinimum().equals(that.getMinimum()) : that.getMinimum() != null) return false;
        if (getMaximum() != null ? !getMaximum().equals(that.getMaximum()) : that.getMaximum() != null) return false;
        if (getDefault() != null ? !getDefault().equals(that.getDefault()) : that.getDefault() != null) return false;
        if (getProperties() != null ? !getProperties().equals(that.getProperties()) : that.getProperties() != null)
            return false;
        if (getAdditionalProperties() != null ? !getAdditionalProperties().equals(that.getAdditionalProperties()) : that.getAdditionalProperties() != null)
            return false;
        if (getItems() != null ? !getItems().equals(that.getItems()) : that.getItems() != null) return false;
        if (getAnnotations() != null ? !getAnnotations().equals(that.getAnnotations()) : that.getAnnotations() != null)
            return false;
        if (getEnum() != null ? !getEnum().equals(that.getEnum()) : that.getEnum() != null) return false;
        if (getEnumDescriptions() != null ? !getEnumDescriptions().equals(that.getEnumDescriptions()) : that.getEnumDescriptions() != null)
            return false;
        if (getRequired() != null ? !getRequired().equals(that.getRequired()) : that.getRequired() != null)
            return false;
        return !(getRepeated() != null ? !getRepeated().equals(that.getRepeated()) : that.getRepeated() != null);

    }

    @Override
    public int hashCode() {
        int result = "Schema".hashCode();
        result = 31 * result + (getId() != null ? getId().hashCode() : 0);
        result = 31 * result + (getType() != null ? getType().hashCode() : 0);
        result = 31 * result + (get$ref() != null ? get$ref().hashCode() : 0);
        result = 31 * result + (getDescription() != null ? getDescription().hashCode() : 0);
        result = 31 * result + (getLocation() != null ? getLocation().hashCode() : 0);
        result = 31 * result + (getFormat() != null ? getFormat().hashCode() : 0);
        result = 31 * result + (getPattern() != null ? getPattern().hashCode() : 0);
        result = 31 * result + (getMinimum() != null ? getMinimum().hashCode() : 0);
        result = 31 * result + (getMaximum() != null ? getMaximum().hashCode() : 0);
        result = 31 * result + (getDefault() != null ? getDefault().hashCode() : 0);
        result = 31 * result + (getProperties() != null ? getProperties().hashCode() : 0);
        result = 31 * result + (getAdditionalProperties() != null ? getAdditionalProperties().hashCode() : 0);
        result = 31 * result + (getItems() != null ? getItems().hashCode() : 0);
        result = 31 * result + (getAnnotations() != null ? getAnnotations().hashCode() : 0);
        result = 31 * result + (getEnum() != null ? getEnum().hashCode() : 0);
        result = 31 * result + (getEnumDescriptions() != null ? getEnumDescriptions().hashCode() : 0);
        result = 31 * result + (getRequired() != null ? getRequired().hashCode() : 0);
        result = 31 * result + (getRepeated() != null ? getRepeated().hashCode() : 0);
        return result;
    }
}
