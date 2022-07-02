package com.unsautomation.ignition.piintegration.piwebapi.api;

import com.inductiveautomation.ignition.common.gson.JsonArray;
import com.inductiveautomation.ignition.common.gson.JsonElement;
import com.inductiveautomation.ignition.common.gson.JsonObject;
import com.unsautomation.ignition.piintegration.piwebapi.ApiClient;
import com.unsautomation.ignition.piintegration.piwebapi.ApiException;
import org.apache.http.client.HttpResponseException;

public class DataServerApi {

    private final ApiClient client;
    public DataServerApi(ApiClient apiClient) {
        this.client = apiClient;
    }

    /**
     * Retrieve a list of all Asset Servers known to this service.
     *
     */
    public JsonArray list(String selectedFields) throws ApiException {
        var r = new JsonArray();
        var first = new JsonObject();
        first.addProperty("name","First Server");
        var second = new JsonObject();
        second.addProperty("name", "Second Server");
        r.add(first);
        r.add(second);
        return r;
    }

    public JsonObject getByPath(String path) throws ApiException {

        if (client.getSimulationMode()) {
            var first = new JsonObject();
            first.addProperty("webId","First Server");
            return first;
        }

        path = client.urlEncode(path);

        return client.doGet("dataservers?path=" + path).getContent().getAsJsonObject();


    }

    public JsonArray getPoints(String dataServerWebId, String nameFilter, Integer startIndex, Integer maxCount, String selectedFields) throws ApiException{

        if (client.getSimulationMode()) {
            var r = new JsonArray();
            for (int i = 0; i < 100; i++) {
                var first = new JsonObject();
                first.addProperty("name","Tag" + i);
                r.add(first);
            }
            return r;
        }
        var url = String.format("dataservers/%s/points", dataServerWebId);


        if (null != nameFilter) {
            url += "nameFilter=" + client.urlEncode(nameFilter) + "&";
        }
        if (null != startIndex) {
            url += "startIndex=" + startIndex + "&";
        }
        if (null != maxCount) {
            url += "maxCount=" + maxCount + "&";
        }
        return client.doGet(url).getContent().getAsJsonObject().get("Items").getAsJsonArray();
    }

    /**
     * dataservers/{webId}/points
     * @param tagDetails
     * @return The WebID of the created Point
     */
    public String createPoint(String dataServerWebId, JsonObject tagDetails) throws ApiException {
        var url = String.format("dataservers/%s/points", dataServerWebId);
        var response = client.doPost(url, tagDetails);
        var pointUrl = response.getHeaders().get("Location");

        var tagParts = pointUrl.split("/");
        return tagParts[tagParts.length-1];


    }
}
