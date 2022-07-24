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
            toEncode = URLEncoder.encode(toEncode, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            throw new ApiException("Error Encoding URL Parameters", e);
        }
        return toEncode;
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
                url = addUrlParameter(url, parameter.getKey(), urlEncode(parameter.getValue().toString()));
            }
        }
        return url;
    }

    /***
     *
     * @param url
     * @param parameter
     * @param value
     * @return
     */
    public static String addUrlParameter(String url, String parameter, String value) {
        if (!url.contains("?")) {
            url += "?";
        } else if (!url.endsWith("&") && !url.endsWith("?")) {
            url += "&";
        }
        url += parameter + "=" + value; //String.format("%s%s=%s",url,parameter,value);
        return url;
    }
}
