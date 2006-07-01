/*
 * $Id: FileLoader.java 5462 2005-08-05 18:35:48Z jonesde $
 *
 * Copyright (c) 2001-2005 The Open For Business Project - www.ofbiz.org
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
package org.ofbiz.base.config;

import java.net.*;
import java.io.*;
import org.ofbiz.base.util.*;

/**
 * Loads resources from the file system
 *
 * @author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @version    $Rev$
 * @since      2.0
 */
public class FileLoader extends ResourceLoader implements java.io.Serializable {

    public URL getURL(String location) throws GenericConfigException {
        String fullLocation = fullLocation(location);
        URL fileUrl = null;

        fileUrl = UtilURL.fromFilename(fullLocation);
        if (fileUrl == null) {
            throw new GenericConfigException("File Resource not found: " + fullLocation);
        }
        return fileUrl;
    }
    
    public InputStream loadResource(String location) throws GenericConfigException {
        URL fileUrl = getURL(location);
        try {
            return fileUrl.openStream();
        } catch (java.io.IOException e) {
            throw new GenericConfigException("Error opening file at location [" + fileUrl.toExternalForm() + "]", e);
        }
    }
}
