package io.swagger.gdd.models.factory;

import io.swagger.gdd.models.*;

/**
 * Creates the GDD model classes. Extending this class will let you inject custom implementations of the models.
 */
public class GDDModelFactory {
    public Annotations newAnnotations() { return new Annotations(); }
    public Auth newAuth() { return new Auth(); }
    public GoogleDiscoveryDocument newGoogleDiscoveryDocument() { return new GoogleDiscoveryDocument(); }
    public Icons newIcons() { return new Icons(); }
    public MediaUpload newMediaUpload() { return new MediaUpload(); }
    public Method newMethod() { return new Method(); }
    public OAuth2 newOAuth2() { return new OAuth2(); }
    public Parameter newParameter() { return new Parameter(); }
    public Protocol newProtocol() { return new Protocol(); }
    public Resource newResource() { return new Resource(); }
    public Schema newSchema() { return new Schema(); }
    public SchemaRef newSchemaRef(String $ref) { return new SchemaRef($ref); }
    public Scope newScope() { return new Scope(); }
}
