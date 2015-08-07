package io.swagger.gdd;

import java.util.Map;

public class Resource {
    /**
     * Resource-level methods.
     */
    public Map<String, Method> methods;
    /**
     * Sub-resources.
     */
    public Map<String, Resource> resources;
}
