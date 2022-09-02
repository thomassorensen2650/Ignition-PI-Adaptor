import com.inductiveautomation.ignition.gateway.model.GatewayContext;
import com.unsautomation.ignition.piintegration.IPIHistoryProviderSettings;
import com.unsautomation.ignition.piintegration.PIHistoryProvider;
import com.unsautomation.ignition.piintegration.PIHistoryProviderSettings;
import com.unsautomation.ignition.piintegration.piwebapi.ApiException;
import org.junit.Test;

import java.net.URISyntaxException;

public class IgnitionClassTests {

    class TestSettings implements IPIHistoryProviderSettings {

        @Override
        public String getWebAPIUrl() {
            return "http://localhost/piwebapi";
        }

        @Override
        public String getUsername() {
            return "a";
        }

        @Override
        public String getPassword() {
            return "b";
        }

        @Override
        public boolean getVerifySSL() {
            return false;
        }

        @Override
        public String getBrowsableAFServers() {
            return "*";
        }

        @Override
        public String getBrowsablePIServers() {
            return "*";
        }

        @Override
        public boolean getOnlyBrowsePITagsWithPrefix() {
            return false;
        }

        @Override
        public String getPITagPrefix() {
            return "prefix.";
        }

        @Override
        public String getPIArchiver() {
            return "archiver";
        }
    }
    private final PIHistoryProvider hp;
    private final IPIHistoryProviderSettings settings;


    public IgnitionClassTests() throws URISyntaxException, ApiException {
        settings = new TestSettings();

        //var webApi = System.getProperty("PIWebAPIUrl","https://ignitionadxpoc.eastus.kusto.windows.net");
        //settings.setString(PIHistoryProviderSettings.PIWebAPIUrl, webApi);


        hp = new PIHistoryProvider((GatewayContext) null, "test", settings);
    }

    @Test
    public void testGetAggregates() throws Exception {

        

       //var agg = hp.getAvailableAggregates();

    }

}
