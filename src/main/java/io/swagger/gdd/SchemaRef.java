package io.swagger.gdd;

/**
 * Models the "request" and "response" properties of Methods.
 */
public class SchemaRef {
    private String $ref;

    public SchemaRef(String $ref) {
        this.$ref = $ref;
    }

    public String get$ref() {
        return $ref;
    }

    public void set$ref(String $ref) {
        this.$ref = $ref;
    }
}
