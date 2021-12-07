package com.unsautomation.ignition.piintegration;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
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
    private PIHistoryProviderSettings settings;

    void ingestRecords(@NotNull List<HistoricalTagValue> records) throws IOException, InterruptedException, URISyntaxException {

        logger.info("Ingest Size: '" + records.size() + "'");


            //JsonArray batchRequest = new JsonArray();
            //batchRequest.add(new JsonObject());

            // Create object graph
            JsonArray requests = new JsonArray();
            String gerArchiverUrl = String.format("%s/dataservers/?name=%s", "",""); //settings.getWebAPIUrl(), settings.getPIArchiver());
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
                JsonArray tagWrites = new JsonArray(); // TODO: Fixme
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

        logger.info("HTTP TO GO");

        try {
            HttpRequest request = HttpRequest
                    .newBuilder()
                    .uri(new URI("http://192.168.50.3:1880/test"))
                    .headers("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requests.toString()))
                    .build();


            HttpResponse<String> response = HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_1_1)
                    .build()
                    .send(request, HttpResponse.BodyHandlers.ofString());

        } catch (Exception e) {
            logger.error("LORT", e);
        }
        logger.info("HTTP DONE");


        // Publish

        // Check result


        // Create tags if required

       /* ByteArrayOutputStream bis = new ByteArrayOutputStream();
        GZIPOutputStream gzipOutputStream = new GZIPOutputStream(bis);
        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(gzipOutputStream);
        CsvWriter csvWriter = new CsvWriter(outputStreamWriter, new CsvWriterSettings());
        // Write as csv stream
        if (records.size() > 0) {
            // TODO how much data can one such batch have - maybe we should write straight to blob
            logger.debug("Logging " + records.size() + " records");
            for (AzureKustoTagValue record : records) {
                Object[] recordAsObjects = new Object[8];
                csvWriter.writeRow();
                if (record.getTag().getSystemName() != null) recordAsObjects[0] = record.getTag().getSystemName();
                if (record.getTag().getTagProvider() != null) recordAsObjects[1] = record.getTag().getTagProvider();
                if (record.getTag().getTagPath() != null) recordAsObjects[2] = record.getTag().getTagPath();
                Object value = record.getValue();
                if (value != null) {
                    ObjectMapper objectMapper = new ObjectMapper();
                    String valueAsJson = objectMapper.writeValueAsString(value);
                    recordAsObjects[3] = valueAsJson;

                    if (value instanceof Double || value instanceof Float) {
                        recordAsObjects[4] = value;
                    } else if (value instanceof Boolean || value instanceof Byte || value instanceof Short || value instanceof Integer || value instanceof Long) {
                        recordAsObjects[4] = value;
                        recordAsObjects[5] = value;
                    }
                }

                if (record.getTimestamp() != null) {
                    String formattedDate = simpleDateFormat.format(record.getTimestamp());
                    recordAsObjects[6] = formattedDate;
                }
                if (record.getQuality() != null) recordAsObjects[7] = record.getQuality();
                csvWriter.writeRow(recordAsObjects);
            }
        }
        csvWriter.flush();
        gzipOutputStream.finish();
        gzipOutputStream.close();
        StreamSourceInfo streamSourceInfo = new StreamSourceInfo(new ByteArrayInputStream(bis.toByteArray()), false);
        streamSourceInfo.setCompressionType(CompressionType.gz);
        gzipOutputStream.finish();
        gzipOutputStream.close();
        // Can change here to streaming
        queuedClient.ingestFromStream(streamSourceInfo, ingestionProperties);
        */
    }

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
