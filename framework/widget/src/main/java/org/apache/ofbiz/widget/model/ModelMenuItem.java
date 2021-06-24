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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.UtilXml;
import org.apache.ofbiz.base.util.string.FlexibleStringExpander;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.widget.model.CommonWidgetModels.AutoEntityParameters;
import org.apache.ofbiz.widget.model.CommonWidgetModels.AutoServiceParameters;
import org.apache.ofbiz.widget.model.CommonWidgetModels.Image;
import org.apache.ofbiz.widget.model.CommonWidgetModels.Link;
import org.apache.ofbiz.widget.model.CommonWidgetModels.Parameter;
import org.apache.ofbiz.widget.portal.PortalPageWorker;
import org.apache.ofbiz.widget.renderer.MenuStringRenderer;
import org.w3c.dom.Element;

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
     * This model is intended to be a read-only data structure that represents
     * an XML element. Outside of object construction, the class should not
     * have any behaviors.
     * Instances of this class will be shared by multiple threads - therefore
     * it is immutable. DO NOT CHANGE THE OBJECT'S STATE AT RUN TIME!
     */

    private static final String MODULE = ModelMenuItem.class.getName();

    private final List<ModelAction> actions;
    private final String align;
    private final String alignStyle;
    private final FlexibleStringExpander associatedContentId;
    private final String cellWidth;
    private final ModelMenuCondition condition;
    private final String disabledTitleStyle;
    private final String disableIfEmpty;
    private final String entityName;
    private final Boolean hideIfSelected;
    private final MenuLink link;
    private final List<ModelMenuItem> menuItemList;
    private final ModelMenu modelMenu;
    private final String overrideName;
    private final ModelMenuItem parentMenuItem;
    private final FlexibleStringExpander parentPortalPageId;
    private final Integer position;
    private final String selectedStyle;
    private final String subMenu;
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
        if (!hideIfSelected.isEmpty()) {
            if ("true".equalsIgnoreCase(hideIfSelected)) {
                this.hideIfSelected = Boolean.TRUE;
            } else {
                this.hideIfSelected = Boolean.FALSE;
            }
        } else {
            this.hideIfSelected = null;
        }
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
                        + "], using the default of the menu renderer", MODULE);
                position = null;
            }
        }
        this.position = position;
        this.associatedContentId = FlexibleStringExpander.getInstance(menuItemElement.getAttribute("associated-content-id"));
        this.cellWidth = menuItemElement.getAttribute("cell-width");
        this.subMenu = menuItemElement.getAttribute("sub-menu");
        Element linkElement = UtilXml.firstChildElement(menuItemElement, "link");
        if (linkElement != null) {
            this.link = new MenuLink(linkElement, this);
        } else {
            this.link = null;
        }
        // read in add item defs, add/override one by one using the menuItemList and menuItemMap
        List<? extends Element> itemElements = UtilXml.childElementList(menuItemElement, "menu-item");
        if (!itemElements.isEmpty()) {
            ArrayList<ModelMenuItem> menuItemList = new ArrayList<>();
            Map<String, ModelMenuItem> menuItemMap = new HashMap<>();
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
            conditionElement = UtilXml.firstChildElement(conditionElement);
            this.condition = new ModelMenuCondition(this, conditionElement);
        } else {
            this.condition = null;
        }
        // read all actions under the "actions" element
        Element actionsElement = UtilXml.firstChildElement(conditionElement, "actions");
        if (actionsElement != null) {
            this.actions = AbstractModelAction.readSubActions(this, actionsElement);
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
        this.subMenu = "";
        this.title = FlexibleStringExpander.getInstance((String) portalPage.get("portalPageName", locale));
        this.titleStyle = "";
        this.tooltip = FlexibleStringExpander.getInstance("");
        this.tooltipStyle = "";
        this.widgetStyle = "";
        this.link = new MenuLink(portalPage, parentMenuItem, locale);
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

    private static void addUpdateMenuItem(ModelMenuItem modelMenuItem, List<ModelMenuItem> menuItemList,
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

    /**
     * Gets actions.
     * @return the actions
     */
    public List<ModelAction> getActions() {
        return actions;
    }

    /**
     * Gets align.
     * @return the align
     */
    public String getAlign() {
        if (!this.align.isEmpty()) {
            return this.align;
        } else if (parentMenuItem != null) {
            return parentMenuItem.getAlign();
        } else {
            return this.modelMenu.getDefaultAlign();
        }
    }

    /**
     * Gets align style.
     * @return the align style
     */
    public String getAlignStyle() {
        if (!this.alignStyle.isEmpty()) {
            return this.alignStyle;
        } else if (parentMenuItem != null) {
            return parentMenuItem.getAlignStyle();
        } else {
            return this.modelMenu.getDefaultAlignStyle();
        }
    }

    /**
     * Gets associated content id.
     * @return the associated content id
     */
    public FlexibleStringExpander getAssociatedContentId() {
        return associatedContentId;
    }

    /**
     * Gets associated content id.
     * @param context the context
     * @return the associated content id
     */
    public String getAssociatedContentId(Map<String, Object> context) {
        String retStr = null;
        if (this.associatedContentId != null) {
            retStr = associatedContentId.expandString(context);
        }
        if (retStr == null || retStr.isEmpty()) {
            retStr = this.modelMenu.getDefaultAssociatedContentId(context);
        }
        return retStr;
    }

    /**
     * Gets cell width.
     * @return the cell width
     */
    public String getCellWidth() {
        if (!this.cellWidth.isEmpty()) {
            return this.cellWidth;
        }
        return this.modelMenu.getDefaultCellWidth();
    }

    /**
     * Gets condition.
     * @return the condition
     */
    public ModelMenuCondition getCondition() {
        return condition;
    }

    /**
     * Gets disabled title style.
     * @return the disabled title style
     */
    public String getDisabledTitleStyle() {
        if (!this.disabledTitleStyle.isEmpty()) {
            return this.disabledTitleStyle;
        } else if (parentMenuItem != null) {
            return parentMenuItem.getDisabledTitleStyle();
        } else {
            return this.modelMenu.getDefaultDisabledTitleStyle();
        }
    }

    /**
     * Gets disable if empty.
     * @return the disable if empty
     */
    public String getDisableIfEmpty() {
        return this.disableIfEmpty;
    }

    /**
     * Gets entity name.
     * @return the entity name
     */
    public String getEntityName() {
        if (!this.entityName.isEmpty()) {
            return this.entityName;
        } else if (parentMenuItem != null) {
            return parentMenuItem.getEntityName();
        } else {
            return this.modelMenu.getDefaultEntityName();
        }
    }

    /**
     * Gets hide if selected.
     * @return the hide if selected
     */
    public Boolean getHideIfSelected() {
        if (hideIfSelected != null) {
            return this.hideIfSelected;
        }
        return this.modelMenu.getDefaultHideIfSelected();
    }

    /**
     * Gets link.
     * @return the link
     */
    public MenuLink getLink() {
        return this.link;
    }

    /**
     * Gets menu item list.
     * @return the menu item list
     */
    public List<ModelMenuItem> getMenuItemList() {
        return menuItemList;
    }

    /**
     * Gets model menu.
     * @return the model menu
     */
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

    /**
     * Gets override name.
     * @return the override name
     */
    public String getOverrideName() {
        return overrideName;
    }

    /**
     * Gets parent menu item.
     * @return the parent menu item
     */
    public ModelMenuItem getParentMenuItem() {
        return parentMenuItem;
    }

    /**
     * Gets parent portal page id.
     * @return the parent portal page id
     */
    public FlexibleStringExpander getParentPortalPageId() {
        return parentPortalPageId;
    }

    /**
     * Gets parent portal page id.
     * @param context the context
     * @return the parent portal page id
     */
    public String getParentPortalPageId(Map<String, Object> context) {
        return this.parentPortalPageId.expandString(context);
    }

    /**
     * Gets position.
     * @return the position
     */
    public int getPosition() {
        if (this.position == null) {
            return 1;
        }
        return position;
    }

    /**
     * Gets selected style.
     * @return the selected style
     */
    public String getSelectedStyle() {
        if (!this.selectedStyle.isEmpty()) {
            return this.selectedStyle;
        } else if (parentMenuItem != null) {
            return parentMenuItem.getSelectedStyle();
        } else {
            return this.modelMenu.getDefaultSelectedStyle();
        }
    }

    /**
     * Gets sub menu.
     * @return the sub menu
     */
    public String getSubMenu() {
        return subMenu;
    }

    /**
     * Gets title.
     * @return the title
     */
    public FlexibleStringExpander getTitle() {
        return title;
    }

    /**
     * Gets title.
     * @param context the context
     * @return the title
     */
    public String getTitle(Map<String, Object> context) {
        return title.expandString(context);
    }

    /**
     * Gets title style.
     * @return the title style
     */
    public String getTitleStyle() {
        if (!this.titleStyle.isEmpty()) {
            return this.titleStyle;
        } else if (parentMenuItem != null) {
            return parentMenuItem.getTitleStyle();
        } else {
            return this.modelMenu.getDefaultTitleStyle();
        }
    }

    /**
     * Gets tooltip.
     * @return the tooltip
     */
    public FlexibleStringExpander getTooltip() {
        return tooltip;
    }

    /**
     * Gets tooltip.
     * @param context the context
     * @return the tooltip
     */
    public String getTooltip(Map<String, Object> context) {
        if (UtilValidate.isNotEmpty(tooltip)) {
            return tooltip.expandString(context);
        }
        return "";
    }

    /**
     * Gets tooltip style.
     * @return the tooltip style
     */
    public String getTooltipStyle() {
        if (!this.tooltipStyle.isEmpty()) {
            return this.tooltipStyle;
        } else if (parentMenuItem != null) {
            return parentMenuItem.getTooltipStyle();
        } else {
            return this.modelMenu.getDefaultTooltipStyle();
        }
    }

    /**
     * Gets widget style.
     * @return the widget style
     */
    public String getWidgetStyle() {
        if (!this.widgetStyle.isEmpty()) {
            return this.widgetStyle;
        } else if (parentMenuItem != null) {
            return parentMenuItem.getWidgetStyle();
        } else {
            return this.modelMenu.getDefaultWidgetStyle();
        }
    }

    /**
     * Is selected boolean.
     * @param context the context
     * @return the boolean
     */
    public boolean isSelected(Map<String, Object> context) {
        return getName().equals(modelMenu.getSelectedMenuItemContextFieldName(context));
    }

    /**
     * Merge override model menu item model menu item.
     * @param overrideMenuItem the override menu item
     * @return the model menu item
     */
    public ModelMenuItem mergeOverrideModelMenuItem(ModelMenuItem overrideMenuItem) {
        return new ModelMenuItem(this, overrideMenuItem);
    }

    /**
     * Render menu item string.
     * @param writer the writer
     * @param context the context
     * @param menuStringRenderer the menu string renderer
     * @throws IOException the io exception
     */
    public void renderMenuItemString(Appendable writer, Map<String, Object> context, MenuStringRenderer menuStringRenderer)
            throws IOException {
        if (shouldBeRendered(context)) {
            AbstractModelAction.runSubActions(actions, context);
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

    /**
     * Should be rendered boolean.
     * @param context the context
     * @return the boolean
     */
    public boolean shouldBeRendered(Map<String, Object> context) {
        if (this.condition != null) {
            return this.condition.getCondition().eval(context);
        }
        return true;
    }

    public static class MenuLink {
        private final ModelMenuItem linkMenuItem;
        private final Link link;

        public MenuLink(Element linkElement, ModelMenuItem parentMenuItem) {
            this.linkMenuItem = parentMenuItem;
            if (linkElement.getAttribute("text").isEmpty()) {
                linkElement.setAttribute("text", parentMenuItem.getTitle().getOriginal());
            }
            this.link = new Link(linkElement);
        }

        public MenuLink(GenericValue portalPage, ModelMenuItem parentMenuItem, Locale locale) {
            this.linkMenuItem = parentMenuItem;
            List<Parameter> parameterList = new ArrayList<>();
            if (parentMenuItem.link != null) {
                parameterList.addAll(parentMenuItem.link.getParameterList());
            }
            parameterList.add(new Parameter("portalPageId", portalPage.getString("portalPageId"), false));
            parameterList.add(new Parameter("parentPortalPageId", portalPage.getString("parentPortalPageId"), false));
            String target = "showPortalPage";
            if (parentMenuItem.link != null) {
                target = parentMenuItem.link.getTargetExdr().getOriginal();
            }
            this.link = new Link(portalPage, parameterList, target, locale);
        }

        /**
         * Gets auto entity parameters.
         * @return the auto entity parameters
         */
        public AutoEntityParameters getAutoEntityParameters() {
            return link.getAutoEntityParameters();
        }

        /**
         * Gets auto service parameters.
         * @return the auto service parameters
         */
        public AutoServiceParameters getAutoServiceParameters() {
            return link.getAutoServiceParameters();
        }

        /**
         * Gets encode.
         * @return the encode
         */
        public boolean getEncode() {
            return link.getEncode();
        }

        /**
         * Gets full path.
         * @return the full path
         */
        public boolean getFullPath() {
            return link.getFullPath();
        }

        /**
         * Gets height.
         * @return the height
         */
        public String getHeight() {
            return link.getHeight();
        }

        /**
         * Gets id.
         * @param context the context
         * @return the id
         */
        public String getId(Map<String, Object> context) {
            return link.getId(context);
        }

        /**
         * Gets id exdr.
         * @return the id exdr
         */
        public FlexibleStringExpander getIdExdr() {
            return link.getIdExdr();
        }

        /**
         * Gets image.
         * @return the image
         */
        public Image getImage() {
            return link.getImage();
        }

        /**
         * Gets link type.
         * @return the link type
         */
        public String getLinkType() {
            return link.getLinkType();
        }

        /**
         * Gets name.
         * @return the name
         */
        public String getName() {
            return link.getName();
        }

        /**
         * Gets name.
         * @param context the context
         * @return the name
         */
        public String getName(Map<String, Object> context) {
            return link.getName(context);
        }

        /**
         * Gets name exdr.
         * @return the name exdr
         */
        public FlexibleStringExpander getNameExdr() {
            return link.getNameExdr();
        }

        /**
         * Gets parameter list.
         * @return the parameter list
         */
        public List<Parameter> getParameterList() {
            return link.getParameterList();
        }

        /**
         * Gets parameter map.
         * @param context the context
         * @return the parameter map
         */
        public Map<String, String> getParameterMap(Map<String, Object> context) {
            return link.getParameterMap(context);
        }

        /**
         * Gets parameter map.
         * @param context the context
         * @param propagateCallback indicate if we need to propagate callback element
         * @return the parameter map
         */
        public Map<String, String> getParameterMap(Map<String, Object> context, boolean propagateCallback) {
            return link.getParameterMap(context, propagateCallback);
        }

        /**
         * Gets prefix.
         * @param context the context
         * @return the prefix
         */
        public String getPrefix(Map<String, Object> context) {
            return link.getPrefix(context);
        }

        /**
         * Gets prefix exdr.
         * @return the prefix exdr
         */
        public FlexibleStringExpander getPrefixExdr() {
            return link.getPrefixExdr();
        }

        /**
         * Gets secure.
         * @return the secure
         */
        public boolean getSecure() {
            return link.getSecure();
        }

        /**
         * Gets style.
         * @param context the context
         * @return the style
         */
        public String getStyle(Map<String, Object> context) {
            return link.getStyle(context);
        }

        /**
         * Gets style exdr.
         * @return the style exdr
         */
        public FlexibleStringExpander getStyleExdr() {
            return link.getStyleExdr();
        }

        /**
         * Gets target.
         * @param context the context
         * @return the target
         */
        public String getTarget(Map<String, Object> context) {
            return link.getTarget(context);
        }

        /**
         * Gets target exdr.
         * @return the target exdr
         */
        public FlexibleStringExpander getTargetExdr() {
            return link.getTargetExdr();
        }

        /**
         * Gets target window.
         * @param context the context
         * @return the target window
         */
        public String getTargetWindow(Map<String, Object> context) {
            return link.getTargetWindow(context);
        }

        /**
         * Gets target window exdr.
         * @return the target window exdr
         */
        public FlexibleStringExpander getTargetWindowExdr() {
            return link.getTargetWindowExdr();
        }

        /**
         * Gets text.
         * @param context the context
         * @return the text
         */
        public String getText(Map<String, Object> context) {
            return link.getText(context);
        }

        /**
         * Gets text exdr.
         * @return the text exdr
         */
        public FlexibleStringExpander getTextExdr() {
            return link.getTextExdr();
        }

        /**
         * Gets url mode.
         * @return the url mode
         */
        public String getUrlMode() {
            return link.getUrlMode();
        }

        /**
         * Gets width.
         * @return the width
         */
        public String getWidth() {
            return link.getWidth();
        }

        /**
         * Gets link menu item.
         * @return the link menu item
         */
        public ModelMenuItem getLinkMenuItem() {
            return linkMenuItem;
        }

        /**
         * Gets link.
         * @return the link
         */
        public Link getLink() {
            return link;
        }

        /**
         * Render link string.
         * @param writer the writer
         * @param context the context
         * @param menuStringRenderer the menu string renderer
         * @throws IOException the io exception
         */
        public void renderLinkString(Appendable writer, Map<String, Object> context, MenuStringRenderer menuStringRenderer)
                throws IOException {
            menuStringRenderer.renderLink(writer, context, this);
        }
    }
}
