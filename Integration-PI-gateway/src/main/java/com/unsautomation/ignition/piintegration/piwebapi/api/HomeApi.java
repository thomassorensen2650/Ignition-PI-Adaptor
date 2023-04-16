package com.unsautomation.ignition.piintegration.piwebapi.api;

import com.inductiveautomation.ignition.common.gson.JsonObject;
import com.unsautomation.ignition.piintegration.piwebapi.ApiClient;
import com.unsautomation.ignition.piintegration.piwebapi.ApiException;

public class HomeApi {

    private final ApiClient client;
    public HomeApi(ApiClient apiClient) {
        this.client = apiClient;
    }

    public JsonObject get() throws ApiException {
        if (client.getSimulationMode()) {
            return new JsonObject(); // Only used to check status
        }
        return client.doGet("").getContent().getAsJsonObject();
    }
}