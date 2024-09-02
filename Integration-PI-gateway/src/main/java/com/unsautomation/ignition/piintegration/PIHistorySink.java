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

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Responsible for actually storing the data to PI.
 */
public class PIHistorySink implements DataSink {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private PIHistoryProviderSettings settings; // Holds the settings for the current provider, needed to connect to ADX
    private final String pipelineName;

    public String PIDataServer;
    private final PIWebApiClient piClient;

    private final IPIDataSink piSink;

    public PIHistorySink(PIWebApiClient client, String pipelineName, GatewayContext context, PIHistoryProviderSettings settings) {
        this.piClient = client;
        this.pipelineName = pipelineName;
        this.piSink = new PIDataSinkBasic(client);
        setSettings(settings);
        logger.debug("Started Sink with Pipeline: '" + pipelineName + "'");
    }

    public void setSettings(PIHistoryProviderSettings settings) {
        PIDataServer = settings.getPIArchiver();
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
        try {
            if (!PIDataServer.equals("")) {
                var data = piClient.getDataServer().getByPath("\\\\" + PIDataServer);
                return data.get("IsConnected").getAsBoolean();
            } else {
                return false;
            }
        } catch (ApiException ex) {
            logger.error("Unable to get Data Archiver Status", ex);
            return false;
        }
    }

    @Override
    public List<DataSinkInformation> getInfo() {
        return List.of(new HistorySinkStatus());
    }

    @Override
    public QuarantineManager getQuarantineManager() {
        return null; // Not implemented
    }

    /**
     * Called from Ignition when tags change and have data available for storage.
     */
    @Override
    public void storeData(HistoricalData data) throws ApiException { // TODO Should we fail on error?
        var validatedRecords = new ArrayList<HistoricalTagValue>();

        for (var row : ((BasicDataTransaction) data).getData()) {
            if (row instanceof BasicScanclassHistorySet) {
                var scanset = (BasicScanclassHistorySet) row;
                if (scanset.size() == 0) continue;

                for (var r : scanset) {
                    var tagName = r.getSource().toStringPartial();
                    var validPiTag = tagName.matches("^[A-Za-z0-9_%][^*'?;{}\\[\\]|\\`]*$");
                    if (!validPiTag) {
                        validatedRecords.add(r);
                    } else {
                        // FIXME: Need to support Ignition tags which are not valid PI tags
                        logger.warn("Tagname '{}' is not valid in PI.. unable to store history", tagName);
                    }
                }
            } else {
                logger.info("Not storing data with the following class: " + row.getClass().toString());
            }
        }
        if (validatedRecords.size() > 0) {
            piSink.ingestRecords(validatedRecords, settings.getPITagPrefix(), settings.getPIArchiver());
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