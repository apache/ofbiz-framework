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
import java.util.Map;


/**
 * Widget Library - Screen String Renderer interface
 */
public interface ScreenStringRenderer {
    public void renderSectionBegin(Writer writer, Map context, ModelScreenWidget.Section section) throws IOException;
    public void renderSectionEnd(Writer writer, Map context, ModelScreenWidget.Section section) throws IOException;
    public void renderContainerBegin(Writer writer, Map context, ModelScreenWidget.Container container) throws IOException;
    public void renderContainerEnd(Writer writer, Map context, ModelScreenWidget.Container container) throws IOException;
    public void renderContentBegin(Writer writer, Map context, ModelScreenWidget.Content content) throws IOException;
    public void renderContentBody(Writer writer, Map context, ModelScreenWidget.Content content) throws IOException;
    public void renderContentEnd(Writer writer, Map context, ModelScreenWidget.Content content) throws IOException;
    public void renderSubContentBegin(Writer writer, Map context, ModelScreenWidget.SubContent content) throws IOException;
    public void renderSubContentBody(Writer writer, Map context, ModelScreenWidget.SubContent content) throws IOException;
    public void renderSubContentEnd(Writer writer, Map context, ModelScreenWidget.SubContent content) throws IOException;

    public void renderLabel(Writer writer, Map context, ModelScreenWidget.Label label) throws IOException;
    public void renderLink(Writer writer, Map context, ModelScreenWidget.Link link) throws IOException;
    public void renderImage(Writer writer, Map context, ModelScreenWidget.Image image) throws IOException;

    public void renderContentFrame(Writer writer, Map context, ModelScreenWidget.Content content) throws IOException;
}

