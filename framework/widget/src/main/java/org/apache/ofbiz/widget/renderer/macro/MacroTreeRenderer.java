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
import java.io.StringWriter;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.ofbiz.base.util.StringUtil;
import org.apache.ofbiz.base.util.UtilCodec;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.webapp.control.RequestHandler;
import org.apache.ofbiz.webapp.taglib.ContentUrlTag;
import org.apache.ofbiz.widget.WidgetWorker;
import org.apache.ofbiz.widget.model.CommonWidgetModels;
import org.apache.ofbiz.widget.model.ModelTree;
import org.apache.ofbiz.widget.model.ModelWidget;
import org.apache.ofbiz.widget.renderer.ScreenRenderer;
import org.apache.ofbiz.widget.renderer.ScreenStringRenderer;
import org.apache.ofbiz.widget.renderer.TreeStringRenderer;

import com.google.common.collect.ImmutableList;

import freemarker.template.TemplateException;

/**
 * Widget Library - Tree Renderer implementation based on Freemarker macros
 *
 */
public class MacroTreeRenderer implements TreeStringRenderer {

    private final FtlWriter ftlWriter;

    public MacroTreeRenderer(String macroLibraryPath, Appendable writer) throws TemplateException, IOException {
        this.ftlWriter = new FtlWriter(macroLibraryPath);
    }

    private void executeMacro(Appendable writer, String macroName, Map<String, Object> macroParameters) {
        Map<String, Object> parameters = new HashMap<>();
        if (macroParameters != null) {
            macroParameters.forEach((name, value) -> parameters.put(name, value == null ? "" : value));
        }
        ftlWriter.processMacroCall(writer, null, macroName, parameters);
    }

    /**
     * Renders the beginning boundary comment string.
     * @param writer The writer to write to
     * @param widgetType The widget type: "Screen Widget", "Tree Widget", etc.
     * @param modelWidget The widget
     */
    public void renderBeginningBoundaryComment(Appendable writer, String widgetType, ModelWidget modelWidget) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("boundaryType", "Begin");
        parameters.put("widgetType", widgetType);
        parameters.put("widgetName", modelWidget.getBoundaryCommentName());
        executeMacro(writer, "formatBoundaryComment", parameters);
    }

    /**
     * Renders the ending boundary comment string.
     * @param writer The writer to write to
     * @param widgetType The widget type: "Screen Widget", "Tree Widget", etc.
     * @param modelWidget The widget
     */
    public void renderEndingBoundaryComment(Appendable writer, String widgetType, ModelWidget modelWidget) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("boundaryType", "End");
        parameters.put("widgetType", widgetType);
        parameters.put("widgetName", modelWidget.getBoundaryCommentName());
        executeMacro(writer, "formatBoundaryComment", parameters);
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

        executeMacro(writer, "renderNodeBegin", UtilMisc.toMap("style", style));

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
            if (targetNodeTrail != null && depth < targetNodeTrail.size()) {
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
                    expandCollapseLink = new ModelTree.ModelNode.Link("collapsed", target.toString(), " ",
                            ImmutableList.of(CommonWidgetModels.Parameter.literal(trailName, currentNodeTrailPiped)));
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
                expandCollapseLink = new ModelTree.ModelNode.Link("expanded", target.toString(), " ",
                        ImmutableList.of(CommonWidgetModels.Parameter.literal(trailName, currentNodeTrailPiped)));
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
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("processChildren", processChildren);
        parameters.put("isRootNode", node.isRootNode());
        executeMacro(writer, "renderNodeEnd", parameters);
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
            executeMacro(writer, "renderLastElement", UtilMisc.toMap("style", "basic-tree"));
        }
    }

    @Override
    public void renderLabel(Appendable writer, Map<String, Object> context, ModelTree.ModelNode.Label label) throws IOException {
        String id = label.getId(context);
        String style = label.getStyle(context);
        String labelText = label.getText(context);

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("id", id);
        parameters.put("style", style);
        parameters.put("labelText", labelText);
        executeMacro(writer, "renderLabel", parameters);
    }

    @Override
    public void renderLink(Appendable writer, Map<String, Object> context, ModelTree.ModelNode.Link link) throws IOException {
        String target = link.getTarget(context);
        StringBuilder linkUrl = new StringBuilder();
        HttpServletResponse response = (HttpServletResponse) context.get("response");
        HttpServletRequest request = (HttpServletRequest) context.get("request");

        if (UtilValidate.isNotEmpty(target)) {
            final URI uri = WidgetWorker.buildHyperlinkUri(target, link.getUrlMode(), link.getParameterMap(context),
                    link.getPrefix(context), link.getFullPath(), link.getSecure(), link.getEncode(),
                    request, response);
            linkUrl.append(uri.toString());
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

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("id", id);
        parameters.put("style", style);
        parameters.put("name", name);
        parameters.put("title", title);
        parameters.put("targetWindow", targetWindow);
        parameters.put("linkUrl", linkUrl.toString().replace("|", "%7C")); // Fix for OFBIZ-9191
        parameters.put("linkText", linkText);
        parameters.put("imgStr", imgStr);
        executeMacro(writer, "renderLink", parameters);
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
        } else if (urlMode != null && "content".equalsIgnoreCase(urlMode)) {
            if (request != null && response != null) {
                StringBuilder newURL = new StringBuilder();
                ContentUrlTag.appendContentPrefix(request, newURL);
                newURL.append(src);
                urlString = newURL.toString();
            }
        } else {
            urlString = src;
        }
        UtilCodec.SimpleEncoder simpleEncoder = (UtilCodec.SimpleEncoder) context.get("simpleEncoder");
        if (simpleEncoder != null) {
            urlString = simpleEncoder.encode(urlString);
        }
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("src", src);
        parameters.put("id", id);
        parameters.put("style", style);
        parameters.put("wid", wid);
        parameters.put("hgt", hgt);
        parameters.put("border", border);
        parameters.put("alt", alt);
        parameters.put("urlString", urlString);
        executeMacro(writer, "renderImage", parameters);
    }

    @Override
    public ScreenStringRenderer getScreenStringRenderer(Map<String, Object> context) {
        ScreenRenderer screenRenderer = (ScreenRenderer) context.get("screens");
        if (screenRenderer != null) {
            return screenRenderer.getScreenStringRenderer();
        }
        return null;
    }
}
