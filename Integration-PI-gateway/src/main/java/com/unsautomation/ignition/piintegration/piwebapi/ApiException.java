package com.unsautomation.ignition.piintegration.piwebapi;

public class ApiException extends Exception{

    public ApiException(Exception e) {

    }

    public ApiException(String message, Exception e) {
        super(message, e);
    }

    public ApiException(String message) {
        super(message);
    }
}
