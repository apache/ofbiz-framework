package org.ofbiz.base.start;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;

import org.ofbiz.base.start.Start.ServerState;
import org.ofbiz.base.start.StartupControlPanel.OfbizSocketCommand;

final class AdminPortThread extends Thread {
    private ServerSocket serverSocket = null;
    private ArrayList<StartupLoader> loaders = null;
    private AtomicReference<ServerState> serverState = null;
    private Config config = null;

    AdminPortThread(ArrayList<StartupLoader> loaders, AtomicReference<ServerState> serverState, Config config) throws StartupException {
        super("OFBiz-AdminPortThread");
        try {
            this.serverSocket = new ServerSocket(config.adminPort, 1, config.adminAddress);
        } catch (IOException e) {
            throw new StartupException("Couldn't create server socket(" + config.adminAddress + ":" + config.adminPort + ")",
                    e);
        }
        setDaemon(false);
        this.loaders = loaders;
        this.serverState = serverState;
        this.config = config;
    }

    private void processClientRequest(Socket client, ArrayList<StartupLoader> loaders, AtomicReference<ServerState> serverState) throws IOException {
        BufferedReader reader = null;
        PrintWriter writer = null;
        try {
            reader = new BufferedReader(new InputStreamReader(client.getInputStream()));
            String request = reader.readLine();
            writer = new PrintWriter(client.getOutputStream(), true);
            OfbizSocketCommand control;
            if (request != null && !request.isEmpty() && request.contains(":")) {
                String key = request.substring(0, request.indexOf(':'));
                if (key.equals(config.adminKey)) {
                    control = OfbizSocketCommand.valueOf(request.substring(request.indexOf(':') + 1));
                    if (control == null) {
                        control = OfbizSocketCommand.FAIL;
                    }
                } else {
                    control = OfbizSocketCommand.FAIL;
                }
            } else {
                control = OfbizSocketCommand.FAIL;
            }
            switch(control) {
            case SHUTDOWN:
                if (serverState.get() == ServerState.STOPPING) {
                    writer.println("IN-PROGRESS");
                } else {
                    writer.println("OK");
                    writer.flush();
                    StartupControlPanel.stopServer(loaders, serverState, this);
                }
                break;
            case STATUS:
                writer.println(serverState.get());
                break;
            case FAIL:
                writer.println("FAIL");
                break;
            }
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
}
