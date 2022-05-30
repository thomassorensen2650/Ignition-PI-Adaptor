package com.unsautomation.ignition.piintegration.piwebapi;

import com.inductiveautomation.ignition.common.gson.GsonBuilder;
import com.inductiveautomation.ignition.common.gson.JsonArray;
import com.inductiveautomation.ignition.common.gson.JsonElement;
import com.inductiveautomation.ignition.common.gson.JsonObject;

import java.util.ArrayList;
import java.util.Map;

public class BatchRequestBuilder {

    private Map<String, ArrayList<JsonObject>> _writes;
    private JsonObject requests = new JsonObject();
    private String baseUrl;
    public BatchRequestBuilder(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    BatchRequestBuilder getPIArchiver(String id, String archiverName) {
        var gerArchiverUrl = String.format("%s/dataservers/?name=%s", baseUrl , archiverName);
        var getArchiver = buildBatchItem(gerArchiverUrl, "GET", "", "", null, false);
        requests.add("GetArchiverID", getArchiver);

        return this;
    }

    BatchRequestBuilder createTag(String id, String archiverId, String tagName, JsonObject tagData) {
        var tagDetails = new JsonObject();
        tagDetails.addProperty("Name", tagName);
        tagDetails.addProperty("PointType", "Int32"); //values.get(0).getDataType());
        tagDetails.addProperty("EngineeringUnits", "");
        tagDetails.addProperty("PointClass", "classic");

        var createTag = buildBatchItem("{0}/points/", "POST", archiverId, "$." + archiverId + ".Content.Links.Self", tagDetails, true);
        requests.add(id, createTag);
        return this;
    }

    BatchRequestBuilder getTag(String id, String archiverId, String tagName) {
        var getTagUrl = "{0}/points/?nameFilter=" + tagName;
        var getTag = buildBatchItem(getTagUrl, "GET", archiverId, "$." + archiverId + ".Content.Links.Self", null, true);
        requests.add(id, getTag);
        return this;
    }

    BatchRequestBuilder writeTagValue(String id, String tagId, JsonObject value) {
        if (!_writes.containsKey(tagId)) {
            _writes.put(tagId, new ArrayList<>());
        }
        _writes.get(tagId).add(value);
        return this;
    }

    BatchRequestBuilder customBatchItem(String resource, String method, String parentId, String parameter, JsonElement content, boolean disableCache) {
        buildBatchItem(resource, method, parentId, parameter, content, disableCache);
        return this;
    }

    JsonObject build() {
        return null;
    }

    // Private
    private JsonObject buildBatchItem(String resource, String method, String parentId, String parameter, JsonElement content, boolean disableCache) {
        var rtn = new JsonObject();
        rtn.addProperty("Resource", resource);
        rtn.addProperty("Method", method);

        if (parentId != null && parentId != "") {
            var a = new JsonArray();
            a.add(parentId);
            rtn.add("ParentIDs", a);
        }

        if (parameter != null && parameter != "") {
            var a = new JsonArray();
            a.add(parameter);
            rtn.add("Parameters", a);
        }

        if(content != null) {
            var gson = new GsonBuilder().create();
            var c = gson.toJson(content);
            rtn.addProperty("Content", c);
        }

        if (disableCache) {
            var x = new JsonObject();
            x.addProperty("Cache-Control", "no-cache");
            rtn.add("Headers", x);
        }
        return rtn;
    }
}
