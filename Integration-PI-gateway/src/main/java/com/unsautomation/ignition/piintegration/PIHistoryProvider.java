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
import com.inductiveautomation.ignition.gateway.localdb.persistence.RecordListenerAdapter;
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
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

// , AnnotationQueryProvider
public class PIHistoryProvider implements TagHistoryProvider  {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final String name;
    private final GatewayContext context;
    private IPIHistoryProviderSettings settings;
    private PIHistorySink sink;
    private PIWebApiClient piClient;

    public PIHistoryProvider(GatewayContext context, String name, IPIHistoryProviderSettings settings) throws URISyntaxException, ApiException {
        logger.debug("PIHistoryProvider CTOR");
        this.name = name;
        this.context = context;
        setSettings(settings);

        if (settings.getClass().isAssignableFrom(PIHistoryProviderSettings.class)) {
            var s = (PIHistoryProviderSettings)settings;
            s.META.addRecordListener(new RecordListenerAdapter<>() {
                @Override
                public void recordUpdated(PIHistoryProviderSettings record) {
                    //applyNewSettings(record);
                    logger.info("Configuration change detected... Updating module settings");
                    sink.setSettings(record);
                    try {
                        setSettings(record);
                    } catch (ApiException e) {
                        logger.error("Failed to update settings", e);
                    }
                }
            });
        }
    }

    /***
     * update settings
     * @param settings
     * @throws ApiException
     */
    public void setSettings(IPIHistoryProviderSettings settings) throws ApiException {
        this.settings = settings;
        piClient = new PIWebApiClient(settings.getWebAPIUrl(), settings.getUsername(), settings.getPassword(), settings.getVerifySSL(), false);
    }

    @Override
    public void startup() {
        try {
            logger.info("Starting PIHistoryProvider Provider");
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

        logger.info("Calling getAvailableAggregates");
        final var values = Arrays.stream(PIAggregates.values()).map(n -> n.getIgnitionAggregate()).collect(Collectors.toList());;
        return values;
    }

    @Override
    public ProfileStatus getStatus() {
        return piClient.getCustom().isAvailable() ? ProfileStatus.RUNNING : ProfileStatus.ERRORED;
    }

    @Override
    public TagHistoryProviderInformation getStatusInformation() {
        return TagHistoryProviderInformation.newBuilder()
                .allowsStorage(true)
                .status(getStatus())
                .name(getName())
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

    private List<TagResult> createResults(QualifiedPath basePath, JsonArray items, boolean hasChildren) {
        var list = new ArrayList<TagResult>();
        var tagPath = basePath.getPathComponent(WellKnownPathTypes.Tag);
        for (var item : items) {
            var name = item.getAsJsonObject().get("Name").getAsString();
            var tr = new TagResult();

            // TODO: we need a better solution.
            name = name.replace("/", "\\");

            var p = basePath.replace(WellKnownPathTypes.Tag, tagPath + "/" + name);

            tr.setHasChildren(hasChildren);
            tr.setPath(p);
            tr.setType(WellKnownPathTypes.Tag);
            list.add(tr);
        }
        return list;
    }

    private ArrayList<Result> createRootResults(QualifiedPath qualifiedPath) {
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

    /**
     * Browses for tags available in PI. Returns a tree of tags. The function is called several times,
     * lazy loading, from a specific starting point.
     * @return
     */
    @Override
    public Results<Result> browse(QualifiedPath qualifiedPath, BrowseFilter browseFilter)  {
        if (logger.isDebugEnabled()) {
            logger.debug("browse(qualifiedPath, browseFilter) called.  qualifiedPath: " + qualifiedPath.toString()
                    + ", browseFilter: " + (browseFilter == null ? "null" : browseFilter.toString()));
        }

        final var tagPath = qualifiedPath.getPathComponent(WellKnownPathTypes.Tag);
        Results<Result> result;

        if (null == tagPath) {
            // Browse root
            result = new Results<>();
            final var list = createRootResults(qualifiedPath);
            result.setResults(list);
            result.setResultQuality(QualityCode.Good);
            return result;
        } else {
            try {
                final var cPoint = browseFilter != null ? browseFilter.getContinuationPoint() : null;
                result = browseInternal(qualifiedPath, cPoint);
            } catch (Exception ex) {
                logger.error("Unable to browse PI", ex);
                result = new Results<>();
                result.setResultQuality(QualityCode.Bad_Failure);
            }
        }
        return result;
    }

    private Results<Result> browseInternal(QualifiedPath qualifiedPath,  String continuationPoint) throws ApiException, UnsupportedEncodingException {
        final int pagSize = 100;
        final var result = new Results<Result>();
        final var list = new ArrayList<Result>();
        final var tagType = PIPathUtilities.findPathType(qualifiedPath);
        var webId = "";
        Integer currentContinuationPoint = 0;

        // Looks like Ignition does not support ContinuationPoint on browse
        // so this will handle data paging internally (need to test with large PI System)
        if (null != continuationPoint) {
            try {
                currentContinuationPoint = Integer.parseInt(continuationPoint);
            }catch (Exception e) {
                logger.error("Unable to parse continuationPoint :" + continuationPoint);
            }
        }

        if (!tagType.equals(PIObjectType.AssetsRoot) && !tagType.equals(PIObjectType.PointsRoot)) {
            webId = WebIdUtils.toWebID(qualifiedPath);
        }
        var data = new JsonArray();

        switch (tagType) {
            case AssetsRoot: // Top level Elements search
                data = piClient.getAssetServer().list("Items.Name");
                data = filterList(data, settings.getBrowsableAFServers());
                list.addAll(createResults(qualifiedPath, data, true));
                break;
            case PIAFServer:
                data = piClient.getAssetDatabase().list(webId, "Items.Name");
                var res = createResults(qualifiedPath, data, true);

                // If there are only one server, then show DBs directly..
                if (res.size() == 1) {
                    logger.info("Browser AF Server only return one server.. browsing DBs");
                    return browseInternal(res.get(0).getPath(), continuationPoint);
                }
                list.addAll(res);
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
            case PointsRoot: // Top level DataServer List
                data = piClient.getDataServer().list("Items.Name");
                data = filterList(data, settings.getBrowsablePIServers());
                var r = createResults(qualifiedPath, data, true);

                // There are no reason to show the user one PIServer node. if there are only one PI server, then return Points for that PI Server
                if (r.size() == 1) {
                    logger.info("Browser AF Server only return one server.. browsing DBs");
                    return browseInternal(r.get(0).getPath(), continuationPoint);
                }
                list.addAll(r);
                break;
            case PIServer:
                // Search a PI Server for tags
                var nameFilter = settings.getOnlyBrowsePITagsWithPrefix() ? settings.getPITagPrefix() + "*" : null;
                var resp = piClient.getDataServer().getPoints(webId, nameFilter, currentContinuationPoint, pagSize,"Items.Name");
                data = resp.get("Items").getAsJsonArray();
                list.addAll(createResults(qualifiedPath, data, false));

                break;
            default:
                logger.error("Unable to parse Path " + qualifiedPath.toString());
                result.setResultQuality(QualityCode.Bad_Failure);
                return result;
        }
        result.setResults(list);
        result.setResultQuality(QualityCode.Good);

        if (pagSize == data.size() && tagType == PIObjectType.PIServer) { // if the returned data size == pageSize, then there is a good change there are more data pages.
            currentContinuationPoint += pagSize;
            // TODO: This does not work
            //result.setContinuationPoint(currentContinuationPoint.toString());
            //result.setTotalAvailableResults(currentContinuationPoint + pagSize);

            // Handle data paging internally until ContinuationPoint get implement in Browse method..
            var d = browseInternal(qualifiedPath,currentContinuationPoint.toString());
            if (logger.isDebugEnabled()) {
                logger.debug("Browsing Continuation Point" + currentContinuationPoint);
            }
            list.addAll(d.getResults());
        }
        return result;
    }

    private JsonArray filterList(JsonArray list, String filter) {
        if (null == filter || "" == filter) {
            return list;
        }
        final var filterNames = Arrays.asList(filter.toUpperCase().split(","));
        final var r = new JsonArray();

        for (var item : list) {
            final var itemName = item.getAsJsonObject().get("Name").getAsString().toUpperCase();
            if (filterNames.contains(itemName)) {
                r.add(item);
            }
        }
        return r;
    }
    @Override
    public TimelineSet queryDensity(
            List<QualifiedPath> tags,
            Date startDate,
            Date endDate,
            String queryId) throws Exception {
        logger.info("queryDensity(tags, startDate, endDate, queryId) called.  tags: " + tags.toString()
                + ", startDate: " + startDate.toString() + ", endDate: " + endDate.toString() + ", queryId: " + queryId);

        var value = 0f;
        for (var qualifiedPath : tags) {
            final var webId = WebIdUtils.toWebID(qualifiedPath);
            final var response = piClient.getStream().getSummary(webId, startDate, endDate, "PercentGood", "TimeWeighted");
            final var valueWrapper = response.getContent().getAsJsonObject().get("Items").getAsJsonArray().get(0);
            value += valueWrapper.getAsJsonObject().get("Value").getAsJsonObject().get("value").getAsFloat();
        }
        // avg. total percent of the time tags are good in the time range
        value /= tags.size();

        var timelines = new ArrayList<Timeline>();
        var t = new Timeline();
        t.add(startDate.getTime(), endDate.getTime(), value);
        timelines.add(t);
        var timelineSet = new TimelineSet(timelines);
        return timelineSet;
    }

    /*
    @Override
    public List<Annotation> queryAnnotations(List<QualifiedPath> paths, Date start, Date end, TypeFilter filter, String queryId) throws Exception {
        return null;
    } */
}
