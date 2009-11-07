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
import java.util.List;
import java.util.Map;

import org.ofbiz.base.util.BshUtil;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.UtilXml;
import org.ofbiz.base.util.collections.FlexibleMapAccessor;
import org.ofbiz.base.util.string.FlexibleStringExpander;
import org.ofbiz.entity.Delegator;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.widget.ModelWidget;
import org.w3c.dom.Element;

import bsh.EvalError;
import bsh.Interpreter;

/**
 * Widget Library - Menu model class
 */
@SuppressWarnings("serial")
public class ModelMenu extends ModelWidget {

    public static final String module = ModelMenu.class.getName();

    protected Delegator delegator;
    protected LocalDispatcher dispatcher;

    protected String menuLocation;
    protected String type;
    protected String target;
    protected String id;
    protected FlexibleStringExpander title;
    protected String tooltip;
    protected String defaultEntityName;
    protected String defaultTitleStyle;
    protected String defaultWidgetStyle;
    protected String defaultTooltipStyle;
    protected String defaultSelectedStyle;
    protected String defaultMenuItemName;
    protected String defaultPermissionOperation;
    protected String defaultPermissionEntityAction;
    protected FlexibleStringExpander defaultAssociatedContentId;
    protected String defaultPermissionStatusId;
    protected String defaultPrivilegeEnumId;
    protected String orientation = "horizontal";
    protected String menuWidth;
    protected String defaultCellWidth;
    protected Boolean defaultHideIfSelected;
    protected String defaultDisabledTitleStyle;
    protected FlexibleMapAccessor<String> selectedMenuItemContextFieldName;
    protected FlexibleStringExpander menuContainerStyleExdr;
    protected String defaultAlign;
    protected String defaultAlignStyle;
    protected String fillStyle;

    /** This List will contain one copy of each item for each item name in the order
     * they were encountered in the service, entity, or menu definition; item definitions
     * with constraints will also be in this list but may appear multiple times for the same
     * item name.
     *
     * When rendering the menu the order in this list should be following and it should not be
     * necessary to use the Map. The Map is used when loading the menu definition to keep the
     * list clean and implement the override features for item definitions.
     */
    protected List<ModelMenuItem> menuItemList = new ArrayList<ModelMenuItem>();

    /** This Map is keyed with the item name and has a ModelMenuItem for the value; items
     * with conditions will not be put in this Map so item definition overrides for items
     * with conditions is not possible.
     */
    protected Map<String, ModelMenuItem> menuItemMap = new HashMap<String, ModelMenuItem>();

    protected List<ModelMenuAction> actions;


   // ===== CONSTRUCTORS =====
    /** Default Constructor */
    public ModelMenu() {}

    /** XML Constructor */
    public ModelMenu(Element menuElement, Delegator delegator, LocalDispatcher dispatcher) {
        super(menuElement);
        this.delegator = delegator;
        this.dispatcher = dispatcher;

        // check if there is a parent menu to inherit from
        String parentResource = menuElement.getAttribute("extends-resource");
        String parentMenu = menuElement.getAttribute("extends");
        if (parentMenu.length() > 0 && !(parentMenu.equals(menuElement.getAttribute("name")) && UtilValidate.isEmpty(parentResource))) {
            ModelMenu parent = null;
            // check if we have a resource name (part of the string before the ?)
            if (UtilValidate.isNotEmpty(parentResource)) {
                try {
                    parent = MenuFactory.getMenuFromLocation(parentResource, parentMenu, delegator, dispatcher);
                } catch (Exception e) {
                    Debug.logError(e, "Failed to load parent menu definition '" + parentMenu + "' at resource '" + parentResource + "'", module);
                }
            } else {
                // try to find a menu definition in the same file
                Element rootElement = menuElement.getOwnerDocument().getDocumentElement();
                List<? extends Element> menuElements = UtilXml.childElementList(rootElement, "menu");
                //Uncomment below to add support for abstract menus
                //menuElements.addAll(UtilXml.childElementList(rootElement, "abstract-menu"));
                for (Element menuElementEntry : menuElements) {
                    if (menuElementEntry.getAttribute("name").equals(parentMenu)) {
                        parent = new ModelMenu(menuElementEntry, delegator, dispatcher);
                        break;
                    }
                }
                if (parent == null) {
                    Debug.logError("Failed to find parent menu definition '" + parentMenu + "' in same document.", module);
                }
            }

            if (parent != null) {
                this.type = parent.type;
                this.target = parent.target;
                this.id = parent.id;
                this.title = parent.title;
                this.tooltip = parent.tooltip;
                this.defaultEntityName = parent.defaultEntityName;
                this.defaultTitleStyle = parent.defaultTitleStyle;
                this.defaultSelectedStyle = parent.defaultSelectedStyle;
                this.defaultWidgetStyle = parent.defaultWidgetStyle;
                this.defaultTooltipStyle = parent.defaultTooltipStyle;
                this.defaultMenuItemName = parent.defaultMenuItemName;
                this.menuItemList.addAll(parent.menuItemList);
                this.menuItemMap.putAll(parent.menuItemMap);
                this.defaultPermissionOperation = parent.defaultPermissionOperation;
                this.defaultPermissionEntityAction = parent.defaultPermissionEntityAction;
                this.defaultAssociatedContentId = parent.defaultAssociatedContentId;
                this.defaultPermissionStatusId = parent.defaultPermissionStatusId;
                this.defaultPrivilegeEnumId = parent.defaultPrivilegeEnumId;
                this.defaultHideIfSelected = parent.defaultHideIfSelected;
                this.orientation = parent.orientation;
                this.menuWidth = parent.menuWidth;
                this.defaultCellWidth = parent.defaultCellWidth;
                this.defaultDisabledTitleStyle = parent.defaultDisabledTitleStyle;
                this.defaultAlign = parent.defaultAlign;
                this.defaultAlignStyle = parent.defaultAlignStyle;
                this.fillStyle = parent.fillStyle;
                this.selectedMenuItemContextFieldName = parent.selectedMenuItemContextFieldName;
                this.menuContainerStyleExdr = parent.menuContainerStyleExdr;
                if (parent.actions != null) {
                    this.actions = new ArrayList<ModelMenuAction>();
                    this.actions.addAll(parent.actions);
                }
            }
        }

        if (this.type == null || menuElement.hasAttribute("type"))
            this.type = menuElement.getAttribute("type");
        if (this.target == null || menuElement.hasAttribute("target"))
            this.target = menuElement.getAttribute("target");
        if (this.id == null || menuElement.hasAttribute("id"))
            this.id = menuElement.getAttribute("id");
        if (this.title == null || menuElement.hasAttribute("title"))
            this.setTitle(menuElement.getAttribute("title"));
        if (this.tooltip == null || menuElement.hasAttribute("tooltip"))
            this.tooltip = menuElement.getAttribute("tooltip");
        if (this.defaultEntityName == null || menuElement.hasAttribute("default-entity-name"))
            this.defaultEntityName = menuElement.getAttribute("default-entity-name");
        if (this.defaultTitleStyle == null || menuElement.hasAttribute("default-title-style"))
            this.defaultTitleStyle = menuElement.getAttribute("default-title-style");
        if (this.defaultSelectedStyle == null || menuElement.hasAttribute("default-selected-style"))
            this.defaultSelectedStyle = menuElement.getAttribute("default-selected-style");
        if (this.defaultWidgetStyle == null || menuElement.hasAttribute("default-widget-style"))
            this.defaultWidgetStyle = menuElement.getAttribute("default-widget-style");
        if (this.defaultTooltipStyle == null || menuElement.hasAttribute("default-tooltip-style"))
            this.defaultTooltipStyle = menuElement.getAttribute("default-tooltip-style");
        if (this.defaultMenuItemName == null || menuElement.hasAttribute("default-menu-item-name"))
            this.defaultMenuItemName = menuElement.getAttribute("default-menu-item-name");
        if (this.defaultPermissionOperation == null || menuElement.hasAttribute("default-permission-operation"))
            this.defaultPermissionOperation = menuElement.getAttribute("default-permission-operation");
        if (this.defaultPermissionEntityAction == null || menuElement.hasAttribute("default-permission-entity-action"))
            this.defaultPermissionEntityAction = menuElement.getAttribute("default-permission-entity-action");
        if (this.defaultPermissionStatusId == null || menuElement.hasAttribute("defaultPermissionStatusId"))
            this.defaultPermissionStatusId = menuElement.getAttribute("default-permission-status-id");
        if (this.defaultPrivilegeEnumId == null || menuElement.hasAttribute("defaultPrivilegeEnumId"))
            this.defaultPrivilegeEnumId = menuElement.getAttribute("default-privilege-enum-id");
        if (this.defaultAssociatedContentId == null || menuElement.hasAttribute("defaultAssociatedContentId"))
            this.setDefaultAssociatedContentId(menuElement.getAttribute("default-associated-content-id"));
        if (this.orientation == null || menuElement.hasAttribute("orientation"))
            this.orientation = menuElement.getAttribute("orientation");
        if (this.menuWidth == null || menuElement.hasAttribute("menu-width"))
            this.menuWidth = menuElement.getAttribute("menu-width");
        if (this.defaultCellWidth == null || menuElement.hasAttribute("default-cell-width"))
            this.defaultCellWidth = menuElement.getAttribute("default-cell-width");
        if (menuElement.hasAttribute("default-hide-if-selected")) {
            String val = menuElement.getAttribute("default-hide-if-selected");
                //Debug.logInfo("in ModelMenu, hideIfSelected, val:" + val, module);
            if (val != null && val.equalsIgnoreCase("true"))
                defaultHideIfSelected = Boolean.TRUE;
            else
                defaultHideIfSelected = Boolean.FALSE;
        }
        if (this.defaultDisabledTitleStyle == null || menuElement.hasAttribute("default-disabled-title-style"))
            this.defaultDisabledTitleStyle = menuElement.getAttribute("default-disabled-title-style");
        if (this.selectedMenuItemContextFieldName == null || menuElement.hasAttribute("selected-menuitem-context-field-name"))
            this.selectedMenuItemContextFieldName = FlexibleMapAccessor.getInstance(menuElement.getAttribute("selected-menuitem-context-field-name"));
        if (this.menuContainerStyleExdr == null || menuElement.hasAttribute("menu-container-style"))
            this.setMenuContainerStyle(menuElement.getAttribute("menu-container-style"));
        if (this.defaultAlign == null || menuElement.hasAttribute("default-align"))
            this.defaultAlign = menuElement.getAttribute("default-align");
        if (this.defaultAlignStyle == null || menuElement.hasAttribute("default-align-style"))
            this.defaultAlignStyle = menuElement.getAttribute("default-align-style");
        if (this.fillStyle == null || menuElement.hasAttribute("fill-style"))
            this.fillStyle = menuElement.getAttribute("fill-style");

        // read all actions under the "actions" element
        Element actionsElement = UtilXml.firstChildElement(menuElement, "actions");
        if (actionsElement != null) {
            if (this.actions == null) {
                this.actions = ModelMenuAction.readSubActions(this, actionsElement);
            } else {
                this.actions.addAll(ModelMenuAction.readSubActions(this, actionsElement));
                ArrayList<ModelMenuAction> actionsList = (ArrayList<ModelMenuAction>)this.actions;
                actionsList.trimToSize();
            }
        }

        // read in add item defs, add/override one by one using the menuItemList
        List<? extends Element> itemElements = UtilXml.childElementList(menuElement, "menu-item");
        for (Element itemElement : itemElements) {
            ModelMenuItem modelMenuItem = new ModelMenuItem(itemElement, this);
            modelMenuItem = this.addUpdateMenuItem(modelMenuItem);
        }
    }
    /**
     * add/override modelMenuItem using the menuItemList and menuItemMap
     *
     * @return The same ModelMenuItem, or if merged with an existing item, the existing item.
     */
    public ModelMenuItem addUpdateMenuItem(ModelMenuItem modelMenuItem) {

            // not a conditional item, see if a named item exists in Map
            ModelMenuItem existingMenuItem = (ModelMenuItem) this.menuItemMap.get(modelMenuItem.getName());
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

    public ModelMenuItem getModelMenuItemByName(String name) {
            ModelMenuItem existingMenuItem = (ModelMenuItem) this.menuItemMap.get(name);
            return existingMenuItem;
    }

    public ModelMenuItem getModelMenuItemByContentId(String contentId, Map<String, Object> context) {

        ModelMenuItem existingMenuItem = null;
        if (UtilValidate.isEmpty(contentId))
            return existingMenuItem;
        for (ModelMenuItem mi : this.menuItemList) {
            String assocContentId = mi.getAssociatedContentId(context);
            if (contentId.equals(assocContentId)) {
                existingMenuItem = mi;
                break;
            }
        }
            return existingMenuItem;
    }

    /**
     * Renders this menu to a String, i.e. in a text format, as defined with the
     * MenuStringRenderer implementation.
     *
     * @param writer The Writer that the menu text will be written to
     * @param context Map containing the menu context; the following are
     *   reserved words in this context: parameters (Map), isError (Boolean),
     *   itemIndex (Integer, for lists only, otherwise null), bshInterpreter,
     *   menuName (String, optional alternate name for menu, defaults to the
     *   value of the name attribute)
     * @param menuStringRenderer An implementation of the MenuStringRenderer
     *   interface that is responsible for the actual text generation for
     *   different menu elements; implementing you own makes it possible to
     *   use the same menu definitions for many types of menu UIs
     */
    public void renderMenuString(Appendable writer, Map<String, Object> context, MenuStringRenderer menuStringRenderer) throws IOException {
        setWidgetBoundaryComments(context);

        boolean passed = true;

            //Debug.logInfo("in ModelMenu, name:" + this.getName(), module);
        if (passed) {
            ModelMenuAction.runSubActions(this.actions, context);
            if ("simple".equals(this.type)) {
                this.renderSimpleMenuString(writer, context, menuStringRenderer);
            } else {
                throw new IllegalArgumentException("The type " + this.getType() + " is not supported for menu with name " + this.getName());
            }
        }
            //Debug.logInfo("in ModelMenu, buffer:" + buffer.toString(), module);
    }

    public void renderSimpleMenuString(Appendable writer, Map<String, Object> context, MenuStringRenderer menuStringRenderer) throws IOException {
        //Iterator menuItemIter = null;
        //Set alreadyRendered = new TreeSet();

        // render menu open
        menuStringRenderer.renderMenuOpen(writer, context, this);

        // render formatting wrapper open
        menuStringRenderer.renderFormatSimpleWrapperOpen(writer, context, this);

        //Debug.logInfo("in ModelMenu, menuItemList:" + menuItemList, module);
        // render each menuItem row, except hidden & ignored rows
        // include portal pages if specified
        //menuStringRenderer.renderFormatSimpleWrapperRows(writer, context, this);
        for (ModelMenuItem item : this.menuItemList) {
                item.renderMenuItemString(writer, context, menuStringRenderer);
        }
        // render formatting wrapper close
        menuStringRenderer.renderFormatSimpleWrapperClose(writer, context, this);

        // render menu close
        menuStringRenderer.renderMenuClose(writer, context, this);
    }


    public LocalDispatcher getDispacher() {
        return this.dispatcher;
    }

    public Delegator getDelegator() {
        return this.delegator;
    }

    public String getDefaultEntityName() {
        return this.defaultEntityName;
    }

    public String getDefaultAlign() {
        return this.defaultAlign;
    }

    public String getDefaultAlignStyle() {
        return this.defaultAlignStyle;
    }


    public String getDefaultTitleStyle() {
        return this.defaultTitleStyle;
    }

    public String getDefaultDisabledTitleStyle() {
        return this.defaultDisabledTitleStyle;
    }

    public String getDefaultSelectedStyle() {
        return this.defaultSelectedStyle;
    }

    public String getDefaultWidgetStyle() {
        return this.defaultWidgetStyle;
    }

    public String getDefaultTooltipStyle() {
        return this.defaultTooltipStyle;
    }

    public String getDefaultMenuItemName() {
        return this.defaultMenuItemName;
    }

    public String getFillStyle() {
        return this.fillStyle;
    }

    public String getSelectedMenuItemContextFieldName(Map<String, Object> context) {
        String menuItemName = this.selectedMenuItemContextFieldName.get(context);
        if (UtilValidate.isEmpty(menuItemName)) {
            return this.defaultMenuItemName;
        }
        return menuItemName;
    }

    public String getCurrentMenuName(Map<String, Object> context) {
        return this.name;
    }

    public String getId() {
        return this.id;
    }

    public String getTitle(Map<String, Object> context) {
        return title.expandString(context);
    }

    public String getTooltip() {
        return this.tooltip;
    }

    public String getType() {
        return this.type;
    }

    @Override
    public String getBoundaryCommentName() {
        return menuLocation + "#" + name;
    }

    public Interpreter getBshInterpreter(Map<String, Object> context) throws EvalError {
        Interpreter bsh = (Interpreter) context.get("bshInterpreter");
        if (bsh == null) {
            bsh = BshUtil.makeInterpreter(context);
            context.put("bshInterpreter", bsh);
        }
        return bsh;
    }

    /**
     * @param string
     */
    public void setDefaultEntityName(String string) {
        this.defaultEntityName = string;
    }


    /**
     * @param string
     */
    public void setDefaultTitleStyle(String string) {
        this.defaultTitleStyle = string;
    }

    /**
     * @param string
     */
    public void setDefaultSelectedStyle(String string) {
        this.defaultSelectedStyle = string;
    }

    /**
     * @param string
     */
    public void setDefaultWidgetStyle(String string) {
        this.defaultWidgetStyle = string;
    }

    /**
     * @param string
     */
    public void setDefaultTooltipStyle(String string) {
        this.defaultTooltipStyle = string;
    }

    /**
     * @param string
     */
    public void setDefaultMenuItemName(String string) {
        this.defaultMenuItemName = string;
    }

    /**
     * @param string
     */
    public void setMenuLocation(String menuLocation) {
        this.menuLocation = menuLocation;
    }

    /**
     * @param string
     */
    public void setName(String string) {
        this.name = string;
    }

    /**
     * @param string
     */
    public void setTarget(String string) {
        this.target = string;
    }

    /**
     * @param string
     */
    public void setId(String string) {
        this.id = string;
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
    public void setTooltip(String string) {
        this.tooltip = string;
    }

    /**
     * @param string
     */
    public void setType(String string) {
        this.type = string;
    }

    /**
     * @param string
     */
    public void setDefaultAssociatedContentId(String string) {
        this.defaultAssociatedContentId = FlexibleStringExpander.getInstance(string);
    }

    /**
     * @param string
     */
    public void setMenuContainerStyle(String string) {
        this.menuContainerStyleExdr = FlexibleStringExpander.getInstance(string);
    }

    public String getDefaultAssociatedContentId(Map<String, Object> context) {
        return defaultAssociatedContentId.expandString(context);
    }
    public String getMenuContainerStyle(Map<String, Object> context) {
        return menuContainerStyleExdr.expandString(context);
    }

    /**
     * @param string
     */
    public void setDefaultPermissionOperation(String string) {
        this.defaultPermissionOperation = string;
    }

    public String getDefaultPermissionStatusId() {
        return this.defaultPermissionStatusId;
    }

    /**
     * @param string
     */
    public void setDefaultPermissionStatusId(String string) {
        this.defaultPermissionStatusId = string;
    }

    /**
     * @param string
     */
    public void setDefaultPrivilegeEnumId(String string) {
        this.defaultPrivilegeEnumId = string;
    }

    public String getDefaultPrivilegeEnumId() {
        return this.defaultPrivilegeEnumId;
    }

    /**
     * @param string
     */
    public void setOrientation(String string) {
        this.orientation = string;
    }

    public String getOrientation() {
        return this.orientation;
    }

    /**
     * @param string
     */
    public void setMenuWidth(String string) {
        this.menuWidth = string;
    }

    public String getMenuWidth() {
        return this.menuWidth;
    }

    /**
     * @param string
     */
    public void setDefaultCellWidth(String string) {
        this.defaultCellWidth = string;
    }

    public String getDefaultCellWidth() {
        return this.defaultCellWidth;
    }

    public String getDefaultPermissionOperation() {
        return this.defaultPermissionOperation;
    }

    /**
     * @param string
     */
    public void setDefaultPermissionEntityAction(String string) {
        this.defaultPermissionEntityAction = string;
    }

    public String getDefaultPermissionEntityAction() {
        return this.defaultPermissionEntityAction;
    }

    /**
     * @param val
     */
    public void setDefaultHideIfSelected(Boolean val) {
        this.defaultHideIfSelected = val;
    }

    public Boolean getDefaultHideIfSelected() {
        return this.defaultHideIfSelected;
    }

    public List<ModelMenuItem> getMenuItemList() {
        return menuItemList;
    }

}
