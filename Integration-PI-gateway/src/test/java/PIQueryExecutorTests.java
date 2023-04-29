import com.google.protobuf.Api;
import com.inductiveautomation.ignition.common.QualifiedPath;
import com.inductiveautomation.ignition.common.WellKnownPathTypes;
import com.inductiveautomation.ignition.common.gson.JsonArray;
import com.inductiveautomation.ignition.common.gson.JsonObject;
import com.inductiveautomation.ignition.common.sqltags.history.TagHistoryQueryParams;
import com.inductiveautomation.ignition.gateway.sqltags.history.query.ColumnQueryDefinition;
import com.inductiveautomation.ignition.gateway.sqltags.history.query.QueryController;
import com.inductiveautomation.ignition.gateway.sqltags.history.query.columns.HistoryColumn;
import com.inductiveautomation.ignition.gateway.sqltags.history.query.columns.ProcessedHistoryColumn;
import com.inductiveautomation.ignition.gateway.sqltags.history.query.processing.QueryContext;
import com.unsautomation.ignition.piintegration.PIAggregates;
import com.unsautomation.ignition.piintegration.PIHistoryProvider;
import com.unsautomation.ignition.piintegration.PIHistoryProviderSettings;
import com.unsautomation.ignition.piintegration.PIQueryExecutor;
import com.unsautomation.ignition.piintegration.piwebapi.ApiClient;
import com.unsautomation.ignition.piintegration.piwebapi.ApiException;
import com.unsautomation.ignition.piintegration.piwebapi.PIDataSimulator;
import com.unsautomation.ignition.piintegration.piwebapi.PIResponse;
import org.apiguardian.api.API;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
public class PIQueryExecutorTests {

    final String providerName = "Test PI Provider";

    PIHistoryProvider historyProvider = null;

    PIDataSimulator simulator = new PIDataSimulator();
    @Mock
    QueryController queryController;

    @Mock
    TagHistoryQueryParams queryParams;

    @Mock
    ApiClient webApiClient;

    @Mock ApiClient actualWebClient;

    @Mock
    PIHistoryProviderSettings settings;

    @BeforeEach
    public void setUp() throws ApiException {
        settings = mock(PIHistoryProviderSettings.class);
        var webApi = System.getProperty("PIWebAPIUrl","https://localhost/piwebapi");
        lenient().when(settings.getWebAPIUrl()).thenReturn(webApi);
        lenient().when(settings.getUsername()).thenReturn("TestUser");
        lenient().when(settings.getPassword()).thenReturn("TestPassword");
        lenient().when(settings.getAPIMaxResponseLimit()).thenReturn(1000);
        lenient().when(settings.getAPIRequestPageSize()).thenReturn(10);
        lenient().when(settings.getSimulationMode()).thenReturn(false);
        historyProvider = new PIHistoryProvider(null, providerName, settings);
        webApiClient = mock(ApiClient.class);
        queryController = mock(QueryController.class);

        var startDate = new GregorianCalendar(2018, Calendar.JUNE, 25, 5, 0)
                .getTime();
        var endDate = new GregorianCalendar(2018, Calendar.JUNE, 25, 7, 0)
                .getTime();

        queryParams = mock(TagHistoryQueryParams.class);
        lenient().when(queryParams.getStartDate()).thenReturn(startDate);
        lenient().when(queryParams.getEndDate()).thenReturn(endDate);
        lenient().when(queryParams.getAggregationMode()).thenReturn(PIAggregates.PI_PLOT);

       // var x = new TagHistoryQueryParams()
        lenient().when(queryController.getBlockSize()).thenReturn(100L);
        lenient().when(queryController.getQueryParameters()).thenReturn(queryParams);

        //lenient().when(queryController.getQueryParameters().getEndDate()).thenReturn(endDate);

        actualWebClient = historyProvider.piClient.apiClient;
        historyProvider.piClient.apiClient = webApiClient;


    }

    @Test
    public void getPITagDataMinMax() throws ApiException {
        // TODO
        var simulatedData = simulateWebApiItemsResponse(new String[]{"WebId", "Name", "PointType"}, 1)
                .getAsJsonArray("Items")
                .get(0).getAsJsonObject();

        var p = new QualifiedPath.Builder()
                .set(WellKnownPathTypes.HistoryProvider, "Hi")
                .setTag("Assets/My Server/My DB/Random/Element")
                .build();

        var plotData = simulator.getPlot(queryParams.getStartDate(), queryParams.getEndDate(), 7200L);
;
        // API Call to lookup Datatype
        lenient().when(webApiClient.doGet(eq("points/Element"))).thenReturn(new PIResponse(200, simulatedData));
        lenient().when(webApiClient.doGet(startsWith("streams/Element/"))).thenReturn(new PIResponse(200, plotData));

        //@"streams/Element/plot?startTime=2018-06-25T12:00:00Z&endTime=2018-06-25T14:00:00Z&intervals=72000"
        // API Call to get data

        var tags = new ArrayList<ColumnQueryDefinition>();
        tags.add(new ColumnQueryDefinition(p, PIAggregates.PI_PLOT.getIgnitionAggregate(), "Element"));
        var queryExecutor = historyProvider.createQuery(tags, queryController);
        try {
            queryExecutor.initialize();
        }catch (Exception ex) {
            assertFalse(true);
            throw new RuntimeException(ex);
        }

        try {
            queryExecutor.startReading();
        } catch (Exception ex) {
            assertFalse(true);
            throw new RuntimeException(ex);
        }


        var cn = queryExecutor.getColumnNodes();
        assertEquals(1, cn.size());
        var phc =  (ProcessedHistoryColumn)cn.get(0);

        // Test Getting data type from PI
        var dt = phc.getDataType();


        Object x = phc.getValue(phc.getNextTimestamp(),0);
        int i = 0;

         while(phc.getNextTimestamp() != Long.MAX_VALUE) {
            i++;
            x = phc.getValue(phc.getNextTimestamp(),0);
             phc.markCompleted(phc.getNextTimestamp());

         }
        assertEquals(7200,i);
    }

    @Test
    public void getPITagDataMinMaxError() {
        // TODO
    }

    @Test
    public void getPIDataRaw() {
        // TODO
    }

    @Test
    public void getPIDataRawError() {
        // TODO
    }

    // TODO: Get Attribute Data
    // FIXMEwdz
    public JsonObject simulateWebApiItemsResponse(String[] attributes, int count) {
        var root = new JsonObject();
        var items = new JsonArray();
        root.add("Items", items);

        for (int i = 0; i < count; i++) {
            var item = new JsonObject();
            for (String attribute : attributes) {
                item.addProperty(attribute, String.format("%s %s", attribute, i));
            }
            items.add(item);
        }
        return root;
    }
}