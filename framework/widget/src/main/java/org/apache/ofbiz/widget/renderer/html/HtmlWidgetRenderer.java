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
package org.apache.ofbiz.widget.renderer.html;

import java.io.IOException;
import java.util.List;

import org.apache.ofbiz.base.util.UtilHtml;
import org.apache.ofbiz.base.util.UtilHttp;
import org.apache.ofbiz.webapp.SeoConfigUtil;
import org.apache.ofbiz.widget.model.ModelWidget;

/**
 * Widget Library - HTML Widget Renderer implementation. HtmlWidgetRenderer
 * is a base class that is extended by other widget HTML rendering classes.
 */
public class HtmlWidgetRenderer {
    private static final String MODULE = HtmlWidgetRenderer.class.getName();

    /**
     * Characters that are appended to the end of each rendered element. Currently set to
     * CR/LF.
     */
    public static final String WHITE_SPACE = "\r\n";

    /**
     * Store property value of widget.dev.namedBorder
     */
    public static final ModelWidget.NamedBorderType NAMED_BORDER_TYPE = ModelWidget.widgetNamedBorderType();

    /**
     * Sets widget comments enabled.
     * @param widgetCommentsEnabled the widget comments enabled
     */
    public void setWidgetCommentsEnabled(boolean widgetCommentsEnabled) {
        this.widgetCommentsEnabled = widgetCommentsEnabled;
    }

    private boolean widgetCommentsEnabled = false;

    /**
     * Is widget comments enabled boolean.
     * @return the boolean
     */
    public boolean isWidgetCommentsEnabled() {
        return widgetCommentsEnabled;
    }

    /**
     * Helper method used to append whitespace characters to the end of each rendered element.
     * @param writer The writer to write to
     */
    public void appendWhitespace(Appendable writer) throws IOException {
        writer.append(WHITE_SPACE);
    }

    /**
     * Helper method used to build the boundary comment string.
     * @param boundaryType The boundary type: "Begin" or "End"
     * @param widgetType The widget type: "Screen Widget", "Form Widget", etc.
     * @param widgetName The widget name
     */
    public static String buildBoundaryComment(String boundaryType, String widgetType, String widgetName) {
        return "<!-- " + boundaryType + " " + widgetType + " " + widgetName + " -->" + WHITE_SPACE;
    }

    /**
     * Always check the following condition is true before running the method:
     * HtmlWidgetRenderer.namedBorderType != ModelWidget.NamedBorderType.NONE
     * @param widgetType
     * @param location
     * @param contextPath
     * @return
     */
    public static String beginNamedBorder(String widgetType, String location, String contextPath) {
        List<String> themeBasePathsToExempt = UtilHtml.getVisualThemeFolderNamesToExempt();
        if (!themeBasePathsToExempt.stream().anyMatch(location::contains)) {
            String fileName = location.substring(location.lastIndexOf("/") + 1);
            switch (NAMED_BORDER_TYPE) {
            case SOURCE:
                return "<div class='info-container'><span class='info-overlay-item info-cursor-none info-"
                        + widgetType.toLowerCase().replaceAll(" ", "-") + "' data-source='"
                        + location + "' data-target='" + contextPath
                        + (SeoConfigUtil.isCategoryUrlEnabled(contextPath) ? "" : "/control")
                        + "/openSourceFile'>"
                        + fileName
                        + "</span>";
            case LABEL:
                return "<div class='info-container'><span class='info-overlay-item'>"
                        + fileName
                        + "</span>";
            default:
                return "";
            }
        }
        return "";
    }

    /**
     * Always check the following condition is true before running the method:
     * HtmlWidgetRenderer.namedBorderType != ModelWidget.NamedBorderType.NONE
     * @param widgetType
     * @param location
     * @return
     */
    public static String endNamedBorder(String widgetType, String location) {
        List<String> themeBasePathsToExempt = UtilHtml.getVisualThemeFolderNamesToExempt();
        if (!themeBasePathsToExempt.stream().anyMatch(location::contains)) {
            return "</div>";
        }
        return "";
    }

    public static String formatBoundaryJsComment(String boundaryType, String widgetType, String widgetName) {
        return "// " + boundaryType + " " + widgetType + " " + widgetName + WHITE_SPACE;
    }

    /**
     * Renders the beginning boundary comment string.
     * @param writer The writer to write to
     * @param widgetType The widget type: "Screen Widget", "Form Widget", etc.
     * @param modelWidget The widget
     */
    public void renderBeginningBoundaryComment(Appendable writer, String widgetType, ModelWidget modelWidget) throws IOException {
        if (this.widgetCommentsEnabled) {
            writer.append(buildBoundaryComment("Begin", widgetType, modelWidget.getBoundaryCommentName()));
        }
    }

    /**
     * Renders the ending boundary comment string.
     * @param writer The writer to write to
     * @param widgetType The widget type: "Screen Widget", "Form Widget", etc.
     * @param modelWidget The widget
     */
    public void renderEndingBoundaryComment(Appendable writer, String widgetType, ModelWidget modelWidget) throws IOException {
        if (this.widgetCommentsEnabled) {
            writer.append(buildBoundaryComment("End", widgetType, modelWidget.getBoundaryCommentName()));
        }
    }

    /** Extracts parameters from a target URL string, prepares them for an Ajax
     * JavaScript call. This method is currently set to return a parameter string
     * suitable for the Prototype.js library.
     * @param target Target URL string
     * @return Parameter string
     */
    public static String getAjaxParamsFromTarget(String target) {
        String targetParams = UtilHttp.getQueryStringFromTarget(target);
        targetParams = targetParams.replace("?", "");
        targetParams = targetParams.replace("&amp;", "&");
        return targetParams;
    }
}
