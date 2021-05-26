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
package org.apache.ofbiz.widget.renderer.macro;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Mocked;
import mockit.Tested;
import org.apache.ofbiz.webapp.control.ConfigXMLReader;
import org.apache.ofbiz.webapp.control.RequestHandler;
import org.apache.ofbiz.widget.model.ModelForm;
import org.apache.ofbiz.widget.model.ModelFormField;
import org.apache.ofbiz.widget.model.ModelScreenWidget;
import org.apache.ofbiz.widget.model.ModelTheme;
import org.apache.ofbiz.widget.renderer.VisualTheme;
import org.apache.ofbiz.widget.renderer.macro.renderable.RenderableFtlMacroCall;
import org.apache.ofbiz.widget.renderer.macro.renderable.RenderableFtl;
import org.apache.ofbiz.widget.renderer.macro.renderable.RenderableFtlNoop;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class RenderableFtlFormElementsBuilderTest {

    @Injectable
    private VisualTheme visualTheme;

    @Injectable
    private RequestHandler requestHandler;

    @Injectable
    private HttpServletRequest request;

    @Injectable
    private HttpServletResponse response;

    @Mocked
    private HttpSession httpSession;

    @Mocked
    private ModelTheme modelTheme;

    @Mocked
    private ModelFormField.ContainerField containerField;

    @Mocked
    private ModelFormField modelFormField;

    @Tested
    private RenderableFtlFormElementsBuilder renderableFtlFormElementsBuilder;

    @Test
    public void emptyLabelUsesNoopMacro(@Mocked ModelScreenWidget.Label label) {
        new Expectations() {
            {
                label.getText(withNotNull());
                result = "";
            }
        };

        final RenderableFtl renderableFtl = renderableFtlFormElementsBuilder.label(ImmutableMap.of(), label);
        assertThat(renderableFtl, equalTo(RenderableFtlNoop.INSTANCE));
    }

    @Test
    public void labelMacroCallUsesText(@Mocked final ModelScreenWidget.Label label) {
        new Expectations() {
            {
                label.getText(withNotNull());
                result = "TEXT";
            }
        };

        final RenderableFtl renderableFtl = renderableFtlFormElementsBuilder.label(ImmutableMap.of(), label);
        assertThat(renderableFtl,
                MacroCallMatcher.hasNameAndParameters("renderLabel",
                        MacroCallParameterMatcher.hasNameAndStringValue("text", "TEXT")));
    }

    @Test
    public void displayFieldMacroUsesType(@Mocked final ModelFormField.DisplayField displayField) {
        new Expectations() {
            {
                displayField.getType();
                result = "TYPE";

                displayField.getDescription(withNotNull());
                result = "DESCRIPTION";
            }
        };

        final RenderableFtl renderableFtl = renderableFtlFormElementsBuilder.displayField(ImmutableMap.of(),
                displayField, false);
        assertThat(renderableFtl,
                MacroCallMatcher.hasNameAndParameters("renderDisplayField",
                        MacroCallParameterMatcher.hasNameAndStringValue("type", "TYPE")));
    }

    @Test
    public void containerMacroCallUsesContainerId() {
        new Expectations() {
            {
                modelFormField.getCurrentContainerId(withNotNull());
                result = "CurrentContainerId";
            }
        };

        final RenderableFtlMacroCall macroCall = renderableFtlFormElementsBuilder.containerMacroCall(ImmutableMap.of(), containerField);
        assertThat(macroCall,
                MacroCallMatcher.hasNameAndParameters("renderContainerField",
                        MacroCallParameterMatcher.hasNameAndStringValue("id", "CurrentContainerId")));
    }

    @Test
    public void basicAnchorLinkCreatesMacroCall(@Mocked final ModelFormField.SubHyperlink subHyperlink) {

        final Map<String, ConfigXMLReader.RequestMap> requestMapMap = new HashMap<>();

        new Expectations() {
            {
                subHyperlink.getStyle(withNotNull());
                result = "TestLinkStyle";

                subHyperlink.getUrlMode();
                result = "url-mode";

                subHyperlink.shouldUse(withNotNull());
                result = true;

                subHyperlink.getDescription(withNotNull());
                result = "LinkDescription";

                subHyperlink.getTarget(withNotNull());
                result = "/link/target/path";

                request.getAttribute("requestMapMap");
                result = requestMapMap;
            }
        };

        final RenderableFtl linkElement =
                renderableFtlFormElementsBuilder.makeHyperlinkString(subHyperlink, new HashMap<>());
        assertThat(linkElement,
                MacroCallMatcher.hasNameAndParameters("makeHyperlinkString",
                        MacroCallParameterMatcher.hasNameAndStringValue("linkStyle", "TestLinkStyle"),
                        MacroCallParameterMatcher.hasNameAndStringValue("linkUrl", "/link/target/path")));
    }

    @Test
    public void textFieldSetsIdValueAndLength(@Mocked final ModelFormField.TextField textField) {
        final int maxLength = 42;
        new Expectations() {
            {
                modelFormField.getCurrentContainerId(withNotNull());
                result = "CurrentTextId";

                modelFormField.getEntry(withNotNull(), anyString);
                result = "TEXTVALUE";

                textField.getMaxlength();
                result = maxLength;

                httpSession.getAttribute("delegatorName");
                result = "DelegatorName";
            }
        };

        final HashMap<String, Object> context = new HashMap<>();
        context.put("session", httpSession);

        final RenderableFtl renderableFtl = renderableFtlFormElementsBuilder.textField(context, textField, true);
        assertThat(renderableFtl, MacroCallMatcher.hasNameAndParameters("renderTextField",
                MacroCallParameterMatcher.hasNameAndStringValue("id", "CurrentTextId"),
                MacroCallParameterMatcher.hasNameAndStringValue("value", "TEXTVALUE"),
                MacroCallParameterMatcher.hasNameAndStringValue("maxlength", Integer.toString(maxLength))));

    }

    @Test
    public void textFieldCreatesAjaxUrl(@Mocked final ModelFormField.TextField textField) {

        final List<ModelForm.UpdateArea> updateAreas = ImmutableList.of(
                new ModelForm.UpdateArea("change", "areaId1", "target1?param1=${param1}&param2=ThisIsParam2"),
                new ModelForm.UpdateArea("change", "areaId2", "target2"));
        new Expectations() {
            {
                modelFormField.getOnChangeUpdateAreas();
                result = updateAreas;

                requestHandler.makeLink(request, response, "target1");
                result = "http://host.domain/target1";

                requestHandler.makeLink(request, response, "target2");
                result = "http://host.domain/target2";

                httpSession.getAttribute("delegatorName");
                result = "DelegatorName";
            }
        };

        final HashMap<String, Object> context = new HashMap<>();
        context.put("param1", "ThisIsParam1");
        context.put("session", httpSession);

        final RenderableFtl renderableFtl = renderableFtlFormElementsBuilder.textField(context, textField, true);
        assertThat(renderableFtl, MacroCallMatcher.hasName("renderTextField"));
        assertThat(renderableFtl, MacroCallMatcher.hasParameters(
                MacroCallParameterMatcher.hasNameAndStringValue("ajaxUrl",
                        "areaId1,http://host.domain/target1,param1=ThisIsParam1&param2=ThisIsParam2,"
                                + "areaId2,http://host.domain/target2,")));
    }

    @Test
    public void fieldGroupOpenRendersCollapsibleAreaId(@Mocked final ModelForm.FieldGroup fieldGroup) {
        new Expectations() {
            {
                fieldGroup.getStyle();
                result = "GROUPSTYLE";

                fieldGroup.getTitle();
                result = "TITLE${title}";

                fieldGroup.getId();
                result = "FIELDGROUPID";

                fieldGroup.initiallyCollapsed();
                result = true;
            }
        };

        final Map<String, Object> context = new HashMap<>();
        context.put("title", "ABC");

        final RenderableFtl renderableFtl = renderableFtlFormElementsBuilder.fieldGroupOpen(context, fieldGroup);
        assertThat(renderableFtl, MacroCallMatcher.hasName("renderFieldGroupOpen"));
        assertThat(renderableFtl, MacroCallMatcher.hasParameters(MacroCallParameterMatcher.hasNameAndStringValue("title", "TITLEABC")));
        assertThat(renderableFtl, MacroCallMatcher.hasParameters(MacroCallParameterMatcher.hasNameAndStringValue("style", "GROUPSTYLE")));
        assertThat(renderableFtl, MacroCallMatcher.hasParameters(MacroCallParameterMatcher.hasNameAndStringValue(
                "collapsibleAreaId", "FIELDGROUPID_body")));
        assertThat(renderableFtl, MacroCallMatcher.hasParameters(MacroCallParameterMatcher.hasNameAndBooleanValue("collapsed", true)));
    }

    @Test
    public void fieldGroupCloseRendersStyle(@Mocked final ModelForm.FieldGroup fieldGroup) {
        new Expectations() {
            {
                fieldGroup.getStyle();
                result = "GROUPSTYLE";
            }
        };

        final Map<String, Object> context = new HashMap<>();
        context.put("title", "ABC");

        final RenderableFtl renderableFtl = renderableFtlFormElementsBuilder.fieldGroupClose(context, fieldGroup);
        assertThat(renderableFtl, MacroCallMatcher.hasName("renderFieldGroupClose"));
        assertThat(renderableFtl, MacroCallMatcher.hasParameters(MacroCallParameterMatcher.hasNameAndStringValue("style", "GROUPSTYLE")));
    }
}
