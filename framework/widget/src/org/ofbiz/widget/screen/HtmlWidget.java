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
import java.io.Writer;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilXml;
import org.ofbiz.base.util.collections.MapStack;
import org.ofbiz.base.util.string.FlexibleStringExpander;
import org.ofbiz.base.util.template.FreeMarkerWorker;
import org.w3c.dom.Element;

import freemarker.template.TemplateException;

/**
 * Widget Library - Screen model HTML class
 */
public class HtmlWidget extends ModelScreenWidget {
    public static final String module = HtmlWidget.class.getName();
    
    protected ModelScreenWidget childWidget;
    
    public HtmlWidget(ModelScreen modelScreen, Element htmlElement) {
        super(modelScreen, htmlElement);
        List childElementList = UtilXml.childElementList(htmlElement);
        Iterator childElementIter = childElementList.iterator();
        while (childElementIter.hasNext()) {
            Element childElement = (Element) childElementIter.next();
            if ("html-template".equals(childElement.getNodeName())) {
                this.childWidget = new HtmlTemplate(modelScreen, childElement);
            } else if ("html-template-decorator".equals(childElement.getNodeName())) {
                this.childWidget = new HtmlTemplateDecorator(modelScreen, childElement);
            } else {
                throw new IllegalArgumentException("Tag not supported under the platform-specific -> html tag with name: " + childElement.getNodeName());
            }
        }
    }

    public void renderWidgetString(Writer writer, Map context, ScreenStringRenderer screenStringRenderer) throws GeneralException {
        childWidget.renderWidgetString(writer, context, screenStringRenderer);
    }

    public String rawString() {
        return "<html-widget>" + (this.childWidget==null?"":this.childWidget.rawString());
    }
    
    public static void renderHtmlTemplate(Writer writer, FlexibleStringExpander locationExdr, Map context) {
        String location = locationExdr.expandString(context);
        //Debug.logInfo("Rendering template at location [" + location + "] with context: \n" + context, module);
        
        if (location.endsWith(".ftl")) {
            try {
                FreeMarkerWorker.renderTemplateAtLocation(location, context, writer);
            } catch (MalformedURLException e) {
                String errMsg = "Error rendering included template at location [" + location + "]: " + e.toString();
                Debug.logError(e, errMsg, module);
                throw new RuntimeException(errMsg);
            } catch (TemplateException e) {
                String errMsg = "Error rendering included template at location [" + location + "]: " + e.toString();
                Debug.logError(e, errMsg, module);
                throw new RuntimeException(errMsg);
            } catch (IOException e) {
                String errMsg = "Error rendering included template at location [" + location + "]: " + e.toString();
                Debug.logError(e, errMsg, module);
                throw new RuntimeException(errMsg);
            }
        } else {
            throw new IllegalArgumentException("Rending not yet support for the tempalte at location: " + location);
        }
    }
    
    public static class HtmlTemplate extends ModelScreenWidget {
        protected FlexibleStringExpander locationExdr;
        
        public HtmlTemplate(ModelScreen modelScreen, Element htmlTemplateElement) {
            super(modelScreen, htmlTemplateElement);
            this.locationExdr = new FlexibleStringExpander(htmlTemplateElement.getAttribute("location"));
        }

        public void renderWidgetString(Writer writer, Map context, ScreenStringRenderer screenStringRenderer) {
            renderHtmlTemplate(writer, this.locationExdr, context);
        }

        public String rawString() {
            return "<html-template location=\"" + this.locationExdr.getOriginal() + "\"/>";
        }
    }

    public static class HtmlTemplateDecorator extends ModelScreenWidget {
        protected FlexibleStringExpander locationExdr;
        protected Map sectionMap = new HashMap();
        
        public HtmlTemplateDecorator(ModelScreen modelScreen, Element htmlTemplateDecoratorElement) {
            super(modelScreen, htmlTemplateDecoratorElement);
            this.locationExdr = new FlexibleStringExpander(htmlTemplateDecoratorElement.getAttribute("location"));
            
            List htmlTemplateDecoratorSectionElementList = UtilXml.childElementList(htmlTemplateDecoratorElement, "html-template-decorator-section");
            Iterator htmlTemplateDecoratorSectionElementIter = htmlTemplateDecoratorSectionElementList.iterator();
            while (htmlTemplateDecoratorSectionElementIter.hasNext()) {
                Element htmlTemplateDecoratorSectionElement = (Element) htmlTemplateDecoratorSectionElementIter.next();
                String name = htmlTemplateDecoratorSectionElement.getAttribute("name");
                this.sectionMap.put(name, new HtmlTemplateDecoratorSection(modelScreen, htmlTemplateDecoratorSectionElement));
            }
        }

        public void renderWidgetString(Writer writer, Map context, ScreenStringRenderer screenStringRenderer) {
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

            renderHtmlTemplate(writer, this.locationExdr, context);
        }

        public String rawString() {
            return "<html-template-decorator location=\"" + this.locationExdr.getOriginal() + "\"/>";
        }
    }

    public static class HtmlTemplateDecoratorSection extends ModelScreenWidget {
        protected String name;
        protected List subWidgets;
        
        public HtmlTemplateDecoratorSection(ModelScreen modelScreen, Element htmlTemplateDecoratorSectionElement) {
            super(modelScreen, htmlTemplateDecoratorSectionElement);
            this.name = htmlTemplateDecoratorSectionElement.getAttribute("name");
            // read sub-widgets
            List subElementList = UtilXml.childElementList(htmlTemplateDecoratorSectionElement);
            this.subWidgets = ModelScreenWidget.readSubWidgets(this.modelScreen, subElementList);
        }

        public void renderWidgetString(Writer writer, Map context, ScreenStringRenderer screenStringRenderer) throws GeneralException {
            // render sub-widgets
            renderSubWidgetsString(this.subWidgets, writer, context, screenStringRenderer);
        }

        public String rawString() {
            return "<html-template-decorator-section name=\"" + this.name + "\"/>";
        }
    }
}

