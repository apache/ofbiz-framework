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
package org.ofbiz.widget.html;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ofbiz.base.util.StringUtil;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.webapp.control.RequestHandler;
import org.ofbiz.webapp.taglib.ContentUrlTag;
import org.ofbiz.widget.screen.ScreenRenderer;
import org.ofbiz.widget.screen.ScreenStringRenderer;
import org.ofbiz.widget.tree.ModelTree;
import org.ofbiz.widget.tree.TreeStringRenderer;


/**
 * Widget Library - HTML Tree Renderer implementation
 */
public class HtmlTreeRenderer implements TreeStringRenderer {

    ScreenStringRenderer screenStringRenderer = null;
    public static final String module = HtmlTreeRenderer.class.getName(); 

    public HtmlTreeRenderer() {}
    
    public static String buildPathString(ModelTree modelTree, int depth) {
        StringBuffer buf = new StringBuffer();
        for (int i=1; i <= depth; i++) {
            int idx = modelTree.getNodeIndexAtDepth(i);
            buf.append(".");
            buf.append(Integer.toString(idx + 1));
        }
        return buf.toString();
    }

    public void renderNodeBegin(Writer writer, Map context, ModelTree.ModelNode node, int depth, boolean isLast) throws IOException {

        String pathString = buildPathString(node.getModelTree(), depth);
        String currentNodeTrailPiped = null;
        List currentNodeTrail = node.getModelTree().getCurrentNodeTrail();
        String staticNodeTrailPiped = StringUtil.join(currentNodeTrail, "|");
        context.put("staticNodeTrailPiped", staticNodeTrailPiped);
        context.put("nodePathString", pathString);
        context.put("depth", Integer.toString(depth));
        if (node.isRootNode()) {
            appendWhitespace(writer);
            writer.write("<!-- begin tree widget -->");
            appendWhitespace(writer);
            writer.write("<ul class=\"basic-tree\">");
        } else {
            appendWhitespace(writer);
            writer.write(" <li>");
        }

        String pkName = node.getPkName();
        String entityId = null;
        String entryName = node.getEntryName();
        if (UtilValidate.isNotEmpty(entryName)) {
            Map map = (Map)context.get(entryName);
            entityId = (String)map.get(pkName);
        } else {
            entityId = (String) context.get(pkName);
        }
        boolean hasChildren = node.hasChildren(context);
            //Debug.logInfo("HtmlTreeExpandCollapseRenderer, hasChildren(1):" + hasChildren, module);

        // check to see if this node needs to be expanded.
        if (hasChildren && node.isExpandCollapse()) {
            String targetEntityId = null;
            List targetNodeTrail = node.getModelTree().getTrailList();
            if (depth < targetNodeTrail.size()) {
                targetEntityId = (String)targetNodeTrail.get(depth);
            }
            //Debug.logInfo("HtmlTreeExpandCollapseRenderer, targetEntityId(1):" + targetEntityId, module);
            //Debug.logInfo("HtmlTreeExpandCollapseRenderer, depth(1):" + depth, module);
    
            ModelTree.ModelNode.Image expandCollapseImage = new ModelTree.ModelNode.Image();
            expandCollapseImage.setBorder("0");
            ModelTree.ModelNode.Link expandCollapseLink = new ModelTree.ModelNode.Link();
            //String currentNodeTrailCsv = (String)context.get("currentNodeTrailCsv");
    
            int openDepth = node.getModelTree().getOpenDepth();
            if (depth >= openDepth && (targetEntityId == null || !targetEntityId.equals(entityId))) {
                // Not on the trail
                if( node.showPeers(depth)) {
                    context.put("processChildren", Boolean.FALSE);
                    //expandCollapseLink.setText("&nbsp;+&nbsp;");
                    currentNodeTrailPiped = StringUtil.join(currentNodeTrail, "|");
                    context.put("currentNodeTrailPiped", currentNodeTrailPiped);
                    //context.put("currentNodeTrailCsv", currentNodeTrailCsv);
                    expandCollapseLink.setStyle("collapsed");
                    expandCollapseLink.setText("&nbsp;");
                    String target = node.getModelTree().getExpandCollapseRequest(context);
                    String trailName = node.getModelTree().getTrailName(context);
                    if (target.indexOf("?") < 0) {
                        target += "?";
                    } else {
                        target += "&";
                    }
                    target += trailName + "=" + currentNodeTrailPiped;
                    target += "#" + staticNodeTrailPiped;
                    //expandCollapseLink.setTarget("/ViewOutline?docRootContentId=${docRootContentId}&targetNodeTrailCsv=${currentNodeTrailCsv}");
                    expandCollapseLink.setTarget(target);
                }
            } else {
                context.put("processChildren", Boolean.TRUE);
                //expandCollapseLink.setText("&nbsp;-&nbsp;");
                String lastContentId = (String)currentNodeTrail.remove(currentNodeTrail.size() - 1);
                currentNodeTrailPiped = StringUtil.join(currentNodeTrail, "|");
                if (currentNodeTrailPiped == null) {
                    currentNodeTrailPiped = "";
                }
                context.put("currentNodeTrailPiped", currentNodeTrailPiped);
                //context.put("currentNodeTrailCsv", currentNodeTrailCsv);
                expandCollapseLink.setStyle("expanded");
                expandCollapseLink.setText("&nbsp;");
                String target = node.getModelTree().getExpandCollapseRequest(context);
                String trailName = node.getModelTree().getTrailName(context);
                if (target.indexOf("?") < 0)  target += "?";
                else target += "&";
                target += trailName + "=" + currentNodeTrailPiped;
                target += "#" + staticNodeTrailPiped;
                expandCollapseLink.setTarget(target);
                // add it so it can be remove in renderNodeEnd
                currentNodeTrail.add(lastContentId);
                currentNodeTrailPiped = StringUtil.join(currentNodeTrail, "|");
                if (currentNodeTrailPiped == null) {
                    currentNodeTrailPiped = "";
                }
                context.put("currentNodeTrailPiped", currentNodeTrailPiped);
            }
            renderLink( writer, context, expandCollapseLink);
        } else if (!hasChildren){
                //writer.write(" ");
                context.put("processChildren", Boolean.FALSE);
                //currentNodeTrail.add(contentId);
        }
    }

    public void renderNodeEnd(Writer writer, Map context, ModelTree.ModelNode node) throws IOException {
        if (node.isRootNode()) {
            appendWhitespace(writer);
            writer.write("</ul>");
            appendWhitespace(writer);
            writer.write("<!-- end tree widget -->");
            appendWhitespace(writer);
        }
        else {
            Boolean processChildren = (Boolean) context.get("processChildren");
            if (processChildren.booleanValue()) {
                appendWhitespace(writer);
                writer.write("</ul>");
            }
            writer.write("</li>");
        }
    }

    public void renderLastElement(Writer writer, Map context, ModelTree.ModelNode node) throws IOException {
        if (!node.isRootNode()) {
            Boolean processChildren = (Boolean) context.get("processChildren");
            if (processChildren.booleanValue()) {
                appendWhitespace(writer);
                writer.write("<ul class=\"basic-tree\">");
            }
        }
    }
    
    public void renderLabel(Writer writer, Map context, ModelTree.ModelNode.Label label) throws IOException {
        // open tag
        writer.write("<span");
        String id = label.getId(context);
        if (UtilValidate.isNotEmpty(id)) {
            writer.write(" id=\"");
            writer.write(id);
            writer.write("\"");
        }
        String style = label.getStyle(context);
        if (UtilValidate.isNotEmpty(style)) {
            writer.write(" class=\"");
            writer.write(style);
            writer.write("\"");
        }
        writer.write(">");
        
        // the text
        writer.write(label.getText(context));
        
        // close tag
        writer.write("</span>");
        
        appendWhitespace(writer);
    }


    public void renderLink(Writer writer, Map context, ModelTree.ModelNode.Link link) throws IOException {
        // open tag
        writer.write("<a");
        String id = link.getId(context);
        if (UtilValidate.isNotEmpty(id)) {
            writer.write(" id=\"");
            writer.write(id);
            writer.write("\"");
        }
        String style = link.getStyle(context);
        if (UtilValidate.isNotEmpty(style)) {
            writer.write(" class=\"");
            writer.write(style);
            writer.write("\"");
        }
        String name = link.getName(context);
        if (UtilValidate.isNotEmpty(name)) {
            writer.write(" name=\"");
            writer.write(name);
            writer.write("\"");
        }
        String title = link.getTitle(context);
        if (UtilValidate.isNotEmpty(title)) {
            writer.write(" title=\"");
            writer.write(title);
            writer.write("\"");
        }        
        String targetWindow = link.getTargetWindow(context);
        if (UtilValidate.isNotEmpty(targetWindow)) {
            writer.write(" target=\"");
            writer.write(targetWindow);
            writer.write("\"");
        }
        String target = link.getTarget(context);
        if (UtilValidate.isNotEmpty(target)) {
            writer.write(" href=\"");
            String urlMode = link.getUrlMode();
            String prefix = link.getPrefix(context);
            boolean fullPath = link.getFullPath();
            boolean secure = link.getSecure();
            boolean encode = link.getEncode();
            HttpServletResponse res = (HttpServletResponse) context.get("response");
            HttpServletRequest req = (HttpServletRequest) context.get("request");
            if (urlMode != null && urlMode.equalsIgnoreCase("intra-app")) {
                if (req != null && res != null) {
                    ServletContext ctx = (ServletContext) req.getAttribute("servletContext");
                    RequestHandler rh = (RequestHandler) ctx.getAttribute("_REQUEST_HANDLER_");
                    String urlString = rh.makeLink(req, res, target, fullPath, secure, encode);
                    writer.write(urlString);
                } else if (prefix != null) {
                    writer.write(prefix + target);
                } else {
                    writer.write(target);
                }
            } else  if (urlMode != null && urlMode.equalsIgnoreCase("content")) {
                StringBuffer newURL = new StringBuffer();
                ContentUrlTag.appendContentPrefix(req, newURL);
                newURL.append(target);
                writer.write(newURL.toString());
            } else {
                writer.write(target);
            }

            writer.write("\"");
        }
        writer.write(">");
        
        // the text
        ModelTree.ModelNode.Image img = link.getImage();
        if (img == null)
            writer.write(link.getText(context));
        else
            renderImage(writer, context, img);
        
        // close tag
        writer.write("</a>");
        
//        appendWhitespace(writer);
    }

    public void renderImage(Writer writer, Map context, ModelTree.ModelNode.Image image) throws IOException {
        // open tag
        writer.write("<img ");
        String id = image.getId(context);
        if (UtilValidate.isNotEmpty(id)) {
            writer.write(" id=\"");
            writer.write(id);
            writer.write("\"");
        }
        String style = image.getStyle(context);
        if (UtilValidate.isNotEmpty(style)) {
            writer.write(" class=\"");
            writer.write(style);
            writer.write("\"");
        }
        String wid = image.getWidth(context);
        if (UtilValidate.isNotEmpty(wid)) {
            writer.write(" width=\"");
            writer.write(wid);
            writer.write("\"");
        }
        String hgt = image.getHeight(context);
        if (UtilValidate.isNotEmpty(hgt)) {
            writer.write(" height=\"");
            writer.write(hgt);
            writer.write("\"");
        }
        String border = image.getBorder(context);
        if (UtilValidate.isNotEmpty(border)) {
            writer.write(" border=\"");
            writer.write(border);
            writer.write("\"");
        }
        String src = image.getSrc(context);
        if (UtilValidate.isNotEmpty(src)) {
            writer.write(" src=\"");
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
                    writer.write(urlString);
                } else {
                    writer.write(src);
                }
            } else  if (urlMode != null && urlMode.equalsIgnoreCase("content")) {
                if (request != null && response != null) {
                    StringBuffer newURL = new StringBuffer();
                    ContentUrlTag.appendContentPrefix(request, newURL);
                    newURL.append(src);
                    writer.write(newURL.toString());
                }
            } else {
                writer.write(src);
            }

            writer.write("\"");
        }
        writer.write("/>");
        
    }

    public void appendWhitespace(Writer writer) throws IOException {
        // appending line ends for now, but this could be replaced with a simple space or something
        writer.write("\r\n");
        //writer.write(' ');
    }

    public ScreenStringRenderer getScreenStringRenderer(Map context) {

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
