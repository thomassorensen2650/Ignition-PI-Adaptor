package com.unsautomation.ignition.piintegration;

import com.inductiveautomation.ignition.common.QualifiedPath;
import com.inductiveautomation.ignition.common.QualifiedPathUtils;
import com.inductiveautomation.ignition.common.WellKnownPathTypes;
import com.inductiveautomation.ignition.common.browsing.BrowseFilter;
import com.inductiveautomation.ignition.common.browsing.Result;
import com.inductiveautomation.ignition.common.browsing.Results;
import com.inductiveautomation.ignition.common.browsing.TagResult;
import com.inductiveautomation.ignition.common.gson.JsonArray;
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
import com.unsautomation.ignition.piintegration.piwebapi.ApiException;
import com.unsautomation.ignition.piintegration.piwebapi.PIObjectType;
import com.unsautomation.ignition.piintegration.piwebapi.PIWebApiClient;
import com.unsautomation.ignition.piintegration.piwebapi.WebIdUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
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
    private final PIWebApiClient piClient;

    public PIHistoryProvider(GatewayContext context, String name, PIHistoryProviderSettings settings) throws URISyntaxException, ApiException {
        logger.debug("PIHistoryProvider CTOR Provider");
        this.name = name;
        this.context = context;
        this.settings = settings;
        piClient = new PIWebApiClient(settings.getWebAPIUrl(), settings.getUsername(), settings.getUsername(), settings.getVerifySSL(), false);
    }

    @Override
    public void startup() {
        try {
            logger.info("Starting Provider");
            // Create a new data sink with the same name as the provider to store data
            sink = new PIHistorySink(piClient, name, context, settings);
            context.getHistoryManager().registerSink(sink);
        } catch (Throwable e) {
            logger.error("Error registering PI history sink", e);
        }
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
            return new PIQueryExecutor(piClient, context, settings, tags, queryController);
        } catch (Exception e) {
            logger.error("Unable to create Query", e);
        }
        return null;
    }

    public List<TagResult> createResults(QualifiedPath basePath, JsonArray items, boolean hasChildren) {
        List<TagResult> list = new ArrayList<>();
        var tagPath = basePath.getPathComponent(WellKnownPathTypes.Tag);
        for (var item : items) {
            var name = item.getAsJsonObject().get("name").getAsString();
            var tr = new TagResult();
            var p = basePath.replace(WellKnownPathTypes.Tag, tagPath + "/" + name);
            tr.setHasChildren(hasChildren);
            tr.setPath(p);
            tr.setType(WellKnownPathTypes.Tag);
            list.add(tr);
        }
        return list;
    }

    public ArrayList<Result> createRootResults(QualifiedPath qualifiedPath) {
        logger.debug("Browsing Root Level");

        var histProv = qualifiedPath.getPathComponent(WellKnownPathTypes.HistoryProvider);
        var list = new ArrayList<Result>();

        var assets = new TagResult();
        var p = QualifiedPathUtils.toPathFromHistoricalString("[" + histProv + "]Assets");
        assets.setType(WellKnownPathTypes.Tag);
        assets.setHasChildren(true);
        assets.setPath(p);
        list.add(assets);

        var points = new TagResult();
        var p2 = QualifiedPathUtils.toPathFromHistoricalString("[" + histProv + "]Points");
        points.setType(WellKnownPathTypes.Tag);
        points.setHasChildren(true);
        points.setPath(p2);
        list.add(points);

        return list;
    }


    public GatewayContext getContext() {
        return context;
    }

    /**
     * Browses for tags available in PI. Returns a tree of tags. The function is called several times,
     * lazy loading, from a specific starting point.
     * @return
     */
    @Override
    public Results<Result> browse(QualifiedPath qualifiedPath, BrowseFilter browseFilter)  {
        logger.info("browse(qualifiedPath, browseFilter) called.  qualifiedPath: " + qualifiedPath.toString()
                + ", browseFilter: " + (browseFilter == null ? "null" : browseFilter.toString()));

        var tagPath = qualifiedPath.getPathComponent(WellKnownPathTypes.Tag);
        if (null == tagPath) {
            // Browse root
            var list = new ArrayList<Result>();
            var result = new Results<Result>();
            list = createRootResults(qualifiedPath);
            result.setResults(list);
            result.setResultQuality(QualityCode.Good);
            return result;
        }

        try {
            return browseInternal(qualifiedPath);
        } catch (Exception ex) {
            logger.error("Unable to browse PI", ex);
        }
        return new Results<Result>();
    }
    private Results<Result> browseInternal(QualifiedPath qualifiedPath) throws ApiException, UnsupportedEncodingException {
        Results<Result> result = new Results<>();
        var list = new ArrayList<Result>();

        var tagType = PIPathUtilities.findPathType(qualifiedPath);
        var webId = "";

        if (!tagType.equals(PIObjectType.Assets) && !tagType.equals(PIObjectType.Points)) {
            WebIdUtils.toWebID(qualifiedPath);
        }
        var data = new JsonArray();
        var hasChildren = true;

        switch (tagType) {

            case Assets:
                data = piClient.getAssetServer().list(null);
                list.addAll(createResults(qualifiedPath, data, true));
                break;
            case PIAFServer:
                data = piClient.getAssetDatabase().list(webId, null);
                list.addAll(createResults(qualifiedPath, data, true));
                break;
            case PIAFDatabase:
                data =  piClient.getAssetDatabase().getElements(webId);
                list.addAll(createResults(qualifiedPath, data, true));
                break;
            case PIAFElement:
                // Child Elements
                data = piClient.getElementApi().getElements(webId);
                list.addAll(createResults(qualifiedPath, data, true));
                // Attributes
                data = piClient.getElementApi().getAttributes(webId);
                list.addAll(createResults(qualifiedPath, data, false));
                break;
            case Points:
                data = piClient.getDataServer().list(null);
                list.addAll(createResults(qualifiedPath, data, true));
                break;
            case PIServer:
                var res = piClient.getDataServer().getPoints(webId, null,null,null,null);
                data = res.get("Items").getAsJsonArray();
                list.addAll(createResults(qualifiedPath, data, false));
                break;
            default:
                throw new UnsupportedEncodingException("Unable to parse Path " + qualifiedPath.toString());
        }

        //list.addAll(createResults(qualifiedPath, data, hasChildren));
        result.setResults(list);
        result.setResultQuality(QualityCode.Good);
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

        Timeline t = new Timeline();
        t.addSegment(2, 3);
        timelines.add(t);

        TimelineSet timelineSet = new TimelineSet(timelines);
        return timelineSet;
    }
}
