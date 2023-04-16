package com.unsautomation.ignition.piintegration.piwebapi;

import com.inductiveautomation.ignition.gateway.history.HistoricalTagValue;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface IPIDataProducer {

    /***
     * This methods will setup the data producer to auto create PI AF Contextual Model
     * @param enableAFAutoCreate
     * @param piAFServer
     * @param piAFDatabase
     * @param piAFElementRoot
     */
    void setupAFAutoCreate(Boolean enableAFAutoCreate, String piAFServer, String piAFDatabase, String piAFElementRoot);

    /***
     * This is the main method used to ingest data to PI.
     * @param records
     * @param tagPrefix
     * @param piArchiver
     * @throws ApiException
     */
     void ingestRecords(@NotNull List<HistoricalTagValue> records, String tagPrefix, String piArchiver) throws ApiException;
}
