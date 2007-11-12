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
import org.ofbiz.widget.ModelWidget;

/**
 * Widget Library - HTML Widget Renderer implementation. HtmlWidgetRenderer
 * is a base class that is extended by other widget HTML rendering classes.
 */
public class HtmlWidgetRenderer {

    /**
     * Characters that are appended to the end of each rendered element. Currently set to
     * CR/LF.
     */
    public static final String whiteSpace = "\r\n";

    /**
     * Helper method used to append whitespace characters to the end of each rendered element.
     * @param writer The writer to write to
     */
    public static void appendWhitespace(Writer writer) throws IOException {
        writer.write(whiteSpace);
    }
    
    /**
     * Helper method used to append whitespace characters to the end of each rendered element.
     * @param buffer The buffer to write to
     */
    public static void appendWhitespace(StringBuffer buffer) {
        buffer.append(whiteSpace);
    }

    /**
     * Helper method used to build the boundary comment string.
     * @param boundaryType The boundary type: "Begin" or "End"
     * @param widgetType The widget type: "Screen Widget", "Form Widget", etc.
     * @param widgetName The widget name
     */
    public static String buildBoundaryComment(String boundaryType, String widgetType, String widgetName) {
        return "<!-- " + boundaryType + " " + widgetType + " " + widgetName + " -->" + whiteSpace;
    }

    /**
     * Renders the beginning boundary comment string.
     * @param buffer The buffer to write to
     * @param widgetType The widget type: "Screen Widget", "Form Widget", etc.
     * @param modelWidget The widget
     */
    public void renderBeginningBoundaryComment(StringBuffer buffer, String widgetType, ModelWidget modelWidget) {
        if (modelWidget.boundaryCommentsEnabled()) {
            buffer.append(buildBoundaryComment("Begin", widgetType, modelWidget.getBoundaryCommentName()));
        }
    }

    /**
     * Renders the beginning boundary comment string.
     * @param writer The writer to write to
     * @param widgetType The widget type: "Screen Widget", "Form Widget", etc.
     * @param modelWidget The widget
     */
    public void renderBeginningBoundaryComment(Writer writer, String widgetType, ModelWidget modelWidget) throws IOException {
        if (modelWidget.boundaryCommentsEnabled()) {
            writer.write(buildBoundaryComment("Begin", widgetType, modelWidget.getBoundaryCommentName()));
        }
    }

    /**
     * Renders the ending boundary comment string.
     * @param writer The writer to write to
     * @param widgetType The widget type: "Screen Widget", "Form Widget", etc.
     * @param modelWidget The widget
     */
    public void renderEndingBoundaryComment(Writer writer, String widgetType, ModelWidget modelWidget) throws IOException {
        if (modelWidget.boundaryCommentsEnabled()) {
            writer.write(buildBoundaryComment("End", widgetType, modelWidget.getBoundaryCommentName()));
        }
    }

    /**
     * Renders the ending boundary comment string.
     * @param buffer The buffer to write to
     * @param widgetType The widget type: "Screen Widget", "Form Widget", etc.
     * @param modelWidget The widget
     */
    public void renderEndingBoundaryComment(StringBuffer buffer, String widgetType, ModelWidget modelWidget) {
        if (modelWidget.boundaryCommentsEnabled()) {
            buffer.append(buildBoundaryComment("End", widgetType, modelWidget.getBoundaryCommentName()));
        }
    }
    
}
