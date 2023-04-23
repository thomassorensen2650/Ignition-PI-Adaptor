import com.inductiveautomation.ignition.gateway.model.GatewayContext;
import com.unsautomation.ignition.piintegration.PIHistoryProvider;
import com.unsautomation.ignition.piintegration.PIHistoryProviderSettings;
import com.unsautomation.ignition.piintegration.piwebapi.ApiException;
import org.junit.Test;

import java.net.URISyntaxException;

public class IgnitionClassTests {


    private final PIHistoryProvider hp = null;
    private final PIHistoryProviderSettings settings;

    public IgnitionClassTests()  {
        settings = new PIHistoryProviderSettings();

        var webApi = System.getProperty("PIWebAPIUrl","https://localhost/piwebapi");
        settings.setString(PIHistoryProviderSettings.PIWebAPIUrl, webApi);
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

    @Test
    public void testGetAggregates()  {
        var api = settings.getWebAPIUrl();


        

       //var agg = hp.getAvailableAggregates();

    }

}
