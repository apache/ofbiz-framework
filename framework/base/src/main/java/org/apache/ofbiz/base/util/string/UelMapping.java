package org.apache.ofbiz.base.util.string;

import java.lang.reflect.Method;

/**
 * Small Uel utility class
 */
public final class UelMapping {

    /**
     * The key of this Uel, often composed by a domain and a name separated by a column
     */
    private final String myKey;

    /**
     * The method that is called by the Uel
     */
    private final Method myMethod;

    /**
     * The description of the Uel that will be displayed in the Uel screen
     */
    private final String myDescription;

    public UelMapping(String key, Method method) {
        myKey = key;
        myMethod = method;
        myDescription = "No description";
    }

    public UelMapping(String key, Method method, String description) {
        myKey = key;
        myMethod = method;
        myDescription = description;
    }

    public String getKey() {
        return myKey;
    }

    public Method getMethod() {
        return myMethod;
    }

    public String getDescription() {
        return myDescription;
    }

}
