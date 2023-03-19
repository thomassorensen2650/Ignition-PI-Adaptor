package com.unsautomation.ignition.piintegration;

import com.inductiveautomation.ignition.common.StatMetric;
import com.inductiveautomation.ignition.common.i18n.LocalizedString;
import com.inductiveautomation.ignition.gateway.history.*;
import com.inductiveautomation.ignition.gateway.history.sf.BasicDataTransaction;
import com.inductiveautomation.ignition.gateway.model.GatewayContext;
import com.inductiveautomation.ignition.gateway.sqltags.model.BasicScanclassHistorySet;
import com.unsautomation.ignition.piintegration.piwebapi.ApiException;
import com.unsautomation.ignition.piintegration.piwebapi.PIWebApiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Responsible for actually storing the data to PI.
 */
public class PIHistorySink implements DataSink {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private PIHistoryProviderSettings settings; // Holds the settings for the current provider, needed to connect to ADX
    private final GatewayContext context;
    private final String pipelineName;
    private PIWebApiClient piClient;

    public PIHistorySink(PIWebApiClient client, String pipelineName, GatewayContext context, PIHistoryProviderSettings settings) throws URISyntaxException {
        this.piClient = client;
        this.pipelineName = pipelineName;
        this.context = context;
        setSettings(settings);
        logger.debug("Started Sink with Pipeline: '" + pipelineName + "'");
    }

    public void setSettings(PIHistoryProviderSettings settings) {
        this.settings = settings;
    }

    @Override
    public String getPipelineName() {
        return pipelineName;
    }

    @Override
    public void startup() {
        logger.debug("Startup called");
    }

    @Override
    public void shutdown() {
    }

    @Override
    public boolean isAccepting() {
        return piClient.getCustom().isAvailable();
    }

    @Override
    public List<DataSinkInformation> getInfo() {
        return Arrays.asList(new HistorySinkStatus());
    }

    @Override
    public QuarantineManager getQuarantineManager() {
        return null; // Not implemented
    }

    /**
     * Called from Ignition when tags change and have data available for storage.
     */
    @Override
    public void storeData(HistoricalData data) throws ApiException, IOException { // TODO Should we fail on error?
        var validatedRecords = new ArrayList<HistoricalTagValue>();

        for (var row : BasicDataTransaction.class.cast(data).getData()) {
            if (row instanceof BasicScanclassHistorySet) {
                var scanset = BasicScanclassHistorySet.class.cast(row);
                if (scanset.size() == 0) continue;

                for (var r : scanset) {
                    var tagName = r.getSource().toStringPartial();
                    var validPiTag = tagName.matches("^[A-Za-z0-9_%][^*'?;{}\\[\\]|\\`]*$");
                    if (!validPiTag) {
                        validatedRecords.add(r);
                    } else {
                        // FIXME: Need to support Ignition tags which are not valid PI tags
                    }
                }
                piClient.getCustom().ingestRecords(validatedRecords, settings.getPITagPrefix(), settings.getPIArchiver());
            } else {
                logger.info("Not storing data with the following class: " + row.getClass().toString());
            }
        }
    }

    @Override
    public boolean acceptsData(HistoryFlavor historyFlavor) {
        return historyFlavor.equals(HistoryFlavor.SQLTAG);
    }

    @Override
    public boolean isLicensedFor(HistoryFlavor historyFlavor) {
        return true;
    }

    protected class HistorySinkStatus implements DataSinkInformation {

        @Override
        public DataStoreStatus getDataStoreStatus() {
            return null;
        }

        @Override
        public String getDescriptionKey() {
            return null;
        }

        @Override
        public boolean isAvailable() {
            return isAccepting();
        }

        @Override
        public boolean isDataStore() {
            return false;
        }

        @Override
        public List<LocalizedString> getMessages() {
            return null;
        }

        @Override
        public StatMetric getStorageMetric() {
            return null;
        }
    }
}