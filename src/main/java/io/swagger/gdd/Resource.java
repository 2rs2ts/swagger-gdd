package io.swagger.gdd;

import java.util.Map;

public class Resource {
    /**
     * Resource-level methods.
     */
    private Map<String, Method> methods;
    /**
     * Sub-resources.
     */
    private Map<String, Resource> resources;

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
}
