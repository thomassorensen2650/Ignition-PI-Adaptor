package com.unsautomation.ignition.piintegration;

import com.inductiveautomation.ignition.common.WellKnownPathTypes;
import com.inductiveautomation.ignition.common.gson.JsonArray;
import com.inductiveautomation.ignition.common.gson.JsonObject;
import com.inductiveautomation.ignition.common.model.values.QualityCode;
import com.inductiveautomation.ignition.common.sqltags.model.types.DataQuality;
import com.inductiveautomation.ignition.common.sqltags.model.types.DataTypeClass;
import com.inductiveautomation.ignition.gateway.model.GatewayContext;
import com.inductiveautomation.ignition.gateway.sqltags.history.query.*;
import com.inductiveautomation.ignition.gateway.sqltags.history.query.columns.DefaultHistoryColumn;
import com.inductiveautomation.ignition.gateway.sqltags.history.query.columns.ErrorHistoryColumn;
import com.inductiveautomation.ignition.gateway.sqltags.history.query.columns.HistoryColumn;
import com.inductiveautomation.ignition.gateway.sqltags.history.query.columns.ProcessedHistoryColumn;
import com.inductiveautomation.ignition.gateway.sqltags.history.query.processing.ProcessedValue;
import com.inductiveautomation.metro.utils.StringUtils;
import com.unsautomation.ignition.piintegration.piwebapi.PIWebApiClient;
import com.unsautomation.ignition.piintegration.piwebapi.WebIdUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Responsible for actually querying the data from PI. The query controller
 * provides the list of tags and settings for querying the data. We can either
 * query for raw data or break up the data into intervals. If we break data up,
 * we can apply an aggregation function against the intervals, such as average.
 */
public class PIQueryExecutor  implements HistoryQueryExecutor {
    private final Logger logger = LoggerFactory.getLogger(getClass());


    private List<String> webIds;
    private final GatewayContext context;
    private final PIHistoryProviderSettings settings; // Holds the settings for the current provider, needed to connect to ADX
    private final QueryController controller; // Holds the settings for what the user wants to query
    private final List<ColumnQueryDefinition> paths; // Holds the definition of each tag
    protected final List<DelegatingHistoryNode> nodes = new ArrayList();
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
        this.webIds = new ArrayList<>();

        for (var c : paths) {
            var qPath = c.getPath();
            var tagPath = qPath.getPathComponent(WellKnownPathTypes.Tag);

            if (StringUtils.isBlank(tagPath)) {
                var historyTag = new ErrorHistoryColumn(c.getColumnName(), DataTypeClass.Integer, QualityCode.Error_Configuration);
                logger.error(controller.getQueryId() + ": The item path '" + c.getPath() + "' does not have a valid tag path component.");
            } else {
                var historyTag = new ProcessedHistoryColumn(c.getColumnName(), isRaw);
                var tagParts = tagPath.split("/");
                var webId = tagParts[tagParts.length-1];
                var dt = piClient.getPoint()
                                .Get(webId)
                                .get("PointType")
                                .getAsString();
                switch (dt) {
                    case "Digital":
                    case "String":
                    case "Blob":
                        historyTag.setDataType(DataTypeClass.String);
                        break;
                    case "Float64":
                    case "Float32":
                    case "Float16":
                        historyTag.setDataType(DataTypeClass.Float);
                        break;
                    case "Timestamp":
                        historyTag.setDataType(DataTypeClass.DateTime);
                        break;
                    case "Int32":
                    case "Int16":
                        historyTag.setDataType(DataTypeClass.Integer);
                        break;
                    default:
                        logger.warn("Unknown DataType:" + dt);
                }
                tags.add(historyTag);
            }
        }
    }

    /**
     * Provides the data structure to Ignition where we have our tags stored
     */
    @Override
    public List<? extends HistoryNode> getColumnNodes() {
        return new ArrayList<HistoryNode>(tags);
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

        // TODO: Should be wrapped in PI BatchRequest to save PI web api "server round trips" if more than one reading
        for (int i = 0; i < paths.size() ; i++) {
            var t = paths.get(i).getPath();
            var tagPath = t.getPathComponent(WellKnownPathTypes.Tag);
            var tagParts = tagPath.split("/");
            var webId = tagParts[tagParts.length-1];

            if (blockSize == 0) {
                // TODO: Support data pagination
                logger.debug("Fetching raw PI data");
                var d =  piClient.getStream().getRecorded(webId, startDate, endDate, null,null,null);
                queryResult = d.getContent().getAsJsonArray("Items");
            } else  {
                var function = controller.getQueryParameters().getAggregationMode();
                logger.debug("Fetching data using Pi Aggregate " + function);
                var interval = (endDate.getTime() - startDate.getTime())/blockSize;

                if (function.equals(PIAggregates.PI_PLOT.getIgnitionAggregate())) {
                    queryResult = piClient.getStream().getPlot(webId, startDate, endDate, interval, null,null,null);
                } else if (function.equals(PIAggregates.PI_INTERPOLATED.getIgnitionAggregate())) {
                    var d = piClient.getStream().getInterpolated(webId, startDate, endDate, interval, null,null,null);
                    queryResult = d.getContent().getAsJsonArray("Items");
                } else {
                    var retrievalMode = PIAggregates.getPiAggregate(function);
                    var d = piClient.getStream().getSummary(webId, startDate, endDate, retrievalMode, "TimeWeighted");
                    var data = d.getContent().getAsJsonArray("Items");

                    var returnData = new JsonArray();
                    for (var value : data) {
                        returnData.add(value.getAsJsonObject().getAsJsonObject("Value"));
                    }
                    queryResult = returnData;
                }
            }
            for (var dv : queryResult) {
                var value = new PITagValue(dv.getAsJsonObject());
                var ts = value.getTimestamp();
                // FIXME: Float?

                var h = new ProcessedValue(value.getValue(), value.getQuality(), ts,blockSize > 0);
                tags.get(i).put(h);

                if (ts > this.maxTSInData) {
                    this.maxTSInData = ts;
                }
            }
        }
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
    public long processData() {
        processed = true;
        return maxTSInData;
    }

    /**
     * Called after we have processed data for any clean up
     */
    @Override
    public void endReading() {
        // NOP
    }

    @Override
    public long nextTime() {
        return Long.MAX_VALUE;
    }
}