package com.unsautomation.ignition.piintegration;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.inductiveautomation.ignition.common.QualifiedPath;
import com.inductiveautomation.ignition.common.browsing.Result;
import com.inductiveautomation.ignition.common.browsing.Results;
import com.inductiveautomation.ignition.gateway.history.HistoricalTagValue;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

public class PIQueryClientImpl {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final PIHistoryProviderSettings settings;
    private final HttpClient httpClient;
    private final URI batchUri;

    public PIQueryClientImpl(PIHistoryProviderSettings settings) throws URISyntaxException {
        this.settings = settings;
        batchUri = new URI("http://192.168.50.3:1880/test");
        httpClient  = HttpClient.newBuilder()
                        .version(HttpClient.Version.HTTP_1_1)
                        .build();
    }

    Results<Result> query(QualifiedPath path) {
        return null;
    }

    List<HistoricalTagValue> ingestRecords(@NotNull List<HistoricalTagValue> records) {

        // Create object graph
        JsonArray requests = new JsonArray();
        String gerArchiverUrl = String.format("%s/dataservers/?name=%s", settings.getWebAPIUrl(), settings.getPIArchiver());
        JsonObject getArchiver = buildBaseBatchItem("GetArchiverID", gerArchiverUrl, "GET", "", "");
        requests.add(getArchiver);
        List<HistoricalTagValue> errors = new ArrayList<>();

        if (records.size() > 0) {
            //
            // Write Tags
            //
            logger.debug("Logging " + records.size() + " records");
            for (int i = 0; i < records.size(); i++) {
                HistoricalTagValue record = records.get(i);
                JsonArray batchItems = buildWriteBatchItem(record, i, false);
                requests.addAll(batchItems);
            }
            JsonElement response = postBatchRequest(batchUri, requests);
            batchResponseResult responses = analyzeBatchResponse(response, records);

            //
            // Process Result and Create missing tags (if any)
            //

            errors.addAll(responses.error);
            if (responses.tagNotExist.size() > 0) {
                requests = new JsonArray();
                for (int i = 0; i < responses.tagNotExist.size(); i++) {
                    HistoricalTagValue record = responses.tagNotExist.get(i);
                    JsonArray batchItems = buildWriteBatchItem(record,i, true);
                    requests.addAll(batchItems);
                }
                response = postBatchRequest(batchUri, requests);
                batchResponseResult responses2 = analyzeBatchResponse(response, records);
                errors.addAll(responses2.error); // We already tried to create tags, treat as normal errors
                errors.addAll(responses2.tagNotExist);
            }
        }
        return errors;
    }

    /**
     * ablyse the response from PI WebAPI.
     * @param response
     * @param records
     * @return
     */
    batchResponseResult analyzeBatchResponse(JsonElement response, List<HistoricalTagValue> records) {
        batchResponseResult result = new batchResponseResult();

        if (response != null && response.isJsonArray()) {
            // Loop though keys
            for (Map.Entry<String, JsonElement> entry : response.getAsJsonObject().entrySet()) {
                JsonObject value = entry.getValue().getAsJsonObject();
                String key = entry.getKey();

                int status = value.has("Status") ? value.get("Status").getAsInt() : 900;
                String content = value.has("Content") ? value.get("Content").getAsString() : null;

                if (key.startsWith("WriteValue") && status > 300) {
                    int id = Integer.parseInt(key.split("_")[1]);   //.pop(); // Get array index of tags that need to be created.
                    HistoricalTagValue record = records.get(id);

                    if (content != null && content.startsWith("Some JSON paths did not select any tokens: $.GetTagID_")) {
                        // Create tags
                        result.tagNotExist.add(record);
                    }else {
                        // Unknown Error
                        logger.error("Unable to Write Value");
                        result.error.add(record);
                    }
                }
            }
        } else {
            result.error.addAll(records); // Assume that all writes are bad
            logger.error("Invalid response from PI WebAPI, assuming all values are bad");
        }
        return result;
    }

    /***
     *
     * @param name
     * @param resource
     * @param method
     * @param parentId
     * @param parameter
     * @return
     */
    private JsonObject buildBaseBatchItem(String name, String resource, String method, String parentId, String parameter) {
        JsonObject rtn = new JsonObject();
        JsonObject inner = new JsonObject();
        inner.addProperty("Resource", resource);
        inner.addProperty("Method", method);

        if (parentId != null && parentId != "") {
            JsonArray a = new JsonArray();
            a.add(parentId);
            inner.add("ParentIDs", a);
        }

        if (parameter != null && parameter != "") {
            JsonArray a = new JsonArray();
            a.add(parameter);
            inner.add("Parameters", a);
        }
        rtn.add(name, inner);
        return rtn;
    }

    /**
     * Create a Request to Find or create tag and then write a value to the tag.
     * @param record the tag to write to
     * @param i the ID that the write will be given in the request
     * @param create true if it should create the tag
     * @return the JSON object
     */
    JsonArray buildWriteBatchItem(HistoricalTagValue record, int i, boolean create) {
        JsonArray requests = new JsonArray();

        // Get Tag Request
        String getTagUrl = "{0}/points/?nameFilter=" + record.getSource().toStringPartial();
        JsonObject getTag = buildBaseBatchItem("GetTag_" + i, getTagUrl, "GET", "GetArchiverID","$.GetArchiverID.Content.Links.Self");
        requests.add(getTag);

        // Value
        JsonArray tagWrites = new JsonArray();
        JsonObject j = new JsonObject();
        j.addProperty("Value", record.getValue().toString());
        j.addProperty("Timestamp", record.getTimestamp().toInstant().toString()); //FIXME : Is Epic or Zule the right way to send data
        j.addProperty("Good", record.getQuality().isGood());
        tagWrites.add(j);

        // Write Tag Request
        String writeTagUrl = "{0}?bufferOption=Buffer";

        JsonObject writeTag = buildBaseBatchItem("WriteTag_" + i, writeTagUrl, "POST","GetTag_" + i, "$.GetTagID_"+ i +".Content.Items[0].Links.RecordedData");
        writeTag.getAsJsonObject("WriteTag_" + i).add("Content", tagWrites);
        requests.add(writeTag);
        return requests;
    }

    /***
     * crete the basic structure that is required in order to write data to PI via the PI WebAPI Batch Controller.
     * @param uri
     * @param requests
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    private JsonElement postBatchRequest(URI uri, JsonArray requests)  {
        try {
            HttpRequest request = HttpRequest
                    .newBuilder()
                    .uri(uri)
                    .headers("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requests.toString()))
                    .build();

            HttpResponse<String> response = httpClient
                    .send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() < 300 && response.statusCode() >= 200) {
                // Success
                return JsonParser.parseString(response.body());
            }
            logger.error("Invalid Response Code '" + response.statusCode() +  "' Error: " + response.body());
            return null;

        } catch (Exception e) {
            logger.error("Unable to send HTTP Request", e);
            return null;
        }
    }

    /***
     * Represent the result from a PI WebAPI Batch Request
     */
    class batchResponseResult {
        public final List<HistoricalTagValue> tagNotExist;
        public final List<HistoricalTagValue> error;
        public JsonObject result;

        public batchResponseResult() {
            tagNotExist = new ArrayList<>();
            error = new ArrayList<>();
            result = null;
        }
    }
}
