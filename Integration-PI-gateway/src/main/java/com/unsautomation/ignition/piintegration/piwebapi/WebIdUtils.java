package com.unsautomation.ignition.piintegration.piwebapi;

import com.inductiveautomation.ignition.common.QualifiedPath;
import com.inductiveautomation.ignition.common.WellKnownPathTypes;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

public class WebIdUtils {

    public static String toWebID(QualifiedPath path) throws ApiException {

        var tagPath = path.getPathComponent(WellKnownPathTypes.Tag).toUpperCase();

        var isValid = tagPath != null && tagPath.split("/").length > 1 &&
                (tagPath.startsWith("Assets/") || tagPath.startsWith("Points/"));
        var marker = "";
        var ownerMarker = "";

        if (isValid) {

            var tagParts = tagPath.split("/");

            if (tagParts[0].equals("ASSETS") && tagParts.length == 2) {
                // Encoding a Asset Server ID
                marker = "RS";
            } else if (tagParts[0].equals("ASSETS") && tagParts.length == 3) {
                // Encoding Asset DB
                ownerMarker = "RS";
                marker = "RD";
            } else if (tagParts[0].equals("ASSETS")) {
                // AF Element
                ownerMarker = "RD";
                marker = "Em";
            } else if (tagParts[0].equals("POINTS") && tagParts.length == 2) {
                // Encoding a Asset Server ID
                marker = "RS";
            } else if (tagParts[0].equals("POINTS") && tagParts.length == 3) {
                // Encoding Asset DB
                ownerMarker = "RS";
                marker = "RD";
            } else if (tagParts[0].equals("POINTS")) {
                // AF Element
                ownerMarker = "RD";
                marker = "Em";
            }
        } else {
            throw new ApiException("Unable to encode tagpath : " + tagPath);
        }

        tagPath = tagPath.substring(tagPath.indexOf('/'));
        var encodedPath = encode(tagPath);

        return "P1" + marker + ownerMarker + encodedPath;
    }

    public static QualifiedPath toQualifiedPath() {
        return null;
    }


    public static String encode(String value)
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
