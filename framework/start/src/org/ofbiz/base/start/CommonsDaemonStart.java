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


/**
 * Special startup class for Commons Daemon users.
 * 
 * @see <a href="http://commons.apache.org/proper/commons-daemon/jsvc.html">Commons Daemon</a>
 */
public final class CommonsDaemonStart {

    public CommonsDaemonStart() {
    }

    public void init(String[] args) throws StartupException {
        Start.getInstance().init(StartupCommandUtil.parseOfbizCommands(args));
    }

    public void destroy() {
        // FIXME: undo init() calls.
    }

    public void start() throws Exception {
        Start.getInstance().start();
    }

    public void stop() {
        Start.getInstance().shutdownServer();
    }
}
