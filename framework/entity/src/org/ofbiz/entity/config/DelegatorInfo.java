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
package org.ofbiz.entity.config;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.UtilXml;
import org.w3c.dom.Element;

/**
 * Misc. utility method for dealing with the entityengine.xml file
 *
 */
public class DelegatorInfo extends NamedInfo {

    public String entityModelReader;
    public String entityGroupReader;
    public String entityEcaReader;
    public boolean useEntityEca;
    public String entityEcaHandlerClassName;
    public boolean useDistributedCacheClear;
    public String distributedCacheClearClassName;
    public String distributedCacheClearUserLoginId;
    public String sequencedIdPrefix;
    public Map groupMap = new HashMap();

    public DelegatorInfo(Element element) {
        super(element);
        this.entityModelReader = element.getAttribute("entity-model-reader");
        this.entityGroupReader = element.getAttribute("entity-group-reader");
        this.entityEcaReader = element.getAttribute("entity-eca-reader");

        // this defaults to true, ie anything but false is true
        this.useEntityEca = !"false".equalsIgnoreCase(element.getAttribute("entity-eca-enabled"));
        this.entityEcaHandlerClassName = element.getAttribute("entity-eca-handler-class-name");

        // this defaults to false, ie anything but true is false
        this.useDistributedCacheClear = "true".equalsIgnoreCase(element.getAttribute("distributed-cache-clear-enabled"));
        this.distributedCacheClearClassName = element.getAttribute("distributed-cache-clear-class-name");
        if (UtilValidate.isEmpty(this.distributedCacheClearClassName)) this.distributedCacheClearClassName = "org.ofbiz.entityext.cache.EntityCacheServices";
        
        this.distributedCacheClearUserLoginId = element.getAttribute("distributed-cache-clear-user-login-id");
        if (UtilValidate.isEmpty(this.distributedCacheClearUserLoginId)) this.distributedCacheClearUserLoginId= "admin";

        this.sequencedIdPrefix = element.getAttribute("sequenced-id-prefix");
        
        List groupMapList = UtilXml.childElementList(element, "group-map");
        Iterator groupMapIter = groupMapList.iterator();

        while (groupMapIter.hasNext()) {
            Element groupMapElement = (Element) groupMapIter.next();
            groupMap.put(groupMapElement.getAttribute("group-name"), groupMapElement.getAttribute("datasource-name"));
        }
    }
}
