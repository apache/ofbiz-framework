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

    public static EntityArtifactInfo makeEntityArtifactInfo(String entityName, String delegatorName) throws GeneralException {
        return new EntityArtifactInfo(entityName, ArtifactInfoContext.makeArtifactInfoContext(delegatorName));
    }
    
    public static ServiceArtifactInfo makeServiceArtifactInfo(String serviceName, String delegatorName) throws GeneralException {
        return new ServiceArtifactInfo(serviceName, ArtifactInfoContext.makeArtifactInfoContext(delegatorName));
    }
    
    public static FormWidgetArtifactInfo makeFormWidgetArtifactInfo(String formName, String formLocation, String delegatorName) throws GeneralException, IOException, SAXException, ParserConfigurationException {
        return new FormWidgetArtifactInfo(formName, formLocation, ArtifactInfoContext.makeArtifactInfoContext(delegatorName));
    }
    
    public static ScreenWidgetArtifactInfo makeScreenWidgetArtifactInfo(String screenName, String screenLocation, String delegatorName) throws GeneralException, IOException, SAXException, ParserConfigurationException {
        return new ScreenWidgetArtifactInfo(screenName, screenLocation, ArtifactInfoContext.makeArtifactInfoContext(delegatorName));
    }
    
    static public class ArtifactInfoContext {
        protected static Map<String, ArtifactInfoContext> artifactInfoContextCache = FastMap.newInstance();
        
        protected String delegatorName;
        protected ModelReader entityModelReader;
        protected DispatchContext dispatchContext;
        protected Map<String, Map<String, List<EntityEcaRule>>> entityEcaCache;
        protected Map<String, Map<String, List<ServiceEcaRule>>> serviceEcaCache;
        
        public static ArtifactInfoContext makeArtifactInfoContext(String delegatorName) throws GenericEntityException {
            if (UtilValidate.isEmpty(delegatorName)) {
                delegatorName = "default";
            }
            
            ArtifactInfoContext aic = artifactInfoContextCache.get(delegatorName);
            if (aic == null) {
                aic = new ArtifactInfoContext(delegatorName);
            }
            return aic;
        }
        
        protected ArtifactInfoContext(String delegatorName) throws GenericEntityException {
            this.delegatorName = delegatorName;
            this.entityModelReader = ModelReader.getModelReader(delegatorName);
            this.dispatchContext = new DispatchContext("ArtifactInfoDispCtx", null, this.getClass().getClassLoader(), null);
            this.entityEcaCache = EntityEcaUtil.getEntityEcaCache(EntityEcaUtil.getEntityEcaReaderName(delegatorName));
            this.serviceEcaCache = ServiceEcaUtil.ecaCache;
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
    }
}
