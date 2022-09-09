package com.unsautomation.ignition.piintegration.piwebapi.api;

import com.inductiveautomation.ignition.common.gson.JsonArray;
import com.inductiveautomation.ignition.common.gson.JsonObject;
import com.unsautomation.ignition.piintegration.piwebapi.ApiClient;
import com.unsautomation.ignition.piintegration.piwebapi.ApiException;
import com.unsautomation.ignition.piintegration.piwebapi.UrlUtils;
import com.unsautomation.ignition.piintegration.piwebapi.WebIdUtils;
import org.apache.http.client.HttpResponseException;

public class AssetServerApi {

    private final ApiClient client;

    public AssetServerApi(ApiClient client) {
        this.client = client;
    }
    /**
     * Retrieve a list of all Asset Servers known to this service.
     *
     */
    public JsonArray list(String selectedFields) throws ApiException {

        if (client.getSimulationMode()) {
            var r = new JsonArray();
            var first = new JsonObject();
            first.addProperty("Name", "First AF Server");

            first.addProperty("WebId","xxRSENCODEDAFSERVER1");
            var second = new JsonObject();
            second.addProperty("Name", "Second AF Server");
            second.addProperty("WebId","xxRSENCODEDAFSERVER2");
            r.add(first);
            r.add(second);
            return r;
        }
        var url = UrlUtils.addUrlParameter("assetservers", "selectedFields", selectedFields);
        return client.doGet(url).getContent().getAsJsonObject().get("Items").getAsJsonArray();
    }

    public JsonArray getAssetDatabases(String assetServerWebId, String selectedFields) throws ApiException {

        if (client.getSimulationMode()) {
            var r = new JsonArray();
            var first = new JsonObject();
            first.addProperty("Name", "First AF DB");
            var second = new JsonObject();
            second.addProperty("Name", "Second AF DB");
            r.add(first);
            r.add(second);
            return r;
        }

        var url = String.format("assetservers/%s/assetdatabases",assetServerWebId);
        url = UrlUtils.addUrlParameter(url, "selectedFields", selectedFields);
        return client.doGet(url).getContent().getAsJsonObject().get("Items").getAsJsonArray();
    }

}
