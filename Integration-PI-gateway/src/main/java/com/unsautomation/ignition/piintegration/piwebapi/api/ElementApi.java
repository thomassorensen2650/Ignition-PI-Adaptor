package com.unsautomation.ignition.piintegration.piwebapi.api;

import com.inductiveautomation.ignition.common.gson.JsonArray;
import com.inductiveautomation.ignition.common.gson.JsonObject;
import com.unsautomation.ignition.piintegration.piwebapi.ApiClient;
import com.unsautomation.ignition.piintegration.piwebapi.ApiException;
import com.unsautomation.ignition.piintegration.piwebapi.PIDataSimulator;
import com.unsautomation.ignition.piintegration.piwebapi.UrlUtils;

public class ElementApi {

    private final ApiClient client;

    public ElementApi(ApiClient client) {
        this.client = client;
    }

    public JsonObject getByPath(String path) throws ApiException {
        if (client.getSimulationMode()) {
            client.getSimulator().getElements(1);
        }
        path = UrlUtils.urlEncode(path);
        return client.doGet("elements?path=" + path).getContent().getAsJsonObject();
    }

    public JsonArray getElements(String elementWebId) throws ApiException {
        if (client.getSimulationMode()) {
            return client.getSimulator().getAFElements(5).getAsJsonArray();
        }
        var url = String.format("elements/%s/elements",elementWebId);
        return client.doGet(url).getContent().getAsJsonObject().get("Items").getAsJsonArray();
    }

    // elements/{webId}/attributes
    public JsonArray getAttributes(String elementWebId) throws ApiException {
        if (client.getSimulationMode()) {
            client.getSimulator().getAttributes(5);
        }
        var url = String.format("elements/%s/attributes",elementWebId);
        return client.doGet(url).getContent().getAsJsonObject().get("Items").getAsJsonArray();
    }
    
    public Boolean elementNameValid(String elementName) {
        var regex = "^[^\\*\\?\\;\\{\\}\\[\\]\\|\\\\\\`\\'\\\"]{1,259}$";
        return elementName.matches(regex);
    }
}