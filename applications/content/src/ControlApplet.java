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

import java.applet.Applet;
import java.applet.AppletContext;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Control Applet - Client applet for page pushing and (future) chat
 */
public class ControlApplet extends Applet implements Runnable {
    
    private static String pushUrl = "/commonapp/control/pushapplet";
    private static String pullUrl = "/commonapp/control/pullapplet";
    
    protected AppletContext ctx = null;
    
    protected String sessionId = null;
    protected String visitId = null;
    protected String serverUrl = null;     
    protected String timeoutUrl = null;
    protected String currentPage = null;    
    protected String debug = null;
    
    protected int waitTime = 1500;
    protected int timeout = 300000;
    
    protected boolean isRunning = false;
    protected boolean stopped = true;
    
    protected Thread thread = null;
    
    public void init() {
        ctx = this.getAppletContext();
        this.sessionId = this.getParameter("sessionId");
        this.visitId = this.getParameter("visitId");
        this.serverUrl = this.getParameter("serverUrl");        
        this.timeoutUrl = this.getParameter("timeoutUrl");  
        this.currentPage = this.getParameter("currentPage");      
        this.debug = this.getParameter("debug");
        
        // see if we override the integer values
        try {
            int waitInt = Integer.parseInt(this.getParameter("waitTime"));
            if (waitInt > 0)
                waitTime = waitInt;            
        } catch (NumberFormatException e) {
        }
        try {
            int outInt = Integer.parseInt(this.getParameter("timeout"));
            if (outInt > 0) 
                timeout = outInt;
        } catch (NumberFormatException e) {
        }
                
        if (serverUrl != null) {
            boolean sessionOkay = false;
            boolean visitOkay = false;
            boolean pageOkay = false;         
            
            if (sessionId != null && sessionId.length() > 0)
                sessionOkay = true;
            if (visitId != null && visitId.length() > 0)
                visitOkay = true;  
            if (currentPage != null && currentPage.length() > 0)    
                pageOkay = true;           
             
            if (sessionOkay && visitOkay && pageOkay) {                    
                // tell the host about our current page (mainly for followers)
                this.push();
            }
        
            // start the polling thread
            this.isRunning = true;
            this.stopped = false;
            thread = new Thread(this);
            thread.setDaemon(false);    
            thread.start();
        }            
    }
    
    public void destroy() {
        this.stopped = true;       
    }
    
    // poll the servlet for page request
    public void run() {                  
        while (isRunning && !stopped) {
            this.pull();
            try {
                Thread.sleep(waitTime);
            } catch (InterruptedException e) {
                this.stopped = true;
                this.isRunning = false;
            }                                
        }
        if (debug != null && debug.equalsIgnoreCase("true"))
            System.out.println("Polling finished.");
    }
    
    protected void pull() {     
        Map params = new HashMap();
        params.put("sessionId", this.sessionId.trim());
        params.put("visitId", this.visitId.trim());
                            
        String pullResp = null;
        URL callPullUrl = null;
        try {
            callPullUrl = new URL(serverUrl + pullUrl);
        } catch (MalformedURLException e) {
        }
        
        if (callPullUrl != null) {
            try {
                pullResp = callServer(callPullUrl, params);
            } catch (IOException e) {
            }      
        }
        
        if (pullResp != null && pullResp.length() > 0) {
            URL docUrl = null;
            try {
                docUrl = new URL(pullResp);
            } catch (MalformedURLException e) {
            }
            if (docUrl != null) 
                ctx.showDocument(docUrl, "appletWindow");
        }
    }
    
    protected void push() {
        Map params = new HashMap();  
        params.put("sessionId", this.sessionId.trim());
        params.put("visitId", this.visitId.trim());   
        params.put("currentPage", this.currentPage.trim());      
        
        String pushResp = null;
        URL callPushUrl = null;
        try {
            callPushUrl = new URL(serverUrl + pushUrl);      
        } catch (MalformedURLException e) {
        }
        
        if (callPushUrl != null) {
            try {
                pushResp = callServer(callPushUrl, params);
            } catch (IOException e) {
            }
        }
    }
    
    private String callServer(URL serverUrl, Map parms) throws IOException {    
        // send the request   
        String parameters = this.encodeArgs(parms);   
        if (debug != null && debug.equalsIgnoreCase("true")) 
            System.out.println("Sending parameters: " + parameters);                      
        URLConnection uc = serverUrl.openConnection();
        uc.setDoOutput(true);
        uc.setRequestProperty("Content-Type","application/x-www-form-urlencoded");
        PrintWriter pw = new PrintWriter(uc.getOutputStream());
        pw.println(parameters);
        pw.close();
        
        // read the response
        BufferedReader in = new BufferedReader(new InputStreamReader(uc.getInputStream()));
        String responseStr = in.readLine();
        in.close();
        if (responseStr != null)
            responseStr = responseStr.trim();
        if (debug != null && debug.equalsIgnoreCase("true"))
            System.out.println("Receive response: " + responseStr);                     
        return responseStr;                 
    }
    
    public String encodeArgs(Map args) {
        StringBuffer buf = new StringBuffer();
        if (args != null) {
            Iterator i = args.entrySet().iterator();
            while (i.hasNext()) {
                Map.Entry entry = (Map.Entry) i.next();
                String name = (String) entry.getKey();
                Object value = entry.getValue();
                String valueStr = null;
                if (name != null && value != null) {
                    if (value instanceof String) {
                        valueStr = (String) value;
                    } else {
                        valueStr = value.toString();
                    }
                    
                    if (valueStr != null && valueStr.length() > 0) {
                        if (buf.length() > 0) buf.append('&');
                        try {
                            buf.append(URLEncoder.encode(name, "UTF-8"));
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();                        
                        }
                        buf.append('=');
                        try {
                            buf.append(URLEncoder.encode(valueStr, "UTF-8"));
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();                        
                        }
                    }
                }
            }
        }
        return buf.toString();
    }          
}
