package com.unsautomation.ignition.piintegration.piwebapi.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.unsautomation.ignition.piintegration.piwebapi.ApiClient;
import com.unsautomation.ignition.piintegration.piwebapi.ApiException;

import java.util.Date;
import java.util.Random;

public class StreamApi {

    public StreamApi(ApiClient client) {

    }

    /**
     * Retrieves values over the specified time range suitable for plotting over the number of intervals (typically represents pixels).
     */
    public JsonObject getPlot(String webId, Date startTime, Date endTime, Integer intervals, String desiredUnits,  String selectedFields, String timeZone) throws ApiException {
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
            item.addProperty("Value", r.nextFloat()*100);
            dt = dt.plusMillis(valueOffset);
            items.add(item);
        }
        obj.add("Items", items);
        return obj; //resp.getData();
    }
}
