package io.swagger.gdd.models;

/**
 * Models the "simple" and "resumable" fields of MediaUpload.
 */
public class Protocol {
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
