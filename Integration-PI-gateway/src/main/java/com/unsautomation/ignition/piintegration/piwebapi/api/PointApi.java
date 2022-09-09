package com.unsautomation.ignition.piintegration.piwebapi.api;

import com.inductiveautomation.ignition.common.gson.JsonObject;
import com.unsautomation.ignition.piintegration.piwebapi.ApiClient;
import com.unsautomation.ignition.piintegration.piwebapi.ApiException;
import com.unsautomation.ignition.piintegration.piwebapi.UrlUtils;
import org.apache.http.client.HttpResponseException;

public class PointApi {

    private final ApiClient client;
    public PointApi(ApiClient apiClient) {
        this.client = apiClient;
    }

    public JsonObject Get(String webId) {
        return null;
    }

    public JsonObject getByPath(String path) throws ApiException {

        if (client.getSimulationMode()) {
            var first = new JsonObject();
            first.addProperty("WebId","PI Tag");
            first.addProperty("name","PI Tag");
            return first;
        }
        var urlParams = UrlUtils.urlEncode(path);
        return client.doGet("points?path=" + urlParams).getContent().getAsJsonObject();
    }
}
