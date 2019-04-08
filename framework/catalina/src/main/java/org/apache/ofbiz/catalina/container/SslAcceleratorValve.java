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
package org.apache.ofbiz.catalina.container;

import java.io.IOException;

import javax.servlet.ServletException;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;

/**
 * To use add (or uncomment) the following line to the Tomcat/Catalina configuarion (ie in ofbiz-containers.xml under the <property name="default-server" value="engine"> element)
 *    <property name="ssl-accelerator-port" value="8443"/>
 *
 * Once that is done just setup a connector just like the example http-connector and have it listen on the port you set in the ssl-accelerator-port value.
 */
public class SslAcceleratorValve extends ValveBase {

    protected Integer sslAcceleratorPort = null;

    public void setSslAcceleratorPort(Integer sslAcceleratorPort) {
        this.sslAcceleratorPort = sslAcceleratorPort;
    }

    public Integer getSslAcceleratorPort() {
        return sslAcceleratorPort;
    }

    public @Override void invoke(Request req, Response resp) throws IOException, ServletException {
        if (sslAcceleratorPort != null && req.getLocalPort() == sslAcceleratorPort.intValue()) {
            req.setSecure(true);
        }

        if (getNext() != null) {
            getNext().invoke(req, resp);
        }
    }
}
