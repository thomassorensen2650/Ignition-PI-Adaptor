package com.unsautomation.ignition.piintegration.piwebapi;

import com.inductiveautomation.ignition.common.gson.JsonElement;
import com.inductiveautomation.ignition.common.gson.JsonParser;
import com.unsautomation.ignition.piintegration.piwebapi.model.PIResponse;
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
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class ApiClient {
    private final CloseableHttpClient httpClient;
    private String username;
    private String password;
    private String baseUrl;
    private Boolean verifySSL;

    private final Logger logger = LoggerFactory.getLogger("PIWebApiClient");

    public ApiClient(String baseUrl, String username, String password, Boolean verifySsl) throws ApiException {
        this.verifySSL = verifySsl;

        this.baseUrl = baseUrl;
        if (!this.baseUrl.endsWith("/")) {
            this.baseUrl+= "/";
        }

        httpClient = getHttpClient();
        this.username = username;
        this.password = password;
    }

    public Boolean getSimulationMode() {return false;}
    /***
     *
     * @param relativeUrl
     * @param requests
     * @return
     * @throws ApiException
     */
     public PIResponse doPost(String relativeUrl, JsonElement requests) throws ApiException {
         var uri = URI.create(baseUrl  + relativeUrl);
         var request = new HttpPost(uri);
         logger.info("Posting: " + uri.toString());

         request.setHeader("Content-type", "application/json");
         try {
             request.setEntity(new StringEntity(requests.toString()));
         } catch (UnsupportedEncodingException e) {
             throw new ApiException(e);
         }

         if (username != "") {
            var auth = new UsernamePasswordCredentials(username, password);
            try {
                request.addHeader(new BasicScheme().authenticate(auth, request, null));
            } catch (AuthenticationException e) {
                throw new ApiException("Error Setting Authentication", e);
            }
        }

        try {
            var response = httpClient.execute(request);
            return new PIResponse(response);
        }catch (IOException ex) {
            throw new ApiException("Unable to POST", ex);
        }
    }

    /***
     *
     * @param toEncode
     * @return
     * @throws ApiException
     */
    public String urlEncode(String toEncode) throws ApiException {
        try {
            toEncode = URLEncoder.encode(toEncode, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            throw new ApiException("Error Encoding URL Parameters", e);
        }
        return toEncode;
    }

    /***
     *
     * @param relativeUrl
     * @return
     * @throws ApiException
     */
    public PIResponse doGet(String relativeUrl) throws ApiException {

        var uri = URI.create(baseUrl  + relativeUrl);
        var request = new HttpGet(uri);

        logger.info("Getting: " + uri.toString());
         //request.setHeader("Accept", "application/json");
        request.setHeader("Content-type", "application/json");

        if (username != "") {
            var auth = new UsernamePasswordCredentials(username, password);
            try {
                request.addHeader(new BasicScheme().authenticate(auth, request, null));
            } catch (AuthenticationException e) {
                e.printStackTrace();
                throw new ApiException("Error Setting Authentication", e);
            }
        }

        try {
            var response = httpClient.execute(request); //, HttpResponse.BodyHandlers.ofString());
            return new PIResponse(response);
        } catch (IOException ex) {
            throw new ApiException("Unknown Exception on doGet", ex);
        }
    }

    /* Create a HTTP Client
     * @return a configured HTTP Client
     */
    CloseableHttpClient getHttpClient() throws ApiException {
        HttpClientBuilder builder = HttpClients.custom();

        builder.setUserAgent("Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11");
        try {
            if (!verifySSL) {
                builder.setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE);
            }
            if (!verifySSL) {
                builder.setSSLContext(new SSLContextBuilder().loadTrustMaterial(null, (TrustStrategy) (arg0, arg1) -> true).build());
            }
        } catch (Exception e) {
           throw new ApiException("Unable to initialize Http Client", e);
        }
        return builder.build();
    }
}
