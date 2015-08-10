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
package org.ofbiz.widget.renderer.html;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ofbiz.base.util.StringUtil;
import org.ofbiz.base.util.UtilGenerics;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.webapp.control.RequestHandler;
import org.ofbiz.webapp.taglib.ContentUrlTag;
import org.ofbiz.widget.WidgetWorker;
import org.ofbiz.widget.model.ModelTree;
import org.ofbiz.widget.model.ModelWidget;
import org.ofbiz.widget.renderer.ScreenRenderer;
import org.ofbiz.widget.renderer.ScreenStringRenderer;
import org.ofbiz.widget.renderer.TreeStringRenderer;


/**
 * Widget Library - HTML Tree Renderer implementation
 */
public class HtmlTreeRenderer extends HtmlWidgetRenderer implements TreeStringRenderer {

    ScreenStringRenderer screenStringRenderer = null;
    public static final String module = HtmlTreeRenderer.class.getName();

    public HtmlTreeRenderer() {}

    public void renderNodeBegin(Appendable writer, Map<String, Object> context, ModelTree.ModelNode node, int depth) throws IOException {
        String currentNodeTrailPiped = null;
        List<String> currentNodeTrail = UtilGenerics.toList(context.get("currentNodeTrail"));
        if (node.isRootNode()) {
            appendWhitespace(writer);
            this.widgetCommentsEnabled = ModelWidget.widgetBoundaryCommentsEnabled(context);
            renderBeginningBoundaryComment(writer, "Tree Widget", node.getModelTree());
            writer.append("<ul class=\"basic-tree\">");
        }
        appendWhitespace(writer);
        writer.append("<li>");

        String pkName = node.getPkName(context);
        String entityId = null;
        String entryName = node.getEntryName();
        if (UtilValidate.isNotEmpty(entryName)) {
            entityId = UtilGenerics.<Map<String, String>>cast(context.get(entryName)).get(pkName);
        } else {
            entityId = (String) context.get(pkName);
        }
        boolean hasChildren = node.hasChildren(context);

        // check to see if this node needs to be expanded.
        if (hasChildren && node.isExpandCollapse()) {
            String targetEntityId = null;
            List<String> targetNodeTrail = UtilGenerics.toList(context.get("targetNodeTrail"));
            if (depth < targetNodeTrail.size()) {
                targetEntityId = targetNodeTrail.get(depth);
            }
            // FIXME: Using a widget model in this way is an ugly hack.
            ModelTree.ModelNode.Link expandCollapseLink = null;
            int openDepth = node.getModelTree().getOpenDepth();
            if (depth >= openDepth && (targetEntityId == null || !targetEntityId.equals(entityId))) {
                // Not on the trail
                if (node.showPeers(depth, context)) {
                    context.put("processChildren", Boolean.FALSE);
                    //expandCollapseLink.setText("&nbsp;+&nbsp;");
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
                //expandCollapseLink.setText("&nbsp;-&nbsp;");
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

    public void renderNodeEnd(Appendable writer, Map<String, Object> context, ModelTree.ModelNode node) throws IOException {
        Boolean processChildren = (Boolean) context.get("processChildren");
        if (processChildren.booleanValue()) {
            appendWhitespace(writer);
            writer.append("</ul>");
        }
        appendWhitespace(writer);
        writer.append("</li>");
        if (node.isRootNode()) {
            appendWhitespace(writer);
            writer.append("</ul>");
            appendWhitespace(writer);
            renderEndingBoundaryComment(writer, "Tree Widget", node.getModelTree());
        }
    }

    public void renderLastElement(Appendable writer, Map<String, Object> context, ModelTree.ModelNode node) throws IOException {
        Boolean processChildren = (Boolean) context.get("processChildren");
        if (processChildren.booleanValue()) {
            appendWhitespace(writer);
            writer.append("<ul class=\"basic-tree\">");
        }
    }

    public void renderLabel(Appendable writer, Map<String, Object> context, ModelTree.ModelNode.Label label) throws IOException {
        // open tag
        writer.append("<span");
        String id = label.getId(context);
        if (UtilValidate.isNotEmpty(id)) {
            writer.append(" id=\"");
            writer.append(id);
            writer.append("\"");
        }
        String style = label.getStyle(context);
        if (UtilValidate.isNotEmpty(style)) {
            writer.append(" class=\"");
            writer.append(style);
            writer.append("\"");
        }
        writer.append(">");

        // the text
        writer.append(label.getText(context));

        // close tag
        writer.append("</span>");

        appendWhitespace(writer);
    }


    public void renderLink(Appendable writer, Map<String, Object> context, ModelTree.ModelNode.Link link) throws IOException {
        // open tag
        writer.append("<a");
        String id = link.getId(context);
        if (UtilValidate.isNotEmpty(id)) {
            writer.append(" id=\"");
            writer.append(id);
            writer.append("\"");
        }
        String style = link.getStyle(context);
        if (UtilValidate.isNotEmpty(style)) {
            writer.append(" class=\"");
            writer.append(style);
            writer.append("\"");
        }
        String name = link.getName(context);
        if (UtilValidate.isNotEmpty(name)) {
            writer.append(" name=\"");
            writer.append(name);
            writer.append("\"");
        }
        String title = link.getTitle(context);
        if (UtilValidate.isNotEmpty(title)) {
            writer.append(" title=\"");
            writer.append(title);
            writer.append("\"");
        }
        String targetWindow = link.getTargetWindow(context);
        if (UtilValidate.isNotEmpty(targetWindow)) {
            writer.append(" target=\"");
            writer.append(targetWindow);
            writer.append("\"");
        }
        String target = link.getTarget(context);
        if (UtilValidate.isNotEmpty(target)) {
            writer.append(" href=\"");
            String urlMode = link.getUrlMode();
            String prefix = link.getPrefix(context);
            HttpServletResponse res = (HttpServletResponse) context.get("response");
            HttpServletRequest req = (HttpServletRequest) context.get("request");
            if (urlMode != null && urlMode.equalsIgnoreCase("intra-app")) {
                if (req != null && res != null) {
                    WidgetWorker.buildHyperlinkUrl(writer, target, link.getUrlMode(), link.getParameterMap(context), link.getPrefix(context),
                        link.getFullPath(), link.getSecure(), link.getEncode(), req, res, context);
                } else if (prefix != null) {
                    writer.append(prefix).append(target);
                } else {
                    writer.append(target);
                }
            } else if (urlMode != null && urlMode.equalsIgnoreCase("content")) {
                StringBuilder newURL = new StringBuilder();
                ContentUrlTag.appendContentPrefix(req, newURL);
                newURL.append(target);
                writer.append(newURL.toString());
            } else if ("inter-app".equalsIgnoreCase(urlMode) && req != null) {
                String externalLoginKey = (String) req.getAttribute("externalLoginKey");
                if (UtilValidate.isNotEmpty(externalLoginKey)) {
                    writer.append(target);
                    if (target.contains("?")) {
                        writer.append("&externalLoginKey=");
                    } else {
                        writer.append("?externalLoginKey=");
                    }
                    writer.append(externalLoginKey);
                }
            } else {
                writer.append(target);
            }
            writer.append("\"");
        }
        writer.append(">");

        // the text
        ModelTree.ModelNode.Image img = link.getImage();
        if (img == null) {
            writer.append(link.getText(context));
        } else {
            renderImage(writer, context, img);
        }
        // close tag
        writer.append("</a>");
    }

    public void renderImage(Appendable writer, Map<String, Object> context, ModelTree.ModelNode.Image image) throws IOException {
        // open tag
        writer.append("<img ");
        String id = image.getId(context);
        if (UtilValidate.isNotEmpty(id)) {
            writer.append(" id=\"");
            writer.append(id);
            writer.append("\"");
        }
        String style = image.getStyle(context);
        if (UtilValidate.isNotEmpty(style)) {
            writer.append(" class=\"");
            writer.append(style);
            writer.append("\"");
        }
        String wid = image.getWidth(context);
        if (UtilValidate.isNotEmpty(wid)) {
            writer.append(" width=\"");
            writer.append(wid);
            writer.append("\"");
        }
        String hgt = image.getHeight(context);
        if (UtilValidate.isNotEmpty(hgt)) {
            writer.append(" height=\"");
            writer.append(hgt);
            writer.append("\"");
        }
        String border = image.getBorder(context);
        if (UtilValidate.isNotEmpty(border)) {
            writer.append(" border=\"");
            writer.append(border);
            writer.append("\"");
        }
        String src = image.getSrc(context);
        if (UtilValidate.isNotEmpty(src)) {
            writer.append(" src=\"");
            String urlMode = image.getUrlMode();
            boolean fullPath = false;
            boolean secure = false;
            boolean encode = false;
            HttpServletResponse response = (HttpServletResponse) context.get("response");
            HttpServletRequest request = (HttpServletRequest) context.get("request");
            if (urlMode != null && urlMode.equalsIgnoreCase("intra-app")) {
                if (request != null && response != null) {
                    ServletContext ctx = (ServletContext) request.getAttribute("servletContext");
                    RequestHandler rh = (RequestHandler) ctx.getAttribute("_REQUEST_HANDLER_");
                    String urlString = rh.makeLink(request, response, src, fullPath, secure, encode);
                    writer.append(urlString);
                } else {
                    writer.append(src);
                }
            } else  if (urlMode != null && urlMode.equalsIgnoreCase("content")) {
                if (request != null && response != null) {
                    StringBuilder newURL = new StringBuilder();
                    ContentUrlTag.appendContentPrefix(request, newURL);
                    newURL.append(src);
                    writer.append(newURL.toString());
                }
            } else {
                writer.append(src);
            }
            writer.append("\"");
        }
        writer.append("/>");

    }

    public ScreenStringRenderer getScreenStringRenderer(Map<String, Object> context) {
        ScreenRenderer screenRenderer = (ScreenRenderer)context.get("screens");
        if (screenRenderer != null) {
            screenStringRenderer = screenRenderer.getScreenStringRenderer();
        } else {
            if (screenStringRenderer == null) {
                screenStringRenderer = new HtmlScreenRenderer();
            }
        }
        return screenStringRenderer;
    }
}
