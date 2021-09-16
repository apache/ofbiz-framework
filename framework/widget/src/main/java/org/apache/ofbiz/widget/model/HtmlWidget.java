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
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import javax.servlet.http.HttpServletRequest;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.StringUtil;
import org.apache.ofbiz.base.util.UtilCodec;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilHtml;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.UtilXml;
import org.apache.ofbiz.base.util.cache.UtilCache;
import org.apache.ofbiz.base.util.collections.MapStack;
import org.apache.ofbiz.base.util.string.FlexibleStringExpander;
import org.apache.ofbiz.base.util.template.FreeMarkerWorker;
import org.apache.ofbiz.widget.renderer.ScreenRenderer;
import org.apache.ofbiz.widget.renderer.ScreenStringRenderer;
import org.apache.ofbiz.widget.renderer.html.HtmlWidgetRenderer;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.parser.ParseError;
import org.jsoup.select.Elements;
import org.w3c.dom.Element;

import freemarker.ext.beans.BeansWrapper;
import freemarker.ext.beans.CollectionModel;
import freemarker.ext.beans.StringModel;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.Version;

/**
 * Widget Library - Screen model HTML class.
 */
@SuppressWarnings("serial")
public class HtmlWidget extends ModelScreenWidget {
    private static final String MODULE = HtmlWidget.class.getName();

    private static final UtilCache<String, Template> SPECIAL_TEMPLATE_CACHE =
            UtilCache.createUtilCache("widget.screen.template.ftl.general", 0, 0, false);
    protected static final Configuration SPECIAL_CONFIG = FreeMarkerWorker.makeConfiguration(new ExtendedWrapper(FreeMarkerWorker.VERSION));
    private static final Configuration SPECIAL_CONFIG_SQUARE_INTERPOLATION =
            FreeMarkerWorker.makeConfiguration(new ExtendedWrapper(FreeMarkerWorker.VERSION));
    static {
        SPECIAL_CONFIG_SQUARE_INTERPOLATION.setInterpolationSyntax(Configuration.SQUARE_BRACKET_INTERPOLATION_SYNTAX);
    }
    // not sure if this is the best way to get FTL to use my fancy MapModel derivative, but should work at least...
    public static class ExtendedWrapper extends BeansWrapper {
        public ExtendedWrapper(Version version) {
            super(version);
        }

        @Override
        public TemplateModel wrap(Object object) throws TemplateModelException {
            // This StringHtmlWrapperForFtl option seems to be the best option
            // and handles most things without causing too many problems
            if (object instanceof String) {
                return new StringHtmlWrapperForFtl((String) object, this);
            } else if (object instanceof Collection && !(object instanceof Map)) {
                // An additional wrapper to ensure ${aCollection} is properly encoded for html
                return new CollectionHtmlWrapperForFtl((Collection<?>) object, this);
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
            return UtilCodec.getEncoder("html").encode(super.getAsString());
        }
    }

    public static class CollectionHtmlWrapperForFtl extends CollectionModel {

        public CollectionHtmlWrapperForFtl(Collection<?> collection, BeansWrapper wrapper) {
            super(collection, wrapper);
        }

        @Override
        public String getAsString() {
            return UtilCodec.getEncoder("html").encode(super.getAsString());
        }

    }

    // End Static, begin class section

    private final List<ModelScreenWidget> subWidgets;

    public HtmlWidget(ModelScreen modelScreen, Element htmlElement) {
        super(modelScreen, htmlElement);
        List<? extends Element> childElementList = UtilXml.childElementList(htmlElement);
        if (childElementList.isEmpty()) {
            this.subWidgets = Collections.emptyList();
        } else {
            List<ModelScreenWidget> subWidgets = new ArrayList<>(childElementList.size());
            for (Element childElement : childElementList) {
                String childNodeName = childElement.getNodeName().contains(":")
                        ? StringUtil.split(childElement.getNodeName(), ":").get(1)
                        : childElement.getNodeName();
                switch (childNodeName) {
                case "html-template":
                    subWidgets.add(new HtmlTemplate(modelScreen, childElement));
                    break;
                case "html-template-decorator":
                    subWidgets.add(new HtmlTemplateDecorator(modelScreen, childElement));
                    break;
                default:
                    throw new IllegalArgumentException("Tag not supported under the platform-specific -> html tag with name: "
                            + childElement.getNodeName());
                }
            }
            this.subWidgets = Collections.unmodifiableList(subWidgets);
        }
    }

    /**
     * Gets sub widgets.
     * @return the sub widgets
     */
    public List<ModelScreenWidget> getSubWidgets() {
        return subWidgets;
    }

    @Override
    public void renderWidgetString(Appendable writer, Map<String, Object> context, ScreenStringRenderer screenStringRenderer)
            throws GeneralException, IOException {
        for (ModelScreenWidget subWidget : subWidgets) {
            subWidget.renderWidgetString(writer, context, screenStringRenderer);
        }
    }

    public static void renderHtmlTemplate(Appendable writer, FlexibleStringExpander locationExdr, Map<String, Object> context) {
        String location = locationExdr.expandString(context);

        if (UtilValidate.isEmpty(location)) {
            throw new IllegalArgumentException("Template location is empty with search string location " + locationExdr.getOriginal());
        }

        if (location.endsWith(".ftl")) {
            try {
                boolean insertWidgetBoundaryComments = ModelWidget.widgetBoundaryCommentsEnabled(context);
                if (insertWidgetBoundaryComments) {
                    writer.append(HtmlWidgetRenderer.buildBoundaryComment("Begin", "Template", location));
                }
                if (HtmlWidgetRenderer.NAMED_BORDER_TYPE != ModelWidget.NamedBorderType.NONE && !location.endsWith(".fo.ftl")) {
                    HttpServletRequest request = ((HttpServletRequest) context.get("request"));
                    writer.append(HtmlWidgetRenderer.beginNamedBorder("Template", location, request.getContextPath()));
                }
                Template template = null;
                if (location.endsWith(".fo.ftl")) { // FOP can't render correctly escaped characters
                    template = FreeMarkerWorker.getTemplate(location);
                } else {
                    if (location.endsWith(".sqi.ftl")) {
                        template = FreeMarkerWorker.getTemplate(location, SPECIAL_TEMPLATE_CACHE, SPECIAL_CONFIG_SQUARE_INTERPOLATION);
                    } else {
                        template = FreeMarkerWorker.getTemplate(location, SPECIAL_TEMPLATE_CACHE, SPECIAL_CONFIG);
                    }
                }
                FreeMarkerWorker.renderTemplate(template, context, writer);

                if (HtmlWidgetRenderer.NAMED_BORDER_TYPE != ModelWidget.NamedBorderType.NONE && !location.endsWith(".fo.ftl")) {
                    writer.append(HtmlWidgetRenderer.endNamedBorder("Template", location));
                }
                if (insertWidgetBoundaryComments) {
                    writer.append(HtmlWidgetRenderer.buildBoundaryComment("End", "Template", location));
                }
            } catch (IllegalArgumentException | TemplateException | IOException e) {
                String errMsg = "Error rendering included template at location [" + location + "]: " + e.toString();
                Debug.logError(e, errMsg, MODULE);
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
        private FlexibleStringExpander locationExdr;
        private boolean multiBlock;
        private String inlineFTL;

        public HtmlTemplate(ModelScreen modelScreen, Element htmlTemplateElement) {
            super(modelScreen, htmlTemplateElement);
            this.locationExdr = FlexibleStringExpander.getInstance(htmlTemplateElement.getAttribute("location"));
            this.multiBlock = !"false".equals(htmlTemplateElement.getAttribute("multi-block"));
            this.inlineFTL = htmlTemplateElement.getTextContent();
        }

        /**
         * Gets location.
         * @param context the context
         * @return the location
         */
        public String getLocation(Map<String, Object> context) {
            return locationExdr.expandString(context);
        }

        /**
         * Is multi block boolean.
         * @return the boolean
         */
        public boolean isMultiBlock() {
            return this.multiBlock;
        }

        @Override
        public void renderWidgetString(Appendable writer, Map<String, Object> context, ScreenStringRenderer screenStringRenderer) throws IOException {

            if (UtilValidate.isNotEmpty(this.inlineFTL)) {
                try {
                    FreeMarkerWorker.renderTemplateFromString("", this.inlineFTL, context, writer, 0, false);
                } catch (TemplateException | IOException e) {
                    String errMsg = "Error rendering [" + this.inlineFTL + "]: " + e.toString();
                    Debug.logError(e, errMsg, MODULE);
                    writeError(writer, errMsg);
                }
                return;
            }

            if (!isMultiBlock() && !Debug.verboseOn()) {
                renderHtmlTemplate(writer, locationExdr, context);
                return;
            }

            /**
             * We use stack to store the string writer because a freemarker template may also render a sub screen
             * widget by using ${screens.render(link to the screen)}. So before rendering the sub screen widget,
             * ScreenRenderer class will check for the existence of the stack and retrieve the correct string writer.
             * Inline script tags are removed from the final rendering, if multi-block = true
             */
            String location = locationExdr.expandString(context);
            StringWriter stringWriter = new StringWriter();
            Stack<StringWriter> stringWriterStack = UtilGenerics.cast(context.get(ScriptLinkHelper.FTL_WRITER));
            if (stringWriterStack == null) {
                stringWriterStack = new Stack<>();
            }
            stringWriterStack.push(stringWriter);
            context.put(ScriptLinkHelper.FTL_WRITER, stringWriterStack);
            renderHtmlTemplate(stringWriter, locationExdr, context);
            stringWriterStack.pop();
            // check if no more parent freemarker template before removing from context
            if (stringWriterStack.empty()) {
                context.remove(ScriptLinkHelper.FTL_WRITER);
            }
            String data = stringWriter.toString();
            stringWriter.close();

            if (Debug.verboseOn()) {
                List<String> themeBasePathsToExempt = UtilHtml.getVisualThemeFolderNamesToExempt();
                if (!themeBasePathsToExempt.stream().anyMatch(location::contains)) {
                    // check for unclosed tags
                    List<String> errorList = UtilHtml.hasUnclosedTag(data);
                    if (UtilValidate.isNotEmpty(errorList)) {
                        errorList.forEach(a -> UtilHtml.logHtmlWarning(data, location, a, MODULE));
                        // check with JSoup Html Parser
                        List<ParseError> errList = UtilHtml.validateHtmlFragmentWithJSoup(data);
                        if (UtilValidate.isNotEmpty(errList)) {
                            errList.forEach(a -> UtilHtml.logHtmlWarning(data, location, a.toString(), MODULE));
                        }
                    }
                }
            }

            if (isMultiBlock()) {
                Document doc = Jsoup.parseBodyFragment(data);
                // extract js script tags
                Elements scriptElements = doc.select("script");
                if (scriptElements != null && scriptElements.size() > 0) {
                    StringBuilder scripts = new StringBuilder();
                    for (org.jsoup.nodes.Element script : scriptElements) {
                        String type = script.attr("type");
                        String src = script.attr("src");
                        if (UtilValidate.isEmpty(src)) {
                            if (UtilValidate.isEmpty(type) || "application/javascript".equals(type)) {
                                scripts.append(script.data());
                                script.remove();
                            }
                        }
                    }

                    if (scripts.length() > 0) {
                        // store script for retrieval by the browser
                        String fileName = location;
                        fileName = fileName.substring(fileName.lastIndexOf('/') + 1);
                        if (fileName.endsWith(".ftl")) {
                            fileName = fileName.substring(0, fileName.length() - 4);
                        }
                        HttpServletRequest request = (HttpServletRequest) context.get("request");
                        ScriptLinkHelper.prepareScriptLinkForBodyEnd(request, fileName, scripts.toString());
                    }
                }
                // the 'template' block
                String body = doc.body().html();
                writer.append(body);
            } else {
                writer.append(data);
            }
        }

        @Override
        public void accept(ModelWidgetVisitor visitor) throws Exception {
            visitor.visit(this);
        }

        /**
         * Gets location exdr.
         * @return the location exdr
         */
        public FlexibleStringExpander getLocationExdr() {
            return locationExdr;
        }
    }

    public static class HtmlTemplateDecorator extends ModelScreenWidget {
        private FlexibleStringExpander locationExdr;
        private Map<String, ModelScreenWidget> sectionMap = new HashMap<>();

        public HtmlTemplateDecorator(ModelScreen modelScreen, Element htmlTemplateDecoratorElement) {
            super(modelScreen, htmlTemplateDecoratorElement);
            this.locationExdr = FlexibleStringExpander.getInstance(htmlTemplateDecoratorElement.getAttribute("location"));

            List<? extends Element> htmlTemplateDecoratorSectionElementList = UtilXml.childElementList(
                    htmlTemplateDecoratorElement, "html-template-decorator-section");
            for (Element htmlTemplateDecoratorSectionElement: htmlTemplateDecoratorSectionElementList) {
                String name = htmlTemplateDecoratorSectionElement.getAttribute("name");
                this.sectionMap.put(name, new HtmlTemplateDecoratorSection(modelScreen, htmlTemplateDecoratorSectionElement));
            }
        }

        @Override
        public void renderWidgetString(Appendable writer, Map<String, Object> context, ScreenStringRenderer screenStringRenderer) {
            // isolate the scope
            MapStack<String> contextMs;
            if (!(context instanceof MapStack<?>)) {
                contextMs = MapStack.create(context);
                context = contextMs;
            } else {
                contextMs = UtilGenerics.cast(context);
            }

            // create a standAloneStack, basically a "save point" for this SectionsRenderer,
            // and make a new "screens" object just for it so it is isolated and doesn't follow the stack down
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
        public void accept(ModelWidgetVisitor visitor) throws Exception {
            visitor.visit(this);
        }

        /**
         * Gets location exdr.
         * @return the location exdr
         */
        public FlexibleStringExpander getLocationExdr() {
            return locationExdr;
        }

        /**
         * Gets section map.
         * @return the section map
         */
        public Map<String, ModelScreenWidget> getSectionMap() {
            return sectionMap;
        }
    }

    public static class HtmlTemplateDecoratorSection extends ModelScreenWidget {
        private List<ModelScreenWidget> subWidgets;

        public HtmlTemplateDecoratorSection(ModelScreen modelScreen, Element htmlTemplateDecoratorSectionElement) {
            super(modelScreen, htmlTemplateDecoratorSectionElement);
            List<? extends Element> subElementList = UtilXml.childElementList(htmlTemplateDecoratorSectionElement);
            this.subWidgets = ModelScreenWidget.readSubWidgets(getModelScreen(), subElementList);
        }

        @Override
        public void renderWidgetString(Appendable writer, Map<String, Object> context,
                                       ScreenStringRenderer screenStringRenderer) throws GeneralException, IOException {
            // render sub-widgets
            renderSubWidgetsString(this.subWidgets, writer, context, screenStringRenderer);
        }

        @Override
        public void accept(ModelWidgetVisitor visitor) throws Exception {
            visitor.visit(this);
        }

        /**
         * Gets sub widgets.
         * @return the sub widgets
         */
        public List<ModelScreenWidget> getSubWidgets() {
            return subWidgets;
        }
    }

    @Override
    public void accept(ModelWidgetVisitor visitor) throws Exception {
        visitor.visit(this);
    }
}
