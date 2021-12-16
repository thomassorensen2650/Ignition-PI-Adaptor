package com.unsautomation.ignition.piintegration;

import com.inductiveautomation.ignition.common.QualifiedPath;
import com.inductiveautomation.ignition.common.QualifiedPathUtils;
import com.inductiveautomation.ignition.common.WellKnownPathTypes;
import com.inductiveautomation.ignition.common.browsing.BrowseFilter;
import com.inductiveautomation.ignition.common.browsing.Result;
import com.inductiveautomation.ignition.common.browsing.Results;
import com.inductiveautomation.ignition.common.browsing.TagResult;
import com.inductiveautomation.ignition.common.model.values.QualityCode;
import com.inductiveautomation.ignition.common.sqltags.history.Aggregate;
import com.inductiveautomation.ignition.common.util.Timeline;
import com.inductiveautomation.ignition.common.util.TimelineSet;
import com.inductiveautomation.ignition.gateway.model.GatewayContext;
import com.inductiveautomation.ignition.gateway.model.ProfileStatus;
import com.inductiveautomation.ignition.gateway.sqltags.history.TagHistoryProvider;
import com.inductiveautomation.ignition.gateway.sqltags.history.TagHistoryProviderInformation;
import com.inductiveautomation.ignition.gateway.sqltags.history.query.ColumnQueryDefinition;
import com.inductiveautomation.ignition.gateway.sqltags.history.query.QueryController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class PIHistoryProvider implements TagHistoryProvider {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private String name;
    private GatewayContext context;
    private PIHistoryProviderSettings settings;
    private PIHistorySink sink;
    //private PIQueryClientImpl piClient; // A client for querying data

    public PIHistoryProvider(GatewayContext context, String name, PIHistoryProviderSettings settings) {
        logger.info("CTOR Provider");
        this.name = name;
        this.context = context;
        this.settings = settings;
        //piClient = new PIQueryClientImpl(settings);
    }

    @Override
    public void startup() {
        try {
            logger.info("Starting Provider");
            // Create a new data sink with the same name as the provider to store data
            sink = new PIHistorySink(name, context, settings);
            context.getHistoryManager().registerSink(sink);

            // Create a PI client
            ConnectToPI();

        } catch (Throwable e) {
            logger.error("Error registering PI history sink", e);
        }
    }

    public void ConnectToPI() throws URISyntaxException {
        /*String clusterURL = settings.getClusterURL();
        String applicationId = settings.getApplicationId();
        String applicationKey = settings.getApplicationKey();
        String aadTenantId = settings.getAADTenantId();
*/
        //ConnectionStringBuilder connectionString;

        //connectionString = ConnectionStringBuilder.createWithAadApplicationCredentials(
        //        clusterURL,
        //        applicationId,
        //        applicationKey,
        //        aadTenantId);

    }

    @Override
    public void shutdown() {
        try {
            // Unregister the data sink so it doesn't show up in the list to choose from
            context.getHistoryManager().unregisterSink(sink, false);
        } catch (Throwable e) {
            logger.error("Error shutting down PI history sink", e);
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public List<Aggregate> getAvailableAggregates() {
        // TODO: Determine the list of aggregate functions that the module supports
        return new ArrayList<Aggregate>();
    }

    @Override
    public ProfileStatus getStatus() {
        // TODO: Determine status for retrieval
        return ProfileStatus.RUNNING;
    }

    @Override
    public TagHistoryProviderInformation getStatusInformation() {
        return TagHistoryProviderInformation.newBuilder().allowsStorage(true).status(getStatus()).name(getName())
                .build();
    }

    @Override
    public PIQueryExecutor createQuery(List<ColumnQueryDefinition> tags, QueryController queryController) {
        logger.info("createQuery(tags, queryController) called.  tags: " + tags.toString()
                + ", queryController: " + queryController.toString());

        try {
            return new PIQueryExecutor(context, settings, tags, queryController);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Browses for tags available in PI. Returns a tree of tags. The function is called several times,
     * lazy loading, from a specific starting point.
     * @return
     */
    @Override
    public Results<Result> browse(QualifiedPath qualifiedPath, BrowseFilter browseFilter) {
        logger.debug("browse(qualifiedPath, browseFilter) called.  qualifiedPath: " + qualifiedPath.toString()
                + ", browseFilter: " + (browseFilter == null ? "null" : browseFilter.toString()));

        // [Tag Provider]folder/path/tag.property

        // qualifiedPath: histprov:HAHAH,
        // browseFilter: BrowseFilter{allowedTypes=null, nameFilters=null, properties={}, excludeProperties=null, maxResults=-1, offset=-1, continuationPoint='null', recursive=false}
        ArrayList<Result> list = new ArrayList<>();
        String histProv = qualifiedPath.getPathComponent(WellKnownPathTypes.HistoryProvider);
        String systemName = null;
        String tagProvider = null;
        String driver = qualifiedPath.getPathComponent(WellKnownPathTypes.Driver);
        if (driver != null) {
            String[] parts = driver.split(":");
            systemName = parts[0];
            tagProvider = parts[1];
        }
        String tagPath = qualifiedPath.getPathComponent(WellKnownPathTypes.Tag);

        logger.info("provider: '" + histProv +
                ",  driver: '" + driver +
                "', systemName: '" + systemName +
                "', tagProvider: '" + tagProvider +
                "', tagPath: '" + tagPath);

        Results<Result> result = new Results<Result>();


        if (systemName == null) {
            // Query Top Level
            TagResult af = new TagResult();
            af.setType("tag");
            af.setHasChildren(true);
            QualifiedPath fullTagPath = QualifiedPathUtils.toPathFromHistoricalString("[" + histProv + "/pi:default]");
            af.setPath(fullTagPath);
            list.add(af);

            TagResult pi = new TagResult();
            pi.setType("tag");
            pi.setHasChildren(true);
            QualifiedPath fullTagPath2 = QualifiedPathUtils.toPathFromHistoricalString("[" + histProv + "/pi:default]");
            pi.setPath(fullTagPath2);


            list.add(pi);

        } else {
            TagResult pi = new TagResult();
            pi.setType("tag");
            pi.setHasChildren(false);
            QualifiedPath fullTagPath = QualifiedPathUtils.toPathFromHistoricalString("[" + histProv + "/pi:default]TEST");
            pi.setPath(fullTagPath);
            list.add(pi);
        }




        result.setResults(list);
        result.setResultQuality(QualityCode.Good);
        // FIXME: First one to implement

        /*ArrayList<Result> list = new ArrayList<>();

        // First, we need to find the starting point based on history provider, system name, tag provider, and tag path



        String query = settings.getTableName();
        if (systemName == null) {
            query += " | distinct systemName, tagProvider, tagPath";
            query += " | summarize countChildren = dcount(tagPath) by systemName, tagProvider";
            query += " | extend hasChildren = countChildren > 0";
            query += " | project systemName, tagProvider, hasChildren";
        } else if (tagPath == null) {
            query += " | where systemName == \"" + systemName + "\" | where tagProvider == \"" + tagProvider + "\"";
            query += " | distinct systemName, tagProvider, tagPath";
            query += " | extend tagPrefix = tostring(split(tagPath, \"/\")[0])";
            query += " | summarize countChildren = dcountif(tagPath, tagPath != tagPrefix) by systemName, tagProvider, tagPrefix";
            query += " | extend hasChildren = countChildren > 0";
            query += " | project systemName, tagProvider, tagPrefix, hasChildren";
        } else {
            String[] tagPathParts = tagPath.split("/");
            query += " | where systemName == \"" + systemName + "\" | where tagProvider == \"" + tagProvider + "\" | where tagPath startswith \"" + tagPath + "/\"";
            query += " | distinct systemName, tagProvider, tagPath";
            query += " | extend tagPrefix = strcat_array(array_slice(split(tagPath, \"/\"), 0, " + tagPathParts.length + "), \"/\")";
            query += " | summarize countChildren = dcountif(tagPath, tagPath != tagPrefix) by systemName, tagProvider, tagPrefix";
            query += " | extend hasChildren = countChildren > 0";
            query += " | project systemName, tagProvider, tagPrefix, hasChildren";
        }
        logger.debug("Issuing query:" + query);

        try {
            KustoOperationResult results = piClient.execute(settings.getDatabaseName(), query);
            KustoResultSetTable mainTableResult = results.getPrimaryResults();

            while (mainTableResult.next()) {
                boolean hasChildren = mainTableResult.getBoolean("hasChildren");
                String systemNameFromRecord = systemNameFromRecord = mainTableResult.getString("systemName");
                String tagProviderFromRecord = tagProviderFromRecord = mainTableResult.getString("tagProvider");
                String tagPathFromRecord = null;
                if (systemName != null) {
                    tagPathFromRecord = mainTableResult.getString("tagPrefix");
                }

                TagResult tagResult = new TagResult();
                tagResult.setHasChildren(hasChildren);
                QualifiedPath.Builder builder = new QualifiedPath.Builder().set(WellKnownPathTypes.HistoryProvider, histProv);
                if (systemNameFromRecord != null && !systemNameFromRecord.isEmpty()) {
                    builder.setDriver(systemNameFromRecord + ":" + tagProviderFromRecord);
                }
                if (tagPathFromRecord != null && !tagPathFromRecord.isEmpty()) {
                    builder.setTag(tagPathFromRecord);
                }
                tagResult.setPath(builder.build());
                list.add(tagResult);
            }
        } catch (Exception e) {
            logger.error("Issuing query failed: returning empty results: " + query);
        }

        result.setResults(list);

       //result.setResultQuality((Quality.GOOD);)*/
        result.setTotalAvailableResults(0);
        return result;
    }

    @Override
    public TimelineSet queryDensity(
            List<QualifiedPath> tags,
            Date startDate,
            Date endDate,
            String queryId) throws Exception {
        logger.info("queryDensity(tags, startDate, endDate, queryId) called.  tags: " + tags.toString()
                + ", startDate: " + startDate.toString() + ", endDate: " + endDate.toString() + ", queryId: " + queryId);

        ArrayList<Timeline> timelines = new ArrayList<>();
    /*
        String queryPrefix = "let startTime = " + Utils.getDateLiteral(startDate) + ";\n" +
                "let endTime = " + Utils.getDateLiteral(endDate) + ";\n";
        String queryData = settings.getTableName() + "| where timestamp between(startTime..endTime) ";

        queryData += "| where ";
        QualifiedPath[] tagKeys = tags.toArray(new QualifiedPath[]{});
        for (int i = 0; i < tagKeys.length; i++) {
            QualifiedPath tag = tagKeys[i];
            String systemName = null;
            String tagProvider = null;
            String driver = tag.getPathComponent(WellKnownPathTypes.Driver);
            if (driver != null) {
                String[] parts = driver.split(":");
                systemName = parts[0];
                tagProvider = parts[1];
            }
            String tagPath = tag.getPathComponent(WellKnownPathTypes.Tag);

            queryData += "(systemName has \"" + systemName + "\" and tagProvider has \"" + tagProvider + "\" and tagPath has \"" + tagPath + "\")";
            if (i < (tagKeys.length - 1)) {
                queryData += " or ";
            }
        }

        String querySuffix = "| summarize startDate = min(timestamp), endDate = max(timestamp) by systemName, tagProvider, tagPath";
        String query = queryPrefix + queryData + querySuffix;
        logger.debug("Issuing query:" + query);

        KustoOperationResult results = kustoQueryClient.execute(settings.getDatabaseName(), query);
        KustoResultSetTable mainTableResult = results.getPrimaryResults();

        while (mainTableResult.next()) {
            Timeline t = new Timeline();
            Timestamp start = mainTableResult.getTimestamp("startDate");
            Timestamp end = mainTableResult.getTimestamp("endDate");
            t.addSegment(start.getTime(), end.getTime());
            timelines.add(t);
        }
*/
        TimelineSet timelineSet = new TimelineSet(timelines);
        return timelineSet;
    }
}
