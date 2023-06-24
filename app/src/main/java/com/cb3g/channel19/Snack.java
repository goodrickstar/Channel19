package com.cb3g.channel19;


import com.google.android.material.snackbar.Snackbar;

public class Snack {
    private String message;
    private int length = Snackbar.LENGTH_INDEFINITE;

    public Snack(String message) {
        this.message = message;
    }

    public Snack(String message, int length) {
        this.message = message;
        this.length = length;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }
}
