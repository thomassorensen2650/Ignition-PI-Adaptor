import com.inductiveautomation.ignition.gateway.model.GatewayContext;
import com.unsautomation.ignition.piintegration.PIHistoryProvider;
import com.unsautomation.ignition.piintegration.PIHistoryProviderSettings;
import com.unsautomation.ignition.piintegration.piwebapi.ApiException;
import org.junit.Test;

import java.net.URISyntaxException;

public class IgnitionClassTests {


    private final PIHistoryProvider hp;
    private final PIHistoryProviderSettings settings;


    public IgnitionClassTests() throws URISyntaxException, ApiException {
        settings = new PIHistoryProviderSettings();

        //var webApi = System.getProperty("PIWebAPIUrl","https://ignitionadxpoc.eastus.kusto.windows.net");
        //settings.setString(PIHistoryProviderSettings.PIWebAPIUrl, webApi);


        hp = new PIHistoryProvider((GatewayContext) null, "test", settings);
    }

    @Test
    public void testGetAggregates() throws Exception {

        

       //var agg = hp.getAvailableAggregates();

    }

}
