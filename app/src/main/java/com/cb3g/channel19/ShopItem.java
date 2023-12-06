package com.cb3g.channel19;

public class ShopItem {
    private int id = 0;
    private String label = "";
    private int cost = 0;
    private int version = 0;

    public ShopItem(int id, String label, int cost) {
        this.id = id;
        this.label = label;
        this.cost = cost;
    }

    public ShopItem(int id, String label, int cost, int version) {
        this.id = id;
        this.label = label;
        this.cost = cost;
        this.version = version;
    }

    public ShopItem() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public int getCost() {
        return cost;
    }

    public void setCost(int cost) {
        this.cost = cost;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }
}
