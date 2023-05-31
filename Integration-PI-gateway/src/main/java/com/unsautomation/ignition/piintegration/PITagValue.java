package com.unsautomation.ignition.piintegration;

import com.inductiveautomation.ignition.common.gson.JsonObject;
import com.inductiveautomation.ignition.common.model.values.QualityCode;

import java.time.Instant;

public class PITagValue {

    private final long timeStamp;
    private final JsonObject value;
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
        return isSystemValue() ? null : value.get("Value"); }

    public Boolean isSystemValue() {

        if (value.get("Good").getAsBoolean()) {
            return false;
        }
        // It can still be a system value with good quality.
        // value for system quality is always JSON objects.
        // TODO: If user stores JSON in PI TAGS, this will not work.
        var v = value.get("Value").toString();
        return  v.startsWith("{") && v.endsWith("}");
    }
}
