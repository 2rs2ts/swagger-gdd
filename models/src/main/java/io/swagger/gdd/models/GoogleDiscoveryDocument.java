package io.swagger.gdd.models;

import java.util.List;
import java.util.Map;

/**
 * Models the entire Google Discovery Document.
 *
 * <a href="https://developers.google.com/discovery/v1/reference/apis?hl=en">GDD docs</a>
 */
public class GoogleDiscoveryDocument {
    private String kind = "discovery@restDescription";
    private String discoveryVersion = "v1";
    private String id;
    private String name;
    private String version;
    private String revision;
    private String title;
    private String description;
    private String documentationLink;
    private String protocol = "rest";
    private String rootUrl;
    private String servicePath;
    private String batchPath;
    private Icons icons;
    /**
     * Labels for the status of this API.
     */
    private List<Label> labels;
    /**
     * Common parameters that apply across all APIs. Unlike in swagger, in which the global parameters definition only
     * defines parameters that can be referenced by operations, adding a parameter here means it applies to every
     * Method.
     */
    private Map<String, Parameter> parameters;
    /**
     * Authentication information.
     */
    private Auth auth;
    /**
     * A list of supported features for this API.
     */
    private List<String> features;
    /**
     * Schema definitions used in this document.
     */
    private Map<String, Schema> schemas;
    /**
     * API-level methods.
     */
    private Map<String, Method> methods;
    /**
     * Resource-level methods.
     */
    private Map<String, Resource> resources;

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public String getDiscoveryVersion() {
        return discoveryVersion;
    }

    public void setDiscoveryVersion(String discoveryVersion) {
        this.discoveryVersion = discoveryVersion;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getRevision() {
        return revision;
    }

    public void setRevision(String revision) {
        this.revision = revision;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDocumentationLink() {
        return documentationLink;
    }

    public void setDocumentationLink(String documentationLink) {
        this.documentationLink = documentationLink;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getRootUrl() {
        return rootUrl;
    }

    public void setRootUrl(String rootUrl) {
        this.rootUrl = rootUrl;
    }

    public String getServicePath() {
        return servicePath;
    }

    public void setServicePath(String servicePath) {
        this.servicePath = servicePath;
    }

    public String getBatchPath() {
        return batchPath;
    }

    public void setBatchPath(String batchPath) {
        this.batchPath = batchPath;
    }

    public Icons getIcons() {
        return icons;
    }

    public void setIcons(Icons icons) {
        this.icons = icons;
    }

    public List<Label> getLabels() {
        return labels;
    }

    public void setLabels(List<Label> labels) {
        this.labels = labels;
    }

    public Map<String, Parameter> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, Parameter> parameters) {
        this.parameters = parameters;
    }

    public Auth getAuth() {
        return auth;
    }

    public void setAuth(Auth auth) {
        this.auth = auth;
    }

    public List<String> getFeatures() {
        return features;
    }

    public void setFeatures(List<String> features) {
        this.features = features;
    }

    public Map<String, Schema> getSchemas() {
        return schemas;
    }

    public void setSchemas(Map<String, Schema> schemas) {
        this.schemas = schemas;
    }

    public Map<String, Method> getMethods() {
        return methods;
    }

    public void setMethods(Map<String, Method> methods) {
        this.methods = methods;
    }

    public Map<String, Resource> getResources() {
        return resources;
    }

    public void setResources(Map<String, Resource> resources) {
        this.resources = resources;
    }

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
}
