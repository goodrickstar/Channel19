package com.cb3g.channel19;

public class FileUpload {
    String uri;
    RequestCode code;

    public FileUpload(String uri, RequestCode code, String sendToId, String sendToHandle, int height, int width) {
        this.uri = uri;
        this.code = code;
        this.sendToId = sendToId;
        this.sendToHandle = sendToHandle;
        this.height = height;
        this.width = width;
    }

    public FileUpload(String uri, RequestCode code, int height, int width) {
        this.uri = uri;
        this.code = code;
        this.height = height;
        this.width = width;
    }

    public FileUpload(String uri, RequestCode code, String sendToId, String sendToHandle) {
        this.uri = uri;
        this.code = code;
        this.sendToId = sendToId;
        this.sendToHandle = sendToHandle;
    }

    String sendToId;
    String sendToHandle;

    String caption = "";

    public String getCaption() {
        return caption;
    }

    public void setCaption(String caption) {
        this.caption = caption;
    }

    int height;
    int width;

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public RequestCode getCode() {
        return code;
    }

    public void setCode(RequestCode code) {
        this.code = code;
    }

    public String getSendToId() {
        return sendToId;
    }

    public void setSendToId(String sendToId) {
        this.sendToId = sendToId;
    }

    public String getSendToHandle() {
        return sendToHandle;
    }

    public void setSendToHandle(String sendToHandle) {
        this.sendToHandle = sendToHandle;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }
}
