package com.unsautomation.ignition.piintegration.piwebapi;

import org.apache.http.client.HttpResponseException;

public class ApiException extends Exception{

    public int statusCode = 0;

    public ApiException(Exception e) {

    }

    public ApiException(String message, HttpResponseException e) {
        super(message, e);
        statusCode = e.getStatusCode();
    }

    public ApiException(String message, Exception e) {
        super(message, e);
    }

    public ApiException(String message) {
        super(message);
    }

    public ApiException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }

    public int getStatusCode() { return statusCode; }
}
