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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.ParserConfigurationException;

import javolution.util.FastList;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.StringUtil;
import org.ofbiz.base.util.UtilFormatOut;
import org.ofbiz.base.util.UtilGenerics;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.UtilXml;
import org.ofbiz.base.util.collections.MapStack;
import org.ofbiz.base.util.string.FlexibleStringExpander;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.widget.ModelWidget;
import org.ofbiz.widget.WidgetWorker;
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
import org.w3c.dom.Element;
import org.xml.sax.SAXException;


/**
 * Widget Library - Screen model class
 */
@SuppressWarnings("serial")
public abstract class ModelScreenWidget extends ModelWidget implements Serializable {
    public static final String module = ModelScreenWidget.class.getName();

    protected ModelScreen modelScreen;

    public ModelScreenWidget(ModelScreen modelScreen, Element widgetElement) {
        super(widgetElement);
        this.modelScreen = modelScreen;
        if (Debug.verboseOn()) Debug.logVerbose("Reading Screen sub-widget with name: " + widgetElement.getNodeName(), module);
    }

    public abstract void renderWidgetString(Appendable writer, Map<String, Object> context, ScreenStringRenderer screenStringRenderer) throws GeneralException, IOException;

    public abstract String rawString();

    public static List<ModelScreenWidget> readSubWidgets(ModelScreen modelScreen, List<? extends Element> subElementList) {
        List<ModelScreenWidget> subWidgets = FastList.newInstance();
        for (Element subElement: subElementList) {
            if ("section".equals(subElement.getNodeName())) {
                subWidgets.add(new Section(modelScreen, subElement));
            } else if ("container".equals(subElement.getNodeName())) {
                subWidgets.add(new Container(modelScreen, subElement));
            } else if ("screenlet".equals(subElement.getNodeName())) {
                subWidgets.add(new Screenlet(modelScreen, subElement));
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
            } else if ("horizontal-separator".equals(subElement.getNodeName())) {
                subWidgets.add(new HorizontalSeparator(modelScreen, subElement));
            } else {
                throw new IllegalArgumentException("Found invalid screen widget element with name: " + subElement.getNodeName());
            }
        }

        return subWidgets;
    }

    public static void renderSubWidgetsString(List<ModelScreenWidget> subWidgets, Appendable writer, Map<String, Object> context, ScreenStringRenderer screenStringRenderer) throws GeneralException, IOException {
        if (subWidgets == null) {
            return;
        }
        for (ModelScreenWidget subWidget: subWidgets) {
            if (Debug.verboseOn()) Debug.logVerbose("Rendering screen " + subWidget.modelScreen.getName() + "; widget class is " + subWidget.getClass().getName(), module);

            // render the sub-widget itself
            subWidget.renderWidgetString(writer, context, screenStringRenderer);
        }
    }

    @Override
    public boolean boundaryCommentsEnabled() {
        return modelScreen.boundaryCommentsEnabled();
    }

    public ModelScreen getModelScreen() {
        return this.modelScreen;
    }

    public static class SectionsRenderer extends HashMap<String, Object> {
        protected ScreenStringRenderer screenStringRenderer;
        protected Map<String, Object> context;
        protected Appendable writer;

        public SectionsRenderer(Map<String, ? extends Object> sectionMap, Map<String, Object> context, Appendable writer, ScreenStringRenderer screenStringRenderer) {
            this.putAll(sectionMap);
            this.context = context;
            this.writer = writer;
            this.screenStringRenderer = screenStringRenderer;
        }

        /** This is a lot like the ScreenRenderer class and returns an empty String so it can be used more easily with FreeMarker */
        public String render(String sectionName) throws GeneralException, IOException {
            ModelScreenWidget section = (ModelScreenWidget) this.get(sectionName);
            // if no section by that name, write nothing
            if (section != null) {
                section.renderWidgetString(this.writer, this.context, this.screenStringRenderer);
            }
            return "";
        }
    }

    public static class Section extends ModelScreenWidget {
        protected ModelScreenCondition condition;
        protected List<ModelScreenAction> actions;
        protected List<ModelScreenWidget> subWidgets;
        protected List<ModelScreenWidget> failWidgets;
        public boolean isMainSection = false;

        public Section(ModelScreen modelScreen, Element sectionElement) {
            super(modelScreen, sectionElement);

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
            List<? extends Element> subElementList = UtilXml.childElementList(widgetsElement);
            this.subWidgets = ModelScreenWidget.readSubWidgets(this.modelScreen, subElementList);

            // read fail-widgets
            Element failWidgetsElement = UtilXml.firstChildElement(sectionElement, "fail-widgets");
            if (failWidgetsElement != null) {
                List<? extends Element> failElementList = UtilXml.childElementList(failWidgetsElement);
                this.failWidgets = ModelScreenWidget.readSubWidgets(this.modelScreen, failElementList);
            }
        }

        @Override
        public void renderWidgetString(Appendable writer, Map<String, Object> context, ScreenStringRenderer screenStringRenderer) throws GeneralException, IOException {
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

        @Override
        public String getBoundaryCommentName() {
            if (isMainSection) {
                return modelScreen.getSourceLocation() + "#" + modelScreen.getName();
            } else {
                return name;
            }
        }

        @Override
        public String rawString() {
            return "<section" + (UtilValidate.isNotEmpty(this.name)?" name=\"" + this.name + "\"":"") + ">";
        }
    }

    public static class Container extends ModelScreenWidget {
        protected FlexibleStringExpander idExdr;
        protected FlexibleStringExpander styleExdr;
        protected FlexibleStringExpander autoUpdateTargetExdr;
        protected String autoUpdateInterval = "2";
        protected List<ModelScreenWidget> subWidgets;

        public Container(ModelScreen modelScreen, Element containerElement) {
            super(modelScreen, containerElement);
            this.idExdr = FlexibleStringExpander.getInstance(containerElement.getAttribute("id"));
            this.styleExdr = FlexibleStringExpander.getInstance(containerElement.getAttribute("style"));
            this.autoUpdateTargetExdr = FlexibleStringExpander.getInstance(containerElement.getAttribute("auto-update-target"));
            if (containerElement.hasAttribute("auto-update-interval")) {
                this.autoUpdateInterval = containerElement.getAttribute("auto-update-interval");
            }

            // read sub-widgets
            List<? extends Element> subElementList = UtilXml.childElementList(containerElement);
            this.subWidgets = ModelScreenWidget.readSubWidgets(this.modelScreen, subElementList);
        }

        @Override
        public void renderWidgetString(Appendable writer, Map<String, Object> context, ScreenStringRenderer screenStringRenderer) throws GeneralException, IOException {
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

        public String getId(Map<String, Object> context) {
            return this.idExdr.expandString(context);
        }

        public String getStyle(Map<String, Object> context) {
            return this.styleExdr.expandString(context);
        }

        public String getAutoUpdateTargetExdr(Map<String, Object> context) {
            return this.autoUpdateTargetExdr.expandString(context);
        }

        public String getAutoUpdateInterval() {
            return this.autoUpdateInterval;
        }

        @Override
        public String rawString() {
            return "<container id=\"" + this.idExdr.getOriginal() + "\" style=\"" + this.styleExdr.getOriginal() + "\" auto-update-target=\"" + this.autoUpdateTargetExdr.getOriginal() + "\">";
        }
    }

    public static class Screenlet extends ModelScreenWidget {
        protected FlexibleStringExpander idExdr;
        protected FlexibleStringExpander titleExdr;
        protected Menu navigationMenu = null;
        protected Menu tabMenu = null;
        protected Form navigationForm = null;
        protected boolean collapsible = false;
        protected boolean initiallyCollapsed = false;
        protected boolean saveCollapsed = true;
        protected boolean padded = true;
        protected List<ModelScreenWidget> subWidgets;

        public Screenlet(ModelScreen modelScreen, Element screenletElement) {
            super(modelScreen, screenletElement);
            this.idExdr = FlexibleStringExpander.getInstance(screenletElement.getAttribute("id"));
            this.collapsible = "true".equals(screenletElement.getAttribute("collapsible"));
            this.initiallyCollapsed = "true".equals(screenletElement.getAttribute("initially-collapsed"));
            if (this.initiallyCollapsed) {
                this.collapsible = true;
            }
            // By default, for a collapsible screenlet, the collapsed/expanded status must be saved
            this.saveCollapsed = !("false".equals(screenletElement.getAttribute("save-collapsed")));
            
            this.padded = !"false".equals(screenletElement.getAttribute("padded"));
            if (this.collapsible && UtilValidate.isEmpty(this.name) && idExdr.isEmpty()) {
                throw new IllegalArgumentException("Collapsible screenlets must have a name or id [" + this.modelScreen.getName() + "]");
            }
            this.titleExdr = FlexibleStringExpander.getInstance(screenletElement.getAttribute("title"));
            List<? extends Element> subElementList = UtilXml.childElementList(screenletElement);
            this.subWidgets = ModelScreenWidget.readSubWidgets(this.modelScreen, subElementList);
            String navMenuName = screenletElement.getAttribute("navigation-menu-name");
            if (UtilValidate.isNotEmpty(navMenuName)) {
                for (ModelWidget subWidget : this.subWidgets) {
                    if (navMenuName.equals(subWidget.getName()) && subWidget instanceof Menu) {
                        this.navigationMenu = (Menu) subWidget;
                        subWidgets.remove(subWidget);
                        break;
                    }
                }
            }
            String tabMenuName = screenletElement.getAttribute("tab-menu-name");
            if (UtilValidate.isNotEmpty(tabMenuName)) {
                for (ModelWidget subWidget : this.subWidgets) {
                    if (tabMenuName.equals(subWidget.getName()) && subWidget instanceof Menu) {
                        this.tabMenu = (Menu) subWidget;
                        subWidgets.remove(subWidget);
                        break;
                    }
                }
            }
            String formName = screenletElement.getAttribute("navigation-form-name");
            if (UtilValidate.isNotEmpty(formName) && this.navigationMenu == null) {
                for (ModelWidget subWidget : this.subWidgets) {
                    if (formName.equals(subWidget.getName()) && subWidget instanceof Form) {
                        this.navigationForm = (Form) subWidget;
                        // Let's give this a try, it can be removed later if it
                        // proves to cause problems
                        this.padded = false;
                        break;
                    }
                }
            }
        }

        @Override
        public void renderWidgetString(Appendable writer, Map<String, Object> context, ScreenStringRenderer screenStringRenderer) throws GeneralException, IOException {
            boolean collapsed = getInitiallyCollapsed(context);
            if (this.collapsible) {
                String preferenceKey = getPreferenceKey(context) + "_collapsed";
                Map<String, Object> requestParameters = UtilGenerics.checkMap(context.get("requestParameters"));
                if (requestParameters != null) {
                    String collapsedParam = (String) requestParameters.get(preferenceKey);
                    if (UtilValidate.isNotEmpty(collapsedParam)) {
                        collapsed = "true".equals(collapsedParam);
                    }
                }
            }
            try {
                screenStringRenderer.renderScreenletBegin(writer, context, collapsed, this);
                for (ModelScreenWidget subWidget : this.subWidgets) {
                    screenStringRenderer.renderScreenletSubWidget(writer, context, subWidget, this);
                }
                screenStringRenderer.renderScreenletEnd(writer, context, this);
            } catch (IOException e) {
                String errMsg = "Error rendering screenlet in screen named [" + this.modelScreen.getName() + "]: ";
                Debug.logError(e, errMsg, module);
                throw new RuntimeException(errMsg + e);
            }
        }

        public boolean collapsible() {
            return this.collapsible;
        }
        
        //initially-collapsed status, which may be overriden by user preference
        public boolean getInitiallyCollapsed(Map<String, Object> context) {
            String screenletId = this.getId(context) + "_collapsed";
            Map<String, ? extends Object> userPreferences = UtilGenerics.checkMap(context.get("userPreferences"));
            if (userPreferences != null && userPreferences.containsKey(screenletId)) {
                return Boolean.valueOf((String)userPreferences.get(screenletId)).booleanValue() ; 
            }
            
            return this.initiallyCollapsed;
        }

        public boolean saveCollapsed() {
            return this.saveCollapsed;
        }
        public boolean padded() {
            return this.padded;
        }

        public String getPreferenceKey(Map<String, Object> context) {
            String name = this.idExdr.expandString(context);
            if (UtilValidate.isEmpty(name)) {
                name = "screenlet" + "_" + Integer.toHexString(hashCode());
            }
            return name;
        }

        public String getId(Map<String, Object> context) {
            return this.idExdr.expandString(context);
        }

        public String getTitle(Map<String, Object> context) {
            return this.titleExdr.expandString(context);
        }

        public Menu getNavigationMenu() {
            return this.navigationMenu;
        }

        public Form getNavigationForm() {
            return this.navigationForm;
        }

        public Menu getTabMenu() {
            return this.tabMenu;
        }

        @Override
        public String rawString() {
            return "<screenlet id=\"" + this.idExdr.getOriginal() + "\" title=\"" + this.titleExdr.getOriginal() + "\">";
        }
    }

    public static class HorizontalSeparator extends ModelScreenWidget {
        protected FlexibleStringExpander idExdr;
        protected FlexibleStringExpander styleExdr;

        public HorizontalSeparator(ModelScreen modelScreen, Element separatorElement) {
            super(modelScreen, separatorElement);
            this.idExdr = FlexibleStringExpander.getInstance(separatorElement.getAttribute("id"));
            this.styleExdr = FlexibleStringExpander.getInstance(separatorElement.getAttribute("style"));
        }

        @Override
        public void renderWidgetString(Appendable writer, Map<String, Object> context, ScreenStringRenderer screenStringRenderer) throws GeneralException, IOException {
            screenStringRenderer.renderHorizontalSeparator(writer, context, this);
        }

        public String getId(Map<String, Object> context) {
            return this.idExdr.expandString(context);
        }

        public String getStyle(Map<String, Object> context) {
            return this.styleExdr.expandString(context);
        }

        @Override
        public String rawString() {
            return "<horizontal-separator id=\"" + this.idExdr.getOriginal() + "\" name=\"" + this.idExdr.getOriginal() + "\" style=\"" + this.styleExdr.getOriginal() + "\">";
        }
    }

    public static class IncludeScreen extends ModelScreenWidget {
        protected FlexibleStringExpander nameExdr;
        protected FlexibleStringExpander locationExdr;
        protected FlexibleStringExpander shareScopeExdr;

        public IncludeScreen(ModelScreen modelScreen, Element includeScreenElement) {
            super(modelScreen, includeScreenElement);
            this.nameExdr = FlexibleStringExpander.getInstance(includeScreenElement.getAttribute("name"));
            this.locationExdr = FlexibleStringExpander.getInstance(includeScreenElement.getAttribute("location"));
            this.shareScopeExdr = FlexibleStringExpander.getInstance(includeScreenElement.getAttribute("share-scope"));
        }

        @Override
        public void renderWidgetString(Appendable writer, Map<String, Object> context, ScreenStringRenderer screenStringRenderer) throws GeneralException, IOException {
            // if we are not sharing the scope, protect it using the MapStack
            boolean protectScope = !shareScope(context);
            if (protectScope) {
                if (!(context instanceof MapStack)) {
                    context = MapStack.create(context);
                }

                UtilGenerics.<MapStack<String>>cast(context).push();

                // build the widgetpath
                List<String> widgetTrail = UtilGenerics.toList(context.get("_WIDGETTRAIL_"));
                if (widgetTrail == null) {
                    widgetTrail = FastList.newInstance();
                }

                String thisName = nameExdr.expandString(context);
                widgetTrail.add(thisName);
                context.put("_WIDGETTRAIL_", widgetTrail);
            }

            // dont need the renderer here, will just pass this on down to another screen call; screenStringRenderer.renderContainerBegin(writer, context, this);
            String name = this.getName(context);
            String location = this.getLocation(context);

            if (UtilValidate.isEmpty(name)) {
                if (Debug.verboseOn()) Debug.logVerbose("In the include-screen tag the screen name was empty, ignoring include; in screen [" + this.modelScreen.getName() + "]", module);
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
                modelScreen = this.modelScreen.modelScreenMap.get(name);
                if (modelScreen == null) {
                    throw new IllegalArgumentException("Could not find screen with name [" + name + "] in the same file as the screen with name [" + this.modelScreen.getName() + "]");
                }
            }
            modelScreen.renderScreenString(writer, context, screenStringRenderer);

            if (protectScope) {
                UtilGenerics.<MapStack<String>>cast(context).pop();
            }
        }

        public String getName(Map<String, Object> context) {
            return this.nameExdr.expandString(context);
        }

        public String getLocation(Map<String, Object> context) {
            return this.locationExdr.expandString(context);
        }

        public boolean shareScope(Map<String, Object> context) {
            String shareScopeString = this.shareScopeExdr.expandString(context);
            // defaults to false, so anything but true is false
            return "true".equals(shareScopeString);
        }

        @Override
        public String rawString() {
            return "<include-screen name=\"" + this.nameExdr.getOriginal() + "\" location=\"" + this.locationExdr.getOriginal() + "\" share-scope=\"" + this.shareScopeExdr.getOriginal() + "\"/>";
        }
    }

    public static class DecoratorScreen extends ModelScreenWidget {
        protected FlexibleStringExpander nameExdr;
        protected FlexibleStringExpander locationExdr;
        protected Map<String, DecoratorSection> sectionMap = new HashMap<String, DecoratorSection>();

        public DecoratorScreen(ModelScreen modelScreen, Element decoratorScreenElement) {
            super(modelScreen, decoratorScreenElement);
            this.nameExdr = FlexibleStringExpander.getInstance(decoratorScreenElement.getAttribute("name"));
            this.locationExdr = FlexibleStringExpander.getInstance(decoratorScreenElement.getAttribute("location"));

            List<? extends Element> decoratorSectionElementList = UtilXml.childElementList(decoratorScreenElement, "decorator-section");
            for (Element decoratorSectionElement: decoratorSectionElementList) {
                String name = decoratorSectionElement.getAttribute("name");
                this.sectionMap.put(name, new DecoratorSection(modelScreen, decoratorSectionElement));
            }
        }

        @Override
        @SuppressWarnings("unchecked")
        public void renderWidgetString(Appendable writer, Map<String, Object> context, ScreenStringRenderer screenStringRenderer) throws GeneralException, IOException {
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
                modelScreen = this.modelScreen.modelScreenMap.get(name);
                if (modelScreen == null) {
                    throw new IllegalArgumentException("Could not find screen with name [" + name + "] in the same file as the screen with name [" + this.modelScreen.getName() + "]");
                }
            }
            modelScreen.renderScreenString(writer, context, screenStringRenderer);

            contextMs.pop();
        }

        public String getName(Map<String, Object> context) {
            return this.nameExdr.expandString(context);
        }

        public String getLocation(Map<String, Object> context) {
            return this.locationExdr.expandString(context);
        }

        @Override
        public String rawString() {
            return "<decorator-screen name=\"" + this.nameExdr.getOriginal() + "\" location=\"" + this.locationExdr.getOriginal() + "\"/>";
        }
    }

    public static class DecoratorSection extends ModelScreenWidget {
        protected List<ModelScreenWidget> subWidgets;

        public DecoratorSection(ModelScreen modelScreen, Element decoratorSectionElement) {
            super(modelScreen, decoratorSectionElement);
            // read sub-widgets
            List<? extends Element> subElementList = UtilXml.childElementList(decoratorSectionElement);
            this.subWidgets = ModelScreenWidget.readSubWidgets(this.modelScreen, subElementList);
        }

        @Override
        public void renderWidgetString(Appendable writer, Map<String, Object> context, ScreenStringRenderer screenStringRenderer) throws GeneralException, IOException {
            // render sub-widgets
            renderSubWidgetsString(this.subWidgets, writer, context, screenStringRenderer);
        }

        @Override
        public String rawString() {
            return "<decorator-section name=\"" + this.name + "\">";
        }
    }

    public static class DecoratorSectionInclude extends ModelScreenWidget {

        public DecoratorSectionInclude(ModelScreen modelScreen, Element decoratorSectionElement) {
            super(modelScreen, decoratorSectionElement);
        }

        @Override
        public void renderWidgetString(Appendable writer, Map<String, Object> context, ScreenStringRenderer screenStringRenderer) throws GeneralException, IOException {
            Map<String, ? extends Object> preRenderedContent = UtilGenerics.checkMap(context.get("preRenderedContent"));
            if (preRenderedContent != null && preRenderedContent.containsKey(this.name)) {
                try {
                    writer.append((String) preRenderedContent.get(this.name));
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

        @Override
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
            this.textExdr = FlexibleStringExpander.getInstance(textAttr + pcdata);

            this.idExdr = FlexibleStringExpander.getInstance(labelElement.getAttribute("id"));
            this.styleExdr = FlexibleStringExpander.getInstance(labelElement.getAttribute("style"));
        }

        @Override
        public void renderWidgetString(Appendable writer, Map<String, Object> context, ScreenStringRenderer screenStringRenderer) {
            try {
                screenStringRenderer.renderLabel(writer, context, this);
            } catch (IOException e) {
                String errMsg = "Error rendering label in screen named [" + this.modelScreen.getName() + "]: " + e.toString();
                Debug.logError(e, errMsg, module);
                throw new RuntimeException(errMsg);
            }
        }

        public String getText(Map<String, Object> context) {
            String text = this.textExdr.expandString(context);
            StringUtil.SimpleEncoder simpleEncoder = (StringUtil.SimpleEncoder) context.get("simpleEncoder");
            if (simpleEncoder != null) {
                text = simpleEncoder.encode(text);
            }
            return text;
        }

        public String getId(Map<String, Object> context) {
            return this.idExdr.expandString(context);
        }

        public String getStyle(Map<String, Object> context) {
            return this.styleExdr.expandString(context);
        }

        @Override
        public String rawString() {
            return "<label id=\"" + this.idExdr.getOriginal() + "\" style=\"" + this.styleExdr.getOriginal() + "\" text=\"" + this.textExdr.getOriginal() + "\"/>";
        }
    }

    public static class Form extends ModelScreenWidget {
        protected FlexibleStringExpander nameExdr;
        protected FlexibleStringExpander locationExdr;
        protected FlexibleStringExpander shareScopeExdr;
        protected ModelForm modelForm = null;

        public Form(ModelScreen modelScreen, Element formElement) {
            super(modelScreen, formElement);

            this.nameExdr = FlexibleStringExpander.getInstance(formElement.getAttribute("name"));
            this.locationExdr = FlexibleStringExpander.getInstance(formElement.getAttribute("location"));
            this.shareScopeExdr = FlexibleStringExpander.getInstance(formElement.getAttribute("share-scope"));
        }

        @Override
        public void renderWidgetString(Appendable writer, Map<String, Object> context, ScreenStringRenderer screenStringRenderer) {
            boolean protectScope = !shareScope(context);
            if (protectScope) {
                if (!(context instanceof MapStack)) {
                    context = MapStack.create(context);
                }
                UtilGenerics.<MapStack<String>>cast(context).push();
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

            ModelForm modelForm = getModelForm(context);
            //Debug.logInfo("before renderFormString, context:" + context, module);
            try {
                modelForm.renderFormString(writer, context, formStringRenderer);
            } catch (IOException e) {
                String errMsg = "Error rendering included form named [" + name + "] at location [" + this.getLocation(context) + "]: " + e.toString();
                Debug.logError(e, errMsg, module);
                throw new RuntimeException(errMsg + e);
            }

            if (protectScope) {
                UtilGenerics.<MapStack<String>>cast(context).pop();
            }
        }

        public ModelForm getModelForm(Map<String, Object> context) {
            ModelForm modelForm = null;
            String name = this.getName(context);
            String location = this.getLocation(context);
            try {
                modelForm = FormFactory.getFormFromLocation(location, name, this.modelScreen.getDelegator(context).getModelReader(), this.modelScreen.getDispatcher(context).getDispatchContext());
            } catch (Exception e) {
                String errMsg = "Error rendering included form named [" + name + "] at location [" + location + "]: ";
                Debug.logError(e, errMsg, module);
                throw new RuntimeException(errMsg + e);
            }
            return modelForm;
        }

        public String getName(Map<String, Object> context) {
            return this.nameExdr.expandString(context);
        }

        public String getLocation(Map<String, Object> context) {
            return this.locationExdr.expandString(context);
        }

        public boolean shareScope(Map<String, Object> context) {
            String shareScopeString = this.shareScopeExdr.expandString(context);
            // defaults to false, so anything but true is false
            return "true".equals(shareScopeString);
        }

        @Override
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

            this.nameExdr = FlexibleStringExpander.getInstance(treeElement.getAttribute("name"));
            this.locationExdr = FlexibleStringExpander.getInstance(treeElement.getAttribute("location"));
            this.shareScopeExdr = FlexibleStringExpander.getInstance(treeElement.getAttribute("share-scope"));
        }

        @Override
        public void renderWidgetString(Appendable writer, Map<String, Object> context, ScreenStringRenderer screenStringRenderer) throws GeneralException, IOException {
            boolean protectScope = !shareScope(context);
            if (protectScope) {
                if (!(context instanceof MapStack)) {
                    context = MapStack.create(context);
                }
                UtilGenerics.<MapStack<String>>cast(context).push();
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
 
            TreeStringRenderer treeStringRenderer = (TreeStringRenderer) context.get("treeStringRenderer");
            if (treeStringRenderer == null) {
                throw new IllegalArgumentException("Could not find a treeStringRenderer in the context");
            }

            StringBuffer renderBuffer = new StringBuffer();
            modelTree.renderTreeString(renderBuffer, context, treeStringRenderer);
            try {
                writer.append(renderBuffer.toString());
            } catch (IOException e) {
                String errMsg = "Error rendering included tree named [" + name + "] at location [" + location + "]: " + e.toString();
                Debug.logError(e, errMsg, module);
                throw new RuntimeException(errMsg);
            }

            if (protectScope) {
                UtilGenerics.<MapStack<String>>cast(context).pop();
            }
        }

        public String getName(Map<String, Object> context) {
            return this.nameExdr.expandString(context);
        }

        public String getLocation(Map<String, Object> context) {
            return this.locationExdr.expandString(context);
        }

        public boolean shareScope(Map<String, Object> context) {
            String shareScopeString = this.shareScopeExdr.expandString(context);
            // defaults to false, so anything but true is false
            return "true".equals(shareScopeString);
        }

        @Override
        public String rawString() {
            return "<include-tree name=\"" + this.nameExdr.getOriginal() + "\" location=\"" + this.locationExdr.getOriginal() + "\" share-scope=\"" + this.shareScopeExdr.getOriginal() + "\"/>";
        }
    }

    public static class PlatformSpecific extends ModelScreenWidget {
        protected Map<String, ModelScreenWidget> subWidgets;

        public PlatformSpecific(ModelScreen modelScreen, Element platformSpecificElement) {
            super(modelScreen, platformSpecificElement);
            subWidgets = new HashMap<String, ModelScreenWidget>();
            List<? extends Element> childElements = UtilXml.childElementList(platformSpecificElement);
            if (childElements != null) {
                for (Element childElement: childElements) {
                    if ("html".equals(childElement.getNodeName())) {
                        subWidgets.put("html", new HtmlWidget(modelScreen, childElement));
                    } else if ("xsl-fo".equals(childElement.getNodeName())) {
                        subWidgets.put("xsl-fo", new HtmlWidget(modelScreen, childElement));
                    } else if ("xml".equals(childElement.getNodeName())) {
                        subWidgets.put("xml", new HtmlWidget(modelScreen, childElement));
                    } else {
                        throw new IllegalArgumentException("Tag not supported under the platform-specific tag with name: " + childElement.getNodeName());
                    }
                }
            }
        }

        @Override
        public void renderWidgetString(Appendable writer, Map<String, Object> context, ScreenStringRenderer screenStringRenderer) throws GeneralException, IOException {
            ModelScreenWidget subWidget = null;
            subWidget = (ModelScreenWidget)subWidgets.get(screenStringRenderer.getRendererName());
            if (subWidget == null) {
                // This is here for backward compatibility
                Debug.logWarning("In platform-dependent could not find template for " + screenStringRenderer.getRendererName() + ", using the one for html.", module);
                subWidget = (ModelScreenWidget)subWidgets.get("html");
            }
            if (subWidget != null) {
                subWidget.renderWidgetString(writer, context, screenStringRenderer);
            }
        }

        @Override
        public String rawString() {
            Collection<ModelScreenWidget> subWidgetList = this.subWidgets.values();
            StringBuilder subWidgetsRawString = new StringBuilder("<platform-specific>");
            for (ModelScreenWidget subWidget: subWidgetList) {
                subWidgetsRawString.append(subWidget.rawString());
            }
            return subWidgetsRawString.append("</platform-specific>").toString();
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
            this.contentId = FlexibleStringExpander.getInstance(subContentElement.getAttribute("content-id"));
            this.dataResourceId = FlexibleStringExpander.getInstance(subContentElement.getAttribute("dataresource-id"));
            this.editRequest = FlexibleStringExpander.getInstance(subContentElement.getAttribute("edit-request"));
            this.editContainerStyle = FlexibleStringExpander.getInstance(subContentElement.getAttribute("edit-container-style"));
            this.enableEditName = FlexibleStringExpander.getInstance(subContentElement.getAttribute("enable-edit-name"));
            this.xmlEscape = "true".equals(subContentElement.getAttribute("xml-escape"));
            this.width = subContentElement.getAttribute("width");
            if (UtilValidate.isEmpty(this.width)) this.width="60%";
            this.height = subContentElement.getAttribute("height");
            if (UtilValidate.isEmpty(this.height)) this.width="400px";
            this.border = subContentElement.getAttribute("border");
        }

        @Override
        public void renderWidgetString(Appendable writer, Map<String, Object> context, ScreenStringRenderer screenStringRenderer) {
            try {
                // pushing the contentId on the context as "contentId" is done
                // because many times there will be embedded "subcontent" elements
                // that use the syntax: <subcontent content-id="${contentId}"...
                // and this is a step to make sure that it is there.
                Delegator delegator = (Delegator) context.get("delegator");
                GenericValue content = null;
                String expandedDataResourceId = getDataResourceId(context);
                String expandedContentId = getContentId(context);
                if (!(context instanceof MapStack)) {
                    context = MapStack.create(context);
                }

                // This is an important step to make sure that the current contentId is in the context
                // as templates that contain "subcontent" elements will expect to find the master
                // contentId in the context as "contentId".
                UtilGenerics.<MapStack<String>>cast(context).push();
                context.put("contentId", expandedContentId);

                if (UtilValidate.isEmpty(expandedDataResourceId)) {
                    if (UtilValidate.isNotEmpty(expandedContentId)) {
                        content = delegator.findByPrimaryKeyCache("Content", UtilMisc.toMap("contentId", expandedContentId));
                    } else {
                        String errMsg = "contentId is empty.";
                        Debug.logError(errMsg, module);
                        return;
                    }
                    if (content != null) {
                        expandedDataResourceId = content.getString("dataResourceId");
                    } else {
                        String errMsg = "Could not find content with contentId [" + expandedContentId + "] ";
                        Debug.logError(errMsg, module);
                        throw new RuntimeException(errMsg);
                    }
                }

                GenericValue dataResource = null;
                if (UtilValidate.isNotEmpty(expandedDataResourceId)) {
                    dataResource = delegator.findByPrimaryKeyCache("DataResource", UtilMisc.toMap("dataResourceId", expandedDataResourceId));
                    this.dataResourceId = FlexibleStringExpander.getInstance(expandedDataResourceId);
                }

                String mimeTypeId = null;
                if (dataResource != null) {
                    mimeTypeId = dataResource.getString("mimeTypeId");
                }
                if (UtilValidate.isNotEmpty(content)) {
                    mimeTypeId = content.getString("mimeTypeId");
                }

                if (UtilValidate.isNotEmpty(mimeTypeId)
                        && ((mimeTypeId.indexOf("application") >= 0) || (mimeTypeId.indexOf("image")) >= 0)) {
                    
/*                    
                     // this code is not yet working, will update it the next few weeks...(Hans)
                    if (mimeTypeId.equals("application/pdf")) {
                        TransformerFactory tfactory = TransformerFactory.newInstance();
                        try {
                            
                            //
                            //  this part is not working. If somebody can help to make it work?
                            //  currently using only real temp files for debugging purposes.
                            //
                            //  most of the code should be replaced by functions in xslTransform.java and other files.
                            //  for debugging here mostly code using the code outside of ofbiz.
                            //
                            SAXParserFactory pfactory= SAXParserFactory.newInstance();
                            pfactory.setNamespaceAware(true);
                            pfactory.setValidating(true);
                            pfactory.setXIncludeAware(true);
                            XMLReader reader = null;
                            try {
                                reader = pfactory.newSAXParser().getXMLReader();
                            } catch (Exception e) {
                                throw new TransformerException("Error creating SAX parser/reader", e);
                            }

                            // do the actual preprocessing fr includes
                            String fileName = "/home/hans/ofbiz/svn/applications/commonext/documents/ApacheOfbiz.xml";
                            SAXSource source = new SAXSource(reader, new InputSource(fileName));
                            // compile the xsl template
                            Transformer transformer1 = tfactory.newTransformer(new StreamSource("/home/hans/ofbiz/svn/applications/content/template/docbook/fo/docbook.xsl"));
                            // and apply the xsl template to the source document and save in a result string
                            StringWriter sw = new StringWriter();
                            StreamResult sr = new StreamResult(sw);
                            transformer1.transform(source, sr);
                            // store into a file for debugging
//                            java.io.FileWriter fw = new java.io.FileWriter(new java.io.File("/tmp/file1.fo"));
//                            fw.write(sw.toString());
//                            fw.close();

                            
                            FopFactory fopFactory = FopFactory.newInstance();                            
                            FOUserAgent foUserAgent = fopFactory.newFOUserAgent();
                            // configure foUserAgent as desired
                    
                            // Setup output stream.  Note: Using BufferedOutputStream
                            // for performance reasons (helpful with FileOutputStreams).
//                            OutputStream out = new FileOutputStream("/tmp/file.pdf");
//                            out = new BufferedOutputStream(out);
//                            OutputStream outWriter = (OutputStream) writer;
                            ByteArrayOutputStream out = new ByteArrayOutputStream();
                            Fop fop = fopFactory.newFop(MimeConstants.MIME_PDF, foUserAgent, out);

                            // Setup JAXP using identity transformer
                            TransformerFactory factory = TransformerFactory.newInstance();
                            Transformer transformer = factory.newTransformer(); // identity transformer
                            
                            // Setup input stream
                            Source src = new StreamSource(new ByteArrayInputStream(sw.toString().getBytes()));

                            // Resulting SAX events (the generated FO) must be piped through to FOP
                            Result result = new SAXResult(fop.getDefaultHandler());
                            
                            // Start XSLT transformation and FOP processing
                            transformer.transform(src, result);
                            
                            out.flush();
                            out.close();
                            // to a file for debugging
                           FileOutputStream fw = new FileOutputStream("/tmp/file.pdf");
                            fw.write(out.toByteArray());
                            fw.close();
                            HttpServletResponse response = (HttpServletResponse) context.get("response");
                            response.setContentType("application/pdf");
                            response.setContentLength(out.size());
                            response.setHeader("Content-Disposition", "attachment;filename=" + fileName);
                            writer.append(new String(out.toByteArray()));
                        } catch(Exception e) {
                            Debug.logError("Exception converting from FO to PDF: " + e, module);
                        }
                    } else {
*/                        screenStringRenderer.renderContentFrame(writer, context, this);
//                    }
                } else {
                    screenStringRenderer.renderContentBegin(writer, context, this);
                    screenStringRenderer.renderContentBody(writer, context, this);
                    screenStringRenderer.renderContentEnd(writer, context, this);
                }
                UtilGenerics.<MapStack<String>>cast(context).pop();
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

        public String getContentId(Map<String, Object> context) {
            return this.contentId.expandString(context);
        }

        public String getDataResourceId(Map<String, Object> context) {
            return this.dataResourceId.expandString(context);
        }

        public String getEditRequest(Map<String, Object> context) {
            return this.editRequest.expandString(context);
        }

        public String getEditContainerStyle(Map<String, Object> context) {
            return this.editContainerStyle.expandString(context);
        }

        public String getEnableEditName(Map<String, Object> context) {
            return this.enableEditName.expandString(context);
        }

        public boolean xmlEscape() {
            return this.xmlEscape;
        }

        @Override
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
            this.contentId = FlexibleStringExpander.getInstance(UtilFormatOut.checkNull(subContentElement.getAttribute("content-id")));
            this.mapKey = FlexibleStringExpander.getInstance(UtilFormatOut.checkNull(subContentElement.getAttribute("map-key")));
            if (this.mapKey.isEmpty()) {
                this.mapKey = FlexibleStringExpander.getInstance(UtilFormatOut.checkNull(subContentElement.getAttribute("assoc-name")));
            }
            this.editRequest = FlexibleStringExpander.getInstance(UtilFormatOut.checkNull(subContentElement.getAttribute("edit-request")));
            this.editContainerStyle = FlexibleStringExpander.getInstance(subContentElement.getAttribute("edit-container-style"));
            this.enableEditName = FlexibleStringExpander.getInstance(subContentElement.getAttribute("enable-edit-name"));
            this.xmlEscape = "true".equals(subContentElement.getAttribute("xml-escape"));
        }

        @Override
        public void renderWidgetString(Appendable writer, Map<String, Object> context, ScreenStringRenderer screenStringRenderer) {
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

        public String getContentId(Map<String, Object> context) {
            return this.contentId.expandString(context);
        }

        public String getMapKey(Map<String, Object> context) {
            return this.mapKey.expandString(context);
        }

        public String getEditRequest(Map<String, Object> context) {
            return this.editRequest.expandString(context);
        }

        public String getEditContainerStyle(Map<String, Object> context) {
            return this.editContainerStyle.expandString(context);
        }

        public String getEnableEditName(Map<String, Object> context) {
            return this.enableEditName.expandString(context);
        }

        public boolean xmlEscape() {
            return this.xmlEscape;
        }

        @Override
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

            this.nameExdr = FlexibleStringExpander.getInstance(menuElement.getAttribute("name"));
            this.locationExdr = FlexibleStringExpander.getInstance(menuElement.getAttribute("location"));
        }

        @Override
        public void renderWidgetString(Appendable writer, Map<String, Object> context, ScreenStringRenderer screenStringRenderer) throws IOException {
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

            ModelMenu modelMenu = getModelMenu(context);
            modelMenu.renderMenuString(writer, context, menuStringRenderer);
        }

        public ModelMenu getModelMenu(Map<String, Object> context) {
            String name = this.getName(context);
            String location = this.getLocation(context);
            ModelMenu modelMenu = null;
            try {
                modelMenu = MenuFactory.getMenuFromLocation(location, name, this.modelScreen.getDelegator(context), this.modelScreen.getDispatcher(context));
            } catch (Exception e) {
                String errMsg = "Error rendering included menu named [" + name + "] at location [" + location + "]: ";
                Debug.logError(e, errMsg, module);
                throw new RuntimeException(errMsg + e);
            }
            return modelMenu;
        }

        public String getName(Map<String, Object> context) {
            return this.nameExdr.expandString(context);
        }

        public String getLocation(Map<String, Object> context) {
            return this.locationExdr.expandString(context);
        }

        @Override
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
        protected String linkType;
        protected List<WidgetWorker.Parameter> parameterList = FastList.newInstance();


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

            this.linkType = linkElement.getAttribute("link-type");
            List<? extends Element> parameterElementList = UtilXml.childElementList(linkElement, "parameter");
            for (Element parameterElement: parameterElementList) {
                this.parameterList.add(new WidgetWorker.Parameter(parameterElement));
            }
        }

        @Override
        public void renderWidgetString(Appendable writer, Map<String, Object> context, ScreenStringRenderer screenStringRenderer) {
            try {
                screenStringRenderer.renderLink(writer, context, this);
            } catch (IOException e) {
                String errMsg = "Error rendering link with id [" + getId(context) + "]: " + e.toString();
                Debug.logError(e, errMsg, module);
                throw new RuntimeException(errMsg);
            }
        }

        public String getText(Map<String, Object> context) {
            String text = this.textExdr.expandString(context);
            StringUtil.SimpleEncoder simpleEncoder = (StringUtil.SimpleEncoder) context.get("simpleEncoder");
            if (simpleEncoder != null) {
                text = simpleEncoder.encode(text);
            }
            return text;
        }

        public String getId(Map<String, Object> context) {
            return this.idExdr.expandString(context);
        }

        public String getStyle(Map<String, Object> context) {
            return this.styleExdr.expandString(context);
        }

        public String getTarget(Map<String, Object> context) {
            Map<String, Object> expanderContext = context;
            StringUtil.SimpleEncoder simpleEncoder = context == null ? null : (StringUtil.SimpleEncoder) context.get("simpleEncoder");
            if (simpleEncoder != null) {
                expanderContext = StringUtil.HtmlEncodingMapWrapper.getHtmlEncodingMapWrapper(context, simpleEncoder);
            }
            return this.targetExdr.expandString(expanderContext);
        }

        public String getName(Map<String, Object> context) {
            return this.nameExdr.expandString(context);
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
        public void setName(String val) {
            this.nameExdr = FlexibleStringExpander.getInstance(val);
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

        @Override
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
        protected FlexibleStringExpander alt;
        protected String urlMode = "content";

        public Image(ModelScreen modelScreen, Element imageElement) {
            super(modelScreen, imageElement);

            setSrc(imageElement.getAttribute("src"));
            setId(imageElement.getAttribute("id"));
            setStyle(imageElement.getAttribute("style"));
            setWidth(imageElement.getAttribute("width"));
            setHeight(imageElement.getAttribute("height"));
            setBorder(imageElement.getAttribute("border"));
            setAlt(imageElement.getAttribute("alt"));
            setUrlMode(UtilFormatOut.checkEmpty(imageElement.getAttribute("url-mode"), "content"));
        }

        @Override
        public void renderWidgetString(Appendable writer, Map<String, Object> context, ScreenStringRenderer screenStringRenderer) {
            try {
                screenStringRenderer.renderImage(writer, context, this);
            } catch (IOException e) {
                String errMsg = "Error rendering image with id [" + getId(context) + "]: " + e.toString();
                Debug.logError(e, errMsg, module);
                throw new RuntimeException(errMsg);
            }
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

        public String getAlt(Map<String, Object> context) {
            return this.alt.expandString(context);
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
        public void setAlt(String val) {
            String altAttr = UtilFormatOut.checkNull(val);
            this.alt = FlexibleStringExpander.getInstance(altAttr);
        }

        public void setUrlMode(String val) {
            if (UtilValidate.isEmpty(val)) {
                this.urlMode = "content";
            } else {
                this.urlMode = val;
            }
        }

        @Override
        public String rawString() {
            // may want to add more to this
            return "<image id=\"" + this.idExdr.getOriginal() + "\" style=\"" + this.styleExdr.getOriginal() + "\" src=\"" + this.srcExdr.getOriginal() + "\" url-mode=\"" + this.urlMode + "\"/>";
        }
    }
}














