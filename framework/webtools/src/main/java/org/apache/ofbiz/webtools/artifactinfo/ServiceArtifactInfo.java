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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

import org.apache.ofbiz.base.location.FlexibleLocation;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.FileUtil;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.UtilJavaParse;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.minilang.MiniLangException;
import org.apache.ofbiz.minilang.SimpleMethod;
import org.apache.ofbiz.minilang.artifact.ArtifactInfoContext;
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

    private Set<EntityArtifactInfo> entitiesUsedByThisService = new ConcurrentSkipListSet<>();
    private Set<ServiceArtifactInfo> servicesCalledByThisService = new ConcurrentSkipListSet<>();
    private Set<ServiceEcaArtifactInfo> serviceEcasTriggeredByThisService = new ConcurrentSkipListSet<>();

    public ServiceArtifactInfo(String serviceName, ArtifactInfoFactory aif) throws GeneralException {
        super(aif);
        this.modelService = aif.getModelService(serviceName);
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
            // } else if ("group".equals(this.modelService.getEngineName())) {
            // nothing to do, there won't be entities referred to in these
        }
    }

    /**
     * Populate entities from name set.
     * @param allEntityNameSet the all entity name set
     * @throws GeneralException the general exception
     */
    protected void populateEntitiesFromNameSet(Set<String> allEntityNameSet) throws GeneralException {
        for (String entityName : allEntityNameSet) {
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
            addToSortedSetInMap(this, getAif().getAllServiceInfosReferringToEntityName(), validEntityName);
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
                for (GroupServiceModel groupServiceModel : groupServiceModels) {
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
        for (String serviceName : allServiceNameSet) {
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
            addToSortedSetInMap(this, getAif().getAllServiceInfosReferringToServiceName(), serviceName);
        }
    }

    /**
     * Populate triggered service ecas.
     * @throws GeneralException the general exception
     */
    protected void populateTriggeredServiceEcas() throws GeneralException {
        // populate serviceEcasTriggeredByThisService and for each the reverse-associate cache in the aif
        Map<String, List<ServiceEcaRule>> serviceEventMap = ServiceEcaUtil.getServiceEventMap(this.modelService.getName());
        if (serviceEventMap == null) {
            return;
        }
        for (List<ServiceEcaRule> ecaRuleList : serviceEventMap.values()) {
            for (ServiceEcaRule ecaRule : ecaRuleList) {
                this.serviceEcasTriggeredByThisService.add(getAif().getServiceEcaArtifactInfo(ecaRule));
                // the reverse reference
                addToSortedSetInMap(this, getAif().getAllServiceInfosReferringToServiceEcaRule(), ecaRule);
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
