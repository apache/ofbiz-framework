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
package org.ofbiz.widget;

import org.ofbiz.widget.form.ModelForm;
import org.ofbiz.widget.menu.ModelMenu;
import org.ofbiz.widget.menu.ModelMenuItem;
import org.ofbiz.widget.screen.HtmlWidget;
import org.ofbiz.widget.screen.IterateSectionWidget;
import org.ofbiz.widget.screen.ModelScreen;
import org.ofbiz.widget.screen.ModelScreenWidget;
import org.ofbiz.widget.tree.ModelTree;

/**
 *  A <code>ModelWidget</code> visitor.
 */
public interface ModelWidgetVisitor {

    void visit(HtmlWidget htmlWidget);

    void visit(HtmlWidget.HtmlTemplate htmlTemplate);

    void visit(HtmlWidget.HtmlTemplateDecorator htmlTemplateDecorator);

    void visit(HtmlWidget.HtmlTemplateDecoratorSection htmlTemplateDecoratorSection);

    void visit(IterateSectionWidget iterateSectionWidget);

    void visit(ModelForm modelForm);

    void visit(ModelMenu modelMenu);

    void visit(ModelMenuItem modelMenuItem);

    void visit(ModelScreen modelScreen);

    void visit(ModelScreenWidget.ColumnContainer columnContainer);

    void visit(ModelScreenWidget.Container container);

    void visit(ModelScreenWidget.Content content);

    void visit(ModelScreenWidget.DecoratorScreen decoratorScreen);

    void visit(ModelScreenWidget.DecoratorSection decoratorSection);

    void visit(ModelScreenWidget.DecoratorSectionInclude decoratorSectionInclude);

    void visit(ModelScreenWidget.Form form);

    void visit(ModelScreenWidget.HorizontalSeparator horizontalSeparator);

    void visit(ModelScreenWidget.Image image);

    void visit(ModelScreenWidget.IncludeScreen includeScreen);

    void visit(ModelScreenWidget.Label label);

    void visit(ModelScreenWidget.Link link);

    void visit(ModelScreenWidget.Menu menu);

    void visit(ModelScreenWidget.PlatformSpecific platformSpecific);

    void visit(ModelScreenWidget.PortalPage portalPage);

    void visit(ModelScreenWidget.Screenlet screenlet);

    void visit(ModelScreenWidget.Section section);

    void visit(ModelScreenWidget.Tree tree);

    void visit(ModelTree modelTree);

    void visit(ModelTree.ModelNode modelNode);

    void visit(ModelTree.ModelNode.ModelSubNode modelSubNode);
}
