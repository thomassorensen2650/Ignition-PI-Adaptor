package com.unsautomation.ignition.piintegration.piwebapi.api;

import com.inductiveautomation.ignition.common.gson.JsonObject;
import com.unsautomation.ignition.piintegration.piwebapi.ApiClient;
import com.unsautomation.ignition.piintegration.piwebapi.ApiException;
import com.unsautomation.ignition.piintegration.piwebapi.UrlUtils;

public class HomeApi {

    private final ApiClient client;
    public HomeApi(ApiClient apiClient) {
        this.client = apiClient;
    }

    public JsonObject get() throws ApiException {
        if (client.getSimulationMode()) {
            var first = new JsonObject();
            first.addProperty("Self","http://127.0.0.1");
            first.addProperty("AssetServers","http://127.0.0.1");
            return first;
        }
        return client.doGet("").getContent().getAsJsonObject();
    }
}
