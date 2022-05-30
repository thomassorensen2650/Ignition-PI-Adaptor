package com.unsautomation.ignition.piintegration.piwebapi.model;


import com.inductiveautomation.ignition.common.gson.annotations.SerializedName;

import java.util.Map;

public class PIResponse {
    @SerializedName("Status")
    private Integer status = null;

    @SerializedName("Headers")
    private Map<String, String> headers = null;

    @SerializedName("Content")
    private Object content = null;

    public PIResponse() {
    }


    public void setStatus(Integer status) { this.status = status;}

    public Integer getStatus() { return this.status;}

    public void setHeaders(Map<String, String> headers) { this.headers = headers;}

    public Map<String, String> getHeaders() { return this.headers;}

    public void setContent(Object content) { this.content = content;}

    public Object getContent() { return this.content;}
}
