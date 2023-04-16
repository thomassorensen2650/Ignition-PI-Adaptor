package com.unsautomation.ignition.piintegration.piwebapi;

import com.inductiveautomation.ignition.common.gson.JsonObject;
import com.inductiveautomation.ignition.gateway.history.HistoricalTagValue;
import com.unsautomation.ignition.piintegration.piwebapi.api.DataServerApi;
import com.unsautomation.ignition.piintegration.piwebapi.api.PointApi;
import com.unsautomation.ignition.piintegration.piwebapi.api.StreamApi;
import org.jetbrains.annotations.NotNull;
import java.util.HashMap;
import java.util.List;

public class PIDataProducerBasic implements IPIDataProducer {

    private final HashMap<String, String> tagCache = new HashMap<>();

    private final PointApi point;

    private final StreamApi stream;
    private final DataServerApi dataserver;

    public PIDataProducerBasic(PIWebApiClient client) {
        this.stream = client.getStream();
        this.dataserver = client.getDataServer();
        this.point = client.getPoint();
    }

    public void setupAFAutoCreate(Boolean enableAFAutoCreate, String piAFServer, String piAFDatabase, String piAFElementRoot) {
        // TODO
    }

    /***
     * Ingest data into PI Historian
     * Basic Implementation that uses the PI WebAPI REST Methods to Ingress data
     * @param records records to ingress to PI
     */
    public void ingestRecords(@NotNull List<HistoricalTagValue> records, String tagPrefix, String piArchiver) throws ApiException {

        // Create Write Request
        for (var record : records) {
            var tagName = record.getSource().toStringPartial();

            if (tagPrefix != null && !tagPrefix.equals("")) {
                tagName = tagPrefix + tagName;
            }
            var type = record.getTypeClass().isNumeric() ?
                    record.getTypeClass().getDataType().isFloatingPoint() ? "Float" : "Int32" : "String";
            var point = new JsonObject();
            point.addProperty("Name", tagName);
            point.addProperty("PointType", type);
            var webId = getOrCreateTag(piArchiver, tagName);
            var value = new JsonObject();
            value.addProperty("value", (Number) record.getValue()); // FIXME: What about texts
            value.addProperty("timestamp", record.getTimestamp().getTime());
            stream.updateValue(webId, value);
        }
    }

    public String getOrCreateTag(String dataServer, String tagName) throws ApiException {

        var path = "\\\\" + dataServer + "\\" + tagName;
        if (tagCache.containsKey(path)) {
            return tagCache.get(path);
        }

        String tagWebId;
        try {
            var tag = this.point.getByPath(path);
            tagWebId = tag.getAsJsonObject().get("WebId").getAsString();
        } catch (ApiException ex) {
            if (ex.statusCode == 404) { // Tag not found;
                var x = new JsonObject();
                var tagParts = path.substring(2).split("\\\\");
                var archiver = tagParts[0];
                var tag = tagParts[1];
                x.addProperty("Name", tag);
                x.addProperty("PointType", "Float32");
                x.addProperty("PointClass", "Classic");

                var archiverWebId = this.dataserver.getByPath("\\\\" + archiver).getAsJsonObject().get("WebId").getAsString();
                tagWebId = this.dataserver.createPoint(archiverWebId, x);
            } else {
                throw ex;
            }
        }
        tagCache.put(path, tagWebId);
        return tagWebId;
    }

}