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

import javolution.util.FastList;

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

	static public class ArtifactInfoContext {
		protected String delegatorName;
		protected ModelReader entityModelReader;
        protected DispatchContext dispatchContext;
        protected Map<String, Map<String, List<EntityEcaRule>>> entityEcaCache;
        protected Map<String, Map<String, List<ServiceEcaRule>>> serviceEcaCache;
        
        public ArtifactInfoContext(String delegatorName) throws GenericEntityException {
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
	
	static public class EntityInfo {
		protected ArtifactInfoContext aic;
		protected ModelEntity modelEntity;
		
		public EntityInfo(String entityName, ArtifactInfoContext aic) throws GenericEntityException {
			this.aic = aic;
			this.modelEntity = this.aic.getModelEntity(entityName);
		}
		
		public List<ModelEntity> getEntitiesRelatedOne() {
			List<ModelEntity> entityList = FastList.newInstance();
			// TODO: implement this
			return entityList;
		}

		public List<ModelEntity> getEntitiesRelatedMany() {
			List<ModelEntity> entityList = FastList.newInstance();
			// TODO: implement this
			return entityList;
		}
		
		/** Get the Services that use this Entity */
		public List<ModelService> getServicesUsingEntity() {
			List<ModelService> serviceList = FastList.newInstance();
			// TODO: implement this
			return serviceList;
		}
		
		/** Get the Services called by Entity ECA */
		public List<ModelService> getServicesCalledByEntityEca() {
			List<ModelService> serviceList = FastList.newInstance();
			// TODO: implement this
			return serviceList;
		}
		
		public List<EntityEcaRule> getEntityEcaRules() {
			List<EntityEcaRule> eecaList = FastList.newInstance();
			// TODO: implement this
			return eecaList;
		}
		
		public List<ModelForm> getFormsUsingEntity() {
			List<ModelForm> formList = FastList.newInstance();
			// TODO: implement this
			return formList;
		}
		
		public List<ModelScreen> getScreensUsingEntity() {
			List<ModelScreen> screenList = FastList.newInstance();
			// TODO: implement this
			return screenList;
		}
	}

	static public class ServiceInfo {
		protected ArtifactInfoContext aic;
		protected ModelService modelService;
		
		public ServiceInfo(String serviceName, ArtifactInfoContext aic) throws GenericServiceException {
			this.aic = aic;
			this.modelService = this.aic.getModelService(serviceName);
		}
		
		public List<ModelEntity> getEntitiesUsedByService() {
			List<ModelEntity> entityList = FastList.newInstance();
			// TODO: implement this
			return entityList;
		}
		
		public List<ModelService> getServicesCallingService() {
			List<ModelService> serviceList = FastList.newInstance();
			// TODO: implement this
			return serviceList;
		}
		
		public List<ModelService> getServicesCalledByService() {
			List<ModelService> serviceList = FastList.newInstance();
			// TODO: implement this
			return serviceList;
		}
		
		public List<ModelService> getServicesCalledByServiceEca() {
			List<ModelService> serviceList = FastList.newInstance();
			// TODO: implement this
			return serviceList;
		}
		
		public List<ServiceEcaRule> getServiceEcaRulesTriggeredByService() {
			List<ServiceEcaRule> secaList = FastList.newInstance();
			// TODO: implement this
			return secaList;
		}
		
		public List<ModelService> getServicesCallingServiceByEca() {
			List<ModelService> serviceList = FastList.newInstance();
			// TODO: implement this
			return serviceList;
		}
		
		public List<ServiceEcaRule> getServiceEcaRulesCallingService() {
			List<ServiceEcaRule> secaList = FastList.newInstance();
			// TODO: implement this
			return secaList;
		}
		
		public List<ModelForm> getFormsCallingService() {
			List<ModelForm> formList = FastList.newInstance();
			// TODO: implement this
			return formList;
		}
		
		public List<ModelForm> getFormsBasedOnService() {
			List<ModelForm> formList = FastList.newInstance();
			// TODO: implement this
			return formList;
		}
		
		public List<ModelScreen> getScreensCallingService() {
			List<ModelScreen> screenList = FastList.newInstance();
			// TODO: implement this
			return screenList;
		}
		
		public List<ModelScreen> getRequestsWithEventCallingService() {
			List<ModelScreen> screenList = FastList.newInstance();
			// TODO: implement this
			return screenList;
		}
	}

	static public class FormInfo {
		protected ArtifactInfoContext aic;
		protected ModelForm modelForm;
		
		public FormInfo(String formName, String formLocation, ArtifactInfoContext aic) throws ParserConfigurationException, SAXException, IOException {
			this.aic = aic;
            this.modelForm = aic.getModelForm(formName, formLocation);
		}
	}		

	static public class ScreenInfo {
		protected ArtifactInfoContext aic;
		protected ModelScreen modelScreen;
		
		public ScreenInfo(String screenName, String screenLocation, ArtifactInfoContext aic) throws ParserConfigurationException, SAXException, IOException {
			this.aic = aic;
            this.modelScreen = aic.getModelScreen(screenName, screenLocation);
		}
	}		
}
