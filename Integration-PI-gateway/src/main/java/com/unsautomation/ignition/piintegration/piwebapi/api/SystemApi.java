package com.unsautomation.ignition.piintegration.piwebapi.api;

import com.inductiveautomation.ignition.common.gson.JsonObject;
import com.unsautomation.ignition.piintegration.piwebapi.ApiClient;
import com.unsautomation.ignition.piintegration.piwebapi.ApiException;

import java.util.Date;

public class SystemApi {

    private final ApiClient client;
    public SystemApi(ApiClient client) {
        this.client = client;
    }

    public JsonObject getLanding() {
        return null;
    }

    public JsonObject getStatus() throws ApiException {

        if (client.getSimulationMode()) {
            var x = new JsonObject();
            x.addProperty("State", "Running");
            x.addProperty("ServerTime", new Date().toString());
            return x;
        }
        return client.doGet("system/status").getContent().getAsJsonObject();
    }
}
