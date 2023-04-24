
import com.inductiveautomation.ignition.common.QualifiedPath;
import com.inductiveautomation.ignition.common.WellKnownPathTypes;
import com.inductiveautomation.ignition.common.browsing.Result;
import com.inductiveautomation.ignition.common.gson.JsonArray;
import com.inductiveautomation.ignition.common.gson.JsonObject;
import com.inductiveautomation.ignition.common.model.values.QualityCode;
import com.inductiveautomation.ignition.gateway.model.ProfileStatus;
import com.unsautomation.ignition.piintegration.PIAggregates;
import com.unsautomation.ignition.piintegration.PIHistoryProvider;
import com.unsautomation.ignition.piintegration.PIHistoryProviderSettings;
import com.unsautomation.ignition.piintegration.piwebapi.ApiClient;
import com.unsautomation.ignition.piintegration.piwebapi.ApiException;
import com.unsautomation.ignition.piintegration.piwebapi.PIResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
public class PIHistoryProviderTests {

    final String providerName = "Test PI Provider";

    PIHistoryProvider historyProvider = null;

    @Mock ApiClient webApiClient;

    @Mock ApiClient actualWebClient;

    @Mock PIHistoryProviderSettings settings;

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


        // Custom WebAPI Implementation that returned simulated data.
        // Not sure this is the best way to test.
        webApiClient = mock(ApiClient.class);
        actualWebClient = historyProvider.piClient.apiClient;
        historyProvider.piClient.apiClient = webApiClient;
    }

    @Test
    public void getNameTest() {
        assertEquals(historyProvider.getName(), providerName);
    }

    @Test
    public void getAvailableAggregatesTest() {
        var expected = Arrays.stream(PIAggregates.values()).map(PIAggregates::getIgnitionAggregate).collect(Collectors.toList());
        assertEquals(historyProvider.getAvailableAggregates(), expected);
    }

    @Test
    public void getStatusErrorTest() throws ApiException {
        // Test API Timeout
        historyProvider.piClient.apiClient = actualWebClient;
        assertEquals(ProfileStatus.ERRORED, historyProvider.getStatus());

        // Test Server API Error
        historyProvider.piClient.apiClient = webApiClient;
        lenient().when(webApiClient.doGet("")).thenReturn(new PIResponse(500, null));
        assertEquals(ProfileStatus.ERRORED, historyProvider.getStatus());

        // Test Client HTTP Server
        lenient().when(webApiClient.doGet("")).thenReturn(new PIResponse(404, null));
        assertEquals(ProfileStatus.ERRORED, historyProvider.getStatus());
    }

    @Test
    public void getStatusSuccessTest() throws ApiException {

        // Simulate good response.
        lenient().when(webApiClient.doGet(anyString())).thenReturn(new PIResponse(200, new JsonObject()));

        // Force update
        assertEquals(historyProvider.getStatus(), ProfileStatus.RUNNING);
    }

    @Test
    public void browseRootResultsTest() {

        var root = new QualifiedPath();
        var result = historyProvider.browse(root, null); // Browse root should return result even without PI WebAPI Access
        assertEquals(result.getResultQuality(), QualityCode.Good);
        assertEquals(Objects.requireNonNull(result.getResults()).size(), 2);
        var results = result.getResults().toArray(new Result[2]);

        assertEquals("Assets", results[0].getPath().getPathComponent(WellKnownPathTypes.Tag));
        assertEquals("Points", results[1].getPath().getPathComponent(WellKnownPathTypes.Tag));

        // Browse Asset Root
        // Browse invalid path and verify that a BAD request is returned and not a
    }

    @Test
    public void browseInvalidPathTest() {
        var p = new QualifiedPath.Builder()
                .set(WellKnownPathTypes.HistoryProvider, "Hi")
                .setTag("IM_INVALID")
                .build();
        var result = historyProvider.browse(p, null);
        assertEquals(QualityCode.Bad_Failure, result.getResultQuality());
    }

    @Test
    public void browseAFServersTest() throws ApiException {

        var validResponse = simulateWebApiItemsResponse(new String[]{"WebId", "Name"}, 5); // TODO: Get a https://docs.aveva.com/bundle/pi-web-api-reference/page/help/controllers/assetserver/actions/list.html
        lenient().when(webApiClient.doGet(startsWith("assetservers"))).thenReturn(new PIResponse(200, validResponse));
        var p = new QualifiedPath.Builder()
                .set(WellKnownPathTypes.HistoryProvider, "Hi")
                .setTag("Assets")
                .build();
        var result = historyProvider.browse(p, null);
        assertEquals(QualityCode.Good, result.getResultQuality());
        assertEquals(5, result.getReturnedSize());
    }

    public void browseAFServersFilterTest() {
        // Test basic browse
    }

    public void browseAFServersBadResponseTest() {
        // Test that a 500 or 404 Api response is handled correctly
        // also check API timeout.
    }

    @Test
    public void filterListTest() {
        //TODO:
    }

    // TODO: Test Invalid cert
    // TODO: Test Invalid cert with cert ignore
    //
    // Old Stuff

    public PIHistoryProviderTests()  {

        //

       /* settings.setBoolean(PIHistoryProviderSettings.SimulationMode, true);
        settings.setString(PIHistoryProviderSettings.Username, "test");
        settings.setString(PIHistoryProviderSettings.Password, "test");
        settings.setInt(PIHistoryProviderSettings.APIRequestPageSize, 100);
        settings.setString(PIHistoryProviderSettings.BrowsableAFServers, "");
        settings.setString(PIHistoryProviderSettings.BrowsablePIServers, "");
        settings.setBoolean(PIHistoryProviderSettings.IgnoreSSLIssues, true);
*/
        //hp = new PIHistoryProvider((GatewayContext) null, "test", settings);
    }


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