package com.unsautomation.ignition.piintegration;

import com.inductiveautomation.ignition.common.gson.JsonObject;
import com.inductiveautomation.ignition.common.gson.JsonParser;
import com.inductiveautomation.ignition.common.gson.JsonSyntaxException;
import com.inductiveautomation.ignition.common.model.values.QualityCode;

import java.time.Instant;

public class PITagValue {

    private long timeStamp;
    private JsonObject value;
    public PITagValue(JsonObject value) {
        this.value = value;
        var time = value.get("Timestamp").getAsString();
        var instant = Instant.parse(time);
        timeStamp = instant.toEpochMilli();
    }

    public QualityCode getQuality() {
        return value.get("Good").getAsBoolean() ? QualityCode.Good : QualityCode.Bad;
    }

    public float getAsFloat() {
        return value.get("Value").getAsFloat();
    }

    public long getTimestamp() {
        return this.timeStamp;
    }

    public Object getValue() {
        return isSystemValue() ? null : value.get("Value").getAsFloat(); }

    public Boolean isSystemValue() {

        if (value.get("Good").getAsBoolean() == true) {
            return false;
        }

        // If the value is not good, then we need to investigate some more
        // TODO: Will it always be a system value when bad?
        try {
            (new JsonParser()).parse(value.get("Value").getAsString());
            return true;
        } catch (JsonSyntaxException e) {
            return false;
        }

    }
}
