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
package org.ofbiz.widget.menu;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilCodec;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.UtilXml;
import org.ofbiz.base.util.collections.FlexibleMapAccessor;
import org.ofbiz.base.util.string.FlexibleStringExpander;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.widget.ModelWidget;
import org.ofbiz.widget.ModelWidgetAction;
import org.ofbiz.widget.ModelWidgetVisitor;
import org.ofbiz.widget.PortalPageWorker;
import org.ofbiz.widget.WidgetWorker.AutoEntityParameters;
import org.ofbiz.widget.WidgetWorker.AutoServiceParameters;
import org.ofbiz.widget.WidgetWorker.Parameter;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 * Models the &lt;menu-item&gt; element.
 * 
 * @see <code>widget-menu.xsd</code>
 */
@SuppressWarnings("serial")
public class ModelMenuItem extends ModelWidget {

    /*
     * ----------------------------------------------------------------------- *
     *                     DEVELOPERS PLEASE READ
     * ----------------------------------------------------------------------- *
     * 
     * This model is intended to be a read-only data structure that represents
     * an XML element. Outside of object construction, the class should not
     * have any behaviors.
     * 
     * Instances of this class will be shared by multiple threads - therefore
     * it is immutable. DO NOT CHANGE THE OBJECT'S STATE AT RUN TIME!
     * 
     */

    public static final String module = ModelMenuItem.class.getName();

    private final List<ModelWidgetAction> actions;
    private final String align;
    private final String alignStyle;
    private final FlexibleStringExpander associatedContentId;
    private final String cellWidth;
    private final ModelMenuCondition condition;
    private final String disabledTitleStyle;
    private final String disableIfEmpty;
    private final String entityName;
    private final Boolean hideIfSelected;
    private final Link link;
    private final List<ModelMenuItem> menuItemList;
    private final ModelMenu modelMenu;
    private final String overrideName;
    private final ModelMenuItem parentMenuItem;
    private final FlexibleStringExpander parentPortalPageId;
    private final Integer position;
    private final String selectedStyle;
    private final ModelMenu subMenu;
    private final FlexibleStringExpander title;
    private final String titleStyle;
    private final FlexibleStringExpander tooltip;
    private final String tooltipStyle;
    private final String widgetStyle;

    // ===== CONSTRUCTORS =====

    public ModelMenuItem(Element menuItemElement, ModelMenu modelMenu) {
        this(menuItemElement, modelMenu, null);
    }

    private ModelMenuItem(Element menuItemElement, ModelMenu modelMenu, ModelMenuItem parentMenuItem) {
        super(menuItemElement);
        this.modelMenu = modelMenu;
        this.parentMenuItem = parentMenuItem;
        this.entityName = menuItemElement.getAttribute("entity-name");
        this.title = FlexibleStringExpander.getInstance(menuItemElement.getAttribute("title"));
        this.tooltip = FlexibleStringExpander.getInstance(menuItemElement.getAttribute("tooltip"));
        this.parentPortalPageId = FlexibleStringExpander.getInstance(menuItemElement.getAttribute("parent-portal-page-value"));
        this.titleStyle = menuItemElement.getAttribute("title-style");
        this.disabledTitleStyle = menuItemElement.getAttribute("disabled-title-style");
        this.widgetStyle = menuItemElement.getAttribute("widget-style");
        this.tooltipStyle = menuItemElement.getAttribute("tooltip-style");
        this.selectedStyle = menuItemElement.getAttribute("selected-style");
        String hideIfSelected = menuItemElement.getAttribute("hide-if-selected");
        if (!hideIfSelected.isEmpty())
            if (hideIfSelected.equalsIgnoreCase("true"))
                this.hideIfSelected = Boolean.TRUE;
            else
                this.hideIfSelected = Boolean.FALSE;
        else
            this.hideIfSelected = null;
        this.disableIfEmpty = menuItemElement.getAttribute("disable-if-empty");
        this.align = menuItemElement.getAttribute("align");
        this.alignStyle = menuItemElement.getAttribute("align-style");
        Integer position = null;
        String positionStr = menuItemElement.getAttribute("position");
        if (!positionStr.isEmpty()) {
            try {
                position = Integer.valueOf(positionStr);
            } catch (Exception e) {
                Debug.logError(e, "Could not convert position attribute of the field element to an integer: [" + positionStr
                        + "], using the default of the menu renderer", module);
                position = null;
            }
        }
        this.position = position;
        this.associatedContentId = FlexibleStringExpander.getInstance(menuItemElement.getAttribute("associated-content-id"));
        this.cellWidth = menuItemElement.getAttribute("cell-width");
        Element subMenuElement = UtilXml.firstChildElement(menuItemElement, "sub-menu");
        if (subMenuElement != null) {
            String subMenuLocation = subMenuElement.getAttribute("location");
            String subMenuName = subMenuElement.getAttribute("name");
            try {
                this.subMenu = MenuFactory.getMenuFromLocation(subMenuLocation, subMenuName);
            } catch (IOException e) {
                String errMsg = "Error getting subMenu in menu named [" + this.modelMenu.getName() + "]: " + e.toString();
                Debug.logError(e, errMsg, module);
                throw new RuntimeException(errMsg);
            } catch (SAXException e2) {
                String errMsg = "Error getting subMenu in menu named [" + this.modelMenu.getName() + "]: " + e2.toString();
                Debug.logError(e2, errMsg, module);
                throw new RuntimeException(errMsg);
            } catch (ParserConfigurationException e3) {
                String errMsg = "Error getting subMenu in menu named [" + this.modelMenu.getName() + "]: " + e3.toString();
                Debug.logError(e3, errMsg, module);
                throw new RuntimeException(errMsg);
            }
        } else {
            this.subMenu = null;
        }
        Element linkElement = UtilXml.firstChildElement(menuItemElement, "link");
        if (linkElement != null) {
            this.link = new Link(linkElement, this);
        } else {
            this.link = null;
        }
        // read in add item defs, add/override one by one using the menuItemList and menuItemMap
        List<? extends Element> itemElements = UtilXml.childElementList(menuItemElement, "menu-item");
        if (!itemElements.isEmpty()) {
            ArrayList<ModelMenuItem> menuItemList = new ArrayList<ModelMenuItem>();
            Map<String, ModelMenuItem> menuItemMap = new HashMap<String, ModelMenuItem>();
            for (Element itemElement : itemElements) {
                ModelMenuItem modelMenuItem = new ModelMenuItem(itemElement, modelMenu, this);
                addUpdateMenuItem(modelMenuItem, menuItemList, menuItemMap);
            }
            menuItemList.trimToSize();
            this.menuItemList = Collections.unmodifiableList(menuItemList);
        } else {
            this.menuItemList = Collections.emptyList();
        }
        // read condition under the "condition" element
        Element conditionElement = UtilXml.firstChildElement(menuItemElement, "condition");
        if (conditionElement != null) {
            this.condition = new ModelMenuCondition(this, conditionElement);
        } else {
            this.condition = null;
        }
        // read all actions under the "actions" element
        Element actionsElement = UtilXml.firstChildElement(conditionElement, "actions");
        if (actionsElement != null) {
            this.actions = ModelWidgetAction.readSubActions(this, actionsElement);
        } else {
            this.actions = Collections.emptyList();
        }
        this.overrideName = "";
    }

    // Portal constructor
    private ModelMenuItem(GenericValue portalPage, ModelMenuItem parentMenuItem, Locale locale) {
        super(portalPage.getString("portalPageId"));
        this.actions = Collections.emptyList();
        this.align = "";
        this.alignStyle = "";
        this.associatedContentId = FlexibleStringExpander.getInstance("");
        this.cellWidth = "";
        this.condition = null;
        this.disabledTitleStyle = "";
        this.disableIfEmpty = "";
        this.entityName = "";
        this.hideIfSelected = null;
        this.menuItemList = Collections.emptyList();
        this.overrideName = "";
        this.parentMenuItem = null;
        this.parentPortalPageId = FlexibleStringExpander.getInstance(portalPage.getString("parentPortalPageId"));
        this.position = null;
        this.selectedStyle = "";
        this.subMenu = null;
        this.title = FlexibleStringExpander.getInstance((String) portalPage.get("portalPageName", locale));
        this.titleStyle = "";
        this.tooltip = FlexibleStringExpander.getInstance("");
        this.tooltipStyle = "";
        this.widgetStyle = "";
        this.link = new Link(portalPage, parentMenuItem, locale);
        this.modelMenu = parentMenuItem.modelMenu;
    }

    // Merge constructor
    private ModelMenuItem(ModelMenuItem existingMenuItem, ModelMenuItem overrideMenuItem) {
        super(existingMenuItem.getName());
        this.modelMenu = existingMenuItem.modelMenu;
        if (UtilValidate.isNotEmpty(overrideMenuItem.getName())) {
            this.overrideName = overrideMenuItem.getName();
        } else {
            this.overrideName = existingMenuItem.getName();
        }
        if (UtilValidate.isNotEmpty(overrideMenuItem.entityName)) {
            this.entityName = overrideMenuItem.entityName;
        } else {
            this.entityName = existingMenuItem.entityName;
        }
        if (UtilValidate.isNotEmpty(overrideMenuItem.parentPortalPageId)) {
            this.parentPortalPageId = overrideMenuItem.parentPortalPageId;
        } else {
            this.parentPortalPageId = existingMenuItem.parentPortalPageId;
        }
        if (UtilValidate.isNotEmpty(overrideMenuItem.title)) {
            this.title = overrideMenuItem.title;
        } else {
            this.title = existingMenuItem.title;
        }
        if (UtilValidate.isNotEmpty(overrideMenuItem.tooltip)) {
            this.tooltip = overrideMenuItem.tooltip;
        } else {
            this.tooltip = existingMenuItem.tooltip;
        }
        if (UtilValidate.isNotEmpty(overrideMenuItem.titleStyle)) {
            this.titleStyle = overrideMenuItem.titleStyle;
        } else {
            this.titleStyle = existingMenuItem.titleStyle;
        }
        if (UtilValidate.isNotEmpty(overrideMenuItem.selectedStyle)) {
            this.selectedStyle = overrideMenuItem.selectedStyle;
        } else {
            this.selectedStyle = existingMenuItem.selectedStyle;
        }
        if (UtilValidate.isNotEmpty(overrideMenuItem.widgetStyle)) {
            this.widgetStyle = overrideMenuItem.widgetStyle;
        } else {
            this.widgetStyle = existingMenuItem.widgetStyle;
        }
        if (overrideMenuItem.position != null) {
            this.position = overrideMenuItem.position;
        } else {
            this.position = existingMenuItem.position;
        }
        this.actions = existingMenuItem.actions;
        this.align = existingMenuItem.align;
        this.alignStyle = existingMenuItem.alignStyle;
        this.associatedContentId = existingMenuItem.associatedContentId;
        this.cellWidth = existingMenuItem.cellWidth;
        this.condition = existingMenuItem.condition;
        this.disabledTitleStyle = existingMenuItem.disabledTitleStyle;
        this.disableIfEmpty = existingMenuItem.disableIfEmpty;
        this.hideIfSelected = existingMenuItem.hideIfSelected;
        this.menuItemList = existingMenuItem.menuItemList;
        this.parentMenuItem = existingMenuItem.parentMenuItem;
        this.subMenu = existingMenuItem.subMenu;
        this.tooltipStyle = existingMenuItem.tooltipStyle;
        this.link = existingMenuItem.link;
    }

    @Override
    public void accept(ModelWidgetVisitor visitor) throws Exception {
        visitor.visit(this);
    }

    private void addUpdateMenuItem(ModelMenuItem modelMenuItem, List<ModelMenuItem> menuItemList,
            Map<String, ModelMenuItem> menuItemMap) {
        ModelMenuItem existingMenuItem = menuItemMap.get(modelMenuItem.getName());
        if (existingMenuItem != null) {
            // does exist, update the item by doing a merge/override
            ModelMenuItem mergedMenuItem = existingMenuItem.mergeOverrideModelMenuItem(modelMenuItem);
            int existingItemIndex = menuItemList.indexOf(existingMenuItem);
            menuItemList.set(existingItemIndex, mergedMenuItem);
            menuItemMap.put(modelMenuItem.getName(), mergedMenuItem);
        } else {
            // does not exist, add to List and Map
            menuItemList.add(modelMenuItem);
            menuItemMap.put(modelMenuItem.getName(), modelMenuItem);
        }
    }

    public List<ModelWidgetAction> getActions() {
        return actions;
    }

    public String getAlign() {
        if (!this.align.isEmpty()) {
            return this.align;
        } else if (parentMenuItem != null) {
            return parentMenuItem.getAlign();
        } else {
            return this.modelMenu.getDefaultAlign();
        }
    }

    public String getAlignStyle() {
        if (!this.alignStyle.isEmpty()) {
            return this.alignStyle;
        } else if (parentMenuItem != null) {
            return parentMenuItem.getAlignStyle();
        } else {
            return this.modelMenu.getDefaultAlignStyle();
        }
    }

    public FlexibleStringExpander getAssociatedContentId() {
        return associatedContentId;
    }

    public String getAssociatedContentId(Map<String, Object> context) {
        String retStr = null;
        if (this.associatedContentId != null) {
            retStr = associatedContentId.expandString(context);
        }
        if (retStr.isEmpty()) {
            retStr = this.modelMenu.getDefaultAssociatedContentId(context);
        }
        return retStr;
    }

    public String getCellWidth() {
        if (!this.cellWidth.isEmpty()) {
            return this.cellWidth;
        } else {
            return this.modelMenu.getDefaultCellWidth();
        }
    }

    public ModelMenuCondition getCondition() {
        return condition;
    }

    public String getDisabledTitleStyle() {
        if (!this.disabledTitleStyle.isEmpty()) {
            return this.disabledTitleStyle;
        } else if (parentMenuItem != null) {
            return parentMenuItem.getDisabledTitleStyle();
        } else {
            return this.modelMenu.getDefaultDisabledTitleStyle();
        }
    }

    public String getDisableIfEmpty() {
        return this.disableIfEmpty;
    }

    public String getEntityName() {
        if (!this.entityName.isEmpty()) {
            return this.entityName;
        } else if (parentMenuItem != null) {
            return parentMenuItem.getEntityName();
        } else {
            return this.modelMenu.getDefaultEntityName();
        }
    }

    public Boolean getHideIfSelected() {
        if (hideIfSelected != null) {
            return this.hideIfSelected;
        } else {
            return this.modelMenu.getDefaultHideIfSelected();
        }
    }

    public Link getLink() {
        return this.link;
    }

    public List<ModelMenuItem> getMenuItemList() {
        return menuItemList;
    }

    public ModelMenu getModelMenu() {
        return modelMenu;
    }

    @Override
    public String getName() {
        if (!this.overrideName.isEmpty()) {
            return this.overrideName;
        }
        return super.getName();
    }

    public String getOverrideName() {
        return overrideName;
    }

    public ModelMenuItem getParentMenuItem() {
        return parentMenuItem;
    }

    public FlexibleStringExpander getParentPortalPageId() {
        return parentPortalPageId;
    }

    public String getParentPortalPageId(Map<String, Object> context) {
        return this.parentPortalPageId.expandString(context);
    }

    public int getPosition() {
        if (this.position == null) {
            return 1;
        } else {
            return position.intValue();
        }
    }

    public String getSelectedStyle() {
        if (!this.selectedStyle.isEmpty()) {
            return this.selectedStyle;
        } else if (parentMenuItem != null) {
            return parentMenuItem.getSelectedStyle();
        } else {
            return this.modelMenu.getDefaultSelectedStyle();
        }
    }

    public ModelMenu getSubMenu() {
        return subMenu;
    }

    public FlexibleStringExpander getTitle() {
        return title;
    }

    public String getTitle(Map<String, Object> context) {
        return title.expandString(context);
    }

    public String getTitleStyle() {
        if (!this.titleStyle.isEmpty()) {
            return this.titleStyle;
        } else if (parentMenuItem != null) {
            return parentMenuItem.getTitleStyle();
        } else {
            return this.modelMenu.getDefaultTitleStyle();
        }
    }

    public FlexibleStringExpander getTooltip() {
        return tooltip;
    }

    public String getTooltip(Map<String, Object> context) {
        if (UtilValidate.isNotEmpty(tooltip)) {
            return tooltip.expandString(context);
        } else {
            return "";
        }
    }

    public String getTooltipStyle() {
        if (!this.tooltipStyle.isEmpty()) {
            return this.tooltipStyle;
        } else if (parentMenuItem != null) {
            return parentMenuItem.getTooltipStyle();
        } else {
            return this.modelMenu.getDefaultTooltipStyle();
        }
    }

    public String getWidgetStyle() {
        if (!this.widgetStyle.isEmpty()) {
            return this.widgetStyle;
        } else if (parentMenuItem != null) {
            return parentMenuItem.getWidgetStyle();
        } else {
            return this.modelMenu.getDefaultWidgetStyle();
        }
    }

    public boolean isSelected(Map<String, Object> context) {
        return getName().equals(modelMenu.getSelectedMenuItemContextFieldName(context));
    }

    public ModelMenuItem mergeOverrideModelMenuItem(ModelMenuItem overrideMenuItem) {
        return new ModelMenuItem(this, overrideMenuItem);
    }

    public void renderMenuItemString(Appendable writer, Map<String, Object> context, MenuStringRenderer menuStringRenderer)
            throws IOException {
        if (shouldBeRendered(context)) {
            ModelWidgetAction.runSubActions(actions, context);
            String parentPortalPageId = getParentPortalPageId(context);
            if (UtilValidate.isNotEmpty(parentPortalPageId)) {
                List<GenericValue> portalPages = PortalPageWorker.getPortalPages(parentPortalPageId, context);
                if (UtilValidate.isNotEmpty(portalPages)) {
                    Locale locale = (Locale) context.get("locale");
                    for (GenericValue portalPage : portalPages) {
                        if (UtilValidate.isNotEmpty(portalPage.getString("portalPageName"))) {
                            ModelMenuItem localItem = new ModelMenuItem(portalPage, this, locale);
                            menuStringRenderer.renderMenuItem(writer, context, localItem);
                        }
                    }
                }
            } else {
                menuStringRenderer.renderMenuItem(writer, context, this);
            }
        }
    }

    public boolean shouldBeRendered(Map<String, Object> context) {
        if (this.condition != null) {
            return this.condition.eval(context);
        }
        return true;
    }

    public static class Image {

        private final FlexibleStringExpander borderExdr;
        private final FlexibleStringExpander heightExdr;
        private final FlexibleStringExpander idExdr;
        private final FlexibleStringExpander srcExdr;
        private final FlexibleStringExpander styleExdr;
        private final String urlMode;
        private final FlexibleStringExpander widthExdr;

        public Image(Element imageElement) {
            this.borderExdr = FlexibleStringExpander.getInstance(UtilXml.checkEmpty(imageElement.getAttribute("border"), "0"));
            this.heightExdr = FlexibleStringExpander.getInstance(imageElement.getAttribute("height"));
            this.idExdr = FlexibleStringExpander.getInstance(imageElement.getAttribute("id"));
            this.srcExdr = FlexibleStringExpander.getInstance(imageElement.getAttribute("src"));
            this.styleExdr = FlexibleStringExpander.getInstance(imageElement.getAttribute("style"));
            this.urlMode = UtilXml.checkEmpty(imageElement.getAttribute("url-mode"), "content");
            this.widthExdr = FlexibleStringExpander.getInstance(imageElement.getAttribute("width"));
        }

        public String getBorder(Map<String, Object> context) {
            return this.borderExdr.expandString(context);
        }

        public String getHeight(Map<String, Object> context) {
            return this.heightExdr.expandString(context);
        }

        public String getId(Map<String, Object> context) {
            return this.idExdr.expandString(context);
        }

        public String getSrc(Map<String, Object> context) {
            return this.srcExdr.expandString(context);
        }

        public String getStyle(Map<String, Object> context) {
            return this.styleExdr.expandString(context);
        }

        public String getUrlMode() {
            return this.urlMode;
        }

        public String getWidth(Map<String, Object> context) {
            return this.widthExdr.expandString(context);
        }

        public void renderImageString(Appendable writer, Map<String, Object> context, MenuStringRenderer menuStringRenderer)
                throws IOException {
            menuStringRenderer.renderImage(writer, context, this);
        }
    }

    public static class Link {
        private final AutoEntityParameters autoEntityParameters;
        private final AutoServiceParameters autoServiceParameters;
        private final FlexibleStringExpander confirmationMsgExdr;
        private final boolean encode;
        private final boolean fullPath;
        private final FlexibleStringExpander idExdr;
        private final Image image;
        private final ModelMenuItem linkMenuItem;
        private final String linkType;
        private final FlexibleStringExpander nameExdr;
        private final List<Parameter> parameterList;
        private final FlexibleMapAccessor<Map<String, String>> parametersMapAcsr;
        private final FlexibleStringExpander prefixExdr;
        private final boolean requestConfirmation;
        private final boolean secure;
        private final FlexibleStringExpander styleExdr;
        private final FlexibleStringExpander targetExdr;
        private final FlexibleStringExpander targetWindowExdr;
        private final FlexibleStringExpander textExdr;
        private final String urlMode;

        public Link(Element linkElement, ModelMenuItem parentMenuItem) {
            Element autoEntityParamsElement = UtilXml.firstChildElement(linkElement, "auto-parameters-entity");
            if (autoEntityParamsElement != null) {
                this.autoEntityParameters = new AutoEntityParameters(autoEntityParamsElement);
            } else {
                this.autoEntityParameters = null;
            }
            Element autoServiceParamsElement = UtilXml.firstChildElement(linkElement, "auto-parameters-service");
            if (autoServiceParamsElement != null) {
                this.autoServiceParameters = new AutoServiceParameters(autoServiceParamsElement);
            } else {
                this.autoServiceParameters = null;
            }
            this.confirmationMsgExdr = FlexibleStringExpander.getInstance(linkElement.getAttribute("confirmation-message"));
            this.encode = "true".equals(linkElement.getAttribute("encode"));
            this.fullPath = "true".equals(linkElement.getAttribute("full-path"));
            this.idExdr = FlexibleStringExpander.getInstance(linkElement.getAttribute("id"));
            Element imageElement = UtilXml.firstChildElement(linkElement, "image");
            if (imageElement != null) {
                this.image = new Image(imageElement);
            } else {
                this.image = null;
            }
            this.linkMenuItem = parentMenuItem;
            this.linkType = linkElement.getAttribute("link-type");
            this.nameExdr = FlexibleStringExpander.getInstance(linkElement.getAttribute("name"));
            List<? extends Element> parameterElementList = UtilXml.childElementList(linkElement, "parameter");
            if (!parameterElementList.isEmpty()) {
                List<Parameter> parameterList = new ArrayList<Parameter>(parameterElementList.size());
                for (Element parameterElement : parameterElementList) {
                    parameterList.add(new Parameter(parameterElement));
                }
                this.parameterList = Collections.unmodifiableList(parameterList);
            } else {
                this.parameterList = Collections.emptyList();
            }
            this.parametersMapAcsr = FlexibleMapAccessor.getInstance(linkElement.getAttribute("parameters-map"));
            this.prefixExdr = FlexibleStringExpander.getInstance(linkElement.getAttribute("prefix"));
            this.requestConfirmation = "true".equals(linkElement.getAttribute("request-confirmation"));
            this.secure = "true".equals(linkElement.getAttribute("secure"));
            this.styleExdr = FlexibleStringExpander.getInstance(linkElement.getAttribute("style"));
            this.targetExdr = FlexibleStringExpander.getInstance(linkElement.getAttribute("target"));
            this.targetWindowExdr = FlexibleStringExpander.getInstance(linkElement.getAttribute("target-window"));
            this.textExdr = FlexibleStringExpander.getInstance(linkElement.getAttribute("text"));
            this.urlMode = UtilXml.checkEmpty(linkElement.getAttribute("url-mode"), "intra-app");
        }

        public Link(GenericValue portalPage, ModelMenuItem parentMenuItem, Locale locale) {
            this.autoEntityParameters = null;
            this.autoServiceParameters = null;
            this.confirmationMsgExdr = FlexibleStringExpander.getInstance("");
            this.encode = false;
            this.fullPath = false;
            this.idExdr = FlexibleStringExpander.getInstance("");
            this.image = null;
            this.linkMenuItem = parentMenuItem;
            this.linkType = "";
            this.nameExdr = FlexibleStringExpander.getInstance("");
            ArrayList<Parameter> parameterList = new ArrayList<Parameter>();
            if (parentMenuItem.link != null) {
                parameterList.addAll(parentMenuItem.link.parameterList);
            }
            parameterList.add(new Parameter("portalPageId", portalPage.getString("portalPageId"), false));
            parameterList.add(new Parameter("parentPortalPageId", portalPage.getString("parentPortalPageId"), false));
            parameterList.trimToSize();
            this.parameterList = Collections.unmodifiableList(parameterList);
            this.parametersMapAcsr = FlexibleMapAccessor.getInstance("");
            this.prefixExdr = FlexibleStringExpander.getInstance("");
            this.requestConfirmation = false;
            this.secure = false;
            this.styleExdr = FlexibleStringExpander.getInstance("");
            if (parentMenuItem.link != null) {
                this.targetExdr = FlexibleStringExpander.getInstance("");
            } else {
                this.targetExdr = FlexibleStringExpander.getInstance("showPortalPage");
            }
            this.targetWindowExdr = FlexibleStringExpander.getInstance("");
            this.textExdr = FlexibleStringExpander.getInstance((String) portalPage.get("portalPageName", locale));
            this.urlMode = "intra-app";
        }

        public String getConfirmation(Map<String, Object> context) {
            String message = getConfirmationMsg(context);
            if (UtilValidate.isNotEmpty(message)) {
                return message;
            } else if (getRequestConfirmation()) {
                FlexibleStringExpander defaultMessage = FlexibleStringExpander.getInstance(UtilProperties.getPropertyValue(
                        "general", "default.confirmation.message", "${uiLabelMap.CommonConfirm}"));
                return defaultMessage.expandString(context);
            }
            return "";
        }

        public String getConfirmationMsg(Map<String, Object> context) {
            return this.confirmationMsgExdr.expandString(context);
        }

        public boolean getEncode() {
            return this.encode;
        }

        public boolean getFullPath() {
            return this.fullPath;
        }

        public String getId(Map<String, Object> context) {
            return this.idExdr.expandString(context);
        }

        public Image getImage() {
            return this.image;
        }

        public ModelMenuItem getLinkMenuItem() {
            return linkMenuItem;
        }

        public String getLinkType() {
            return this.linkType;
        }

        public String getName(Map<String, Object> context) {
            return this.nameExdr.expandString(context);
        }

        public List<Parameter> getParameterList() {
            return this.parameterList;
        }

        public Map<String, String> getParameterMap(Map<String, Object> context) {
            Map<String, String> fullParameterMap = new HashMap<String, String>();
            if (this.parametersMapAcsr != null) {
                Map<String, String> addlParamMap = this.parametersMapAcsr.get(context);
                if (addlParamMap != null) {
                    fullParameterMap.putAll(addlParamMap);
                }
            }
            for (Parameter parameter : this.parameterList) {
                fullParameterMap.put(parameter.getName(), parameter.getValue(context));
            }
            if (autoServiceParameters != null) {
                fullParameterMap.putAll(autoServiceParameters.getParametersMap(context, null));
            }
            if (autoEntityParameters != null) {
                fullParameterMap.putAll(autoEntityParameters.getParametersMap(context, linkMenuItem.getModelMenu()
                        .getDefaultEntityName()));
            }
            return fullParameterMap;
        }

        public String getPrefix(Map<String, Object> context) {
            return this.prefixExdr.expandString(context);
        }

        public boolean getRequestConfirmation() {
            return this.requestConfirmation;
        }

        public boolean getSecure() {
            return this.secure;
        }

        public String getStyle(Map<String, Object> context) {
            String style = this.styleExdr.expandString(context);
            if (UtilValidate.isEmpty(style)) {
                style = this.linkMenuItem.getWidgetStyle();
            }
            return style;
        }

        public String getTarget(Map<String, Object> context) {
            UtilCodec.SimpleEncoder simpleEncoder = (UtilCodec.SimpleEncoder) context.get("simpleEncoder");
            if (simpleEncoder != null) {
                return this.targetExdr.expandString(UtilCodec.HtmlEncodingMapWrapper.getHtmlEncodingMapWrapper(context,
                        simpleEncoder));
            } else {
                return this.targetExdr.expandString(context);
            }
        }

        public String getTargetWindow(Map<String, Object> context) {
            return this.targetWindowExdr.expandString(context);
        }

        public String getText(Map<String, Object> context) {
            String txt = this.textExdr.expandString(context);
            if (UtilValidate.isEmpty(txt)) {
                txt = linkMenuItem.getTitle(context);
            }
            // FIXME: Encoding should be done by the renderer, not by the model.
            UtilCodec.SimpleEncoder simpleEncoder = (UtilCodec.SimpleEncoder) context.get("simpleEncoder");
            if (simpleEncoder != null) {
                txt = simpleEncoder.encode(txt);
            }
            return txt;
        }

        public String getUrlMode() {
            return this.urlMode;
        }

        public void renderLinkString(Appendable writer, Map<String, Object> context, MenuStringRenderer menuStringRenderer)
                throws IOException {
            menuStringRenderer.renderLink(writer, context, this);
        }
    }
}
