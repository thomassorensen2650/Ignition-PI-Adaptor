package com.unsautomation.ignition.piintegration.piwebapi.api;

import com.inductiveautomation.ignition.common.gson.JsonArray;
import com.unsautomation.ignition.piintegration.piwebapi.*;
import org.bouncycastle.pqc.jcajce.provider.qtesla.SignatureSpi;

public class AssetServerApi {

    private final ApiClient client;

    public AssetServerApi(ApiClient client) {
        this.client = client;
    }
    /**
     * Retrieve a list of all Asset Servers known to this service.
     *
     */
    public PIResponse list(String selectedFields) throws ApiException {

        if (client.getSimulationMode()) {
         var data = client.getSimulator().getAFServers(4);
         return new PIResponse(200, data);
        }
        var url = UrlUtils.addUrlParameter("assetservers", "selectedFields", selectedFields);
        return client.doGet(url);
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
