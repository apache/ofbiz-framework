/*
 * $Id: OFBizHomeLocationResolver.java 5462 2005-08-05 18:35:48Z jonesde $
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

import org.ofbiz.base.util.UtilURL;

/**
 * A special location resolver that uses Strings like URLs, but with more options 
 *
 *@author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 *@version    $Rev$
 *@since      3.1
 */

public class OFBizHomeLocationResolver implements LocationResolver {
    
    public static final String envName = "ofbiz.home";
    
    public URL resolveLocation(String location) throws MalformedURLException {
        String propValue = System.getProperty(envName);
        if (propValue == null) {
            String errMsg = "The Java environment (-Dxxx=yyy) variable with name " + envName + " is not set, cannot resolve location.";
            throw new MalformedURLException(errMsg);
        }

        StringBuffer baseLocation = new StringBuffer(FlexibleLocation.stripLocationType(location));
        
        // if there is not a forward slash between the two, add it
        if (baseLocation.charAt(0) != '/' && propValue.charAt(propValue.length()) != '/') {
            baseLocation.insert(0, '/');
        }
        
        baseLocation.insert(0, propValue);
        
        return UtilURL.fromFilename(baseLocation.toString());
    }
}
