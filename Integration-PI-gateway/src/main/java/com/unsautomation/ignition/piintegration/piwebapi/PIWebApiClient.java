package com.unsautomation.ignition.piintegration.piwebapi;

import com.unsautomation.ignition.piintegration.piwebapi.api.*;

public class PIWebApiClient {

    public ApiClient apiClient;

    public PIWebApiClient(String baseUrl, String username, String password, Boolean verifySsl, Boolean debug, Boolean simulationMode) throws ApiException {
        Boolean cacheDisabled = true;
        this.apiClient = new ApiClient(baseUrl, username, password, verifySsl, simulationMode);
    }
    public HomeApi getHome() { return new HomeApi(apiClient);}

    public BatchApi getBatch() { return new BatchApi(apiClient); }

    public AssetServerApi getAssetServer() {
        return new AssetServerApi(apiClient);
    }

    public AssetDatabaseApi getAssetDatabase() {
        return new AssetDatabaseApi(apiClient);
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

    public PointApi getPoint() {return new PointApi(apiClient);}

    public AttributeApi getAttribute() {return new AttributeApi(apiClient);}
}
