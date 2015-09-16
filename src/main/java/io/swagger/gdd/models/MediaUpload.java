package io.swagger.gdd.models;

import java.util.List;

/**
 * Models the "mediaUpload" property.
 */
public class MediaUpload {
    private List<String> accept;
    private String maxSize;
    private Protocol simple;
    private Protocol resumable;

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
