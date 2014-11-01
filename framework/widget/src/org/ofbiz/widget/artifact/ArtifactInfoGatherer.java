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
package org.ofbiz.widget.artifact;

import java.util.Set;

import org.ofbiz.base.util.GeneralException;
import org.ofbiz.webapp.control.ConfigXMLReader;
import org.ofbiz.widget.ModelActionVisitor;
import org.ofbiz.widget.ModelWidgetAction;
import org.ofbiz.widget.ModelWidgetAction.EntityAnd;
import org.ofbiz.widget.ModelWidgetAction.EntityCondition;
import org.ofbiz.widget.ModelWidgetAction.EntityOne;
import org.ofbiz.widget.ModelWidgetAction.GetRelated;
import org.ofbiz.widget.ModelWidgetAction.GetRelatedOne;
import org.ofbiz.widget.ModelWidgetAction.PropertyMap;
import org.ofbiz.widget.ModelWidgetAction.PropertyToField;
import org.ofbiz.widget.ModelWidgetAction.Script;
import org.ofbiz.widget.ModelWidgetAction.Service;
import org.ofbiz.widget.ModelWidgetAction.SetField;
import org.ofbiz.widget.ModelWidgetVisitor;
import org.ofbiz.widget.form.ModelForm;
import org.ofbiz.widget.menu.ModelMenu;
import org.ofbiz.widget.screen.HtmlWidget;
import org.ofbiz.widget.screen.HtmlWidget.HtmlTemplate;
import org.ofbiz.widget.screen.HtmlWidget.HtmlTemplateDecorator;
import org.ofbiz.widget.screen.HtmlWidget.HtmlTemplateDecoratorSection;
import org.ofbiz.widget.screen.IterateSectionWidget;
import org.ofbiz.widget.screen.ModelScreen;
import org.ofbiz.widget.screen.ModelScreenWidget;
import org.ofbiz.widget.screen.ModelScreenWidget.Column;
import org.ofbiz.widget.screen.ModelScreenWidget.ColumnContainer;
import org.ofbiz.widget.screen.ModelScreenWidget.Container;
import org.ofbiz.widget.screen.ModelScreenWidget.Content;
import org.ofbiz.widget.screen.ModelScreenWidget.DecoratorScreen;
import org.ofbiz.widget.screen.ModelScreenWidget.DecoratorSection;
import org.ofbiz.widget.screen.ModelScreenWidget.DecoratorSectionInclude;
import org.ofbiz.widget.screen.ModelScreenWidget.Form;
import org.ofbiz.widget.screen.ModelScreenWidget.HorizontalSeparator;
import org.ofbiz.widget.screen.ModelScreenWidget.Image;
import org.ofbiz.widget.screen.ModelScreenWidget.IncludeScreen;
import org.ofbiz.widget.screen.ModelScreenWidget.Label;
import org.ofbiz.widget.screen.ModelScreenWidget.Link;
import org.ofbiz.widget.screen.ModelScreenWidget.Menu;
import org.ofbiz.widget.screen.ModelScreenWidget.PlatformSpecific;
import org.ofbiz.widget.screen.ModelScreenWidget.PortalPage;
import org.ofbiz.widget.screen.ModelScreenWidget.Screenlet;
import org.ofbiz.widget.screen.ModelScreenWidget.Section;
import org.ofbiz.widget.screen.ModelScreenWidget.Tree;
import org.ofbiz.widget.tree.ModelTree;

/**
 * An object that gathers artifact information from screen widgets.
 */
public final class ArtifactInfoGatherer implements ModelWidgetVisitor, ModelActionVisitor {

    private final ArtifactInfoContext infoContext;

    public ArtifactInfoGatherer(ArtifactInfoContext infoContext) {
        this.infoContext = infoContext;
    }

    @Override
    public void visit(EntityAnd entityAnd) {
        infoContext.addEntityName(entityAnd.getFinder().getEntityName());
    }

    @Override
    public void visit(EntityCondition entityCondition) {
        infoContext.addEntityName(entityCondition.getFinder().getEntityName());
    }

    @Override
    public void visit(EntityOne entityOne) {
        infoContext.addEntityName(entityOne.getFinder().getEntityName());
    }

    @Override
    public void visit(GetRelated getRelated) {
        infoContext.addEntityName(getRelated.getRelationName());
    }

    @Override
    public void visit(GetRelatedOne getRelatedOne) {
        infoContext.addEntityName(getRelatedOne.getRelationName());
    }

    @Override
    public void visit(PropertyMap propertyMap) {
    }

    @Override
    public void visit(PropertyToField propertyToField) {
    }

    @Override
    public void visit(Script script) {
    }

    @Override
    public void visit(Service service) {
        infoContext.addServiceName(service.getServiceNameExdr().getOriginal());
    }

    @Override
    public void visit(SetField setField) {
    }

    @Override
    public void visit(HtmlWidget htmlWidget) {
    }

    @Override
    public void visit(HtmlTemplate htmlTemplate) {
    }

    @Override
    public void visit(HtmlTemplateDecorator htmlTemplateDecorator) {
    }

    @Override
    public void visit(HtmlTemplateDecoratorSection htmlTemplateDecoratorSection) {
    }

    @Override
    public void visit(IterateSectionWidget iterateSectionWidget) {
        for (Section section : iterateSectionWidget.getSectionList()) {
            section.accept(this);
        }
    }

    @Override
    public void visit(ModelForm modelForm) {
    }

    @Override
    public void visit(ModelMenu modelMenu) {
    }

    @Override
    public void visit(ModelScreen modelScreen) {
        String screenLocation = modelScreen.getSourceLocation().concat("#").concat(modelScreen.getName());
        infoContext.addScreenLocation(screenLocation);
        modelScreen.getSection().accept(this);;
    }

    @Override
    public void visit(ColumnContainer columnContainer) {
        for (Column column : columnContainer.getColumns()) {
            for (ModelScreenWidget widget : column.getSubWidgets()) {
                widget.accept(this);
            }
        }
    }

    @Override
    public void visit(Container container) {
        for (ModelScreenWidget widget : container.getSubWidgets()) {
            widget.accept(this);
        }
    }

    @Override
    public void visit(Content content) {
        infoContext.addEntityName("Content");
        if (!content.getDataResourceId().isEmpty()) {
            infoContext.addEntityName("DataResource");
        }
    }

    @Override
    public void visit(DecoratorScreen decoratorScreen) {
        for (DecoratorSection section : decoratorScreen.getSectionMap().values()) {
            section.accept(this);
        }
    }

    @Override
    public void visit(DecoratorSection decoratorSection) {
        for (ModelScreenWidget widget : decoratorSection.getSubWidgets()) {
            widget.accept(this);
        }
    }

    @Override
    public void visit(DecoratorSectionInclude decoratorSectionInclude) {
    }

    @Override
    public void visit(Form form) {
        String formLocation = form.getLocation().concat("#").concat(form.getName());
        infoContext.addFormLocation(formLocation);
    }

    @Override
    public void visit(HorizontalSeparator horizontalSeparator) {
    }

    @Override
    public void visit(Image image) {
    }

    @Override
    public void visit(IncludeScreen includeScreen) {
    }

    @Override
    public void visit(Label label) {
    }

    @Override
    public void visit(Link link) {
        String target = link.getTarget(null);
        String urlMode = link.getUrlMode();
        try {
            Set<String> controllerLocAndRequestSet = ConfigXMLReader.findControllerRequestUniqueForTargetType(target, urlMode);
            if (controllerLocAndRequestSet != null) {
                for (String requestLocation : controllerLocAndRequestSet) {
                    infoContext.addRequestLocation(requestLocation);
                }
            }
        } catch (GeneralException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void visit(Menu menu) {
    }

    @Override
    public void visit(PlatformSpecific platformSpecific) {
    }

    @Override
    public void visit(PortalPage portalPage) {
    }

    @Override
    public void visit(Screenlet screenlet) {
        for (ModelScreenWidget widget : screenlet.getSubWidgets()) {
            widget.accept(this);
        }
    }

    @Override
    public void visit(Section section) {
        for (ModelWidgetAction action : section.getActions()) {
            action.accept(this);
        }
        for (ModelScreenWidget subWidget : section.getSubWidgets()) {
            subWidget.accept(this);
        }
        for (ModelScreenWidget subWidget : section.getFailWidgets()) {
            subWidget.accept(this);
        }
    }

    @Override
    public void visit(Tree tree) {
    }

    @Override
    public void visit(ModelTree modelTree) {
    }
}
