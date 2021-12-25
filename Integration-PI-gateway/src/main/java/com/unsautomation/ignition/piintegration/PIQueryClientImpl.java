package com.unsautomation.ignition.piintegration;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.inductiveautomation.ignition.common.QualifiedPath;
import com.inductiveautomation.ignition.common.browsing.Result;
import com.inductiveautomation.ignition.common.browsing.Results;
import com.inductiveautomation.ignition.gateway.history.HistoricalTagValue;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PIQueryClientImpl {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final PIHistoryProviderSettings settings;
    private final CloseableHttpClient httpClient;
    private final URI batchUri;

    public PIQueryClientImpl(PIHistoryProviderSettings settings) throws URISyntaxException {
        this.settings = settings;
        batchUri = new URI(settings.getWebAPIUrl() + "/batch");
        httpClient = getHttpClient();
    }


    public CloseableHttpClient getHttpClient() {
        HttpClientBuilder builder = HttpClients.custom();

        builder.setUserAgent("Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11");
        try {
            if (settings.getVerifyCertificateHostname()) {
                builder.setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE);
            }
            if (settings.getVerifySSL()) {
                builder.setSSLContext(new SSLContextBuilder().loadTrustMaterial(null, new TrustStrategy()
                {
                    public boolean isTrusted(X509Certificate[] arg0, String arg1) throws CertificateException
                    {
                        return true;
                    }
                }).build());
            }
        } catch (KeyManagementException e) {
            logger.error("KeyManagementException in creating http client instance", e);
        } catch (NoSuchAlgorithmException e) {
            logger.error("NoSuchAlgorithmException in creating http client instance", e);
        } catch (KeyStoreException e) {
            logger.error("KeyStoreException in creating http client instance", e);
        }
        return builder.build();
    }

    Results<Result> query(QualifiedPath path) {
        return null;
    }

    List<HistoricalTagValue> ingestRecords(@NotNull List<HistoricalTagValue> records) throws IOException, InterruptedException {

        var errors = new ArrayList<HistoricalTagValue>();
        if (records.size() > 0) {

            // Create object graph
            var requests = new JsonObject();
            var gerArchiverUrl = String.format("%s/dataservers/?name=%s", settings.getWebAPIUrl(), settings.getPIArchiver());
            var getArchiver = buildBatchItem(gerArchiverUrl, "GET", "", "", null);
            requests.add("GetArchiverID", getArchiver);

            // Create Write Request
            logger.debug("Logging " + records.size() + " records");
            for (int i = 0; i < records.size(); i++) {
                HistoricalTagValue record = records.get(i);

                var batchItems = createWriteBatchItem(record, i, false);
                for (var e : batchItems.entrySet()) {
                    requests.add(e.getKey(), e.getValue());
                }
            }

            // Send Request
            var response = postBatch(batchUri, requests);

            //
            // Process Write Value result
            //
            var result = analyzeBatchResponse(response, records);
            errors.addAll(result.errors);

            if (result.tagNotExist.size() > 0) {

                var requests2 = new JsonObject();
                requests2.add("GetArchiverID", getArchiver);
                for (int i = 0; i < result.tagNotExist.size(); i++) {
                    var record = result.tagNotExist.get(i);
                    var batchItems = createWriteBatchItem(record,i, true);

                    for (var e : batchItems.entrySet()) {
                        requests2.add(e.getKey(), e.getValue());
                    }

                }
                response = postBatch(batchUri, requests2);
                var responses2 = analyzeBatchResponse(response, records);
                errors.addAll(responses2.tagNotExist); // We already tried to create tags, treat as normal errors
                errors.addAll(responses2.errors);
            }
        }
        return errors;
    }

    /***
     * Analyse the result from a PI Web API Batch request, and verify that all tag writes
     * @param response
     * @param records
     * @return
     */
    batchResponseResult analyzeBatchResponse(JsonElement response, List<HistoricalTagValue> records) {

        var r = new batchResponseResult(response);

        if (response != null && response.isJsonObject()) {
            // Loop though keys
            for (var entry : response.getAsJsonObject().entrySet()) {
                var value = entry.getValue().getAsJsonObject();
                var key = entry.getKey();
                var status = value.has("Status") ? value.get("Status").getAsInt() : 0;
                var content = value.has("Content") ? value.get("Content").getAsString() : null;

                if (key.startsWith("WriteValue") && status > 300) {
                    int id = Integer.parseInt(key.split("_")[1]);   //.pop(); // Get array index of tags that need to be created.
                    var record = records.get(id);

                    if (content != null && content.startsWith("Some JSON paths did not select any tokens: $.GetTagID_")) {
                        // Create tags
                        r.tagNotExist.add(record);
                    }else {
                        // Unknown Error
                        logger.error("Unable to Write Value");
                        r.errors.add(record);
                    }
                }
            }
        } else {
            r.errors.addAll(records); // Assume that all writes are bad
            logger.error("Invalid response from PI WebAPI, assuming all values are bad");
        }

        return r;
    }



    Map<String, JsonElement> createWriteBatchItem(HistoricalTagValue record, int i, boolean create) {
        var requests = new HashMap<String, JsonElement>();

        // Get Tag Request
        if (create) {

            var tagDetails = new JsonObject();
            tagDetails.addProperty("Name", record.getSource().toStringPartial());
            tagDetails.addProperty("PointType", record.getTypeClass().getDataType().toString());
            tagDetails.addProperty("EngineeringUnits", "");
            //tagDetails.addProperty("PointClass", "classic");

            // TODO:

            /*
            * "Headers": {
          "Cache-Control": "no-cache"
            }
            * */
            var createTag = buildBatchItem("{0}/points/", "POST", "GetArchiverID", "$.GetArchiverID.Content.Links.Self", tagDetails);
            requests.put("CreateTag_" + i, createTag);
        } else {
            var getTagUrl = "{0}/points/?nameFilter=" + record.getSource().toStringPartial();
            var getTag = buildBatchItem(getTagUrl, "GET", "GetArchiverID", "$.GetArchiverID.Content.Links.Self", null);
            requests.put("GetTag_" + i, getTag);
        }



        // Value
        var tagWrites = new JsonArray();
        var j = new JsonObject();
        j.addProperty("Value", record.getValue().toString());
        j.addProperty("Timestamp", record.getTimestamp().toInstant().toString()); //FIXME : Is Epic or Zule the right way to send data
        j.addProperty("Good", record.getQuality().isGood());
        tagWrites.add(j);

        // Write Tag Request
        var writeTagUrl = "{0}?bufferOption=Buffer";

        var writeTag = buildBatchItem(writeTagUrl, "POST","GetTag_" + i, "$.GetTag_"+ i +".Content.Items[0].Links.RecordedData", tagWrites);
        requests.put("WriteTag_" + i, writeTag);
        return requests;
    }

    /***
     *
     * @param uri
     * @param requests
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    private JsonElement postBatch(URI uri, JsonObject requests) throws IOException, InterruptedException {

        logger.info("Posting " + requests.toString() + " to " + batchUri.toString());
        var request = new HttpPost(uri);
        //request.setHeader("Accept", "application/json");
        request.setHeader("Content-type", "application/json");
        request.setEntity(new StringEntity(requests.toString()));

        if (settings.getUsername() != "") {
            var auth = new UsernamePasswordCredentials(settings.getUsername(), settings.getPassword());
            try {
                request.addHeader(new BasicScheme().authenticate(auth, request, null));
            } catch (AuthenticationException e) {
                logger.error("Error Setting Authentication, Trying without Authentication", e);
            }
        }

        var response = httpClient.execute(request); //, HttpResponse.BodyHandlers.ofString());
        try {
            var content = new BasicResponseHandler().handleResponse(response);
            return JsonParser.parseString(content);
        } catch (HttpResponseException ex) {
            logger.error("Invalid Response", ex);
            throw new IOException(ex.getMessage());
        }
    }

    /***
     *
     * @param resource
     * @param method
     * @param parentId
     * @param parameter
     * @return
     */
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

    class batchResponseResult {
        public final List<HistoricalTagValue> tagNotExist;
        public final List<HistoricalTagValue> errors;
        public JsonElement response;

        public batchResponseResult(JsonElement response) {
            this.tagNotExist = new ArrayList<>();
            this.errors = new ArrayList<>();
            this.response = response;
        }

       /* public batchResponseResult(JsonElement response, List<HistoricalTagValue> tagNotExist, List<HistoricalTagValue> error) {
            this.tagNotExist = tagNotExist;
            this.errors = error;
            this.response = response;
        }*/
    }
}
