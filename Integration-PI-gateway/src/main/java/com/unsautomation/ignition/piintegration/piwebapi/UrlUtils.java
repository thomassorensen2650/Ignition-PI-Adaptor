package com.unsautomation.ignition.piintegration.piwebapi;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;

/***
 * Helper class for HTTP Urls
 */
public class UrlUtils {
    /***
     *
     * @param toEncode
     * @return
     * @throws ApiException
     */
    public static String urlEncode(String toEncode) throws ApiException {
        try {
            return URLEncoder.encode(toEncode, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new ApiException("Error Encoding URL Parameters", e);
        }
    }

    /**
     *
     * @param url
     * @param parameters
     * @return
     * @throws ApiException
     */
    public static String addUrlParameters(String url, Map<String, ? extends Serializable> parameters) throws ApiException {
        for (var parameter : parameters.entrySet()) {
            if (null != parameter.getValue()) {
                url = addUrlParameter(url, parameter.getKey(), parameter.getValue().toString());
            }
        }
        return url;
    }

    /**
     * Adds a URL parameter to the given URL
     *
     * @param url       the URL to which the parameter will be added
     * @param parameter the parameter name
     * @param value     the parameter value
     * @return the URL with the added parameter
     * @throws ApiException if there's an error encoding the URL parameter
     */
    public static String addUrlParameter(String url, String parameter, String value) throws ApiException {
        if (!url.contains("?")) {
            url += "?";
        } else if (!url.endsWith("&") && !url.endsWith("?")) {
            url += "&";
        }
        url += parameter + "=" + urlEncode(value); //String.format("%s%s=%s",url,parameter,value);
        return url;
    }
}
