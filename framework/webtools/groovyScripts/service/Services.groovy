/*
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
 */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.sql.Timestamp;

import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.service.ServiceDispatcher;
import org.ofbiz.service.RunningService;
import org.ofbiz.service.engine.GenericEngine;
import org.ofbiz.base.util.UtilHttp;
import org.ofbiz.base.util.UtilProperties;

uiLabelMap = UtilProperties.getResourceBundleMap("WebtoolsUiLabels", locale);
uiLabelMap.addBottomResourceBundle("CommonUiLabels");

log = ServiceDispatcher.getServiceLogMap();
serviceList = [];
log.each { rs, value ->
    service = [:];
    service.serviceName = rs.getModelService().name;
    service.localName = rs.getLocalName();
    service.startTime = rs.getStartStamp();
    service.endTime = rs.getEndStamp();
    service.modeStr = rs.getMode() == GenericEngine.SYNC_MODE ? uiLabelMap.WebtoolsSync : uiLabelMap.WebtoolsAsync;

    serviceList.add(service);
}
sortField = parameters.sortField;
if (sortField) { 
    context.services = UtilMisc.sortMaps(serviceList, UtilMisc.toList(sortField));
} else {
    context.services = serviceList;
}
