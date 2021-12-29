package com.unsautomation.ignition.piintegration.Internal;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.inductiveautomation.ignition.common.QualifiedPath;
import com.inductiveautomation.ignition.common.browsing.Result;
import com.inductiveautomation.ignition.common.browsing.Results;
import com.inductiveautomation.ignition.gateway.history.HistoricalTagValue;
import com.unsautomation.ignition.piintegration.PIHistoryProviderSettings;
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
import java.util.*;

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
     * @return
     */
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

    public Results<Result> query(QualifiedPath path) {
        return null;
    }

    /***
     * Ingest data into PI Historian
     * @param records
     * @return
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
                var creteTagsRequest = result.buildWriteRequest();
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

        //logger.info("Posting " + requests.toString() + " to " + batchUri.toString());
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

}
