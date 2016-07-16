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
package org.apache.ofbiz.accounting.thirdparty.eway;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.ofbiz.base.util.Debug;

/**
 * Handles connections to the eWay servers.
 * 
 * Based on public domain sample code provided by eWay.com.au
 */
public class GatewayConnector {
    
    private static final String module = GatewayConnector.class.getName();
    
    private int timeout = 0;

    public GatewayConnector(int timeout) {
        this.timeout = timeout;
    }
    
    public GatewayConnector() {
        this(60);
    }
    
    /**
     * Get the timeout value set in the corresponding setter.
     * @return timeout value in seconds, 0 for infinite
     */
    public int getTimeout() {
        return timeout;
    }

    /**
     * Set the timout value. Note that setting the timeout for an HttpURLConnection
     * is possible only since Java 1.5. This method has no effect on earlier
     * versions.
     * @param time timeout value in seconds, 0 for infinite
     */
    public void setTimeout(int time) {
        timeout = time;
    }

    /**
     * Send a request to the payment gateway and get the response. This is a
     * blocking method: when it returns the response object is filled in with
     * the parameters from the gateway.
     * @param request the request object, can be any of the 3 supported payment
     * methods. Its data have to be filled in by its setter methods before
     * calling sendRequest().
     * @return the response object, containing the gateway's response to the 
     * request
     * @throws Exception in case of networking and xml parsing errors 
     */
    public GatewayResponse sendRequest(GatewayRequest request) throws Exception {
        
        // determine the gateway url to be used, based on the request type
        String serverurl = request.getUrl();        
        
        GatewayResponse response = null;
        InputStream in = null;
        HttpURLConnection connection = null;
        try {
            // connect to the gateway
            URL u = new URL(serverurl);
            connection = (HttpURLConnection)(u.openConnection());
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setRequestMethod("POST");            
            connection.setConnectTimeout(timeout*1000);            
            
            OutputStream out = connection.getOutputStream();
            Writer wout = new OutputStreamWriter(out);
            wout.write(request.toXml());
            wout.flush();
            wout.close();

            in = connection.getInputStream();
            response = new GatewayResponse(in, request);
            return response;
        } 
        catch (Exception e) {
            // rethrow exception so that the caller learns what went wrong
            Debug.logError(e, e.getMessage(), module);
            throw e;
        }
        finally {
            // close resources
            if (in != null) in.close();
            if (connection != null) connection.disconnect();
        }
    }
}