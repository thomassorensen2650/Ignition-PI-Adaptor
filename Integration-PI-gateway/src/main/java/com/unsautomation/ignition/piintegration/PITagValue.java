package com.unsautomation.ignition.piintegration;

import com.inductiveautomation.ignition.common.gson.JsonObject;
import com.inductiveautomation.ignition.common.model.values.QualityCode;

import java.time.Instant;

public class PITagValue {

    private JsonObject value;
    public PITagValue(JsonObject value) {
        this.value = value;
    }

    public QualityCode getQuality() {
        return QualityCode.Good; // TODO: Convert PI Quality to Ignition Quality
    }

    public float getAsFloat() {
        return value.getAsJsonObject().get("Value").getAsFloat();
    }

    public long getTimestamp() {
        var time = value.get("Timestamp").getAsString();
        var instant = Instant.parse(time);
        return instant.toEpochMilli();
    }
}
