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
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javolution.util.FastMap;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.StringUtil;
import org.ofbiz.base.util.UtilGenerics;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.UtilXml;
import org.ofbiz.base.util.cache.UtilCache;
import org.ofbiz.base.util.collections.MapStack;
import org.ofbiz.base.util.string.FlexibleStringExpander;
import org.ofbiz.base.util.template.FreeMarkerWorker;
import org.ofbiz.widget.ModelWidget;
import org.ofbiz.widget.html.HtmlWidgetRenderer;
import org.w3c.dom.Element;

import freemarker.ext.beans.BeansWrapper;
import freemarker.ext.beans.StringModel;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;

/**
 * Widget Library - Screen model HTML class.
 */
@SuppressWarnings("serial")
public class HtmlWidget extends ModelScreenWidget {
    public static final String module = HtmlWidget.class.getName();

    public static UtilCache<String, Template> specialTemplateCache = UtilCache.createUtilCache("widget.screen.template.ftl.general", 0, 0, false);
    protected static BeansWrapper specialBeansWrapper = new ExtendedWrapper();
    protected static Configuration specialConfig = FreeMarkerWorker.makeConfiguration(specialBeansWrapper);

    // not sure if this is the best way to get FTL to use my fancy MapModel derivative, but should work at least...
    public static class ExtendedWrapper extends BeansWrapper {
        @Override
        public TemplateModel wrap(Object object) throws TemplateModelException {
            /* NOTE: don't use this and the StringHtmlWrapperForFtl or things will be double-encoded
            if (object instanceof GenericValue) {
                return new GenericValueHtmlWrapperForFtl((GenericValue) object, this);
            }*/
            // This StringHtmlWrapperForFtl option seems to be the best option
            // and handles most things without causing too many problems
            if (object instanceof String) {
                return new StringHtmlWrapperForFtl((String) object, this);
            }
            return super.wrap(object);
        }
    }

    public static class StringHtmlWrapperForFtl extends StringModel {
        public StringHtmlWrapperForFtl(String str, BeansWrapper wrapper) {
            super(str, wrapper);
        }
        @Override
        public String getAsString() {
            return StringUtil.htmlEncoder.encode(super.getAsString());
        }
    }

    // End Static, begin class section

    protected List<ModelScreenWidget> subWidgets = new ArrayList<ModelScreenWidget>();

    public HtmlWidget(ModelScreen modelScreen, Element htmlElement) {
        super(modelScreen, htmlElement);
        List<? extends Element> childElementList = UtilXml.childElementList(htmlElement);
        for (Element childElement : childElementList) {
            if ("html-template".equals(childElement.getNodeName())) {
                this.subWidgets.add(new HtmlTemplate(modelScreen, childElement));
            } else if ("html-template-decorator".equals(childElement.getNodeName())) {
                this.subWidgets.add(new HtmlTemplateDecorator(modelScreen, childElement));
            } else {
                throw new IllegalArgumentException("Tag not supported under the platform-specific -> html tag with name: " + childElement.getNodeName());
            }
        }
    }

    @Override
    public void renderWidgetString(Appendable writer, Map<String, Object> context, ScreenStringRenderer screenStringRenderer) throws GeneralException, IOException {
        for (ModelScreenWidget subWidget : subWidgets) {
            subWidget.renderWidgetString(writer, context, screenStringRenderer);
        }
    }

    @Override
    public String rawString() {
        StringBuilder buffer = new StringBuilder("<html-widget>");
        for (ModelScreenWidget subWidget : subWidgets) {
            buffer.append(subWidget.rawString());
        }
        buffer.append("</html-widget>");
        return buffer.toString();
    }

    public static void renderHtmlTemplate(Appendable writer, FlexibleStringExpander locationExdr, Map<String, Object> context) {
        String location = locationExdr.expandString(context);
        //Debug.logInfo("Rendering template at location [" + location + "] with context: \n" + context, module);

        if (UtilValidate.isEmpty(location)) {
            throw new IllegalArgumentException("Template location is empty");
        }


        /*
        // =======================================================================
        // Go through the context and find GenericValue objects and wrap them

        // NOTE PROBLEM: there are still problems with this as it gets some things
        // but does not get non-entity data including lots of strings
        // directly in the context or things prepared or derived right in
        // the FTL file, like the results of service calls, etc; we could
        // do something more aggressive to encode and wrap EVERYTHING in
        // the context, but I've been thinking that even this is too much
        // overhead and that would be crazy

        // NOTE ALTERNATIVE1: considering instead to use the FTL features to wrap
        // everything in an <#escape x as x?html>...</#escape>, but that could
        // cause problems with ${} expansions that have HTML in them, including:
        // included screens (using ${screens.render(...)}), content that should
        // have HTML in it (lots of general, product, category, etc content), etc

        // NOTE ALTERNATIVE2: kind of like the "#escape X as x?html" option,
        // implement an FTL *Model class and load it through a ObjectWrapper
        // FINAL NOTE: after testing all of these alternatives, this one seems
        // to behave the best, so going with that for now.

        // isolate the scope so these wrapper objects go away after rendering is done
        MapStack<String> contextMs;
        if (!(context instanceof MapStack)) {
            contextMs = MapStack.create(context);
            context = contextMs;
        } else {
            contextMs = UtilGenerics.cast(context);
        }

        contextMs.push();
        for(Map.Entry<String, Object> mapEntry: contextMs.entrySet()) {
            Object value = mapEntry.getValue();
            if (value instanceof GenericValue) {
                contextMs.put(mapEntry.getKey(), GenericValueHtmlWrapper.create((GenericValue) value));
            } else if (value instanceof List) {
                if (((List) value).size() > 0 && ((List) value).get(0) instanceof GenericValue) {
                    List<GenericValue> theList = (List<GenericValue>) value;
                    List<GenericValueHtmlWrapper> newList = FastList.newInstance();
                    for (GenericValue gv: theList) {
                        newList.add(GenericValueHtmlWrapper.create(gv));
                    }
                    contextMs.put(mapEntry.getKey(), newList);
                }
            }
            // TODO and NOTE: should get most stuff, but we could support Maps
            // and Lists in Maps and such; that's tricky because we have to go
            // through the entire Map and not just one entry, and we would
            // have to shallow copy the whole Map too

        }
        // this line goes at the end of the method, but moved up here to be part of the big comment about this
        contextMs.pop();
         */

        if (location.endsWith(".ftl")) {
            try {
                Map<String, ? extends Object> parameters = UtilGenerics.checkMap(context.get("parameters"));
                boolean insertWidgetBoundaryComments = ModelWidget.widgetBoundaryCommentsEnabled(parameters);
                if (insertWidgetBoundaryComments) {
                    writer.append(HtmlWidgetRenderer.formatBoundaryComment("Begin", "Template", location));
                }

                //FreeMarkerWorker.renderTemplateAtLocation(location, context, writer);
                Template template = null;
                if (location.endsWith(".fo.ftl")) { // FOP can't render correctly escaped characters
                    template = FreeMarkerWorker.getTemplate(location);
                } else {
                    template = FreeMarkerWorker.getTemplate(location, specialTemplateCache, specialConfig);
                }
                FreeMarkerWorker.renderTemplate(template, context, writer);

                if (insertWidgetBoundaryComments) {
                    writer.append(HtmlWidgetRenderer.formatBoundaryComment("End", "Template", location));
                }
            } catch (IllegalArgumentException e) {
                String errMsg = "Error rendering included template at location [" + location + "]: " + e.toString();
                Debug.logError(e, errMsg, module);
                writeError(writer, errMsg);
            } catch (MalformedURLException e) {
                String errMsg = "Error rendering included template at location [" + location + "]: " + e.toString();
                Debug.logError(e, errMsg, module);
                writeError(writer, errMsg);
            } catch (TemplateException e) {
                String errMsg = "Error rendering included template at location [" + location + "]: " + e.toString();
                Debug.logError(e, errMsg, module);
                writeError(writer, errMsg);
            } catch (IOException e) {
                String errMsg = "Error rendering included template at location [" + location + "]: " + e.toString();
                Debug.logError(e, errMsg, module);
                writeError(writer, errMsg);
            }
        } else {
            throw new IllegalArgumentException("Rendering not yet supported for the template at location: " + location);
        }
    }

    // TODO: We can make this more fancy, but for now this is very functional
    public static void writeError(Appendable writer, String message) {
        try {
            writer.append(message);
        } catch (IOException e) {
        }
    }

    public static class HtmlTemplate extends ModelScreenWidget {
        protected FlexibleStringExpander locationExdr;

        public HtmlTemplate(ModelScreen modelScreen, Element htmlTemplateElement) {
            super(modelScreen, htmlTemplateElement);
            this.locationExdr = FlexibleStringExpander.getInstance(htmlTemplateElement.getAttribute("location"));
        }

        @Override
        public void renderWidgetString(Appendable writer, Map<String, Object> context, ScreenStringRenderer screenStringRenderer) {
            renderHtmlTemplate(writer, this.locationExdr, context);
        }

        @Override
        public String rawString() {
            return "<html-template location=\"" + this.locationExdr.getOriginal() + "\"/>";
        }
    }

    public static class HtmlTemplateDecorator extends ModelScreenWidget {
        protected FlexibleStringExpander locationExdr;
        protected Map<String, HtmlTemplateDecoratorSection> sectionMap = FastMap.newInstance();

        public HtmlTemplateDecorator(ModelScreen modelScreen, Element htmlTemplateDecoratorElement) {
            super(modelScreen, htmlTemplateDecoratorElement);
            this.locationExdr = FlexibleStringExpander.getInstance(htmlTemplateDecoratorElement.getAttribute("location"));

            List<? extends Element> htmlTemplateDecoratorSectionElementList = UtilXml.childElementList(htmlTemplateDecoratorElement, "html-template-decorator-section");
            for (Element htmlTemplateDecoratorSectionElement: htmlTemplateDecoratorSectionElementList) {
                String name = htmlTemplateDecoratorSectionElement.getAttribute("name");
                this.sectionMap.put(name, new HtmlTemplateDecoratorSection(modelScreen, htmlTemplateDecoratorSectionElement));
            }
        }

        @Override
        public void renderWidgetString(Appendable writer, Map<String, Object> context, ScreenStringRenderer screenStringRenderer) {
            // isolate the scope
            MapStack<String> contextMs;
            if (!(context instanceof MapStack)) {
                contextMs = MapStack.create(context);
                context = contextMs;
            } else {
                contextMs = UtilGenerics.cast(context);
            }

            // create a standAloneStack, basically a "save point" for this SectionsRenderer, and make a new "screens" object just for it so it is isolated and doesn't follow the stack down
            MapStack<String> standAloneStack = contextMs.standAloneChildStack();
            standAloneStack.put("screens", new ScreenRenderer(writer, standAloneStack, screenStringRenderer));
            SectionsRenderer sections = new SectionsRenderer(this.sectionMap, standAloneStack, writer, screenStringRenderer);

            // put the sectionMap in the context, make sure it is in the sub-scope, ie after calling push on the MapStack
            contextMs.push();
            context.put("sections", sections);

            renderHtmlTemplate(writer, this.locationExdr, context);
            contextMs.pop();
        }

        @Override
        public String rawString() {
            return "<html-template-decorator location=\"" + this.locationExdr.getOriginal() + "\"/>";
        }
    }

    public static class HtmlTemplateDecoratorSection extends ModelScreenWidget {
        protected String name;
        protected List<ModelScreenWidget> subWidgets;

        public HtmlTemplateDecoratorSection(ModelScreen modelScreen, Element htmlTemplateDecoratorSectionElement) {
            super(modelScreen, htmlTemplateDecoratorSectionElement);
            this.name = htmlTemplateDecoratorSectionElement.getAttribute("name");
            // read sub-widgets
            List<? extends Element> subElementList = UtilXml.childElementList(htmlTemplateDecoratorSectionElement);
            this.subWidgets = ModelScreenWidget.readSubWidgets(this.modelScreen, subElementList);
        }

        @Override
        public void renderWidgetString(Appendable writer, Map<String, Object> context, ScreenStringRenderer screenStringRenderer) throws GeneralException, IOException {
            // render sub-widgets
            renderSubWidgetsString(this.subWidgets, writer, context, screenStringRenderer);
        }

        @Override
        public String rawString() {
            return "<html-template-decorator-section name=\"" + this.name + "\"/>";
        }
    }
}
