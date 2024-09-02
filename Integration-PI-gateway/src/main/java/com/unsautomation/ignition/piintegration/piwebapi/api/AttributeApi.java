package com.unsautomation.ignition.piintegration.piwebapi.api;

import com.inductiveautomation.ignition.common.gson.JsonObject;
import com.unsautomation.ignition.piintegration.piwebapi.ApiClient;
import com.unsautomation.ignition.piintegration.piwebapi.ApiException;
import com.unsautomation.ignition.piintegration.piwebapi.UrlUtils;

public class AttributeApi {

    private final ApiClient client;
    public AttributeApi(ApiClient apiClient) {
        this.client = apiClient;
    }

    public JsonObject getByPath(String path) throws ApiException {

        if (client.getSimulationMode()) {
            client.getSimulator().getAttributes(1);
        }
        var urlParams = UrlUtils.urlEncode(path);
        return client.doGet("Attribute?path=" + urlParams).getContent().getAsJsonObject();
    }

    public JsonObject get(String webId) throws ApiException {

        if (client.getSimulationMode()) {
            client.getSimulator().getAttributes(1); // TODO Attribute Simulator
        }
        var url = String.format("Attribute/%s", webId);
        return client.doGet(url).getContent().getAsJsonObject();

    }
}