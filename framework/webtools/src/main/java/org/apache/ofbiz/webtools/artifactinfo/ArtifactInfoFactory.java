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

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.ofbiz.base.component.ComponentConfig;
import org.apache.ofbiz.base.concurrent.ExecutionPool;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.FileUtil;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.cache.UtilCache;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.config.model.DelegatorElement;
import org.apache.ofbiz.entity.config.model.EntityConfig;
import org.apache.ofbiz.entity.model.ModelEntity;
import org.apache.ofbiz.entity.model.ModelReader;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.ModelService;
import org.apache.ofbiz.service.eca.ServiceEcaRule;
import org.apache.ofbiz.webapp.control.ConfigXMLReader;
import org.apache.ofbiz.webapp.control.ConfigXMLReader.ControllerConfig;
import org.apache.ofbiz.webapp.control.WebAppConfigurationException;
import org.apache.ofbiz.widget.model.FormFactory;
import org.apache.ofbiz.widget.model.ModelForm;
import org.apache.ofbiz.widget.model.ModelScreen;
import org.apache.ofbiz.widget.model.ScreenFactory;
import org.apache.ofbiz.widget.model.ThemeFactory;
import org.xml.sax.SAXException;

/**
 * The type Artifact info factory.
 */
public class ArtifactInfoFactory {

    private static final String MODULE = ArtifactInfoFactory.class.getName();

    private static final UtilCache<String, ArtifactInfoFactory> ARTIFACT_INFO_FACTORY_CACHE = UtilCache.createUtilCache("ArtifactInfoFactory");

    public static final String ENTITY_INFO_TYPE_ID = "entity";
    public static final String SERVICE_INFO_TYPE_ID = "service";
    public static final String SERVICE_ECA_INFO_TYPE_ID = "serviceEca";
    public static final String FORM_WIDGET_INFO_TYPE_ID = "form";
    public static final String SCREEN_WIDGET_INFO_TYPE_ID = "screen";
    public static final String CONTROLLER_REQ_INFO_TYPE_ID = "request";
    public static final String CONTROLLER_VIEW_INFO_TYPE_ID = "view";

    private final String delegatorName;
    private final ModelReader entityModelReader;
    private final DispatchContext dispatchContext;

    private Map<String, EntityArtifactInfo> allEntityInfos = new ConcurrentHashMap<>();
    private Map<String, ServiceArtifactInfo> allServiceInfos = new ConcurrentHashMap<>();
    private Map<ServiceEcaRule, ServiceEcaArtifactInfo> allServiceEcaInfos = new ConcurrentHashMap<>();
    private Map<String, FormWidgetArtifactInfo> allFormInfos = new ConcurrentHashMap<>();
    private Map<String, ScreenWidgetArtifactInfo> allScreenInfos = new ConcurrentHashMap<>();
    private Map<String, ControllerRequestArtifactInfo> allControllerRequestInfos = new ConcurrentHashMap<>();
    private Map<String, ControllerViewArtifactInfo> allControllerViewInfos = new ConcurrentHashMap<>();

    // reverse-associative caches for walking backward in the diagram
    private Map<String, Set<ServiceEcaArtifactInfo>> allServiceEcaInfosReferringToServiceName = new ConcurrentHashMap<>();
    private Map<String, Set<ServiceArtifactInfo>> allServiceInfosReferringToServiceName = new ConcurrentHashMap<>();
    private Map<String, Set<FormWidgetArtifactInfo>> allFormInfosReferringToServiceName = new ConcurrentHashMap<>();
    private Map<String, Set<FormWidgetArtifactInfo>> allFormInfosBasedOnServiceName = new ConcurrentHashMap<>();
    private Map<String, Set<ScreenWidgetArtifactInfo>> allScreenInfosReferringToServiceName = new ConcurrentHashMap<>();
    private Map<String, Set<ControllerRequestArtifactInfo>> allRequestInfosReferringToServiceName = new ConcurrentHashMap<>();

    private Map<String, Set<ServiceArtifactInfo>> allServiceInfosReferringToEntityName = new ConcurrentHashMap<>();
    private Map<String, Set<FormWidgetArtifactInfo>> allFormInfosReferringToEntityName = new ConcurrentHashMap<>();
    private Map<String, Set<ScreenWidgetArtifactInfo>> allScreenInfosReferringToEntityName = new ConcurrentHashMap<>();

    private Map<ServiceEcaRule, Set<ServiceArtifactInfo>> allServiceInfosReferringToServiceEcaRule = new ConcurrentHashMap<>();

    private Map<String, Set<FormWidgetArtifactInfo>> allFormInfosExtendingForm = new ConcurrentHashMap<>();
    private Map<String, Set<ScreenWidgetArtifactInfo>> allScreenInfosReferringToForm = new ConcurrentHashMap<>();

    private Map<String, Set<ScreenWidgetArtifactInfo>> allScreenInfosReferringToScreen = new ConcurrentHashMap<>();
    private Map<String, Set<ControllerViewArtifactInfo>> allViewInfosReferringToScreen = new ConcurrentHashMap<>();

    private Map<String, Set<ControllerRequestArtifactInfo>> allRequestInfosReferringToView = new ConcurrentHashMap<>();

    private Map<String, Set<FormWidgetArtifactInfo>> allFormInfosTargetingRequest = new ConcurrentHashMap<>();
    private Map<String, Set<FormWidgetArtifactInfo>> allFormInfosReferringToRequest = new ConcurrentHashMap<>();
    private Map<String, Set<ScreenWidgetArtifactInfo>> allScreenInfosReferringToRequest = new ConcurrentHashMap<>();
    private Map<String, Set<ControllerRequestArtifactInfo>> allRequestInfosReferringToRequest = new ConcurrentHashMap<>();

    /**
     * Gets all service eca infos referring to service name.
     * @return the all service eca infos referring to service name
     */
    public Map<String, Set<ServiceEcaArtifactInfo>> getAllServiceEcaInfosReferringToServiceName() {
        return allServiceEcaInfosReferringToServiceName;
    }

    /**
     * Gets all service infos referring to service name.
     * @return the all service infos referring to service name
     */
    public Map<String, Set<ServiceArtifactInfo>> getAllServiceInfosReferringToServiceName() {
        return allServiceInfosReferringToServiceName;
    }

    /**
     * Gets all form infos referring to service name.
     * @return the all form infos referring to service name
     */
    public Map<String, Set<FormWidgetArtifactInfo>> getAllFormInfosReferringToServiceName() {
        return allFormInfosReferringToServiceName;
    }

    /**
     * Gets all form infos based on service name.
     * @return the all form infos based on service name
     */
    public Map<String, Set<FormWidgetArtifactInfo>> getAllFormInfosBasedOnServiceName() {
        return allFormInfosBasedOnServiceName;
    }

    /**
     * Gets all screen infos referring to service name.
     * @return the all screen infos referring to service name
     */
    public Map<String, Set<ScreenWidgetArtifactInfo>> getAllScreenInfosReferringToServiceName() {
        return allScreenInfosReferringToServiceName;
    }

    /**
     * Gets all request infos referring to service name.
     * @return the all request infos referring to service name
     */
    public Map<String, Set<ControllerRequestArtifactInfo>> getAllRequestInfosReferringToServiceName() {
        return allRequestInfosReferringToServiceName;
    }

    /**
     * Gets all service infos referring to entity name.
     * @return the all service infos referring to entity name
     */
    public Map<String, Set<ServiceArtifactInfo>> getAllServiceInfosReferringToEntityName() {
        return allServiceInfosReferringToEntityName;
    }

    /**
     * Gets all form infos referring to entity name.
     * @return the all form infos referring to entity name
     */
    public Map<String, Set<FormWidgetArtifactInfo>> getAllFormInfosReferringToEntityName() {
        return allFormInfosReferringToEntityName;
    }

    /**
     * Gets all screen infos referring to entity name.
     * @return the all screen infos referring to entity name
     */
    public Map<String, Set<ScreenWidgetArtifactInfo>> getAllScreenInfosReferringToEntityName() {
        return allScreenInfosReferringToEntityName;
    }

    /**
     * Gets all service infos referring to service eca rule.
     * @return the all service infos referring to service eca rule
     */
    public Map<ServiceEcaRule, Set<ServiceArtifactInfo>> getAllServiceInfosReferringToServiceEcaRule() {
        return allServiceInfosReferringToServiceEcaRule;
    }

    /**
     * Gets all form infos extending form.
     * @return the all form infos extending form
     */
    public Map<String, Set<FormWidgetArtifactInfo>> getAllFormInfosExtendingForm() {
        return allFormInfosExtendingForm;
    }

    /**
     * Gets all screen infos referring to form.
     * @return the all screen infos referring to form
     */
    public Map<String, Set<ScreenWidgetArtifactInfo>> getAllScreenInfosReferringToForm() {
        return allScreenInfosReferringToForm;
    }

    /**
     * Gets all screen infos referring to screen.
     * @return the all screen infos referring to screen
     */
    public Map<String, Set<ScreenWidgetArtifactInfo>> getAllScreenInfosReferringToScreen() {
        return allScreenInfosReferringToScreen;
    }

    /**
     * Gets all view infos referring to screen.
     * @return the all view infos referring to screen
     */
    public Map<String, Set<ControllerViewArtifactInfo>> getAllViewInfosReferringToScreen() {
        return allViewInfosReferringToScreen;
    }

    /**
     * Gets all request infos referring to view.
     * @return the all request infos referring to view
     */
    public Map<String, Set<ControllerRequestArtifactInfo>> getAllRequestInfosReferringToView() {
        return allRequestInfosReferringToView;
    }

    /**
     * Gets all form infos targeting request.
     * @return the all form infos targeting request
     */
    public Map<String, Set<FormWidgetArtifactInfo>> getAllFormInfosTargetingRequest() {
        return allFormInfosTargetingRequest;
    }

    /**
     * Gets all form infos referring to request.
     * @return the all form infos referring to request
     */
    public Map<String, Set<FormWidgetArtifactInfo>> getAllFormInfosReferringToRequest() {
        return allFormInfosReferringToRequest;
    }

    /**
     * Gets all screen infos referring to request.
     * @return the all screen infos referring to request
     */
    public Map<String, Set<ScreenWidgetArtifactInfo>> getAllScreenInfosReferringToRequest() {
        return allScreenInfosReferringToRequest;
    }

    /**
     * Gets all request infos referring to request.
     * @return the all request infos referring to request
     */
    public Map<String, Set<ControllerRequestArtifactInfo>> getAllRequestInfosReferringToRequest() {
        return allRequestInfosReferringToRequest;
    }

    public static ArtifactInfoFactory getArtifactInfoFactory(String delegatorName) throws GeneralException {
        if (UtilValidate.isEmpty(delegatorName)) {
            delegatorName = "default";
        }

        ArtifactInfoFactory aif = ARTIFACT_INFO_FACTORY_CACHE.get(delegatorName);
        if (aif == null) {
            aif = ARTIFACT_INFO_FACTORY_CACHE.putIfAbsentAndGet(delegatorName, new ArtifactInfoFactory(delegatorName));
        }
        return aif;
    }

    protected ArtifactInfoFactory(String delegatorName) throws GeneralException {
        this.delegatorName = delegatorName;
        this.entityModelReader = ModelReader.getModelReader(delegatorName);
        DelegatorElement delegatorInfo = EntityConfig.getInstance().getDelegator(delegatorName);
        String modelName;
        if (delegatorInfo != null) {
            modelName = delegatorInfo.getEntityModelReader();
        } else {
            modelName = "main";
        }
        // since we do not associate a dispatcher to this DispatchContext, it is important to set a name of an existing entity model reader:
        // in this way it will be possible to retrieve the service models from the cache
        this.dispatchContext = new DispatchContext(modelName, this.getClass().getClassLoader(), null);

        this.prepareAll();
    }

    /**
     * Prepare all.
     * @throws GeneralException the general exception
     */
    public void prepareAll() throws GeneralException {
        Debug.logInfo("Loading artifact info objects...", MODULE);
        List<Future<Void>> futures = new ArrayList<>();
        Set<String> entityNames = this.getEntityModelReader().getEntityNames();
        for (String entityName: entityNames) {
            this.getEntityArtifactInfo(entityName);
        }

        Set<String> serviceNames = this.getDispatchContext().getAllServiceNames();
        for (String serviceName: serviceNames) {
            futures.add(ExecutionPool.GLOBAL_FORK_JOIN.submit(prepareTaskForServiceAnalysis(serviceName)));
        }
        // how to get all Service ECAs to prepare? don't worry about it, will be populated from service load, ie all ECAs for each service

        Collection<ComponentConfig> componentConfigs = ComponentConfig.getAllComponents();
        ExecutionPool.getAllFutures(futures);
        futures = new ArrayList<>();
        for (ComponentConfig componentConfig: componentConfigs) {
            futures.add(ExecutionPool.GLOBAL_FORK_JOIN.submit(prepareTaskForComponentAnalysis(componentConfig)));
        }
        ExecutionPool.getAllFutures(futures);
        Debug.logInfo("Artifact info objects loaded.", MODULE);
    }

    /**
     * Gets entity model reader.
     * @return the entity model reader
     */
    public ModelReader getEntityModelReader() {
        return this.entityModelReader;
    }

    /**
     * Gets dispatch context.
     * @return the dispatch context
     */
    public DispatchContext getDispatchContext() {
        return this.dispatchContext;
    }

    /**
     * Gets model entity.
     * @param entityName the entity name
     * @return the model entity
     * @throws GenericEntityException the generic entity exception
     */
    public ModelEntity getModelEntity(String entityName) throws GenericEntityException {
        return this.getEntityModelReader().getModelEntity(entityName);
    }

    /**
     * Gets model service.
     * @param serviceName the service name
     * @return the model service
     * @throws GenericServiceException the generic service exception
     */
    public ModelService getModelService(String serviceName) throws GenericServiceException {
        return this.getDispatchContext().getModelService(serviceName);
    }

    /**
     * Gets model form.
     * @param formNameAndLocation the form name and location
     * @return the model form
     * @throws ParserConfigurationException the parser configuration exception
     * @throws SAXException                 the sax exception
     * @throws IOException                  the io exception
     */
    public ModelForm getModelForm(String formNameAndLocation) throws ParserConfigurationException, SAXException, IOException {
        return getModelForm(formNameAndLocation.substring(formNameAndLocation.indexOf("#") + 1), formNameAndLocation.substring(0,
                formNameAndLocation.indexOf("#")));
    }

    /**
     * Gets model form.
     * @param formName     the form name
     * @param formLocation the form location
     * @return the model form
     * @throws ParserConfigurationException the parser configuration exception
     * @throws SAXException                 the sax exception
     * @throws IOException                  the io exception
     */
    public ModelForm getModelForm(String formName, String formLocation) throws ParserConfigurationException, SAXException, IOException {
        return FormFactory.getFormFromLocation(formLocation, formName, this.entityModelReader,
                ThemeFactory.getVisualThemeFromId("COMMON"), this.dispatchContext);
    }

    /**
     * Gets model screen.
     * @param screenName     the screen name
     * @param screenLocation the screen location
     * @return the model screen
     * @throws ParserConfigurationException the parser configuration exception
     * @throws SAXException                 the sax exception
     * @throws IOException                  the io exception
     */
    public ModelScreen getModelScreen(String screenName, String screenLocation) throws ParserConfigurationException, SAXException, IOException {
        return ScreenFactory.getScreenFromLocation(screenLocation, screenName);
    }

    /**
     * Gets controller request map.
     * @param controllerXmlUrl the controller xml url
     * @param requestUri the request uri
     * @return the controller request map
     */
    public ConfigXMLReader.RequestMap getControllerRequestMap(URL controllerXmlUrl, String requestUri) {
        try {
            return ConfigXMLReader.getControllerConfig(controllerXmlUrl).getRequestMapMap().get(requestUri);
        } catch (WebAppConfigurationException e) {
            Debug.logError(e, "Exception thrown while parsing controller.xml file: ", MODULE);
        }
        return null;
    }

    /**
     * Gets controller view map.
     * @param controllerXmlUrl the controller xml url
     * @param viewUri the view uri
     * @return the controller view map
     */
    public ConfigXMLReader.ViewMap getControllerViewMap(URL controllerXmlUrl, String viewUri) {
        ControllerConfig cc;
        try {
            cc = ConfigXMLReader.getControllerConfig(controllerXmlUrl);
            return cc.getViewMapMap().get(viewUri);
        } catch (WebAppConfigurationException e) {
            Debug.logError(e, "Exception thrown while parsing controller.xml file: ", MODULE);
        }
        return null;
    }

    /**
     * Gets entity artifact info.
     * @param entityName the entity name
     * @return the entity artifact info
     * @throws GeneralException the general exception
     */
    public EntityArtifactInfo getEntityArtifactInfo(String entityName) throws GeneralException {
        EntityArtifactInfo curInfo = this.allEntityInfos.get(entityName);
        if (curInfo == null) {
            curInfo = new EntityArtifactInfo(entityName, this);
            this.allEntityInfos.put(entityName, curInfo);
            curInfo.populateAll();
        }
        return curInfo;
    }

    /**
     * Gets service artifact info.
     * @param serviceName the service name
     * @return the service artifact info
     * @throws GeneralException the general exception
     */
    public ServiceArtifactInfo getServiceArtifactInfo(String serviceName) throws GeneralException {
        ServiceArtifactInfo curInfo = this.allServiceInfos.get(serviceName);
        if (curInfo == null) {
            curInfo = new ServiceArtifactInfo(serviceName, this);
            this.allServiceInfos.put(serviceName, curInfo);
            curInfo.populateAll();
        }
        return curInfo;
    }

    /**
     * Gets service eca artifact info.
     * @param ecaRule the eca rule
     * @return the service eca artifact info
     * @throws GeneralException the general exception
     */
    public ServiceEcaArtifactInfo getServiceEcaArtifactInfo(ServiceEcaRule ecaRule) throws GeneralException {
        ServiceEcaArtifactInfo curInfo = this.allServiceEcaInfos.get(ecaRule);
        if (curInfo == null) {
            curInfo = new ServiceEcaArtifactInfo(ecaRule, this);
            this.allServiceEcaInfos.put(ecaRule, curInfo);
            curInfo.populateAll();
        }
        return curInfo;
    }

    /**
     * Gets form widget artifact info.
     * @param formNameAndLocation the form name and location
     * @return the form widget artifact info
     * @throws GeneralException the general exception
     */
    public FormWidgetArtifactInfo getFormWidgetArtifactInfo(String formNameAndLocation) throws GeneralException {
        return getFormWidgetArtifactInfo(formNameAndLocation.substring(formNameAndLocation.indexOf("#") + 1), formNameAndLocation.substring(0,
                formNameAndLocation.indexOf("#")));
    }

    /**
     * Gets form widget artifact info.
     * @param formName     the form name
     * @param formLocation the form location
     * @return the form widget artifact info
     * @throws GeneralException the general exception
     */
    public FormWidgetArtifactInfo getFormWidgetArtifactInfo(String formName, String formLocation) throws GeneralException {
        FormWidgetArtifactInfo curInfo = this.allFormInfos.get(formLocation + "#" + formName);
        if (curInfo == null) {
            curInfo = new FormWidgetArtifactInfo(formName, formLocation, this);
            this.allFormInfos.put(curInfo.getUniqueId(), curInfo);
            curInfo.populateAll();
        }
        return curInfo;
    }

    /**
     * Gets screen widget artifact info.
     * @param screenName     the screen name
     * @param screenLocation the screen location
     * @return the screen widget artifact info
     */
    public ScreenWidgetArtifactInfo getScreenWidgetArtifactInfo(String screenName, String screenLocation) {
        ScreenWidgetArtifactInfo curInfo = this.allScreenInfos.get(screenLocation + "#" + screenName);
        if (curInfo == null) {
            try {
                curInfo = new ScreenWidgetArtifactInfo(screenName, screenLocation, this);
                this.allScreenInfos.put(curInfo.getUniqueId(), curInfo);
                curInfo.populateAll();
            } catch (GeneralException e) {
                Debug.logWarning("Error loading screen [" + screenName + "] from resource [" + screenLocation + "]: " + e.toString(), MODULE);
                return null;
            }
        }
        return curInfo;
    }

    /**
     * Gets controller request artifact info.
     * @param controllerXmlUrl the controller xml url
     * @param requestUri       the request uri
     * @return the controller request artifact info
     * @throws GeneralException the general exception
     */
    public ControllerRequestArtifactInfo getControllerRequestArtifactInfo(URL controllerXmlUrl, String requestUri) throws GeneralException {
        if (controllerXmlUrl == null) {
            throw new GeneralException("Got a null URL controller");
        }
        if (requestUri == null) {
            throw new GeneralException("Got a null requestUri for controller: " + controllerXmlUrl);
        }
        ControllerRequestArtifactInfo curInfo = this.allControllerRequestInfos.get(controllerXmlUrl.toExternalForm() + "#" + requestUri);
        if (curInfo == null) {
            curInfo = new ControllerRequestArtifactInfo(controllerXmlUrl, requestUri, this);
            this.allControllerRequestInfos.put(curInfo.getUniqueId(), curInfo);
            curInfo.populateAll();
        }
        return curInfo;
    }

    /**
     * Gets controller view artifact info.
     * @param controllerXmlUrl the controller xml url
     * @param viewUri          the view uri
     * @return the controller view artifact info
     * @throws GeneralException the general exception
     */
    public ControllerViewArtifactInfo getControllerViewArtifactInfo(URL controllerXmlUrl, String viewUri) throws GeneralException {
        ControllerViewArtifactInfo curInfo = this.allControllerViewInfos.get(controllerXmlUrl.toExternalForm() + "#" + viewUri);
        if (curInfo == null) {
            curInfo = new ControllerViewArtifactInfo(controllerXmlUrl, viewUri, this);
            this.allControllerViewInfos.put(curInfo.getUniqueId(), curInfo);
        }
        return curInfo;
    }

    /**
     * Gets artifact info by unique id and type.
     * @param uniqueId the unique id
     * @param type     the type
     * @return the artifact info by unique id and type
     */
    public ArtifactInfoBase getArtifactInfoByUniqueIdAndType(String uniqueId, String type) {
        if (uniqueId.contains("#")) {
            int poundIndex = uniqueId.indexOf('#');
            return getArtifactInfoByNameAndType(uniqueId.substring(poundIndex + 1), uniqueId.substring(0, poundIndex), type);
        } else {
            return getArtifactInfoByNameAndType(uniqueId, null, type);
        }
    }

    /**
     * Gets artifact info by name and type.
     * @param artifactName the artifact name
     * @param artifactLocation the artifact location
     * @param type the type
     * @return the artifact info by name and type
     */
    public ArtifactInfoBase getArtifactInfoByNameAndType(String artifactName, String artifactLocation, String type) {
        try {
            if ("entity".equals(type)) {
                return this.getEntityArtifactInfo(artifactName);
            } else if ("service".equals(type)) {
                return this.getServiceArtifactInfo(artifactName);
            } else if ("form".equals(type)) {
                return this.getFormWidgetArtifactInfo(artifactName, artifactLocation);
            } else if ("screen".equals(type)) {
                return this.getScreenWidgetArtifactInfo(artifactName, artifactLocation);
            } else if ("request".equals(type)) {
                return this.getControllerRequestArtifactInfo(new URL(artifactLocation), artifactName);
            } else if ("view".equals(type)) {
                return this.getControllerViewArtifactInfo(new URL(artifactLocation), artifactName);
            }
        } catch (GeneralException | MalformedURLException e) {
            Debug.logError(e, "Error getting artifact info: " + e.toString(), MODULE);
        }
        return null;
    }

    /**
     * Gets all artifact infos by name partial.
     * @param artifactNamePartial the artifact name partial
     * @param type the type
     * @return the all artifact infos by name partial
     */
    public Set<ArtifactInfoBase> getAllArtifactInfosByNamePartial(String artifactNamePartial, String type) {
        Set<ArtifactInfoBase> aiBaseSet = new HashSet<>();

        if (UtilValidate.isEmpty(artifactNamePartial)) {
            return aiBaseSet;
        }

        if (UtilValidate.isEmpty(type) || "entity".equals(type)) {
            for (Map.Entry<String, EntityArtifactInfo> curEntry: allEntityInfos.entrySet()) {
                if (curEntry.getKey().toUpperCase().contains(artifactNamePartial.toUpperCase())) {
                    aiBaseSet.add(curEntry.getValue());
                }
            }
        }
        if (UtilValidate.isEmpty(type) || "service".equals(type)) {
            for (Map.Entry<String, ServiceArtifactInfo> curEntry: allServiceInfos.entrySet()) {
                if (curEntry.getKey().toUpperCase().contains(artifactNamePartial.toUpperCase())) {
                    aiBaseSet.add(curEntry.getValue());
                }
            }
        }
        if (UtilValidate.isEmpty(type) || "form".equals(type)) {
            for (Map.Entry<String, FormWidgetArtifactInfo> curEntry: allFormInfos.entrySet()) {
                if (curEntry.getKey().toUpperCase().contains(artifactNamePartial.toUpperCase())) {
                    aiBaseSet.add(curEntry.getValue());
                }
            }
        }
        if (UtilValidate.isEmpty(type) || "screen".equals(type)) {
            for (Map.Entry<String, ScreenWidgetArtifactInfo> curEntry: allScreenInfos.entrySet()) {
                if (curEntry.getKey().toUpperCase().contains(artifactNamePartial.toUpperCase())) {
                    aiBaseSet.add(curEntry.getValue());
                }
            }
        }
        if (UtilValidate.isEmpty(type) || "request".equals(type)) {
            for (Map.Entry<String, ControllerRequestArtifactInfo> curEntry: allControllerRequestInfos.entrySet()) {
                if (curEntry.getKey().toUpperCase().contains(artifactNamePartial.toUpperCase())) {
                    aiBaseSet.add(curEntry.getValue());
                }
            }
        }
        if (UtilValidate.isEmpty(type) || "view".equals(type)) {
            for (Map.Entry<String, ControllerViewArtifactInfo> curEntry: allControllerViewInfos.entrySet()) {
                if (curEntry.getKey().toUpperCase().contains(artifactNamePartial.toUpperCase())) {
                    aiBaseSet.add(curEntry.getValue());
                }
            }
        }

        return aiBaseSet;
    }

    // private methods
    private Callable<Void> prepareTaskForServiceAnalysis(final String serviceName) {
        return () -> {
            try {
                getServiceArtifactInfo(serviceName);
            } catch (Exception exc) {
                Debug.logWarning(exc, "Error processing service: " + serviceName, MODULE);
            }
            return null;
        };
    }

    private Callable<Void> prepareTaskForComponentAnalysis(final ComponentConfig componentConfig) {
        return () -> {
            String componentName = componentConfig.getGlobalName();
            String rootComponentPath = componentConfig.rootLocation().toString();
            List<File> screenFiles = new ArrayList<>();
            List<File> formFiles = new ArrayList<>();
            List<File> controllerFiles = new ArrayList<>();
            try {
                screenFiles = FileUtil.findXmlFiles(rootComponentPath, null, "screens", "widget-screen.xsd");
            } catch (IOException ioe) {
                Debug.logWarning(ioe.getMessage(), MODULE);
            }
            try {
                formFiles = FileUtil.findXmlFiles(rootComponentPath, null, "forms", "widget-form.xsd");
            } catch (IOException ioe) {
                Debug.logWarning(ioe.getMessage(), MODULE);
            }
            try {
                controllerFiles = FileUtil.findXmlFiles(rootComponentPath, null, "site-conf", "site-conf.xsd");
            } catch (IOException ioe) {
                Debug.logWarning(ioe.getMessage(), MODULE);
            }
            for (File screenFile: screenFiles) {
                String screenFilePath = screenFile.getAbsolutePath();
                screenFilePath = screenFilePath.replace('\\', '/');
                String screenFileRelativePath = screenFilePath.substring(rootComponentPath.length());
                String screenLocation = "component://" + componentName + "/" + screenFileRelativePath;
                Map<String, ModelScreen> modelScreenMap = null;
                try {
                    modelScreenMap = ScreenFactory.getScreensFromLocation(screenLocation);
                } catch (Exception exc) {
                    Debug.logWarning(exc.getMessage(), MODULE);
                }
                for (String screenName : modelScreenMap.keySet()) {
                    getScreenWidgetArtifactInfo(screenName, screenLocation);
                }
            }
            for (File formFile: formFiles) {
                String formFilePath = formFile.getAbsolutePath();
                formFilePath = formFilePath.replace('\\', '/');
                String formFileRelativePath = formFilePath.substring(rootComponentPath.length());
                String formLocation = "component://" + componentName + "/" + formFileRelativePath;
                Map<String, ModelForm> modelFormMap = null;
                try {
                    modelFormMap = FormFactory.getFormsFromLocation(formLocation, getEntityModelReader(),
                            ThemeFactory.getVisualThemeFromId("COMMON"), getDispatchContext());
                } catch (Exception exc) {
                    Debug.logWarning(exc.getMessage(), MODULE);
                }
                for (String formName : modelFormMap.keySet()) {
                    try {
                        getFormWidgetArtifactInfo(formName, formLocation);
                    } catch (GeneralException ge) {
                        Debug.logWarning(ge.getMessage(), MODULE);
                    }
                }
            }
            for (File controllerFile: controllerFiles) {
                URL controllerUrl = null;
                try {
                    controllerUrl = controllerFile.toURI().toURL();
                } catch (MalformedURLException mue) {
                    Debug.logWarning(mue.getMessage(), MODULE);
                }
                if (controllerUrl == null) continue;
                ControllerConfig cc = ConfigXMLReader.getControllerConfig(controllerUrl);
                for (String requestUri: cc.getRequestMapMap().keySet()) {
                    try {
                        getControllerRequestArtifactInfo(controllerUrl, requestUri);
                    } catch (GeneralException e) {
                        Debug.logWarning(e.getMessage(), MODULE);
                    }
                }
                for (String viewUri: cc.getViewMapMap().keySet()) {
                    try {
                        getControllerViewArtifactInfo(controllerUrl, viewUri);
                    } catch (GeneralException e) {
                        Debug.logWarning(e.getMessage(), MODULE);
                    }
                }
            }
            return null;
        };
    }

}
