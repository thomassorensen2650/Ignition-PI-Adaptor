package com.unsautomation.ignition.piintegration.piwebapi;

import com.inductiveautomation.ignition.common.gson.JsonArray;
import com.inductiveautomation.ignition.common.gson.JsonObject;

import java.util.Date;
import java.util.Random;

public class PIDataSimulator {

    public JsonObject getTags(int count){
        return null;
    }

    public JsonObject getAFDatabase(int count) {
        var r = new JsonArray();
        var first = new JsonObject();
        first.addProperty("Name", "First AF DB");
        first.addProperty("WebId", "xxRDEncodedStuff");
        var second = new JsonObject();
        second.addProperty("Name", "Second AF DB");
        second.addProperty("WebId", "xxRDEncodedStuff123");

        r.add(first);
        r.add(second);
        return null;
    }

    public JsonObject getAFElements(int count) {
        return null;
    }

    public JsonObject getAFServers(int count) {
        var r = new JsonArray();
        var first = new JsonObject();
        first.addProperty("Name", "First AF Server");

        first.addProperty("WebId","xxRSENCODEDAFSERVER1");
        var second = new JsonObject();
        second.addProperty("Name", "Second AF Server");
        second.addProperty("WebId","xxRSENCODEDAFSERVER2");
        r.add(first);
        r.add(second);
        return null;
    }

    public JsonObject getDataArchivers(int count) {
        return null;
    }

    public JsonObject getElements(int count) {
        return null;
    }
    public JsonObject getAttributes(int count) {
        return null;
    }

    public JsonObject getPoints(int count) {
        return null;
    }

    public JsonArray getPlot(Date startTime, Date endTime, Long intervals) {
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
}
