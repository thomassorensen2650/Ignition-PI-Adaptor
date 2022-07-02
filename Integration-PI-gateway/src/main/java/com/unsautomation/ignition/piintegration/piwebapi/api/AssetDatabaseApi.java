package com.unsautomation.ignition.piintegration.piwebapi.api;

import com.inductiveautomation.ignition.common.gson.JsonArray;
import com.inductiveautomation.ignition.common.gson.JsonObject;
import com.unsautomation.ignition.piintegration.piwebapi.ApiClient;
import com.unsautomation.ignition.piintegration.piwebapi.ApiException;

public class AssetDatabaseApi {


    private final ApiClient client;

    public AssetDatabaseApi(ApiClient client) {
        this.client = client;
    }

    /**
     * Retrieve a list of all Asset Servers known to this service.
     *
     */
    public JsonArray list(String afServerWebID, String selectedFields) throws ApiException {

        if (client.getSimulationMode()) {
            var r = new JsonArray();
            var first = new JsonObject();
            first.addProperty("name", "First AF Server");
            var second = new JsonObject();
            second.addProperty("name", "Second AF Server");
            r.add(first);
            r.add(second);
            return r;
        }
        var url = String.format("assetservers/%s/assetdatabases",afServerWebID);
        return client.doGet(url).getContent().getAsJsonObject().get("Items").getAsJsonArray();
    }

    public JsonObject getByPath(String path) throws ApiException {

        if (client.getSimulationMode()) {
            var first = new JsonObject();
            first.addProperty("webId","Element");
            return first;
        }
        path = client.urlEncode(path);
        return client.doGet("assetdatabases?path=" + path).getContent().getAsJsonObject();
    }

    public JsonArray getElements(String afDBWebId) throws ApiException {
        var url = String.format("assetdatabases/%s/elements",afDBWebId);
        return client.doGet(url).getContent().getAsJsonObject().get("Items").getAsJsonArray();
    }

}
