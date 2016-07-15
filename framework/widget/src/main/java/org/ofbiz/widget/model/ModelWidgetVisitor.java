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
package org.ofbiz.widget.model;


/**
 *  A <code>ModelWidget</code> visitor.
 */
public interface ModelWidgetVisitor {

    void visit(HtmlWidget htmlWidget) throws Exception;

    void visit(HtmlWidget.HtmlTemplate htmlTemplate) throws Exception;

    void visit(HtmlWidget.HtmlTemplateDecorator htmlTemplateDecorator) throws Exception;

    void visit(HtmlWidget.HtmlTemplateDecoratorSection htmlTemplateDecoratorSection) throws Exception;

    void visit(IterateSectionWidget iterateSectionWidget) throws Exception;

    void visit(ModelSingleForm modelForm) throws Exception;

    void visit(ModelGrid modelGrid) throws Exception;

    void visit(ModelMenu modelMenu) throws Exception;

    void visit(ModelMenuItem modelMenuItem) throws Exception;

    void visit(ModelScreen modelScreen) throws Exception;

    void visit(ModelScreenWidget.ColumnContainer columnContainer) throws Exception;

    void visit(ModelScreenWidget.Container container) throws Exception;

    void visit(ModelScreenWidget.Content content) throws Exception;

    void visit(ModelScreenWidget.DecoratorScreen decoratorScreen) throws Exception;

    void visit(ModelScreenWidget.DecoratorSection decoratorSection) throws Exception;

    void visit(ModelScreenWidget.DecoratorSectionInclude decoratorSectionInclude) throws Exception;

    void visit(ModelScreenWidget.Form form) throws Exception;

    void visit(ModelScreenWidget.Grid grid) throws Exception;

    void visit(ModelScreenWidget.HorizontalSeparator horizontalSeparator) throws Exception;

    void visit(ModelScreenWidget.ScreenImage image) throws Exception;

    void visit(ModelScreenWidget.IncludeScreen includeScreen) throws Exception;

    void visit(ModelScreenWidget.Label label) throws Exception;

    void visit(ModelScreenWidget.ScreenLink link) throws Exception;

    void visit(ModelScreenWidget.Menu menu) throws Exception;

    void visit(ModelScreenWidget.PlatformSpecific platformSpecific) throws Exception;

    void visit(ModelScreenWidget.PortalPage portalPage) throws Exception;

    void visit(ModelScreenWidget.Screenlet screenlet) throws Exception;

    void visit(ModelScreenWidget.Section section) throws Exception;

    void visit(ModelScreenWidget.Tree tree) throws Exception;

    void visit(ModelTree modelTree) throws Exception;

    void visit(ModelTree.ModelNode modelNode) throws Exception;

    void visit(ModelTree.ModelNode.ModelSubNode modelSubNode) throws Exception;

    void visit(ModelScreenWidget.Column column) throws Exception;
}
