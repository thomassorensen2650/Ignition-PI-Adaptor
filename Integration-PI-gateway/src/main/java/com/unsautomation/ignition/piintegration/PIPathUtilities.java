package com.unsautomation.ignition.piintegration;

import com.inductiveautomation.ignition.common.QualifiedPath;
import com.inductiveautomation.ignition.common.WellKnownPathTypes;
import com.unsautomation.ignition.piintegration.piwebapi.PIObjectType;

public class PIPathUtilities {

    public static PIObjectType findPathType(QualifiedPath path) {

        var tagPath = path.getPathComponent(WellKnownPathTypes.Tag).toUpperCase();
        var tagParts = tagPath.split("/");

        if (tagPath.equals("ASSETS")) {
            return PIObjectType.AssetsRoot;
        }
        if (tagPath.equals("POINTS")) {
            return PIObjectType.PointsRoot;
        }
        if (tagPath.startsWith("ASSETS") && tagParts.length == 2) {
            return PIObjectType.PIAFServer;
        }
        if (tagPath.startsWith("ASSETS") && tagParts.length == 3) {
            return PIObjectType.PIAFDatabase;
        }
        if (tagPath.startsWith("ASSETS") && tagParts.length > 3) {
            return PIObjectType.PIAFElement;
        }
        if (tagPath.startsWith("POINTS") && tagParts.length == 2) {
            return PIObjectType.PIServer;
        }
        if (tagPath.startsWith("POINTS") && tagParts.length == 3) {
            return PIObjectType.PIPoint;
        }
        return PIObjectType.Unknown;
    }
}
