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
package org.apache.ofbiz.base.config;

import java.io.InputStream;
import java.io.Serializable;
import java.net.URL;

import org.w3c.dom.Document;

/**
 * Contains resource information and provides for loading data
 *
 */
public interface ResourceHandler extends Serializable {

    String getLoaderName();

    String getLocation();

    Document getDocument() throws GenericConfigException;

    InputStream getStream() throws GenericConfigException;

    URL getURL() throws GenericConfigException;

    boolean isFileResource() throws GenericConfigException;

    String getFullLocation() throws GenericConfigException;
}
