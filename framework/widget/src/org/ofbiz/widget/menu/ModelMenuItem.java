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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.StringUtil;
import org.ofbiz.base.util.UtilFormatOut;
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
import org.ofbiz.widget.WidgetWorker;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 * Widget Library - Form model class
 */
@SuppressWarnings("serial")
public class ModelMenuItem extends ModelWidget {

    public static final String module = ModelMenuItem.class.getName();

    protected List<ModelWidgetAction> actions;
    protected String align;
    protected String alignStyle;
    protected FlexibleStringExpander associatedContentId;
    protected String cellWidth;
    protected ModelMenuCondition condition;
    protected Map<String, Object> dataMap = new HashMap<String, Object>();
    protected boolean disabled = false;
    protected String disabledTitleStyle;
    protected String disableIfEmpty;
    protected String entityName;
    protected Boolean hasPermission;
    protected Boolean hideIfSelected;
    protected Link link;
    /** This List will contain one copy of each item for each item name in the order
     * they were encountered in the service, entity, or menu definition; item definitions
     * with constraints will also be in this list but may appear multiple times for the same
     * item name.
     *
     * When rendering the menu the order in this list should be following and it should not be
     * necessary to use the Map. The Map is used when loading the menu definition to keep the
     * list clean and implement the override features for item definitions.
     */
    protected List<ModelMenuItem> menuItemList = new LinkedList<ModelMenuItem>();
    /** This Map is keyed with the item name and has a ModelMenuItem for the value; items
     * with conditions will not be put in this Map so item definition overrides for items
     * with conditions is not possible.
     */
    protected Map<String, ModelMenuItem> menuItemMap = new HashMap<String, ModelMenuItem>();
    protected ModelMenu modelMenu;
    protected String overrideName = null;
    protected ModelMenuItem parentMenuItem;
    protected FlexibleStringExpander parentPortalPageId;
    protected Integer position = null;
    protected String selectedStyle;
    protected ModelMenu subMenu;
    protected FlexibleStringExpander title;
    protected String titleStyle;
    protected FlexibleStringExpander tooltip;
    protected String tooltipStyle;
    protected String widgetStyle;

    // ===== CONSTRUCTORS =====
    public ModelMenuItem(String name) {
        super(name);
    }

    public ModelMenuItem(Element menuItemElement) {
        super(menuItemElement);
        loadMenuItem(menuItemElement);
    }

    public ModelMenuItem(Element menuItemElement, ModelMenu modelMenu) {
        super(menuItemElement);
        loadMenuItem(menuItemElement, modelMenu);
    }

    public ModelMenuItem(Element menuItemElement, ModelMenuItem modelMenuItem) {
        super(menuItemElement);
        parentMenuItem = modelMenuItem;
        loadMenuItem(menuItemElement, modelMenuItem.getModelMenu());
    }

    private void loadMenuItem(Element menuItemElement, ModelMenu modelMenu) {
        this.modelMenu = modelMenu;
        loadMenuItem(menuItemElement);
    }

    private void loadMenuItem(Element menuItemElement) {
        this.entityName = menuItemElement.getAttribute("entity-name");
        this.setTitle(menuItemElement.getAttribute("title"));
        this.setTooltip(menuItemElement.getAttribute("tooltip"));
        this.setParentPortalPageId(menuItemElement.getAttribute("parent-portal-page-value"));
        this.titleStyle = menuItemElement.getAttribute("title-style");
        this.disabledTitleStyle = menuItemElement.getAttribute("disabled-title-style");
        this.widgetStyle = menuItemElement.getAttribute("widget-style");
        this.tooltipStyle = menuItemElement.getAttribute("tooltip-style");
        this.selectedStyle = menuItemElement.getAttribute("selected-style");
        this.setHideIfSelected(menuItemElement.getAttribute("hide-if-selected"));
        this.disableIfEmpty = menuItemElement.getAttribute("disable-if-empty");
        this.align = menuItemElement.getAttribute("align");
        this.alignStyle = menuItemElement.getAttribute("align-style");
        String positionStr = menuItemElement.getAttribute("position");
        try {
            if (UtilValidate.isNotEmpty(positionStr)) {
                position = Integer.valueOf(positionStr);
            }
        } catch (Exception e) {
            Debug.logError(e, "Could not convert position attribute of the field element to an integer: [" +
                    positionStr + "], using the default of the menu renderer", module);
        }

        this.setAssociatedContentId(menuItemElement.getAttribute("associated-content-id"));
        this.cellWidth = menuItemElement.getAttribute("cell-width");

        dataMap.put("name", getName());

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
        }

        Element linkElement = UtilXml.firstChildElement(menuItemElement, "link");
        if (linkElement != null) {
            link = new Link(linkElement, this);
        }

        // read in add item defs, add/override one by one using the menuItemList and menuItemMap
        List<? extends Element> itemElements = UtilXml.childElementList(menuItemElement, "menu-item");
        for (Element itemElement: itemElements) {
            ModelMenuItem modelMenuItem = new ModelMenuItem(itemElement, this);
            modelMenuItem = this.addUpdateMenuItem(modelMenuItem);
        }
        // read condition under the "condition" element
        Element conditionElement = UtilXml.firstChildElement(menuItemElement, "condition");
        if (conditionElement != null) {
            this.condition = new ModelMenuCondition(this, conditionElement);
        }
        // read all actions under the "actions" element
        Element actionsElement = UtilXml.firstChildElement(conditionElement, "actions");
        if (actionsElement != null) {
            this.actions = ModelWidgetAction.readSubActions(this, actionsElement);
        }

    }

    public ModelMenuItem addUpdateMenuItem(ModelMenuItem modelMenuItem) {

        // not a conditional item, see if a named item exists in Map
        ModelMenuItem existingMenuItem = this.menuItemMap.get(modelMenuItem.getName());
        if (existingMenuItem != null) {
            // does exist, update the item by doing a merge/override
            existingMenuItem.mergeOverrideModelMenuItem(modelMenuItem);
            return existingMenuItem;
        } else {
            // does not exist, add to List and Map
            this.menuItemList.add(modelMenuItem);
            this.menuItemMap.put(modelMenuItem.getName(), modelMenuItem);
            return modelMenuItem;
        }
    }

    public List<ModelMenuItem> getMenuItemList() {
        return menuItemList;
    }

    public void setHideIfSelected(String val) {
        if (UtilValidate.isNotEmpty(val))
            if (val.equalsIgnoreCase("true"))
                hideIfSelected = Boolean.TRUE;
            else
                hideIfSelected = Boolean.FALSE;
        else
            hideIfSelected = null;

    }

    public void setDisabled(boolean val) {
         this.disabled = val;
    }

    public boolean getDisabled() {
         return this.disabled;
    }

    @Override
    public String getName() {
        if (this.overrideName != null) {
            return this.overrideName;
        }
        return super.getName();
    }

    public void mergeOverrideModelMenuItem(ModelMenuItem overrideMenuItem) {
        if (overrideMenuItem == null)
            return;

        // incorporate updates for values that are not empty in the overrideMenuItem
        if (UtilValidate.isNotEmpty(overrideMenuItem.getName()))
            this.overrideName = overrideMenuItem.getName();
        if (UtilValidate.isNotEmpty(overrideMenuItem.entityName))
            this.entityName = overrideMenuItem.entityName;
        if (UtilValidate.isNotEmpty(overrideMenuItem.parentPortalPageId))
            this.parentPortalPageId = overrideMenuItem.parentPortalPageId;
        if (UtilValidate.isNotEmpty(overrideMenuItem.title))
            this.title = overrideMenuItem.title;
        if (UtilValidate.isNotEmpty(overrideMenuItem.tooltip))
            this.tooltip = overrideMenuItem.tooltip;
        if (UtilValidate.isNotEmpty(overrideMenuItem.titleStyle))
            this.titleStyle = overrideMenuItem.titleStyle;
        if (UtilValidate.isNotEmpty(overrideMenuItem.selectedStyle))
            this.selectedStyle = overrideMenuItem.selectedStyle;
        if (UtilValidate.isNotEmpty(overrideMenuItem.widgetStyle))
            this.widgetStyle = overrideMenuItem.widgetStyle;
        if (overrideMenuItem.position != null)
            this.position = overrideMenuItem.position;

    }

    public boolean shouldBeRendered(Map<String, Object> context) {
        boolean passed = true;
        if (this.condition != null) {
            if (!this.condition.eval(context)) {
                passed = false;
            }
        }
        return passed;
    }
    
    public void renderMenuItemString(Appendable writer, Map<String, Object> context, MenuStringRenderer menuStringRenderer) throws IOException {

        boolean passed = true;
        if (this.condition != null) {
            if (!this.condition.eval(context)) {
                passed = false;
            }
        }
        Locale locale = (Locale) context.get("locale");
           //Debug.logInfo("in ModelMenu, name:" + this.getName(), module);
        if (passed) {
            ModelWidgetAction.runSubActions(this.actions, context);
            String parentPortalPageId = this.getParentPortalPageId(context);
            if (UtilValidate.isNotEmpty(parentPortalPageId)) {
                List<GenericValue> portalPages = PortalPageWorker.getPortalPages(parentPortalPageId, context);
                if (UtilValidate.isNotEmpty(portalPages)) {
                    for (GenericValue portalPage : portalPages) {
                        if (UtilValidate.isNotEmpty(portalPage.getString("portalPageName"))) {
                            String itemName =  portalPage.getString("portalPageId");
                            ModelMenuItem localItem = new ModelMenuItem(itemName);
                            localItem.setTitle((String) portalPage.get("portalPageName", locale));
                            localItem.link = new Link(this);
                            List<WidgetWorker.Parameter> linkParams = localItem.link.getParameterList();
                            linkParams.add(new WidgetWorker.Parameter("portalPageId", portalPage.getString("portalPageId"), false));
                            linkParams.add(new WidgetWorker.Parameter("parentPortalPageId", parentPortalPageId, false));
                            if (link != null) {
                                localItem.link.setTarget(link.targetExdr.getOriginal());
                                linkParams.addAll(link.parameterList);
                            } else {
                                localItem.link.setTarget("showPortalPage");
                            }
                            localItem.link.setText((String)portalPage.get("portalPageName", locale));
                            localItem.modelMenu = this.getModelMenu();
                            menuStringRenderer.renderMenuItem(writer, context, localItem);
                        }
                    }
                }
            } else {
                menuStringRenderer.renderMenuItem(writer, context, this);
            }
        }
    }


    public ModelMenu getModelMenu() {
        return modelMenu;
    }

    public List<ModelWidgetAction> getActions() {
        return actions;
    }

    public String getEntityName() {
        if (UtilValidate.isNotEmpty(this.entityName)) {
            return this.entityName;
        } else if (parentMenuItem != null) {
            return parentMenuItem.getEntityName();
        } else {
            return this.modelMenu.getDefaultEntityName();
        }
    }

    public String getAlign() {
        if (UtilValidate.isNotEmpty(this.align)) {
            return this.align;
        } else if (parentMenuItem != null) {
            return parentMenuItem.getAlign();
        } else {
            return this.modelMenu.getDefaultAlign();
        }
    }

    public int getPosition() {
        if (this.position == null) {
            return 1;
        } else {
            return position.intValue();
        }
    }

    public String getTitle(Map<String, Object> context) {
            return title.expandString(context);
    }

    public String getTitleStyle() {
        if (UtilValidate.isNotEmpty(this.titleStyle)) {
            return this.titleStyle;
        } else if (parentMenuItem != null) {
            return parentMenuItem.getTitleStyle();
         } else {
            return this.modelMenu.getDefaultTitleStyle();
        }
    }

    public String getDisabledTitleStyle() {
        if (UtilValidate.isNotEmpty(this.disabledTitleStyle)) {
            return this.disabledTitleStyle;
        } else if (parentMenuItem != null) {
            return parentMenuItem.getDisabledTitleStyle();
        } else {
            return this.modelMenu.getDefaultDisabledTitleStyle();
        }
    }

    public void setDisabledTitleStyle(String style) {
            this.disabledTitleStyle = style;
    }

    public String getSelectedStyle() {
        if (UtilValidate.isNotEmpty(this.selectedStyle)) {
            return this.selectedStyle;
        } else if (parentMenuItem != null) {
            return parentMenuItem.getSelectedStyle();
        } else {
            return this.modelMenu.getDefaultSelectedStyle();
        }
    }

    public String getTooltip(Map<String, Object> context) {
        if (UtilValidate.isNotEmpty(tooltip)) {
            return tooltip.expandString(context);
        } else {
            return "";
        }
    }

    public void setParentPortalPageId(String string) {
        this.parentPortalPageId = FlexibleStringExpander.getInstance(string);
    }

    public String getParentPortalPageId(Map<String, Object> context) {
        return this.parentPortalPageId.expandString(context);
    }

    public String getWidgetStyle() {
        if (UtilValidate.isNotEmpty(this.widgetStyle)) {
            return this.widgetStyle;
        } else if (parentMenuItem != null) {
            return parentMenuItem.getWidgetStyle();
        } else {
            return this.modelMenu.getDefaultWidgetStyle();
        }
    }

    public String getAlignStyle() {
        if (UtilValidate.isNotEmpty(this.alignStyle)) {
            return this.alignStyle;
        } else if (parentMenuItem != null) {
            return parentMenuItem.getAlignStyle();
        } else {
            return this.modelMenu.getDefaultAlignStyle();
        }
    }

    public String getTooltipStyle() {
        if (UtilValidate.isNotEmpty(this.tooltipStyle)) {
            return this.tooltipStyle;
        } else if (parentMenuItem != null) {
            return parentMenuItem.getTooltipStyle();
        } else {
            return this.modelMenu.getDefaultTooltipStyle();
        }
    }

    /**
     * @param string
     */
    public void setEntityName(String string) {
        entityName = string;
    }

    /**
     * @param i
     */
    public void setPosition(int i) {
        position = Integer.valueOf(i);
    }


    /**
     * @param string
     */
    public void setTitle(String string) {
        this.title = FlexibleStringExpander.getInstance(string);
    }

    /**
     * @param string
     */
    public void setTitleStyle(String string) {
        this.titleStyle = string;
    }

    /**
     * @param string
     */
    public void setTooltip(String string) {
        this.tooltip = FlexibleStringExpander.getInstance(string);
    }

    /**
     * @param string
     */
    public void setWidgetStyle(String string) {
        this.widgetStyle = string;
    }

    /**
     * @param string
     */
    public void setTooltipStyle(String string) {
        this.tooltipStyle = string;
    }


    /**
     * @param string
     */
    public void setAssociatedContentId(String string) {
        this.associatedContentId = FlexibleStringExpander.getInstance(string);
    }

    public String getAssociatedContentId(Map<String, Object> context) {
        String retStr = null;
        if (this.associatedContentId != null) {
            retStr = associatedContentId.expandString(context);
        }
        if (UtilValidate.isEmpty(retStr)) {
            retStr = this.modelMenu.getDefaultAssociatedContentId(context);
        }
        return retStr;
    }


    /**
     * @param string
     */
    public void setCellWidth(String string) {
        this.cellWidth = string;
    }

    public String getCellWidth() {
        if (UtilValidate.isNotEmpty(this.cellWidth)) {
            return this.cellWidth ;
        } else {
            return this.modelMenu.getDefaultCellWidth ();
        }
    }

    /**
     * @param val
     */
    public void setHideIfSelected(Boolean val) {
        this.hideIfSelected = val;
    }

    public Boolean getHideIfSelected() {
        if (hideIfSelected != null) {
            return this.hideIfSelected;
        } else {
            return this.modelMenu.getDefaultHideIfSelected();
        }
    }

    public String getDisableIfEmpty() {
            return this.disableIfEmpty;
    }

    /**
     * @param val
     */
    public void setHasPermission(Boolean val) {
        this.hasPermission = val;
    }

    public Boolean getHasPermission() {
        return this.hasPermission;
    }

    public Link getLink() {
       return this.link;
    }

    public boolean isSelected(Map<String, Object> context) {
        return getName().equals(modelMenu.getSelectedMenuItemContextFieldName(context));
    }

    public static class Link {
        protected ModelMenuItem linkMenuItem;
        protected FlexibleStringExpander textExdr;
        protected FlexibleStringExpander idExdr;
        protected FlexibleStringExpander styleExdr;
        protected FlexibleStringExpander targetExdr;
        protected FlexibleStringExpander targetWindowExdr;
        protected FlexibleStringExpander prefixExdr;
        protected FlexibleStringExpander nameExdr;
        protected Image image;
        protected String urlMode = "intra-app";
        protected boolean fullPath = false;
        protected boolean secure = false;
        protected boolean encode = false;
        protected String linkType;
        protected WidgetWorker.AutoServiceParameters autoServiceParameters;
        protected WidgetWorker.AutoEntityParameters autoEntityParameters;
        protected FlexibleMapAccessor<Map<String, String>> parametersMapAcsr;
        protected List<WidgetWorker.Parameter> parameterList = new ArrayList<WidgetWorker.Parameter>();
        protected boolean requestConfirmation = false;
        protected FlexibleStringExpander confirmationMsgExdr;

        public Link(Element linkElement, ModelMenuItem parentMenuItem) {
            this.linkMenuItem = parentMenuItem;
            setText(linkElement.getAttribute("text"));
            setId(linkElement.getAttribute("id"));
            setStyle(linkElement.getAttribute("style"));
            setTarget(linkElement.getAttribute("target"));
            setTargetWindow(linkElement.getAttribute("target-window"));
            setPrefix(linkElement.getAttribute("prefix"));
            setUrlMode(linkElement.getAttribute("url-mode"));
            setFullPath(linkElement.getAttribute("full-path"));
            setSecure(linkElement.getAttribute("secure"));
            setEncode(linkElement.getAttribute("encode"));
            setName(linkElement.getAttribute("name"));
            Element imageElement = UtilXml.firstChildElement(linkElement, "image");
            if (imageElement != null) {
                this.image = new Image(imageElement);
            }

            this.linkType = linkElement.getAttribute("link-type");
            this.parametersMapAcsr = FlexibleMapAccessor.getInstance(linkElement.getAttribute("parameters-map"));
            List<? extends Element> parameterElementList = UtilXml.childElementList(linkElement, "parameter");
            for (Element parameterElement: parameterElementList) {
                this.parameterList.add(new WidgetWorker.Parameter(parameterElement));
            }
            setRequestConfirmation("true".equals(linkElement.getAttribute("request-confirmation")));
            setConfirmationMsg(linkElement.getAttribute("confirmation-message"));
            Element autoServiceParamsElement = UtilXml.firstChildElement(linkElement, "auto-parameters-service");
            if (autoServiceParamsElement != null) {
                autoServiceParameters = new WidgetWorker.AutoServiceParameters(autoServiceParamsElement);
            }
            Element autoEntityParamsElement = UtilXml.firstChildElement(linkElement, "auto-parameters-entity");
            if (autoEntityParamsElement != null) {
                autoEntityParameters = new WidgetWorker.AutoEntityParameters(autoEntityParamsElement);
            }
        }

        public Link(ModelMenuItem parentMenuItem) {
            this.linkMenuItem = parentMenuItem;
            setText("");
            setId("");
            setStyle("");
            setTarget("");
            setTargetWindow("");
            setPrefix("");
            setUrlMode("");
            setFullPath("");
            setSecure("");
            setEncode("");
            setName("");
            setConfirmationMsg("");
        }

        public void renderLinkString(Appendable writer, Map<String, Object> context, MenuStringRenderer menuStringRenderer) throws IOException {
            menuStringRenderer.renderLink(writer, context, this);
        }

        public String getText(Map<String, Object> context) {
            String txt = this.textExdr.expandString(context);
            if (UtilValidate.isEmpty(txt)) txt = linkMenuItem.getTitle(context);

            StringUtil.SimpleEncoder simpleEncoder = (StringUtil.SimpleEncoder) context.get("simpleEncoder");
            if (simpleEncoder != null) {
                txt = simpleEncoder.encode(txt);
            }

            return txt;
        }

        public String getId(Map<String, Object> context) {
            return this.idExdr.expandString(context);
        }

        public String getStyle(Map<String, Object> context) {
            String style = this.styleExdr.expandString(context);
            if (UtilValidate.isEmpty(style)) {
                style = this.linkMenuItem.getWidgetStyle();
            }
            return style;
        }

        public String getName(Map<String, Object> context) {
            return this.nameExdr.expandString(context);
        }

        public String getTarget(Map<String, Object> context) {
            StringUtil.SimpleEncoder simpleEncoder = (StringUtil.SimpleEncoder) context.get("simpleEncoder");
            if (simpleEncoder != null) {
                return this.targetExdr.expandString(StringUtil.HtmlEncodingMapWrapper.getHtmlEncodingMapWrapper(context, simpleEncoder));
            } else {
                return this.targetExdr.expandString(context);
            }
        }

        public String getTargetWindow(Map<String, Object> context) {
            return this.targetWindowExdr.expandString(context);
        }

        public String getUrlMode() {
            return this.urlMode;
        }

        public String getPrefix(Map<String, Object> context) {
            return this.prefixExdr.expandString(context);
        }

        public boolean getFullPath() {
            return this.fullPath;
        }

        public boolean getSecure() {
            return this.secure;
        }

        public boolean getEncode() {
            return this.encode;
        }

        public Image getImage() {
            return this.image;
        }

        public String getLinkType() {
            return this.linkType;
        }

        public List<WidgetWorker.Parameter> getParameterList() {
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

            for (WidgetWorker.Parameter parameter: this.parameterList) {
                fullParameterMap.put(parameter.getName(), parameter.getValue(context));
            }
            if (autoServiceParameters != null) {
                fullParameterMap.putAll(autoServiceParameters.getParametersMap(context, null));
            }
            if (autoEntityParameters != null) {
                fullParameterMap.putAll(autoEntityParameters.getParametersMap(context, linkMenuItem.getModelMenu().getDefaultEntityName()));
            }
            return fullParameterMap;
        }

        public String getConfirmation(Map<String, Object> context) {
            String message = getConfirmationMsg(context);
            if (UtilValidate.isNotEmpty(message)) {
                return message;
            }
            else if (getRequestConfirmation()) {
                String defaultMessage = UtilProperties.getPropertyValue("general", "default.confirmation.message", "${uiLabelMap.CommonConfirm}");
                setConfirmationMsg(defaultMessage);
                return getConfirmationMsg(context);
            }
            return "";
        }

        public boolean getRequestConfirmation() {
            return this.requestConfirmation;
        }

        public String getConfirmationMsg(Map<String, Object> context) {
            return this.confirmationMsgExdr.expandString(context);
        }

        public void setText(String val) {
            String textAttr = UtilFormatOut.checkNull(val);
            this.textExdr = FlexibleStringExpander.getInstance(textAttr);
        }

        public void setId(String val) {
            this.idExdr = FlexibleStringExpander.getInstance(val);
        }

        public void setStyle(String val) {
            this.styleExdr = FlexibleStringExpander.getInstance(val);
        }

        public void setTarget(String val) {
            this.targetExdr = FlexibleStringExpander.getInstance(val);
        }

        public void setTargetWindow(String val) {
            this.targetWindowExdr = FlexibleStringExpander.getInstance(val);
        }

        public void setPrefix(String val) {
            this.prefixExdr = FlexibleStringExpander.getInstance(val);
        }

        public void setUrlMode(String val) {
            if (UtilValidate.isNotEmpty(val))
                this.urlMode = val;
        }

        public void setName(String val) {
            this.nameExdr = FlexibleStringExpander.getInstance(val);
        }

        public void setFullPath(String val) {
            String sFullPath = val;
            if (sFullPath != null && sFullPath.equalsIgnoreCase("true"))
                this.fullPath = true;
            else
                this.fullPath = false;
        }

        public void setSecure(String val) {
            String sSecure = val;
            if (sSecure != null && sSecure.equalsIgnoreCase("true"))
                this.secure = true;
            else
                this.secure = false;
        }

        public void setEncode(String val) {
            String sEncode = val;
            if (sEncode != null && sEncode.equalsIgnoreCase("true"))
                this.encode = true;
            else
                this.encode = false;
        }

        public void setImage(Image img) {
            this.image = img;
        }

        public void setRequestConfirmation(boolean val) {
            this.requestConfirmation = val;
        }

        public void setConfirmationMsg(String val) {
            this.confirmationMsgExdr = FlexibleStringExpander.getInstance(val);
        }

        public ModelMenuItem getLinkMenuItem() {
            return linkMenuItem;
        }

    }

    public static class Image {

        protected FlexibleStringExpander srcExdr;
        protected FlexibleStringExpander idExdr;
        protected FlexibleStringExpander styleExdr;
        protected FlexibleStringExpander widthExdr;
        protected FlexibleStringExpander heightExdr;
        protected FlexibleStringExpander borderExdr;
        protected String urlMode;

        public Image(Element imageElement) {

            setSrc(imageElement.getAttribute("src"));
            setId(imageElement.getAttribute("id"));
            setStyle(imageElement.getAttribute("style"));
            setWidth(imageElement.getAttribute("width"));
            setHeight(imageElement.getAttribute("height"));
            setBorder(UtilFormatOut.checkEmpty(imageElement.getAttribute("border"), "0"));
            setUrlMode(UtilFormatOut.checkEmpty(imageElement.getAttribute("url-mode"), "content"));

        }

        public void renderImageString(Appendable writer, Map<String, Object> context, MenuStringRenderer menuStringRenderer) throws IOException {
            menuStringRenderer.renderImage(writer, context, this);
        }

        public String getSrc(Map<String, Object> context) {
            return this.srcExdr.expandString(context);
        }

        public String getId(Map<String, Object> context) {
            return this.idExdr.expandString(context);
        }

        public String getStyle(Map<String, Object> context) {
            return this.styleExdr.expandString(context);
        }

        public String getWidth(Map<String, Object> context) {
            return this.widthExdr.expandString(context);
        }

        public String getHeight(Map<String, Object> context) {
            return this.heightExdr.expandString(context);
        }

        public String getBorder(Map<String, Object> context) {
            return this.borderExdr.expandString(context);
        }

        public String getUrlMode() {
            return this.urlMode;
        }

        public void setSrc(String val) {
            String textAttr = UtilFormatOut.checkNull(val);
            this.srcExdr = FlexibleStringExpander.getInstance(textAttr);
        }

        public void setId(String val) {
            this.idExdr = FlexibleStringExpander.getInstance(val);
        }

        public void setStyle(String val) {
            this.styleExdr = FlexibleStringExpander.getInstance(val);
        }

        public void setWidth(String val) {
            this.widthExdr = FlexibleStringExpander.getInstance(val);
        }

        public void setHeight(String val) {
            this.heightExdr = FlexibleStringExpander.getInstance(val);
        }

        public void setBorder(String val) {
            this.borderExdr = FlexibleStringExpander.getInstance(val);
        }

        public void setUrlMode(String val) {
            if (UtilValidate.isEmpty(val))
                this.urlMode = "content";
            else
                this.urlMode = val;
        }

    }

    @Override
    public void accept(ModelWidgetVisitor visitor) throws Exception {
        visitor.visit(this);
    }
}
