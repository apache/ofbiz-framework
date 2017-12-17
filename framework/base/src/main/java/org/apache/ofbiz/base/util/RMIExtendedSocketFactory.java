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
package org.apache.ofbiz.base.util;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.rmi.server.RMISocketFactory;

/**
 * A <code>RMISocketFactory</code> implementation that creates <code>ServerSocket</code>s bound
 * on a specified network interface.
 */
public class RMIExtendedSocketFactory extends RMISocketFactory {

    /**
     * The network interface to bind the <code>ServerSocket</code> to. If null than bind to all interfaces.
     */
    private InetAddress hostInetAddress;

    /**
     * Default constructor. Bind the server sockets on all interfaces.
     */
    public RMIExtendedSocketFactory() {
        // leave hostInetAddress null
    }

    /**
     * Creates a new <code>RMIExtendedSocketFactory</code> which will create <code>ServerSocket</code>s
     * bound on the specified network interface.
     *
     * @param inetAddress The <code>InetAddress</code> of the network interface.
     */
    public RMIExtendedSocketFactory( InetAddress inetAddress ) {
        this.hostInetAddress = inetAddress;
    }

    /**
     * Creates a new <code>RMIExtendedSocketFactory</code> which will create <code>ServerSocket</code>s
     * bound on the specified network interface.
     *
     * @param hostIpAddress The IP address of the interface to bind the server sockets to.
     * @throws UnknownHostException If an invalid IP address is provided.
     */
    public RMIExtendedSocketFactory( String hostIpAddress ) throws UnknownHostException {

        // check if host length is at least equal to "0.0.0.0"
        if ( hostIpAddress != null && hostIpAddress.length() >= 7 ) {
            String[] octets = hostIpAddress.split( "\\." );

            if (octets.length != 4) {
                throw new UnknownHostException( "Invalid IP address: " + hostIpAddress );
            }

            byte[] ipAddr = new byte[4];
            for ( int i = 0; i < octets.length; i++ ) {
                try {
                    ipAddr[i] = ( byte ) Integer.parseInt( octets[i] );
                } catch ( NumberFormatException nfEx ) {
                    throw new UnknownHostException( "Invalid IP address: " + hostIpAddress );
                }
            }

            hostInetAddress = InetAddress.getByAddress( ipAddr );

        }

    }

    @Override
    public ServerSocket createServerSocket(int port) throws IOException {
        if ( hostInetAddress !=  null ) {
            return new ServerSocket( port, 0, hostInetAddress );
        }
        return new ServerSocket( port );
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException {

        return new Socket( host, port );
    }

}
