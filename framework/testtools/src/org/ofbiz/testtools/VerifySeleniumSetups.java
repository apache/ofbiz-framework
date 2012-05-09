/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *******************************************************************************/
package org.ofbiz.testtools;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilURL;


import javolution.util.FastMap;


import net.sf.json.JSONObject;


public class VerifySeleniumSetups {

    public static final String module = VerifySeleniumSetups.class.getName();
    private static int currentValue  = 0;
    private static int contentLength = 0;
    private static Map<String,Object> josonMap = null;
    // Note: If the download path changes please also the update install-seleniumxml ant target
    private static String urlSite = "http://selenium.googlecode.com/files/selenium-server-standalone-2.0b3.jar";
    private static String libPath = "framework/testtools/lib/selenium-server.jar";

    public VerifySeleniumSetups(){

    }

    /* prepare lib for selenium test
     * Check & download selenium-server.jar from http://downloads.sourceforge.net/seleniumxml/selenium-server-1.0-SNAPSHOT-20081126.jar?use_mirror=
     * Check a change use HTTP as the default at file framework/webapp/config/url.properties
     * Check correct the config/seleniumXml.properties and firefox  path
     */

    public static String verifyConfig(HttpServletRequest request, HttpServletResponse response){
        boolean lib = false;
        boolean urlset = false;
        boolean internetAccess = false;
        Map<String,Object> msgMap = FastMap.newInstance();
        Properties urlProps = null;
        Properties testProps = null;
        try{
            /* Check and download selenium-server.jar */
            URL testProp = UtilURL.fromResource("seleniumXml.properties");
            if (testProps == null) {
                testProps = new Properties();
                if (testProp == null) {
                    String errMsg = "Cannot resolve location from test.properties.";
                    throw new MalformedURLException(errMsg);
                }
            }
            internetAccess = UtilProperties.getPropertyAsBoolean("seleniumXml", "internet.access", true);
            File file = new File(libPath);
            if (internetAccess) {
                URL url = new URL(urlSite);
                URLConnection connection = url.openConnection();
                contentLength = connection.getContentLength();
                if (contentLength == -1) {
                    request.setAttribute("_ERROR_MESSAGE_", "can not conect to the internet");
                }
                Debug.logInfo("file size. "+contentLength,module);
                if (file.exists()) {
                    if (contentLength == file.length()) {
                        lib = true;
                    } else lib = false;
                    msgMap.put("LIBFLAG",lib);
                }
            } else {
                if (file.exists()) {
                    msgMap.put("LIBFLAG",true);
                } else {
                    msgMap.put("LIBFLAG", "noInternet");
                }
            }

            /* Check a change use HTTP as the default */
            String httpStatus = null;
            URL urlProp = UtilURL.fromResource("url.properties");
            if (urlProps == null) {
                urlProps = new Properties();
                if (urlProp == null) {
                    String errMsg = "Cannot resolve location from url.properties.";
                    throw new MalformedURLException(errMsg);
                }
                urlProps = UtilProperties.getProperties(urlProp);
                if (urlProps != null) {
                    httpStatus = urlProps.getProperty("port.https.enabled");
                    if (httpStatus != null && httpStatus.equals("N")) {
                        urlset = true;
                    }
                }
            }
            msgMap.put("URLFLAG",urlset);
            request.setAttribute("MSGMAP", msgMap);
        }catch(Exception e){
            Debug.logError(e.getMessage(), module);
        }
        return "success";
    }

    public  static void doDownload( HttpServletRequest request, HttpServletResponse response){
        URL url = null;
        long bytesRead= 0;
        currentValue  = 0;
        contentLength = 0;
        BufferedInputStream in = null;
        BufferedOutputStream out = null;
        try {
            url = new URL(urlSite);

            URLConnection connection = url.openConnection();
            contentLength = connection.getContentLength();
            InputStream stream = connection.getInputStream();
            Debug.logInfo("getContentLength is :"+contentLength, module);

            in = new BufferedInputStream(stream);

            long totalBytes = in.available();
            Debug.logInfo("totalBytes is : "+totalBytes, module);

            byte[] b = new byte[1024];

            FileOutputStream file = new FileOutputStream(libPath);
            out = new BufferedOutputStream(file);

            int r;
            Debug.logInfo("currentValue is : "+currentValue+" bytes", module);
            while ((r = in.read(b,0,b.length)) != -1) {
                out.write(b,0,r);
                bytesRead += r;
                currentValue = (int)(bytesRead * 100 / contentLength);
                // Debug.logInfo("loading.. :"+bytesRead);
            }
            out.flush();
        } catch (IOException ex) {
            Debug.logError("Error message :"+ex.getMessage(),module);
        }finally {
            try {
                if (in != null) in.close();
                if (out != null) out.close();
            }catch (Exception ex) {
                Debug.logError("Error message :"+ex.getMessage(),module);
            }
        }
    }

    public static String checkProgressDownloadStatus(HttpServletRequest request, HttpServletResponse response){
        if(josonMap == null){
            josonMap = FastMap.newInstance();
        }

        josonMap.put("loadPercent", currentValue);
        josonMap.put("contentLength", contentLength);
        toJsonObject(josonMap,response);

        return "success";
    }

    public static void toJsonObject(Map<String,Object> attrMap, HttpServletResponse response){
        JSONObject json = JSONObject.fromObject(attrMap);
        String jsonStr = json.toString();
        if (jsonStr == null) {
            Debug.logError("JSON Object was empty; fatal error!",module);
        }
        // set the X-JSON content type
        response.setContentType("application/json");
        // jsonStr.length is not reliable for unicode characters
        try {
            response.setContentLength(jsonStr.getBytes("UTF8").length);
        } catch (UnsupportedEncodingException e) {
            Debug.logError("Problems with Json encoding",module);
        }
        // return the JSON String
        Writer out;
        try {
            out = response.getWriter();
            out.write(jsonStr);
            out.flush();
        } catch (IOException e) {
            Debug.logError("Unable to get response writer",module);
        }
    }
}
