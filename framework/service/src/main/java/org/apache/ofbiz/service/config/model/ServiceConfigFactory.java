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
package org.apache.ofbiz.service.config.model;

import org.apache.ofbiz.base.config.GenericConfigException;
import org.apache.ofbiz.base.config.XmlFileReader;
import org.w3c.dom.Element;

public class ServiceConfigFactory {
    private static ServiceConfig instance = null;

    private static final String SERVICE_ENGINE_XML_FILENAME = "serviceengine.xml";

    public static ServiceConfig createInstance() throws GenericConfigException {
        Element serviceConfigElement = XmlFileReader.read(SERVICE_ENGINE_XML_FILENAME);

        instance = ServiceConfig.create(serviceConfigElement);
        return instance;
    }

    public static ServiceConfig getInstance() throws GenericConfigException {
        if (instance == null) {
            instance = createInstance();
        }
        return instance;
    }

}
