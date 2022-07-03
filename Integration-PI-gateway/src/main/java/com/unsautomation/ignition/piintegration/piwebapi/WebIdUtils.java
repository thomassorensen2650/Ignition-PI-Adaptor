package com.unsautomation.ignition.piintegration.piwebapi;

import com.inductiveautomation.ignition.common.QualifiedPath;
import com.inductiveautomation.ignition.common.WellKnownPathTypes;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Locale;
import java.util.UUID;

public class WebIdUtils {

    public static String tagToWebId(String dataServer, String tagName) {
        return null;
    }

    public static String dataServerToWebId(String dataServer) {
        return null;
    }

    public static String toWebID(String tagPath) throws ApiException, UnsupportedEncodingException {

        tagPath = tagPath.toUpperCase();
        var isValid = tagPath != null && tagPath.split("/").length > 1 &&
                (tagPath.startsWith("ASSETS/") || tagPath.startsWith("POINTS/"));
        var marker = "";
        var ownerMarker = "";

        if (isValid) {

            var tagParts = tagPath.split("/");
            var tagType = tagParts[0].toUpperCase();

            if (tagType.equals("ASSETS") && tagParts.length == 2) {
                // Encoding a Asset Server ID
                marker = "RS";
            } else if (tagType.equals("ASSETS") && tagParts.length == 3) {
                // Encoding Asset DB
                //ownerMarker = "R";
                ownerMarker = "";
                marker = "RD";
            } else if (tagType.equals("ASSETS")) {
                // AF Element
                //ownerMarker = "RD";
                ownerMarker = "";
                marker = "Em";
            } else if (tagType.equals("POINTS") && tagParts.length == 2) {
                // Encoding a PI Server
                marker = "DS";
                ownerMarker = "";
            } else if (tagType.equals("POINTS") && tagParts.length == 3) {
                // Encoding Asset DB
                ownerMarker = "";// "DS";
                marker = "DP";
            } else if (tagType.equals("POINTS")) {
                // AF Element
                ownerMarker = "RD";
                marker = "Em";
            } else {
                throw new ApiException("unknown tagpath : " + tagPath);
            }
        } else {
            throw new ApiException("Unable to encode tagpath : " + tagPath);
        }

        tagPath = tagPath.substring(tagPath.indexOf('/')+1).replace('/', '\\');
        tagPath = tagPath.toUpperCase();
        //tagPath = tagPath.replace("\\", "\\\\");

        var encodedPath = encode(tagPath);
        return "P1" + marker + ownerMarker + encodedPath;
    }


    public static String toWebID(QualifiedPath path) throws ApiException, UnsupportedEncodingException {
        var tagPath = path.getPathComponent(WellKnownPathTypes.Tag).toUpperCase();
        return toWebID(tagPath);

    }



    public static String encode(String value) throws UnsupportedEncodingException
    {

        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        return encode(bytes);
    }

    public static String encode(byte[] bytes)
    {
        String value =  Base64.getEncoder().encodeToString(bytes);
        value = trimStringByString(value, "=");
        return value.replace('+', '-').replace('/', '_');
    }

    public static String encode(UUID value)
    {
        byte[] bytes = value.toString().getBytes();
        return encode(bytes);
    }

    private static String trimStringByString(String text, String trimBy) {
        int beginIndex = 0;
        int endIndex = text.length();

        while (text.substring(beginIndex, endIndex).startsWith(trimBy)) {
            beginIndex += trimBy.length();
        }

        while (text.substring(beginIndex, endIndex).endsWith(trimBy)) {
            endIndex -= trimBy.length();
        }

        return text.substring(beginIndex, endIndex);
    }
}
