package com.unsautomation.ignition.piintegration.piwebapi.api;

import com.inductiveautomation.ignition.common.gson.JsonArray;
import com.inductiveautomation.ignition.common.gson.JsonObject;
import com.inductiveautomation.ignition.gateway.history.HistoricalTagValue;
import com.unsautomation.ignition.piintegration.PIHistoryProviderSettings;
import com.unsautomation.ignition.piintegration.piwebapi.ApiException;
import com.unsautomation.ignition.piintegration.piwebapi.PIWebApiClient;
import org.apache.http.client.HttpResponseException;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class CustomApi {
    private final HashMap<String, String> tagCache = new HashMap<>();
    //private final HashMap<String, String> dataserverCache = new HashMap<>();
    private final StreamApi stream;
    private final DataServerApi dataserver;
    private final SystemApi system;
    private final PointApi point;

    public CustomApi(PIWebApiClient client) {
        this.stream = client.getStream();
        this.dataserver = client.getDataServer();
        this.system = client.getSystem();
        this.point = client.getPoint();
    }

    /***
     * Ingest data into PI Historian
     * TODO: Refactor this to something more modern!M!!!!!!
     * @param records
     * @return a map of write errors
     * @throws IOException
     * @throws InterruptedException
     */
    public void ingestRecords(@NotNull List<HistoricalTagValue> records, String tagPrefix, String piArchiver) throws ApiException {

        // Create Write Request
        for (int i = 0; i < records.size(); i++) {
            var record = records.get(i);
            var tagName = record.getSource().toStringPartial();

            if (tagPrefix != null && tagPrefix != "") {
                tagName = tagPrefix + tagName;
            }
            var type = record.getTypeClass().isNumeric() ?
                    record.getTypeClass().getDataType().isFloatingPoint() ? "Float" : "Int32"  : "String";
            var point = new JsonObject();
            point.addProperty("Name", tagName);
            point.addProperty("PointType", type);
            var webId = getOrCreateTag(piArchiver, tagName, point);
            var value = new JsonObject();
            value.addProperty("value", (Number) record.getValue()); // FIXME: What about texts
            value.addProperty("timestamp", record.getTimestamp().getTime());
            stream.updateValue(webId, value);
        }
    }

    public String getOrCreateTag(String dataServer, String tagName, JsonObject point) throws ApiException {

        var path = "\\\\" + dataServer + "\\" + tagName;
        if (tagCache.containsKey(path)) {
            return tagCache.get(path);
        }
        
        String tagWebId = null;
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

    public boolean isAvailable() {
        try {
            return true;
            //return system.getStatus().get("State").getAsString() == "Running";
        }catch (Exception e){
            return false;
        }
    }

   /* public Float getDensity(String dataServer, String tagName, Date startTime, Date endTime) {

        //var archiver = dataserver.getByPath(dataServer);
        //var webId = archiver.get("WebId").getAsString();

       // stream.getGetSummary(webId, )
        return 0.5f;
    }*/
}
