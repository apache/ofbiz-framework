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
import java.util.Map;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.UtilXml;
import org.ofbiz.base.util.collections.FlexibleMapAccessor;
import org.ofbiz.base.util.string.FlexibleStringExpander;
import org.ofbiz.widget.ModelWidget;
import org.ofbiz.widget.ModelWidgetAction;
import org.ofbiz.widget.ModelWidgetVisitor;
import org.w3c.dom.Element;

/**
 * Models the &lt;menu&gt; element.
 * 
 * @see <code>widget-menu.xsd</code>
 */
@SuppressWarnings("serial")
public class ModelMenu extends ModelWidget {

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

    public static final String module = ModelMenu.class.getName();

    private final List<ModelWidgetAction> actions;
    private final String defaultAlign;
    private final String defaultAlignStyle;
    private final FlexibleStringExpander defaultAssociatedContentId;
    private final String defaultCellWidth;
    private final String defaultDisabledTitleStyle;
    private final String defaultEntityName;
    private final Boolean defaultHideIfSelected;
    private final String defaultMenuItemName;
    private final String defaultPermissionEntityAction;
    private final String defaultPermissionOperation;
    private final String defaultPermissionStatusId;
    private final String defaultPrivilegeEnumId;
    private final String defaultSelectedStyle;
    private final String defaultTitleStyle;
    private final String defaultTooltipStyle;
    private final String defaultWidgetStyle;
    private final FlexibleStringExpander extraIndex;
    private final String fillStyle;
    private final String id;
    private final FlexibleStringExpander menuContainerStyleExdr;
    /** This List will contain one copy of each item for each item name in the order
     * they were encountered in the service, entity, or menu definition; item definitions
     * with constraints will also be in this list but may appear multiple times for the same
     * item name.
     *
     * When rendering the menu the order in this list should be following and it should not be
     * necessary to use the Map. The Map is used when loading the menu definition to keep the
     * list clean and implement the override features for item definitions.
     */
    private final List<ModelMenuItem> menuItemList;
    /** This Map is keyed with the item name and has a ModelMenuItem for the value; items
     * with conditions will not be put in this Map so item definition overrides for items
     * with conditions is not possible.
     */
    private final Map<String, ModelMenuItem> menuItemMap;
    private final String menuLocation;
    private final String menuWidth;
    private final String orientation;
    private final FlexibleMapAccessor<String> selectedMenuItemContextFieldName;
    private final String target;
    private final FlexibleStringExpander title;
    private final String tooltip;
    private final String type;

    /** XML Constructor */
    public ModelMenu(Element menuElement, String menuLocation) {
        super(menuElement);
        ArrayList<ModelWidgetAction> actions = new ArrayList<ModelWidgetAction>();
        String defaultAlign = "";
        String defaultAlignStyle = "";
        FlexibleStringExpander defaultAssociatedContentId = FlexibleStringExpander.getInstance("");
        String defaultCellWidth = "";
        String defaultDisabledTitleStyle = "";
        String defaultEntityName = "";
        Boolean defaultHideIfSelected = Boolean.FALSE;
        String defaultMenuItemName = "";
        String defaultPermissionEntityAction = "";
        String defaultPermissionOperation = "";
        String defaultPermissionStatusId = "";
        String defaultPrivilegeEnumId = "";
        String defaultSelectedStyle = "";
        String defaultTitleStyle = "";
        String defaultTooltipStyle = "";
        String defaultWidgetStyle = "";
        FlexibleStringExpander extraIndex = FlexibleStringExpander.getInstance("");
        String fillStyle = "";
        String id = "";
        FlexibleStringExpander menuContainerStyleExdr = FlexibleStringExpander.getInstance("");
        ArrayList<ModelMenuItem> menuItemList = new ArrayList<ModelMenuItem>();
        Map<String, ModelMenuItem> menuItemMap = new HashMap<String, ModelMenuItem>();
        String menuWidth = "";
        String orientation = "horizontal";
        FlexibleMapAccessor<String> selectedMenuItemContextFieldName = FlexibleMapAccessor.getInstance("");
        String target = "";
        FlexibleStringExpander title = FlexibleStringExpander.getInstance("");
        String tooltip = "";
        String type = "";
        // check if there is a parent menu to inherit from
        String parentResource = menuElement.getAttribute("extends-resource");
        String parentMenu = menuElement.getAttribute("extends");
        if (!parentMenu.isEmpty()) {
            ModelMenu parent = null;
            if (!parentResource.isEmpty()) {
                try {
                    parent = MenuFactory.getMenuFromLocation(parentResource, parentMenu);
                } catch (Exception e) {
                    Debug.logError(e, "Failed to load parent menu definition '" + parentMenu + "' at resource '" + parentResource
                            + "'", module);
                }
            } else {
                parentResource = menuLocation;
                // try to find a menu definition in the same file
                Element rootElement = menuElement.getOwnerDocument().getDocumentElement();
                List<? extends Element> menuElements = UtilXml.childElementList(rootElement, "menu");
                //Uncomment below to add support for abstract menus
                //menuElements.addAll(UtilXml.childElementList(rootElement, "abstract-menu"));
                for (Element menuElementEntry : menuElements) {
                    if (menuElementEntry.getAttribute("name").equals(parentMenu)) {
                        parent = new ModelMenu(menuElementEntry, parentResource);
                        break;
                    }
                }
                if (parent == null) {
                    Debug.logError("Failed to find parent menu definition '" + parentMenu + "' in same document.", module);
                }
            }
            if (parent != null) {
                type = parent.type;
                target = parent.target;
                id = parent.id;
                title = parent.title;
                tooltip = parent.tooltip;
                defaultEntityName = parent.defaultEntityName;
                defaultTitleStyle = parent.defaultTitleStyle;
                defaultSelectedStyle = parent.defaultSelectedStyle;
                defaultWidgetStyle = parent.defaultWidgetStyle;
                defaultTooltipStyle = parent.defaultTooltipStyle;
                defaultMenuItemName = parent.defaultMenuItemName;
                menuItemList.addAll(parent.menuItemList);
                menuItemMap.putAll(parent.menuItemMap);
                defaultPermissionOperation = parent.defaultPermissionOperation;
                defaultPermissionEntityAction = parent.defaultPermissionEntityAction;
                defaultAssociatedContentId = parent.defaultAssociatedContentId;
                defaultPermissionStatusId = parent.defaultPermissionStatusId;
                defaultPrivilegeEnumId = parent.defaultPrivilegeEnumId;
                defaultHideIfSelected = parent.defaultHideIfSelected;
                orientation = parent.orientation;
                menuWidth = parent.menuWidth;
                defaultCellWidth = parent.defaultCellWidth;
                defaultDisabledTitleStyle = parent.defaultDisabledTitleStyle;
                defaultAlign = parent.defaultAlign;
                defaultAlignStyle = parent.defaultAlignStyle;
                fillStyle = parent.fillStyle;
                extraIndex = parent.extraIndex;
                selectedMenuItemContextFieldName = parent.selectedMenuItemContextFieldName;
                menuContainerStyleExdr = parent.menuContainerStyleExdr;
                if (parent.actions != null) {
                    actions.addAll(parent.actions);
                }
            }
        }
        if (menuElement.hasAttribute("type"))
            type = menuElement.getAttribute("type");
        if (menuElement.hasAttribute("target"))
            target = menuElement.getAttribute("target");
        if (menuElement.hasAttribute("id"))
            id = menuElement.getAttribute("id");
        if (menuElement.hasAttribute("title"))
            title = FlexibleStringExpander.getInstance(menuElement.getAttribute("title"));
        if (menuElement.hasAttribute("tooltip"))
            tooltip = menuElement.getAttribute("tooltip");
        if (menuElement.hasAttribute("default-entity-name"))
            defaultEntityName = menuElement.getAttribute("default-entity-name");
        if (menuElement.hasAttribute("default-title-style"))
            defaultTitleStyle = menuElement.getAttribute("default-title-style");
        if (menuElement.hasAttribute("default-selected-style"))
            defaultSelectedStyle = menuElement.getAttribute("default-selected-style");
        if (menuElement.hasAttribute("default-widget-style"))
            defaultWidgetStyle = menuElement.getAttribute("default-widget-style");
        if (menuElement.hasAttribute("default-tooltip-style"))
            defaultTooltipStyle = menuElement.getAttribute("default-tooltip-style");
        if (menuElement.hasAttribute("default-menu-item-name"))
            defaultMenuItemName = menuElement.getAttribute("default-menu-item-name");
        if (menuElement.hasAttribute("default-permission-operation"))
            defaultPermissionOperation = menuElement.getAttribute("default-permission-operation");
        if (menuElement.hasAttribute("default-permission-entity-action"))
            defaultPermissionEntityAction = menuElement.getAttribute("default-permission-entity-action");
        if (menuElement.hasAttribute("defaultPermissionStatusId"))
            defaultPermissionStatusId = menuElement.getAttribute("default-permission-status-id");
        if (menuElement.hasAttribute("defaultPrivilegeEnumId"))
            defaultPrivilegeEnumId = menuElement.getAttribute("default-privilege-enum-id");
        if (menuElement.hasAttribute("defaultAssociatedContentId"))
            defaultAssociatedContentId = FlexibleStringExpander.getInstance(menuElement
                    .getAttribute("default-associated-content-id"));
        if (menuElement.hasAttribute("orientation"))
            orientation = menuElement.getAttribute("orientation");
        if (menuElement.hasAttribute("menu-width"))
            menuWidth = menuElement.getAttribute("menu-width");
        if (menuElement.hasAttribute("default-cell-width"))
            defaultCellWidth = menuElement.getAttribute("default-cell-width");
        if (menuElement.hasAttribute("default-hide-if-selected"))
            defaultHideIfSelected = "true".equals(menuElement.getAttribute("default-hide-if-selected"));
        if (menuElement.hasAttribute("default-disabled-title-style"))
            defaultDisabledTitleStyle = menuElement.getAttribute("default-disabled-title-style");
        if (menuElement.hasAttribute("selected-menuitem-context-field-name"))
            selectedMenuItemContextFieldName = FlexibleMapAccessor.getInstance(menuElement
                    .getAttribute("selected-menuitem-context-field-name"));
        if (menuElement.hasAttribute("menu-container-style"))
            menuContainerStyleExdr = FlexibleStringExpander.getInstance(menuElement.getAttribute("menu-container-style"));
        if (menuElement.hasAttribute("default-align"))
            defaultAlign = menuElement.getAttribute("default-align");
        if (menuElement.hasAttribute("default-align-style"))
            defaultAlignStyle = menuElement.getAttribute("default-align-style");
        if (menuElement.hasAttribute("fill-style"))
            fillStyle = menuElement.getAttribute("fill-style");
        if (menuElement.hasAttribute("extra-index"))
            extraIndex = FlexibleStringExpander.getInstance(menuElement.getAttribute("extra-index"));
        // read all actions under the "actions" element
        Element actionsElement = UtilXml.firstChildElement(menuElement, "actions");
        if (actionsElement != null) {
            actions.addAll(ModelMenuAction.readSubActions(this, actionsElement));
        }
        actions.trimToSize();
        this.actions = Collections.unmodifiableList(actions);
        this.defaultAlign = defaultAlign;
        this.defaultAlignStyle = defaultAlignStyle;
        this.defaultAssociatedContentId = defaultAssociatedContentId;
        this.defaultCellWidth = defaultCellWidth;
        this.defaultDisabledTitleStyle = defaultDisabledTitleStyle;
        this.defaultEntityName = defaultEntityName;
        this.defaultHideIfSelected = defaultHideIfSelected;
        this.defaultMenuItemName = defaultMenuItemName;
        this.defaultPermissionEntityAction = defaultPermissionEntityAction;
        this.defaultPermissionOperation = defaultPermissionOperation;
        this.defaultPermissionStatusId = defaultPermissionStatusId;
        this.defaultPrivilegeEnumId = defaultPrivilegeEnumId;
        this.defaultSelectedStyle = defaultSelectedStyle;
        this.defaultTitleStyle = defaultTitleStyle;
        this.defaultTooltipStyle = defaultTooltipStyle;
        this.defaultWidgetStyle = defaultWidgetStyle;
        this.extraIndex = extraIndex;
        this.fillStyle = fillStyle;
        this.id = id;
        this.menuContainerStyleExdr = menuContainerStyleExdr;
        List<? extends Element> itemElements = UtilXml.childElementList(menuElement, "menu-item");
        for (Element itemElement : itemElements) {
            ModelMenuItem modelMenuItem = new ModelMenuItem(itemElement, this);
            addUpdateMenuItem(modelMenuItem, menuItemList, menuItemMap);
        }
        menuItemList.trimToSize();
        this.menuItemList = Collections.unmodifiableList(menuItemList);
        this.menuItemMap = Collections.unmodifiableMap(menuItemMap);
        this.menuLocation = menuLocation;
        this.menuWidth = menuWidth;
        this.orientation = orientation;
        this.selectedMenuItemContextFieldName = selectedMenuItemContextFieldName;
        this.target = target;
        this.title = title;
        this.tooltip = tooltip;
        this.type = type;
    }

    @Override
    public void accept(ModelWidgetVisitor visitor) throws Exception {
        visitor.visit(this);
    }

    /**
     * add/override modelMenuItem using the menuItemList and menuItemMap
     *
     */
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
            // does not exist, add to Map
            menuItemList.add(modelMenuItem);
            menuItemMap.put(modelMenuItem.getName(), modelMenuItem);
        }
    }

    public List<ModelWidgetAction> getActions() {
        return actions;
    }

    @Override
    public String getBoundaryCommentName() {
        return menuLocation + "#" + getName();
    }

    public String getCurrentMenuName(Map<String, Object> context) {
        return getName();
    }

    public String getDefaultAlign() {
        return this.defaultAlign;
    }

    public String getDefaultAlignStyle() {
        return this.defaultAlignStyle;
    }

    public FlexibleStringExpander getDefaultAssociatedContentId() {
        return defaultAssociatedContentId;
    }

    public String getDefaultAssociatedContentId(Map<String, Object> context) {
        return defaultAssociatedContentId.expandString(context);
    }

    public String getDefaultCellWidth() {
        return this.defaultCellWidth;
    }

    public String getDefaultDisabledTitleStyle() {
        return this.defaultDisabledTitleStyle;
    }

    public String getDefaultEntityName() {
        return this.defaultEntityName;
    }

    public Boolean getDefaultHideIfSelected() {
        return this.defaultHideIfSelected;
    }

    public String getDefaultMenuItemName() {
        return this.defaultMenuItemName;
    }

    public String getDefaultPermissionEntityAction() {
        return this.defaultPermissionEntityAction;
    }

    public String getDefaultPermissionOperation() {
        return this.defaultPermissionOperation;
    }

    public String getDefaultPermissionStatusId() {
        return this.defaultPermissionStatusId;
    }

    public String getDefaultPrivilegeEnumId() {
        return this.defaultPrivilegeEnumId;
    }

    public String getDefaultSelectedStyle() {
        return this.defaultSelectedStyle;
    }

    public String getDefaultTitleStyle() {
        return this.defaultTitleStyle;
    }

    public String getDefaultTooltipStyle() {
        return this.defaultTooltipStyle;
    }

    public String getDefaultWidgetStyle() {
        return this.defaultWidgetStyle;
    }

    public FlexibleStringExpander getExtraIndex() {
        return extraIndex;
    }

    public String getExtraIndex(Map<String, Object> context) {
        try {
            return extraIndex.expandString(context);
        } catch (Exception ex) {
            return "";
        }
    }

    public String getFillStyle() {
        return this.fillStyle;
    }

    public String getId() {
        return this.id;
    }

    public String getMenuContainerStyle(Map<String, Object> context) {
        return menuContainerStyleExdr.expandString(context);
    }

    public FlexibleStringExpander getMenuContainerStyleExdr() {
        return menuContainerStyleExdr;
    }

    public List<ModelMenuItem> getMenuItemList() {
        return menuItemList;
    }

    public Map<String, ModelMenuItem> getMenuItemMap() {
        return menuItemMap;
    }

    public String getMenuLocation() {
        return menuLocation;
    }

    public String getMenuWidth() {
        return this.menuWidth;
    }

    public ModelMenuItem getModelMenuItemByName(String name) {
        return this.menuItemMap.get(name);
    }

    public String getOrientation() {
        return this.orientation;
    }

    public FlexibleMapAccessor<String> getSelectedMenuItemContextFieldName() {
        return selectedMenuItemContextFieldName;
    }

    public String getSelectedMenuItemContextFieldName(Map<String, Object> context) {
        String menuItemName = this.selectedMenuItemContextFieldName.get(context);
        if (UtilValidate.isEmpty(menuItemName)) {
            return this.defaultMenuItemName;
        }
        return menuItemName;
    }

    public String getTarget() {
        return target;
    }

    public FlexibleStringExpander getTitle() {
        return title;
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

    public int renderedMenuItemCount(Map<String, Object> context) {
        int count = 0;
        for (ModelMenuItem item : this.menuItemList) {
            if (item.shouldBeRendered(context))
                count++;
        }
        return count;
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
    public void renderMenuString(Appendable writer, Map<String, Object> context, MenuStringRenderer menuStringRenderer)
            throws IOException {
        ModelWidgetAction.runSubActions(this.actions, context);
        if ("simple".equals(this.type)) {
            this.renderSimpleMenuString(writer, context, menuStringRenderer);
        } else {
            throw new IllegalArgumentException("The type " + this.getType() + " is not supported for menu with name "
                    + this.getName());
        }
    }

    public void renderSimpleMenuString(Appendable writer, Map<String, Object> context, MenuStringRenderer menuStringRenderer)
            throws IOException {
        // render menu open
        menuStringRenderer.renderMenuOpen(writer, context, this);

        // render formatting wrapper open
        menuStringRenderer.renderFormatSimpleWrapperOpen(writer, context, this);

        // render each menuItem row, except hidden & ignored rows
        for (ModelMenuItem item : this.menuItemList) {
            item.renderMenuItemString(writer, context, menuStringRenderer);
        }
        // render formatting wrapper close
        menuStringRenderer.renderFormatSimpleWrapperClose(writer, context, this);

        // render menu close
        menuStringRenderer.renderMenuClose(writer, context, this);
    }

    public void runActions(Map<String, Object> context) {
        ModelWidgetAction.runSubActions(this.actions, context);
    }
}
