package com.unsautomation.ignition.piintegration.piwebapi;

import com.inductiveautomation.ignition.gateway.history.HistoricalTagValue;
import com.unsautomation.ignition.piintegration.Impl.PIBatchWriteValue;
import com.unsautomation.ignition.piintegration.Impl.PIWebAPIBatchWrite;
import com.unsautomation.ignition.piintegration.PIHistoryProviderSettings;
import com.unsautomation.ignition.piintegration.piwebapi.api.*;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PIWebApiClient {

    private ApiClient apiClient = null;
    private String baseUrl = null;
    private Boolean cacheDisabled = null;

    public PIWebApiClient(String baseUrl, String username, String password, Boolean verifySsl, Boolean debug) throws ApiException {
        this.baseUrl = baseUrl;
        this.cacheDisabled = true;
        this.apiClient = new ApiClient(baseUrl, username, password, verifySsl);
    }

    public BatchApi getBatch()
    {
        return new BatchApi(apiClient);
    }
    public AssetServerApi getAssetServer() {
        return new AssetServerApi(apiClient);
    }

    public SearchApi getSearch() {
        return new SearchApi(apiClient);
    }

    public DataServerApi getDataServer() {
        return new DataServerApi(apiClient);
    }

    public StreamApi getStream() {
        return new StreamApi(apiClient);
    }

    /***
     * Ingest data into PI Historian
     * TODO: Refactor this to something more modern!M!!!!!!
     * @param records
     * @return a map of write errors
     * @throws IOException
     * @throws InterruptedException
     */
    public Map<String, List<PIBatchWriteValue>> ingestRecords(PIHistoryProviderSettings settings,  @NotNull List<HistoricalTagValue> records) throws ApiException, IOException {

        Map<String, List<PIBatchWriteValue>> errors;
        if (records.size() > 0) {

            var batchRequestManager = new PIWebAPIBatchWrite(settings.getWebAPIUrl(), settings.getPIArchiver());

            // Create Write Request
            for (int i = 0; i < records.size(); i++) {
                var record = records.get(i);
                var write = PIBatchWriteValue.fromHistoricalTagValue(record);
                batchRequestManager.addWrite(write);
            }
            var reqData = batchRequestManager.buildWriteRequest();
            var response =  apiClient.postBatch("batch", reqData);
            var result = batchRequestManager.analyseResponse(response);
            errors = result.getErrors(false);

            // Crete tags if they dont exist.
            if (result.hasTagNotFound()) {

                var creteTagsRequest = result.buildCreateAndWriteRequest();
                var createResponse = apiClient.postBatch(settings.getWebAPIUrl(), creteTagsRequest);
                var createResult = batchRequestManager.analyseResponse(createResponse);
                errors.putAll(createResult.getErrors(true));
            }
        }else {
            errors = new HashMap<>();
        }
        return errors;
    }
}
