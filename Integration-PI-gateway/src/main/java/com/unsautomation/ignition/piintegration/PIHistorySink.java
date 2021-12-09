package com.unsautomation.ignition.piintegration;

import com.inductiveautomation.ignition.common.StatMetric;
import com.inductiveautomation.ignition.common.i18n.LocalizedString;
import com.inductiveautomation.ignition.gateway.history.*;
import com.inductiveautomation.ignition.gateway.model.GatewayContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Responsible for actually storing the data to PI. Can either use the
 * built-in store & forward system for Ignition or use its own.
 */
public class PIHistorySink implements DataSink {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private PIHistoryProviderSettings settings; // Holds the settings for the current provider, needed to connect to ADX
    private GatewayContext context;
    private String pipelineName;
    private String table;
    private String database;

    private PIQueryClientImpl piClient;
    private final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSSSSS");

   //private IngestionProperties ingestionProperties;

    public PIHistorySink(String pipelineName, GatewayContext context, PIHistoryProviderSettings settings) {
        piClient = new PIQueryClientImpl(settings);
        logger.info("Starting Sink...:)");
        this.pipelineName = pipelineName;
        this.context = context;
        this.settings = settings;
        logger.info("Started with Pipeline: '" + pipelineName + "'");

    }

    @Override
    public String getPipelineName() {
        return pipelineName;
    }

    @Override
    public void startup() {
        logger.debug("Startup called");

        /*String clusterURL = settings.getClusterURL();
        String applicationId = settings.getApplicationId();
        String applicationKey = settings.getApplicationKey();
        String aadTenantId = settings.getAADTenantId();
        database = settings.getDatabaseName();

        String dmUrl = Utils.getDMUriFromSetting(clusterURL);
        String engineURL = Utils.getEngineUriFromSetting(clusterURL);

        ConnectionStringBuilder connectionString = ConnectionStringBuilder.createWithAadApplicationCredentials(
                engineURL,
                applicationId,
                applicationKey,
                aadTenantId);
        ConnectionStringBuilder DmConnectionString = ConnectionStringBuilder.createWithAadApplicationCredentials(
                dmUrl,
                applicationId,
                applicationKey,
                aadTenantId);

        try {
            ClientImpl client = new ClientImpl(connectionString);

            try {
                KustoOperationResult result = client.execute(database, ".show table " + table);
            } catch (Throwable ex) {
                try {
                    client.execute(database, ".create table " + table + " ( systemName:string, tagProvider:string, tagPath:string, value:dynamic, value_double:real, value_integer:int, timestamp:datetime, quality:int)");
                } catch (Throwable ex2) {
                    logger.error("Error creating table '" + table + "'", ex2);
                }
            }
            streamingIngestClient = IngestClientFactory.createStreamingIngestClient(connectionString);
            queuedClient = IngestClientFactory.createClient(DmConnectionString);
            table = settings.getTableName();
            ingestionProperties = new IngestionProperties(database, table);
            ingestionProperties.setDataFormat(IngestionProperties.DATA_FORMAT.csv);
        } catch (URISyntaxException ex) {
            logger.error("Error on AzureKustoHistorySink startup ", ex);
        }
         */
    }

    @Override
    public void shutdown() {

    }

    @Override
    public boolean isAccepting() {
        // TODO: Determine if ADX is accepting data
        return true;
    }

    @Override
    public List<DataSinkInformation> getInfo() {
        // TODO: Determine the status of the history sink
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
    public void storeData(HistoricalData data) throws IOException, InterruptedException, URISyntaxException { // TODO Should we fail on error?
        logger.debug("Received data of type '" + data.getClass().toString() + "'");

        List<HistoricalTagValue> records = new ArrayList<HistoricalTagValue>();

        List<HistoricalData> dataList;
        if (data instanceof DataTransaction) {
            dataList = ((DataTransaction) data).getData();
        } else {
            dataList = Collections.singletonList(data);
        }

        // Find all of the tags passed in that have data
        logger.debug("History set with '" + dataList.size() + "' row(s)");
        for (HistoricalData d : dataList) {
            if (d instanceof ScanclassHistorySet) {
                ScanclassHistorySet dSet = (ScanclassHistorySet) d;
                logger.debug("Scan class set '" + dSet.getSetName() + "' has '" + dSet.size() + "' tag(s)");
                for (HistoricalTagValue historicalTagValue : dSet) {

                    records.add(historicalTagValue);
                }
            } else if (d instanceof HistoricalTagValue) {
                HistoricalTagValue dValue = (HistoricalTagValue) d;
                records.add(dValue);
            }
        }
        piClient.ingestRecords(records);

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
