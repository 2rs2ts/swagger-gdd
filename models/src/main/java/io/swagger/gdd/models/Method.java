package io.swagger.gdd.models;

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
