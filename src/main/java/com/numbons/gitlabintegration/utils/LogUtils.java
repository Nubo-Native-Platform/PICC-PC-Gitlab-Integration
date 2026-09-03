package com.numbons.gitlabintegration.utils;

public class LogUtils {
    public static String  sanitizeForLog(Object value) {
        if (value == null) {
            return "";
        }

        return value.toString()
                .replace("\r", "\\r")
                .replace("\n", "\\n");
    }
}
