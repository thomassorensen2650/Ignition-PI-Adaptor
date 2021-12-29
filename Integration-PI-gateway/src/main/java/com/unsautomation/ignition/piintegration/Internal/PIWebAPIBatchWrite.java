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

    public PIWebAPIBatchWrite(String url, String piArchiver) {
        this.url = url;
        this.piArchiver = piArchiver;
        tagIdMap = new HashMap<>();
        writes = new HashMap<>();
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

    public batchResponseResult analyseResponse(JsonElement response) throws IOException {
        var r = new batchResponseResult(this, response);

        if (response != null && response.isJsonObject()) {
            // Loop though keys
            for (var entry : response.getAsJsonObject().entrySet()) {
                var value = entry.getValue().getAsJsonObject();
                var key = entry.getKey();
                var status = value.has("Status") ? value.get("Status").getAsInt() : 0;
                var content = value.has("Content") && value.get("Content").isJsonPrimitive() ? value.get("Content").getAsString() : null;

                if (key.startsWith("w_") && (status > 300 || status < 200)) {
                    int id = Integer.parseInt(key.split("_")[1]);

                    var tagName = getTagForId(id);

                    if (content != null && content.startsWith("Some JSON paths did not select any tokens")) {
                        r.addTagNotFound(tagName, writes.get(tagName));
                    }else if (content != null){
                        logger.error(content.toString());
                        r.addError(tagName, writes.get(tagName));
                    } else {
                        logger.debug("Unknown Error");
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
                logger.info("Creating tag" + tagName);
                var tagDetails = new JsonObject();
                tagDetails.addProperty("Name", tagName);
                tagDetails.addProperty("PointType", values.get(0).getDataType());
                tagDetails.addProperty("EngineeringUnits", "");
                tagDetails.addProperty("PointClass", "classic");

                var createTag = buildBatchItem("{0}/points/", "POST", "GetArchiverID", "$.GetArchiverID.Content.Links.Self", tagDetails);
                requests.add("t_" + writeId, createTag);
            } else {
                var getTagUrl = "{0}/points/?nameFilter=" + piArchiver;
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

            var param = create ? "$.t_${idx}.Headers.Location" :
                    "$.t_" + writeId +".Content.Items[0].Links.RecordedData";
            // Write Tag Request
            var writeTagUrl = "{0}?bufferOption=Buffer";
            var writeTag = buildBatchItem(writeTagUrl, "POST","t_" + writeId, param, tagWrites);
            requests.add("w_" + writeId, writeTag);
            //writeMap.put(tagId, val);
        }
        return requests;
    }

    String getTagForId(Integer id) throws IOException {
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
