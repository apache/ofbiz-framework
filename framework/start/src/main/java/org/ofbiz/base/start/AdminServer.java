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
package org.ofbiz.base.start;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.ofbiz.base.start.Start.ServerState;

/**
 * The AdminServer provides a way to communicate with a running
 * OFBiz instance after it has started and send commands to that instance
 * such as inquiring on server status or requesting system shutdown
 */
final class AdminServer extends Thread {

    /**
     * Commands communicated between AdminClient and AdminServer
     */
    enum OfbizSocketCommand {
        SHUTDOWN, STATUS, FAIL
    }

    private ServerSocket serverSocket = null;
    private List<StartupLoader> loaders = null;
    private AtomicReference<ServerState> serverState = null;
    private Config config = null;

    AdminServer(List<StartupLoader> loaders, AtomicReference<ServerState> serverState, Config config) throws StartupException {
        super("OFBiz-AdminServer");
        try {
            this.serverSocket = new ServerSocket(config.adminPort, 1, config.adminAddress);
        } catch (IOException e) {
            throw new StartupException("Couldn't create server socket(" + config.adminAddress + ":" + config.adminPort + ")", e);
        }
        setDaemon(false);
        this.loaders = loaders;
        this.serverState = serverState;
        this.config = config;
    }

    @Override
    public void run() {
        System.out.println("Admin socket configured on - " + config.adminAddress + ":" + config.adminPort);
        while (!Thread.interrupted()) {
            try {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Received connection from - " + clientSocket.getInetAddress() + " : "
                        + clientSocket.getPort());
                processClientRequest(clientSocket, loaders, serverState);
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void processClientRequest(Socket client, List<StartupLoader> loaders, AtomicReference<ServerState> serverState) throws IOException {
        BufferedReader reader = null;
        PrintWriter writer = null;
        try {
            reader = new BufferedReader(new InputStreamReader(client.getInputStream()));
            writer = new PrintWriter(client.getOutputStream(), true);

            executeClientRequest(reader, writer, loaders, serverState);
        } finally {
            if (reader != null) {
                reader.close();
            }
            if (writer != null) {
                writer.flush();
                writer.close();
            }
        }
    }

    private void executeClientRequest(BufferedReader reader, PrintWriter writer,
            List<StartupLoader> loaders, AtomicReference<ServerState> serverState) throws IOException {

        String clientRequest = reader.readLine();
        OfbizSocketCommand clientCommand = determineClientCommand(clientRequest);
        String serverResponse = prepareResponseToClient(clientCommand, serverState);
        
        writer.println(serverResponse);

        if(clientCommand.equals(OfbizSocketCommand.SHUTDOWN)) {
            writer.flush();
            StartupControlPanel.stop(loaders, serverState, this);
        }
    }

    private OfbizSocketCommand determineClientCommand(String request) {
        OfbizSocketCommand clientCommand;
        if(request == null 
                || request.isEmpty()
                || !request.contains(":")
                || !request.substring(0, request.indexOf(':')).equals(config.adminKey)
                || request.substring(request.indexOf(':') + 1) == null) {
            clientCommand = OfbizSocketCommand.FAIL;
        } else {
            clientCommand = OfbizSocketCommand.valueOf(request.substring(request.indexOf(':') + 1));
        }
        return clientCommand;
    }

    private String prepareResponseToClient(OfbizSocketCommand control, AtomicReference<ServerState> serverState) {
        String response = null;
        switch(control) {
            case SHUTDOWN:
                if (serverState.get() == ServerState.STOPPING) {
                    response = "IN-PROGRESS";
                } else {
                    response = "OK";
                }
                break;
            case STATUS:
                response = serverState.get().toString();
                break;
            case FAIL:
                response = "FAIL";
                break;
        }
        return response;
    }
}
