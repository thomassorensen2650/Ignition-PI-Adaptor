import com.inductiveautomation.ignition.common.sqltags.history.Aggregate;
import com.inductiveautomation.ignition.common.sqltags.history.AggregationMode;
import com.unsautomation.ignition.piintegration.PIAggregates;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class PIAggregatesTest {

    @Test
    public void testGetPiAggregate() {
        String piAggregate = PIAggregates.getPiAggregate(AggregationMode.Range);
        assertEquals("Range", piAggregate);
    }

    @Test
    public void testGetPiAggregateNotFound() {
        String piAggregate = PIAggregates.getPiAggregate(new Aggregate() {
            @Override
            public int getId() {
                return -1;
            }

            @Override
            public String getName() {
                return "NonExistentAggregate";
            }

            @Override
            public String getDesc() {
                return "NonExistentAggregate Description";
            }
        });
        assertEquals("Plot", piAggregate);
    }

    @Test
    public void testGetIgnitionAggregate() {
        for (PIAggregates piAggregate : PIAggregates.values()) {
            assertNotNull(piAggregate.getIgnitionAggregate());
        }
    }

    @Test
    public void testGetId() {
        for (PIAggregates piAggregate : PIAggregates.values()) {
            assertEquals(piAggregate.getIgnitionAggregate().getId(), piAggregate.getId());
        }
    }

    @Test
    public void testGetName() {
        for (PIAggregates piAggregate : PIAggregates.values()) {
            assertEquals(piAggregate.getIgnitionAggregate().getName(), piAggregate.getName());
        }
    }

    @Test
    public void testGetDesc() {
        for (PIAggregates piAggregate : PIAggregates.values()) {
            assertEquals(piAggregate.getIgnitionAggregate().getDesc(), piAggregate.getDesc());
        }
    }
}