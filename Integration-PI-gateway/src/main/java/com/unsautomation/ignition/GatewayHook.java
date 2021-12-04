package com.unsautomation.ignition;

import com.inductiveautomation.ignition.common.BundleUtil;
import com.inductiveautomation.ignition.common.licensing.LicenseState;
import com.inductiveautomation.ignition.gateway.model.AbstractGatewayModuleHook;
import com.inductiveautomation.ignition.gateway.model.GatewayContext;
import com.unsautomation.ignition.piintegration.PIHistoryProviderType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GatewayHook extends AbstractGatewayModuleHook {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private GatewayContext context;
    private PIHistoryProviderType piHistoryProviderType;

    @Override
    public void setup(GatewayContext gatewayContext) {
        this.context = gatewayContext;

        piHistoryProviderType = new PIHistoryProviderType();

        // Add bundle resource for localization
        BundleUtil.get().addBundle(PIHistoryProviderType.class);

        // Add PI history provider type
        try {
            context.getTagHistoryManager().addTagHistoryProviderType(piHistoryProviderType);
        } catch (Exception ex) {
            logger.error("Error adding Azure Kusto history provider type", ex);
        }
    }

    @Override
    public void startup(LicenseState licenseState) {

    }

    @Override
    public void shutdown() {
        // Remove bundle resource
        BundleUtil.get().removeBundle(PIHistoryProviderType.class);

        // Remove PI b history provider type
        try {
            context.getTagHistoryManager().removeTagHistoryProviderType(piHistoryProviderType);
        } catch (Exception ex) {
            logger.error("Error shutting down PI history provider type", ex);
        }
    }

}
