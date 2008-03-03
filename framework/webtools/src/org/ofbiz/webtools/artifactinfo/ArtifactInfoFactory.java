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

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import javolution.util.FastMap;

import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.model.ModelEntity;
import org.ofbiz.entity.model.ModelReader;
import org.ofbiz.entityext.eca.EntityEcaRule;
import org.ofbiz.entityext.eca.EntityEcaUtil;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.ModelService;
import org.ofbiz.service.eca.ServiceEcaRule;
import org.ofbiz.service.eca.ServiceEcaUtil;
import org.ofbiz.widget.form.FormFactory;
import org.ofbiz.widget.form.ModelForm;
import org.ofbiz.widget.screen.ModelScreen;
import org.ofbiz.widget.screen.ScreenFactory;
import org.xml.sax.SAXException;

/**
 *
 */
public class ArtifactInfoFactory {
    
    protected static Map<String, ArtifactInfoFactory> artifactInfoFactoryCache = FastMap.newInstance();
    
    protected String delegatorName;
    protected ModelReader entityModelReader;
    protected DispatchContext dispatchContext;
    protected Map<String, Map<String, List<EntityEcaRule>>> entityEcaCache;
    protected Map<String, Map<String, List<ServiceEcaRule>>> serviceEcaCache;
    
    public Map<String, EntityArtifactInfo> allEntityInfos = FastMap.newInstance();
    public Map<String, ServiceArtifactInfo> allServiceInfos = FastMap.newInstance();
    public Map<String, FormWidgetArtifactInfo> allFormInfos = FastMap.newInstance();
    public Map<String, ScreenWidgetArtifactInfo> allScreenInfos = FastMap.newInstance();

    // reverse-associative caches for walking backward in the diagram
    public Map<String, Set<ServiceEcaArtifactInfo>> allServiceEcaInfosReferringToServiceName = FastMap.newInstance();
    public Map<String, Set<ServiceArtifactInfo>> allServiceInfosReferringToServiceName = FastMap.newInstance();
    public Map<String, Set<ServiceArtifactInfo>> allServiceInfosReferringToEntityName = FastMap.newInstance();
    public Map<String, Set<ServiceArtifactInfo>> allFormInfosReferringToServiceName = FastMap.newInstance();
    public Map<String, Set<ServiceArtifactInfo>> allFormInfosReferringToEntityName = FastMap.newInstance();
    public Map<String, Set<ServiceArtifactInfo>> allScreenInfosReferringToServiceName = FastMap.newInstance();
    public Map<String, Set<ServiceArtifactInfo>> allScreenInfosReferringToEntityName = FastMap.newInstance();
    
    public static ArtifactInfoFactory makeArtifactInfoFactory(String delegatorName) throws GenericEntityException {
        if (UtilValidate.isEmpty(delegatorName)) {
            delegatorName = "default";
        }
        
        ArtifactInfoFactory aic = artifactInfoFactoryCache.get(delegatorName);
        if (aic == null) {
            aic = new ArtifactInfoFactory(delegatorName);
        }
        return aic;
    }
    
    protected ArtifactInfoFactory(String delegatorName) throws GenericEntityException {
        this.delegatorName = delegatorName;
        this.entityModelReader = ModelReader.getModelReader(delegatorName);
        this.dispatchContext = new DispatchContext("ArtifactInfoDispCtx", null, this.getClass().getClassLoader(), null);
        this.entityEcaCache = EntityEcaUtil.getEntityEcaCache(EntityEcaUtil.getEntityEcaReaderName(delegatorName));
        this.serviceEcaCache = ServiceEcaUtil.ecaCache;
    }
    
    public void prepareAll() throws GeneralException {
        Set<String> entityNames = this.getEntityModelReader().getEntityNames();
        for (String entityName: entityNames) {
            this.getEntityArtifactInfo(entityName);
        }
        
        Set<String> serviceNames = this.getDispatchContext().getAllServiceNames();
        for (String serviceName: serviceNames) {
            this.getServiceArtifactInfo(serviceName);
        }
        
        // TODO: how to get all Service ECAs to prepare?
        
        // TODO: how to get all forms to prepare?
        
        // TODO: how to get all screens to prepare?
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
    
    public ModelForm getModelForm(String formName, String formLocation) throws ParserConfigurationException, SAXException, IOException {
        return FormFactory.getFormFromLocation(formLocation, formName, this.entityModelReader, this.dispatchContext);
    }
    
    public ModelScreen getModelScreen(String screenName, String screenLocation) throws ParserConfigurationException, SAXException, IOException {
        return ScreenFactory.getScreenFromLocation(screenLocation, screenName);
    }

    public EntityArtifactInfo getEntityArtifactInfo(String entityName) throws GeneralException {
        EntityArtifactInfo curInfo = this.allEntityInfos.get(entityName);
        if (curInfo == null) {
            curInfo = new EntityArtifactInfo(entityName, this);
            this.allEntityInfos.put(entityName, curInfo);
        }
        return curInfo;
    }
    
    public ServiceArtifactInfo getServiceArtifactInfo(String serviceName) throws GeneralException {
        ServiceArtifactInfo curInfo = this.allServiceInfos.get(serviceName);
        if (curInfo == null) {
            curInfo = new ServiceArtifactInfo(serviceName, this);
            this.allServiceInfos.put(serviceName, curInfo);
        }
        return curInfo;
    }
    
    public FormWidgetArtifactInfo getFormWidgetArtifactInfo(String formName, String formLocation) throws GeneralException, IOException, SAXException, ParserConfigurationException {
        FormWidgetArtifactInfo curInfo = this.allFormInfos.get(formName + formLocation);
        if (curInfo == null) {
            curInfo = new FormWidgetArtifactInfo(formName, formLocation, this);
            this.allFormInfos.put(formName + formLocation, curInfo);
        }
        return curInfo;
    }
    
    public ScreenWidgetArtifactInfo getScreenWidgetArtifactInfo(String screenName, String screenLocation) throws GeneralException, IOException, SAXException, ParserConfigurationException {
        ScreenWidgetArtifactInfo curInfo = this.allScreenInfos.get(screenName + screenLocation);
        if (curInfo == null) {
            curInfo = new ScreenWidgetArtifactInfo(screenName, screenLocation, this);
            this.allScreenInfos.put(screenName + screenLocation, curInfo);
        }
        return curInfo;
    }
}
