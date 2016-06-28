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

package org.ofbiz.example;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.ofbiz.base.util.Debug;

@ServerEndpoint("/ws/pushNotifications")
public class ExampleWebSockets {

    public static final String module = ExampleWebSockets.class.getName();
    private static Set<Session> clients = Collections.synchronizedSet(new HashSet<Session>());
    

    @OnMessage
    public void onMessage(Session session, String msg, boolean last) {
        try {
            if (session.isOpen()) {
                synchronized (clients) {
                    for(Session client : clients){
                        if (!client.equals(session)){
                            client.getBasicRemote().sendText(msg);
                        }
                    }
                }
            }
        } catch (IOException e) {
            try {
                session.close();
            } catch (IOException ioe) {
                Debug.logError(ioe.getMessage(), module);
            }
        }
    }

    @OnOpen
    public void onOpen (Session session) {
        // Add session to the connected sessions clients set
        clients.add(session);
    }

    @OnClose
    public void onClose (Session session) {
        // Remove session from the connected sessions clients set
        clients.remove(session);
    }

    public static Set<Session> getClients () {
        return clients;
    }
}

