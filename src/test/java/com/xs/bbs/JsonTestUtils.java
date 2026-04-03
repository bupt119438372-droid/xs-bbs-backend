package com.xs.bbs;

import com.jayway.jsonpath.JsonPath;

public final class JsonTestUtils {

    private JsonTestUtils() {
    }

    public static String readString(String json, String path) {
        return JsonPath.read(json, path);
    }

    public static Integer readInteger(String json, String path) {
        return JsonPath.read(json, path);
    }
}
