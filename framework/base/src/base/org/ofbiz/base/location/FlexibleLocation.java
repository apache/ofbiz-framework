/*
 * $Id: FlexibleLocation.java 5462 2005-08-05 18:35:48Z jonesde $
 *
 * Copyright (c) 2004 The Open For Business Project - www.ofbiz.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 * CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT
 * OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 * THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */
package org.ofbiz.base.location;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.ofbiz.base.util.UtilProperties;

/**
 * A special location resolver that uses Strings like URLs, but with more options 
 *
 *@author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 *@version    $Rev$
 *@since      3.1
 */

public class FlexibleLocation {
    
    protected static Map locationResolvers = new HashMap();
    
    protected static Map defaultResolvers = new HashMap();
    
    protected static String standardUrlResolverName = StandardUrlLocationResolver.class.getName();
    protected static String classpathResolverName = ClasspathLocationResolver.class.getName();
    protected static String ofbizHomeResolverName = OFBizHomeLocationResolver.class.getName();
    protected static String componentResolverName = ComponentLocationResolver.class.getName();
    
    static {
        defaultResolvers.put("http", standardUrlResolverName);
        defaultResolvers.put("https", standardUrlResolverName);
        defaultResolvers.put("ftp", standardUrlResolverName);
        defaultResolvers.put("jar", standardUrlResolverName);
        defaultResolvers.put("file", standardUrlResolverName);

        defaultResolvers.put("classpath", classpathResolverName);
        defaultResolvers.put("ofbizhome", ofbizHomeResolverName);
        defaultResolvers.put("component", componentResolverName);
    }
    
    /**
     * Resolves the gives location into a URL object for use in various ways.
     * 
     * The general format of the location is like a URL: {locationType}://location/path/file.ext
     * 
     * Supports standard locationTypes like http, https, ftp, jar, & file
     * Supports a classpath location type for when desired to be used like any other URL
     * Supports OFBiz specific location types like ofbizhome and component
     * Supports additional locationTypes specified in the locationresolvers.properties file
     *  
     * @param location The location String to parse and create a URL from
     * @return URL object corresponding to the location String passed in
     * @throws MalformedURLException
     */
    public static URL resolveLocation(String location) throws MalformedURLException {
        return resolveLocation(location, null);
    }
    
    public static URL resolveLocation(String location, ClassLoader loader) throws MalformedURLException {
        if (location == null || location.length() == 0) {
            return null;
        }
        String locationType = getLocationType(location);
        
        LocationResolver resolver = (LocationResolver) locationResolvers.get(locationType);
        if (resolver == null) {
            synchronized (FlexibleLocation.class) {
                resolver = (LocationResolver) locationResolvers.get(locationType);
                if (resolver == null) {
                    String locationResolverName = UtilProperties.getPropertyValue("locationresolvers", locationType);
                    if (locationResolverName == null || locationResolverName.length() == 0) {
                        // try one of the defaults
                        locationResolverName = (String) defaultResolvers.get(locationType);
                    }

                    if (locationResolverName == null || locationResolverName.length() == 0) {
                        // still nothing, give up
                        throw new MalformedURLException("Could not find a LocationResolver class name for the location type: " + locationType);
                    }

                    // now create a new instance of the class...
                    try {
                        Class lClass = null;

                        try {
                            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
                            lClass = classLoader.loadClass(locationResolverName);
                        } catch (ClassNotFoundException e) {
                            throw new MalformedURLException("Error loading Location Resolver class \"" + locationResolverName + "\": " + e.toString());
                        }

                        try {
                            resolver = (LocationResolver) lClass.newInstance();
                        } catch (IllegalAccessException e) {
                            throw new MalformedURLException("Error loading Location Resolver class \"" + locationResolverName + "\": " + e.toString());
                        } catch (InstantiationException e) {
                            throw new MalformedURLException("Error loading Location Resolver class \"" + locationResolverName + "\": " + e.toString());
                        }
                    } catch (SecurityException e) {
                        throw new MalformedURLException("Error loading Location Resolver class \"" + locationResolverName + "\": " + e.toString());
                    }
                    
                    if (resolver != null) {
                        locationResolvers.put(locationType, resolver);
                    }
                }
            }
        }
        
        if (resolver != null) {
            if (loader != null && resolver instanceof ClasspathLocationResolver) {
                ClasspathLocationResolver cplResolver = (ClasspathLocationResolver) resolver;
                return cplResolver.resolveLocation(location, loader);
            } else {
                return resolver.resolveLocation(location);
            }
        } else {
            throw new MalformedURLException("Could not find a LocationResolver for the location type: " + locationType);
        }
    }
    
    /** 
     * Find the location type descriptor for the passed location String; 
     *   generally is all text before the first ":" character.
     *   If no type descriptor is found, defaults to "classpath".
     */
    public static String getLocationType(String location) {
        if (location == null || location.length() == 0) {
            return null;
        }
        
        int colonIndex = location.indexOf(":");
        if (colonIndex > 0) {
            return location.substring(0, colonIndex);
        } else {
            return "classpath";
        }
    }
    
    public static String stripLocationType(String location) {
        if (location == null || location.length() == 0) {
            return "";
        }
        
        StringBuffer strippedSoFar = new StringBuffer(location);
        
        // first take care of the colon and everything before it
        int colonIndex = strippedSoFar.indexOf(":");
        if (colonIndex == 0) {
            strippedSoFar.deleteCharAt(0);
        } else if (colonIndex > 0) {
            strippedSoFar.delete(0, colonIndex + 1);
        }
        
        // now remove any extra forward slashes, ie as long as the first two are forward slashes remove the first one
        while (strippedSoFar.charAt(0) == '/' && strippedSoFar.charAt(1) == '/') {
            strippedSoFar.deleteCharAt(0);
        }
        
        return strippedSoFar.toString();
    }
}
