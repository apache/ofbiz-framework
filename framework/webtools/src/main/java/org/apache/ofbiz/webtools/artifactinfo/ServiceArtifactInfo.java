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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
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
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.FileUtil;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.UtilJavaParse;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilPlist;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.minilang.MiniLangException;
import org.apache.ofbiz.minilang.SimpleMethod;
import org.apache.ofbiz.minilang.artifact.ArtifactInfoContext;
import org.apache.ofbiz.service.ModelParam;
import org.apache.ofbiz.service.ModelService;
import org.apache.ofbiz.service.eca.ServiceEcaRule;
import org.apache.ofbiz.service.eca.ServiceEcaUtil;
import org.apache.ofbiz.service.group.GroupModel;
import org.apache.ofbiz.service.group.GroupServiceModel;
import org.apache.ofbiz.service.group.ServiceGroupReader;

/**
 *
 */
public class ServiceArtifactInfo extends ArtifactInfoBase {
    private static final String MODULE = ServiceArtifactInfo.class.getName();

    private ModelService modelService;
    private String displayPrefix = null;

    private Set<EntityArtifactInfo> entitiesUsedByThisService = new TreeSet<>();
    private Set<ServiceArtifactInfo> servicesCalledByThisService = new TreeSet<>();
    private Set<ServiceEcaArtifactInfo> serviceEcasTriggeredByThisService = new TreeSet<>();

    public ServiceArtifactInfo(String serviceName, ArtifactInfoFactory aif) throws GeneralException {
        super(aif);
        this.modelService = this.getAif().getModelService(serviceName);
    }

    /**
     * This must be called after creation from the ArtifactInfoFactory after this class has been put into the global Map in order to avoid recursive
     * initialization
     * @throws GeneralException
     */
    public void populateAll() throws GeneralException {
        this.populateUsedEntities();
        this.populateCalledServices();
        this.populateTriggeredServiceEcas();
    }

    /**
     * Populate used entities.
     * @throws GeneralException the general exception
     */
    protected void populateUsedEntities() throws GeneralException {
        // populate entitiesUsedByThisService and for each the reverse-associate cache in the aif
        if ("simple".equals(this.modelService.getEngineName())) {
            // we can do something with this!
            SimpleMethod simpleMethodToCall = null;
            try {
                simpleMethodToCall = SimpleMethod.getSimpleMethod(this.modelService.getLocation(), this.modelService.getInvoke(), null);
            } catch (MiniLangException e) {
                Debug.logWarning("Error getting Simple-method [" + this.modelService.getInvoke() + "] in [" + this.modelService.getLocation()
                        + "] referenced in service [" + this.modelService.getName() + "]: " + e.toString(), MODULE);
            }
            if (simpleMethodToCall == null) {
                Debug.logWarning("Simple-method [" + this.modelService.getInvoke() + "] in [" + this.modelService.getLocation()
                        + "] referenced in service [" + this.modelService.getName() + "] not found", MODULE);
                return;
            }

            ArtifactInfoContext aic = new ArtifactInfoContext();
            simpleMethodToCall.gatherArtifactInfo(aic);
            populateEntitiesFromNameSet(aic.getEntityNames());

        } else if ("java".equals(this.modelService.getEngineName())) {
            String fullClassPathAndFile = UtilJavaParse.findRealPathAndFileForClass(this.modelService.getLocation());
            if (fullClassPathAndFile != null) {
                String javaFile = null;
                try {
                    javaFile = FileUtil.readTextFile(fullClassPathAndFile, true).toString();
                } catch (IOException e) {
                    Debug.logWarning("Error reading java file [" + fullClassPathAndFile + "] for service implementation: " + e.toString(), MODULE);
                    return;
                }

                javaFile = UtilJavaParse.stripComments(javaFile);
                int methodBlockStart = UtilJavaParse.findServiceMethodBlockStart(this.modelService.getInvoke(), javaFile);
                int methodBlockEnd = UtilJavaParse.findEndOfBlock(methodBlockStart, javaFile);
                Set<String> allEntityNameSet = UtilJavaParse.findEntityUseInBlock(methodBlockStart, methodBlockEnd, javaFile);
                populateEntitiesFromNameSet(allEntityNameSet);
            }
        //} else if ("group".equals(this.modelService.getEngineName())) {
            // nothing to do, there won't be entities referred to in these
        }
    }

    /**
     * Populate entities from name set.
     * @param allEntityNameSet the all entity name set
     * @throws GeneralException the general exception
     */
    protected void populateEntitiesFromNameSet(Set<String> allEntityNameSet) throws GeneralException {
        for (String entityName: allEntityNameSet) {
            if (UtilValidate.isEmpty(entityName) || entityName.contains("${")) {
                continue;
            }
            // attempt to convert relation names to entity names
            String validEntityName = getAif().getEntityModelReader().validateEntityName(entityName);
            if (validEntityName == null) {
                Debug.logWarning("Entity [" + entityName + "] reference in service [" + this.modelService.getName() + "] does not exist!", MODULE);
                continue;
            }

            // the forward reference
            this.entitiesUsedByThisService.add(getAif().getEntityArtifactInfo(validEntityName));
            // the reverse reference
            UtilMisc.addToSortedSetInMap(this, getAif().getAllServiceInfosReferringToEntityName(), validEntityName);
        }
    }

    /**
     * Populate called services.
     * @throws GeneralException the general exception
     */
    protected void populateCalledServices() throws GeneralException {
        // populate servicesCalledByThisService and for each the reverse-associate cache in the aif
        if ("simple".equals(this.modelService.getEngineName())) {
            // we can do something with this!
            SimpleMethod simpleMethodToCall = null;
            try {
                simpleMethodToCall = SimpleMethod.getSimpleMethod(this.modelService.getLocation(), this.modelService.getInvoke(), null);
            } catch (MiniLangException e) {
                Debug.logWarning("Error getting Simple-method [" + this.modelService.getInvoke() + "] in [" + this.modelService.getLocation()
                        + "] referenced in service [" + this.modelService.getName() + "]: " + e.toString(), MODULE);
            }
            if (simpleMethodToCall == null) {
                Debug.logWarning("Simple-method [" + this.modelService.getInvoke() + "] in [" + this.modelService.getLocation()
                        + "] referenced in service [" + this.modelService.getName() + "] not found", MODULE);
                return;
            }

            ArtifactInfoContext aic = new ArtifactInfoContext();
            simpleMethodToCall.gatherArtifactInfo(aic);
            populateServicesFromNameSet(aic.getServiceNames());

        } else if ("java".equals(this.modelService.getEngineName())) {
            String fullClassPathAndFile = UtilJavaParse.findRealPathAndFileForClass(this.modelService.getLocation());
            if (fullClassPathAndFile != null) {
                String javaFile = null;
                try {
                    javaFile = FileUtil.readTextFile(fullClassPathAndFile, true).toString();
                } catch (IOException e) {
                    Debug.logWarning("Error reading java file [" + fullClassPathAndFile + "] for service implementation: " + e.toString(), MODULE);
                    return;
                }

                javaFile = UtilJavaParse.stripComments(javaFile);
                int methodBlockStart = UtilJavaParse.findServiceMethodBlockStart(this.modelService.getInvoke(), javaFile);
                int methodBlockEnd = UtilJavaParse.findEndOfBlock(methodBlockStart, javaFile);
                Set<String> allServiceNameSet = UtilJavaParse.findServiceCallsInBlock(methodBlockStart, methodBlockEnd, javaFile);

                populateServicesFromNameSet(allServiceNameSet);
            }
        } else if ("group".equals(this.modelService.getEngineName())) {
            Set<String> allServiceNameSet = new HashSet<>();
            GroupModel groupModel = modelService.getInternalGroup();
            if (groupModel == null) {
                groupModel = ServiceGroupReader.getGroupModel(this.modelService.getLocation());
            }

            if (groupModel != null) {
                List<GroupServiceModel> groupServiceModels = groupModel.getServices();
                for (GroupServiceModel groupServiceModel: groupServiceModels) {
                    allServiceNameSet.add(groupServiceModel.getName());
                }
            }

            populateServicesFromNameSet(allServiceNameSet);
        }
    }

    /**
     * Populate services from name set.
     * @param allServiceNameSet the all service name set
     * @throws GeneralException the general exception
     */
    protected void populateServicesFromNameSet(Set<String> allServiceNameSet) throws GeneralException {
        for (String serviceName: allServiceNameSet) {
            if (serviceName.contains("${")) {
                continue;
            }
            if (!getAif().getDispatchContext().getAllServiceNames().contains(serviceName)) {
                Debug.logWarning("Service [" + serviceName + "] reference in service [" + this.modelService.getName() + "] does not exist!", MODULE);
                continue;
            }

            // the forward reference
            this.servicesCalledByThisService.add(getAif().getServiceArtifactInfo(serviceName));
            // the reverse reference
            UtilMisc.addToSortedSetInMap(this, getAif().getAllServiceInfosReferringToServiceName(), serviceName);
        }
    }

    /**
     * Populate triggered service ecas.
     * @throws GeneralException the general exception
     */
    protected void populateTriggeredServiceEcas() throws GeneralException {
        // populate serviceEcasTriggeredByThisService and for each the reverse-associate cache in the aif
        Map<String, List<ServiceEcaRule>> serviceEventMap = ServiceEcaUtil.getServiceEventMap(this.modelService.getName());
        if (serviceEventMap == null) return;
        for (List<ServiceEcaRule> ecaRuleList: serviceEventMap.values()) {
            for (ServiceEcaRule ecaRule: ecaRuleList) {
                this.serviceEcasTriggeredByThisService.add(getAif().getServiceEcaArtifactInfo(ecaRule));
                // the reverse reference
                UtilMisc.addToSortedSetInMap(this, getAif().getAllServiceInfosReferringToServiceEcaRule(), ecaRule);
            }
        }
    }

    /**
     * Gets model service.
     * @return the model service
     */
    public ModelService getModelService() {
        return this.modelService;
    }

    /**
     * Sets display prefix.
     * @param displayPrefix the display prefix
     */
    public void setDisplayPrefix(String displayPrefix) {
        this.displayPrefix = displayPrefix;
    }

    @Override
    public String getDisplayName() {
        return this.getDisplayPrefixedName();
    }

    /**
     * Gets display prefixed name.
     * @return the display prefixed name
     */
    public String getDisplayPrefixedName() {
        return (this.displayPrefix != null ? this.displayPrefix : "") + this.modelService.getName();
    }

    @Override
    public String getDisplayType() {
        return "Service";
    }

    @Override
    public String getType() {
        return ArtifactInfoFactory.SERVICE_INFO_TYPE_ID;
    }

    @Override
    public String getUniqueId() {
        return this.modelService.getName();
    }

    @Override
    public URL getLocationURL() throws MalformedURLException {
        return FlexibleLocation.resolveLocation(modelService.getDefinitionLocation());
    }

    /**
     * Gets implementation location url.
     * @return the implementation location url
     * @throws MalformedURLException the malformed url exception
     */
    public URL getImplementationLocationURL() throws MalformedURLException {
        return FlexibleLocation.resolveLocation(modelService.getLocation());
    }

    /**
     * Gets entities used by service.
     * @return the entities used by service
     */
    public Set<EntityArtifactInfo> getEntitiesUsedByService() {
        return this.entitiesUsedByThisService;
    }

    /**
     * Gets services calling service.
     * @return the services calling service
     */
    public Set<ServiceArtifactInfo> getServicesCallingService() {
        return getAif().getAllServiceInfosReferringToServiceName().get(this.modelService.getName());
    }

    /**
     * Gets services called by service.
     * @return the services called by service
     */
    public Set<ServiceArtifactInfo> getServicesCalledByService() {
        return this.servicesCalledByThisService;
    }

    /**
     * Gets services called by service ecas.
     * @return the services called by service ecas
     */
    public Set<ServiceArtifactInfo> getServicesCalledByServiceEcas() {
        // TODO: implement this sometime, not really necessary
        return new HashSet<>();
    }

    /**
     * Gets service eca rules triggered by service.
     * @return the service eca rules triggered by service
     */
    public Set<ServiceEcaArtifactInfo> getServiceEcaRulesTriggeredByService() {
        return this.serviceEcasTriggeredByThisService;
    }

    /**
     * Gets services calling service by ecas.
     * @return the services calling service by ecas
     */
    public Set<ServiceArtifactInfo> getServicesCallingServiceByEcas() {
        // TODO: implement this sometime, not really necessary
        return new HashSet<>();
    }

    /**
     * Gets service eca rules calling service.
     * @return the service eca rules calling service
     */
    public Set<ServiceEcaArtifactInfo> getServiceEcaRulesCallingService() {
        return this.getAif().getAllServiceEcaInfosReferringToServiceName().get(this.modelService.getName());
    }

    /**
     * Gets forms calling service.
     * @return the forms calling service
     */
    public Set<FormWidgetArtifactInfo> getFormsCallingService() {
        return this.getAif().getAllFormInfosReferringToServiceName().get(this.modelService.getName());
    }

    /**
     * Gets forms based on service.
     * @return the forms based on service
     */
    public Set<FormWidgetArtifactInfo> getFormsBasedOnService() {
        return this.getAif().getAllFormInfosBasedOnServiceName().get(this.modelService.getName());
    }

    /**
     * Gets screens calling service.
     * @return the screens calling service
     */
    public Set<ScreenWidgetArtifactInfo> getScreensCallingService() {
        return this.getAif().getAllScreenInfosReferringToServiceName().get(this.modelService.getName());
    }

    /**
     * Gets requests with event calling service.
     * @return the requests with event calling service
     */
    public Set<ControllerRequestArtifactInfo> getRequestsWithEventCallingService() {
        return this.getAif().getAllRequestInfosReferringToServiceName().get(this.modelService.getName());
    }

    /**
     * Write service call graph eo model.
     * @param eomodeldFullPath the eomodeld full path
     * @throws GeneralException             the general exception
     * @throws FileNotFoundException        the file not found exception
     * @throws UnsupportedEncodingException the unsupported encoding exception
     */
    public void writeServiceCallGraphEoModel(String eomodeldFullPath) throws GeneralException, FileNotFoundException, UnsupportedEncodingException {
        boolean useMoreDetailedNames = true;

        Debug.logInfo("Writing Service Call Graph EO Model for service [" + this.modelService.getName() + "] to [" + eomodeldFullPath + "]", MODULE);

        Set<String> allDiagramEntitiesWithPrefixes = new HashSet<>();
        List<ServiceArtifactInfo> allServiceList = new LinkedList<>();
        List<ServiceEcaArtifactInfo> allServiceEcaList = new LinkedList<>();

        // make sure that any prefix that might have been set on this is cleared
        this.setDisplayPrefix("");

        // put this service in the master list
        allDiagramEntitiesWithPrefixes.add(this.modelService.getName());

        // all services that call this service
        Set<ServiceArtifactInfo> callingServiceSet = this.getServicesCallingService();
        if (callingServiceSet != null) {
            // set the prefix and add to the all list
            for (ServiceArtifactInfo callingService: callingServiceSet) {
                callingService.setDisplayPrefix("Calling_");
                allDiagramEntitiesWithPrefixes.add(callingService.getDisplayPrefixedName());
                allServiceList.add(callingService);
            }
        }

        // all services this service calls
        Set<ServiceArtifactInfo> calledServiceSet = this.getServicesCalledByService();
        for (ServiceArtifactInfo calledService: calledServiceSet) {
            calledService.setDisplayPrefix("Called_");
            allDiagramEntitiesWithPrefixes.add(calledService.getDisplayPrefixedName());
            allServiceList.add(calledService);
        }

        Map<String, Integer> displaySuffixNumByEcaName = new HashMap<>();

        // all SECAs and triggering services that call this service as an action
        Set<ServiceEcaArtifactInfo> callingServiceEcaSet = this.getServiceEcaRulesCallingService();
        if (callingServiceEcaSet != null) {
            for (ServiceEcaArtifactInfo callingServiceEca: callingServiceEcaSet) {
                callingServiceEca.setDisplayPrefix("Triggering_");

                Integer displaySuffix = displaySuffixNumByEcaName.get(callingServiceEca.getDisplayPrefixedName());
                if (displaySuffix == null) {
                    displaySuffix = 1;
                } else {
                    displaySuffix++;
                }
                displaySuffixNumByEcaName.put(callingServiceEca.getDisplayPrefixedName(), displaySuffix);
                callingServiceEca.setDisplaySuffixNum(displaySuffix);

                allDiagramEntitiesWithPrefixes.add(callingServiceEca.getDisplayPrefixedName());
                allServiceEcaList.add(callingServiceEca);
            }
        }

        // all SECAs and corresponding services triggered by this service
        Set<ServiceEcaArtifactInfo> calledServiceEcaSet = this.getServiceEcaRulesTriggeredByService();
        for (ServiceEcaArtifactInfo calledServiceEca: calledServiceEcaSet) {
            calledServiceEca.setDisplayPrefix("Triggered_");

            Integer displaySuffix = displaySuffixNumByEcaName.get(calledServiceEca.getDisplayPrefixedName());
            if (displaySuffix == null) {
                displaySuffix = 1;
            } else {
                displaySuffix++;
            }
            displaySuffixNumByEcaName.put(calledServiceEca.getDisplayPrefixedName(), displaySuffix);
            calledServiceEca.setDisplaySuffixNum(displaySuffix);

            allDiagramEntitiesWithPrefixes.add(calledServiceEca.getDisplayPrefixedName());
            allServiceEcaList.add(calledServiceEca);
        }

        // write index.eomodeld file
        Map<String, Object> indexEoModelMap = new HashMap<>();
        indexEoModelMap.put("EOModelVersion", "\"2.1\"");
        List<Map<String, Object>> entitiesMapList = new LinkedList<>();
        indexEoModelMap.put("entities", entitiesMapList);
        for (String entityName: allDiagramEntitiesWithPrefixes) {
            Map<String, Object> entitiesMap = new HashMap<>();
            entitiesMapList.add(entitiesMap);
            entitiesMap.put("className", "EOGenericRecord");
            entitiesMap.put("name", entityName);
        }
        UtilPlist.writePlistFile(indexEoModelMap, eomodeldFullPath, "index.eomodeld", true);

        // write this service description file
        Map<String, Object> thisServiceEoModelMap = createEoModelMap(callingServiceSet, calledServiceSet, callingServiceEcaSet, calledServiceEcaSet,
                useMoreDetailedNames);
        UtilPlist.writePlistFile(thisServiceEoModelMap, eomodeldFullPath, this.modelService.getName() + ".plist", true);

        // write service description files
        if (callingServiceSet != null) {
            for (ServiceArtifactInfo callingService: callingServiceSet) {
                Map<String, Object> serviceEoModelMap = callingService.createEoModelMap(null, UtilMisc.toSet(this), null, null, useMoreDetailedNames);
                UtilPlist.writePlistFile(serviceEoModelMap, eomodeldFullPath, callingService.getDisplayPrefixedName() + ".plist", true);
            }
        }
        if (calledServiceSet != null) {
            for (ServiceArtifactInfo calledService: calledServiceSet) {
                Map<String, Object> serviceEoModelMap = calledService.createEoModelMap(UtilMisc.toSet(this), null, null, null, useMoreDetailedNames);
                UtilPlist.writePlistFile(serviceEoModelMap, eomodeldFullPath, calledService.getDisplayPrefixedName() + ".plist", true);
            }
        }

        // write SECA description files
        if (callingServiceEcaSet != null) {
            for (ServiceEcaArtifactInfo callingServiceEca: callingServiceEcaSet) {
                // add List<ServiceArtifactInfo> for services that trigger this eca rule
                Set<ServiceArtifactInfo> ecaCallingServiceSet = callingServiceEca.getServicesTriggeringServiceEca();
                for (ServiceArtifactInfo ecaCallingService: ecaCallingServiceSet) {
                    ecaCallingService.setDisplayPrefix("Triggering_");
                }
                ecaCallingServiceSet.add(this);

                Map<String, Object> serviceEcaEoModelMap = callingServiceEca.createEoModelMap(ecaCallingServiceSet, null, useMoreDetailedNames);
                UtilPlist.writePlistFile(serviceEcaEoModelMap, eomodeldFullPath, callingServiceEca.getDisplayPrefixedName() + ".plist", true);
            }
        }
        if (calledServiceEcaSet != null) {
            for (ServiceEcaArtifactInfo calledServiceEca: calledServiceEcaSet) {
                // add List<ServiceArtifactInfo> for services this eca rule calls in action
                Set<ServiceArtifactInfo> ecaCalledServiceSet = calledServiceEca.getServicesCalledByServiceEcaActions();
                for (ServiceArtifactInfo ecaCalledService: ecaCalledServiceSet) {
                    ecaCalledService.setDisplayPrefix("Triggered_");
                }
                ecaCalledServiceSet.add(this);

                Map<String, Object> serviceEcaEoModelMap = calledServiceEca.createEoModelMap(null, ecaCalledServiceSet, useMoreDetailedNames);
                UtilPlist.writePlistFile(serviceEcaEoModelMap, eomodeldFullPath, calledServiceEca.getDisplayPrefixedName() + ".plist", true);
            }
        }
    }

    /**
     * Create eo model map map.
     * @param callingServiceSet    the calling service set
     * @param calledServiceSet     the called service set
     * @param callingServiceEcaSet the calling service eca set
     * @param calledServiceEcaSet  the called service eca set
     * @param useMoreDetailedNames the use more detailed names
     * @return the map
     */
    public Map<String, Object> createEoModelMap(Set<ServiceArtifactInfo> callingServiceSet, Set<ServiceArtifactInfo> calledServiceSet,
            Set<ServiceEcaArtifactInfo> callingServiceEcaSet, Set<ServiceEcaArtifactInfo> calledServiceEcaSet, boolean useMoreDetailedNames) {
        if (callingServiceSet == null) callingServiceSet = new HashSet<>();
        if (calledServiceSet == null) calledServiceSet = new HashSet<>();
        if (callingServiceEcaSet == null) callingServiceEcaSet = new HashSet<>();
        if (calledServiceEcaSet == null) calledServiceEcaSet = new HashSet<>();
        Map<String, Object> topLevelMap = new HashMap<>();

        topLevelMap.put("name", this.getDisplayPrefixedName());
        topLevelMap.put("className", "EOGenericRecord");

        // for classProperties add attribute names AND relationship names to get a nice, complete chart
        List<String> classPropertiesList = new LinkedList<>();
        topLevelMap.put("classProperties", classPropertiesList);
        for (ModelParam param: this.modelService.getModelParamList()) {
            // skip the internal parameters, very redundant in the diagrams
            if (param.getInternal()) continue;

            if (useMoreDetailedNames) {
                classPropertiesList.add(param.getShortDisplayDescription());
            } else {
                classPropertiesList.add(param.getName());
            }
        }
        for (ServiceArtifactInfo sai: callingServiceSet) {
            classPropertiesList.add(sai.getDisplayPrefixedName());
        }
        for (ServiceArtifactInfo sai: calledServiceSet) {
            classPropertiesList.add(sai.getDisplayPrefixedName());
        }
        for (ServiceEcaArtifactInfo seai: callingServiceEcaSet) {
            classPropertiesList.add(seai.getDisplayPrefixedName());
        }
        for (ServiceEcaArtifactInfo seai: calledServiceEcaSet) {
            classPropertiesList.add(seai.getDisplayPrefixedName());
        }

        // attributes
        List<Map<String, Object>> attributesList = new LinkedList<>();
        topLevelMap.put("attributes", attributesList);
        for (ModelParam param: this.modelService.getModelParamList()) {
            Map<String, Object> attributeMap = new HashMap<>();
            attributesList.add(attributeMap);

            if (useMoreDetailedNames) {
                attributeMap.put("name", param.getShortDisplayDescription());
            } else {
                attributeMap.put("name", param.getName());
            }
            attributeMap.put("valueClassName", param.getType());
            attributeMap.put("externalType", param.getType());
        }

        // relationships
        List<Map<String, Object>> relationshipsMapList = new LinkedList<>();

        for (ServiceArtifactInfo sai: callingServiceSet) {
            Map<String, Object> relationshipMap = new HashMap<>();
            relationshipsMapList.add(relationshipMap);

            relationshipMap.put("name", sai.getDisplayPrefixedName());
            relationshipMap.put("destination", sai.getDisplayPrefixedName());
            relationshipMap.put("isToMany", "N");
            relationshipMap.put("isMandatory", "Y");
        }
        for (ServiceArtifactInfo sai: calledServiceSet) {
            Map<String, Object> relationshipMap = new HashMap<>();
            relationshipsMapList.add(relationshipMap);

            relationshipMap.put("name", sai.getDisplayPrefixedName());
            relationshipMap.put("destination", sai.getDisplayPrefixedName());
            relationshipMap.put("isToMany", "Y");
            relationshipMap.put("isMandatory", "Y");
        }

        for (ServiceEcaArtifactInfo seai: callingServiceEcaSet) {
            Map<String, Object> relationshipMap = new HashMap<>();
            relationshipsMapList.add(relationshipMap);

            relationshipMap.put("name", seai.getDisplayPrefixedName());
            relationshipMap.put("destination", seai.getDisplayPrefixedName());
            relationshipMap.put("isToMany", "N");
            relationshipMap.put("isMandatory", "Y");
        }
        for (ServiceEcaArtifactInfo seai: calledServiceEcaSet) {
            Map<String, Object> relationshipMap = new HashMap<>();
            relationshipsMapList.add(relationshipMap);

            relationshipMap.put("name", seai.getDisplayPrefixedName());
            relationshipMap.put("destination", seai.getDisplayPrefixedName());
            relationshipMap.put("isToMany", "Y");
            relationshipMap.put("isMandatory", "Y");
        }

        if (!relationshipsMapList.isEmpty()) {
            topLevelMap.put("relationships", relationshipsMapList);
        }

        return topLevelMap;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ServiceArtifactInfo) {
            return this.modelService.getName().equals(((ServiceArtifactInfo) obj).modelService.getName());
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }
}
