package com.unsautomation.ignition;

import com.inductiveautomation.ignition.common.BundleUtil;
import com.inductiveautomation.ignition.common.licensing.LicenseState;
import com.inductiveautomation.ignition.common.script.ScriptManager;
import com.inductiveautomation.ignition.gateway.model.AbstractGatewayModuleHook;
import com.inductiveautomation.ignition.gateway.model.GatewayContext;
import com.unsautomation.ignition.piintegration.PIHistoryProvider;
import com.unsautomation.ignition.piintegration.PIHistoryProviderSettings;
import com.unsautomation.ignition.piintegration.PIHistoryProviderType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptContext;
import java.sql.SQLException;

public class GatewayHook extends AbstractGatewayModuleHook {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private GatewayContext context;
    private PIHistoryProviderType piHistoryProviderType;

    @Override
    public void setup(GatewayContext gatewayContext) {
        this.context = gatewayContext;
        BundleUtil.get().addBundle(PIHistoryProvider.class);
        BundleUtil.get().addBundle(PIHistoryProviderSettings.class);

        try {
            context.getSchemaUpdater().updatePersistentRecords(PIHistoryProviderSettings.META);
        } catch (SQLException e) {
            e.printStackTrace();
            logger.error("Unable to verify schema",e);
        }

        piHistoryProviderType = new PIHistoryProviderType();

        // Add PI history provider type
        try {
            context.getTagHistoryManager().addTagHistoryProviderType(piHistoryProviderType);
        } catch (Exception ex) {
            logger.error("Error adding PI history provider type", ex);
        }
    }

    @Override
    public void startup(LicenseState licenseState) {
    }

    @Override
    public void initializeScriptManager(ScriptManager manager) {
        //TODO : Add Event Frame Functions
    }

    @Override
    public boolean isFreeModule() {
        return true;
    }
    @Override
    public void shutdown() {
        // Remove bundle resource
        BundleUtil.get().removeBundle(PIHistoryProvider.class);
        BundleUtil.get().removeBundle(PIHistoryProviderSettings.class);

        // Remove PI history provider type
        try {
            context.getTagHistoryManager().removeTagHistoryProviderType(piHistoryProviderType);
        } catch (Exception ex) {
            logger.error("Error shutting down PI history provider type", ex);
        }
    }
}
