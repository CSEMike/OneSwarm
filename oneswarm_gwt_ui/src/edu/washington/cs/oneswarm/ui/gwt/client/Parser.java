package edu.washington.cs.oneswarm.ui.gwt.client;

import java.util.HashMap;
import java.util.Map;

import com.google.gwt.user.client.Window;

public class Parser {
    public static boolean isAcceptableString(String str) {
        char[] c = str.toCharArray();
        for (int i = 0; i < c.length; i++) {
            if (!isSafeChar(c[i])) {
                return false;
            }
        }
        return true;

    }

    public static Map<String, String> parseURLParameters() {
        return parseParameters(getURL());
    }

    public static Map<String, String> parseParameters(String url) {

        Map<String, String> map = new HashMap<String, String>();

        if (url.indexOf('?') == -1) {
            return map;
        }
        String parameterString = url.substring(url.lastIndexOf('?') + 1);

        String[] parameterPairs = parameterString.split("&");

        for (int i = 0; i < parameterPairs.length; i++) {
            String parameterPair = parameterPairs[i];
            String name = parameterPair.substring(0, parameterPair.indexOf('='));

            String value = parameterPair.substring(parameterPair.indexOf('=') + 1);
            // Window.alert(parameterPair + " '" + name + "'= '" + value + "'");
            if (isAcceptableString(name) && isAcceptableString(value)) {
                map.put(name, value);
            } else {
                Window.alert("Sorry, all parameters must be url-encoded:\n\n" + "'" + name + "'='"
                        + value + "'\n\n" + "is not a valid url encoded string, skipping '" + name
                        + "'");
            }
        }

        return map;
    }

    // check if the char is safe, all characters that are possible in
    // URL-encoded strings are ok.
    // this means that all safe chars, and % is ok
    public static boolean isSafeChar(char c) {
        return ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')
                || c == '-' || c == '_' || c == '.' || c == '*' || c == '%');
    }

    private static native String getURL() /*-{
                                          return parent.location.href
                                          //return window.location.href
                                          }-*/;

}
