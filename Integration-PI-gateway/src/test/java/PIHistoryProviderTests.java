
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

import static org.junit.jupiter.api.Assertions.*;
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
        lenient().when(webApiClient.doGet("")).thenThrow(new ApiException(404, "Test123"));
        assertEquals(ProfileStatus.ERRORED, historyProvider.getStatus());
    }

    @Test
    public void getStatusErrorTest2() throws ApiException {
        // Test API Timeout
        historyProvider.piClient.apiClient = actualWebClient;
        assertEquals(ProfileStatus.ERRORED, historyProvider.getStatus());

        // Test Server API Error
        historyProvider.piClient.apiClient = webApiClient;
        lenient().when(webApiClient.doGet("")).thenThrow(new ApiException(500, "Test123"));
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
        assertEquals(QualityCode.Good, result.getResultQuality());
        assertEquals(2, Objects.requireNonNull(result.getResults()).size());
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

    @Test
    public void browseAFServersFilterTest() throws ApiException {
        var validResponse = simulateWebApiItemsResponse(new String[]{"WebId", "Name"}, 5); // TODO: Get a https://docs.aveva.com/bundle/pi-web-api-reference/page/help/controllers/assetserver/actions/list.html
        lenient().when(webApiClient.doGet(startsWith("assetservers"))).thenReturn(new PIResponse(200, validResponse));
        lenient().when(settings.getBrowsableAFServers()).thenReturn("Name 1, Name 2");
        var p = new QualifiedPath.Builder()
                .set(WellKnownPathTypes.HistoryProvider, "Hi")
                .setTag("Assets")
                .build();
        var result = historyProvider.browse(p, null);
        assertEquals(QualityCode.Good, result.getResultQuality());
        assertEquals(2, result.getReturnedSize());
    }

    @Test
    public void browseAFServersBadResponseTest() throws ApiException {
        try {
            var p = new QualifiedPath.Builder()
                    .set(WellKnownPathTypes.HistoryProvider, "Hi")
                    .setTag("Assets")
                    .build();

            historyProvider.piClient.apiClient = this.actualWebClient;
            // Timeout
            var result = historyProvider.browse(p, null);
            assertEquals(QualityCode.Bad_Failure, result.getResultQuality());
            assertEquals(0, result.getReturnedSize());

            historyProvider.piClient.apiClient = this.webApiClient;

            // 404
            lenient().when(webApiClient.doGet(startsWith("assetservers"))).thenThrow(new ApiException(404, "Test123"));
            result = historyProvider.browse(p, null);
            assertEquals(QualityCode.Bad_Failure, result.getResultQuality());
            assertEquals(0, result.getReturnedSize());

            // 500
            lenient().when(webApiClient.doGet(startsWith("assetservers"))).thenThrow(new ApiException(500, "Test123"));
            result = historyProvider.browse(p, null);
            assertEquals(QualityCode.Bad_Failure, result.getResultQuality());
            assertEquals(0, result.getReturnedSize());

        } catch (ApiException ex) {
            assertFalse(false);
        }

    }

    @Test
    public void browseAFDBsTest() {

        var validResponse = simulateWebApiItemsResponse(new String[]{"WebId", "Name"}, 10); // TODO: Get a https://docs.aveva.com/bundle/pi-web-api-reference/page/help/controllers/assetserver/actions/list.html
        try {
            lenient().when(webApiClient.doGet(eq("assetservers/P1RSTVkgU0VSVkVS/assetdatabases?selectedFields=Items.Name"))).thenReturn(new PIResponse(200, validResponse));
        } catch (ApiException e) {
            throw new RuntimeException(e);
        }
        var p = new QualifiedPath.Builder()
                .set(WellKnownPathTypes.HistoryProvider, "Hi")
                .setTag("Assets/My Server")
                .build();
        var result = historyProvider.browse(p, null);
        assertEquals(QualityCode.Good, result.getResultQuality());
        assertEquals(10, result.getReturnedSize());
    }
    @Test
    public void browseAFDBBadResponseTest() throws ApiException {
        lenient().when(webApiClient.doGet(eq("assetservers/P1RSTVkgU0VSVkVS/assetdatabases?selectedFields=Items.Name"))).thenReturn(new PIResponse(200, new JsonObject()));
        var p = new QualifiedPath.Builder()
                .set(WellKnownPathTypes.HistoryProvider, "Hi")
                .setTag("Assets/My Server")
                .build();

        // Timeout
        var result = historyProvider.browse(p, null);
        assertEquals(QualityCode.Bad_Failure, result.getResultQuality());
        assertEquals(0, result.getReturnedSize());

        // 404
        lenient().when(webApiClient.doGet(eq("assetservers/P1RSTVkgREI/assetdatabases?selectedFields=Items.Name"))).thenThrow(new ApiException(404, "Test123"));
        result = historyProvider.browse(p, null);
        assertEquals(QualityCode.Bad_Failure, result.getResultQuality());
        assertEquals(0, result.getReturnedSize());
    }
    @Test
    public void browseAFDBBadResponse2Test() throws ApiException {
        var p = new QualifiedPath.Builder()
                .set(WellKnownPathTypes.HistoryProvider, "Hi")
                .setTag("Assets/My Server")
                .build();
        // 500
        lenient().when(webApiClient.doGet(startsWith("assetservers"))).thenThrow(new ApiException(500, "Test123"));
        var result = historyProvider.browse(p, null);
        assertEquals(QualityCode.Bad_Failure, result.getResultQuality());
        assertEquals(0, result.getReturnedSize());
    }

    @Test
    public void browseElementsTest() {
        var validElementResponse = simulateWebApiItemsResponse(new String[]{"WebId", "Name"}, 10);
        var validAttributeResponse = simulateWebApiItemsResponse(new String[]{"WebId", "Name"}, 5);

        try {
            lenient().when(webApiClient.doGet(eq("elements/P1EmTVkgU0VSVkVSXE1ZIERCXFJBTkRPTVxFTEVNRU5U/elements"))).thenReturn(new PIResponse(200, validElementResponse));
            lenient().when(webApiClient.doGet(eq("elements/P1EmTVkgU0VSVkVSXE1ZIERCXFJBTkRPTVxFTEVNRU5U/attributes"))).thenReturn(new PIResponse(200, validAttributeResponse));
        } catch (ApiException e) {
            throw new RuntimeException(e);
        }
        var p = new QualifiedPath.Builder()
                .set(WellKnownPathTypes.HistoryProvider, "Hi")
                .setTag("Assets/My Server/My DB/Random/Element")
                .build();
        var result = historyProvider.browse(p, null);
        assertEquals(QualityCode.Good, result.getResultQuality());
        assertEquals(15, result.getReturnedSize());

        for (var item : result.getResults()) {
            if(item.hasChildren()) {
                // Element
                assertEquals(item.getPath(), item.getDisplayPath());
            }else {
                // Attribute values are actually webids but display path should be correct path
                assertTrue(item.getPath().toString().contains("WebId"));
                assertTrue(item.getDisplayPath().toString().contains("Name"));
                assertNotEquals(item.getPath(), item.getDisplayPath());
            }
        }
    }

    // ++++++++ Helper functions ++++++++
    // TODO: Test Invalid cert
    // TODO: Test Invalid cert with cert ignore
    //
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