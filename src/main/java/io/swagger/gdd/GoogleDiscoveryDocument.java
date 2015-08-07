package io.swagger.gdd;

import java.util.List;
import java.util.Map;

/**
 * Models the entire Google Discovery Document.
 *
 * <a href="https://developers.google.com/discovery/v1/reference/apis?hl=en">GDD docs</a>
 */
public class GoogleDiscoveryDocument {
    // todo populate discoveryVersion
    public String kind = "discovery@restDescription", discoveryVersion, id, name, version, revision, title, description,
            documentationLink, protocol = "rest", rootUrl, servicePath, batchPath;
    public Icons icons;
    /**
     * Labels for the status of this API.
     */
    public List<Label> labels;
    /**
     * Common parameters that apply across all APIs. Unlike in swagger, in which the global parameters definition only
     * defines parameters that can be referenced by operations, adding a parameter here means it applies to every
     * Method.
     */
    public Map<String, Parameter> parameters;
    /**
     * Authentication information.
     */
    public Auth auth;
    /**
     * A list of supported features for this API.
     */
    public List<String> features;
    /**
     * Schema definitions used in this document.
     */
    public Map<String, Schema> schemas;
    /**
     * API-level methods.
     */
    public Map<String, Method> methods;
    /**
     * Resource-level methods.
     */
    public Map<String, Resource> resources;

    /**
     * Models the "icons" property.
     */
    public static class Icons {
        public String x16, x32;
    }
}
