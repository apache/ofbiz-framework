package org.apache.ofbiz.base.util.string;

import java.lang.reflect.Method;

/**
 * Small Uel utility class
 */
public class UelMapping {
    String myKey;
    Method myMethod;
    String myDescription;

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
