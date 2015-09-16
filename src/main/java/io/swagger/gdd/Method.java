package io.swagger.gdd;

import java.util.List;
import java.util.Map;

public class Method {
    private String id;
    private String description;
    private String path;
    private String httpMethod;
    private SchemaRef request;
    private SchemaRef response;
    private Map<String, Parameter> parameters;
    private List<String> parameterOrder;
    private List<String> scopes;
    private Boolean supportsMediaDownload;
    private Boolean supportsMediaUpload;
    private Boolean supportsSubscription;
    private MediaUpload mediaUpload;
    @Deprecated private String baseUrl;
    @Deprecated private String basePath;

    /**
     * Models the "request" and "response" properties.
     */
    public static class SchemaRef {
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

    /**
     * Models the "mediaUpload" property.
     */
    public static class MediaUpload {
        private List<String> accept;
        private String maxSize;
        private Protocol simple;
        private Protocol resumable;

        public static class Protocol {
            private Boolean multipart;
            private String path;

            public String getPath() {
                return path;
            }

            public void setPath(String path) {
                this.path = path;
            }

            public Boolean getMultipart() {
                return multipart;
            }

            public void setMultipart(Boolean multipart) {
                this.multipart = multipart;
            }
        }

        public List<String> getAccept() {
            return accept;
        }

        public void setAccept(List<String> accept) {
            this.accept = accept;
        }

        public String getMaxSize() {
            return maxSize;
        }

        public void setMaxSize(String maxSize) {
            this.maxSize = maxSize;
        }

        public Protocol getSimple() {
            return simple;
        }

        public void setSimple(Protocol simple) {
            this.simple = simple;
        }

        public Protocol getResumable() {
            return resumable;
        }

        public void setResumable(Protocol resumable) {
            this.resumable = resumable;
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    public SchemaRef getRequest() {
        return request;
    }

    public void setRequest(SchemaRef request) {
        this.request = request;
    }

    public SchemaRef getResponse() {
        return response;
    }

    public void setResponse(SchemaRef response) {
        this.response = response;
    }

    public Map<String, Parameter> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, Parameter> parameters) {
        this.parameters = parameters;
    }

    public List<String> getParameterOrder() {
        return parameterOrder;
    }

    public void setParameterOrder(List<String> parameterOrder) {
        this.parameterOrder = parameterOrder;
    }

    public List<String> getScopes() {
        return scopes;
    }

    public void setScopes(List<String> scopes) {
        this.scopes = scopes;
    }

    public Boolean getSupportsMediaDownload() {
        return supportsMediaDownload;
    }

    public void setSupportsMediaDownload(Boolean supportsMediaDownload) {
        this.supportsMediaDownload = supportsMediaDownload;
    }

    public Boolean getSupportsMediaUpload() {
        return supportsMediaUpload;
    }

    public void setSupportsMediaUpload(Boolean supportsMediaUpload) {
        this.supportsMediaUpload = supportsMediaUpload;
    }

    public Boolean getSupportsSubscription() {
        return supportsSubscription;
    }

    public void setSupportsSubscription(Boolean supportsSubscription) {
        this.supportsSubscription = supportsSubscription;
    }

    public MediaUpload getMediaUpload() {
        return mediaUpload;
    }

    public void setMediaUpload(MediaUpload mediaUpload) {
        this.mediaUpload = mediaUpload;
    }

    @Deprecated
    public String getBaseUrl() {
        return baseUrl;
    }

    @Deprecated
    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    @Deprecated
    public String getBasePath() {
        return basePath;
    }

    @Deprecated
    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }

}
