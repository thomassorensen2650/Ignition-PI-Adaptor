package com.unsautomation.ignition.piintegration.piwebapi;


import com.inductiveautomation.ignition.common.gson.JsonElement;
import com.inductiveautomation.ignition.common.gson.JsonObject;
import com.inductiveautomation.ignition.common.gson.JsonParser;
import com.inductiveautomation.ignition.common.gson.annotations.SerializedName;
import com.unsautomation.ignition.piintegration.piwebapi.ApiException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.util.EntityUtils;

import java.io.EOFException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public class PIResponse {
    @SerializedName("Status")
    private Integer status;

    @SerializedName("Headers")
    private Map<String, String> headers;

    @SerializedName("Content")
    private JsonObject content;

    public PIResponse(CloseableHttpResponse response) throws ApiException, IOException {

        var c = "";
        try {
            c = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8.name());
        } catch (EOFException ex) {
            content = null; // No respose body (common on POST requests)
        }
        status = response.getStatusLine().getStatusCode();
        headers = Arrays.stream(response.getAllHeaders()).collect(Collectors
                .toMap(k-> k.getName(), v -> v.getValue()));

        if (status > 299) {
            throw new ApiException(status, c);
        }
        content = (new JsonParser()).parse(c).getAsJsonObject();
    }

    public PIResponse(Integer status, JsonObject content) {
        setStatus(status);
        this.content = content;
    }
    private void setStatus(Integer status) { this.status = status;}

    public Integer getStatus() { return this.status;}

    private void setHeaders(Map<String, String> headers) { this.headers = headers;}

    public Map<String, String> getHeaders() { return this.headers;}


    public JsonObject getContent() { return this.content;}
}
