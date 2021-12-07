package com.unsautomation.ignition.piintegration;

import com.inductiveautomation.ignition.gateway.localdb.persistence.*;
import com.inductiveautomation.ignition.gateway.sqltags.config.TagHistoryProviderRecord;
import simpleorm.dataset.SFieldFlags;

/**
 * Represents the settings required for the PI history provider.
 * A user will fill in all of these fields. The data is stored inside of
 * Ignition's internal sqlite database into the table specified below.
 */
public class PIHistoryProviderSettings extends PersistentRecord {
    public static final RecordMeta<PIHistoryProviderSettings> META = new RecordMeta<PIHistoryProviderSettings>(
            PIHistoryProviderSettings.class, "PIHistoryProviderSettings");

    public static final LongField ProfileId = new LongField(META, "ProfileId", SFieldFlags.SPRIMARY_KEY);
    public static final ReferenceField<TagHistoryProviderRecord> Profile =
            new ReferenceField<TagHistoryProviderRecord>(META, TagHistoryProviderRecord.META, "Profile", ProfileId);
    public static final StringField PIWebAPIUrl = new StringField(META, "PIWebAPIUrl", SFieldFlags.SMANDATORY).setDefault("https://localhost/piwebapi");
    //public static final BooleanField enableSecurity = new BooleanField(META, "enableSecurity", SFieldFlags.SMANDATORY).setDefault(true);

    //public static final StringField userName = new StringField(META, "userName", SFieldFlags.SMANDATORY);
    //public static final EncodedStringField password = new EncodedStringField(META, "password", SFieldFlags.SMANDATORY);


    //public static final StringField PIArchiver = new StringField(META, "PIArchiver", SFieldFlags.SMANDATORY);


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
        ProfileId.getFormMeta().setVisible(false);
        Profile.getFormMeta().setVisible(false);
    }

    @Override
    public RecordMeta<?> getMeta() {
        return META;
    }
}
