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
package org.ofbiz.webtools.artifactinfo;

import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javolution.util.FastList;
import javolution.util.FastMap;
import javolution.util.FastSet;

import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilFormatOut;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.ModelParam;
import org.ofbiz.service.ModelService;
import org.ofbiz.webtools.artifactinfo.ArtifactInfoFactory.ArtifactInfoContext;

/**
 *
 */
public class ServiceArtifactInfo {
    protected ArtifactInfoContext aic;
    protected ModelService modelService;
    protected String displayPrefix = null;
    
    public ServiceArtifactInfo(String serviceName, ArtifactInfoContext aic) throws GenericServiceException {
        this.aic = aic;
        this.modelService = this.aic.getModelService(serviceName);
    }
    
    public ModelService getModelService() {
        return this.modelService;
    }
    
    public void setDisplayPrefix(String displayPrefix) {
        this.displayPrefix = displayPrefix;
    }
    
    public String getDisplayPrefixedName() {
        return (this.displayPrefix != null ? this.displayPrefix : "") + this.modelService.name;
    }
    
    public List<EntityArtifactInfo> getEntitiesUsedByService() {
        List<EntityArtifactInfo> entityList = FastList.newInstance();
        // TODO: implement this
        return entityList;
    }
    
    public List<ServiceArtifactInfo> getServicesCallingService() {
        List<ServiceArtifactInfo> serviceList = FastList.newInstance();
        // TODO: *implement this
        return serviceList;
    }
    
    public List<ServiceArtifactInfo> getServicesCalledByService() {
        List<ServiceArtifactInfo> serviceList = FastList.newInstance();
        // TODO: *implement this
        return serviceList;
    }
    
    public List<ServiceArtifactInfo> getServicesCalledByServiceEcas() {
        List<ServiceArtifactInfo> serviceList = FastList.newInstance();
        // TODO: implement this
        return serviceList;
    }
    
    public List<ServiceEcaArtifactInfo> getServiceEcaRulesTriggeredByService() {
        List<ServiceEcaArtifactInfo> secaList = FastList.newInstance();
        // TODO: *implement this
        return secaList;
    }
    
    public List<ServiceArtifactInfo> getServicesCallingServiceByEcas() {
        List<ServiceArtifactInfo> serviceList = FastList.newInstance();
        // TODO: implement this
        return serviceList;
    }
    
    public List<ServiceEcaArtifactInfo> getServiceEcaRulesCallingService() {
        List<ServiceEcaArtifactInfo> secaList = FastList.newInstance();
        // TODO: *implement this
        return secaList;
    }
    
    public List<FormWidgetArtifactInfo> getFormsCallingService() {
        List<FormWidgetArtifactInfo> formList = FastList.newInstance();
        // TODO: implement this
        return formList;
    }
    
    public List<FormWidgetArtifactInfo> getFormsBasedOnService() {
        List<FormWidgetArtifactInfo> formList = FastList.newInstance();
        // TODO: implement this
        return formList;
    }
    
    public List<ScreenWidgetArtifactInfo> getScreensCallingService() {
        List<ScreenWidgetArtifactInfo> screenList = FastList.newInstance();
        // TODO: implement this
        return screenList;
    }
    
    public List<ScreenWidgetArtifactInfo> getRequestsWithEventCallingService() {
        List<ScreenWidgetArtifactInfo> screenList = FastList.newInstance();
        // TODO: implement this
        return screenList;
    }
    
    public void writeServiceCallGraphEoModel(String eomodeldFullPath) throws GeneralException, FileNotFoundException, UnsupportedEncodingException {
        // TODO: add support for parameters with recursion: int callingHops, int calledHops,
        boolean useMoreDetailedNames = true;
        
        Set<String> allDiagramEntitiesWithPrefixes = FastSet.newInstance();
        List<ServiceArtifactInfo> allServiceList = FastList.newInstance(); 
        List<ServiceEcaArtifactInfo> allServiceEcaList = FastList.newInstance();
        
        // all services that call this service
        List<ServiceArtifactInfo> callingServiceList = this.getServicesCallingService();
        
        // set the prefix and add to the all list
        for (ServiceArtifactInfo callingService: callingServiceList) {
            callingService.setDisplayPrefix("Calling:");
            allDiagramEntitiesWithPrefixes.add(callingService.getDisplayPrefixedName());
            allServiceList.add(callingService);
        }
        
        // all services this service calls
        List<ServiceArtifactInfo> calledServiceList = this.getServicesCalledByService();
        
        for (ServiceArtifactInfo calledService: calledServiceList) {
            calledService.setDisplayPrefix("Called:");
            allDiagramEntitiesWithPrefixes.add(calledService.getDisplayPrefixedName());
            allServiceList.add(calledService);
        }
        
        // all SECAs and triggering services that call this service as an action
        List<ServiceEcaArtifactInfo> callingServiceEcaList = this.getServiceEcaRulesCallingService();
        
        for (ServiceEcaArtifactInfo callingServiceEca: callingServiceEcaList) {
            callingServiceEca.setDisplayPrefix("Triggering:");
            allDiagramEntitiesWithPrefixes.add(callingServiceEca.getDisplayPrefixedName());
            allServiceEcaList.add(callingServiceEca);
        }

        // all SECAs and corresponding services triggered by this service
        List<ServiceEcaArtifactInfo> calledServiceEcaList = this.getServiceEcaRulesTriggeredByService();
        
        for (ServiceEcaArtifactInfo calledServiceEca: calledServiceEcaList) {
            calledServiceEca.setDisplayPrefix("Called:");
            allDiagramEntitiesWithPrefixes.add(calledServiceEca.getDisplayPrefixedName());
            allServiceEcaList.add(calledServiceEca);
        }

        // write index.eomodeld file
        Map<String, Object> indexEoModelMap = FastMap.newInstance();
        indexEoModelMap.put("EOModelVersion", "\"2.1\"");
        List<Map<String, Object>> entitiesMapList = FastList.newInstance();
        indexEoModelMap.put("entities", entitiesMapList);
        for (String entityName: allDiagramEntitiesWithPrefixes) {
            Map<String, Object> entitiesMap = FastMap.newInstance();
            entitiesMapList.add(entitiesMap);
            entitiesMap.put("className", "EOGenericRecord");
            entitiesMap.put("name", entityName);
        }
        UtilFormatOut.writePlistFile(indexEoModelMap, eomodeldFullPath, "index.eomodeld");
        
        // write this service description file
        Map<String, Object> thisServiceEoModelMap = createEoModelMap(allServiceList, allServiceEcaList, useMoreDetailedNames);
        UtilFormatOut.writePlistFile(thisServiceEoModelMap, eomodeldFullPath, this.getDisplayPrefixedName() + ".plist");

        // write service description files
        for (ServiceArtifactInfo callingService: callingServiceList) {
            Map<String, Object> serviceEoModelMap = callingService.createEoModelMap(UtilMisc.toList(this), null, useMoreDetailedNames);
            UtilFormatOut.writePlistFile(serviceEoModelMap, eomodeldFullPath, callingService.getDisplayPrefixedName() + ".plist");
        }
        for (ServiceArtifactInfo calledService: calledServiceList) {
            Map<String, Object> serviceEoModelMap = calledService.createEoModelMap(UtilMisc.toList(this), null, useMoreDetailedNames);
            UtilFormatOut.writePlistFile(serviceEoModelMap, eomodeldFullPath, calledService.getDisplayPrefixedName() + ".plist");
        }
        
        // write SECA description files
        for (ServiceEcaArtifactInfo callingServiceEca: callingServiceEcaList) {
            // add List<ServiceArtifactInfo> for services that trigger this eca rule
            List<ServiceArtifactInfo> ecaCallingServiceList = callingServiceEca.getServicesTriggeringServiceEca();
            for (ServiceArtifactInfo ecaCallingService: ecaCallingServiceList) {
                ecaCallingService.setDisplayPrefix("Triggering:");
            }
            ecaCallingServiceList.add(this);
            
            Map<String, Object> serviceEcaEoModelMap = callingServiceEca.createEoModelMap(ecaCallingServiceList, useMoreDetailedNames);
            UtilFormatOut.writePlistFile(serviceEcaEoModelMap, eomodeldFullPath, callingServiceEca.getDisplayPrefixedName() + ".plist");
        }
        for (ServiceEcaArtifactInfo calledServiceEca: calledServiceEcaList) {
            // add List<ServiceArtifactInfo> for services this eca rule calls in action
            List<ServiceArtifactInfo> ecaCalledServiceList = calledServiceEca.getServicesCalledByServiceEcaActions();
            for (ServiceArtifactInfo ecaCalledService: ecaCalledServiceList) {
                ecaCalledService.setDisplayPrefix("Called:");
            }
            ecaCalledServiceList.add(this);
            
            Map<String, Object> serviceEcaEoModelMap = calledServiceEca.createEoModelMap(ecaCalledServiceList, useMoreDetailedNames);
            UtilFormatOut.writePlistFile(serviceEcaEoModelMap, eomodeldFullPath, calledServiceEca.getDisplayPrefixedName() + ".plist");
        }
    }

    public Map<String, Object> createEoModelMap(List<ServiceArtifactInfo> relatedServiceList, List<ServiceEcaArtifactInfo> relatedServiceEcaList, boolean useMoreDetailedNames) {
        if (relatedServiceList == null) relatedServiceList = FastList.newInstance();
        if (relatedServiceEcaList == null) relatedServiceEcaList = FastList.newInstance();
        Map<String, Object> topLevelMap = FastMap.newInstance();

        topLevelMap.put("name", this.getDisplayPrefixedName());
        topLevelMap.put("className", "EOGenericRecord");

        // for classProperties add attribute names AND relationship names to get a nice, complete chart
        List<String> classPropertiesList = FastList.newInstance();
        topLevelMap.put("classProperties", classPropertiesList);
        for (ModelParam param: this.modelService.getModelParamList()) {
            if (useMoreDetailedNames) {
                classPropertiesList.add(param.getShortDisplayDescription());
            } else {
                classPropertiesList.add(param.name);
            }
        }
        for (ServiceArtifactInfo sai: relatedServiceList) {
            classPropertiesList.add(sai.getDisplayPrefixedName());
        }
        for (ServiceEcaArtifactInfo seai: relatedServiceEcaList) {
            classPropertiesList.add(seai.getDisplayPrefixedName());
        }
        
        // attributes
        List<Map<String, Object>> attributesList = FastList.newInstance();
        topLevelMap.put("attributes", attributesList);
        for (ModelParam param: this.modelService.getModelParamList()) {
            Map<String, Object> attributeMap = FastMap.newInstance();
            attributesList.add(attributeMap);
            
            if (useMoreDetailedNames) {
                attributeMap.put("name", param.getShortDisplayDescription());
            } else {
                attributeMap.put("name", param.name);
            }
            attributeMap.put("valueClassName", param.type);
            attributeMap.put("externalType", param.type);
        }
        
        // relationships
        List<Map<String, Object>> relationshipsMapList = FastList.newInstance();
        
        for (ServiceArtifactInfo sai: relatedServiceList) {
            Map<String, Object> relationshipMap = FastMap.newInstance();
            relationshipsMapList.add(relationshipMap);
            
            relationshipMap.put("name", sai.getDisplayPrefixedName());
            relationshipMap.put("destination", sai.getDisplayPrefixedName());
            
            // not sure if we can use these, or need them, for this type of diagram
            //relationshipMap.put("isToMany", "N");
            //relationshipMap.put("joinSemantic", "EOInnerJoin");
            //relationshipMap.put("joins", joinsMapList);
            //joinsMap.put("sourceAttribute", keyMap.getFieldName());
            //joinsMap.put("destinationAttribute", keyMap.getRelFieldName());
        }
        for (ServiceEcaArtifactInfo seai: relatedServiceEcaList) {
            Map<String, Object> relationshipMap = FastMap.newInstance();
            relationshipsMapList.add(relationshipMap);
            
            relationshipMap.put("name", seai.getDisplayPrefixedName());
            relationshipMap.put("destination", seai.getDisplayPrefixedName());
        }
        
        if (relationshipsMapList.size() > 0) {
            topLevelMap.put("relationships", relationshipsMapList);
        }
        
        return topLevelMap;
    }
}
