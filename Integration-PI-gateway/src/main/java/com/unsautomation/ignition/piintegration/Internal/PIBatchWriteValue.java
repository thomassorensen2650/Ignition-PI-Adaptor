package com.unsautomation.ignition.piintegration.Internal;

import com.inductiveautomation.ignition.gateway.history.HistoricalTagValue;
import java.util.Date;

public class PIBatchWriteValue {

    private Date timestamp;
    private String tagname;
    private Object value;
    private String dataType;

    public PIBatchWriteValue(String tagname, Date timestamp, Object value, String dataType, Boolean isGood) {
        this.tagname = tagname;
        this.timestamp = timestamp;
        this.value = value;
        this.dataType = dataType;
    }
    public static PIBatchWriteValue fromHistoricalTagValue(HistoricalTagValue record) {

        var tagName = record.getSource().toStringPartial();
        var value = record.getValue();
        var timestamp = record.getTimestamp(); //FIXME : Is Epic or Zule the right way to send data
        var isGood = record.getQuality().isGood();
        var dataType = record.getTypeClass().name();
        return new PIBatchWriteValue(tagName, timestamp, value, dataType, true);

    }

    public String getTagname() { return tagname; }
    public Date getTimestamp() { return timestamp; }
    public Object getValue() { return value; }

    public String getDataType() { return dataType; }

}
