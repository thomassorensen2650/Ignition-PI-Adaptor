package com.unsautomation.ignition.piintegration.piwebapi.api;

import com.inductiveautomation.ignition.common.gson.JsonArray;
import com.inductiveautomation.ignition.common.gson.JsonObject;
import com.unsautomation.ignition.piintegration.piwebapi.ApiClient;
import com.unsautomation.ignition.piintegration.piwebapi.ApiException;

public class AssetServerApi {

    private final ApiClient client;

    public AssetServerApi(ApiClient client) {
        this.client = client;
    }
    /**
     * Retrieve a list of all Asset Servers known to this service.
     *
     */
    public JsonArray list(String selectedFields) throws ApiException {

        if (false) {
            var r = new JsonArray();
            var first = new JsonObject();
            first.addProperty("name", "First AF Server");
            var second = new JsonObject();
            second.addProperty("name", "Second AF Server");
            r.add(first);
            r.add(second);
            return r;
        }
        return client.doGet("assetservers").getAsJsonObject().get("Items").getAsJsonArray();
    }

}