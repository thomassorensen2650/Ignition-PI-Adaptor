package com.unsautomation.ignition.piintegration.piwebapi.api;

import com.inductiveautomation.ignition.common.gson.JsonArray;
import com.inductiveautomation.ignition.common.gson.JsonObject;
import com.unsautomation.ignition.piintegration.piwebapi.ApiClient;
import com.unsautomation.ignition.piintegration.piwebapi.ApiException;
import com.unsautomation.ignition.piintegration.piwebapi.model.PIResponse;

public class ElementApi {

    private final ApiClient client;

    public ElementApi(ApiClient client) {
        this.client = client;
    }

    public JsonObject getByPath(String path) throws ApiException {

        if (client.getSimulationMode()) {
            var first = new JsonObject();
            first.addProperty("webId","Element");
            return first;
        }
        path = client.urlEncode(path);
        return client.doGet("elements?path=" + path).getContent().getAsJsonObject();
    }

    public JsonArray getElements(String elementWebId) throws ApiException {
        var url = String.format("elements/%s/elements",elementWebId);
        return client.doGet(url).getContent().getAsJsonObject().get("Items").getAsJsonArray();
    }


}
