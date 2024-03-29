package com.unsautomation.ignition.piintegration;

import com.inductiveautomation.ignition.common.gson.JsonObject;
import com.inductiveautomation.ignition.gateway.history.HistoricalTagValue;
import com.unsautomation.ignition.piintegration.piwebapi.ApiException;
import com.unsautomation.ignition.piintegration.piwebapi.PIWebApiClient;
import com.unsautomation.ignition.piintegration.piwebapi.api.DataServerApi;
import com.unsautomation.ignition.piintegration.piwebapi.api.PointApi;
import com.unsautomation.ignition.piintegration.piwebapi.api.StreamApi;
import org.jetbrains.annotations.NotNull;
import java.util.HashMap;
import java.util.List;

public class PIDataSinkBasic implements IPIDataSink {

    private final HashMap<String, String> tagCache = new HashMap<>();

    private final PointApi point;

    private final StreamApi stream;
    private final DataServerApi dataserver;

    public PIDataSinkBasic(PIWebApiClient client) {
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
            // TODO: What is someone deletes a tag in PI manually?
            // Do we need some sort of clear cache at fixed internal?
            return tagCache.get(path);
        }

        String tagWebId;
        try {
            var tag = this.point.getByPath(path);
            tagWebId = tag.getAsJsonObject().get("WebId").getAsString();
        } catch (ApiException ex) {
            if (ex.statusCode == 404) { // Tag not found;
                var x = new JsonObject();
                x.addProperty("Name", tagName);
                x.addProperty("PointType", "Float32");
                x.addProperty("PointClass", "Classic");

                var archiverWebId = this.dataserver.getByPath("\\\\" + dataServer).getAsJsonObject().get("WebId").getAsString();
                tagWebId = this.dataserver.createPoint(archiverWebId, x);
            } else {
                throw ex;
            }
        }
        tagCache.put(path, tagWebId);
        return tagWebId;
    }
}