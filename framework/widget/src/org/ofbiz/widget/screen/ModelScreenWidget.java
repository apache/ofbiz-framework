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
package org.ofbiz.widget.screen;

import java.io.IOException;
import java.io.Serializable;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.ParserConfigurationException;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilFormatOut;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.UtilXml;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.collections.MapStack;
import org.ofbiz.base.util.string.FlexibleStringExpander;
import org.ofbiz.widget.form.FormFactory;
import org.ofbiz.widget.form.FormStringRenderer;
import org.ofbiz.widget.form.ModelForm;
import org.ofbiz.widget.html.HtmlFormRenderer;
import org.ofbiz.widget.html.HtmlMenuRenderer;
import org.ofbiz.widget.html.HtmlTreeRenderer;
import org.ofbiz.widget.menu.MenuFactory;
import org.ofbiz.widget.menu.MenuStringRenderer;
import org.ofbiz.widget.menu.ModelMenu;
import org.ofbiz.widget.tree.ModelTree;
import org.ofbiz.widget.tree.TreeFactory;
import org.ofbiz.widget.tree.TreeStringRenderer;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.GenericEntityException;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 * Widget Library - Screen model class
 */
public abstract class ModelScreenWidget implements Serializable {
    public static final String module = ModelScreenWidget.class.getName();

    protected ModelScreen modelScreen;
    
    public ModelScreenWidget(ModelScreen modelScreen, Element widgetElement) {
        this.modelScreen = modelScreen;
        if (Debug.verboseOn()) Debug.logVerbose("Reading Screen sub-widget with name: " + widgetElement.getNodeName(), module);
    }
    
    public abstract void renderWidgetString(Writer writer, Map context, ScreenStringRenderer screenStringRenderer) throws GeneralException;

    public abstract String rawString();
    
    public static List readSubWidgets(ModelScreen modelScreen, List subElementList) {
        List subWidgets = new LinkedList();
        
        Iterator subElementIter = subElementList.iterator();
        while (subElementIter.hasNext()) {
            Element subElement = (Element) subElementIter.next();

            if ("section".equals(subElement.getNodeName())) {
                subWidgets.add(new Section(modelScreen, subElement));
            } else if ("container".equals(subElement.getNodeName())) {
                subWidgets.add(new Container(modelScreen, subElement));
            } else if ("include-screen".equals(subElement.getNodeName())) {
                subWidgets.add(new IncludeScreen(modelScreen, subElement));
            } else if ("decorator-screen".equals(subElement.getNodeName())) {
                subWidgets.add(new DecoratorScreen(modelScreen, subElement));
            } else if ("decorator-section-include".equals(subElement.getNodeName())) {
                subWidgets.add(new DecoratorSectionInclude(modelScreen, subElement));
            } else if ("label".equals(subElement.getNodeName())) {
                subWidgets.add(new Label(modelScreen, subElement));
            } else if ("include-form".equals(subElement.getNodeName())) {
                subWidgets.add(new Form(modelScreen, subElement));
            } else if ("include-menu".equals(subElement.getNodeName())) {
                subWidgets.add(new Menu(modelScreen, subElement));
            } else if ("include-tree".equals(subElement.getNodeName())) {
                subWidgets.add(new Tree(modelScreen, subElement));
            } else if ("content".equals(subElement.getNodeName())) {
                subWidgets.add(new Content(modelScreen, subElement));
            } else if ("sub-content".equals(subElement.getNodeName())) {
                subWidgets.add(new SubContent(modelScreen, subElement));
            } else if ("platform-specific".equals(subElement.getNodeName())) {
                subWidgets.add(new PlatformSpecific(modelScreen, subElement));
            } else if ("link".equals(subElement.getNodeName())) {
                subWidgets.add(new Link(modelScreen, subElement));
            } else if ("image".equals(subElement.getNodeName())) {
                subWidgets.add(new Image(modelScreen, subElement));
            } else if ("iterate-section".equals(subElement.getNodeName())) {
                subWidgets.add(new IterateSectionWidget(modelScreen, subElement));
            } else {
                throw new IllegalArgumentException("Found invalid screen widget element with name: " + subElement.getNodeName());
            }
        }
        
        return subWidgets;
    }
    
    public static void renderSubWidgetsString(List subWidgets, Writer writer, Map context, ScreenStringRenderer screenStringRenderer) throws GeneralException {
        if (subWidgets == null) {
            return;
        }
        Iterator subWidgetIter = subWidgets.iterator();
        while (subWidgetIter.hasNext()) {
            ModelScreenWidget subWidget = (ModelScreenWidget) subWidgetIter.next();
            if (Debug.verboseOn()) Debug.logVerbose("Rendering screen " + subWidget.modelScreen.name + " widget " + subWidget.getClass().getName(), module);

            Map parameters = (Map) context.get("parameters");
            boolean insertWidgetBoundaryComments = "true".equals(parameters==null?null:parameters.get("widgetVerbose"));
            StringBuffer widgetDescription = null;
            if (insertWidgetBoundaryComments) {
                widgetDescription = new StringBuffer(); 
                widgetDescription.append("Widget [screen:");
                widgetDescription.append(subWidget.modelScreen.name);
                widgetDescription.append("] ");
                widgetDescription.append(subWidget.rawString());
                
                try {
                    writer.write("<!-- === BEGIN ");
                    writer.write(widgetDescription.toString());
                    writer.write(" -->\n");
                } catch (IOException e) {
                    throw new GeneralException("Error adding verbose sub-widget HTML/XML comments:", e);
                }
            }
            
            // render the sub-widget itself
            subWidget.renderWidgetString(writer, context, screenStringRenderer);
            
            if (insertWidgetBoundaryComments) {
                try {
                    writer.write("\n<!-- === END   ");
                    writer.write(widgetDescription.toString());
                    writer.write(" -->\n");
                } catch (IOException e) {
                    throw new GeneralException("Error adding verbose sub-widget HTML/XML comments:", e);
                }
            }
        }
    }

    public static class SectionsRenderer {
        protected Map sectionMap;
        protected ScreenStringRenderer screenStringRenderer;
        protected Map context;
        protected Writer writer;
        
        public SectionsRenderer(Map sectionMap, Map context, Writer writer, ScreenStringRenderer screenStringRenderer) {
            this.sectionMap = sectionMap;
            this.context = context;
            this.writer = writer;
            this.screenStringRenderer = screenStringRenderer;
        }

        /** This is a lot like the ScreenRenderer class and returns an empty String so it can be used more easily with FreeMarker */
        public String render(String sectionName) throws GeneralException {
            ModelScreenWidget section = (ModelScreenWidget) this.sectionMap.get(sectionName);
            // if no section by that name, write nothing
            if (section != null) {
                section.renderWidgetString(this.writer, this.context, this.screenStringRenderer);
            }
            return "";
        }
    }

    public static class Section extends ModelScreenWidget {
        protected String name;
        protected ModelScreenCondition condition;
        protected List actions;
        protected List subWidgets;
        protected List failWidgets;
        
        public Section(ModelScreen modelScreen, Element sectionElement) {
            super(modelScreen, sectionElement);
            this.name = sectionElement.getAttribute("name");

            // read condition under the "condition" element
            Element conditionElement = UtilXml.firstChildElement(sectionElement, "condition");
            if (conditionElement != null) {
                this.condition = new ModelScreenCondition(modelScreen, conditionElement);
            }

            // read all actions under the "actions" element
            Element actionsElement = UtilXml.firstChildElement(sectionElement, "actions");
            if (actionsElement != null) {
                this.actions = ModelScreenAction.readSubActions(modelScreen, actionsElement);
            }
            
            // read sub-widgets
            Element widgetsElement = UtilXml.firstChildElement(sectionElement, "widgets");
            List subElementList = UtilXml.childElementList(widgetsElement);
            this.subWidgets = ModelScreenWidget.readSubWidgets(this.modelScreen, subElementList);

            // read fail-widgets
            Element failWidgetsElement = UtilXml.firstChildElement(sectionElement, "fail-widgets");
            if (failWidgetsElement != null) {
                List failElementList = UtilXml.childElementList(failWidgetsElement);
                this.failWidgets = ModelScreenWidget.readSubWidgets(this.modelScreen, failElementList);
            }
        }

        public void renderWidgetString(Writer writer, Map context, ScreenStringRenderer screenStringRenderer) throws GeneralException {
            // check the condition, if there is one
            boolean condTrue = true;
            if (this.condition != null) {
                if (!this.condition.eval(context)) {
                    condTrue = false;
                }
            }
            
            // if condition does not exist or evals to true run actions and render widgets, otherwise render fail-widgets
            if (condTrue) {
                // run the actions only if true
                ModelScreenAction.runSubActions(this.actions, context);
                
                try {
                    // section by definition do not themselves do anything, so this method will generally do nothing, but we'll call it anyway
                    screenStringRenderer.renderSectionBegin(writer, context, this);
                    
                    // render sub-widgets
                    renderSubWidgetsString(this.subWidgets, writer, context, screenStringRenderer);

                    screenStringRenderer.renderSectionEnd(writer, context, this);
                } catch (IOException e) {
                    String errMsg = "Error rendering widgets section [" + this.getName() + "] in screen named [" + this.modelScreen.getName() + "]: " + e.toString();
                    Debug.logError(e, errMsg, module);
                    throw new RuntimeException(errMsg);
                }
            } else {
                try {
                    // section by definition do not themselves do anything, so this method will generally do nothing, but we'll call it anyway
                    screenStringRenderer.renderSectionBegin(writer, context, this);
                    
                    // render sub-widgets
                    renderSubWidgetsString(this.failWidgets, writer, context, screenStringRenderer);

                    screenStringRenderer.renderSectionEnd(writer, context, this);
                } catch (IOException e) {
                    String errMsg = "Error rendering fail-widgets section [" + this.getName() + "] in screen named [" + this.modelScreen.getName() + "]: " + e.toString();
                    Debug.logError(e, errMsg, module);
                    throw new RuntimeException(errMsg);
                }
            }
            
        }
        
        public String getName() {
            return name;
        }

        public String rawString() {
            return "<section" + (UtilValidate.isNotEmpty(this.name)?" name=\"" + this.name + "\"":"") + ">";
        }
    }

    public static class Container extends ModelScreenWidget {
        protected FlexibleStringExpander idExdr;
        protected FlexibleStringExpander styleExdr;
        protected List subWidgets;
        
        public Container(ModelScreen modelScreen, Element containerElement) {
            super(modelScreen, containerElement);
            this.idExdr = new FlexibleStringExpander(containerElement.getAttribute("id"));
            this.styleExdr = new FlexibleStringExpander(containerElement.getAttribute("style"));
            
            // read sub-widgets
            List subElementList = UtilXml.childElementList(containerElement);
            this.subWidgets = ModelScreenWidget.readSubWidgets(this.modelScreen, subElementList);
        }

        public void renderWidgetString(Writer writer, Map context, ScreenStringRenderer screenStringRenderer) throws GeneralException {
            try {
                screenStringRenderer.renderContainerBegin(writer, context, this);
                
                // render sub-widgets
                renderSubWidgetsString(this.subWidgets, writer, context, screenStringRenderer);

                screenStringRenderer.renderContainerEnd(writer, context, this);
            } catch (IOException e) {
                String errMsg = "Error rendering container in screen named [" + this.modelScreen.getName() + "]: " + e.toString();
                Debug.logError(e, errMsg, module);
                throw new RuntimeException(errMsg);
            }
        }
        
        public String getId(Map context) {
            return this.idExdr.expandString(context);
        }
        
        public String getStyle(Map context) {
            return this.styleExdr.expandString(context);
        }

        public String rawString() {
            return "<container id=\"" + this.idExdr.getOriginal() + "\" style=\"" + this.styleExdr.getOriginal() + "\">";
        }
    }

    public static class IncludeScreen extends ModelScreenWidget {
        protected FlexibleStringExpander nameExdr;
        protected FlexibleStringExpander locationExdr;
        protected FlexibleStringExpander shareScopeExdr;
        
        public IncludeScreen(ModelScreen modelScreen, Element includeScreenElement) {
            super(modelScreen, includeScreenElement);
            this.nameExdr = new FlexibleStringExpander(includeScreenElement.getAttribute("name"));
            this.locationExdr = new FlexibleStringExpander(includeScreenElement.getAttribute("location"));
            this.shareScopeExdr = new FlexibleStringExpander(includeScreenElement.getAttribute("share-scope"));
        }

        public void renderWidgetString(Writer writer, Map context, ScreenStringRenderer screenStringRenderer) throws GeneralException {
            // if we are not sharing the scope, protect it using the MapStack
            boolean protectScope = !shareScope(context);
            if (protectScope) {
                if (!(context instanceof MapStack)) {
                    context = MapStack.create(context);
                }
                
                ((MapStack) context).push();
                
                // build the widgetpath
                List widgetTrail = (List) context.get("_WIDGETTRAIL_");
                if (widgetTrail == null) {
                    widgetTrail = new ArrayList();
                }
                
                String thisName = nameExdr.expandString(context);
                widgetTrail.add(thisName);
                context.put("_WIDGETTRAIL_", widgetTrail);
            }
            
            // dont need the renderer here, will just pass this on down to another screen call; screenStringRenderer.renderContainerBegin(writer, context, this);
            String name = this.getName(context);
            String location = this.getLocation(context);
            
            if (UtilValidate.isEmpty(name)) {
                if (Debug.infoOn()) Debug.logInfo("In the include-screen tag the screen name was empty, ignoring include; in screen [" + this.modelScreen.getName() + "]", module);
                return;
            }
            
            // check to see if the name is a composite name separated by a #, if so split it up and get it by the full loc#name
            if (ScreenFactory.isCombinedName(name)) {
                String combinedName = name;
                location = ScreenFactory.getResourceNameFromCombined(combinedName);
                name = ScreenFactory.getScreenNameFromCombined(combinedName);
            }
            
            ModelScreen modelScreen = null;
            if (UtilValidate.isNotEmpty(location)) {
                try {
                    modelScreen = ScreenFactory.getScreenFromLocation(location, name);
                } catch (IOException e) {
                    String errMsg = "Error rendering included screen named [" + name + "] at location [" + location + "]: " + e.toString();
                    Debug.logError(e, errMsg, module);
                    throw new RuntimeException(errMsg);
                } catch (SAXException e) {
                    String errMsg = "Error rendering included screen named [" + name + "] at location [" + location + "]: " + e.toString();
                    Debug.logError(e, errMsg, module);
                    throw new RuntimeException(errMsg);
                } catch (ParserConfigurationException e) {
                    String errMsg = "Error rendering included screen named [" + name + "] at location [" + location + "]: " + e.toString();
                    Debug.logError(e, errMsg, module);
                    throw new RuntimeException(errMsg);
                }
            } else {
                modelScreen = (ModelScreen) this.modelScreen.modelScreenMap.get(name);
                if (modelScreen == null) {
                    throw new IllegalArgumentException("Could not find screen with name [" + name + "] in the same file as the screen with name [" + this.modelScreen.getName() + "]");
                }
            }
            modelScreen.renderScreenString(writer, context, screenStringRenderer);

            if (protectScope) {
                ((MapStack) context).pop();
            }
        }
        
        public String getName(Map context) {
            return this.nameExdr.expandString(context);
        }
        
        public String getLocation(Map context) {
            return this.locationExdr.expandString(context);
        }
        
        public boolean shareScope(Map context) {
            String shareScopeString = this.shareScopeExdr.expandString(context);
            // defaults to false, so anything but true is false
            return "true".equals(shareScopeString);
        }

        public String rawString() {
            return "<include-screen name=\"" + this.nameExdr.getOriginal() + "\" location=\"" + this.locationExdr.getOriginal() + "\" share-scope=\"" + this.shareScopeExdr.getOriginal() + "\"/>";
        }
    }

    public static class DecoratorScreen extends ModelScreenWidget {
        protected FlexibleStringExpander nameExdr;
        protected FlexibleStringExpander locationExdr;
        protected Map sectionMap = new HashMap();
        
        public DecoratorScreen(ModelScreen modelScreen, Element decoratorScreenElement) {
            super(modelScreen, decoratorScreenElement);
            this.nameExdr = new FlexibleStringExpander(decoratorScreenElement.getAttribute("name"));
            this.locationExdr = new FlexibleStringExpander(decoratorScreenElement.getAttribute("location"));
            
            List decoratorSectionElementList = UtilXml.childElementList(decoratorScreenElement, "decorator-section");
            Iterator decoratorSectionElementIter = decoratorSectionElementList.iterator();
            while (decoratorSectionElementIter.hasNext()) {
                Element decoratorSectionElement = (Element) decoratorSectionElementIter.next();
                String name = decoratorSectionElement.getAttribute("name");
                this.sectionMap.put(name, new DecoratorSection(modelScreen, decoratorSectionElement));
            }
        }

        public void renderWidgetString(Writer writer, Map context, ScreenStringRenderer screenStringRenderer) throws GeneralException {
            // isolate the scope
            if (!(context instanceof MapStack)) {
                context = MapStack.create(context);
            }
            
            MapStack contextMs = (MapStack) context;

            // create a standAloneStack, basically a "save point" for this SectionsRenderer, and make a new "screens" object just for it so it is isolated and doesn't follow the stack down
            MapStack standAloneStack = contextMs.standAloneChildStack();
            standAloneStack.put("screens", new ScreenRenderer(writer, standAloneStack, screenStringRenderer));
            SectionsRenderer sections = new SectionsRenderer(this.sectionMap, standAloneStack, writer, screenStringRenderer);
            
            // put the sectionMap in the context, make sure it is in the sub-scope, ie after calling push on the MapStack
            contextMs.push();
            context.put("sections", sections);
            
            String name = this.getName(context);
            String location = this.getLocation(context);
            
            // check to see if the name is a composite name separated by a #, if so split it up and get it by the full loc#name
            if (ScreenFactory.isCombinedName(name)) {
                String combinedName = name;
                location = ScreenFactory.getResourceNameFromCombined(combinedName);
                name = ScreenFactory.getScreenNameFromCombined(combinedName);
            }
            
            ModelScreen modelScreen = null;
            if (UtilValidate.isNotEmpty(location)) {
                try {
                    modelScreen = ScreenFactory.getScreenFromLocation(location, name);
                } catch (IOException e) {
                    String errMsg = "Error rendering included screen named [" + name + "] at location [" + location + "]: " + e.toString();
                    Debug.logError(e, errMsg, module);
                    throw new RuntimeException(errMsg);
                } catch (SAXException e) {
                    String errMsg = "Error rendering included screen named [" + name + "] at location [" + location + "]: " + e.toString();
                    Debug.logError(e, errMsg, module);
                    throw new RuntimeException(errMsg);
                } catch (ParserConfigurationException e) {
                    String errMsg = "Error rendering included screen named [" + name + "] at location [" + location + "]: " + e.toString();
                    Debug.logError(e, errMsg, module);
                    throw new RuntimeException(errMsg);
                }
            } else {
                modelScreen = (ModelScreen) this.modelScreen.modelScreenMap.get(name);
                if (modelScreen == null) {
                    throw new IllegalArgumentException("Could not find screen with name [" + name + "] in the same file as the screen with name [" + this.modelScreen.getName() + "]");
                }
            }
            modelScreen.renderScreenString(writer, context, screenStringRenderer);

            contextMs.pop();
        }

        public String getName(Map context) {
            return this.nameExdr.expandString(context);
        }
        
        public String getLocation(Map context) {
            return this.locationExdr.expandString(context);
        }

        public String rawString() {
            return "<decorator-screen name=\"" + this.nameExdr.getOriginal() + "\" location=\"" + this.locationExdr.getOriginal() + "\"/>";
        }
    }

    public static class DecoratorSection extends ModelScreenWidget {
        protected String name;
        protected List subWidgets;
        
        public DecoratorSection(ModelScreen modelScreen, Element decoratorSectionElement) {
            super(modelScreen, decoratorSectionElement);
            this.name = decoratorSectionElement.getAttribute("name");
            // read sub-widgets
            List subElementList = UtilXml.childElementList(decoratorSectionElement);
            this.subWidgets = ModelScreenWidget.readSubWidgets(this.modelScreen, subElementList);
        }

        public void renderWidgetString(Writer writer, Map context, ScreenStringRenderer screenStringRenderer) throws GeneralException {
            // render sub-widgets
            renderSubWidgetsString(this.subWidgets, writer, context, screenStringRenderer);
        }

        public String rawString() {
            return "<decorator-section name=\"" + this.name + "\">";
        }
    }
    
    public static class DecoratorSectionInclude extends ModelScreenWidget {
        protected String name;
        
        public DecoratorSectionInclude(ModelScreen modelScreen, Element decoratorSectionElement) {
            super(modelScreen, decoratorSectionElement);
            this.name = decoratorSectionElement.getAttribute("name");
        }

        public void renderWidgetString(Writer writer, Map context, ScreenStringRenderer screenStringRenderer) throws GeneralException {
            Map preRenderedContent = (Map) context.get("preRenderedContent");
            if (preRenderedContent != null && preRenderedContent.containsKey(this.name)) {
                try {
                    writer.write((String) preRenderedContent.get(this.name));
                } catch (IOException e) {
                    String errMsg = "Error rendering pre-rendered content in screen named [" + this.modelScreen.getName() + "]: " + e.toString();
                    Debug.logError(e, errMsg, module);
                    throw new RuntimeException(errMsg);
                }
            } else {
                SectionsRenderer sections = (SectionsRenderer) context.get("sections");
                // for now if sections is null, just log a warning; may be permissible to make the screen for flexible
                if (sections == null) {
                    Debug.logWarning("In decorator-section-include could not find sections object in the context, not rendering section with name [" + this.name + "]", module);
                } else {
                    sections.render(this.name);
                }
            }
        }

        public String rawString() {
            return "<decorator-section-include name=\"" + this.name + "\">";
        }
    }
    
    public static class Label extends ModelScreenWidget {
        protected FlexibleStringExpander textExdr;
        
        protected FlexibleStringExpander idExdr;
        protected FlexibleStringExpander styleExdr;
        
        public Label(ModelScreen modelScreen, Element labelElement) {
            super(modelScreen, labelElement);

            // put the text attribute first, then the pcdata under the element, if both are there of course
            String textAttr = UtilFormatOut.checkNull(labelElement.getAttribute("text"));
            String pcdata = UtilFormatOut.checkNull(UtilXml.elementValue(labelElement));
            this.textExdr = new FlexibleStringExpander(textAttr + pcdata);

            this.idExdr = new FlexibleStringExpander(labelElement.getAttribute("id"));
            this.styleExdr = new FlexibleStringExpander(labelElement.getAttribute("style"));
        }

        public void renderWidgetString(Writer writer, Map context, ScreenStringRenderer screenStringRenderer) {
            try {
                screenStringRenderer.renderLabel(writer, context, this);
            } catch (IOException e) {
                String errMsg = "Error rendering label in screen named [" + this.modelScreen.getName() + "]: " + e.toString();
                Debug.logError(e, errMsg, module);
                throw new RuntimeException(errMsg);
            }
        }
        
        public String getText(Map context) {
            return this.textExdr.expandString(context);
        }
        
        public String getId(Map context) {
            return this.idExdr.expandString(context);
        }
        
        public String getStyle(Map context) {
            return this.styleExdr.expandString(context);
        }
        
        public String rawString() {
            return "<label id=\"" + this.idExdr.getOriginal() + "\" style=\"" + this.styleExdr.getOriginal() + "\" text=\"" + this.textExdr.getOriginal() + "\"/>";
        }
    }

    public static class Form extends ModelScreenWidget {
        protected FlexibleStringExpander nameExdr;
        protected FlexibleStringExpander locationExdr;
        protected FlexibleStringExpander shareScopeExdr;
        
        public Form(ModelScreen modelScreen, Element formElement) {
            super(modelScreen, formElement);

            this.nameExdr = new FlexibleStringExpander(formElement.getAttribute("name"));
            this.locationExdr = new FlexibleStringExpander(formElement.getAttribute("location"));
            this.shareScopeExdr = new FlexibleStringExpander(formElement.getAttribute("share-scope"));
        }

        public void renderWidgetString(Writer writer, Map context, ScreenStringRenderer screenStringRenderer) {
            boolean protectScope = !shareScope(context);
            if (protectScope) {
                if (!(context instanceof MapStack)) {
                    context = MapStack.create(context);
                }
                ((MapStack) context).push();
            }
            
            String name = this.getName(context);
            String location = this.getLocation(context);
            ModelForm modelForm = null;
            try {
                modelForm = FormFactory.getFormFromLocation(this.getLocation(context), this.getName(context), this.modelScreen.getDelegator(context), this.modelScreen.getDispatcher(context));
            } catch (IOException e) {
                String errMsg = "Error rendering included form named [" + name + "] at location [" + location + "]: " + e.toString();
                Debug.logError(e, errMsg, module);
                throw new RuntimeException(errMsg);
            } catch (SAXException e) {
                String errMsg = "Error rendering included form named [" + name + "] at location [" + location + "]: " + e.toString();
                Debug.logError(e, errMsg, module);
                throw new RuntimeException(errMsg);
            } catch (ParserConfigurationException e) {
                String errMsg = "Error rendering included form named [" + name + "] at location [" + location + "]: " + e.toString();
                Debug.logError(e, errMsg, module);
                throw new RuntimeException(errMsg);
            }
            
            // try finding the formStringRenderer by name in the context in case one was prepared and put there
            FormStringRenderer formStringRenderer = (FormStringRenderer) context.get("formStringRenderer");
            // if there was no formStringRenderer put in place, now try finding the request/response in the context and creating a new one
            if (formStringRenderer == null) {
                HttpServletRequest request = (HttpServletRequest) context.get("request");
                HttpServletResponse response = (HttpServletResponse) context.get("response");
                if (request != null && response != null) {
                    formStringRenderer = new HtmlFormRenderer(request, response);
                }
            }
            // still null, throw an error
            if (formStringRenderer == null) {
                throw new IllegalArgumentException("Could not find a formStringRenderer in the context, and could not find HTTP request/response objects need to create one.");
            }
            
            //Debug.logInfo("before renderFormString, context:" + context, module);
            StringBuffer renderBuffer = new StringBuffer();
            modelForm.renderFormString(renderBuffer, context, formStringRenderer);
            try {
                writer.write(renderBuffer.toString());
            } catch (IOException e) {
                String errMsg = "Error rendering included form named [" + name + "] at location [" + location + "]: " + e.toString();
                Debug.logError(e, errMsg, module);
                throw new RuntimeException(errMsg);
            }

            if (protectScope) {
                ((MapStack) context).pop();
            }
        }
        
        public String getName(Map context) {
            return this.nameExdr.expandString(context);
        }
        
        public String getLocation(Map context) {
            return this.locationExdr.expandString(context);
        }
        
        public boolean shareScope(Map context) {
            String shareScopeString = this.shareScopeExdr.expandString(context);
            // defaults to false, so anything but true is false
            return "true".equals(shareScopeString);
        }

        public String rawString() {
            return "<include-form name=\"" + this.nameExdr.getOriginal() + "\" location=\"" + this.locationExdr.getOriginal() + "\" share-scope=\"" + this.shareScopeExdr.getOriginal() + "\"/>";
        }
    }

    public static class Tree extends ModelScreenWidget {
        protected FlexibleStringExpander nameExdr;
        protected FlexibleStringExpander locationExdr;
        protected FlexibleStringExpander shareScopeExdr;
        
        public Tree(ModelScreen modelScreen, Element treeElement) {
            super(modelScreen, treeElement);

            this.nameExdr = new FlexibleStringExpander(treeElement.getAttribute("name"));
            this.locationExdr = new FlexibleStringExpander(treeElement.getAttribute("location"));
            this.shareScopeExdr = new FlexibleStringExpander(treeElement.getAttribute("share-scope"));
        }

        public void renderWidgetString(Writer writer, Map context, ScreenStringRenderer screenStringRenderer) throws GeneralException {
            boolean protectScope = !shareScope(context);
            if (protectScope) {
                if (!(context instanceof MapStack)) {
                    context = MapStack.create(context);
                }
                ((MapStack) context).push();
            }
            
            String name = this.getName(context);
            String location = this.getLocation(context);
            ModelTree modelTree = null;
            try {
                modelTree = TreeFactory.getTreeFromLocation(this.getLocation(context), this.getName(context), this.modelScreen.getDelegator(context), this.modelScreen.getDispatcher(context));
            } catch (IOException e) {
                String errMsg = "Error rendering included tree named [" + name + "] at location [" + location + "]: " + e.toString();
                Debug.logError(e, errMsg, module);
                throw new RuntimeException(errMsg);
            } catch (SAXException e) {
                String errMsg = "Error rendering included tree named [" + name + "] at location [" + location + "]: " + e.toString();
                Debug.logError(e, errMsg, module);
                throw new RuntimeException(errMsg);
            } catch (ParserConfigurationException e) {
                String errMsg = "Error rendering included tree named [" + name + "] at location [" + location + "]: " + e.toString();
                Debug.logError(e, errMsg, module);
                throw new RuntimeException(errMsg);
            }
            
            // try finding the treeStringRenderer by name in the context in case one was prepared and put there
            TreeStringRenderer treeStringRenderer = (TreeStringRenderer) context.get("treeStringRenderer");
            // if there was no treeStringRenderer put in place, now try finding the request/response in the context and creating a new one
            if (treeStringRenderer == null) {
                treeStringRenderer = new HtmlTreeRenderer();
                /*
                String renderClassStyle = modelTree.getRenderStyle();
                if (UtilValidate.isNotEmpty(renderClassStyle) && renderClassStyle.equals("simple")) 
                    treeStringRenderer = new HtmlTreeRenderer();
                else
                    treeStringRenderer = new HtmlTreeExpandCollapseRenderer();
                */ 
            }
            // still null, throw an error
            if (treeStringRenderer == null) {
                throw new IllegalArgumentException("Could not find a treeStringRenderer in the context, and could not find HTTP request/response objects need to create one.");
            }
            
            StringBuffer renderBuffer = new StringBuffer();
            modelTree.renderTreeString(renderBuffer, context, treeStringRenderer);
            try {
                writer.write(renderBuffer.toString());
            } catch (IOException e) {
                String errMsg = "Error rendering included tree named [" + name + "] at location [" + location + "]: " + e.toString();
                Debug.logError(e, errMsg, module);
                throw new RuntimeException(errMsg);
            }

            if (protectScope) {
                ((MapStack) context).pop();
            }
        }
        
        public String getName(Map context) {
            return this.nameExdr.expandString(context);
        }
        
        public String getLocation(Map context) {
            return this.locationExdr.expandString(context);
        }
        
        public boolean shareScope(Map context) {
            String shareScopeString = this.shareScopeExdr.expandString(context);
            // defaults to false, so anything but true is false
            return "true".equals(shareScopeString);
        }

        public String rawString() {
            return "<include-tree name=\"" + this.nameExdr.getOriginal() + "\" location=\"" + this.locationExdr.getOriginal() + "\" share-scope=\"" + this.shareScopeExdr.getOriginal() + "\"/>";
        }
    }

    public static class PlatformSpecific extends ModelScreenWidget {
        protected ModelScreenWidget subWidget;
        
        public PlatformSpecific(ModelScreen modelScreen, Element platformSpecificElement) {
            super(modelScreen, platformSpecificElement);
            Element childElement = UtilXml.firstChildElement(platformSpecificElement);
            if ("html".equals(childElement.getNodeName())) {
                subWidget = new HtmlWidget(modelScreen, childElement);
            } else {
                throw new IllegalArgumentException("Tag not supported under the platform-specific tag with name: " + childElement.getNodeName());
            }
        }

        public void renderWidgetString(Writer writer, Map context, ScreenStringRenderer screenStringRenderer) throws GeneralException {
            subWidget.renderWidgetString(writer, context, screenStringRenderer);
        }

        public String rawString() {
            return "<platform-specific>" + (this.subWidget==null?"":this.subWidget.rawString());
        }
    }

    public static class Content extends ModelScreenWidget {
        
        protected FlexibleStringExpander contentId;
        protected FlexibleStringExpander editRequest;
        protected FlexibleStringExpander editContainerStyle;
        protected FlexibleStringExpander enableEditName;
        protected boolean xmlEscape = false;
        protected FlexibleStringExpander dataResourceId;
        protected String width;
        protected String height;
        protected String border;
        
        public Content(ModelScreen modelScreen, Element subContentElement) {
            super(modelScreen, subContentElement);

            // put the text attribute first, then the pcdata under the element, if both are there of course
            this.contentId = new FlexibleStringExpander(subContentElement.getAttribute("content-id"));
            this.dataResourceId = new FlexibleStringExpander(subContentElement.getAttribute("dataresource-id"));
            this.editRequest = new FlexibleStringExpander(subContentElement.getAttribute("edit-request"));
            this.editContainerStyle = new FlexibleStringExpander(subContentElement.getAttribute("edit-container-style"));
            this.enableEditName = new FlexibleStringExpander(subContentElement.getAttribute("enable-edit-name"));
            this.xmlEscape = "true".equals(subContentElement.getAttribute("xml-escape"));
            this.width = subContentElement.getAttribute("width");
            if (UtilValidate.isEmpty(this.width)) this.width="60%";
            this.height = subContentElement.getAttribute("height");
            if (UtilValidate.isEmpty(this.height)) this.width="400px";
            this.border = subContentElement.getAttribute("border");
        }

        public void renderWidgetString(Writer writer, Map context, ScreenStringRenderer screenStringRenderer) {
            try {
                // pushing the contentId on the context as "contentId" is done
                // because many times there will be embedded "subcontent" elements
                // that use the syntax: <subcontent content-id="${contentId}"...
                // and this is a step to make sure that it is there.
                GenericDelegator delegator = (GenericDelegator) context.get("delegator");
                GenericValue content = null;
                String expandedDataResourceId = getDataResourceId(context);
                if (UtilValidate.isEmpty(expandedDataResourceId)) {
                    String expandedContentId = getContentId(context);
                    if (!(context instanceof MapStack)) {
                        context = MapStack.create(context);
                    }
                    
                    // This is an important step to make sure that the current contentId is in the context
                    // as templates that contain "subcontent" elements will expect to find the master
                    // contentId in the context as "contentId".
                    ((MapStack) context).push();
                    context.put("contentId", expandedContentId);
                    
                    if (UtilValidate.isNotEmpty(expandedContentId)) {
                    	content = delegator.findByPrimaryKeyCache("Content", UtilMisc.toMap("contentId", expandedContentId));
                    } else {
                        String errMsg = "contentId is empty.";
                        Debug.logError(errMsg, module);
                        return;
                    }
                    if (content != null) {
                        expandedDataResourceId = content.getString("dataResourceId");
                        this.dataResourceId = new FlexibleStringExpander(expandedDataResourceId);
                    } else {
                        String errMsg = "Could not find content with contentId [" + expandedContentId + "] ";
                        Debug.logError(errMsg, module);
                        throw new RuntimeException(errMsg);
                    }
                }
                GenericValue dataResource = null;
                if (UtilValidate.isNotEmpty(expandedDataResourceId)) {
                	dataResource = delegator.findByPrimaryKeyCache("DataResource", UtilMisc.toMap("dataResourceId", expandedDataResourceId));
                }
                
                String mimeTypeId = null;
                if (dataResource != null) {
                	mimeTypeId = dataResource.getString("mimeTypeId");
                }
                
                if (UtilValidate.isNotEmpty(mimeTypeId) 
                		&& ((mimeTypeId.indexOf("application") >= 0) || (mimeTypeId.indexOf("image")) >= 0) ) {
                	screenStringRenderer.renderContentFrame(writer, context, this);
                } else {
                	screenStringRenderer.renderContentBegin(writer, context, this);
                	screenStringRenderer.renderContentBody(writer, context, this);
                	screenStringRenderer.renderContentEnd(writer, context, this);
                }
                ((MapStack) context).pop();
            } catch (IOException e) {
                String errMsg = "Error rendering content with contentId [" + getContentId(context) + "]: " + e.toString();
                Debug.logError(e, errMsg, module);
                throw new RuntimeException(errMsg);
            } catch (GenericEntityException e) {
                String errMsg = "Error obtaining content with contentId [" + getContentId(context) + "]: " + e.toString();
                Debug.logError(e, errMsg, module);
                throw new RuntimeException(errMsg);
            }

        }
        
        public String getContentId(Map context) {
            return this.contentId.expandString(context);
        }
        
        public String getDataResourceId(Map context) {
            return this.dataResourceId.expandString(context);
        }
        
        public String getEditRequest(Map context) {
            return this.editRequest.expandString(context);
        }
        
        public String getEditContainerStyle(Map context) {
            return this.editContainerStyle.expandString(context);
        }
        
        public String getEnableEditName(Map context) {
            return this.enableEditName.expandString(context);
        }
        
        public boolean xmlEscape() {
            return this.xmlEscape;
        }
        
        public String rawString() {
            // may want to expand this a bit more
            return "<content content-id=\"" + this.contentId.getOriginal() + "\" xml-escape=\"" + this.xmlEscape + "\"/>";
        }
        
        public String getWidth() {
            return this.width;
        }
        
        public String getHeight() {
            return this.height;
        }
        
        public String getBorder() {
            return this.border;
        }
    }

    public static class SubContent extends ModelScreenWidget {
        protected FlexibleStringExpander contentId;
        protected FlexibleStringExpander mapKey;
        protected FlexibleStringExpander editRequest;
        protected FlexibleStringExpander editContainerStyle;
        protected FlexibleStringExpander enableEditName;
        protected boolean xmlEscape = false;
        
        public SubContent(ModelScreen modelScreen, Element subContentElement) {
            super(modelScreen, subContentElement);

            // put the text attribute first, then the pcdata under the element, if both are there of course
            this.contentId = new FlexibleStringExpander(UtilFormatOut.checkNull(subContentElement.getAttribute("content-id")));
            this.mapKey = new FlexibleStringExpander(UtilFormatOut.checkNull(subContentElement.getAttribute("map-key")));
            if (this.mapKey.isEmpty()) {
                this.mapKey = new FlexibleStringExpander(UtilFormatOut.checkNull(subContentElement.getAttribute("assoc-name")));
            }
            this.editRequest = new FlexibleStringExpander(UtilFormatOut.checkNull(subContentElement.getAttribute("edit-request")));
            this.editContainerStyle = new FlexibleStringExpander(subContentElement.getAttribute("edit-container-style"));
            this.enableEditName = new FlexibleStringExpander(subContentElement.getAttribute("enable-edit-name"));
            this.xmlEscape = "true".equals(subContentElement.getAttribute("xml-escape"));
        }

        public void renderWidgetString(Writer writer, Map context, ScreenStringRenderer screenStringRenderer) {
            try {
                screenStringRenderer.renderSubContentBegin(writer, context, this);
                screenStringRenderer.renderSubContentBody(writer, context, this);
                screenStringRenderer.renderSubContentEnd(writer, context, this);
            } catch (IOException e) {
                String errMsg = "Error rendering subContent with contentId [" + getContentId(context) + "]: " + e.toString();
                Debug.logError(e, errMsg, module);
                throw new RuntimeException(errMsg);
            }
        }
        
        public String getContentId(Map context) {
            return this.contentId.expandString(context);
        }
        
        public String getMapKey(Map context) {
            return this.mapKey.expandString(context);
        }
        
        public String getEditRequest(Map context) {
            return this.editRequest.expandString(context);
        }
        
        public String getEditContainerStyle(Map context) {
            return this.editContainerStyle.expandString(context);
        }
        
        public String getEnableEditName(Map context) {
            return this.enableEditName.expandString(context);
        }
        
        public boolean xmlEscape() {
            return this.xmlEscape;
        }

        public String rawString() {
            // may want to expand this a bit more
            return "<sub-content content-id=\"" + this.contentId.getOriginal() + "\" map-key=\"" + this.mapKey.getOriginal() + "\" xml-escape=\"" + this.xmlEscape + "\"/>";
        }
    }

    public static class Menu extends ModelScreenWidget {
        protected FlexibleStringExpander nameExdr;
        protected FlexibleStringExpander locationExdr;
        
        public Menu(ModelScreen modelScreen, Element menuElement) {
            super(modelScreen, menuElement);

            this.nameExdr = new FlexibleStringExpander(menuElement.getAttribute("name"));
            this.locationExdr = new FlexibleStringExpander(menuElement.getAttribute("location"));
        }

        public void renderWidgetString(Writer writer, Map context, ScreenStringRenderer screenStringRenderer) {
            String name = this.getName(context);
            String location = this.getLocation(context);
            ModelMenu modelMenu = null;
            try {
                modelMenu = MenuFactory.getMenuFromLocation(this.getLocation(context), this.getName(context), this.modelScreen.getDelegator(context), this.modelScreen.getDispatcher(context));
            } catch (IOException e) {
                String errMsg = "Error rendering included menu named [" + name + "] at location [" + location + "]: " + e.toString();
                Debug.logError(e, errMsg, module);
                throw new RuntimeException(errMsg);
            } catch (SAXException e) {
                String errMsg = "Error rendering included menu named [" + name + "] at location [" + location + "]: " + e.toString();
                Debug.logError(e, errMsg, module);
                throw new RuntimeException(errMsg);
            } catch (ParserConfigurationException e) {
                String errMsg = "Error rendering included menu named [" + name + "] at location [" + location + "]: " + e.toString();
                Debug.logError(e, errMsg, module);
                throw new RuntimeException(errMsg);
            }
            
            // try finding the menuStringRenderer by name in the context in case one was prepared and put there
            MenuStringRenderer menuStringRenderer = (MenuStringRenderer) context.get("menuStringRenderer");
            // if there was no menuStringRenderer put in place, now try finding the request/response in the context and creating a new one
            if (menuStringRenderer == null) {
                HttpServletRequest request = (HttpServletRequest) context.get("request");
                HttpServletResponse response = (HttpServletResponse) context.get("response");
                if (request != null && response != null) {
                    menuStringRenderer = new HtmlMenuRenderer(request, response);
                }
            }
            // still null, throw an error
            if (menuStringRenderer == null) {
                throw new IllegalArgumentException("Could not find a menuStringRenderer in the context, and could not find HTTP request/response objects need to create one.");
            }
            
            StringBuffer renderBuffer = new StringBuffer();
            modelMenu.renderMenuString(renderBuffer, context, menuStringRenderer);
            try {
                writer.write(renderBuffer.toString());
            } catch (IOException e) {
                String errMsg = "Error rendering included menu named [" + name + "] at location [" + location + "]: " + e.toString();
                Debug.logError(e, errMsg, module);
                throw new RuntimeException(errMsg);
            }
        }
        
        public String getName(Map context) {
            return this.nameExdr.expandString(context);
        }
        
        public String getLocation(Map context) {
            return this.locationExdr.expandString(context);
        }

        public String rawString() {
            return "<include-menu name=\"" + this.nameExdr.getOriginal() + "\" location=\"" + this.locationExdr.getOriginal() + "\"/>";
        }
    }
    
    public static class Link extends ModelScreenWidget {
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
        

        public Link(ModelScreen modelScreen, Element linkElement) {
            super(modelScreen, linkElement);

            setText(linkElement.getAttribute("text"));
            setId(linkElement.getAttribute("id"));
            setStyle(linkElement.getAttribute("style"));
            setName(linkElement.getAttribute("name"));
            setTarget(linkElement.getAttribute("target"));
            setTargetWindow(linkElement.getAttribute("target-window"));
            setPrefix(linkElement.getAttribute("prefix"));
            setUrlMode(linkElement.getAttribute("url-mode"));
            setFullPath(linkElement.getAttribute("full-path"));
            setSecure(linkElement.getAttribute("secure"));
            setEncode(linkElement.getAttribute("encode"));
            Element imageElement = UtilXml.firstChildElement(linkElement, "image");
            if (imageElement != null) {
                this.image = new Image(modelScreen, imageElement);
            }

        }

        public void renderWidgetString(Writer writer, Map context, ScreenStringRenderer screenStringRenderer) {
            try {
                screenStringRenderer.renderLink(writer, context, this);
            } catch (IOException e) {
                String errMsg = "Error rendering link with id [" + getId(context) + "]: " + e.toString();
                Debug.logError(e, errMsg, module);
                throw new RuntimeException(errMsg);
            }
        }
        
        public String getText(Map context) {
            return this.textExdr.expandString(context);
        }
        
        public String getId(Map context) {
            return this.idExdr.expandString(context);
        }
        
        public String getStyle(Map context) {
            return this.styleExdr.expandString(context);
        }
        
        public String getTarget(Map context) {
            return this.targetExdr.expandString(context);
        }
        
        public String getName(Map context) {
            return this.nameExdr.expandString(context);
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
        public void setName(String val) {
            this.nameExdr = new FlexibleStringExpander(val);
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

        public String rawString() {
            // may want to add more to this
            return "<link id=\"" + this.idExdr.getOriginal() + "\" style=\"" + this.styleExdr.getOriginal() + "\" text=\"" + this.textExdr.getOriginal() + "\" target=\"" + this.targetExdr.getOriginal() + "\" name=\"" + this.nameExdr.getOriginal() + "\" url-mode=\"" + this.urlMode + "\"/>";
        }
    }

    public static class Image extends ModelScreenWidget {
        protected FlexibleStringExpander srcExdr;
        protected FlexibleStringExpander idExdr;
        protected FlexibleStringExpander styleExdr;
        protected FlexibleStringExpander widthExdr;
        protected FlexibleStringExpander heightExdr;
        protected FlexibleStringExpander borderExdr;
        protected String urlMode = "content";

        public Image(ModelScreen modelScreen, Element imageElement) {
            super(modelScreen, imageElement);

            setSrc(imageElement.getAttribute("src"));
            setId(imageElement.getAttribute("id"));
            setStyle(imageElement.getAttribute("style"));
            setWidth(imageElement.getAttribute("width"));
            setHeight(imageElement.getAttribute("height"));
            setBorder(UtilFormatOut.checkEmpty(imageElement.getAttribute("border"), "0"));
            setUrlMode(UtilFormatOut.checkEmpty(imageElement.getAttribute("url-mode"), "content"));
        }

        public void renderWidgetString(Writer writer, Map context, ScreenStringRenderer screenStringRenderer) {
            try {
                screenStringRenderer.renderImage(writer, context, this);
            } catch (IOException e) {
                String errMsg = "Error rendering image with id [" + getId(context) + "]: " + e.toString();
                Debug.logError(e, errMsg, module);
                throw new RuntimeException(errMsg);
            }
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
            if (UtilValidate.isEmpty(val)) {
                this.urlMode = "content";
            } else {
                this.urlMode = val;
            }
        }

        public String rawString() {
            // may want to add more to this
            return "<image id=\"" + this.idExdr.getOriginal() + "\" style=\"" + this.styleExdr.getOriginal() + "\" src=\"" + this.srcExdr.getOriginal() + "\" url-mode=\"" + this.urlMode + "\"/>";
        }
    }
}

