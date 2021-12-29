package com.unsautomation.ignition.piintegration.Internal;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PIWebAPIBatchWrite {

    private final Map<String, Integer> tagIdMap;
    private Integer nextId = 0;
    private final String url;
    private final String piArchiver;
    private final Map<String, List<PIBatchWriteValue>> writes;
    private final Logger logger = LoggerFactory.getLogger(getClass());

    // Will be used to build request.
    //private JsonObject requests;

    //private PIWebAPIBatchWrite context;

    public PIWebAPIBatchWrite(String url, String piArchiver) {
        this.url = url;
        this.piArchiver = piArchiver;
        tagIdMap = new HashMap<>();
        writes = new HashMap<>();
        //writeMap = new HashMap<>();
    }

    /***
     * Add a write request to the
     * @param tag
     */
    public void addWrite(PIBatchWriteValue tag) {

        if (!writes.containsKey(tag.getTagname())) {
            writes.put(tag.getTagname(), new ArrayList<>());
        }
        writes.get(tag.getTagname()).add(tag);
    }

    public JsonElement buildWriteRequest() {
        return buildWriteRequest(writes, false);
    }

    public JsonElement buildCreateAndWriteRequest(Map<String, List<PIBatchWriteValue>> data) {
        return buildWriteRequest(data, true);
    }

    public batchResponseResult analyseResponse(JsonElement response) throws IOException {

        var r = new batchResponseResult(this, response);

        if (response != null && response.isJsonObject()) {
            // Loop though keys
            for (var entry : response.getAsJsonObject().entrySet()) {
                var value = entry.getValue().getAsJsonObject();
                var key = entry.getKey();
                var status = value.has("Status") ? value.get("Status").getAsInt() : 0;
                var content = value.has("Content") ? value.get("Content").getAsString() : null;

                if (key.startsWith("WriteTag") && status > 300) {
                    int id = Integer.parseInt(key.split("_")[1]);   //.pop(); // Get array index of tags that need to be created.

                    var tagName = getTagforId(id);

                    if (content != null && content.startsWith("Some JSON paths did not select any tokens: $.GetTagID_")) {
                        r.addTagNotFound(tagName, writes.get(tagName));
                    }else if (content != null){
                        logger.error(content.toString());
                        r.addError(tagName, writes.get(tagName));
                    } else {
                        r.addError(tagName, writes.get(tagName));
                    }
                }
            }
        } else {

             logger.error("Invalid response from PI WebAPI:" + response.toString());
        }
        return r;
    }

    JsonElement buildWriteRequest(Map<String,List<PIBatchWriteValue>> data, boolean create) {

        var requests =  new JsonObject();
        var gerArchiverUrl = String.format("%s/dataservers/?name=%s", url , piArchiver);
        var getArchiver = buildBatchItem(gerArchiverUrl, "GET", "", "", null);
        requests.add("GetArchiverID", getArchiver);

        // Create Map of <TagName, array<value>
        for (Map.Entry<String, List<PIBatchWriteValue>> w : data.entrySet()) {
            var tagName = w.getKey();
            var values = w.getValue();

            var writeId = nextId++;
            if (create) {
                var tagDetails = new JsonObject();
                tagDetails.addProperty("Name", tagName);
                tagDetails.addProperty("PointType", values.get(0).getDataType());
                tagDetails.addProperty("EngineeringUnits", "");
                tagDetails.addProperty("PointClass", "classic");

                var createTag = buildBatchItem("{0}/points/", "POST", "GetArchiverID", "$.GetArchiverID.Content.Links.Self", tagDetails);
                requests.add("t_" + writeId, createTag);
            } else {
                var getTagUrl = "{0}/points/?nameFilter=" + writeId;
                var getTag = buildBatchItem(getTagUrl, "GET", "GetArchiverID", "$.GetArchiverID.Content.Links.Self", null);
                requests.add("t_" + writeId, getTag);
            }
            tagIdMap.put(tagName, writeId);

            var tagWrites = new JsonArray();

            // For each write value
            for (var val : values) {
                var j = new JsonObject();
                j.addProperty("Value", val.getValue().toString());
                j.addProperty("Timestamp", val.getTimestamp().toString()); //FIXME : Is Epic or Zule the right way to send data
                //j.addProperty("Good", record.getQuality().isGood());
                tagWrites.add(j);
            }
            // Write Tag Request
            var writeTagUrl = "{0}?bufferOption=Buffer";
            var writeTag = buildBatchItem(writeTagUrl, "POST","t_" + writeId, "$.t_" + writeId +".Content.Items[0].Links.RecordedData", tagWrites);
            requests.add("w_" + writeId, writeTag);
            //writeMap.put(tagId, val);
        }
        return requests;
    }

    String getTagforId(Integer id) throws IOException {
        for (var t : tagIdMap.entrySet()) {
            if (t.getValue() == id) {
                return t.getKey();
            }
        }
        throw new IOException("Tag for id '" + id + "' not found");
    }

    private JsonObject buildBatchItem(String resource, String method, String parentId, String parameter, JsonElement content) {
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
            rtn.add("Content", content);
        }
        return rtn;
    }
}
