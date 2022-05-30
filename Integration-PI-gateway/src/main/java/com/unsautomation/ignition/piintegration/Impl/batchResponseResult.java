package com.unsautomation.ignition.piintegration.Impl;

import com.inductiveautomation.ignition.common.gson.JsonElement;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class batchResponseResult {
    private final Map<String, List<PIBatchWriteValue>> tagNotExist;
    private Map<String, List<PIBatchWriteValue>> errors;
    public JsonElement response;
    private final PIWebAPIBatchWrite context;

    public batchResponseResult(PIWebAPIBatchWrite context, JsonElement response) {
        this.tagNotExist = new HashMap<>();
        this.errors = new HashMap<>();
        this.response = response;
        this.context = context;
    }


    public Map<String, List<PIBatchWriteValue>> getErrors(boolean includeTagNotFound) {
        return errors;
    }

    void addTagNotFound(String tagName, List<PIBatchWriteValue> values) {
        tagNotExist.put(tagName, values);
    }

    void addError(String tagName, List<PIBatchWriteValue> values) {
        errors.put(tagName, values);
    }
    void setErrors(Map<String, List<PIBatchWriteValue>> errors) {
        this.errors = errors;
    }

    public Map<String, List<PIBatchWriteValue>> getNotFoundMap() {
        return tagNotExist;
    }

    public Boolean hasTagNotFound() {
        return tagNotExist.size() > 0;
    }

    public JsonElement buildCreateAndWriteRequest() {
        return context.buildWriteRequest(tagNotExist, true);
    }
}
