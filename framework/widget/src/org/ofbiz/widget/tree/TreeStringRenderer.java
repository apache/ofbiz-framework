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
package org.ofbiz.widget.tree;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;

import org.ofbiz.widget.screen.ScreenStringRenderer;

/**
 * Widget Library - Tree String Renderer interface
 */
public interface TreeStringRenderer {

    public void renderNodeBegin(Writer writer, Map context, ModelTree.ModelNode node, int depth, boolean isLast) throws IOException;
    public void renderNodeEnd(Writer writer, Map context, ModelTree.ModelNode node) throws IOException;
    public void renderLabel(Writer writer, Map context, ModelTree.ModelNode.Label label) throws IOException;
    public void renderLink(Writer writer, Map context, ModelTree.ModelNode.Link link) throws IOException;
    public void renderImage(Writer writer, Map context, ModelTree.ModelNode.Image image) throws IOException;
    public void renderLastElement(Writer writer, Map context, ModelTree.ModelNode node) throws IOException;
    public ScreenStringRenderer getScreenStringRenderer( Map context);
}
