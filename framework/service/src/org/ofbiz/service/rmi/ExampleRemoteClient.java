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
package org.ofbiz.service.rmi;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;

import org.ofbiz.service.GenericServiceException;

/** An example of how to remotely access the Service Engine's RemoteDispatcher.
 *
 * The following files from OFBiz need to be on the client's classpath:
 * cache.properties
 * debug.properties
 * jsse.properties
 * ofbiz-base.jar
 * ofbiz-service-rmi.jar (copied and renamed from "ofbiz/framework/service/build/lib/ofbiz-service-rmi.raj" from an OFBiz build)
 *
 * The following third-party libraries (can be found in OFBiz) also need to be on the client's classpath:
 * commons-collections.jar
 * jdbm.jar
 * log4j.jar
 *
 * Copy the truststore file framework/base/config/ofbizrmi-truststore.jks to the client
 *
 * Run the client specifying the path to the client truststore: -Djavax.net.ssl.trustStore=ofbizrmi-truststore.jks
 *
 */
public class ExampleRemoteClient {

    protected final static String RMI_URL = "rmi://localhost:1099/RMIDispatcher"; // change to match the remote server
    protected RemoteDispatcher rd = null;

    public ExampleRemoteClient() {
        try {
            rd = (RemoteDispatcher) Naming.lookup(RMI_URL);
        } catch (NotBoundException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public Map<String, Object> runTestService() throws RemoteException, GenericServiceException {
        Map<String, Object> context = new HashMap<String, Object>();
        context.put("message", "Remote Service Test");
        return rd.runSync("testScv", context);
    }

    public static void main(String[] args) throws Exception {
        ExampleRemoteClient rm = new ExampleRemoteClient();
        Map<String, Object> result = rm.runTestService();
        System.out.println("Service Result Map: " + result);
    }
}
