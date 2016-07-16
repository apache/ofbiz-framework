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
package org.apache.ofbiz.widget.model;

import java.util.Collection;
import java.util.Map;

import org.apache.ofbiz.widget.model.HtmlWidget.HtmlTemplate;
import org.apache.ofbiz.widget.model.HtmlWidget.HtmlTemplateDecorator;
import org.apache.ofbiz.widget.model.HtmlWidget.HtmlTemplateDecoratorSection;
import org.apache.ofbiz.widget.model.ModelScreenWidget.Column;
import org.apache.ofbiz.widget.model.ModelScreenWidget.ColumnContainer;
import org.apache.ofbiz.widget.model.ModelScreenWidget.Container;
import org.apache.ofbiz.widget.model.ModelScreenWidget.Content;
import org.apache.ofbiz.widget.model.ModelScreenWidget.DecoratorScreen;
import org.apache.ofbiz.widget.model.ModelScreenWidget.DecoratorSection;
import org.apache.ofbiz.widget.model.ModelScreenWidget.DecoratorSectionInclude;
import org.apache.ofbiz.widget.model.ModelScreenWidget.Form;
import org.apache.ofbiz.widget.model.ModelScreenWidget.Grid;
import org.apache.ofbiz.widget.model.ModelScreenWidget.HorizontalSeparator;
import org.apache.ofbiz.widget.model.ModelScreenWidget.IncludeScreen;
import org.apache.ofbiz.widget.model.ModelScreenWidget.Label;
import org.apache.ofbiz.widget.model.ModelScreenWidget.Menu;
import org.apache.ofbiz.widget.model.ModelScreenWidget.PlatformSpecific;
import org.apache.ofbiz.widget.model.ModelScreenWidget.PortalPage;
import org.apache.ofbiz.widget.model.ModelScreenWidget.ScreenImage;
import org.apache.ofbiz.widget.model.ModelScreenWidget.ScreenLink;
import org.apache.ofbiz.widget.model.ModelScreenWidget.Screenlet;
import org.apache.ofbiz.widget.model.ModelScreenWidget.Section;
import org.apache.ofbiz.widget.model.ModelScreenWidget.Tree;
import org.apache.ofbiz.widget.model.ModelTree.ModelNode;
import org.apache.ofbiz.widget.model.ModelTree.ModelNode.ModelSubNode;

/**
 * An object that generates an XML representation from widget models.
 * The generated XML is unformatted - if you want to
 * "pretty print" the XML, then use a transformer.
 *
 */
public class XmlWidgetVisitor extends XmlAbstractWidgetVisitor implements ModelWidgetVisitor {

    private final ModelFieldVisitor fieldVisitor;
    private final ModelActionVisitor actionVisitor;
    private final ModelConditionVisitor conditionVisitor;

    public XmlWidgetVisitor(Appendable writer) {
        super(writer);
        this.fieldVisitor = new XmlWidgetFieldVisitor(writer);
        this.actionVisitor = new XmlWidgetActionVisitor(writer);
        this.conditionVisitor = new XmlWidgetConditionVisitor(writer);
    }

    @Override
    public void visit(Column column) throws Exception {
        writer.append("<column");
        visitModelWidget(column);
        visitAttribute("id", column.getIdExdr());
        visitAttribute("style", column.getStyleExdr());
        writer.append(">");
        visitSubWidgets(column.getSubWidgets());
        writer.append("</column>");
    }

    @Override
    public void visit(ColumnContainer columnContainer) throws Exception {
        writer.append("<column-container");
        visitModelWidget(columnContainer);
        visitAttribute("id", columnContainer.getIdExdr());
        visitAttribute("style", columnContainer.getStyleExdr());
        writer.append(">");
        visitSubWidgets(columnContainer.getColumns());
        writer.append("</column-container>");
    }

    @Override
    public void visit(Container container) throws Exception {
        writer.append("<container");
        visitModelWidget(container);
        visitAttribute("auto-update-interval", container.getAutoUpdateInterval());
        visitAttribute("auto-update-target", container.getAutoUpdateTargetExdr());
        visitAttribute("id", container.getIdExdr());
        visitAttribute("style", container.getStyleExdr());
        writer.append(">");
        visitSubWidgets(container.getSubWidgets());
        writer.append("</container>");
    }

    @Override
    public void visit(Content content) throws Exception {
        writer.append("<content");
        visitModelWidget(content);
        visitAttribute("border", content.getBorder());
        visitAttribute("content-id", content.getContentId());
        visitAttribute("dataresource-id", content.getDataResourceId());
        visitAttribute("edit-container-style", content.getEditContainerStyle());
        visitAttribute("edit-request", content.getEditRequest());
        visitAttribute("enable-edit-name", content.getEnableEditName());
        visitAttribute("height", content.getHeight());
        visitAttribute("width", content.getWidth());
        visitAttribute("xml-escape", content.xmlEscape());
        writer.append("/>");
    }

    @Override
    public void visit(DecoratorScreen decoratorScreen) throws Exception {
        writer.append("<decorator-screen");
        visitModelWidget(decoratorScreen);
        visitAttribute("location", decoratorScreen.getLocationExdr());
        writer.append(">");
        visitSubWidgets(decoratorScreen.getSectionMap().values());
        writer.append("</decorator-screen>");
    }

    @Override
    public void visit(DecoratorSection decoratorSection) throws Exception {
        writer.append("<decorator-section");
        visitModelWidget(decoratorSection);
        writer.append(">");
        visitSubWidgets(decoratorSection.getSubWidgets());
        writer.append("</decorator-screen>");
    }

    @Override
    public void visit(DecoratorSectionInclude decoratorSectionInclude) throws Exception {
        writer.append("<decorator-section-include");
        visitModelWidget(decoratorSectionInclude);
        writer.append("/>");
    }

    @Override
    public void visit(Form form) throws Exception {
        writer.append("<include-form");
        visitModelWidget(form);
        visitAttribute("location", form.getLocationExdr());
        visitAttribute("share-scope", form.getShareScopeExdr());
        writer.append("/>");
    }

    @Override
    public void visit(HorizontalSeparator horizontalSeparator) throws Exception {
        writer.append("<horizontal-separator");
        visitModelWidget(horizontalSeparator);
        visitAttribute("id", horizontalSeparator.getIdExdr());
        visitAttribute("style", horizontalSeparator.getStyleExdr());
        writer.append("/>");
    }

    @Override
    public void visit(HtmlTemplate htmlTemplate) throws Exception {
        writer.append("<html-template");
        visitModelWidget(htmlTemplate);
        visitAttribute("location", htmlTemplate.getLocationExdr());
        writer.append("/>");
    }

    @Override
    public void visit(HtmlTemplateDecorator htmlTemplateDecorator) throws Exception {
        writer.append("<html-template-decorator");
        visitModelWidget(htmlTemplateDecorator);
        visitAttribute("location", htmlTemplateDecorator.getLocationExdr());
        writer.append(">");
        visitSubWidgets(htmlTemplateDecorator.getSectionMap().values());
        writer.append("</html-template-decorator>");
    }

    @Override
    public void visit(HtmlTemplateDecoratorSection htmlTemplateDecoratorSection) throws Exception {
        writer.append("<html-template-decorator-section");
        visitModelWidget(htmlTemplateDecoratorSection);
        writer.append(">");
        visitSubWidgets(htmlTemplateDecoratorSection.getSubWidgets());
        writer.append("</html-template-decorator-section>");
    }

    @Override
    public void visit(HtmlWidget htmlWidget) throws Exception {
        visitSubWidgets(htmlWidget.getSubWidgets());
    }

    @Override
    public void visit(IncludeScreen includeScreen) throws Exception {
        writer.append("<include-screen");
        visitModelWidget(includeScreen);
        visitAttribute("location", includeScreen.getLocationExdr());
        visitAttribute("share-scope", includeScreen.getShareScopeExdr());
        writer.append("/>");
    }

    @Override
    public void visit(IterateSectionWidget iterateSectionWidget) throws Exception {
        writer.append("<iterate-section");
        visitModelWidget(iterateSectionWidget);
        visitAttribute("entry", iterateSectionWidget.getEntryNameExdr());
        visitAttribute("key", iterateSectionWidget.getKeyNameExdr());
        visitAttribute("list", iterateSectionWidget.getListNameExdr());
        visitAttribute("paginate", iterateSectionWidget.getPaginate());
        visitAttribute("paginate-target", iterateSectionWidget.getPaginateTarget());
        visitAttribute("view-size", iterateSectionWidget.getViewSize());
        writer.append(">");
        visitSubWidgets(iterateSectionWidget.getSectionList());
        writer.append("</iterate-section>");
    }

    @Override
    public void visit(Label label) throws Exception {
        writer.append("<label");
        visitModelWidget(label);
        visitAttribute("id", label.getIdExdr());
        visitAttribute("style", label.getStyleExdr());
        writer.append(">");
        writer.append(label.getTextExdr().getOriginal());
        writer.append("</label>");
    }

    @Override
    public void visit(Menu menu) throws Exception {
        writer.append("<include-menu");
        visitModelWidget(menu);
        visitAttribute("location", menu.getLocationExdr());
        writer.append("/>");
    }

    @Override
    public void visit(ModelSingleForm modelForm) throws Exception {
        writer.append("<form");
        visitModelForm(modelForm);
        writer.append("</form>");
    }

    @Override
    public void visit(ModelGrid modelGrid) throws Exception {
        writer.append("<grid");
        visitModelForm(modelGrid);
        writer.append("</grid>");
    }

    @Override
    public void visit(Grid grid) throws Exception {
        writer.append("<include-grid");
        visitModelWidget(grid);
        visitAttribute("location", grid.getLocationExdr());
        visitAttribute("share-scope", grid.getShareScopeExdr());
        writer.append("/>");
    }

    @Override
    public void visit(ModelMenu modelMenu) throws Exception {
        writer.append("<menu");
        visitModelWidget(modelMenu);
        if (modelMenu.getParentMenu() != null) {
            visitAttribute("extends", modelMenu.getParentMenu().getName());
            visitAttribute("extends-resource", modelMenu.getParentMenu().getMenuLocation());
        }
        visitAttribute("type", modelMenu.getType());
        visitAttribute("target", modelMenu.getTarget());
        visitAttribute("id", modelMenu.getId());
        visitAttribute("title", modelMenu.getTitle());
        visitAttribute("tooltip", modelMenu.getTooltip());
        visitAttribute("default-entity-name", modelMenu.getDefaultEntityName());
        visitAttribute("default-title-style", modelMenu.getDefaultTitleStyle());
        visitAttribute("default-selected-style", modelMenu.getDefaultSelectedStyle());
        visitAttribute("default-widget-style", modelMenu.getDefaultWidgetStyle());
        visitAttribute("default-tooltip-style", modelMenu.getDefaultTooltipStyle());
        visitAttribute("default-menu-item-name", modelMenu.getDefaultMenuItemName());
        visitAttribute("default-permission-operation", modelMenu.getDefaultPermissionOperation());
        visitAttribute("default-permission-entity-action", modelMenu.getDefaultPermissionEntityAction());
        visitAttribute("default-associated-content-id", modelMenu.getDefaultAssociatedContentId());
        visitAttribute("orientation", modelMenu.getOrientation());
        visitAttribute("menu-width", modelMenu.getMenuWidth());
        visitAttribute("default-cell-width", modelMenu.getDefaultCellWidth());
        visitAttribute("default-hide-if-selected", modelMenu.getDefaultHideIfSelected());
        visitAttribute("default-disabled-title-style", modelMenu.getDefaultDisabledTitleStyle());
        visitAttribute("selected-menuitem-context-field-name", modelMenu.getSelectedMenuItemContextFieldName());
        visitAttribute("menu-container-style", modelMenu.getMenuContainerStyleExdr());
        visitAttribute("default-align", modelMenu.getDefaultAlign());
        visitAttribute("default-align-style", modelMenu.getDefaultAlignStyle());
        visitAttribute("fill-style", modelMenu.getFillStyle());
        visitAttribute("extra-index", modelMenu.getExtraIndex());
        writer.append(">");
        if (!modelMenu.getActions().isEmpty()) {
            writer.append("<actions>");
            visitActions(modelMenu.getActions());
            writer.append("</actions>");
        }
        for (ModelMenuItem menuItem : modelMenu.getMenuItemList()) {
            menuItem.accept(this);
        }
        writer.append("</menu>");
    }

    @Override
    public void visit(ModelMenuItem modelMenuItem) throws Exception {
        writer.append("<menu-item");
        visitModelWidget(modelMenuItem);
        visitAttribute("entity-name", modelMenuItem.getEntityName());
        visitAttribute("title", modelMenuItem.getTitle());
        visitAttribute("tooltip", modelMenuItem.getTooltip());
        visitAttribute("parent-portal-page-value", modelMenuItem.getParentPortalPageId());
        visitAttribute("title-style", modelMenuItem.getTitleStyle());
        visitAttribute("disabled-title-style", modelMenuItem.getDisabledTitleStyle());
        visitAttribute("widget-style", modelMenuItem.getWidgetStyle());
        visitAttribute("tooltip-style", modelMenuItem.getTooltipStyle());
        visitAttribute("selected-style", modelMenuItem.getSelectedStyle());
        visitAttribute("hide-if-selected", modelMenuItem.getHideIfSelected());
        visitAttribute("disable-if-empty", modelMenuItem.getDisableIfEmpty());
        visitAttribute("align", modelMenuItem.getAlign());
        visitAttribute("align-style", modelMenuItem.getAlignStyle());
        visitAttribute("position", modelMenuItem.getPosition());
        visitAttribute("associated-content-id", modelMenuItem.getAssociatedContentId());
        visitAttribute("cell-width", modelMenuItem.getCellWidth());
        visitAttribute("sub-menu", modelMenuItem.getSubMenu());
        writer.append(">");
        if (modelMenuItem.getCondition() != null) {
            modelMenuItem.getCondition().getCondition().accept(conditionVisitor);
        }
        if (!modelMenuItem.getActions().isEmpty()) {
            writer.append("<actions>");
            visitActions(modelMenuItem.getActions());
            writer.append("</actions>");
        }
        if (modelMenuItem.getLink() != null) {
            visitLink(modelMenuItem.getLink().getLink());
        }
        for (ModelMenuItem menuItem : modelMenuItem.getMenuItemList()) {
            menuItem.accept(this);
            ;
        }
        writer.append("</menu-item>");
    }

    @Override
    public void visit(ModelNode modelNode) throws Exception {
        writer.append("<node");
        visitModelWidget(modelNode);
        visitAttribute("expand-collapse-style", modelNode.getExpandCollapseStyle());
        visitAttribute("wrap-style", modelNode.getWrapStyleExdr());
        visitAttribute("render-style", modelNode.getRenderStyle());
        visitAttribute("entry-name", modelNode.getEntryName());
        visitAttribute("entity-name", modelNode.getEntityName());
        visitAttribute("join-field-name", modelNode.getPkName());
        writer.append(">");
        if (modelNode.getCondition() != null) {
            modelNode.getCondition().getCondition().accept(conditionVisitor);
        }
        if (!modelNode.getActions().isEmpty()) {
            writer.append("<actions>");
            visitActions(modelNode.getActions());
            writer.append("</actions>");
        }
        if (!modelNode.getScreenNameExdr().isEmpty()) {
            writer.append("<include-screen");
            visitAttribute("name", modelNode.getScreenNameExdr());
            visitAttribute("location", modelNode.getScreenLocationExdr());
            visitAttribute("share-scope", modelNode.getShareScope());
            visitAttribute("name", modelNode.getScreenNameExdr());
            writer.append("/>");
        }
        if (modelNode.getLabel() != null) {
            ModelNode.Label label = modelNode.getLabel();
            writer.append("<label");
            visitAttribute("id", label.getIdExdr());
            visitAttribute("style", label.getStyleExdr());
            writer.append(">");
            writer.append(label.getTextExdr().getOriginal());
            writer.append("</label>");
        }
        if (modelNode.getLink() != null) {
            ModelNode.Link link = modelNode.getLink();
            writer.append("<link");
            visitAttribute("encode", link.getEncode());
            visitAttribute("full-path", link.getFullPath());
            visitAttribute("id", link.getIdExdr());
            visitAttribute("link-type", link.getLinkType());
            visitAttribute("prefix", link.getPrefixExdr());
            visitAttribute("secure", link.getSecure());
            visitAttribute("style", link.getStyleExdr());
            visitAttribute("target", link.getTargetExdr());
            visitAttribute("target-window", link.getTargetWindowExdr());
            visitAttribute("text", link.getTextExdr());
            visitAttribute("url-mode", link.getUrlMode());
            if (!link.getParameterList().isEmpty()) {
                writer.append(">");
                visitParameters(link.getParameterList());
                writer.append("</link>");
            } else {
                writer.append("/>");
            }
        }
        writer.append("</node>");
    }

    @Override
    public void visit(ModelScreen modelScreen) throws Exception {
        writer.append("<screen");
        visitModelWidget(modelScreen);
        visitAttribute("transaction-timeout", modelScreen.getTransactionTimeout());
        visitAttribute("use-transaction", modelScreen.getUseTransaction());
        visitAttribute("use-cache", modelScreen.getUseCache());
        writer.append(">");
        modelScreen.getSection().accept(this);
        writer.append("</screen>");
    }

    @Override
    public void visit(ModelSubNode modelSubNode) throws Exception {
        writer.append("<sub-node");
        visitModelWidget(modelSubNode);
        visitAttribute("node-name", modelSubNode.getNodeNameExdr());
        writer.append(">");
        if (!modelSubNode.getActions().isEmpty()) {
            writer.append("<actions>");
            visitActions(modelSubNode.getActions());
            writer.append("</actions>");
        }
        writer.append("</sub-node>");
    }

    @Override
    public void visit(ModelTree modelTree) throws Exception {
        writer.append("<tree");
        visitModelWidget(modelTree);
        visitAttribute("root-node-name", modelTree.getRootNodeName());
        visitAttribute("default-render-style", modelTree.getDefaultRenderStyle());
        visitAttribute("render-style", modelTree.getDefaultRenderStyle());
        visitAttribute("default-wrap-style", modelTree.getDefaultWrapStyleExdr());
        visitAttribute("expand-collapse-request", modelTree.getExpandCollapseRequestExdr());
        visitAttribute("trail-name", modelTree.getTrailNameExdr());
        visitAttribute("force-child-check", modelTree.getForceChildCheck());
        visitAttribute("entity-name", modelTree.getDefaultEntityName());
        visitAttribute("open-depth", modelTree.getOpenDepth());
        visitAttribute("post-trail-open-depth", modelTree.getPostTrailOpenDepth());
        writer.append(">");
        visitSubWidgets(modelTree.getNodeMap().values());
        writer.append("</tree>");
    }

    @Override
    public void visit(PlatformSpecific platformSpecific) throws Exception {
        writer.append("<platform-specific>");
        for (Map.Entry<String, ModelScreenWidget> entry : platformSpecific.getSubWidgets().entrySet()) {
            writer.append("<").append(entry.getKey()).append(">");
            entry.getValue().accept(this);
            writer.append("</").append(entry.getKey()).append(">");
        }
        writer.append("</platform-specific>");
    }

    @Override
    public void visit(PortalPage portalPage) throws Exception {
        writer.append("<include-portal-page");
        visitModelWidget(portalPage);
        visitAttribute("id", portalPage.getIdExdr());
        visitAttribute("conf-mode", portalPage.getConfModeExdr());
        visitAttribute("use-private", portalPage.getUsePrivate());
        writer.append("/>");
    }

    @Override
    public void visit(ScreenImage image) throws Exception {
        visitImage(image.getImage());
    }

    @Override
    public void visit(Screenlet screenlet) throws Exception {
        writer.append("<screenlet");
        visitModelWidget(screenlet);
        visitAttribute("id", screenlet.getIdExdr());
        visitAttribute("collapsible", screenlet.getCollapsible());
        visitAttribute("initially-collapsed", screenlet.getInitiallyCollapsed());
        visitAttribute("save-collapsed", screenlet.getSaveCollapsed());
        visitAttribute("padded", screenlet.getPadded());
        visitAttribute("title", screenlet.getTitleExdr());
        writer.append(">");
        visitSubWidgets(screenlet.getSubWidgets());
        writer.append("</screenlet>");
    }

    @Override
    public void visit(ScreenLink link) throws Exception {
        visitLink(link.getLink());
    }

    @Override
    public void visit(Section section) throws Exception {
        writer.append("<section");
        visitModelWidget(section);
        writer.append(">");
        if (section.getCondition() != null) {
            writer.append("<condition>");
            section.getCondition().accept(conditionVisitor);
            writer.append("</condition>");
        }
        if (!section.getActions().isEmpty()) {
            writer.append("<actions>");
            visitActions(section.getActions());
            writer.append("</actions>");
        }
        if (!section.getSubWidgets().isEmpty()) {
            writer.append("<widgets>");
            visitSubWidgets(section.getSubWidgets());
            writer.append("</widgets>");
        }
        if (!section.getFailWidgets().isEmpty()) {
            writer.append("<fail-widgets>");
            visitSubWidgets(section.getFailWidgets());
            writer.append("</fail-widgets>");
        }
        writer.append("</section>");
    }

    @Override
    public void visit(Tree tree) throws Exception {
        writer.append("<include-tree");
        visitModelWidget(tree);
        visitAttribute("location", tree.getLocationExdr());
        visitAttribute("share-scope", tree.getShareScopeExdr());
        writer.append("/>");
    }

    private void visitActions(Collection<? extends ModelAction> actions) throws Exception {
        for (ModelAction action : actions) {
            action.accept(actionVisitor);
        }
    }

    public void visitModelForm(ModelForm modelForm) throws Exception {
        visitModelWidget(modelForm);
        if (modelForm.getParentModelForm() != null) {
            visitAttribute("extends", modelForm.getParentModelForm().getName());
            visitAttribute("extends-resource", modelForm.getParentModelForm().getFormLocation());
        }
        visitAttribute("view-size", modelForm.getDefaultViewSize());
        visitAttribute("type", modelForm.getType());
        visitAttribute("target", modelForm.getTarget());
        visitAttribute("id", modelForm.getContainerId());
        visitAttribute("style", modelForm.getContainerStyle());
        visitAttribute("title", modelForm.getTitle());
        visitAttribute("tooltip", modelForm.getTooltip());
        visitAttribute("list-name", modelForm.getListName());
        visitAttribute("list-entry-name", modelForm.getListEntryName());
        visitAttribute("default-entity-name", modelForm.getDefaultEntityName());
        visitAttribute("default-service-name", modelForm.getDefaultServiceName());
        visitAttribute("form-title-area-style", modelForm.getFormTitleAreaStyle());
        visitAttribute("form-widget-area-style", modelForm.getFormWidgetAreaStyle());
        visitAttribute("default-title-area-style", modelForm.getDefaultTitleAreaStyle());
        visitAttribute("default-widget-area-style", modelForm.getDefaultWidgetAreaStyle());
        visitAttribute("odd-row-style", modelForm.getOddRowStyle());
        visitAttribute("even-row-style", modelForm.getEvenRowStyle());
        visitAttribute("default-table-style", modelForm.getDefaultTableStyle());
        visitAttribute("header-row-style", modelForm.getHeaderRowStyle());
        visitAttribute("default-title-style", modelForm.getDefaultTitleStyle());
        visitAttribute("default-widget-style", modelForm.getDefaultWidgetStyle());
        visitAttribute("default-tooltip-style", modelForm.getDefaultTooltipStyle());
        visitAttribute("item-index-separator", modelForm.getItemIndexSeparator());
        visitAttribute("separate-columns", modelForm.getSeparateColumns());
        visitAttribute("group-columns", modelForm.getGroupColumns());
        visitAttribute("target-type", modelForm.getTargetType());
        visitAttribute("default-map-name", modelForm.getDefaultMapName());
        visitAttribute("target-window", modelForm.getTargetWindow());
        visitAttribute("hide-header", modelForm.getHideHeader());
        visitAttribute("client-autocomplete-fields", modelForm.getClientAutocompleteFields());
        visitAttribute("paginate-target", modelForm.getPaginateTarget());
        visitAttribute("sort-field-parameter-name", modelForm.getSortFieldParameterName());
        visitAttribute("default-required-field-style", modelForm.getDefaultRequiredFieldStyle());
        visitAttribute("default-sort-field-style", modelForm.getDefaultSortFieldStyle());
        visitAttribute("default-sort-field-asc-style", modelForm.getDefaultSortFieldAscStyle());
        visitAttribute("default-sort-field-desc-style", modelForm.getDefaultSortFieldDescStyle());
        visitAttribute("paginate-target-anchor", modelForm.getPaginateTargetAnchor());
        visitAttribute("paginate-index-field", modelForm.getPaginateIndexField());
        visitAttribute("paginate-size-field", modelForm.getPaginateSizeField());
        visitAttribute("override-list-size", modelForm.getOverrideListSize());
        visitAttribute("paginate-first-label", modelForm.getPaginateFirstLabel());
        visitAttribute("paginate-previous-label", modelForm.getPaginatePreviousLabel());
        visitAttribute("paginate-next-label", modelForm.getPaginateNextLabel());
        visitAttribute("paginate-last-label", modelForm.getPaginateLastLabel());
        visitAttribute("paginate-viewsize-label", modelForm.getPaginateViewSizeLabel());
        visitAttribute("paginate-style", modelForm.getPaginateStyle());
        visitAttribute("paginate", modelForm.getPaginate());
        visitAttribute("skip-start", modelForm.getSkipStart());
        visitAttribute("skip-end", modelForm.getSkipEnd());
        visitAttribute("use-row-submit", modelForm.getUseRowSubmit());
        visitAttribute("row-count", modelForm.getRowCount());
        visitAttribute("focus-field-name", modelForm.getFocusFieldName());
        writer.append(">");
        if (!modelForm.getActions().isEmpty()) {
            writer.append("<actions>");
            visitActions(modelForm.getActions());
            writer.append("</actions>");
        }
        if (!modelForm.getRowActions().isEmpty()) {
            writer.append("<row-actions>");
            visitActions(modelForm.getRowActions());
            writer.append("</row-actions>");
        }
        for (ModelForm.AltRowStyle rowStyle : modelForm.getAltRowStyles()) {
            writer.append("<alt-row-style");
            visitAttribute("use-when", rowStyle.useWhen);
            visitAttribute("style", rowStyle.style);
            writer.append("/>");
        }
        for (ModelForm.AltTarget target : modelForm.getAltTargets()) {
            writer.append("<alt-target");
            visitAttribute("use-when", target.useWhen);
            visitAttribute("target", target.targetExdr);
            writer.append("/>");
        }
        for (ModelForm.AutoFieldsService service : modelForm.getAutoFieldsServices()) {
            writer.append("<auto-fields-service");
            visitAttribute("service-name", service.serviceName);
            visitAttribute("map-name", service.mapName);
            visitAttribute("default-field-type", service.defaultFieldType);
            visitAttribute("default-position", service.defaultPosition);
            writer.append("/>");
        }
        for (ModelForm.AutoFieldsEntity entity : modelForm.getAutoFieldsEntities()) {
            writer.append("<auto-fields-entity");
            visitAttribute("entity-name", entity.entityName);
            visitAttribute("map-name", entity.mapName);
            visitAttribute("default-field-type", entity.defaultFieldType);
            visitAttribute("default-position", entity.defaultPosition);
            writer.append("/>");
        }
        for (ModelFormField field : modelForm.getFieldList()) {
            field.getFieldInfo().accept(fieldVisitor);
        }
        visitUpdateAreas(modelForm.getOnPaginateUpdateAreas());
        visitUpdateAreas(modelForm.getOnSortColumnUpdateAreas());
        visitUpdateAreas(modelForm.getOnSubmitUpdateAreas());
    }

    private void visitSubWidgets(Collection<? extends ModelWidget> subWidgets) throws Exception {
        for (ModelWidget subWidget : subWidgets) {
            subWidget.accept(this);
        }
    }

    private void visitUpdateAreas(Collection<ModelForm.UpdateArea> updateAreas) throws Exception {
        for (ModelForm.UpdateArea updateArea : updateAreas) {
            writer.append("<on-event-update-area");
            visitAttribute("event-type", updateArea.getEventType());
            visitAttribute("area-id", updateArea.getAreaId());
            visitAttribute("area-target", updateArea.getAreaTarget());
            writer.append(">");
            visitAutoEntityParameters(updateArea.getAutoEntityParameters());
            visitAutoServiceParameters(updateArea.getAutoServiceParameters());
            visitParameters(updateArea.getParameterList());
            writer.append("</on-event-update-area>");
        }
    }
}
