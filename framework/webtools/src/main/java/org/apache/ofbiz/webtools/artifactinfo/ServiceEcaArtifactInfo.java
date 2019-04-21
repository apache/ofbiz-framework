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
package org.apache.ofbiz.webtools.artifactinfo;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.ofbiz.base.location.FlexibleLocation;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.service.eca.ServiceEcaAction;
import org.apache.ofbiz.service.eca.ServiceEcaCondition;
import org.apache.ofbiz.service.eca.ServiceEcaRule;

/**
 *
 */
public class ServiceEcaArtifactInfo extends ArtifactInfoBase {
    protected ServiceEcaRule serviceEcaRule;
    protected String displayPrefix = null;
    protected int displaySuffixNum = 0;

    protected Set<ServiceArtifactInfo> servicesCalledByThisServiceEca = new TreeSet<>();

    public ServiceEcaArtifactInfo(ServiceEcaRule serviceEcaRule, ArtifactInfoFactory aif) throws GeneralException {
        super(aif);
        this.serviceEcaRule = serviceEcaRule;
    }

    /**
     * This must be called after creation from the ArtifactInfoFactory after this class has been put into the global Map in order to avoid recursive initialization
     *
     * @throws GeneralException
     */
    public void populateAll() throws GeneralException {
        // populate the services called Set
        for (ServiceEcaAction ecaAction: serviceEcaRule.getEcaActionList()) {
            servicesCalledByThisServiceEca.add(aif.getServiceArtifactInfo(ecaAction.getServiceName()));
            UtilMisc.addToSortedSetInMap(this, aif.allServiceEcaInfosReferringToServiceName, ecaAction.getServiceName());
        }
    }

    @Override
    public String getDisplayName() {
        return this.getDisplayPrefixedName();
    }

    @Override
    public String getDisplayType() {
        return "Service ECA";
    }

    @Override
    public String getType() {
        return ArtifactInfoFactory.ServiceEcaInfoTypeId;
    }

    @Override
    public String getUniqueId() {
        return this.serviceEcaRule.toString();
    }

    @Override
    public URL getLocationURL() throws MalformedURLException {
        return FlexibleLocation.resolveLocation(serviceEcaRule.getDefinitionLocation());
    }

    public ServiceEcaRule getServiceEcaRule() {
        return this.serviceEcaRule;
    }

    public void setDisplayPrefix(String displayPrefix) {
        this.displayPrefix = displayPrefix;
    }

    public void setDisplaySuffixNum(int displaySuffixNum) {
        this.displaySuffixNum = displaySuffixNum;
    }

    public String getDisplayPrefixedName() {
        return (this.displayPrefix != null ? this.displayPrefix : "") + this.serviceEcaRule.getServiceName() + "_" + this.serviceEcaRule.getEventName() + "_" + displaySuffixNum;
    }

    public Set<ServiceArtifactInfo> getServicesCalledByServiceEcaActions() {
        return this.servicesCalledByThisServiceEca;
    }

    public Set<ServiceArtifactInfo> getServicesTriggeringServiceEca() {
        return aif.allServiceInfosReferringToServiceEcaRule.get(this.serviceEcaRule);
    }

    public Map<String, Object> createEoModelMap(Set<ServiceArtifactInfo> triggeringServiceSet, Set<ServiceArtifactInfo> triggeredServiceSet, boolean useMoreDetailedNames) {
        if (triggeringServiceSet == null) triggeringServiceSet = new HashSet<>();
        if (triggeredServiceSet == null) triggeredServiceSet = new HashSet<>();
        Map<String, Object> topLevelMap = new HashMap<>();

        topLevelMap.put("name", this.getDisplayPrefixedName());
        topLevelMap.put("className", "EOGenericRecord");

        // for classProperties add attribute names AND relationship names to get a nice, complete chart
        List<String> classPropertiesList = new LinkedList<>();
        topLevelMap.put("classProperties", classPropertiesList);
        // conditions
        for (ServiceEcaCondition ecaCondition: this.serviceEcaRule.getEcaConditionList()) {
            classPropertiesList.add(ecaCondition.getShortDisplayDescription(useMoreDetailedNames));
        }
        // actions
        for (ServiceEcaAction ecaAction: this.serviceEcaRule.getEcaActionList()) {
            if (useMoreDetailedNames) {
                classPropertiesList.add(ecaAction.getShortDisplayDescription());
            } else {
                classPropertiesList.add(ecaAction.getServiceName());
            }
        }

        /* going to try this without any attributes...
        // attributes
        List<Map<String, Object>> attributesList = new LinkedList();
        topLevelMap.put("attributes", attributesList);
        for (ModelParam param: this.modelService.getModelParamList()) {
            Map<String, Object> attributeMap = new HashMap();
            attributesList.add(attributeMap);

            if (useMoreDetailedNames) {
                attributeMap.put("name", param.getShortDisplayDescription());
            } else {
                attributeMap.put("name", param.name);
            }
            attributeMap.put("valueClassName", param.type);
            attributeMap.put("externalType", param.type);
        }
        */

        // relationships
        List<Map<String, Object>> relationshipsMapList = new LinkedList<>();

        for (ServiceArtifactInfo sai: triggeringServiceSet) {
            Map<String, Object> relationshipMap = new HashMap<>();
            relationshipsMapList.add(relationshipMap);

            relationshipMap.put("name", sai.getDisplayPrefixedName());
            relationshipMap.put("destination", sai.getDisplayPrefixedName());
            relationshipMap.put("isToMany", "N");
            relationshipMap.put("isMandatory", "Y");
        }
        for (ServiceArtifactInfo sai: triggeredServiceSet) {
            Map<String, Object> relationshipMap = new HashMap<>();
            relationshipsMapList.add(relationshipMap);

            relationshipMap.put("name", sai.getDisplayPrefixedName());
            relationshipMap.put("destination", sai.getDisplayPrefixedName());
            relationshipMap.put("isToMany", "Y");
            relationshipMap.put("isMandatory", "Y");
        }

        if (relationshipsMapList.size() > 0) {
            topLevelMap.put("relationships", relationshipsMapList);
        }

        return topLevelMap;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ServiceEcaArtifactInfo) {
            ServiceEcaArtifactInfo that = (ServiceEcaArtifactInfo) obj;
            return this.serviceEcaRule.equals(that.serviceEcaRule);
        } else {
            return false;
        }
    }
}
