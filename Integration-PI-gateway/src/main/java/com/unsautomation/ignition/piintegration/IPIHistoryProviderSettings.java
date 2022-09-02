package com.unsautomation.ignition.piintegration;

public interface IPIHistoryProviderSettings {
    String getWebAPIUrl();

    String getUsername();

    String getPassword();

    boolean getVerifySSL();

    String getBrowsableAFServers();

    String getBrowsablePIServers();

    boolean getOnlyBrowsePITagsWithPrefix();

    String getPITagPrefix();

    String getPIArchiver();
}
