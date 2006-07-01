/*
 * $Id: ComponentLocationResolver.java 5462 2005-08-05 18:35:48Z jonesde $
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

import org.ofbiz.base.component.ComponentConfig;
import org.ofbiz.base.component.ComponentException;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilURL;

/**
 * A special location resolver that uses Strings like URLs, but with more options 
 *
 *@author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 *@version    $Rev$
 *@since      3.1
 */

public class ComponentLocationResolver implements LocationResolver {

    public static final String module = ComponentLocationResolver.class.getName();

    public URL resolveLocation(String location) throws MalformedURLException {
        StringBuffer baseLocation = new StringBuffer(FlexibleLocation.stripLocationType(location));
        
        // componentName is between the first slash and the second
        int firstSlash = baseLocation.indexOf("/");
        int secondSlash = baseLocation.indexOf("/", firstSlash + 1);
        if (firstSlash != 0 || secondSlash == -1) {
            throw new MalformedURLException("Bad component location [" + location + "]: base location missing slashes [" + baseLocation + "], first=" + firstSlash + ", second=" + secondSlash + "; should be like: component://{component-name}/relative/path");
        }
        String componentName = baseLocation.substring(firstSlash + 1, secondSlash);
        
        // got the componentName, now remove it from the baseLocation, removing the second slash too (just in case the rootLocation has one)
        baseLocation.delete(0, secondSlash + 1);

        String rootLocation = null;
        try {
            rootLocation = ComponentConfig.getRootLocation(componentName);
        } catch (ComponentException e) {
            String errMsg = "Could not get root location for component with name [" + componentName + "], error was: " + e.toString(); 
            Debug.logError(e, errMsg, module);
            throw new MalformedURLException(errMsg);
        }

        // if there is not a forward slash between the two, add it
        if (baseLocation.charAt(0) != '/' && rootLocation.charAt(rootLocation.length() - 1) != '/') {
            baseLocation.insert(0, '/');
        }

        // insert the root location and we're done
        baseLocation.insert(0, rootLocation);

        URL fileUrl = UtilURL.fromFilename(baseLocation.toString());
        
        if (fileUrl == null) {
            Debug.logWarning("Unable to get file URL for component location; expanded location was [" + baseLocation + "], original location was [" + location + "]", module);
        }
        
        return fileUrl;
    }
}
