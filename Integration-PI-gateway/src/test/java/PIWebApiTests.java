import com.unsautomation.ignition.piintegration.piwebapi.ApiException;
import com.unsautomation.ignition.piintegration.piwebapi.PIWebApiClient;
import com.unsautomation.ignition.piintegration.piwebapi.WebIdUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.joda.time.DateTime;
import org.junit.Test;

import java.util.Date;

public class PIWebApiTests {

    PIWebApiClient c;

    public PIWebApiTests() throws ApiException {
        c = new PIWebApiClient("https://192.168.1.213/piwebapi/", "t", "t", false, true);
    }

    @Test
    public void testAFServerWebID() throws Exception {
        var test = "Assets/DESKTOP-M7O4A7D";
        var d0 = WebIdUtils.toWebID(test);
        var r0 = c.getAssetServer().getAssetDatabases(d0);
        if (r0.size() < 1) {
            throw new Exception("AF Server not found");
        }
    }

    @Test
    public void testAFDBWebID() throws Exception {
        var test = "Assets/DESKTOP-M7O4A7D/Database2";
        var d1 = WebIdUtils.toWebID(test);
        var r1 = c.getAssetDatabase().getElements(d1);
        System.out.print(r1);
        if (r1.size() < 1) {
            throw new Exception("AF Server not found");
        }
    }

    @Test
    public void testAFElementWebID() throws Exception {
        var test = "Assets/DESKTOP-M7O4A7D/Database2/Element1";
        var d1 = WebIdUtils.toWebID(test);
        var r1 = c.getElementApi().getElements(d1);
        System.out.print(r1);
        if (r1.size() < 1) {
            throw new Exception("AF Server not found");
        }
    }

    @Test
    public void testPIServers() throws Exception {
        var r1 = c.getDataServer().list(null);
        System.out.print(r1);
        if (r1.size() < 1) {
            throw new Exception("AF Server not found");
        }
    }

    @Test
    public void testListPITags() throws Exception {
        var test = "Points/DESKTOP-M7O4A7D";
        var d1 = WebIdUtils.toWebID(test);
        var r1 = c.getDataServer().getPoints(d1, null,0,1000,null);
        System.out.print(r1);
        if (r1.size() < 1) {
            throw new Exception("AF Server not found");
        }
    }



    // Test : piClient.getStream().getRecorded(t.toString(), startDate, endDate, null,null,null);

    /***
     * Test that values are in range and the correct number to events are returned???
     * @throws Exception
     */
    @Test
    public void testGetStream1() throws Exception {
        var test = "Points/DESKTOP-M7O4A7D/PI Tag";
        var startTime = DateUtils.addHours(new Date(), -1);
        var endTime = new Date();
        var d1 = WebIdUtils.toWebID(test);
        var r1 = c.getStream().getPlot(d1, startTime,endTime,100l, null,null, null);
        var size = r1.size();

        if (size < 1) {
            throw new Exception("AF Server not found");
        }
    }

    @Test
    public void testGetStreamRaw() throws Exception {
        var test = "Points/DESKTOP-M7O4A7D/PI Tag";
        var startTime = DateUtils.addHours(new Date(), -1);
        var endTime = new Date();
        var d1 = WebIdUtils.toWebID(test);
        var r1 = c.getStream().getRecorded(d1, startTime,endTime,null,null, null);
        var size = r1.size();

        if (size < 1) {
            throw new Exception("AF Server not found");
        }
    }

    @Test
    public void xxx() throws Exception {
        var x = "ASASD/DESKTOP-M7O4A7D/DATABASE2/ELEMENT1/ATTRIBUTE1";

        var index = x.lastIndexOf('/');
        x = x.substring(0, index)
                + "|"
                + x.substring(index + 1);



        var y = WebIdUtils.toWebID("E","Ab", x);

    }


    // Test that attributes can be returned in Plot
    // Check that we have attributes encoded with a @
    // check that a attribute that starts with @ works.

}
