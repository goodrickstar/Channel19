package com.cb3g.channel19;

public class Link {

    public Link() {
    }

    public Link(String domain, String pageUrl, String pageTitle, String pageDescription, String imageUrl, String imageAltUrl) {
        this.domain = domain;
        this.pageUrl = pageUrl;
        this.pageTitle = pageTitle;
        this.pageDescription = pageDescription;
        this.imageUrl = imageUrl;
        this.imageAltUrl = imageAltUrl;
    }

    private String domain;

    private String pageUrl;

    private String pageTitle;

    private String pageDescription;

    private String imageUrl;

    private String imageAltUrl;

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getPageUrl() {
        return pageUrl;
    }

    public void setPageUrl(String pageUrl) {
        this.pageUrl = pageUrl;
    }

    public String getPageTitle() {
        return pageTitle;
    }

    public void setPageTitle(String pageTitle) {
        this.pageTitle = pageTitle;
    }

    public String getPageDescription() {
        return pageDescription;
    }

    public void setPageDescription(String pageDescription) {
        this.pageDescription = pageDescription;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getImageAltUrl() {
        return imageAltUrl;
    }

    public void setImageAltUrl(String imageAltUrl) {
        this.imageAltUrl = imageAltUrl;
    }

    // Setters and Getters goes here
}