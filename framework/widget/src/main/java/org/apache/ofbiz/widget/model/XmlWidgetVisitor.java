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
        getWriter().append("<column");
        visitModelWidget(column);
        visitAttribute("id", column.getIdExdr());
        visitAttribute("style", column.getStyleExdr());
        getWriter().append(">");
        visitSubWidgets(column.getSubWidgets());
        getWriter().append("</column>");
    }

    @Override
    public void visit(ColumnContainer columnContainer) throws Exception {
        getWriter().append("<column-container");
        visitModelWidget(columnContainer);
        visitAttribute("id", columnContainer.getIdExdr());
        visitAttribute("style", columnContainer.getStyleExdr());
        getWriter().append(">");
        visitSubWidgets(columnContainer.getColumns());
        getWriter().append("</column-container>");
    }

    @Override
    public void visit(Container container) throws Exception {
        getWriter().append("<container");
        visitModelWidget(container);
        visitAttribute("auto-update-interval", container.getAutoUpdateInterval());
        visitAttribute("auto-update-target", container.getAutoUpdateTargetExdr());
        visitAttribute("id", container.getIdExdr());
        visitAttribute("style", container.getStyleExdr());
        getWriter().append(">");
        visitSubWidgets(container.getSubWidgets());
        getWriter().append("</container>");
    }

    @Override
    public void visit(Content content) throws Exception {
        getWriter().append("<content");
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
        getWriter().append("/>");
    }

    @Override
    public void visit(DecoratorScreen decoratorScreen) throws Exception {
        getWriter().append("<decorator-screen");
        visitModelWidget(decoratorScreen);
        visitAttribute("location", decoratorScreen.getLocationExdr());
        getWriter().append(">");
        visitSubWidgets(decoratorScreen.getSectionMap().values());
        getWriter().append("</decorator-screen>");
    }

    @Override
    public void visit(DecoratorSection decoratorSection) throws Exception {
        getWriter().append("<decorator-section");
        visitModelWidget(decoratorSection);
        getWriter().append(">");
        visitSubWidgets(decoratorSection.getSubWidgets());
        getWriter().append("</decorator-screen>");
    }

    @Override
    public void visit(DecoratorSectionInclude decoratorSectionInclude) throws Exception {
        getWriter().append("<decorator-section-include");
        visitModelWidget(decoratorSectionInclude);
        getWriter().append("/>");
    }

    @Override
    public void visit(Form form) throws Exception {
        getWriter().append("<include-form");
        visitModelWidget(form);
        visitAttribute("location", form.getLocationExdr());
        visitAttribute("share-scope", form.getShareScopeExdr());
        getWriter().append("/>");
    }

    @Override
    public void visit(HorizontalSeparator horizontalSeparator) throws Exception {
        getWriter().append("<horizontal-separator");
        visitModelWidget(horizontalSeparator);
        visitAttribute("id", horizontalSeparator.getIdExdr());
        visitAttribute("style", horizontalSeparator.getStyleExdr());
        getWriter().append("/>");
    }

    @Override
    public void visit(HtmlTemplate htmlTemplate) throws Exception {
        getWriter().append("<html-template");
        visitModelWidget(htmlTemplate);
        visitAttribute("location", htmlTemplate.getLocationExdr());
        visitAttribute("multi-block", htmlTemplate.isMultiBlock());
        getWriter().append("/>");
    }

    @Override
    public void visit(HtmlTemplateDecorator htmlTemplateDecorator) throws Exception {
        getWriter().append("<html-template-decorator");
        visitModelWidget(htmlTemplateDecorator);
        visitAttribute("location", htmlTemplateDecorator.getLocationExdr());
        getWriter().append(">");
        visitSubWidgets(htmlTemplateDecorator.getSectionMap().values());
        getWriter().append("</html-template-decorator>");
    }

    @Override
    public void visit(HtmlTemplateDecoratorSection htmlTemplateDecoratorSection) throws Exception {
        getWriter().append("<html-template-decorator-section");
        visitModelWidget(htmlTemplateDecoratorSection);
        getWriter().append(">");
        visitSubWidgets(htmlTemplateDecoratorSection.getSubWidgets());
        getWriter().append("</html-template-decorator-section>");
    }

    @Override
    public void visit(HtmlWidget htmlWidget) throws Exception {
        visitSubWidgets(htmlWidget.getSubWidgets());
    }

    @Override
    public void visit(IncludeScreen includeScreen) throws Exception {
        getWriter().append("<include-screen");
        visitModelWidget(includeScreen);
        visitAttribute("location", includeScreen.getLocationExdr());
        visitAttribute("share-scope", includeScreen.getShareScopeExdr());
        getWriter().append("/>");
    }

    @Override
    public void visit(IterateSectionWidget iterateSectionWidget) throws Exception {
        getWriter().append("<iterate-section");
        visitModelWidget(iterateSectionWidget);
        visitAttribute("entry", iterateSectionWidget.getEntryNameExdr());
        visitAttribute("key", iterateSectionWidget.getKeyNameExdr());
        visitAttribute("list", iterateSectionWidget.getListNameExdr());
        visitAttribute("paginate", iterateSectionWidget.getPaginate());
        visitAttribute("paginate-target", iterateSectionWidget.getPaginateTarget());
        visitAttribute("view-size", iterateSectionWidget.getViewSize());
        getWriter().append(">");
        visitSubWidgets(iterateSectionWidget.getSectionList());
        getWriter().append("</iterate-section>");
    }

    @Override
    public void visit(Label label) throws Exception {
        getWriter().append("<label");
        visitModelWidget(label);
        visitAttribute("id", label.getIdExdr());
        visitAttribute("style", label.getStyleExdr());
        getWriter().append(">");
        getWriter().append(label.getTextExdr().getOriginal());
        getWriter().append("</label>");
    }

    @Override
    public void visit(Menu menu) throws Exception {
        getWriter().append("<include-menu");
        visitModelWidget(menu);
        visitAttribute("location", menu.getLocationExdr());
        getWriter().append("/>");
    }

    @Override
    public void visit(ModelSingleForm modelForm) throws Exception {
        getWriter().append("<form");
        visitModelForm(modelForm);
        getWriter().append("</form>");
    }

    @Override
    public void visit(ModelGrid modelGrid) throws Exception {
        getWriter().append("<grid");
        visitModelForm(modelGrid);
        getWriter().append("</grid>");
    }

    @Override
    public void visit(Grid grid) throws Exception {
        getWriter().append("<include-grid");
        visitModelWidget(grid);
        visitAttribute("location", grid.getLocationExdr());
        visitAttribute("share-scope", grid.getShareScopeExdr());
        getWriter().append("/>");
    }

    @Override
    public void visit(ModelMenu modelMenu) throws Exception {
        getWriter().append("<menu");
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
        getWriter().append(">");
        if (!modelMenu.getActions().isEmpty()) {
            getWriter().append("<actions>");
            visitActions(modelMenu.getActions());
            getWriter().append("</actions>");
        }
        for (ModelMenuItem menuItem : modelMenu.getMenuItemList()) {
            menuItem.accept(this);
        }
        getWriter().append("</menu>");
    }

    @Override
    public void visit(ModelMenuItem modelMenuItem) throws Exception {
        getWriter().append("<menu-item");
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
        getWriter().append(">");
        if (modelMenuItem.getCondition() != null) {
            modelMenuItem.getCondition().getCondition().accept(conditionVisitor);
        }
        if (!modelMenuItem.getActions().isEmpty()) {
            getWriter().append("<actions>");
            visitActions(modelMenuItem.getActions());
            getWriter().append("</actions>");
        }
        if (modelMenuItem.getLink() != null) {
            visitLink(modelMenuItem.getLink().getLink());
        }
        for (ModelMenuItem menuItem : modelMenuItem.getMenuItemList()) {
            menuItem.accept(this);
        }
        getWriter().append("</menu-item>");
    }

    @Override
    public void visit(ModelNode modelNode) throws Exception {
        getWriter().append("<node");
        visitModelWidget(modelNode);
        visitAttribute("expand-collapse-style", modelNode.getExpandCollapseStyle());
        visitAttribute("wrap-style", modelNode.getWrapStyleExdr());
        visitAttribute("render-style", modelNode.getRenderStyle());
        visitAttribute("entry-name", modelNode.getEntryName());
        visitAttribute("entity-name", modelNode.getEntityName());
        visitAttribute("join-field-name", modelNode.getPkName());
        getWriter().append(">");
        if (modelNode.getCondition() != null) {
            modelNode.getCondition().getCondition().accept(conditionVisitor);
        }
        if (!modelNode.getActions().isEmpty()) {
            getWriter().append("<actions>");
            visitActions(modelNode.getActions());
            getWriter().append("</actions>");
        }
        if (!modelNode.getScreenNameExdr().isEmpty()) {
            getWriter().append("<include-screen");
            visitAttribute("name", modelNode.getScreenNameExdr());
            visitAttribute("location", modelNode.getScreenLocationExdr());
            visitAttribute("share-scope", modelNode.getShareScope());
            visitAttribute("name", modelNode.getScreenNameExdr());
            getWriter().append("/>");
        }
        if (modelNode.getLabel() != null) {
            ModelNode.Label label = modelNode.getLabel();
            getWriter().append("<label");
            visitAttribute("id", label.getIdExdr());
            visitAttribute("style", label.getStyleExdr());
            getWriter().append(">");
            getWriter().append(label.getTextExdr().getOriginal());
            getWriter().append("</label>");
        }
        if (modelNode.getLink() != null) {
            ModelNode.Link link = modelNode.getLink();
            getWriter().append("<link");
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
                getWriter().append(">");
                visitParameters(link.getParameterList());
                getWriter().append("</link>");
            } else {
                getWriter().append("/>");
            }
        }
        getWriter().append("</node>");
    }

    @Override
    public void visit(ModelScreen modelScreen) throws Exception {
        getWriter().append("<screen");
        visitModelWidget(modelScreen);
        visitAttribute("transaction-timeout", modelScreen.getTransactionTimeout());
        visitAttribute("use-transaction", modelScreen.getUseTransaction());
        visitAttribute("use-cache", modelScreen.getUseCache());
        getWriter().append(">");
        modelScreen.getSection().accept(this);
        getWriter().append("</screen>");
    }

    @Override
    public void visit(ModelSubNode modelSubNode) throws Exception {
        getWriter().append("<sub-node");
        visitModelWidget(modelSubNode);
        visitAttribute("node-name", modelSubNode.getNodeNameExdr());
        getWriter().append(">");
        if (!modelSubNode.getActions().isEmpty()) {
            getWriter().append("<actions>");
            visitActions(modelSubNode.getActions());
            getWriter().append("</actions>");
        }
        getWriter().append("</sub-node>");
    }

    @Override
    public void visit(ModelTree modelTree) throws Exception {
        getWriter().append("<tree");
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
        getWriter().append(">");
        visitSubWidgets(modelTree.getNodeMap().values());
        getWriter().append("</tree>");
    }

    @Override
    public void visit(PlatformSpecific platformSpecific) throws Exception {
        getWriter().append("<platform-specific>");
        for (Map.Entry<String, ModelScreenWidget> entry : platformSpecific.getSubWidgets().entrySet()) {
            getWriter().append("<").append(entry.getKey()).append(">");
            entry.getValue().accept(this);
            getWriter().append("</").append(entry.getKey()).append(">");
        }
        getWriter().append("</platform-specific>");
    }

    @Override
    public void visit(PortalPage portalPage) throws Exception {
        getWriter().append("<include-portal-page");
        visitModelWidget(portalPage);
        visitAttribute("id", portalPage.getIdExdr());
        visitAttribute("conf-mode", portalPage.getConfModeExdr());
        visitAttribute("use-private", portalPage.getUsePrivate());
        getWriter().append("/>");
    }

    @Override
    public void visit(ScreenImage image) throws Exception {
        visitImage(image.getImage());
    }

    @Override
    public void visit(Screenlet screenlet) throws Exception {
        getWriter().append("<screenlet");
        visitModelWidget(screenlet);
        visitAttribute("id", screenlet.getIdExdr());
        visitAttribute("collapsible", screenlet.getCollapsible());
        visitAttribute("initially-collapsed", screenlet.getInitiallyCollapsed());
        visitAttribute("save-collapsed", screenlet.getSaveCollapsed());
        visitAttribute("padded", screenlet.getPadded());
        visitAttribute("title", screenlet.getTitleExdr());
        getWriter().append(">");
        visitSubWidgets(screenlet.getSubWidgets());
        getWriter().append("</screenlet>");
    }

    @Override
    public void visit(ScreenLink link) throws Exception {
        visitLink(link.getLink());
    }

    @Override
    public void visit(Section section) throws Exception {
        getWriter().append("<section");
        visitModelWidget(section);
        getWriter().append(">");
        if (section.getCondition() != null) {
            getWriter().append("<condition>");
            section.getCondition().accept(conditionVisitor);
            getWriter().append("</condition>");
        }
        if (!section.getActions().isEmpty()) {
            getWriter().append("<actions>");
            visitActions(section.getActions());
            getWriter().append("</actions>");
        }
        if (!section.getSubWidgets().isEmpty()) {
            getWriter().append("<widgets>");
            visitSubWidgets(section.getSubWidgets());
            getWriter().append("</widgets>");
        }
        if (!section.getFailWidgets().isEmpty()) {
            getWriter().append("<fail-widgets>");
            visitSubWidgets(section.getFailWidgets());
            getWriter().append("</fail-widgets>");
        }
        getWriter().append("</section>");
    }

    @Override
    public void visit(Tree tree) throws Exception {
        getWriter().append("<include-tree");
        visitModelWidget(tree);
        visitAttribute("location", tree.getLocationExdr());
        visitAttribute("share-scope", tree.getShareScopeExdr());
        getWriter().append("/>");
    }

    private void visitActions(Collection<? extends ModelAction> actions) throws Exception {
        for (ModelAction action : actions) {
            action.accept(actionVisitor);
        }
    }

    /**
     * Visit model form.
     * @param modelForm the model form
     * @throws Exception the exception
     */
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
        getWriter().append(">");
        if (!modelForm.getActions().isEmpty()) {
            getWriter().append("<actions>");
            visitActions(modelForm.getActions());
            getWriter().append("</actions>");
        }
        if (!modelForm.getRowActions().isEmpty()) {
            getWriter().append("<row-actions>");
            visitActions(modelForm.getRowActions());
            getWriter().append("</row-actions>");
        }
        for (ModelForm.AltRowStyle rowStyle : modelForm.getAltRowStyles()) {
            getWriter().append("<alt-row-style");
            visitAttribute("use-when", rowStyle.getUseWhen());
            visitAttribute("style", rowStyle.getStyle());
            getWriter().append("/>");
        }
        for (ModelForm.AltTarget target : modelForm.getAltTargets()) {
            getWriter().append("<alt-target");
            visitAttribute("use-when", target.getUseWhen());
            visitAttribute("target", target.getTargetExdr());
            getWriter().append("/>");
        }
        for (ModelForm.AutoFieldsService service : modelForm.getAutoFieldsServices()) {
            getWriter().append("<auto-fields-service");
            visitAttribute("service-name", service.getServiceName());
            visitAttribute("map-name", service.getMapName());
            visitAttribute("default-field-type", service.getDefaultFieldType());
            visitAttribute("default-position", service.getDefaultPosition());
            getWriter().append("/>");
        }
        for (ModelForm.AutoFieldsEntity entity : modelForm.getAutoFieldsEntities()) {
            getWriter().append("<auto-fields-entity");
            visitAttribute("entity-name", entity.getEntityName());
            visitAttribute("map-name", entity.getMapName());
            visitAttribute("default-field-type", entity.getDefaultFieldType());
            visitAttribute("default-position", entity.getDefaultPosition());
            getWriter().append("/>");
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
            getWriter().append("<on-event-update-area");
            visitAttribute("event-type", updateArea.getEventType());
            visitAttribute("area-id", updateArea.getAreaId());
            visitAttribute("area-target", updateArea.getAreaTarget());
            getWriter().append(">");
            visitAutoEntityParameters(updateArea.getAutoEntityParameters());
            visitAutoServiceParameters(updateArea.getAutoServiceParameters());
            visitParameters(updateArea.getParameterList());
            getWriter().append("</on-event-update-area>");
        }
    }
}
