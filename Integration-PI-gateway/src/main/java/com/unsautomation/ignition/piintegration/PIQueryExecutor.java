package com.unsautomation.ignition.piintegration;

import com.inductiveautomation.ignition.common.WellKnownPathTypes;
import com.inductiveautomation.ignition.common.gson.JsonArray;
import com.inductiveautomation.ignition.common.gson.JsonObject;
import com.inductiveautomation.ignition.common.model.values.QualityCode;
import com.inductiveautomation.ignition.common.sqltags.model.types.DataTypeClass;
import com.inductiveautomation.ignition.gateway.model.GatewayContext;
import com.inductiveautomation.ignition.gateway.sqltags.history.query.*;
import com.inductiveautomation.ignition.gateway.sqltags.history.query.columns.ProcessedHistoryColumn;
import com.inductiveautomation.ignition.gateway.sqltags.history.query.processing.ProcessedValue;
import com.inductiveautomation.metro.utils.StringUtils;
import com.unsautomation.ignition.piintegration.piwebapi.PIWebApiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Responsible for actually querying the data from PI. The query controller
 * provides the list of tags and settings for querying the data. We can either
 * query for raw data or break up the data into intervals. If we break data up,
 * we can apply an aggregation function against the intervals, such as average.
 */
public class PIQueryExecutor  implements HistoryQueryExecutor {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private GatewayContext context;
    private PIHistoryProviderSettings settings; // Holds the settings for the current provider, needed to connect to ADX
    private QueryController controller; // Holds the settings for what the user wants to query
    private List<ColumnQueryDefinition> paths; // Holds the definition of each tag
    protected List<DelegatingHistoryNode> nodes = new ArrayList();
    protected List<ProcessedHistoryColumn> tags;
    protected JsonArray queryResult;

    private PIWebApiClient piClient; // A client for querying data

    boolean processed = false;
    long maxTSInData = -1;

    public PIQueryExecutor(PIWebApiClient client, GatewayContext context, PIHistoryProviderSettings settings, List<ColumnQueryDefinition> tagDefs, QueryController controller) throws URISyntaxException {
        this.context = context;
        this.settings = settings;
        this.controller = controller;
        this.paths = tagDefs;
        this.piClient = client;

        for (var def : paths) {
            this.nodes.add(new DelegatingHistoryNode(def.getColumnName()));
        }
        initTags();
    }

    /**
     * Initialize the tags we want to query. This will create a list of Tags
     * that provides the fully qualified tag path, aggregation function, and tag to return.
     */
    private void initTags() {
        boolean isRaw = controller.getBlockSize() <= 0;
        this.tags = new ArrayList<>();

        for (ColumnQueryDefinition c : paths) {
            var qPath = c.getPath();
            var tagPath = qPath.getPathComponent(WellKnownPathTypes.Tag);

            if (StringUtils.isBlank(tagPath)) {
                // We set the data type to Integer here, because if the column is going to be errored, at least integer types won't cause charts to complain.
                //historyTag = new ErrorHistoryColumn(c.getColumnName(), DataTypeClass.Integer, DataQuality.CONFIG_ERROR);
                logger.debug(controller.getQueryId() + ": The item path '" + c.getPath() + "' does not have a valid tag path component.");
            } else {
                ProcessedHistoryColumn historyTag = new ProcessedHistoryColumn(c.getColumnName(), isRaw);
                // Set data type to float by default, we can change this later if needed
                historyTag.setDataType(DataTypeClass.Float);
                tags.add(historyTag);
            }
        }
    }

    /**
     * Provides the data structure to Ignition where we have our tags stored
     */
    @Override
    public List<? extends HistoryNode> getColumnNodes() {
        List<HistoryNode> nodes = new ArrayList<>();
        nodes.addAll(tags);
        return nodes;
    }

    /**
     * Called first to initialize the connection
     */
    @Override
    public void initialize() throws Exception {
    }

    @Override
    public int getEffectiveWindowSizeMS() {
        return 0; // Always return 0 to allow for any kind of window sizes
    }

    /**
     * Called after initialization to start reading the data. This is where the bulk
     * of the work happens.
     */
    @Override
    public void startReading() throws Exception {
        var blockSize = (int) controller.getBlockSize();
        var startDate = controller.getQueryParameters().getStartDate();
        var endDate = controller.getQueryParameters().getEndDate();
        logger.debug("startReading(blockSize, startDate, endDate) called.  blockSize: " + blockSize
                + ", startDate: " + startDate.toString() + ", endDate: " + endDate.toString() + " pathSize:" + paths.size());

        for (int i = 0; i < paths.size() ; i++) {
            var t = paths.get(i).getPath();

            if (blockSize == 0) {
                queryResult = piClient.getStream().getRecorded(t.toString(), startDate, endDate, null,null,null);
            } else  {
                // TODO: calculate the interval from block size.
                var interval = (endDate.getTime() - startDate.getTime())/blockSize;
                queryResult = piClient.getStream().getPlot(t.toString(), startDate, endDate, interval, null,null,null);
            }
            for (var dv : queryResult) {
                //((DelegatingHistoryNode)this.nodes.get(i)).setDelegate(this.buildRealNode(dv));
                //((DefaultHistoryColumn)((DelegatingHistoryNode)this.nodes.get(i).getDelegate()).process $(this.historicalValue(p));

                var v = dv.getAsJsonObject().get("Value").getAsFloat();
                var time = dv.getAsJsonObject().get("Timestamp").getAsString();
                var sourceFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
                var d = sourceFormat.parse(time).getTime();

                var h = new ProcessedValue(v, QualityCode.Good, d,blockSize > 0);

                tags.get(i).put(h);

                if (d > this.maxTSInData) {
                    this.maxTSInData = d;
                }
            }
        }
    }

    protected HistoryNode buildRealNode(ColumnQueryDefinition def) {
        Object ret;
        ProcessedHistoryColumn c = new ProcessedHistoryColumn(def.getColumnName(), true);
        //c.setDataType(this.toDataTypeClass(pi.getDataType()));
        ret = c;
        return (HistoryNode)ret;
    }

    /**
     * Called after start reading to determine if there is more data to read
     */
    @Override
    public boolean hasMore() {
        return !processed;
    }

    /**
     * Called after we have no more data to read. Process data if needed.
     */
    @Override
    public long processData() throws Exception {
        processed = true;
        return maxTSInData;
    }

    /**
     * Called after we have processed data for any clean up
     */
    @Override
    public void endReading() {

    }

    @Override
    public long nextTime() {
        return Long.MAX_VALUE;
    }
}