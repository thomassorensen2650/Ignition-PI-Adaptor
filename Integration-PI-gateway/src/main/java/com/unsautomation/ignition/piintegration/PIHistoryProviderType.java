package com.unsautomation.ignition.piintegration;

import com.inductiveautomation.ignition.gateway.localdb.persistence.PersistentRecord;
import com.inductiveautomation.ignition.gateway.localdb.persistence.RecordMeta;
import com.inductiveautomation.ignition.gateway.model.GatewayContext;
import com.inductiveautomation.ignition.gateway.sqltags.config.TagHistoryProviderRecord;
import com.inductiveautomation.ignition.gateway.sqltags.config.TagHistoryProviderType;
import com.inductiveautomation.ignition.gateway.sqltags.history.TagHistoryProvider;

/**
 * Describes the PI history provider. Each provider has a unique id
 * (TYPE_ID), points to the settings record, and instantiates an instance of
 * the provider with the proper settings.
 */
public class PIHistoryProviderType extends TagHistoryProviderType {
    public static final String TYPE_ID = "PI";

    public PIHistoryProviderType() {
        super(TYPE_ID, "PIHistoryProvider.ProviderType.Name", "PIHistoryProvider.ProviderType.Desc");
    }

    @Override
    public RecordMeta<? extends PersistentRecord> getSettingsRecordType() {
        return PIHistoryProviderSettings.META;
    }

    /**
     * Creates a new instance of the PI history provider for storage and retrieval
     */
    @Override
    public TagHistoryProvider createHistoryProvider(TagHistoryProviderRecord profile, GatewayContext context)
            throws Exception {
        PIHistoryProviderSettings settings = findProfileSettingsRecord(context, profile);
        return new PIHistoryProvider(context, profile.getName(), settings);
    }

    @Override
    public boolean supportsStorage() {
        return true;
    }
}