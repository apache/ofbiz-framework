/*
 * Copyright 2007 Bruce Fancher
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * This file is a modified version of the original one.
 */

package org.ofbiz.base.container.groovy;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GroovyShellService extends GroovyService {

    private ServerSocket serverSocket;
    private int port;
    private List<Socket> clientSockets = new ArrayList<Socket>();

    public GroovyShellService() {
        super();
    }

    public GroovyShellService(int port) {
        super();
        this.port = port;
    }

    public GroovyShellService(Map<String, Object> bindings, int port) {
        super(bindings);
        this.port = port;
    }

    @Override
    public void launch() {
        try {
            serverSocket = new ServerSocket(port);
            log.info("Opened server socket: " + serverSocket);

            acceptClientConnections();
        } catch (IOException e) {
            log.error("Could not open server socket.", e);
        } finally {
            closeServerSocket();
        }
    }

    @Override
    public void destroy() {
        closeServerSocket();

        for (Socket clientSocket : clientSockets)  {
            closeClientSocket(clientSocket);
        }
    }

    public void setPort(int port) {
        this.port = port;
    }

    private void acceptClientConnections() {
        while (true) {
            Socket clientSocket = null;
            try {
                clientSocket = serverSocket.accept();
                log.info("Opened client socket: " + clientSocket);
            } catch (IOException e) {
                log.error("Could not open client socket.", e);
                continue;
            }

            clientSockets.add(clientSocket);
            GroovyShellThread clientThread = new GroovyShellThread(clientSocket, createBinding());
            clientThread.start();
        }
    }

    private void closeServerSocket() {
        try {
            if (serverSocket != null) {
                log.info("Closing server socket: " + serverSocket);
                serverSocket.close();
            }
        } catch (IOException e) {
            log.error("Could not close server socket: " + serverSocket, e);
        }
    }

    private void closeClientSocket(Socket socket) {
        try {
            if (socket != null) {
                log.info("Closing client socket: " + socket);
                socket.close();
            }
        } catch (IOException e) {
            log.error("Could not close client socket: " + socket, e);
        }
    }
}
