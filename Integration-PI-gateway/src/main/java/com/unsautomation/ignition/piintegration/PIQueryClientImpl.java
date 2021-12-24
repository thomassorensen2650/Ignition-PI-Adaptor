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
import java.util.List;

public class PIQueryClientImpl {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final PIHistoryProviderSettings settings;
    private final CloseableHttpClient httpClient;
    private final URI batchUri;

    public PIQueryClientImpl(PIHistoryProviderSettings settings) throws URISyntaxException {
        this.settings = settings;
        batchUri = new URI(settings.getWebAPIUrl());
        httpClient = getHttpClient();
    }


    public CloseableHttpClient getHttpClient() {
        HttpClientBuilder builder = HttpClients.custom();
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
            var requests = new JsonArray();
            var gerArchiverUrl = String.format("%s/dataservers/?name=%s", settings.getWebAPIUrl(), settings.getPIArchiver());
            var getArchiver = buildBatchItem("GetArchiverID", gerArchiverUrl, "GET", "", "");
            requests.add(getArchiver);

            // Create Write Request
            logger.debug("Logging " + records.size() + " records");
            for (int i = 0; i < records.size(); i++) {
                HistoricalTagValue record = records.get(i);
                JsonArray batchItems = createWriteBatchItem(record, i, false);
                requests.addAll(batchItems);
            }

            // Send Request
            JsonElement response = postBatch(batchUri, requests);

            //
            // Process Write Value result
            //
            var result = analyzeBatchResponse(response, records);
            errors.addAll(result.errors);

            if (result.tagNotExist.size() > 0) {

                var requests2 = new JsonArray();
                requests2.add(getArchiver);
                for (int i = 0; i < result.tagNotExist.size(); i++) {
                    var record = result.tagNotExist.get(i);
                    var batchItems = createWriteBatchItem(record,i, true);
                    requests2.addAll(batchItems);
                }
                response = postBatch(batchUri, requests);
                var responses2 = analyzeBatchResponse(response, records);
                errors.addAll(responses2.tagNotExist); // We already tried to create tags, treat as normal errors
                errors.addAll(responses2.errors);
            }
        }
        return errors;
    }

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

    JsonArray createWriteBatchItem(HistoricalTagValue record, int i, boolean create) {
        JsonArray requests = new JsonArray();

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
    private JsonElement postBatch(URI uri, JsonArray requests) throws IOException, InterruptedException {
        //try {

        //var provider = new BasicCredentialsProvider();
        //var credentials = new UsernamePasswordCredentials(settings.getUsername(), settings.getPassword());
       // provider.setCredentials(AuthScope.ANY, credentials);
        var creds = new UsernamePasswordCredentials("John", "pass");

        HttpPost request = new HttpPost(uri);
        request.setHeader("Accept", "application/json");
        request.setHeader("Content-type", "application/json");
        request.setEntity(new StringEntity(requests.toString()));
        try {
            request.addHeader(new BasicScheme().authenticate(creds, request, null));
        } catch (AuthenticationException e) {
            logger.error("Authentication Error", e);
        }

        var response = httpClient.execute(request); //, HttpResponse.BodyHandlers.ofString());
        var statusCode = response.getStatusLine().getStatusCode();
        if (statusCode < 300 || statusCode >= 200) {
            // Success
            return JsonParser.parseString(response.body());
        }

        var content = new BasicResponseHandler().handleResponse(response);

        logger.error("Invalid Response Code '" + response.statusCode() +  "' Error: " + response.body());
        throw new IOException("Invalid Response fromn Web Service");
        // TODO Remove, exception will be thrown if not good response
        //logger.error("Invalid Response Code '" + response.statusCode() +  "' Error: " + response.body());
        //return null;

 //       } catch (Exception e) {
  //          logger.error("Unable to send HTTP Request", e);
      //      return null;
    //    }
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

    class batchResponseResult {
        public final List<HistoricalTagValue> tagNotExist;
        public final List<HistoricalTagValue> errors;
        public JsonElement response;

        public batchResponseResult(JsonElement response) {
            this.tagNotExist = new ArrayList<>();
            this.errors = new ArrayList<>();
            this.response = response;
        }

        public batchResponseResult(JsonElement response, List<HistoricalTagValue> tagNotExist, List<HistoricalTagValue> error) {
            this.tagNotExist = tagNotExist;
            this.errors = error;
            this.response = response;
        }
    }
}
