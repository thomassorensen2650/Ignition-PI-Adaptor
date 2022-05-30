package com.unsautomation.ignition.piintegration.piwebapi.api;

import com.inductiveautomation.ignition.common.gson.JsonObject;
import com.unsautomation.ignition.piintegration.piwebapi.ApiClient;

import java.util.Date;

public class SystemApi {

    private final ApiClient client;
    public SystemApi(ApiClient client) {
        this.client = client;
    }

    public JsonObject getLanding() {
        return null;
    }

    public JsonObject getStatus() {

        var x = new JsonObject();
        x.addProperty("State", "Running");
        x.addProperty("ServerTime", new Date().toString());
        return x;
    }
}
