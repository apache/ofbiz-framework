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
import org.xml.sax.SAXException;

public class ArtifactInfoFactory {

    public static final String module = ArtifactInfoFactory.class.getName();

    private static final UtilCache<String, ArtifactInfoFactory> artifactInfoFactoryCache = UtilCache.createUtilCache("ArtifactInfoFactory");

    public static final String EntityInfoTypeId = "entity";
    public static final String ServiceInfoTypeId = "service";
    public static final String ServiceEcaInfoTypeId = "serviceEca";
    public static final String FormWidgetInfoTypeId = "form";
    public static final String ScreenWidgetInfoTypeId = "screen";
    public static final String ControllerRequestInfoTypeId = "request";
    public static final String ControllerViewInfoTypeId = "view";

    protected final String delegatorName;
    protected final ModelReader entityModelReader;
    protected final DispatchContext dispatchContext;

    public Map<String, EntityArtifactInfo> allEntityInfos = new ConcurrentHashMap<String, EntityArtifactInfo>();
    public Map<String, ServiceArtifactInfo> allServiceInfos = new ConcurrentHashMap<String, ServiceArtifactInfo>();
    public Map<ServiceEcaRule, ServiceEcaArtifactInfo> allServiceEcaInfos = new ConcurrentHashMap<ServiceEcaRule, ServiceEcaArtifactInfo>();
    public Map<String, FormWidgetArtifactInfo> allFormInfos = new ConcurrentHashMap<String, FormWidgetArtifactInfo>();
    public Map<String, ScreenWidgetArtifactInfo> allScreenInfos = new ConcurrentHashMap<String, ScreenWidgetArtifactInfo>();
    public Map<String, ControllerRequestArtifactInfo> allControllerRequestInfos = new ConcurrentHashMap<String, ControllerRequestArtifactInfo>();
    public Map<String, ControllerViewArtifactInfo> allControllerViewInfos = new ConcurrentHashMap<String, ControllerViewArtifactInfo>();

    // reverse-associative caches for walking backward in the diagram
    public Map<String, Set<ServiceEcaArtifactInfo>> allServiceEcaInfosReferringToServiceName = new ConcurrentHashMap<String, Set<ServiceEcaArtifactInfo>>();
    public Map<String, Set<ServiceArtifactInfo>> allServiceInfosReferringToServiceName = new ConcurrentHashMap<String, Set<ServiceArtifactInfo>>();
    public Map<String, Set<FormWidgetArtifactInfo>> allFormInfosReferringToServiceName = new ConcurrentHashMap<String, Set<FormWidgetArtifactInfo>>();
    public Map<String, Set<FormWidgetArtifactInfo>> allFormInfosBasedOnServiceName = new ConcurrentHashMap<String, Set<FormWidgetArtifactInfo>>();
    public Map<String, Set<ScreenWidgetArtifactInfo>> allScreenInfosReferringToServiceName = new ConcurrentHashMap<String, Set<ScreenWidgetArtifactInfo>>();
    public Map<String, Set<ControllerRequestArtifactInfo>> allRequestInfosReferringToServiceName = new ConcurrentHashMap<String, Set<ControllerRequestArtifactInfo>>();

    public Map<String, Set<ServiceArtifactInfo>> allServiceInfosReferringToEntityName = new ConcurrentHashMap<String, Set<ServiceArtifactInfo>>();
    public Map<String, Set<FormWidgetArtifactInfo>> allFormInfosReferringToEntityName = new ConcurrentHashMap<String, Set<FormWidgetArtifactInfo>>();
    public Map<String, Set<ScreenWidgetArtifactInfo>> allScreenInfosReferringToEntityName = new ConcurrentHashMap<String, Set<ScreenWidgetArtifactInfo>>();

    public Map<ServiceEcaRule, Set<ServiceArtifactInfo>> allServiceInfosReferringToServiceEcaRule = new ConcurrentHashMap<ServiceEcaRule, Set<ServiceArtifactInfo>>();

    public Map<String, Set<FormWidgetArtifactInfo>> allFormInfosExtendingForm = new ConcurrentHashMap<String, Set<FormWidgetArtifactInfo>>();
    public Map<String, Set<ScreenWidgetArtifactInfo>> allScreenInfosReferringToForm = new ConcurrentHashMap<String, Set<ScreenWidgetArtifactInfo>>();

    public Map<String, Set<ScreenWidgetArtifactInfo>> allScreenInfosReferringToScreen = new ConcurrentHashMap<String, Set<ScreenWidgetArtifactInfo>>();
    public Map<String, Set<ControllerViewArtifactInfo>> allViewInfosReferringToScreen = new ConcurrentHashMap<String, Set<ControllerViewArtifactInfo>>();

    public Map<String, Set<ControllerRequestArtifactInfo>> allRequestInfosReferringToView = new ConcurrentHashMap<String, Set<ControllerRequestArtifactInfo>>();

    public Map<String, Set<FormWidgetArtifactInfo>> allFormInfosTargetingRequest = new ConcurrentHashMap<String, Set<FormWidgetArtifactInfo>>();
    public Map<String, Set<FormWidgetArtifactInfo>> allFormInfosReferringToRequest = new ConcurrentHashMap<String, Set<FormWidgetArtifactInfo>>();
    public Map<String, Set<ScreenWidgetArtifactInfo>> allScreenInfosReferringToRequest = new ConcurrentHashMap<String, Set<ScreenWidgetArtifactInfo>>();
    public Map<String, Set<ControllerRequestArtifactInfo>> allRequestInfosReferringToRequest = new ConcurrentHashMap<String, Set<ControllerRequestArtifactInfo>>();

    public static ArtifactInfoFactory getArtifactInfoFactory(String delegatorName) throws GeneralException {
        if (UtilValidate.isEmpty(delegatorName)) {
            delegatorName = "default";
        }

        ArtifactInfoFactory aif = artifactInfoFactoryCache.get(delegatorName);
        if (aif == null) {
            aif = artifactInfoFactoryCache.putIfAbsentAndGet(delegatorName, new ArtifactInfoFactory(delegatorName));
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

    public void prepareAll() throws GeneralException {
        Debug.logInfo("Loading artifact info objects...", module);
        List<Future<Void>> futures = new ArrayList<Future<Void>>();
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
        futures = new ArrayList<Future<Void>>();
        for (ComponentConfig componentConfig: componentConfigs) {
            futures.add(ExecutionPool.GLOBAL_FORK_JOIN.submit(prepareTaskForComponentAnalysis(componentConfig)));
        }
        ExecutionPool.getAllFutures(futures);
        Debug.logInfo("Artifact info objects loaded.", module);
    }

    public ModelReader getEntityModelReader() {
        return this.entityModelReader;
    }

    public DispatchContext getDispatchContext() {
        return this.dispatchContext;
    }

    public ModelEntity getModelEntity(String entityName) throws GenericEntityException {
        return this.getEntityModelReader().getModelEntity(entityName);
    }

    public ModelService getModelService(String serviceName) throws GenericServiceException {
        return this.getDispatchContext().getModelService(serviceName);
    }

    public ModelForm getModelForm(String formNameAndLocation) throws ParserConfigurationException, SAXException, IOException {
        return getModelForm(formNameAndLocation.substring(formNameAndLocation.indexOf("#") + 1), formNameAndLocation.substring(0, formNameAndLocation.indexOf("#")));
    }
    public ModelForm getModelForm(String formName, String formLocation) throws ParserConfigurationException, SAXException, IOException {
        return FormFactory.getFormFromLocation(formLocation, formName, this.entityModelReader, this.dispatchContext);
    }

    public ModelScreen getModelScreen(String screenName, String screenLocation) throws ParserConfigurationException, SAXException, IOException {
        return ScreenFactory.getScreenFromLocation(screenLocation, screenName);
    }

    public ConfigXMLReader.RequestMap getControllerRequestMap(URL controllerXmlUrl, String requestUri) {
        try {
            return ConfigXMLReader.getControllerConfig(controllerXmlUrl).getRequestMapMap().get(requestUri);
        } catch (WebAppConfigurationException e) {
            Debug.logError(e, "Exception thrown while parsing controller.xml file: ", module);
        }
        return null;
    }

    public ConfigXMLReader.ViewMap getControllerViewMap(URL controllerXmlUrl, String viewUri) {
        ControllerConfig cc;
        try {
            cc = ConfigXMLReader.getControllerConfig(controllerXmlUrl);
            return cc.getViewMapMap().get(viewUri);
        } catch (WebAppConfigurationException e) {
            Debug.logError(e, "Exception thrown while parsing controller.xml file: ", module);
        }
        return null;
    }

    public EntityArtifactInfo getEntityArtifactInfo(String entityName) throws GeneralException {
        EntityArtifactInfo curInfo = this.allEntityInfos.get(entityName);
        if (curInfo == null) {
            curInfo = new EntityArtifactInfo(entityName, this);
            this.allEntityInfos.put(entityName, curInfo);
            curInfo.populateAll();
        }
        return curInfo;
    }

    public ServiceArtifactInfo getServiceArtifactInfo(String serviceName) throws GeneralException {
        ServiceArtifactInfo curInfo = this.allServiceInfos.get(serviceName);
        if (curInfo == null) {
            curInfo = new ServiceArtifactInfo(serviceName, this);
            this.allServiceInfos.put(serviceName, curInfo);
            curInfo.populateAll();
        }
        return curInfo;
    }

    public ServiceEcaArtifactInfo getServiceEcaArtifactInfo(ServiceEcaRule ecaRule) throws GeneralException {
        ServiceEcaArtifactInfo curInfo = this.allServiceEcaInfos.get(ecaRule);
        if (curInfo == null) {
            curInfo = new ServiceEcaArtifactInfo(ecaRule, this);
            this.allServiceEcaInfos.put(ecaRule, curInfo);
            curInfo.populateAll();
        }
        return curInfo;
    }

    public FormWidgetArtifactInfo getFormWidgetArtifactInfo(String formNameAndLocation) throws GeneralException {
        return getFormWidgetArtifactInfo(formNameAndLocation.substring(formNameAndLocation.indexOf("#") + 1), formNameAndLocation.substring(0, formNameAndLocation.indexOf("#")));
    }
    public FormWidgetArtifactInfo getFormWidgetArtifactInfo(String formName, String formLocation) throws GeneralException {
        FormWidgetArtifactInfo curInfo = this.allFormInfos.get(formLocation + "#" + formName);
        if (curInfo == null) {
            curInfo = new FormWidgetArtifactInfo(formName, formLocation, this);
            this.allFormInfos.put(curInfo.getUniqueId(), curInfo);
            curInfo.populateAll();
        }
        return curInfo;
    }

    public ScreenWidgetArtifactInfo getScreenWidgetArtifactInfo(String screenName, String screenLocation) {
        ScreenWidgetArtifactInfo curInfo = this.allScreenInfos.get(screenLocation + "#" + screenName);
        if (curInfo == null) {
            try {
                curInfo = new ScreenWidgetArtifactInfo(screenName, screenLocation, this);
                this.allScreenInfos.put(curInfo.getUniqueId(), curInfo);
                curInfo.populateAll();
            } catch (GeneralException e) {
                Debug.logWarning("Error loading screen [" + screenName + "] from resource [" + screenLocation + "]: " + e.toString(), module);
                return null;
            }
        }
        return curInfo;
    }

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

    public ControllerViewArtifactInfo getControllerViewArtifactInfo(URL controllerXmlUrl, String viewUri) throws GeneralException {
        ControllerViewArtifactInfo curInfo = this.allControllerViewInfos.get(controllerXmlUrl.toExternalForm() + "#" + viewUri);
        if (curInfo == null) {
            curInfo = new ControllerViewArtifactInfo(controllerXmlUrl, viewUri, this);
            this.allControllerViewInfos.put(curInfo.getUniqueId(), curInfo);
        }
        return curInfo;
    }

    public ArtifactInfoBase getArtifactInfoByUniqueIdAndType(String uniqueId, String type) {
        if (uniqueId.contains("#")) {
            int poundIndex = uniqueId.indexOf('#');
            return getArtifactInfoByNameAndType(uniqueId.substring(poundIndex+1), uniqueId.substring(0, poundIndex), type);
        } else {
            return getArtifactInfoByNameAndType(uniqueId, null, type);
        }
    }

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
        } catch (GeneralException e) {
            Debug.logError(e, "Error getting artifact info: " + e.toString(), module);
        } catch (MalformedURLException e) {
            Debug.logError(e, "Error getting artifact info: " + e.toString(), module);
        }
        return null;
    }

    public Set<ArtifactInfoBase> getAllArtifactInfosByNamePartial(String artifactNamePartial, String type) {
        Set<ArtifactInfoBase> aiBaseSet = new HashSet<ArtifactInfoBase>();

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
        return new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                try {
                    getServiceArtifactInfo(serviceName);
                } catch(Exception exc) {
                    Debug.logWarning(exc, "Error processing service: " + serviceName, module);
                }
                return null;
            }
        };
    }

    private Callable<Void> prepareTaskForComponentAnalysis(final ComponentConfig componentConfig) {
        return new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                String componentName = componentConfig.getGlobalName();
                String rootComponentPath = componentConfig.getRootLocation();
                List<File> screenFiles = new ArrayList<File>();
                List<File> formFiles = new ArrayList<File>();
                List<File> controllerFiles = new ArrayList<File>();
                try {
                    screenFiles = FileUtil.findXmlFiles(rootComponentPath, null, "screens", "widget-screen.xsd");
                } catch (IOException ioe) {
                    Debug.logWarning(ioe.getMessage(), module);
                }
                try {
                    formFiles = FileUtil.findXmlFiles(rootComponentPath, null, "forms", "widget-form.xsd");
                } catch (IOException ioe) {
                    Debug.logWarning(ioe.getMessage(), module);
                }
                try {
                    controllerFiles = FileUtil.findXmlFiles(rootComponentPath, null, "site-conf", "site-conf.xsd");
                } catch (IOException ioe) {
                    Debug.logWarning(ioe.getMessage(), module);
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
                        Debug.logWarning(exc.getMessage(), module);
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
                        modelFormMap = FormFactory.getFormsFromLocation(formLocation, getEntityModelReader(), getDispatchContext());
                    } catch (Exception exc) {
                        Debug.logWarning(exc.getMessage(), module);
                    }
                    for (String formName : modelFormMap.keySet()) {
                        try {
                            getFormWidgetArtifactInfo(formName, formLocation);
                        } catch (GeneralException ge) {
                            Debug.logWarning(ge.getMessage(), module);
                        }
                    }
                }
                for (File controllerFile: controllerFiles) {
                    URL controllerUrl = null;
                    try {
                        controllerUrl = controllerFile.toURI().toURL();
                    } catch (MalformedURLException mue) {
                        Debug.logWarning(mue.getMessage(), module);
                    }
                    if (controllerUrl == null) continue;
                    ControllerConfig cc = ConfigXMLReader.getControllerConfig(controllerUrl);
                    for (String requestUri: cc.getRequestMapMap().keySet()) {
                        try {
                            getControllerRequestArtifactInfo(controllerUrl, requestUri);
                        } catch (GeneralException e) {
                            Debug.logWarning(e.getMessage(), module);
                        }
                    }
                    for (String viewUri: cc.getViewMapMap().keySet()) {
                        try {
                            getControllerViewArtifactInfo(controllerUrl, viewUri);
                        } catch (GeneralException e) {
                            Debug.logWarning(e.getMessage(), module);
                        }
                    }
                }
                return null;
            }
        };
    }

}
