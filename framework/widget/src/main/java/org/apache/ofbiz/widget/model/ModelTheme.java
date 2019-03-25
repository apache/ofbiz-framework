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
 * dedicate language governing permissions and limitations
 * under the License.
 *******************************************************************************/
package org.apache.ofbiz.widget.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.SerializationUtils;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.ObjectType;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.UtilXml;
import org.apache.ofbiz.base.util.collections.FlexibleMapAccessor;
import org.apache.ofbiz.widget.renderer.VisualTheme;
import org.w3c.dom.Element;

/**
 * Widget Theme Library - Widget model class.
 */
@SuppressWarnings("serial")
public class ModelTheme implements Serializable {

    public static final String module = ModelTheme.class.getName();
    //generic properties
    private final String name;
    private final Map<String, VisualTheme> visualThemes;
    private final Integer defaultViewSize;

    // Autocomplete configuration
    // Default number of items to be displayed in lookup ajax autocompleter
    private final Integer autocompleterDefaultViewSize;
    // Default minimum number of characters an user has to type before the ajax autocompleter activates (jQuery default is 1)
    private final Integer autocompleterDefaultMinLength;
    // Default delay in milliseconds the Autocomplete waits after a keystroke to activate itself. A zero-delay makes sense for local data (more responsive), but can produce a lot of load for remote data, while being less responsive.
    private final Integer autocompleterDefaultDelay;
    // Show/hide the ID field that is returned from ajax autocompleter
    private final Boolean autocompleterDisplayReturnField;

    //layer modal
    // Default position and size for lookup layered windows
    private final String lookupPosition;
    private final Integer lookupWidth;
    private final Integer lookupHeight;
    private final String lookupShowDescription;
    //Default size for layered modal windows
    private final Integer linkDefaultLayeredModalWidth;
    private final Integer linkDefaultLayeredModalHeight;

    //dedicate theme properties
    private final Map<String, Object> themePropertiesMap;

    //template rendering
    private final Map<String, ModelTemplate> modelTemplateMap;
    private final Map<String, String> modelCommonScreensMap;

    /**
     * Only constructor to initialize a modelTheme from xml definition
     * @param themeElement
     */
    public ModelTheme(Element themeElement) {
        this.name = themeElement.getAttribute("name");
        Map<String, VisualTheme> initVisualThemes = new HashMap<>();
        Map<String, Object> initWidgetPropertiesMap = new HashMap<>();
        Map<String, Object> initThemePropertiesMap = new HashMap<>();
        Map<String, ModelTemplate> initModelTemplateMap = new HashMap<>();
        Map<String, String> initModelCommonScreensMap = new HashMap<>();

        // first resolve value from the origin theme
        Element originThemeElement = UtilXml.firstChildElement(themeElement, "extends");
        ModelTheme originTheme = null;
        if (originThemeElement != null) {
            originTheme = ThemeFactory.getModelThemeFromLocation(originThemeElement.getAttribute("location"));
        }

        if (originTheme != null) {
            initWidgetPropertiesMap.put("defaultViewSize", originTheme.defaultViewSize);
            initWidgetPropertiesMap.put("autocompleterDefaultViewSize", originTheme.autocompleterDefaultViewSize);
            initWidgetPropertiesMap.put("autocompleterDefaultMinLength", originTheme.autocompleterDefaultMinLength);
            initWidgetPropertiesMap.put("autocompleterDefaultDelay", originTheme.autocompleterDefaultDelay);
            initWidgetPropertiesMap.put("autocompleterDisplayReturnField", originTheme.autocompleterDisplayReturnField);
            initWidgetPropertiesMap.put("lookupPosition", originTheme.lookupPosition);
            initWidgetPropertiesMap.put("lookupWidth", originTheme.lookupWidth);
            initWidgetPropertiesMap.put("lookupHeight", originTheme.lookupHeight);
            initWidgetPropertiesMap.put("lookupShowDescription", originTheme.lookupShowDescription);
            initWidgetPropertiesMap.put("linkDefaultLayeredModalWidth", originTheme.linkDefaultLayeredModalWidth);
            initWidgetPropertiesMap.put("linkDefaultLayeredModalHeight", originTheme.linkDefaultLayeredModalHeight);

            // resolve all decicate properties from origin and sucharge by the present dedicate properties
            if (originTheme.themePropertiesMap != null) {
                for (String key : originTheme.themePropertiesMap.keySet()) {
                    initThemePropertiesMap.put(key, SerializationUtils.clone((Serializable)originTheme.themePropertiesMap.get(key)));
                }
            }

            // Add modelTemplate present on origin and not on this
            if (originTheme.modelTemplateMap != null) {
                initModelTemplateMap = UtilMisc.makeMapWritable(originTheme.modelTemplateMap);
            }
            if (originTheme.modelCommonScreensMap != null) {
                initModelCommonScreensMap = UtilMisc.makeMapWritable(originTheme.modelCommonScreensMap);
            }
        }

        //second collect value from XML and surcharge
        for (Element childElement : UtilXml.childElementList(themeElement)) {
            switch (childElement.getNodeName()) {
                case "widget-properties":
                    addWidgetProperties(initWidgetPropertiesMap, childElement);
                    break;
                case "visual-themes":
                    for (Element visualTheme : UtilXml.childElementList(childElement)) {
                        initVisualThemes.put(visualTheme.getAttribute("id"), new VisualTheme(this, visualTheme));
                    }
                case "theme-properties":
                    for (Element property : UtilXml.childElementList(childElement)) {
                        addThemeProperty(initThemePropertiesMap, property);
                    }
                    break;
                case "templates":
                    for (Element template : UtilXml.childElementList(childElement)) {
                        String modelTemplateName = template.getAttribute("name");
                        if (initModelTemplateMap.containsKey(modelTemplateName)) {
                            ModelTemplate surchargeModelTemplate = new ModelTemplate(template);
                            ModelTemplate originModelTemplate = initModelTemplateMap.get(modelTemplateName);
                            initModelTemplateMap.put(modelTemplateName, new ModelTemplate(surchargeModelTemplate, originModelTemplate));
                        } else {
                            initModelTemplateMap.put(modelTemplateName, new ModelTemplate(template));
                        }
                    }
                    break;
                case "common-screens":
                    for (Element screenPurpose : UtilXml.childElementList(childElement)) {
                        String defaultLocation = screenPurpose.getAttribute("default-location");
                        for (Element screen : UtilXml.childElementList(screenPurpose)) {
                            String name = screen.getAttribute("name");
                            String location = screen.getAttribute("location");
                            if (UtilValidate.isEmpty(location)) {
                                location = defaultLocation;
                            }
                            if (UtilValidate.isEmpty(location)) {
                                Debug.logWarning("We can resolve the screen location " + name + " in the theme " + this.name + " so no added it", module);
                                continue;
                            }
                            initModelCommonScreensMap.put(name, location);
                        }
                    }
                    break;
            }
        }

        // now store all values on final variable
        this.defaultViewSize = (Integer) initWidgetPropertiesMap.get("defaultViewSize");
        this.autocompleterDefaultViewSize = (Integer) initWidgetPropertiesMap.get("autocompleterDefaultViewSize");
        this.autocompleterDefaultMinLength = (Integer) initWidgetPropertiesMap.get("autocompleterDefaultMinLength");
        this.autocompleterDefaultDelay = (Integer) initWidgetPropertiesMap.get("autocompleterDefaultDelay");
        this.autocompleterDisplayReturnField = (Boolean) initWidgetPropertiesMap.get("autocompleterDisplayReturnField");
        this.lookupShowDescription = (String) initWidgetPropertiesMap.get("lookupShowDescription");
        this.lookupPosition = (String) initWidgetPropertiesMap.get("lookupPosition");
        this.lookupWidth = (Integer) initWidgetPropertiesMap.get("lookupWidth");
        this.lookupHeight = (Integer) initWidgetPropertiesMap.get("lookupHeight");
        this.linkDefaultLayeredModalWidth = (Integer) initWidgetPropertiesMap.get("linkDefaultLayeredModalWidth");
        this.linkDefaultLayeredModalHeight = (Integer) initWidgetPropertiesMap.get("linkDefaultLayeredModalHeight");
        this.visualThemes = Collections.unmodifiableMap(initVisualThemes);
        this.themePropertiesMap = Collections.unmodifiableMap(initThemePropertiesMap);
        this.modelTemplateMap = Collections.unmodifiableMap(initModelTemplateMap);
        this.modelCommonScreensMap = Collections.unmodifiableMap(initModelCommonScreensMap);
    }

    public String getName() {
        return name;
    }

    public List<String> getVisualThemeIds() {
        return new ArrayList<>(visualThemes.keySet());
    }
    public VisualTheme getVisualTheme(String visualThemeId) { return visualThemes.get(visualThemeId); }

    public Integer getDefaultViewSize() {
        return defaultViewSize;
    }

    public Integer getAutocompleterDefaultViewSize() {
        return autocompleterDefaultViewSize;
    }
    public Integer getAutocompleterDefaultMinLength() {
        return autocompleterDefaultMinLength;
    }
    public Boolean getAutocompleterDisplayReturnField() {
        return autocompleterDisplayReturnField;
    }
    public Integer getAutocompleterDefaultDelay() {
        return autocompleterDefaultDelay;
    }

    public Integer getLinkDefaultLayeredModalHeight() {
        return linkDefaultLayeredModalHeight;
    }
    public Integer getLinkDefaultLayeredModalWidth() {
        return linkDefaultLayeredModalWidth;
    }

    public Integer getLookupHeight() {
        return lookupHeight;
    }

    public Integer getLookupWidth() {
        return lookupWidth;
    }

    public String getLookupPosition() {
        return lookupPosition;
    }
    public String getLookupShowDescription() {
        return lookupShowDescription;
    }

    /**
     * for a map preloaded with the origin values, surcharge them from xml definition
     * @param initWidgetPropertiesMap
     * @param widgetProperties
     */
    private void addWidgetProperties(Map<String, Object> initWidgetPropertiesMap, Element widgetProperties) {
        for (Element childElement : UtilXml.childElementList(widgetProperties)) {
            switch (childElement.getNodeName()) {
                case "default-view-size":
                    initWidgetPropertiesMap.put("defaultViewSize", Integer.valueOf(childElement.getAttribute("value")));
                    break;
                case "autocompleter":
                    initWidgetPropertiesMap.put("autocompleterDefaultDelay", Integer.valueOf(childElement.getAttribute("default-delay")));
                    initWidgetPropertiesMap.put("autocompleterDefaultMinLength", Integer.valueOf(childElement.getAttribute("default-min-lenght")));
                    initWidgetPropertiesMap.put("autocompleterDefaultViewSize", Integer.valueOf(childElement.getAttribute("default-view-size")));
                    initWidgetPropertiesMap.put("autocompleterDisplayReturnField", "true".equalsIgnoreCase(childElement.getAttribute("display-return-field")));
                    break;
                case "lookup":
                    initWidgetPropertiesMap.put("lookupPosition", childElement.getAttribute("position"));
                    initWidgetPropertiesMap.put("lookupHeight", Integer.valueOf(childElement.getAttribute("height")));
                    initWidgetPropertiesMap.put("lookupWidth", Integer.valueOf(childElement.getAttribute("width")));
                    initWidgetPropertiesMap.put("lookupShowDescription", childElement.getAttribute("show-description"));
                    break;
                case "layered-modal":
                    initWidgetPropertiesMap.put("linkDefaultLayeredModalHeight", Integer.valueOf(childElement.getAttribute("height")));
                    initWidgetPropertiesMap.put("linkDefaultLayeredModalWidth", Integer.valueOf(childElement.getAttribute("width")));
                    break;
            }
        }
    }

    /**
     * for a map preloaded with the theme properties values, surcharge them from xml definition
     * @param initThemePropertiesMap
     * @param property
     */
    private void addThemeProperty(Map<String, Object> initThemePropertiesMap, Element property) {
        FlexibleMapAccessor<Object> name = FlexibleMapAccessor.getInstance(property.getAttribute("name"));
        String value = property.getAttribute("value");
        String type = property.getAttribute("type");
        if (UtilValidate.isEmpty(type) || type.endsWith("String")) {
            name.put(initThemePropertiesMap, value);
        } else {
            try {
                name.put(initThemePropertiesMap, ObjectType.simpleTypeConvert(value, type, null, null));
            } catch (GeneralException e) {
                Debug.logError("Impossible to parse the value " + value + " to type " + type + " for the property " + name + " on theme " + this.name, module);
            }
        }
    }
    public Object getProperty(String propertyName) {
        if (! themePropertiesMap.containsKey(propertyName)
                || themePropertiesMap.get(propertyName) == null) {
            return "";
        }
        return themePropertiesMap.get(propertyName);
    }

    /**
     * return the themes properties like VisualThemesRessources, keep the name for understanding compatibility
     * @return
     */
    public Map<String, Object> getThemeResources() {
        return themePropertiesMap;
    }

    public String getType(String name) {
        ModelTemplate modelTemplate = modelTemplateMap.get(name);
        if (modelTemplate != null) {
            return modelTemplate.getType();
        }
        return null;
    }
    public String getEncoder(String name) {
        ModelTemplate modelTemplate = modelTemplateMap.get(name);
        if (modelTemplate != null) {
            return modelTemplate.getEncoder();
        }
        return null;
    }
    public String getCompress(String name) {
        ModelTemplate modelTemplate = modelTemplateMap.get(name);
        if (modelTemplate != null) {
            return modelTemplate.getCompress();
        }
        return null;
    }
    public String getContentType(String name) {
        ModelTemplate modelTemplate = modelTemplateMap.get(name);
        if (modelTemplate != null) {
            return modelTemplate.getContentType();
        }
        return null;
    }
    public String getEncoding(String name) {
        ModelTemplate modelTemplate = modelTemplateMap.get(name);
        if (modelTemplate != null) {
            return modelTemplate.getEncoding();
        }
        return null;
    }

    public String getScreenRendererLocation(String name) {
        ModelTemplate modelTemplate = modelTemplateMap.get(name);
        if (modelTemplate != null) {
            return modelTemplate.getScreenRendererLocation();
        }
        return null;
    }
    public String getFormRendererLocation(String name) {
        ModelTemplate modelTemplate = modelTemplateMap.get(name);
        if (modelTemplate != null) {
            return modelTemplate.getFormRendererLocation();
        }
        return null;
    }
    public String getTreeRendererLocation(String name) {
        ModelTemplate modelTemplate = modelTemplateMap.get(name);
        if (modelTemplate != null) {
            return modelTemplate.getTreeRendererLocation();
        }
        return null;
    }
    public String getMenuRendererLocation(String name) {
        ModelTemplate modelTemplate = modelTemplateMap.get(name);
        if (modelTemplate != null) {
            return modelTemplate.getMenuRendererLocation();
        }
        return null;
    }
    public String getErrorTemplateLocation(String name) {
        ModelTemplate modelTemplate = modelTemplateMap.get(name);
        if (modelTemplate != null) {
            return modelTemplate.getErrorTemplateLocation();
        }
        return null;
    }

    public Map<String,String> getModelCommonScreens() {
        return modelCommonScreensMap;
    }

    /**
     * the ModelTemplate class, manage the complexity of macro library definition and the rendering technology
     */
    private class ModelTemplate implements Serializable {
        private final String name;
        private final String type;
        private final String compress;
        private final String encoder;
        private final String contentType;
        private final String encoding;
        private final String screenRendererLocation;
        private final String formRendererLocation;
        private final String menuRendererLocation;
        private final String treeRendererLocation;
        private final String errorTemplateLocation;

        /**
         * Constructor to initialize a ModelTemplate class from xml definition
         * @param template
         */
        public ModelTemplate(Element template) {
            this.name = template.getAttribute("name");
            this.type = template.getAttribute("type");
            this.compress = template.getAttribute("compress");
            this.encoder = template.getAttribute("encoder");
            this.contentType = template.getAttribute("contentType");
            this.encoding = template.getAttribute("encoding");

            String screenRendererLocation = null;
            String formRendererLocation = null;
            String menuRendererLocation = null;
            String treeRendererLocation = null;
            String errorTemplateLocation = null;
            for (Element templateFile : UtilXml.childElementList(template)) {
                switch (templateFile.getAttribute("widget")) {
                    case "screen":
                        screenRendererLocation = templateFile.getAttribute("location");
                        break;
                    case "form":
                        formRendererLocation = templateFile.getAttribute("location");
                        break;
                    case "tree":
                        treeRendererLocation = templateFile.getAttribute("location");
                        break;
                    case "menu":
                        menuRendererLocation = templateFile.getAttribute("location");
                        break;
                    case "error":
                        errorTemplateLocation = templateFile.getAttribute("location");
                        break;
                }
            }
            this.screenRendererLocation = screenRendererLocation;
            this.formRendererLocation = formRendererLocation;
            this.menuRendererLocation = menuRendererLocation;
            this.treeRendererLocation = treeRendererLocation;
            this.errorTemplateLocation = errorTemplateLocation;
        }

        /**
         * Constructor to create a new ModelTemplate from the fusion on two ModelTemplates
         * @param currentModelTemplate
         * @param originModelTemplate
         */
        public ModelTemplate (ModelTemplate currentModelTemplate, ModelTemplate originModelTemplate) {
            boolean exist = currentModelTemplate != null;
            this.name = exist ? currentModelTemplate.name : originModelTemplate.name;
            this.type = exist ? currentModelTemplate.type : originModelTemplate.type;
            this.compress = exist && currentModelTemplate.compress != null ? currentModelTemplate.compress : originModelTemplate.compress;
            this.encoder = exist && currentModelTemplate.encoder != null ? currentModelTemplate.encoder : originModelTemplate.encoder;
            this.contentType = exist && currentModelTemplate.contentType != null ? currentModelTemplate.contentType : originModelTemplate.contentType;
            this.encoding = exist && currentModelTemplate.encoding != null ? currentModelTemplate.encoding : originModelTemplate.encoding;
            this.screenRendererLocation = exist && currentModelTemplate.screenRendererLocation != null ? currentModelTemplate.screenRendererLocation : originModelTemplate.screenRendererLocation;
            this.formRendererLocation = exist && currentModelTemplate.formRendererLocation != null ? currentModelTemplate.formRendererLocation : originModelTemplate.formRendererLocation;
            this.treeRendererLocation = exist && currentModelTemplate.treeRendererLocation != null ? currentModelTemplate.treeRendererLocation : originModelTemplate.treeRendererLocation;
            this.menuRendererLocation = exist && currentModelTemplate.menuRendererLocation != null ? currentModelTemplate.menuRendererLocation : originModelTemplate.menuRendererLocation;
            this.errorTemplateLocation = exist && currentModelTemplate.errorTemplateLocation != null ? currentModelTemplate.errorTemplateLocation : originModelTemplate.errorTemplateLocation;
        }
        public String getName() {
            return name;
        }
        public String getEncoder() {
            return encoder;
        }
        public String getType() { return type; }
        public String getCompress() {
            return compress;
        }
        public String getContentType() {
            return contentType;
        }
        public String getEncoding() {
            return encoding;
        }

        public String getScreenRendererLocation() {
            return screenRendererLocation;
        }
        public String getFormRendererLocation() {
            return formRendererLocation;
        }
        public String getTreeRendererLocation() {
            return treeRendererLocation;
        }
        public String getMenuRendererLocation() {
            return menuRendererLocation;
        }
        public String getErrorTemplateLocation() {
            return errorTemplateLocation;
        }
    }
}
