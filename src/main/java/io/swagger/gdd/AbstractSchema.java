package io.swagger.gdd;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * The subset of JSON Schema that GDD exposes. Represents both "schemas" and "parameters".
 */
public class AbstractSchema {
    public String id, type, $ref, description, location, format, pattern, minimum, maximum;
    @JsonProperty("default") public String _default;
    public Map<String, Schema> properties;
    public Schema additionalProperties, items;
    public Annotations annotations; // todo populate this
    @JsonProperty("enum") public List<String> _enum;
    public List<String> enumDescriptions;
    public Boolean required, repeated;

    /**
     * Models the "annotations" property.
     */
    public static class Annotations {
        public String[] required;
    }
}
