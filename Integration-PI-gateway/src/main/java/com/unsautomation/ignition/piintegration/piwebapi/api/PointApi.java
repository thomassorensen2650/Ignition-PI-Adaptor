package com.unsautomation.ignition.piintegration.piwebapi.api;

import com.inductiveautomation.ignition.common.gson.JsonObject;
import com.unsautomation.ignition.piintegration.piwebapi.ApiClient;
import com.unsautomation.ignition.piintegration.piwebapi.ApiException;
import com.unsautomation.ignition.piintegration.piwebapi.PIDataSimulator;
import com.unsautomation.ignition.piintegration.piwebapi.UrlUtils;

public class PointApi {

    private final ApiClient client;
    public PointApi(ApiClient apiClient) {
        this.client = apiClient;
    }

    public JsonObject getByPath(String path) throws ApiException {

        if (client.getSimulationMode()) {
            client.getSimulator().getPoints(1);
        }
        var urlParams = UrlUtils.urlEncode(path);
        return client.doGet("points?path=" + urlParams).getContent().getAsJsonObject();
    }

    public JsonObject get(String webId) throws ApiException {

        if (client.getSimulationMode()) {
            client.getSimulator().getPoints(1);
        }
        var url = String.format("points/%s", webId);
        return client.doGet(url).getContent().getAsJsonObject();
    }
}
