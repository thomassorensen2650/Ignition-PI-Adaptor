package com.unsautomation.ignition.piintegration.piwebapi.api;

import com.inductiveautomation.ignition.common.gson.JsonArray;
import com.unsautomation.ignition.piintegration.piwebapi.*;

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
         return client.getSimulator().getAFServers(4).getAsJsonArray();
        }
        var url = UrlUtils.addUrlParameter("assetservers", "selectedFields", selectedFields);
        return client.doGet(url).getContent().getAsJsonObject().get("Items").getAsJsonArray();
    }

    public JsonArray getAssetDatabases(String assetServerWebId, String selectedFields) throws ApiException {

        if (client.getSimulationMode()) {
           return client.getSimulator().getAFDatabase(5).getAsJsonArray();
        }

        var url = String.format("assetservers/%s/assetdatabases",assetServerWebId);
        url = UrlUtils.addUrlParameter(url, "selectedFields", selectedFields);
        return client.doGet(url).getContent().getAsJsonObject().get("Items").getAsJsonArray();
    }

}
