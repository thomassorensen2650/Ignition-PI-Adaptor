package com.unsautomation.ignition.piintegration;

import com.inductiveautomation.ignition.gateway.history.HistoricalTagValue;
import com.unsautomation.ignition.piintegration.piwebapi.ApiException;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface IPIDataSink {

    /***
     * This methods will setup the data producer to auto create PI AF Contextual Model
     * @param enableAFAutoCreate PI AF (Asset Framework) object model will be created when true
     * @param piAFServer AF Server to use
     * @param piAFDatabase AF Database to use
     * @param piAFElementRoot The root AF Element where the model will be created (root of null or empty string)
     */
    void setupAFAutoCreate(Boolean enableAFAutoCreate, String piAFServer, String piAFDatabase, String piAFElementRoot);

    /***
     * This is the main method used to ingest data to PI.
     * @param records The records to ingest
     * @param tagPrefix the tagPrefix to prepend to all the tags created
     * @param piArchiver the PI data archive server where the tags will be created.
     * @throws ApiException Anything goes wrong writing data to PI
     */
     void ingestRecords(@NotNull List<HistoricalTagValue> records, String tagPrefix, String piArchiver) throws ApiException;
}
