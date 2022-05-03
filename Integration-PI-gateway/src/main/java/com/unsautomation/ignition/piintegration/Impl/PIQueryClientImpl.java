package com.unsautomation.ignition.piintegration.Impl;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.inductiveautomation.ignition.common.QualifiedPath;
import com.inductiveautomation.ignition.common.browsing.Result;
import com.inductiveautomation.ignition.common.browsing.Results;
import com.inductiveautomation.ignition.gateway.history.HistoricalTagValue;
import com.unsautomation.ignition.piintegration.PIHistoryProviderSettings;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.HttpGet;
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

    /***
     * Create a HTTP Client
     * @return a configured HTTP Client
     */
    CloseableHttpClient getHttpClient() {
        HttpClientBuilder builder = HttpClients.custom();

        builder.setUserAgent("Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11");
        try {
            if (settings.getVerifyCertificateHostname()) {
                builder.setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE);
            }
            if (settings.getVerifySSL()) {
                builder.setSSLContext(new SSLContextBuilder().loadTrustMaterial(null, (TrustStrategy) (arg0, arg1) -> true).build());
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

    public JsonArray queryAFServers() {
        var a = new JsonArray();
        var s1 = new JsonObject();
        s1.addProperty("name", "AF Server 1");
        a.add(s1);

        var s2 = new JsonObject();
        s2.addProperty("name", "AF Server 2");
        a.add(s2);

        return a;
    }
    private JsonArray queryAFDBs(String server) {
        return null;
    }

    private JsonArray queryAFPath(String server, String db, String path) {
        return null;
    }

    public JsonArray queryPath(String path) throws Exception {
        var result = new JsonArray();
        if (null == path) {  // Top level browse
            var a = new JsonObject();
            a.addProperty("name", "Assets");
            result.add(a);

            var p = new JsonObject();
            p.addProperty("name", "Points");
            result.add(p);
            return result;
        }

        var tagParts = path.split("/");
        var type = tagParts[0];
        var server = tagParts.length > 0 ? tagParts[1] : null;

        if (type == "Points") {

            if (null == server) {
                return queryAFServers();
            } else {
                var db = tagParts.length > 1 ? tagParts[2] : null;
                if (null == db) {
                    return queryAFDBs(server);
                } else {
                    //var dbPath = toString().join("/",tagParts.length > 1 ? Arrays.asList(tagParts).subList(2,tagParts.length-1)) : null;
                    var dbPath = "\\" + path.substring(6).replace("/", "\\");
                    return queryAFPath(server, db, dbPath);
                }

            }
        }else if (type == "Points") {

        } else {
            throw new Exception("Unknown Path :" + path);
        }


        return result;

    }

    public JsonArray queryPIServers() {
        var a = new JsonArray();
        var s1 = new JsonObject();
        s1.addProperty("name", "PI Server 1");
        a.add(s1);

        var s2 = new JsonObject();
        s2.addProperty("name", "PI Server 2");
        a.add(s2);

        return a;
    }

    public JsonArray queryAFPath(String path) {
        var a = new JsonArray();
        var s1 = new JsonObject();
        s1.addProperty("name", "DIMS");
        a.add(s1);
        return a;
    }

    public Results<Result> query(QualifiedPath path) throws IOException, URISyntaxException, InterruptedException {

       return null;
    }

    /***
     * Ingest data into PI Historian
     * @param records
     * @return a map of write errors
     * @throws IOException
     * @throws InterruptedException
     */
    public Map<String, List<PIBatchWriteValue>> ingestRecords(@NotNull List<HistoricalTagValue> records) throws IOException, InterruptedException {

        Map<String, List<PIBatchWriteValue>> errors;
        if (records.size() > 0) {

            var batchRequestManager = new PIWebAPIBatchWrite(settings.getWebAPIUrl(), settings.getPIArchiver());

            // Create Write Request
            logger.debug("Logging " + records.size() + " records using batch resource");
            for (int i = 0; i < records.size(); i++) {
                var record = records.get(i);
                var write = PIBatchWriteValue.fromHistoricalTagValue(record);
                batchRequestManager.addWrite(write);
            }
            var reqData = batchRequestManager.buildWriteRequest();
            var response = postBatch(batchUri, reqData);
            var result = batchRequestManager.analyseResponse(response);
            errors = result.getErrors(false);

            // Crete tags if they dont exist.
            if (result.hasTagNotFound()) {
                logger.info("Tags not found! Trying to create...");
                var creteTagsRequest = result.buildCreateAndWriteRequest();
                var createResponse = postBatch(batchUri, creteTagsRequest);
                var createResult = batchRequestManager.analyseResponse(createResponse);
                errors.putAll(createResult.getErrors(true));
            }
        }else {
            errors = new HashMap<>();
        }
        return errors;
    }

    /***
     *
     * @param uri
     * @param requests
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    private JsonElement postBatch(URI uri, JsonElement requests) throws IOException, InterruptedException {

        logger.debug("Posting " + requests.toString() + " to " + batchUri.toString());
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

    private JsonElement getJson(URI uri) throws IOException, InterruptedException {

        var request = new HttpGet(uri);
        //request.setHeader("Accept", "application/json");
        request.setHeader("Content-type", "application/json");

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
}
