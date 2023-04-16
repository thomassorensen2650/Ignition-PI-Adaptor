package com.unsautomation.ignition.piintegration.piwebapi.api;

import com.inductiveautomation.ignition.common.gson.JsonArray;
import com.inductiveautomation.ignition.common.gson.JsonObject;
import com.unsautomation.ignition.piintegration.piwebapi.ApiClient;
import com.unsautomation.ignition.piintegration.piwebapi.ApiException;
import com.unsautomation.ignition.piintegration.piwebapi.PIDataSimulator;
import com.unsautomation.ignition.piintegration.piwebapi.UrlUtils;
import org.jetbrains.annotations.NotNull;

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
            return client.getSimulator().getDataArchivers(5).getAsJsonArray();
        }
        var url = UrlUtils.addUrlParameter("dataservers", "selectedFields", selectedFields);
        return client.doGet(url).getContent().getAsJsonObject().get("Items").getAsJsonArray();
    }

    public JsonObject getByPath(String path) throws ApiException {

        if (client.getSimulationMode()) {
            return client.getSimulator().getDataArchivers(1);
        }
        var url = UrlUtils.addUrlParameter("dataservers", "path", path);
        return client.doGet(url).getContent().getAsJsonObject();
    }

    public JsonObject getPoints(String dataServerWebId, @NotNull String nameFilter, Integer startIndex, Integer maxCount, @NotNull String selectedFields) throws ApiException{

        if (client.getSimulationMode()) {
            var tagCount = startIndex == null || startIndex == 0 ? maxCount : maxCount -10;
            return client.getSimulator().getPoints(tagCount);
        }
        var url = String.format("dataservers/%s/points?", dataServerWebId);

        final var parameters = Map.of(
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
