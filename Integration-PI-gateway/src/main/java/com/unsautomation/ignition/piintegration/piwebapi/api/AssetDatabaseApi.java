package com.unsautomation.ignition.piintegration.piwebapi.api;

import com.inductiveautomation.ignition.common.gson.JsonArray;
import com.inductiveautomation.ignition.common.gson.JsonObject;
import com.unsautomation.ignition.piintegration.piwebapi.ApiClient;
import com.unsautomation.ignition.piintegration.piwebapi.ApiException;
import com.unsautomation.ignition.piintegration.piwebapi.UrlUtils;

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
            first.addProperty("name", "First AF DB");
            var second = new JsonObject();
            second.addProperty("name", "Second AF DB");
            r.add(first);
            r.add(second);
            return r;
        }
        var url = String.format("assetservers/%s/assetdatabases",afServerWebID);
        url = UrlUtils.addUrlParameter(url, "selectedFields", selectedFields);

        return client.doGet(url).getContent().getAsJsonObject().get("Items").getAsJsonArray();
    }

    public JsonObject getByPath(String path) throws ApiException {

        if (client.getSimulationMode()) {
            var first = new JsonObject();
            first.addProperty("webId","Element");
            return first;
        }
        path = UrlUtils.urlEncode(path);
        return client.doGet("assetdatabases?path=" + path).getContent().getAsJsonObject();
    }

    public JsonArray getElements(String afDBWebId) throws ApiException {
        if (client.getSimulationMode()) {
            var arr = new JsonArray();
            var first = new JsonObject();
            first.addProperty("webId","Element");
            first.addProperty("name","Element");
            arr.add(first);
            return arr;
        }
        var url = String.format("assetdatabases/%s/elements",afDBWebId);
        return client.doGet(url).getContent().getAsJsonObject().get("Items").getAsJsonArray();
    }

}
