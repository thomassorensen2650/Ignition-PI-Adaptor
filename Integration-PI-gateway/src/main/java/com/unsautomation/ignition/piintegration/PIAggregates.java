package com.unsautomation.ignition.piintegration;

import com.inductiveautomation.ignition.common.sqltags.history.Aggregate;
import com.inductiveautomation.ignition.common.sqltags.history.AggregateInfo;
import com.inductiveautomation.ignition.common.sqltags.history.AggregationMode;

public enum PIAggregates implements Aggregate {
    PI_PLOT(AggregationMode.MinMax, "Plot"),
    PI_INTERPOLATED(new AggregateInfo(Aggregate.CUSTOM_RESERVED_ID + 100, "Interpolated", "Interpolated Data"), "Interpolated"),
    PI_RANGE(AggregationMode.Range, "Range"),
    PI_AVERAGE(AggregationMode.SimpleAverage, "Average"),
    PI_STD_DEV(AggregationMode.StdDev, "StdDev"),
    PI_MAXIMUM(AggregationMode.Maximum, "Maximum"),
    PI_MINIMUM(AggregationMode.Minimum, "Minimum"),
    PI_COUNT(AggregationMode.Count, "Count"),
    PI_PERCENT_GOOD(AggregationMode.PctGood, "PercentGood");

    private final Aggregate ignitionAggregate;
    private final String piAggregate;

    PIAggregates(Aggregate ignitionAggregate, String piAggregate) {
        this.ignitionAggregate = ignitionAggregate;
        this.piAggregate = piAggregate;
    }

    public String getPiAggregate() {
        return this.piAggregate;
    }

    public Aggregate getIgnitionAggregate() {
        return ignitionAggregate;
    }

    public static String getPiAggregate(Aggregate aggregate) {
        for (PIAggregates piAggregate : PIAggregates.values()) {
            if (piAggregate.getIgnitionAggregate().equals(aggregate)) {
                return piAggregate.getPiAggregate();
            }
        }
        return "Plot";
    }
    @Override
    public int getId() {
        return ignitionAggregate.getId();
    }

    @Override
    public String getName() {
        return ignitionAggregate.getName();
    }

    @Override
    public String getDesc() {
        return ignitionAggregate.getDesc();
    }
}