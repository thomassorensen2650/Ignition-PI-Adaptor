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
import java.util.List;

public class PIQueryClientImpl {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final PIHistoryProviderSettings settings;
    private final HttpClient httpClient;

    public PIQueryClientImpl(PIHistoryProviderSettings settings) {
        this.settings = settings;
        httpClient  = HttpClient.newBuilder()
                        .version(HttpClient.Version.HTTP_1_1)
                        .build();
    }


    Results<Result> query(QualifiedPath path) {
        return null;
    }

    void ingestRecords(@NotNull List<HistoricalTagValue> records) throws IOException, InterruptedException, URISyntaxException {

        // Create object graph
        JsonArray requests = new JsonArray();
        String gerArchiverUrl = String.format("%s/dataservers/?name=%s", settings.getWebAPIUrl(), settings.getPIArchiver());
        JsonObject getArchiver = buildBatchItem("GetArchiverID", gerArchiverUrl, "GET", "", "");
        requests.add(getArchiver);

        if (records.size() > 0) {
            // TODO how much data can one such batch have - maybe we should write straight to blob
            logger.debug("Logging " + records.size() + " records");
            for (int i = 0; i < records.size(); i++) {
                HistoricalTagValue record = records.get(i);

                // Get Tag Request
                String getTagUrl = "{0}/points/?nameFilter=" + record.getSource().toStringPartial();
                JsonObject getTag = buildBatchItem("GetTag_" + i, getTagUrl, "GET", "GetArchiverID","$.GetArchiverID.Content.Links.Self");
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

                JsonObject writeTag = buildBatchItem("WriteTag_" + i, writeTagUrl, "POST","GetTag_" + i, "$.GetTagID_"+ i +".Content.Items[0].Links.RecordedData");
                writeTag.getAsJsonObject("WriteTag_" + i).add("Content", tagWrites);
                requests.add(writeTag);
            }
        }
        JsonElement response = postBatch(new URI("http://192.168.50.3:1880/test"), requests);
        if (response != null) {
            // TODO: Create  Tags
        }
    }

    /***
     *
     * @param uri
     * @param requests
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    private JsonElement postBatch(URI uri, JsonArray requests)  {
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
     *
     * @param name
     * @param resource
     * @param method
     * @param parentId
     * @param parameter
     * @return
     */
    private JsonObject buildBatchItem(String name, String resource, String method, String parentId, String parameter) {
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
}
