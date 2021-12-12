package com.unsautomation.ignition.piintegration;

import com.inductiveautomation.ignition.gateway.localdb.persistence.*;
import com.inductiveautomation.ignition.gateway.sqltags.config.TagHistoryProviderRecord;
import simpleorm.dataset.SFieldFlags;
import simpleorm.dataset.SFieldMeta;

/**
 * Represents the settings required for the PI history provider.
 * A user will fill in all of these fields. The data is stored inside of
 * Ignition's internal sqlite database into the table specified below.
 */
public class PIHistoryProviderSettings extends PersistentRecord {
    public static final RecordMeta<PIHistoryProviderSettings> META = new RecordMeta<PIHistoryProviderSettings>(
            PIHistoryProviderSettings.class, "PIHistoryProviderSettings");

    public static final LongField ProfileId;
    public static final ReferenceField<TagHistoryProviderRecord> Profile;

    // Connection
    public static final StringField PIWebAPIUrl; // Url for the PI Web API
    public static final StringField PIServer; // PI Server where data will be stored
    public static final BooleanField useBasicAuthentication;
    public static final StringField Username;
    public static final EncodedStringField Password;

    //public static final BooleanField ignoreSLLIssues;
    public static final StringField PITagPrefix;

    // Retrieval
    public static final StringField BrowsablePIServers;
    public static final StringField BrowsableAFServers;
    public static final BooleanField OnlyBrowsePITagsWithPrefix;

    public static final BooleanField IgnoreSSLIssues;

    static final Category Connection;
    static final Category Advanced;
    static final Category Storage;



    public String getWebAPIUrl() {
        return getString(PIWebAPIUrl);
    }

    public boolean getEnableSecurity() {
        return false; //getBoolean(enableSecurity);
    }

    public String getPassword() {
        return ""; //getString(password);
    }

    public String getPIArchiver() {
        return "";//getString(PIArchiver);
    }


    static {
        ProfileId = new LongField(META, "ProfileId", SFieldFlags.SPRIMARY_KEY);
        Profile  = new ReferenceField<TagHistoryProviderRecord>(META, TagHistoryProviderRecord.META, "Profile", ProfileId);
        ProfileId.getFormMeta().setVisible(false);
        Profile.getFormMeta().setVisible(false);

        PIWebAPIUrl  = new StringField(META, "piWebAPIUrl", SFieldFlags.SMANDATORY).setDefault("https://localhost/piwebapi");
        useBasicAuthentication = new BooleanField(META, "basicAuthentication").setDefault(false);
        Username  = new StringField(META, "userName");
        Password  = new EncodedStringField(META, "password");

        PIServer = new StringField(META, "piServer");
        PITagPrefix = new StringField(META, "piTagPrefix", SFieldFlags.SMANDATORY).setDefault("Ignition");

        BrowsablePIServers = new StringField(META, "browsablePIServers");
        BrowsableAFServers = new StringField(META, "browsableAFServers");
        OnlyBrowsePITagsWithPrefix = new BooleanField(META, "onlyBrowsePITagsWithPrefix");

        IgnoreSSLIssues = new BooleanField(META, "ignoreCertificateIssues").setDefault(false);


        Connection = (new Category("PIHistoryProviderSettings.Category.Connection", 1, false)).include(new SFieldMeta[]{PIWebAPIUrl, useBasicAuthentication, Username, Password});
        Storage = (new Category("PIHistoryProviderSettings.Category.Storage", 2, false)).include(new SFieldMeta[]{PIServer, PITagPrefix});
        Advanced = (new Category("PIHistoryProviderSettings.Category.Advanced", 3, true)).include(new SFieldMeta[]{BrowsablePIServers, BrowsableAFServers, OnlyBrowsePITagsWithPrefix, IgnoreSSLIssues});
    }

    @Override
    public RecordMeta<?> getMeta() {
        return META;
    }
}
