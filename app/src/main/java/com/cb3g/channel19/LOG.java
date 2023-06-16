package com.cb3g.channel19;

public class LOG {
    static void i(String tag, String message) {
        System.out.println("!!I " + tag + ": " + message);
    }

    static void i(String tag, int message) {
        System.out.println("!!I " + tag + ": " + message);
    }

    static void i(String  message) {
        System.out.println("!!I " + message);
    }

    static void i(String tag, long message) {
        System.out.println("!!I " + tag + ": " + message);
    }

    static void s(String tag, long message) {
        System.out.println("!!I " + tag + ": " + message);
        System.out.println("!! ");
    }

    static void e(String tag, String message) {
        System.out.println("!!E " + tag + ": " + message);
    }

    static void e(String message) {
        System.out.println("!!E " + message);
    }
}
