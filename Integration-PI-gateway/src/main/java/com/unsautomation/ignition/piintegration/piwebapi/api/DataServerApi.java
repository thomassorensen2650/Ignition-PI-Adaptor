package com.unsautomation.ignition.piintegration.piwebapi.api;

import com.inductiveautomation.ignition.common.gson.JsonArray;
import com.inductiveautomation.ignition.common.gson.JsonObject;
import com.unsautomation.ignition.piintegration.piwebapi.ApiClient;
import com.unsautomation.ignition.piintegration.piwebapi.ApiException;

public class DataServerApi {

    public DataServerApi(ApiClient apiClient) {

    }

    /**
     * Retrieve a list of all Asset Servers known to this service.
     *
     */
    public JsonArray list(String selectedFields) throws ApiException {
        var r = new JsonArray();
        var first = new JsonObject();
        first.addProperty("name","First Server");
        var second = new JsonObject();
        second.addProperty("name", "Second Server");
        r.add(first);
        r.add(second);
        return r;
    }

    public JsonObject getByPath(String path) {
        var first = new JsonObject();
        first.addProperty("webId","First Server");
        return first;
    }

    public JsonArray getPoints(String dataServerWebId, String nameFilter, Integer startIndex, Integer maxCount, String selectedFields) {
        var r = new JsonArray();
        for (int i = 0; i < 100; i++) {
            var first = new JsonObject();
            first.addProperty("name","Tag" + i);
            r.add(first);
        }
        return r;
    }
}
