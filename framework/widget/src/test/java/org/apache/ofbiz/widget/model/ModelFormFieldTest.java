/*
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
 */
package org.apache.ofbiz.widget.model;

import static org.apache.ofbiz.widget.model.ModelFormField.from;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableMap;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.w3c.dom.Element;

public final class ModelFormFieldTest {
    private HashMap<String, Object> context;

    @BeforeEach
    public void setUp() {
        context = new HashMap<>();
    }

    /**
     * Filter a list of fields with {@link ModelFormField#usedFields} predicate.
     * <p>
     * This is useful since Hamcrest does not provide any Stream matchers.
     * @param fields  the fields to filter
     * @return a list a filtered fields.
     */
    List<ModelFormField> getUsedField(ModelFormField... fields) {
        return Arrays.stream(fields)
                .filter(ModelFormField.usedFields(context))
                .collect(Collectors.toList());
    }

    @Test
    public void fieldsToRenderBasic() {
        ModelFormField fA = from(b -> b.setName("A"));
        ModelFormField fB = from(b -> b.setName("B"));
        assertThat(getUsedField(fA, fB), containsInAnyOrder(fA, fB));
    }

    @Test
    public void fieldsToRenderDuplicates() {
        ModelFormField fA0 = from(b -> b.setName("A"));
        ModelFormField fB = from(b -> b.setName("B"));
        ModelFormField fA1 = from(b -> b.setName("A"));
        assertThat(getUsedField(fA0, fB, fA1), containsInAnyOrder(fA0, fA1, fB));
    }

    @Test
    public void fieldsToRenderBasicUseWhen() {
        ModelFormField fA0 = from(b -> b.setName("A").setUseWhen("true"));
        ModelFormField fA1 = from(b -> b.setName("A").setUseWhen("false"));
        assertThat(getUsedField(fA0, fA1), containsInAnyOrder(fA0, fA1));
    }

    @Test
    public void fieldsToRenderDuplicatesUseWhen() {
        ModelFormField fA0 = from(b -> b.setName("A").setUseWhen("true"));
        ModelFormField fA1 = from(b -> b.setName("A").setUseWhen("false"));
        ModelFormField fA2 = from(b -> b.setName("A").setUseWhen("true"));
        assertThat(getUsedField(fA0, fA1, fA2), containsInAnyOrder(fA0, fA1));
    }

    @Test
    public void fieldUsesFlexibleParameterName() {
        ModelFormField field = from(b -> b.setParameterName("${prefix}Param"));
        assertThat(field.getParameterName(ImmutableMap.of("prefix", "P1")), equalTo("P1Param"));
        assertThat(field.getParameterName(ImmutableMap.of("prefix", "P2")), equalTo("P2Param"));
    }

    @Test
    public void dropDownFieldUsesFlexibleParameterNameOther() {
        ModelFormField field = from(b -> b.setParameterName("${prefix}Param"));
        ModelFormField.DropDownField dropDownField = new ModelFormField.DropDownField(field);
        assertThat(dropDownField.getParameterNameOther(ImmutableMap.of("prefix", "P1")), equalTo("P1Param_OTHER"));
        assertThat(dropDownField.getParameterNameOther(ImmutableMap.of("prefix", "P2")), equalTo("P2Param_OTHER"));
    }

    @Test
    public void fieldUsesFlexibleContainerId() {
        ModelFormField field = from(b -> b.setIdName("${prefix}IdValue"));
        assertThat(field.getCurrentContainerId(ImmutableMap.of("prefix", "P1")), equalTo("P1IdValue"));
        assertThat(field.getCurrentContainerId(ImmutableMap.of("prefix", "P2")), equalTo("P2IdValue"));
    }

    /**
     * Ensures behaviour of deprecated method LookupField#getTargetParameterList is maintained while the underlying
     * property type is changed from String to FlexibleStringExpander.
     */
    @Test
    @SuppressWarnings("deprecation")
    public void lookupFieldDeprecatedMethodTreatsTargetParameterAsString() {
        Element element = Mockito.mock(Element.class);
        when(element.getTagName()).thenReturn("lookup");
        when(element.getAttribute("maxlength")).thenReturn("1");
        when(element.getAttribute("size")).thenReturn("1");
        when(element.getAttribute("target-parameter")).thenReturn("${prefix}TargetParam, ${key1}");

        ModelFormField field = from(b -> b.setName("lookup-field"));
        ModelFormField.LookupField lookupField = new ModelFormField.LookupField(element, field);

        assertThat(lookupField.getTargetParameterList(), Matchers.contains("${prefix}TargetParam", "${key1}"));
    }

    @Test
    public void lookupFieldUsesFlexibleTargetParameters() {
        Element element = Mockito.mock(Element.class);
        when(element.getTagName()).thenReturn("lookup");
        when(element.getAttribute("maxlength")).thenReturn("1");
        when(element.getAttribute("size")).thenReturn("1");
        when(element.getAttribute("target-parameter")).thenReturn("${prefix}TargetParam");

        ModelFormField field = from(b -> b.setName("lookup-field"));
        ModelFormField.LookupField lookupField = new ModelFormField.LookupField(element, field);

        assertThat(lookupField.getTargetParameterList(ImmutableMap.of("prefix", "P1")),
                Matchers.contains("P1TargetParam"));
    }

    @Test
    public void lookupFieldEvaluatesExpressionBeforeSplitting() {
        Element element = Mockito.mock(Element.class);
        when(element.getTagName()).thenReturn("lookup");
        when(element.getAttribute("maxlength")).thenReturn("1");
        when(element.getAttribute("size")).thenReturn("1");
        when(element.getAttribute("target-parameter")).thenReturn("${prefix}TargetParam, ${key1}");

        ModelFormField field = from(b -> b.setName("lookup-field"));
        ModelFormField.LookupField lookupField = new ModelFormField.LookupField(element, field);

        final List<String> targetParameterList = lookupField.getTargetParameterList(
                ImmutableMap.of("prefix", "P1", "key1", "AA,BB , CC"));
        assertThat(targetParameterList, Matchers.contains("P1TargetParam", "AA", "BB", "CC"));
    }

    /**
     * Regression test for the CMS caContentIdTo/caMapKey use-when handling (applications/content/widget/cms/CMSForms.xml).
     * <p>{@code shouldUse} expands the {@code use-when} attribute with {@link org.apache.ofbiz.base.util.string.FlexibleStringExpander}
     * before handing it to Groovy: a {@code use-when="&quot;${var}&quot;.length()>0"} template textually splices the
     * <em>stringified value</em> of a context variable into the source string that is then compiled and executed. If that
     * context variable is populated from an HTTP request parameter, a value containing a closing quote can escape the
     * string literal and reach the surrounding Groovy source, which -- as demonstrated separately via reflection
     * (Class.forName("java.lang.Runtime") + Method.invoke) -- is not stopped by GroovyUtil's compile-time
     * SecureASTCustomizer sandbox. The fix (applied after this test is written) is to stop templating the value into a
     * string literal and instead reference the context variable directly, the same safe idiom already used by the vast
     * majority of use-when expressions elsewhere in the codebase (e.g. "currentValue==null").
     */
    @Test
    public void useWhenDoesNotExecuteContextValueAsGroovyCode() throws Exception {
        // A synchronous, in-JVM side effect (no OS process spawn) so the test is deterministic: no race waiting
        // on an async subprocess to finish writing a file. Running arbitrary code is proof enough here; a
        // separate, manually-verified reproduction additionally shows the same string literal escape reaching
        // Runtime.exec() via reflection (Class.forName("java.lang.Runtime") + Method.invoke), bypassing
        // GroovyUtil's compile-time SecureASTCustomizer sandbox -- that sandbox is irrelevant to *this* test,
        // since the sandbox was never the defense that mattered: nothing should be textually splicing untrusted
        // data into compiled source.
        String marker = "ofbiz.usewhen.test.marker";
        System.clearProperty(marker);
        try {
            // Matches the fixed use-when now in CMSForms.xml: a direct, unquoted binding reference instead of a
            // FlexibleStringExpander "${var}" template spliced into a string literal.
            ModelFormField field = from(b -> b.setName("caContentIdTo").setUseWhen("caContentIdTo!=null&&caContentIdTo.length()>0"));

            HashMap<String, Object> legitimate = new HashMap<>();
            legitimate.put("caContentIdTo", "LEGITIMATE_ID");
            assertTrue(field.shouldUse(legitimate), "A non-empty legitimate value must still satisfy the use-when");

            HashMap<String, Object> empty = new HashMap<>();
            assertFalse(field.shouldUse(empty), "A missing value must not satisfy the use-when");

            HashMap<String, Object> untrusted = new HashMap<>();
            untrusted.put("caContentIdTo", "x\";System.setProperty(\"" + marker + "\",\"SIDE_EFFECT\");\"x");
            // The untrusted text is still non-empty, so the field is legitimately shown -- that part
            // is expected and harmless. What must NOT happen is the embedded statement executing as code.
            assertTrue(field.shouldUse(untrusted), "The untrusted value is still non-empty text, so use-when legitimately evaluates true");
            assertFalse(System.getProperty(marker) != null, "The embedded Groovy statement must not execute as code");
        } finally {
            System.clearProperty(marker);
        }
    }

    /**
     * Same handling, same fix, for the sibling caMapKey use-when in the same CMSForms.xml form.
     * See {@link #useWhenDoesNotExecuteContextValueAsGroovyCode()} for the full explanation.
     */
    @Test
    public void useWhenDoesNotExecuteContextValueAsGroovyCodeForCaMapKey() throws Exception {
        String marker = "ofbiz.usewhen.test.marker.camapkey";
        System.clearProperty(marker);
        try {
            ModelFormField field = from(b -> b.setName("caMapKey").setUseWhen("caMapKey!=null&&caMapKey.length()>0"));

            HashMap<String, Object> legitimate = new HashMap<>();
            legitimate.put("caMapKey", "LEGITIMATE_KEY");
            assertTrue(field.shouldUse(legitimate), "A non-empty legitimate value must still satisfy the use-when");

            HashMap<String, Object> empty = new HashMap<>();
            assertFalse(field.shouldUse(empty), "A missing value must not satisfy the use-when");

            HashMap<String, Object> untrusted = new HashMap<>();
            untrusted.put("caMapKey", "x\";System.setProperty(\"" + marker + "\",\"SIDE_EFFECT\");\"x");
            assertTrue(field.shouldUse(untrusted), "The untrusted value is still non-empty text, so use-when legitimately evaluates true");
            assertFalse(System.getProperty(marker) != null, "The embedded Groovy statement must not execute as code");
        } finally {
            System.clearProperty(marker);
        }
    }
}
