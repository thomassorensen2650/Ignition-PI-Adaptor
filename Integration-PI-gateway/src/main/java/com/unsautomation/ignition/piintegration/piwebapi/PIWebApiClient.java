package com.unsautomation.ignition.piintegration.piwebapi;

import com.unsautomation.ignition.piintegration.piwebapi.api.*;

public class PIWebApiClient {

    public ApiClient apiClient = null;
    private String baseUrl = null;
    private Boolean cacheDisabled = null;

    public PIWebApiClient(String baseUrl, String username, String password, Boolean verifySsl, Boolean debug) throws ApiException {
        this.baseUrl = baseUrl;
        this.cacheDisabled = true;
        this.apiClient = new ApiClient(baseUrl, username, password, verifySsl);
    }

    public BatchApi getBatch() {
        return new BatchApi(apiClient);
    }

    public AssetServerApi getAssetServer() {
        return new AssetServerApi(apiClient);
    }

    public AssetDatabaseApi getAssetDatabase() {
        return new AssetDatabaseApi(apiClient);
    }

    public SearchApi getSearch() {
        return new SearchApi(apiClient);
    }

    public DataServerApi getDataServer() {
        return new DataServerApi(apiClient);
    }

    public ElementApi getElementApi() {
        return new ElementApi(apiClient);
    }

    public StreamApi getStream() {
        return new StreamApi(apiClient);
    }

    public SystemApi getSystem() {
        return new SystemApi(apiClient);
    }

    public PointApi getPoint() {return new PointApi(apiClient);}

    public CustomApi getCustom() { return new CustomApi(this);}
}
