package io.swagger.gdd;

import java.util.List;
import java.util.Map;

public class Method {
    public String id, description, path, httpMethod;
    public SchemaRef request, response;
    public Map<String, Parameter> parameters;
    public List<String> parameterOrder, scopes;
    public Boolean supportsMediaDownload, supportsMediaUpload, supportsSubscription;
    public MediaUpload mediaUpload;
    @Deprecated public String baseUrl, basePath;

    /**
     * Models the "request" and "response" properties.
     */
    public static class SchemaRef {
        public String $ref;

        public SchemaRef(String $ref) {
            this.$ref = $ref;
        }
    }

    /**
     * Models the "mediaUpload" property.
     */
    public static class MediaUpload {
        public List<String> accept;
        public String maxSize;
        public Protocol simple, resumable;

        public static class Protocol {
            public Boolean multipart;
            public String path;
        }
    }

}
