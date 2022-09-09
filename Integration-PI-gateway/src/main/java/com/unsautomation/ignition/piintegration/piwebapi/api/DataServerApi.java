package com.unsautomation.ignition.piintegration.piwebapi.api;

import com.inductiveautomation.ignition.common.gson.JsonArray;
import com.inductiveautomation.ignition.common.gson.JsonElement;
import com.inductiveautomation.ignition.common.gson.JsonObject;
import com.unsautomation.ignition.piintegration.piwebapi.ApiClient;
import com.unsautomation.ignition.piintegration.piwebapi.ApiException;
import com.unsautomation.ignition.piintegration.piwebapi.UrlUtils;
import org.apache.http.client.HttpResponseException;

import java.util.Map;

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
        if (client.getSimulationMode()) {
            var r = new JsonArray();
            var first = new JsonObject();
            first.addProperty("Name","First Server");
            first.addProperty("WebId","xxDSENCODEDSERVER1");
            var second = new JsonObject();
            second.addProperty("Name", "Second Server");
            second.addProperty("WebId","xxDSENCODEDSERVER2");
            r.add(first);
            r.add(second);
            return r;
        }
        var url = UrlUtils.addUrlParameter("dataservers", "selectedFields", selectedFields);
        return client.doGet(url).getContent().getAsJsonObject().get("Items").getAsJsonArray();
    }

    public JsonObject getByPath(String path) throws ApiException {

        if (client.getSimulationMode()) {
            var first = new JsonObject();
            first.addProperty("webId","First Server");
            return first;
        }
        var url = UrlUtils.addUrlParameter("dataservers", "path", path);
        return client.doGet(url).getContent().getAsJsonObject();
    }

    public JsonObject getPoints(String dataServerWebId, String nameFilter, Integer startIndex, Integer maxCount, String selectedFields) throws ApiException{

        if (client.getSimulationMode()) {
            var obj = new JsonObject();
            var r = new JsonArray();

            // Simulate two data pages.....
            var tagCount = startIndex == null || startIndex == 0 ? maxCount : maxCount -10;
            for (int i = 0; i < tagCount; i++) {
                var first = new JsonObject();
                first.addProperty("Name","Tag" + (i + startIndex + 1));
                first.addProperty("WebId","xxDPWEBIDXXXTag" + (i + startIndex + 1));

                r.add(first);
            }
            obj.add("Items", r);
            return obj;
        }
        var url = String.format("dataservers/%s/points?", dataServerWebId);

        var parameters = Map.of(
                "nameFilter",nameFilter,
                "startIndex", startIndex,
                "maxCount", maxCount,
                "selectedFields",selectedFields);
        url = UrlUtils.addUrlParameters(url, parameters);
        return client.doGet(url).getContent().getAsJsonObject();
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
