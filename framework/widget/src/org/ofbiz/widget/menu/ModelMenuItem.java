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
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilFormatOut;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.UtilXml;
import org.ofbiz.base.util.string.FlexibleStringExpander;
import org.ofbiz.entityext.permission.EntityPermissionChecker;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 * Widget Library - Form model class
 */
public class ModelMenuItem {

    public static final String module = ModelMenuItem.class.getName();

    protected ModelMenu modelMenu;

    protected Map dataMap = new HashMap();
    protected String name;
    protected String entityName;
    protected FlexibleStringExpander title;
    protected FlexibleStringExpander tooltip;
    protected String titleStyle;
    protected String disabledTitleStyle;
    protected String widgetStyle;
    protected String tooltipStyle;
    protected String selectedStyle;
    protected Integer position = null;

    protected FlexibleStringExpander associatedContentId;
    protected String cellWidth;
    protected Boolean hideIfSelected;
    protected Boolean hasPermission;
    protected String disableIfEmpty;
    protected ModelMenu subMenu;
    protected Link link;
    
    protected List menuItemList = new LinkedList();
    protected Map menuItemMap = new HashMap();


    public static String DEFAULT_TARGET_TYPE = "intra-app";
    
    protected EntityPermissionChecker permissionChecker;
    protected ModelMenuItem parentMenuItem;
    protected ModelMenuCondition condition;
    protected boolean disabled = false;
    protected List actions;
    protected String align;
    protected String alignStyle;
    
    // ===== CONSTRUCTORS =====
    /** Default Constructor */
    public ModelMenuItem(ModelMenu modelMenu) {
        this.modelMenu = modelMenu;
    }

    /** XML Constructor */
    public ModelMenuItem(Element fieldElement, ModelMenuItem modelMenuItem) {
        parentMenuItem = modelMenuItem;
        loadMenuItem(fieldElement, modelMenuItem.getModelMenu());
    }
    

    public ModelMenuItem(Element fieldElement, ModelMenu modelMenu) {
        loadMenuItem(fieldElement, modelMenu);
    }
    
    public void loadMenuItem(Element fieldElement, ModelMenu modelMenu) {
        this.modelMenu = modelMenu;
        this.name = fieldElement.getAttribute("name");
        this.entityName = fieldElement.getAttribute("entity-name");
        this.setTitle(fieldElement.getAttribute("title"));
        this.setTooltip(fieldElement.getAttribute("tooltip"));
        this.titleStyle = fieldElement.getAttribute("title-style");
        this.disabledTitleStyle = fieldElement.getAttribute("disabled-title-style");
        this.widgetStyle = fieldElement.getAttribute("widget-style");
        this.tooltipStyle = fieldElement.getAttribute("tooltip-style");
        this.selectedStyle = fieldElement.getAttribute("selected-style");
        this.setHideIfSelected(fieldElement.getAttribute("hide-if-selected"));
        this.disableIfEmpty = fieldElement.getAttribute("disable-if-empty");
        this.align = fieldElement.getAttribute("align");
        this.alignStyle = fieldElement.getAttribute("align-style");

        String positionStr = fieldElement.getAttribute("position");
        try {
            if (positionStr != null && positionStr.length() > 0) {
                position = Integer.valueOf(positionStr);
            }
        } catch (Exception e) {
            Debug.logError(e, "Could not convert position attribute of the field element to an integer: [" +
                    positionStr + "], using the default of the menu renderer", module);
        }

        this.setAssociatedContentId( fieldElement.getAttribute("associated-content-id"));
        this.cellWidth = fieldElement.getAttribute("cell-width");

        dataMap.put("name", this.name);
        //dataMap.put("associatedContentId", this.associatedContentId);

        Element subMenuElement = UtilXml.firstChildElement(fieldElement, "sub-menu");
        if (subMenuElement != null) {
            String subMenuLocation = subMenuElement.getAttribute("location");
            String subMenuName = subMenuElement.getAttribute("name");
            try {
                this.subMenu = MenuFactory.getMenuFromLocation(subMenuLocation, subMenuName, modelMenu.getDelegator(), modelMenu.getDispacher());
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

        Element linkElement = UtilXml.firstChildElement(fieldElement, "link");
        //if (Debug.infoOn()) Debug.logInfo("in ModelMenuItem, linkElement:" + linkElement, module);
        if (linkElement != null) {
            link = new Link(linkElement, this);
        }
        
//        Element permissionElement = UtilXml.firstChildElement(fieldElement, "if-entity-permission");
//        if (permissionElement != null)
//            permissionChecker = new EntityPermissionChecker(permissionElement);

        // read in add item defs, add/override one by one using the menuItemList and menuItemMap
        List itemElements = UtilXml.childElementList(fieldElement, "menu-item");
        Iterator itemElementIter = itemElements.iterator();
        while (itemElementIter.hasNext()) {
            Element itemElement = (Element) itemElementIter.next();
            ModelMenuItem modelMenuItem = new ModelMenuItem(itemElement, this);
            modelMenuItem = this.addUpdateMenuItem(modelMenuItem);
            //Debug.logInfo("Added item " + modelMenuItem.getName() + " from def, mapName=" + modelMenuItem.getMapName(), module);
        }
        // read condition under the "condition" element
        Element conditionElement = UtilXml.firstChildElement(fieldElement, "condition");
        if (conditionElement != null) {
            this.condition = new ModelMenuCondition(this, conditionElement);
        }
        // read all actions under the "actions" element
        Element actionsElement = UtilXml.firstChildElement(conditionElement, "actions");
        if (actionsElement != null) {
            this.actions = ModelMenuAction.readSubActions(this, actionsElement);
        }

    }
    
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

    public void mergeOverrideModelMenuItem(ModelMenuItem overrideModelMenuItem) {
        if (overrideModelMenuItem == null)
            return;
/*
        // incorporate updates for values that are not empty in the overrideMenuItem
        if (UtilValidate.isNotEmpty(overrideMenuItem.name))
            this.name = overrideMenuItem.name;
        if (UtilValidate.isNotEmpty(overrideMenuItem.entityName))
            this.entityName = overrideMenuItem.entityName;
        if (overrideMenuItem.entryAcsr != null && !overrideMenuItem.entryAcsr.isEmpty())
            this.entryAcsr = overrideMenuItem.entryAcsr;
        if (UtilValidate.isNotEmpty(overrideMenuItem.attributeName))
            this.attributeName = overrideMenuItem.attributeName;
        if (overrideMenuItem.title != null && !overrideMenuItem.title.isEmpty())
            this.title = overrideMenuItem.title;
        if (overrideMenuItem.tooltip != null && !overrideMenuItem.tooltip.isEmpty())
            this.tooltip = overrideMenuItem.tooltip;
        if (UtilValidate.isNotEmpty(overrideMenuItem.titleStyle))
            this.titleStyle = overrideMenuItem.titleStyle;
        if (UtilValidate.isNotEmpty(overrideMenuItem.selectedStyle))
            this.selectedStyle = overrideMenuItem.selectedStyle;
        if (UtilValidate.isNotEmpty(overrideMenuItem.widgetStyle))
            this.widgetStyle = overrideMenuItem.widgetStyle;
        if (overrideMenuItem.position != null)
            this.position = overrideMenuItem.position;
*/
    }

    public void renderMenuItemString(StringBuffer buffer, Map context, MenuStringRenderer menuStringRenderer) {
        
      	boolean passed = true;
        if (this.condition != null) {
            if (!this.condition.eval(context)) {
                passed = false;
            }
        }
           //Debug.logInfo("in ModelMenu, name:" + this.getName(), module);
        if (passed) {
            ModelMenuAction.runSubActions(this.actions, context);
            menuStringRenderer.renderMenuItem(buffer, context, this);
        }
    }


    /**
     * @return
     */
    public ModelMenu getModelMenu() {
        return modelMenu;
    }


    /**
     * @return
     */
    public String getEntityName() {
        if (UtilValidate.isNotEmpty(this.entityName)) {
            return this.entityName;
        } else if (parentMenuItem != null) {
            return parentMenuItem.getEntityName();
        } else {
            return this.modelMenu.getDefaultEntityName();
        }
    }

    /**
     * @return
     */
    public String getAlign() {
        if (UtilValidate.isNotEmpty(this.align)) {
            return this.align;
        } else if (parentMenuItem != null) {
            return parentMenuItem.getAlign();
        } else {
            return this.modelMenu.getDefaultAlign();
        }
    }


    /**
     * @return
     */
    public String getName() {
        return name;
    }

    /**
     * @return
     */
    public int getPosition() {
        if (this.position == null) {
            return 1;
        } else {
            return position.intValue();
        }
    }

    /**
     * @return
     */
    public String getTitle(Map context) {
            return title.expandString(context);
    }

    /**
     * @return
     */
    public String getTitleStyle() {
        if (UtilValidate.isNotEmpty(this.titleStyle)) {
            return this.titleStyle;
        } else if (parentMenuItem != null) {
            return parentMenuItem.getTitleStyle();
         } else {
            return this.modelMenu.getDefaultTitleStyle();
        }
    }

    /**
     * @return
     */
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

    /**
     * @return
     */
    public String getSelectedStyle() {
        if (UtilValidate.isNotEmpty(this.selectedStyle)) {
            return this.selectedStyle;
        } else if (parentMenuItem != null) {
            return parentMenuItem.getSelectedStyle();
        } else {
            return this.modelMenu.getDefaultSelectedStyle();
        }
    }

    /**
     * @return
     */
    public String getTooltip(Map context) {
        if (tooltip != null && !tooltip.isEmpty()) {
            return tooltip.expandString(context);
        } else {
            return "";
        }
    }


    /**
     * @return
     */
    public String getWidgetStyle() {
        if (UtilValidate.isNotEmpty(this.widgetStyle)) {
            return this.widgetStyle;
        } else if (parentMenuItem != null) {
            return parentMenuItem.getWidgetStyle();
        } else {
            return this.modelMenu.getDefaultWidgetStyle();
        }
    }

    /**
     * @return
     */
    public String getAlignStyle() {
        if (UtilValidate.isNotEmpty(this.alignStyle)) {
            return this.alignStyle;
        } else if (parentMenuItem != null) {
            return parentMenuItem.getAlignStyle();
        } else {
            return this.modelMenu.getDefaultAlignStyle();
        }
    }

    /**
     * @return
     */
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
     * @param string
     */
    public void setName(String string) {
        name = string;
    }

    /**
     * @param i
     */
    public void setPosition(int i) {
        position = new Integer(i);
    }


    /**
     * @param string
     */
    public void setTitle(String string) {
        this.title = new FlexibleStringExpander(string);
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
        this.tooltip = new FlexibleStringExpander(string);
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
        this.associatedContentId = new FlexibleStringExpander(string);
    }

    /**
     * @return
     */
    public String getAssociatedContentId(Map context) {
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

    /**
     * @return
     */
    public String getCellWidth() {
        if (UtilValidate.isNotEmpty(this.cellWidth )) {
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

    /**
     * @return
     */
    public Boolean getHideIfSelected() {
        if (hideIfSelected != null) {
            return this.hideIfSelected;
        } else {
            return this.modelMenu.getDefaultHideIfSelected();
        }
    }

    /**
     * @return
     */
    public String getDisableIfEmpty() {
            return this.disableIfEmpty;
    }

    /**
     * @param val
     */
    public void setHasPermission(Boolean val) {
        this.hasPermission = val;
    }

    /**
     * @return
     */
    public Boolean getHasPermission() {
        return this.hasPermission;
    }

    public void dump(StringBuffer buffer ) {
        buffer.append("ModelMenuItem:" + "\n     title=")
                .append(this.title).append("\n     name=")
                .append(this.name).append("\n     entityName=")
                .append(this.entityName).append("\n     titleStyle=")
                .append(this.titleStyle).append("\n     widgetStyle=")
                .append(this.widgetStyle).append("\n     tooltipStyle=")
                .append(this.tooltipStyle).append("\n     selectedStyle=")
                .append(this.selectedStyle)
                .append("\n\n");
    }

    public Link getLink() {
       return this.link;
    }
    
    public boolean isSelected(Map context) {
        String currentMenuItemName = modelMenu.getSelectedMenuItemContextFieldName(context);
        return currentMenuItemName != null && currentMenuItemName.equals(this.name);
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

        }

        public void renderLinkString(StringBuffer buffer, Map context, MenuStringRenderer menuStringRenderer) {
            menuStringRenderer.renderLink(buffer, context, this);
        }

        public String getText(Map context) {
            String txt = this.textExdr.expandString(context);
            if (UtilValidate.isEmpty(txt))
                txt = linkMenuItem.getTitle(context);
            return txt;
        }

        public String getId(Map context) {
            return this.idExdr.expandString(context);
        }

        public String getStyle(Map context) {
            String style = this.styleExdr.expandString(context);
            if (UtilValidate.isEmpty(style)) {
                style = this.linkMenuItem.getWidgetStyle();
            }
            return style;
        }

        public String getName(Map context) {
            return this.nameExdr.expandString(context);
        }

        public String getTarget(Map context) {
            return this.targetExdr.expandString(context);
        }

        public String getTargetWindow(Map context) {
            return this.targetWindowExdr.expandString(context);
        }

        public String getUrlMode() {
            return this.urlMode;
        }

        public String getPrefix(Map context) {
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

        public void setText(String val) {
            String textAttr = UtilFormatOut.checkNull(val);
            this.textExdr = new FlexibleStringExpander(textAttr);
        }

        public void setId(String val) {
            this.idExdr = new FlexibleStringExpander(val);
        }

        public void setStyle(String val) {
            this.styleExdr = new FlexibleStringExpander(val);
        }

        public void setTarget(String val) {
            this.targetExdr = new FlexibleStringExpander(val);
        }

        public void setTargetWindow(String val) {
            this.targetWindowExdr = new FlexibleStringExpander(val);
        }

        public void setPrefix(String val) {
            this.prefixExdr = new FlexibleStringExpander(val);
        }

        public void setUrlMode(String val) {
            if (UtilValidate.isNotEmpty(val))
                this.urlMode = val;
        }

        public void setName(String val) {
            this.nameExdr = new FlexibleStringExpander(val);
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

        public void renderImageString(StringBuffer buffer, Map context, MenuStringRenderer menuStringRenderer) {
            menuStringRenderer.renderImage(buffer, context, this);
        }

        public String getSrc(Map context) {
            return this.srcExdr.expandString(context);
        }

        public String getId(Map context) {
            return this.idExdr.expandString(context);
        }

        public String getStyle(Map context) {
            return this.styleExdr.expandString(context);
        }

        public String getWidth(Map context) {
            return this.widthExdr.expandString(context);
        }

        public String getHeight(Map context) {
            return this.heightExdr.expandString(context);
        }

        public String getBorder(Map context) {
            return this.borderExdr.expandString(context);
        }

        public String getUrlMode() {
            return this.urlMode;
        }

        public void setSrc(String val) {
            String textAttr = UtilFormatOut.checkNull(val);
            this.srcExdr = new FlexibleStringExpander(textAttr);
        }

        public void setId(String val) {
            this.idExdr = new FlexibleStringExpander(val);
        }

        public void setStyle(String val) {
            this.styleExdr = new FlexibleStringExpander(val);
        }

        public void setWidth(String val) {
            this.widthExdr = new FlexibleStringExpander(val);
        }

        public void setHeight(String val) {
            this.heightExdr = new FlexibleStringExpander(val);
        }

        public void setBorder(String val) {
            this.borderExdr = new FlexibleStringExpander(val);
        }

        public void setUrlMode(String val) {
            if (UtilValidate.isEmpty(val))
                this.urlMode = "content";
            else
                this.urlMode = val;
        }

    }
}
