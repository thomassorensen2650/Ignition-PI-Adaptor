package com.unsautomation.ignition.piintegration.piwebapi.api;


import com.inductiveautomation.ignition.common.gson.JsonArray;
import com.inductiveautomation.ignition.common.gson.JsonObject;
import com.unsautomation.ignition.piintegration.piwebapi.ApiClient;

public class SearchApi {

    public SearchApi(ApiClient client) {

    }

    public JsonArray AfChildren(String parentPath) {
        var r = new JsonArray();
        var first = new JsonObject();
        first.addProperty("name", parentPath + "First");
        var second = new JsonObject();
        second.addProperty("name", parentPath + "Second");
        r.add(first);
        r.add(second);
        return r;
    }
}
