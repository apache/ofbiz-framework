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

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ofbiz.base.util.FileUtil;
import org.apache.ofbiz.base.util.UtilValidate;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

public final class HtmlImportWidgetVisitor implements ModelWidgetVisitor {

    private Set<String> jsImports = new LinkedHashSet<String>();

    /**
     * Get the script source locations collected by HtmlImportWidgetVisitor
     * @return
     */
    public Set<String> getJsImports() {
        return jsImports;
    }

    @Override
    public void visit(HtmlWidget htmlWidget) throws Exception {
        List<ModelScreenWidget> widgetList = htmlWidget.getSubWidgets();
        for (ModelScreenWidget widget: widgetList) {
            // HtmlTemplate
            widget.accept(this);
        }
    }

    @Override
    public void visit(HtmlWidget.HtmlTemplate htmlTemplate) throws Exception {
        String fileLocation = htmlTemplate.locationExdr.getOriginal();
        boolean isStaticLocation = !fileLocation.contains("${");
        if (isStaticLocation && htmlTemplate.isMultiBlock()) {
            String template = FileUtil.readString("UTF-8", FileUtil.getFile(fileLocation));
            Document doc = Jsoup.parseBodyFragment(template);
            Elements scriptElements = doc.select("script");
            if (scriptElements != null && scriptElements.size() > 0) {
                for (org.jsoup.nodes.Element script : scriptElements) {
                    String src = script.attr("src");
                    if (UtilValidate.isNotEmpty(src)) {
                        String dataImport = script.attr("data-import");
                        if ("head".equals(dataImport)) {
                            jsImports.add(src);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void visit(HtmlWidget.HtmlTemplateDecorator htmlTemplateDecorator) throws Exception {

    }

    @Override
    public void visit(HtmlWidget.HtmlTemplateDecoratorSection htmlTemplateDecoratorSection) throws Exception {

    }

    @Override
    public void visit(IterateSectionWidget iterateSectionWidget) throws Exception {

    }

    @Override
    public void visit(ModelSingleForm modelForm) throws Exception {

    }

    @Override
    public void visit(ModelGrid modelGrid) throws Exception {

    }

    @Override
    public void visit(ModelMenu modelMenu) throws Exception {

    }

    @Override
    public void visit(ModelMenuItem modelMenuItem) throws Exception {

    }

    @Override
    public void visit(ModelScreen modelScreen) throws Exception {

    }

    @Override
    public void visit(ModelScreenWidget.ColumnContainer columnContainer) throws Exception {

    }

    @Override
    public void visit(ModelScreenWidget.Container container) throws Exception {

    }

    @Override
    public void visit(ModelScreenWidget.Content content) throws Exception {

    }

    @Override
    public void visit(ModelScreenWidget.DecoratorScreen decoratorScreen) throws Exception {

    }

    @Override
    public void visit(ModelScreenWidget.DecoratorSection decoratorSection) throws Exception {

    }

    @Override
    public void visit(ModelScreenWidget.DecoratorSectionInclude decoratorSectionInclude) throws Exception {

    }

    @Override
    public void visit(ModelScreenWidget.Form form) throws Exception {

    }

    @Override
    public void visit(ModelScreenWidget.Grid grid) throws Exception {

    }

    @Override
    public void visit(ModelScreenWidget.HorizontalSeparator horizontalSeparator) throws Exception {

    }

    @Override
    public void visit(ModelScreenWidget.ScreenImage image) throws Exception {

    }

    @Override
    public void visit(ModelScreenWidget.IncludeScreen includeScreen) throws Exception {

    }

    @Override
    public void visit(ModelScreenWidget.Label label) throws Exception {

    }

    @Override
    public void visit(ModelScreenWidget.ScreenLink link) throws Exception {

    }

    @Override
    public void visit(ModelScreenWidget.Menu menu) throws Exception {

    }

    @Override
    public void visit(ModelScreenWidget.PlatformSpecific platformSpecific) throws Exception {
        Map<String, ModelScreenWidget> widgetMap = platformSpecific.getSubWidgets();
        for (Map.Entry<String, ModelScreenWidget> entry : widgetMap.entrySet()) {
            if (entry.getKey().equals("html")) {
                // HtmlWidget
                entry.getValue().accept(this);
            }
        }

    }

    @Override
    public void visit(ModelScreenWidget.PortalPage portalPage) throws Exception {

    }

    @Override
    public void visit(ModelScreenWidget.Screenlet screenlet) throws Exception {

    }

    @Override
    public void visit(ModelScreenWidget.Section section) throws Exception {

    }

    @Override
    public void visit(ModelScreenWidget.Tree tree) throws Exception {

    }

    @Override
    public void visit(ModelTree modelTree) throws Exception {

    }

    @Override
    public void visit(ModelTree.ModelNode modelNode) throws Exception {

    }

    @Override
    public void visit(ModelTree.ModelNode.ModelSubNode modelSubNode) throws Exception {

    }

    @Override
    public void visit(ModelScreenWidget.Column column) throws Exception {

    }
}
