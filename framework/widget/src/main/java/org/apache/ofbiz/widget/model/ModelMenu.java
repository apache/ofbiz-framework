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
import java.util.Map;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.UtilXml;
import org.apache.ofbiz.base.util.collections.FlexibleMapAccessor;
import org.apache.ofbiz.base.util.string.FlexibleStringExpander;
import org.apache.ofbiz.widget.renderer.MenuStringRenderer;
import org.apache.ofbiz.widget.renderer.VisualTheme;
import org.w3c.dom.Element;

/**
 * Models the &lt;menu&gt; element. see widget-menu.xsd
 */
@SuppressWarnings("serial")
public class ModelMenu extends ModelWidget {

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

    private static final String MODULE = ModelMenu.class.getName();

    private final List<ModelAction> actions;
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
    private final ModelMenu parentMenu;
    private final FlexibleMapAccessor<String> selectedMenuItemContextFieldName;
    private final String target;
    private final FlexibleStringExpander title;
    private final String tooltip;
    private final String type;

    /** XML Constructor */
    public ModelMenu(Element menuElement, String menuLocation, VisualTheme visualTheme) {
        super(menuElement);
        ArrayList<ModelAction> actions = new ArrayList<>();
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
        String defaultSelectedStyle = "";
        String defaultTitleStyle = "";
        String defaultTooltipStyle = "";
        String defaultWidgetStyle = "";
        FlexibleStringExpander extraIndex = FlexibleStringExpander.getInstance("");
        String fillStyle = "";
        String id = "";
        FlexibleStringExpander menuContainerStyleExdr = FlexibleStringExpander.getInstance("");
        ArrayList<ModelMenuItem> menuItemList = new ArrayList<>();
        Map<String, ModelMenuItem> menuItemMap = new HashMap<>();
        String menuWidth = "";
        String orientation = "horizontal";
        FlexibleMapAccessor<String> selectedMenuItemContextFieldName = FlexibleMapAccessor.getInstance("");
        String target = "";
        FlexibleStringExpander title = FlexibleStringExpander.getInstance("");
        String tooltip = "";
        String type = "";
        // check if there is a parent menu to inherit from
        ModelMenu parent = null;
        String parentResource = menuElement.getAttribute("extends-resource");
        String parentMenu = menuElement.getAttribute("extends");
        if (!parentMenu.isEmpty()) {
            if (!parentResource.isEmpty()) {
                try {
                    FlexibleStringExpander parentResourceExp = FlexibleStringExpander.getInstance(parentResource);
                    Map<String, String> visualRessources = visualTheme.getModelTheme().getModelCommonMenus();
                    parentResource = parentResourceExp.expandString(visualRessources);
                    parent = MenuFactory.getMenuFromLocation(parentResource, parentMenu, visualTheme);
                } catch (Exception e) {
                    Debug.logError(e, "Failed to load parent menu definition '" + parentMenu + "' at resource '" + parentResource, MODULE);
                }
            } else {
                parentResource = menuLocation;
                // try to find a menu definition in the same file
                Element rootElement = menuElement.getOwnerDocument().getDocumentElement();
                List<? extends Element> menuElements = UtilXml.childElementList(rootElement, "menu");
                Element parentEntry = menuElements.stream()
                        .filter(elem -> elem.getAttribute("name").equals(parentMenu))
                        .findFirst()
                        .orElse(null);
                if (parentEntry != null) {
                    parent = new ModelMenu(parentEntry, parentResource, visualTheme);
                }

                if (parent == null) {
                    Debug.logError("Failed to find parent menu definition '" + parentMenu + "' in same document.", MODULE);
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
        if (!menuElement.getAttribute("type").isEmpty()) {
            type = menuElement.getAttribute("type");
        }
        if (!menuElement.getAttribute("target").isEmpty()) {
            target = menuElement.getAttribute("target");
        }
        if (!menuElement.getAttribute("id").isEmpty()) {
            id = menuElement.getAttribute("id");
        }
        if (!menuElement.getAttribute("title").isEmpty()) {
            title = FlexibleStringExpander.getInstance(menuElement.getAttribute("title"));
        }
        if (!menuElement.getAttribute("tooltip").isEmpty()) {
            tooltip = menuElement.getAttribute("tooltip");
        }
        if (!menuElement.getAttribute("default-entity-name").isEmpty()) {
            defaultEntityName = menuElement.getAttribute("default-entity-name");
        }
        if (!menuElement.getAttribute("default-title-style").isEmpty()) {
            defaultTitleStyle = menuElement.getAttribute("default-title-style");
        }
        if (!menuElement.getAttribute("default-selected-style").isEmpty()) {
            defaultSelectedStyle = menuElement.getAttribute("default-selected-style");
        }
        if (!menuElement.getAttribute("default-widget-style").isEmpty()) {
            defaultWidgetStyle = menuElement.getAttribute("default-widget-style");
        }
        if (!menuElement.getAttribute("default-tooltip-style").isEmpty()) {
            defaultTooltipStyle = menuElement.getAttribute("default-tooltip-style");
        }
        if (!menuElement.getAttribute("default-menu-item-name").isEmpty()) {
            defaultMenuItemName = menuElement.getAttribute("default-menu-item-name");
        }
        if (!menuElement.getAttribute("default-permission-operation").isEmpty()) {
            defaultPermissionOperation = menuElement.getAttribute("default-permission-operation");
        }
        if (!menuElement.getAttribute("default-permission-entity-action").isEmpty()) {
            defaultPermissionEntityAction = menuElement.getAttribute("default-permission-entity-action");
        }
        if (!menuElement.getAttribute("default-associated-content-id").isEmpty()) {
            defaultAssociatedContentId = FlexibleStringExpander.getInstance(menuElement
                    .getAttribute("default-associated-content-id"));
        }
        if (!menuElement.getAttribute("orientation").isEmpty()) {
            orientation = menuElement.getAttribute("orientation");
        }
        if (!menuElement.getAttribute("menu-width").isEmpty()) {
            menuWidth = menuElement.getAttribute("menu-width");
        }
        if (!menuElement.getAttribute("default-cell-width").isEmpty()) {
            defaultCellWidth = menuElement.getAttribute("default-cell-width");
        }
        if (!menuElement.getAttribute("default-hide-if-selected").isEmpty()) {
            defaultHideIfSelected = "true".equals(menuElement.getAttribute("default-hide-if-selected"));
        }
        if (!menuElement.getAttribute("default-disabled-title-style").isEmpty()) {
            defaultDisabledTitleStyle = menuElement.getAttribute("default-disabled-title-style");
        }
        if (!menuElement.getAttribute("selected-menuitem-context-field-name").isEmpty()) {
            selectedMenuItemContextFieldName = FlexibleMapAccessor.getInstance(menuElement
                    .getAttribute("selected-menuitem-context-field-name"));
        }
        if (!menuElement.getAttribute("menu-container-style").isEmpty()) {
            menuContainerStyleExdr = FlexibleStringExpander.getInstance(menuElement.getAttribute("menu-container-style"));
        }
        if (!menuElement.getAttribute("default-align").isEmpty()) {
            defaultAlign = menuElement.getAttribute("default-align");
        }
        if (!menuElement.getAttribute("default-align-style").isEmpty()) {
            defaultAlignStyle = menuElement.getAttribute("default-align-style");
        }
        if (!menuElement.getAttribute("fill-style").isEmpty()) {
            fillStyle = menuElement.getAttribute("fill-style");
        }
        if (!menuElement.getAttribute("extra-index").isEmpty()) {
            extraIndex = FlexibleStringExpander.getInstance(menuElement.getAttribute("extra-index"));
        }
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
        this.parentMenu = parent;
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
     */
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
            // does not exist, add to Map
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

    @Override
    public String getBoundaryCommentName() {
        return menuLocation + "#" + getName();
    }

    /**
     * Gets current menu name.
     * @param context the context
     * @return the current menu name
     */
    public String getCurrentMenuName(Map<String, Object> context) {
        return getName();
    }

    /**
     * Gets default align.
     * @return the default align
     */
    public String getDefaultAlign() {
        return this.defaultAlign;
    }

    /**
     * Gets default align style.
     * @return the default align style
     */
    public String getDefaultAlignStyle() {
        return this.defaultAlignStyle;
    }

    /**
     * Gets default associated content id.
     * @return the default associated content id
     */
    public FlexibleStringExpander getDefaultAssociatedContentId() {
        return defaultAssociatedContentId;
    }

    /**
     * Gets default associated content id.
     * @param context the context
     * @return the default associated content id
     */
    public String getDefaultAssociatedContentId(Map<String, Object> context) {
        return defaultAssociatedContentId.expandString(context);
    }

    /**
     * Gets default cell width.
     * @return the default cell width
     */
    public String getDefaultCellWidth() {
        return this.defaultCellWidth;
    }

    /**
     * Gets default disabled title style.
     * @return the default disabled title style
     */
    public String getDefaultDisabledTitleStyle() {
        return this.defaultDisabledTitleStyle;
    }

    /**
     * Gets default entity name.
     * @return the default entity name
     */
    public String getDefaultEntityName() {
        return this.defaultEntityName;
    }

    /**
     * Gets default hide if selected.
     * @return the default hide if selected
     */
    public Boolean getDefaultHideIfSelected() {
        return this.defaultHideIfSelected;
    }

    /**
     * Gets default menu item name.
     * @return the default menu item name
     */
    public String getDefaultMenuItemName() {
        return this.defaultMenuItemName;
    }

    /**
     * Gets default permission entity action.
     * @return the default permission entity action
     */
    public String getDefaultPermissionEntityAction() {
        return this.defaultPermissionEntityAction;
    }

    /**
     * Gets default permission operation.
     * @return the default permission operation
     */
    public String getDefaultPermissionOperation() {
        return this.defaultPermissionOperation;
    }

    /**
     * Gets default selected style.
     * @return the default selected style
     */
    public String getDefaultSelectedStyle() {
        return this.defaultSelectedStyle;
    }

    /**
     * Gets default title style.
     * @return the default title style
     */
    public String getDefaultTitleStyle() {
        return this.defaultTitleStyle;
    }

    /**
     * Gets default tooltip style.
     * @return the default tooltip style
     */
    public String getDefaultTooltipStyle() {
        return this.defaultTooltipStyle;
    }

    /**
     * Gets default widget style.
     * @return the default widget style
     */
    public String getDefaultWidgetStyle() {
        return this.defaultWidgetStyle;
    }

    /**
     * Gets extra index.
     * @return the extra index
     */
    public FlexibleStringExpander getExtraIndex() {
        return extraIndex;
    }

    /**
     * Gets extra index.
     * @param context the context
     * @return the extra index
     */
    public String getExtraIndex(Map<String, Object> context) {
        try {
            return extraIndex.expandString(context);
        } catch (Exception ex) {
            return "";
        }
    }

    /**
     * Gets fill style.
     * @return the fill style
     */
    public String getFillStyle() {
        return this.fillStyle;
    }

    /**
     * Gets id.
     * @return the id
     */
    public String getId() {
        return this.id;
    }

    /**
     * Gets menu container style.
     * @param context the context
     * @return the menu container style
     */
    public String getMenuContainerStyle(Map<String, Object> context) {
        return menuContainerStyleExdr.expandString(context);
    }

    /**
     * Gets menu container style exdr.
     * @return the menu container style exdr
     */
    public FlexibleStringExpander getMenuContainerStyleExdr() {
        return menuContainerStyleExdr;
    }

    /**
     * Gets menu item list.
     * @return the menu item list
     */
    public List<ModelMenuItem> getMenuItemList() {
        return menuItemList;
    }

    /**
     * Gets menu item map.
     * @return the menu item map
     */
    public Map<String, ModelMenuItem> getMenuItemMap() {
        return menuItemMap;
    }

    /**
     * Gets menu location.
     * @return the menu location
     */
    public String getMenuLocation() {
        return menuLocation;
    }

    /**
     * Gets menu width.
     * @return the menu width
     */
    public String getMenuWidth() {
        return this.menuWidth;
    }

    /**
     * Gets model menu item by name.
     * @param name the name
     * @return the model menu item by name
     */
    public ModelMenuItem getModelMenuItemByName(String name) {
        return this.menuItemMap.get(name);
    }

    /**
     * Gets orientation.
     * @return the orientation
     */
    public String getOrientation() {
        return this.orientation;
    }

    /**
     * Gets parent menu.
     * @return the parent menu
     */
    public ModelMenu getParentMenu() {
        return parentMenu;
    }

    /**
     * Gets selected menu item context field name.
     * @return the selected menu item context field name
     */
    public FlexibleMapAccessor<String> getSelectedMenuItemContextFieldName() {
        return selectedMenuItemContextFieldName;
    }

    /**
     * Gets selected menu item context field name.
     * @param context the context
     * @return the selected menu item context field name
     */
    public String getSelectedMenuItemContextFieldName(Map<String, Object> context) {
        String menuItemName = this.selectedMenuItemContextFieldName.get(context);
        if (UtilValidate.isEmpty(menuItemName)) {
            return this.defaultMenuItemName;
        }
        return menuItemName;
    }

    /**
     * Gets target.
     * @return the target
     */
    public String getTarget() {
        return target;
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
     * Gets tooltip.
     * @return the tooltip
     */
    public String getTooltip() {
        return this.tooltip;
    }

    /**
     * Gets type.
     * @return the type
     */
    public String getType() {
        return this.type;
    }

    /**
     * Rendered menu item count int.
     * @param context the context
     * @return the int
     */
    public int renderedMenuItemCount(Map<String, Object> context) {
        int count = 0;
        for (ModelMenuItem item : this.menuItemList) {
            if (item.shouldBeRendered(context)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Renders this menu to a String, i.e. in a text format, as defined with the MenuStringRenderer implementation.
     * @param writer The Writer that the menu text will be written to
     * @param context Map containing the menu context; the following are reserved words in this context: parameters (Map), isError (Boolean),
     * itemIndex (Integer, for lists only, otherwise null), menuName (String, optional alternate name for menu, defaults to the value of the name
     * attribute)
     * @param menuStringRenderer An implementation of the MenuStringRenderer interface that is responsible for the actual text generation for
     * different menu elements; implementing you own makes it possible to use the same menu definitions for many types of menu UIs
     * @throws IOException the io exception
     */
    public void renderMenuString(Appendable writer, Map<String, Object> context, MenuStringRenderer menuStringRenderer) throws IOException {
        AbstractModelAction.runSubActions(this.actions, context);
        if ("simple".equals(this.type)) {
            this.renderSimpleMenuString(writer, context, menuStringRenderer);
        } else {
            throw new IllegalArgumentException("The type " + this.getType() + " is not supported for menu with name "
                    + this.getName());
        }
    }

    /**
     * Render simple menu string.
     * @param writer the writer
     * @param context the context
     * @param menuStringRenderer the menu string renderer
     * @throws IOException the io exception
     */
    public void renderSimpleMenuString(Appendable writer, Map<String, Object> context, MenuStringRenderer menuStringRenderer) throws IOException {
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

    /**
     * Run actions.
     * @param context the context
     */
    public void runActions(Map<String, Object> context) {
        AbstractModelAction.runSubActions(this.actions, context);
    }
}
