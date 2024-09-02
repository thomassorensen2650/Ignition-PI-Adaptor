package com.unsautomation.ignition.piintegration;

import com.inductiveautomation.ignition.gateway.localdb.persistence.*;
import com.inductiveautomation.ignition.gateway.sqltags.config.TagHistoryProviderRecord;
import com.inductiveautomation.ignition.gateway.web.components.editors.PasswordEditorSource;
import simpleorm.dataset.SFieldFlags;
import simpleorm.dataset.SFieldMeta;

/**
 * Represents the settings required for the PI history provider.
 * A user will fill in all of these fields. The data is stored inside of
 * Ignition's internal sqlite database into the table specified below.
 */
public class PIHistoryProviderSettings extends com.inductiveautomation.ignition.gateway.localdb.persistence.PersistentRecord {
    public static final RecordMeta<PIHistoryProviderSettings> META = new RecordMeta<PIHistoryProviderSettings>(
            PIHistoryProviderSettings.class, "PIHistoryProviderSettings");

    public static final LongField ProfileId;
    public static final ReferenceField<TagHistoryProviderRecord> Profile;

    // Connection
    public static final StringField PIWebAPIUrl; // Url for the PI Web API
    public static final StringField PIServer; // PI Server where data will be stored
    public static final StringField Username;
    public static final EncodedStringField Password;

    //public static final BooleanField ignoreSLLIssues;
    public static final StringField PITagPrefix;

    // Retrieval
    public static final StringField BrowsablePIServers;
    public static final StringField BrowsableAFServers;
    public static final BooleanField OnlyBrowsePITagsWithPrefix;

    public static final BooleanField IgnoreSSLIssues;

    public static final IntField APIRequestPageSize;

    public static final BooleanField SimulationMode;

    static final Category Connection;
    static final Category Advanced;
    static final Category Storage;

    public String getWebAPIUrl() {
        var url = getString(PIWebAPIUrl);

        if (url.substring(url.length() - 1) == "/") {
            url = url.substring(0,url.length() - 1);
        }
        return url;
    }

    public String getPassword() {
        return getString(Password);
    }

    public String getPITagPrefix() {
        return getString(PITagPrefix);
    }

    public String getUsername() {
        return getString(Username);
    }

    public String getPIArchiver() {
        return getString(PIServer);
    }
    public boolean getVerifySSL() { return !getBoolean(IgnoreSSLIssues); }

    public boolean getOnlyBrowsePITagsWithPrefix() { return getBoolean(OnlyBrowsePITagsWithPrefix); }

    public String getBrowsableAFServers()  { return getString(BrowsableAFServers); }

    public String getBrowsablePIServers()  { return getString(BrowsablePIServers); }

    public Integer getAPIRequestPageSize() {
        return getInt(APIRequestPageSize);
    }
    public Integer getAPIMaxResponseLimit() { return 1000000; }

    public Boolean getSimulationMode() { return getBoolean(SimulationMode); }

    static {
        ProfileId = new LongField(META, "ProfileId", SFieldFlags.SPRIMARY_KEY);
        Profile  = new ReferenceField<TagHistoryProviderRecord>(META, TagHistoryProviderRecord.META, "Profile", ProfileId);
        ProfileId.getFormMeta().setVisible(false);
        Profile.getFormMeta().setVisible(false);

        PIWebAPIUrl  = new StringField(META, "piWebAPIUrl", SFieldFlags.SMANDATORY).setDefault("https://localhost/piwebapi");
        Username  = new StringField(META, "userName");
        Password  = new EncodedStringField(META, "password");
        SimulationMode = new BooleanField(META, "simulationMode");
        Password.getFormMeta().setEditorSource(PasswordEditorSource.getSharedInstance());


        PIServer = new StringField(META, "piServer");
        PITagPrefix = new StringField(META, "piTagPrefix", SFieldFlags.SMANDATORY).setDefault("Ignition");

        /**
         * How many results returned in each request
         */
        APIRequestPageSize = new IntField(META, "apiRequestPageSize", SFieldFlags.SMANDATORY).setDefault(500);

        BrowsablePIServers = new StringField(META, "browsablePIServers");
        BrowsableAFServers = new StringField(META, "browsableAFServers");
        OnlyBrowsePITagsWithPrefix = new BooleanField(META, "onlyBrowsePITagsWithPrefix");

        IgnoreSSLIssues = new BooleanField(META, "ignoreCertificateIssues").setDefault(false);


        Connection = (new Category("PIHistoryProviderSettings.Category.Connection", 1, false)).include(new SFieldMeta[]{PIWebAPIUrl, Username, Password});
        Storage = (new Category("PIHistoryProviderSettings.Category.Storage", 2, false)).include(new SFieldMeta[]{PIServer, PITagPrefix});
        Advanced = (new Category("PIHistoryProviderSettings.Category.Advanced", 3, true)).include(new SFieldMeta[]{BrowsablePIServers, BrowsableAFServers, OnlyBrowsePITagsWithPrefix, IgnoreSSLIssues, SimulationMode});
    }

    @Override
    public RecordMeta<?> getMeta() {
        return META;
    }
}
