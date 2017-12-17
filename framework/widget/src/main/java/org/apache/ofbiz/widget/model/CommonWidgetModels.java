/*******************************************************************************
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
 *******************************************************************************/
package org.apache.ofbiz.widget.model;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilCodec;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.UtilXml;
import org.apache.ofbiz.base.util.collections.FlexibleMapAccessor;
import org.apache.ofbiz.base.util.string.FlexibleStringExpander;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.model.ModelEntity;
import org.apache.ofbiz.entity.model.ModelField;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ModelParam;
import org.apache.ofbiz.service.ModelService;
import org.w3c.dom.Element;

/**
 * A collection of shared/reused widget models.
 *
 */
public final class CommonWidgetModels {

    public static final String module = CommonWidgetModels.class.getName();

    private CommonWidgetModels() {
    }

    public static class AutoEntityParameters {
        private String entityName;
        List<String> excludeList = new ArrayList<>();
        boolean includeNonPk;
        boolean includePk;
        private String includeType;
        boolean sendIfEmpty;

        public AutoEntityParameters(Element autoElement) {
            entityName = UtilXml.checkEmpty(autoElement.getAttribute("entity-name"));
            sendIfEmpty = "true".equals(autoElement.getAttribute("send-if-empty"));
            includeType = UtilXml.checkEmpty(autoElement.getAttribute("include"));
            includePk = "pk".equals(includeType) || "all".equals(includeType);
            includeNonPk = "nonpk".equals(includeType) || "all".equals(includeType);
            List<? extends Element> excludes = UtilXml.childElementList(autoElement, "exclude");
            if (excludes != null) {
                for (Element exclude : excludes) {
                    if (UtilValidate.isNotEmpty(exclude.getAttribute("field-name"))) {
                        excludeList.add(exclude.getAttribute("field-name"));
                    }
                }
            }
        }

        @SuppressWarnings("unchecked")
        public Map<String, String> getParametersMap(Map<String, Object> context, String defaultEntityName) {
            Map<String, String> autEntityParams = new HashMap<>();
            Delegator delegator = (Delegator) context.get("delegator");
            if (delegator == null) {
                Debug.logError(
                        "We can not append auto entity Parameters since we could not find delegator in the current context",
                        module);
                return autEntityParams;
            }
            if (UtilValidate.isEmpty(entityName)) {
                entityName = defaultEntityName;
            }
            FlexibleStringExpander toExpand = FlexibleStringExpander.getInstance(entityName);
            ModelEntity entity = delegator.getModelEntity(toExpand.expandString(context));
            if (entity == null) {
                Debug.logError("We can not append auto entity Parameters since we could not find entity with name [" + entityName
                        + "]", module);
                return autEntityParams;
            }

            Iterator<ModelField> fieldsIter = entity.getFieldsIterator();
            while (fieldsIter.hasNext()) {
                ModelField field = fieldsIter.next();
                String fieldName = field.getName();
                FlexibleMapAccessor<Object> fma = FlexibleMapAccessor.getInstance(fieldName);
                boolean shouldExclude = excludeList.contains(fieldName);
                if ((!shouldExclude) && (!field.getIsAutoCreatedInternal())
                    && ((field.getIsPk() && includePk) || (!field.getIsPk() && includeNonPk))) {
                    Object flexibleValue = fma.get(context);
                    if (UtilValidate.isEmpty(flexibleValue) && context.containsKey("parameters")) {
                        flexibleValue = fma.get((Map<String, Object>) context.get("parameters"));
                    }
                    if (UtilValidate.isNotEmpty(flexibleValue) || sendIfEmpty) {
                        autEntityParams.put(fieldName, String.valueOf(flexibleValue));
                    }
                }
            }
            return autEntityParams;
        }
    }

    public static class AutoServiceParameters {
        List<String> excludeList = new ArrayList<>();
        boolean sendIfEmpty;
        private String serviceName;

        public AutoServiceParameters(Element autoElement) {
            serviceName = UtilXml.checkEmpty(autoElement.getAttribute("service-name"));
            sendIfEmpty = "true".equals(autoElement.getAttribute("send-if-empty"));
            List<? extends Element> excludes = UtilXml.childElementList(autoElement, "exclude");
            if (excludes != null) {
                for (Element exclude : excludes) {
                    if (UtilValidate.isNotEmpty(exclude.getAttribute("field-name"))) {
                        excludeList.add(exclude.getAttribute("field-name"));
                    }
                }
            }
        }

        @SuppressWarnings("unchecked")
        public Map<String, String> getParametersMap(Map<String, Object> context, String defaultServiceName) {
            Map<String, String> autServiceParams = new HashMap<>();
            LocalDispatcher dispatcher = (LocalDispatcher) context.get("dispatcher");
            if (dispatcher == null) {
                Debug.logError(
                        "We can not append auto service Parameters since we could not find dispatcher in the current context",
                        module);
                return autServiceParams;
            }
            if (UtilValidate.isEmpty(serviceName)) {
                serviceName = defaultServiceName;
            }
            FlexibleStringExpander toExpand = FlexibleStringExpander.getInstance(serviceName);
            ModelService service = null;
            try {
                service = dispatcher.getDispatchContext().getModelService(toExpand.toString());
            } catch (GenericServiceException e) {
                Debug.logError("Resolve service throw an error : " + e, module);
            }
            if (service == null) {
                Debug.logError("We can not append auto service Parameters since we could not find service with name ["
                        + serviceName + "]", module);
                return autServiceParams;
            }
            Iterator<ModelParam> paramsIter = service.getInModelParamList().iterator();
            if (paramsIter != null) {
                while (paramsIter.hasNext()) {
                    ModelParam param = paramsIter.next();
                    if (param.getInternal()) {
                        continue;
                    }
                    String paramName = param.getName();
                    FlexibleMapAccessor<Object> fma = FlexibleMapAccessor.getInstance(paramName);
                    if (!excludeList.contains(paramName)) {
                        Object flexibleValue = fma.get(context);
                        if (UtilValidate.isEmpty(flexibleValue) && context.containsKey("parameters")) {
                            flexibleValue = fma.get((Map<String, ? extends Object>) context.get("parameters"));
                        }
                        if (UtilValidate.isNotEmpty(flexibleValue) || sendIfEmpty) {
                            autServiceParams.put(paramName, String.valueOf(flexibleValue));
                        }
                    }
                }
            }
            return autServiceParams;
        }
    }

    public static final class Image {
        private final FlexibleStringExpander alt;
        private final FlexibleStringExpander borderExdr;
        private final FlexibleStringExpander heightExdr;
        private final FlexibleStringExpander idExdr;
        private final String name;
        private final FlexibleStringExpander srcExdr;
        private final FlexibleStringExpander styleExdr;
        private final FlexibleStringExpander titleExdr;
        private final String urlMode;
        private final FlexibleStringExpander widthExdr;

        public Image(Element imageElement) {
            if (!imageElement.getAttribute("image-location").isEmpty()) {
                // Form field version, log warning.
                this.srcExdr = FlexibleStringExpander.getInstance(imageElement.getAttribute("image-location"));
                this.alt = FlexibleStringExpander.getInstance(imageElement.getAttribute("alternate"));
                this.titleExdr = FlexibleStringExpander.getInstance(imageElement.getAttribute("image-title"));
                this.name = "";
                this.idExdr = FlexibleStringExpander.getInstance("");
                this.styleExdr = FlexibleStringExpander.getInstance("");
                this.widthExdr = FlexibleStringExpander.getInstance(imageElement.getAttribute("width"));
                this.heightExdr = FlexibleStringExpander.getInstance(imageElement.getAttribute("height"));
                this.borderExdr = FlexibleStringExpander.getInstance("");
                this.urlMode = "content";
            } else {
                this.srcExdr = FlexibleStringExpander.getInstance(imageElement.getAttribute("src"));
                this.alt = FlexibleStringExpander.getInstance(imageElement.getAttribute("alt"));
                this.titleExdr = FlexibleStringExpander.getInstance(imageElement.getAttribute("title"));
                this.name = imageElement.getAttribute("name");
                this.idExdr = FlexibleStringExpander.getInstance(imageElement.getAttribute("id"));
                this.styleExdr = FlexibleStringExpander.getInstance(imageElement.getAttribute("style"));
                this.widthExdr = FlexibleStringExpander.getInstance(imageElement.getAttribute("width"));
                this.heightExdr = FlexibleStringExpander.getInstance(imageElement.getAttribute("height"));
                this.borderExdr = FlexibleStringExpander.getInstance(imageElement.getAttribute("border"));
                String urlMode = imageElement.getAttribute("url-mode");
                if (urlMode.isEmpty()) {
                    urlMode = "content";
                }
                this.urlMode = urlMode;
            }
        }

        public FlexibleStringExpander getAlt() {
            return alt;
        }

        public String getAlt(Map<String, Object> context) {
            String alt = this.alt.expandString(context);
            // FIXME: Encoding should be done by the renderer, not by the model.
            UtilCodec.SimpleEncoder simpleEncoder = (UtilCodec.SimpleEncoder) context.get("simpleEncoder");
            if (simpleEncoder != null) {
                alt = simpleEncoder.encode(alt);
            }
            return alt;
        }

        public String getBorder(Map<String, Object> context) {
            return this.borderExdr.expandString(context);
        }

        public FlexibleStringExpander getBorderExdr() {
            return borderExdr;
        }

        public String getHeight(Map<String, Object> context) {
            return this.heightExdr.expandString(context);
        }

        public FlexibleStringExpander getHeightExdr() {
            return heightExdr;
        }

        public String getId(Map<String, Object> context) {
            return this.idExdr.expandString(context);
        }

        public FlexibleStringExpander getIdExdr() {
            return idExdr;
        }

        public String getName() {
            return name;
        }

        public String getSrc(Map<String, Object> context) {
            return this.srcExdr.expandString(context);
        }

        public FlexibleStringExpander getSrcExdr() {
            return srcExdr;
        }

        public String getStyle(Map<String, Object> context) {
            return this.styleExdr.expandString(context);
        }

        public FlexibleStringExpander getStyleExdr() {
            return styleExdr;
        }

        public FlexibleStringExpander getTitleExdr() {
            return titleExdr;
        }

        public String getUrlMode() {
            return this.urlMode;
        }

        public String getWidth(Map<String, Object> context) {
            return this.widthExdr.expandString(context);
        }

        public FlexibleStringExpander getWidthExdr() {
            return widthExdr;
        }
    }

    public static final class Link {
        // FIXME: This is a bad practice. Client code should not need to "know" what this value is.
        public static final String DEFAULT_URL_MODE = "intra-app";
        private final AutoEntityParameters autoEntityParameters;
        private final AutoServiceParameters autoServiceParameters;
        private final boolean encode;
        private final boolean fullPath;
        private final FlexibleStringExpander idExdr;
        private final Image image;
        private final String linkType; // anchor or hidden form
        private final FlexibleStringExpander nameExdr;
        private final List<Parameter> parameterList;
        private final FlexibleStringExpander prefixExdr;
        private final boolean secure;
        private final Integer size;
        private final FlexibleStringExpander styleExdr;
        private final FlexibleStringExpander targetExdr;
        private final FlexibleStringExpander targetWindowExdr;
        private final FlexibleStringExpander textExdr;
        private final String urlMode;
        private final boolean requestConfirmation;
        private final FlexibleStringExpander confirmationMsgExdr;
        // FIXME: These don't belong in this class (might have been used for image)
        private final String height;
        private final String width;

        public Link(Element linkElement) {
            this.textExdr = FlexibleStringExpander.getInstance(linkElement.getAttribute("text"));
            this.idExdr = FlexibleStringExpander.getInstance(linkElement.getAttribute("id"));
            this.styleExdr = FlexibleStringExpander.getInstance(linkElement.getAttribute("style"));
            this.nameExdr = FlexibleStringExpander.getInstance(linkElement.getAttribute("name"));
            this.targetExdr = FlexibleStringExpander.getInstance(linkElement.getAttribute("target"));
            this.targetWindowExdr = FlexibleStringExpander.getInstance(linkElement.getAttribute("target-window"));
            this.prefixExdr = FlexibleStringExpander.getInstance(linkElement.getAttribute("prefix"));
            this.urlMode = linkElement.getAttribute("url-mode");
            this.fullPath = "true".equals(linkElement.getAttribute("full-path"));
            this.secure = "true".equals(linkElement.getAttribute("secure"));
            this.encode = "true".equals(linkElement.getAttribute("encode"));
            Element imageElement = UtilXml.firstChildElement(linkElement, "image");
            if (imageElement != null) {
                this.image = new Image(imageElement);
            } else {
                if (!linkElement.getAttribute("image-location").isEmpty()) {
                    // Backwards compatibility
                    this.image = new Image(linkElement);
                } else {
                    this.image = null;
                }
            }
            //Backwards compatibility
            if ("ajax-window".equals(linkElement.getAttribute("link-type"))) {
                this.linkType = "layered-modal";
            } else {
                this.linkType = linkElement.getAttribute("link-type");
            }
            List<? extends Element> parameterElementList = UtilXml.childElementList(linkElement, "parameter");
            if (parameterElementList.isEmpty()) {
                this.parameterList = Collections.emptyList();
            } else {
                List<Parameter> parameterList = new ArrayList<>(
                        parameterElementList.size());
                for (Element parameterElement : parameterElementList) {
                    parameterList.add(new Parameter(parameterElement));
                }
                this.parameterList = Collections.unmodifiableList(parameterList);
            }
            Element autoServiceParamsElement = UtilXml.firstChildElement(linkElement, "auto-parameters-service");
            if (autoServiceParamsElement != null) {
                this.autoServiceParameters = new AutoServiceParameters(autoServiceParamsElement);
            } else {
                this.autoServiceParameters = null;
            }
            Element autoEntityParamsElement = UtilXml.firstChildElement(linkElement, "auto-parameters-entity");
            if (autoEntityParamsElement != null) {
                this.autoEntityParameters = new AutoEntityParameters(autoEntityParamsElement);
            } else {
                this.autoEntityParameters = null;
            }
            Integer size = null;
            String sizeAttr = linkElement.getAttribute("size");
            if (!sizeAttr.isEmpty()) {
                size = Integer.valueOf(sizeAttr);
            }
            this.size = size;
            this.requestConfirmation = "true".equals(linkElement.getAttribute("request-confirmation"));
            this.confirmationMsgExdr = FlexibleStringExpander.getInstance(linkElement.getAttribute("confirmation-message"));
            this.width = linkElement.getAttribute("width");
            this.height = linkElement.getAttribute("height");
        }

        // Portal constructor
        public Link(GenericValue portalPage, List<Parameter> parameterList, String target, Locale locale) {
            this.autoEntityParameters = null;
            this.autoServiceParameters = null;
            this.encode = false;
            this.fullPath = false;
            this.idExdr = FlexibleStringExpander.getInstance("");
            this.image = null;
            this.linkType = "";
            this.nameExdr = FlexibleStringExpander.getInstance("");
            this.parameterList = Collections.unmodifiableList(parameterList);
            this.prefixExdr = FlexibleStringExpander.getInstance("");
            this.secure = false;
            this.styleExdr = FlexibleStringExpander.getInstance("");
            this.targetExdr = FlexibleStringExpander.getInstance(target);
            this.targetWindowExdr = FlexibleStringExpander.getInstance("");
            this.textExdr = FlexibleStringExpander.getInstance((String) portalPage.get("portalPageName", locale));
            this.urlMode = "intra-app";
            this.size = null;
            this.requestConfirmation = false;
            this.confirmationMsgExdr = FlexibleStringExpander.getInstance("");
            this.width = "";
            this.height = "";
        }

        public AutoEntityParameters getAutoEntityParameters() {
            return autoEntityParameters;
        }

        public AutoServiceParameters getAutoServiceParameters() {
            return autoServiceParameters;
        }

        public String getConfirmationMsg(Map<String, Object> context) {
            return this.confirmationMsgExdr.expandString(context);
        }

        public FlexibleStringExpander getConfirmationMsgExdr() {
            return confirmationMsgExdr;
        }

        public boolean getEncode() {
            return this.encode;
        }

        public boolean getFullPath() {
            return this.fullPath;
        }

        public String getHeight() {
            return this.height;
        }

        public String getId(Map<String, Object> context) {
            return this.idExdr.expandString(context);
        }

        public FlexibleStringExpander getIdExdr() {
            return idExdr;
        }

        public Image getImage() {
            return this.image;
        }

        public String getLinkType() {
            return this.linkType;
        }

        public String getName() {
            return nameExdr.getOriginal();
        }

        public String getName(Map<String, Object> context) {
            return this.nameExdr.expandString(context);
        }

        public FlexibleStringExpander getNameExdr() {
            return nameExdr;
        }

        public List<Parameter> getParameterList() {
            return parameterList;
        }

        public Map<String, String> getParameterMap(Map<String, Object> context, String defaultEntityName, String defaultServiceName) {
            Map<String, String> fullParameterMap = new HashMap<>();
            for (Parameter parameter : this.parameterList) {
                fullParameterMap.put(parameter.getName(), parameter.getValue(context));
            }
            if (autoServiceParameters != null) {
                fullParameterMap.putAll(autoServiceParameters.getParametersMap(context, defaultServiceName));
            }
            if (autoEntityParameters != null) {
                fullParameterMap.putAll(autoEntityParameters.getParametersMap(context, defaultEntityName));
            }
            return fullParameterMap;
        }

        public Map<String, String> getParameterMap(Map<String, Object> context) {
            Map<String, String> fullParameterMap = new HashMap<>();
            for (Parameter parameter : this.parameterList) {
                fullParameterMap.put(parameter.getName(), parameter.getValue(context));
            }
            if (autoServiceParameters != null) {
                fullParameterMap.putAll(autoServiceParameters.getParametersMap(context, null));
            }
            if (autoEntityParameters != null) {
                fullParameterMap.putAll(autoEntityParameters.getParametersMap(context, null));
            }
            return fullParameterMap;
        }

        public String getPrefix(Map<String, Object> context) {
            return this.prefixExdr.expandString(context);
        }

        public FlexibleStringExpander getPrefixExdr() {
            return prefixExdr;
        }

        public boolean getRequestConfirmation() {
            return this.requestConfirmation;
        }

        public boolean getSecure() {
            return this.secure;
        }

        public Integer getSize() {
            return size;
        }

        public String getStyle(Map<String, Object> context) {
            return this.styleExdr.expandString(context);
        }

        public FlexibleStringExpander getStyleExdr() {
            return styleExdr;
        }

        public String getTarget(Map<String, Object> context) {
            Map<String, Object> expanderContext = context;
            UtilCodec.SimpleEncoder simpleEncoder = context == null ? null : (UtilCodec.SimpleEncoder) context
                    .get("simpleEncoder");
            if (simpleEncoder != null) {
                expanderContext = UtilCodec.HtmlEncodingMapWrapper.getHtmlEncodingMapWrapper(context, simpleEncoder);
            }
            return this.targetExdr.expandString(expanderContext);
        }

        public FlexibleStringExpander getTargetExdr() {
            return targetExdr;
        }

        public String getTargetWindow(Map<String, Object> context) {
            return this.targetWindowExdr.expandString(context);
        }

        public FlexibleStringExpander getTargetWindowExdr() {
            return targetWindowExdr;
        }

        public String getText(Map<String, Object> context) {
            String text = this.textExdr.expandString(context);
            // FIXME: Encoding should be done by the renderer, not by the model.
            UtilCodec.SimpleEncoder simpleEncoder = (UtilCodec.SimpleEncoder) context.get("simpleEncoder");
            if (simpleEncoder != null) {
                text = simpleEncoder.encode(text);
            }
            return text;
        }

        public FlexibleStringExpander getTextExdr() {
            return textExdr;
        }

        public String getUrlMode() {
            return this.urlMode;
        }

        public String getWidth() {
            return this.width;
        }
    }

    /**
     * Models the &lt;parameter&gt; element.
     *
     * @see <code>widget-form.xsd</code>
     */
    public static class Parameter {
        protected FlexibleMapAccessor<Object> fromField;
        protected String name;
        protected FlexibleStringExpander value;

        public Parameter(Element element) {
            this.name = element.getAttribute("param-name");
            this.value = UtilValidate.isNotEmpty(element.getAttribute("value")) ? FlexibleStringExpander.getInstance(element
                    .getAttribute("value")) : null;
            this.fromField = UtilValidate.isNotEmpty(element.getAttribute("from-field")) ? FlexibleMapAccessor
                    .getInstance(element.getAttribute("from-field")) : null;
        }

        public Parameter(String paramName, String paramValue, boolean isField) {
            this.name = paramName;
            if (isField) {
                this.fromField = FlexibleMapAccessor.getInstance(paramValue);
            } else {
                this.value = FlexibleStringExpander.getInstance(paramValue);
            }
        }

        public FlexibleMapAccessor<Object> getFromField() {
            return fromField;
        }

        public String getName() {
            return name;
        }

        public FlexibleStringExpander getValue() {
            return value;
        }

        public String getValue(Map<String, Object> context) {
            if (this.value != null) {
                return this.value.expandString(context);
            }
            Object retVal = null;
            if (this.fromField != null && this.fromField.get(context) != null) {
                retVal = this.fromField.get(context);
            } else {
                retVal = context.get(this.name);
            }
            if (retVal != null) {
                TimeZone timeZone = (TimeZone) context.get("timeZone");
                if (timeZone == null) {
                    timeZone = TimeZone.getDefault();
                }
                String returnValue = null;
                // format string based on the user's time zone (not locale because these are parameters)
                if (retVal instanceof Double || retVal instanceof Float || retVal instanceof BigDecimal) {
                    returnValue = retVal.toString();
                } else if (retVal instanceof java.sql.Date) {
                    DateFormat df = UtilDateTime.toDateFormat(UtilDateTime.getDateFormat(), timeZone, null);
                    returnValue = df.format((java.util.Date) retVal);
                } else if (retVal instanceof java.sql.Time) {
                    DateFormat df = UtilDateTime.toTimeFormat(UtilDateTime.getTimeFormat(), timeZone, null);
                    returnValue = df.format((java.util.Date) retVal);
                } else if (retVal instanceof java.sql.Timestamp) {
                    DateFormat df = UtilDateTime.toDateTimeFormat(UtilDateTime.getDateTimeFormat(), timeZone, null);
                    returnValue = df.format((java.util.Date) retVal);
                } else if (retVal instanceof java.util.Date) {
                    DateFormat df = UtilDateTime.toDateTimeFormat("EEE MMM dd hh:mm:ss z yyyy", timeZone, null);
                    returnValue = df.format((java.util.Date) retVal);
                } else {
                    returnValue = retVal.toString();
                }
                return returnValue;
            }
            return null;
        }
    }
}
