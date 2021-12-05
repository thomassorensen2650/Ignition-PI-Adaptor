package com.unsautomation.ignition.piintegration;

import com.inductiveautomation.ignition.gateway.history.HistoricalTagValue;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.gson.*;
import java.io.IOException;
import java.util.List;

public class PIQueryClientImpl {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    void ingestRecords(@NotNull List<HistoricalTagValue> records) throws IOException {
        logger.info("PIWeb API INGRESS count:" + records.size());
        // Convert to JSON Object
        var gson = new Gson();
        String json = gson.toJson(new Object());  // ==> json is [1,2,3,4,5]

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
}
