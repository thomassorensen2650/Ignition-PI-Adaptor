package com.unsautomation.ignition.piintegration.piwebapi.api;

import com.inductiveautomation.ignition.common.gson.JsonArray;
import com.inductiveautomation.ignition.common.gson.JsonObject;
import com.unsautomation.ignition.piintegration.piwebapi.ApiClient;
import com.unsautomation.ignition.piintegration.piwebapi.ApiException;
import com.unsautomation.ignition.piintegration.piwebapi.UrlUtils;
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
            first.addProperty("name","Root Element");
            return first;
        }
        path = UrlUtils.urlEncode(path);
        return client.doGet("elements?path=" + path).getContent().getAsJsonObject();
    }

    public JsonArray getElements(String elementWebId) throws ApiException {
        if (client.getSimulationMode()) {
            var r = new JsonArray();
            var first = new JsonObject();
            first.addProperty("name", "Child Element");
            var second = new JsonObject();
            second.addProperty("name", "Child Element2");
            r.add(first);
            r.add(second);
            return r;
        }
        var url = String.format("elements/%s/elements",elementWebId);
        return client.doGet(url).getContent().getAsJsonObject().get("Items").getAsJsonArray();
    }

    // elements/{webId}/attributes
    public JsonArray getAttributes(String elementWebId) throws ApiException {
        if (client.getSimulationMode()) {
            var r = new JsonArray();
            var first = new JsonObject();
            first.addProperty("name", "Attribute 1");
            var second = new JsonObject();
            second.addProperty("name", "Attribute 2");
            r.add(first);
            r.add(second);
            return r;
        }

        var url = String.format("elements/%s/attributes",elementWebId);
        return client.doGet(url).getContent().getAsJsonObject().get("Items").getAsJsonArray();
    }
}
