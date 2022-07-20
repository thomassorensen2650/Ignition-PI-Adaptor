package com.unsautomation.ignition.piintegration.piwebapi.api;

import com.inductiveautomation.ignition.common.gson.JsonArray;
import com.inductiveautomation.ignition.common.gson.JsonObject;
import com.unsautomation.ignition.piintegration.piwebapi.ApiClient;
import com.unsautomation.ignition.piintegration.piwebapi.ApiException;
import com.unsautomation.ignition.piintegration.piwebapi.model.PIResponse;
import org.apache.http.client.HttpResponseException;

import java.util.Date;
import java.util.Random;

public class StreamApi {

    private final ApiClient client;
    public StreamApi(ApiClient client) {
        this.client = client;
    }

    /**
     * Retrieves values over the specified time range suitable for plotting over the number of intervals (typically represents pixels).
     */
    public JsonArray getPlot(String webId, Date startTime, Date endTime, Long intervals, String desiredUnits, String selectedFields, String timeZone) throws ApiException, HttpResponseException {

        if (client.getSimulationMode()) {
            JsonObject obj = new JsonObject();
            var items = new JsonArray();

            var r = new Random();
            var start = startTime.getTime();
            var end = endTime.getTime();

            var diff = end - start;
            var valueOffset = diff / intervals;
            var dt = startTime.toInstant();
            for (int i = 0; i < intervals; i++) {
                var item = new JsonObject();
                item.addProperty("Timestamp", dt.toString());
                item.addProperty("Good", true);
                item.addProperty("Value", r.nextFloat()*100);
                dt = dt.plusMillis(valueOffset);
                items.add(item);
            }
            obj.add("Items", items);
            return items; //resp.getData();
        }

        var queryPath = "?startTime=" + startTime.toInstant().toString() + "&endTime=" + endTime.toInstant().toString()
                        + "&intervals=" + intervals;

        var url = "/streams/" + webId + "/plot" + queryPath;
        var response = client.doGet(url);
        var content = response.getContent();
        return content.getAsJsonObject().get("Items").getAsJsonArray();
    }

    /**
     * Query raw data
     * @param webId
     * @param startTime
     * @param endTime
     * @param desiredUnits
     * @param selectedFields
     * @param timeZone
     * @return
     * @throws ApiException
     */
    public JsonArray getRecorded(String webId, Date startTime, Date endTime, String desiredUnits,  String selectedFields, String timeZone) throws ApiException {

        if (client.getSimulationMode()) {
            JsonObject obj = new JsonObject();
            JsonArray items = new JsonArray();

            var r = new Random();
            var start = startTime.getTime();
            var end = endTime.getTime();

            var diff = end - start;
            var valueOffset = diff / 100;
            var dt = startTime.toInstant();
            for (int i = 0; i < 100; i++) {
                var item = new JsonObject();
                item.addProperty("Timestamp", dt.toString());
                item.addProperty("Good", true);
                item.addProperty("Value", r.nextFloat() * 100);
                dt = dt.plusMillis(valueOffset);
                items.add(item);
            }
            obj.add("Items", items);
            return items; //resp.getData();
        }
        var queryPath = "?startTime=" + startTime.toInstant().toString() + "&endTime=" + endTime.toInstant().toString();
        var url = "streams/" + webId + "/recorded" + queryPath;

        //return client.doGet("/streams/" + webId + "/recorded").getContent().getAsJsonObject();
        return client.doGet(url).getContent().getAsJsonObject().get("Items").getAsJsonArray();
    }

    public void updateValue(String webId, JsonObject value) {}


    public PIResponse getSummary(String webId, Date startTime, Date endTime, String summaryType, String calculationBasis) throws ApiException {
        var queryPath = "?startTime=" + startTime.toInstant().toString() + "&endTime=" + endTime.toInstant().toString() +
                "&summaryType=" + summaryType + "&calculationBasis=" + calculationBasis;
        queryPath = client.urlEncode(queryPath);

        if (client.getSimulationMode()) {
            return null; // TODO;
        }

        var url = "streams/" + webId + "/summary" + queryPath;
        return client.doGet(url);
    }
}
