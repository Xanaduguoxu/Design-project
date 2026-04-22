package org.example.utils;

import cn.hutool.json.JSONObject;

public class ReqUtils {

    public static Integer getCurrentPage(JSONObject req) {
        return req.getInt("currentPage");
    }

    public static Integer getPageSize(JSONObject req) {
        return req.getInt("pageSize");
    }

    public static String getKeyword(JSONObject req) {
        return req.getStr("keyword");
    }

    public static String getCategory(JSONObject req) {
        return req.getStr("category");
    }

    public static String getName(JSONObject req) {
        return req.getStr("name");
    }

    public static String getStatus(JSONObject req) {
        return req.getStr("status");
    }

    public static String getType(JSONObject req) {
        return req.getStr("type");
    }
}
