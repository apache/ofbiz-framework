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
package org.apache.ofbiz.widget.renderer.macro;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.StringUtil;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.template.FreeMarkerWorker;
import org.apache.ofbiz.webapp.control.RequestHandler;
import org.apache.ofbiz.webapp.taglib.ContentUrlTag;
import org.apache.ofbiz.widget.WidgetWorker;
import org.apache.ofbiz.widget.model.ModelTree;
import org.apache.ofbiz.widget.model.ModelWidget;
import org.apache.ofbiz.widget.renderer.ScreenRenderer;
import org.apache.ofbiz.widget.renderer.ScreenStringRenderer;
import org.apache.ofbiz.widget.renderer.TreeStringRenderer;

import freemarker.core.Environment;
import freemarker.template.Template;
import freemarker.template.TemplateException;

/**
 * Widget Library - Tree Renderer implementation based on Freemarker macros
 *
 */
public class MacroTreeRenderer implements TreeStringRenderer {

    public static final String module = MacroTreeRenderer.class.getName();
    private Template macroLibrary;
    private Environment environment;


    public MacroTreeRenderer(String macroLibraryPath, Appendable writer) throws TemplateException, IOException {
        this.macroLibrary = FreeMarkerWorker.getTemplate(macroLibraryPath);
        Map<String, Object> input = UtilMisc.toMap("key", null);
        this.environment = FreeMarkerWorker.renderTemplate(this.macroLibrary, input, writer);
    }

    private void executeMacro(String macro) {
        try {
            Reader templateReader = new StringReader(macro);
            // FIXME: I am using a Date as an hack to provide a unique name for the template...
            Template template = new Template((new java.util.Date()).toString(), templateReader,
                    FreeMarkerWorker.getDefaultOfbizConfig());
            templateReader.close();
            this.environment.include(template);
        } catch (TemplateException | IOException e) {
            Debug.logError(e, "Error rendering tree thru ftl", module);
        }
    }

    /**
     * Renders the beginning boundary comment string.
     * @param writer The writer to write to
     * @param widgetType The widget type: "Screen Widget", "Tree Widget", etc.
     * @param modelWidget The widget
     */
    public void renderBeginningBoundaryComment(Appendable writer, String widgetType, ModelWidget modelWidget) {
        StringWriter sr = new StringWriter();
        sr.append("<@formatBoundaryComment ");
        sr.append(" boundaryType=\"");
        sr.append("Begin");
        sr.append("\" widgetType=\"");
        sr.append(widgetType);
        sr.append("\" widgetName=\"");
        sr.append(modelWidget.getBoundaryCommentName());
        sr.append("\" />");
        executeMacro(sr.toString());
    }

    /**
     * Renders the ending boundary comment string.
     * @param writer The writer to write to
     * @param widgetType The widget type: "Screen Widget", "Tree Widget", etc.
     * @param modelWidget The widget
     */
    public void renderEndingBoundaryComment(Appendable writer, String widgetType, ModelWidget modelWidget) {
        StringWriter sr = new StringWriter();
        sr.append("<@formatBoundaryComment ");
        sr.append(" boundaryType=\"");
        sr.append("End");
        sr.append("\" widgetType=\"");
        sr.append(widgetType);
        sr.append("\" widgetName=\"");
        sr.append(modelWidget.getBoundaryCommentName());
        sr.append("\" />");
        executeMacro(sr.toString());
    }

    @Override
    public void renderNodeBegin(Appendable writer, Map<String, Object> context, ModelTree.ModelNode node, int depth) throws IOException {
        String currentNodeTrailPiped = null;
        Object obj = context.get("currentNodeTrail");
        List<String> currentNodeTrail = (obj instanceof List) ? UtilGenerics.cast(obj) : null;

        String style = "";
        if (node.isRootNode()) {
            if (ModelWidget.widgetBoundaryCommentsEnabled(context)) {
                renderBeginningBoundaryComment(writer, "Tree Widget", node.getModelTree());
            }
            style = "basic-tree";
        }

        StringWriter sr = new StringWriter();
        sr.append("<@renderNodeBegin ");
        sr.append(" style=\"");
        sr.append(style);
        sr.append("\" />");
        executeMacro(sr.toString());

        String pkName = node.getPkName(context);
        String entityId = null;
        String entryName = node.getEntryName();
        if (UtilValidate.isNotEmpty(entryName)) {
            Map<String, String> map = UtilGenerics.cast(context.get(entryName));
            entityId = map.get(pkName);
        } else {
            entityId = (String) context.get(pkName);
        }
        boolean hasChildren = node.hasChildren(context);

        // check to see if this node needs to be expanded.
        if (hasChildren && node.isExpandCollapse()) {
            // FIXME: Using a widget model in this way is an ugly hack.
            ModelTree.ModelNode.Link expandCollapseLink = null;
            String targetEntityId = null;
            Object obj1 = context.get("targetNodeTrail");
            List<String> targetNodeTrail = (obj1 instanceof List) ? UtilGenerics.cast(obj1) : null;
            if (depth < targetNodeTrail.size()) {
                targetEntityId = targetNodeTrail.get(depth);
            }

            int openDepth = node.getModelTree().getOpenDepth();
            if (depth >= openDepth && (targetEntityId == null || !targetEntityId.equals(entityId))) {
                // Not on the trail
                if (node.showPeers(depth, context)) {
                    context.put("processChildren", Boolean.FALSE);
                    currentNodeTrailPiped = StringUtil.join(currentNodeTrail, "|");
                    StringBuilder target = new StringBuilder(node.getModelTree().getExpandCollapseRequest(context));
                    String trailName = node.getModelTree().getTrailName(context);
                    if (target.indexOf("?") < 0) {
                        target.append("?");
                    } else {
                        target.append("&");
                    }
                    target.append(trailName).append("=").append(currentNodeTrailPiped);
                    expandCollapseLink = new ModelTree.ModelNode.Link("collapsed", target.toString(), " ");
                }
            } else {
                context.put("processChildren", Boolean.TRUE);
                String lastContentId = currentNodeTrail.remove(currentNodeTrail.size() - 1);
                currentNodeTrailPiped = StringUtil.join(currentNodeTrail, "|");
                if (currentNodeTrailPiped == null) {
                    currentNodeTrailPiped = "";
                }
                StringBuilder target = new StringBuilder(node.getModelTree().getExpandCollapseRequest(context));
                String trailName = node.getModelTree().getTrailName(context);
                if (target.indexOf("?") < 0) {
                    target.append("?");
                } else {
                    target.append("&");
                }
                target.append(trailName).append("=").append(currentNodeTrailPiped);
                expandCollapseLink = new ModelTree.ModelNode.Link("expanded", target.toString(), " ");
                // add it so it can be remove in renderNodeEnd
                currentNodeTrail.add(lastContentId);
            }
            if (expandCollapseLink != null) {
                renderLink(writer, context, expandCollapseLink);
            }
        } else if (!hasChildren) {
            context.put("processChildren", Boolean.FALSE);
            ModelTree.ModelNode.Link expandCollapseLink = new ModelTree.ModelNode.Link("leafnode", "", " ");
            renderLink(writer, context, expandCollapseLink);
        }
    }

    @Override
    public void renderNodeEnd(Appendable writer, Map<String, Object> context, ModelTree.ModelNode node) throws IOException {
        Boolean processChildren = (Boolean) context.get("processChildren");
        StringWriter sr = new StringWriter();
        sr.append("<@renderNodeEnd ");
        sr.append(" processChildren=");
        sr.append(Boolean.toString(processChildren));
        sr.append(" isRootNode=");
        sr.append(Boolean.toString(node.isRootNode()));
        sr.append(" />");
        executeMacro(sr.toString());
        if (node.isRootNode()) {
            if (ModelWidget.widgetBoundaryCommentsEnabled(context)) {
                renderEndingBoundaryComment(writer, "Tree Widget", node.getModelTree());
            }
        }
    }

    @Override
    public void renderLastElement(Appendable writer, Map<String, Object> context, ModelTree.ModelNode node) throws IOException {
        Boolean processChildren = (Boolean) context.get("processChildren");
        if (processChildren) {
            StringWriter sr = new StringWriter();
            sr.append("<@renderLastElement ");
            sr.append("style=\"");
            sr.append("basic-tree");
            sr.append("\" />");
            executeMacro(sr.toString());
        }
    }

    @Override
    public void renderLabel(Appendable writer, Map<String, Object> context, ModelTree.ModelNode.Label label) throws IOException {
        String id = label.getId(context);
        String style = label.getStyle(context);
        String labelText = label.getText(context);

        StringWriter sr = new StringWriter();
        sr.append("<@renderLabel ");
        sr.append("id=\"");
        sr.append(id);
        sr.append("\" style=\"");
        sr.append(style);
        sr.append("\" labelText=\"");
        sr.append(labelText);
        sr.append("\" />");
        executeMacro(sr.toString());
    }

    @Override
    public void renderLink(Appendable writer, Map<String, Object> context, ModelTree.ModelNode.Link link) throws IOException {
        String target = link.getTarget(context);
        StringBuilder linkUrl = new StringBuilder();
        HttpServletResponse response = (HttpServletResponse) context.get("response");
        HttpServletRequest request = (HttpServletRequest) context.get("request");

        if (UtilValidate.isNotEmpty(target)) {
            WidgetWorker.buildHyperlinkUrl(linkUrl, target, link.getUrlMode(), link.getParameterMap(context), link.getPrefix(context),
                    link.getFullPath(), link.getSecure(), link.getEncode(), request, response, context);
        }

        String id = link.getId(context);
        String style = link.getStyle(context);
        String name = link.getName(context);
        String title = link.getTitle(context);
        String targetWindow = link.getTargetWindow(context);
        String linkText = link.getText(context);

        String imgStr = "";
        ModelTree.ModelNode.Image img = link.getImage();
        if (img != null) {
            StringWriter sw = new StringWriter();
            renderImage(sw, context, img);
            imgStr = sw.toString();
        }

        StringWriter sr = new StringWriter();
        sr.append("<@renderLink ");
        sr.append("id=\"");
        sr.append(id);
        sr.append("\" style=\"");
        sr.append(style);
        sr.append("\" name=\"");
        sr.append(name);
        sr.append("\" title=\"");
        sr.append(title);
        sr.append("\" targetWindow=\"");
        sr.append(targetWindow);
        sr.append("\" linkUrl=\"");
        sr.append(linkUrl);
        sr.append("\" linkText=\"");
        sr.append(linkText);
        sr.append("\" imgStr=\"");
        sr.append(imgStr.replaceAll("\"", "\\\\\""));
        sr.append("\" />");
        executeMacro(sr.toString().replace("|", "%7C")); // Fix for OFBIZ-9191
    }

    @Override
    public void renderImage(Appendable writer, Map<String, Object> context, ModelTree.ModelNode.Image image) throws IOException {
        if (image == null) {
            return;
        }
        HttpServletResponse response = (HttpServletResponse) context.get("response");
        HttpServletRequest request = (HttpServletRequest) context.get("request");

        String urlMode = image.getUrlMode();
        String src = image.getSrc(context);
        String id = image.getId(context);
        String style = image.getStyle(context);
        String wid = image.getWidth(context);
        String hgt = image.getHeight(context);
        String border = image.getBorder(context);
        String alt = ""; //TODO add alt to tree images image.getAlt(context);

        boolean fullPath = false;
        boolean secure = false;
        boolean encode = false;
        String urlString = "";

        if (urlMode != null && "intra-app".equalsIgnoreCase(urlMode)) {
            if (request != null && response != null) {
                RequestHandler rh = RequestHandler.from(request);
                urlString = rh.makeLink(request, response, src, fullPath, secure, encode);
            } else {
                urlString = src;
            }
        } else  if (urlMode != null && "content".equalsIgnoreCase(urlMode)) {
            if (request != null && response != null) {
                StringBuilder newURL = new StringBuilder();
                ContentUrlTag.appendContentPrefix(request, newURL);
                newURL.append(src);
                urlString = newURL.toString();
            }
        } else {
            urlString = src;
        }
        StringWriter sr = new StringWriter();
        sr.append("<@renderImage ");
        sr.append("src=\"");
        sr.append(src);
        sr.append("\" id=\"");
        sr.append(id);
        sr.append("\" style=\"");
        sr.append(style);
        sr.append("\" wid=\"");
        sr.append(wid);
        sr.append("\" hgt=\"");
        sr.append(hgt);
        sr.append("\" border=\"");
        sr.append(border);
        sr.append("\" alt=\"");
        sr.append(alt);
        sr.append("\" urlString=\"");
        sr.append(urlString);
        sr.append("\" />");
        executeMacro(sr.toString());
    }

    @Override
    public ScreenStringRenderer getScreenStringRenderer(Map<String, Object> context) {
        ScreenRenderer screenRenderer = (ScreenRenderer)context.get("screens");
        if (screenRenderer != null) {
            return screenRenderer.getScreenStringRenderer();
        }
        return null;
    }
}
