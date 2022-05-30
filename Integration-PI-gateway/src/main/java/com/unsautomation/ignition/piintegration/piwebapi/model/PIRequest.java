package com.unsautomation.ignition.piintegration.piwebapi.model;


import com.inductiveautomation.ignition.common.gson.annotations.SerializedName;

import java.util.List;
import java.util.Map;

public class PIRequest {
    @SerializedName("Method")
    private String method = null;

    @SerializedName("Resource")
    private String resource = null;

    @SerializedName("Parameters")
    private List<String> parameters = null;

    @SerializedName("Headers")
    private Map<String, String> headers = null;

    @SerializedName("Content")
    private String content = null;

    @SerializedName("ParentIds")
    private List<String> parentIds = null;

    public PIRequest() {
    }

    public void setMethod(String method) { this.method = method;}

    public String getMethod() { return this.method;}

    public void setResource(String resource) { this.resource = resource;}

    public String getResource() { return this.resource;}

    public void setParameters(List<String> parameters) { this.parameters = parameters;}

    public List<String> getParameters() { return this.parameters;}

    public void setHeaders(Map<String, String> headers) { this.headers = headers;}

    public Map<String, String> getHeaders() { return this.headers;}

    public void setContent(String content) { this.content = content;}

    public String getContent() { return this.content;}

    public void setParentIds(List<String> parentIds) { this.parentIds = parentIds;}

    public List<String> getParentIds() { return this.parentIds;}
}
