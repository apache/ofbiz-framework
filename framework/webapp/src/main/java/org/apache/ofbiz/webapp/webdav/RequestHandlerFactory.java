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
package org.apache.ofbiz.webapp.webdav;

public interface RequestHandlerFactory {
    /** Returns a <code>RequestHandler<code> instance appropriate
     * for the WebDAV HTTP method.
     *
     * @param method The WebDAV HTTP method. Implementations MUST
     * provide handlers for the following methods: PROPFIND, PROPPATCH,
     * MKCOL, GET, HEAD, POST, DELETE, PUT, COPY, MOVE, LOCK, UNLOCK.
     * @return A <code>RequestHandler</code> instance. Implementations
     * of this interface MUST NOT return <code>null</code>.
     */
    public RequestHandler getHandler(String method);
}
