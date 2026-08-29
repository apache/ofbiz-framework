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
package org.apache.ofbiz.entity.model;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for the Tier-2 complex-alias conversion binding:
 * a single-member, no-function, no-default ComplexAliasField is the only
 * case where a ModelConversion entry is safe to register. All other shapes
 * (arithmetic, functions, defaultValue, nested alias) must stay as wildcards.
 */
public final class ModelViewEntityComplexAliasTests {

    @Mock
    private ModelViewEntity mockViewEntity;

    @Mock
    private ModelReader mockModelReader;

    @Mock
    private ModelEntity mockModelEntity;

    private AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void tearDown() throws Exception {
        mocks.close();
    }

    // ── ComplexAliasField.isPassThrough() ──────────────────────────────────

    @Test
    void isPassThroughPlainFieldReferenceReturnsTrue() {
        ModelViewEntity.ComplexAliasField field =
                new ModelViewEntity.ComplexAliasField("ME", "myField", "", "");
        assertTrue(field.isPassThrough());
    }

    @Test
    void isPassThroughWithFunctionReturnsFalse() {
        ModelViewEntity.ComplexAliasField field =
                new ModelViewEntity.ComplexAliasField("ME", "myField", "", "upper");
        assertFalse(field.isPassThrough());
    }

    @Test
    void isPassThroughWithDefaultValueReturnsFalse() {
        ModelViewEntity.ComplexAliasField field =
                new ModelViewEntity.ComplexAliasField("ME", "myField", "0", "");
        assertFalse(field.isPassThrough());
    }

    @Test
    void isPassThroughEmptyEntityAliasReturnsFalse() {
        ModelViewEntity.ComplexAliasField field =
                new ModelViewEntity.ComplexAliasField("", "myField", "", "");
        assertFalse(field.isPassThrough());
    }

    @Test
    void isPassThroughEmptyFieldReturnsFalse() {
        ModelViewEntity.ComplexAliasField field =
                new ModelViewEntity.ComplexAliasField("ME", "", "", "");
        assertFalse(field.isPassThrough());
    }

    @Test
    void isPassThroughLiteralValueConstantReturnsFalse() {
        // entityAlias and field are both empty — this is a SQL literal constant, not a column ref
        ModelViewEntity.ComplexAliasField field =
                new ModelViewEntity.ComplexAliasField("", "", "", "", "LITERAL_VALUE");
        assertFalse(field.isPassThrough());
    }

    // ── ComplexAlias.bindAliasToConversions() — positive ───────────────────

    @Test
    void bindAliasToConversionsSinglePassThroughFieldRegistersConversion() {
        // ModelConversion is a final inner class and cannot be mocked without the inline mock maker.
        // We verify the dispatch reached getOrCreateModelConversion with the correct entityAlias,
        // which is the key decision point. addConversion() is a trivial HashMap put tested separately.
        doThrow(new UnsupportedOperationException("reached conversion registration"))
                .when(mockViewEntity).getOrCreateModelConversion(anyString());

        ModelViewEntity.ComplexAlias alias = new ModelViewEntity.ComplexAlias("+");
        alias.addComplexAliasMember(new ModelViewEntity.ComplexAliasField("ME", "myField", "", ""));

        assertThrows(UnsupportedOperationException.class, () ->
                alias.bindAliasToConversions("myAlias", mockViewEntity));

        verify(mockViewEntity).getOrCreateModelConversion("ME");
    }

    // ── ComplexAlias.bindAliasToConversions() — negative (stays wildcard) ──

    @Test
    void bindAliasToConversionsSingleFieldWithFunctionDoesNotRegister() {
        ModelViewEntity.ComplexAlias alias = new ModelViewEntity.ComplexAlias("+");
        alias.addComplexAliasMember(new ModelViewEntity.ComplexAliasField("ME", "myField", "", "upper"));

        alias.bindAliasToConversions("myAlias", mockViewEntity);

        verify(mockViewEntity, never()).getOrCreateModelConversion(anyString());
    }

    @Test
    void bindAliasToConversionsSingleFieldWithDefaultValueDoesNotRegister() {
        ModelViewEntity.ComplexAlias alias = new ModelViewEntity.ComplexAlias("+");
        alias.addComplexAliasMember(new ModelViewEntity.ComplexAliasField("ME", "myField", "0", ""));

        alias.bindAliasToConversions("myAlias", mockViewEntity);

        verify(mockViewEntity, never()).getOrCreateModelConversion(anyString());
    }

    @Test
    void bindAliasToConversionsMultipleMembersDoesNotRegister() {
        // Arithmetic across two fields — computed value != either raw field value
        ModelViewEntity.ComplexAlias alias = new ModelViewEntity.ComplexAlias("+");
        alias.addComplexAliasMember(new ModelViewEntity.ComplexAliasField("ME", "unitPrice", "", ""));
        alias.addComplexAliasMember(new ModelViewEntity.ComplexAliasField("ME", "quantity", "", ""));

        alias.bindAliasToConversions("totalAmount", mockViewEntity);

        verify(mockViewEntity, never()).getOrCreateModelConversion(anyString());
    }

    @Test
    void bindAliasToConversionsNestedComplexAliasAsSoleMemberDoesNotRegister() {
        // Sole member is another ComplexAlias, not a ComplexAliasField
        ModelViewEntity.ComplexAlias inner = new ModelViewEntity.ComplexAlias("+");
        inner.addComplexAliasMember(new ModelViewEntity.ComplexAliasField("ME", "myField", "", ""));

        ModelViewEntity.ComplexAlias outer = new ModelViewEntity.ComplexAlias("+");
        outer.addComplexAliasMember(inner);

        outer.bindAliasToConversions("myAlias", mockViewEntity);

        verify(mockViewEntity, never()).getOrCreateModelConversion(anyString());
    }

    // ── ComplexAliasField.makeAliasColName() — default-value quoting ────────

    @Test
    void makeAliasColNameDateTimeDefaultValueIsWrappedInSingleQuotes() {
        ModelField dateTimeField = ModelField.create(null, "estimatedDeliveryDate", "date-time", false);
        when(mockViewEntity.getAliasedEntity("OI", mockModelReader)).thenReturn(mockModelEntity);
        when(mockViewEntity.getAliasedField(mockModelEntity, "estimatedDeliveryDate", mockModelReader)).thenReturn(dateTimeField);

        ModelViewEntity.ComplexAliasField caf =
                new ModelViewEntity.ComplexAliasField("OI", "estimatedDeliveryDate", "2026-06-30 12:34:56.789", "min");

        StringBuilder colNameBuffer = new StringBuilder();
        StringBuilder fieldTypeBuffer = new StringBuilder();
        caf.makeAliasColName(colNameBuffer, fieldTypeBuffer, mockViewEntity, mockModelReader);

        assertEquals("MIN(COALESCE(OI.ESTIMATED_DELIVERY_DATE,'2026-06-30 12:34:56.789'))", colNameBuffer.toString());
        assertEquals("date-time", fieldTypeBuffer.toString());
    }

    @Test
    void makeAliasColNameNumericDefaultValueIsNotQuoted() {
        ModelField numericField = ModelField.create(null, "quantity", "numeric", false);
        when(mockViewEntity.getAliasedEntity("OI", mockModelReader)).thenReturn(mockModelEntity);
        when(mockViewEntity.getAliasedField(mockModelEntity, "quantity", mockModelReader)).thenReturn(numericField);

        ModelViewEntity.ComplexAliasField caf =
                new ModelViewEntity.ComplexAliasField("OI", "quantity", "0", null);

        StringBuilder colNameBuffer = new StringBuilder();
        StringBuilder fieldTypeBuffer = new StringBuilder();
        caf.makeAliasColName(colNameBuffer, fieldTypeBuffer, mockViewEntity, mockModelReader);

        assertEquals("COALESCE(OI.QUANTITY,0)", colNameBuffer.toString());
    }

    @Test
    void makeAliasColNameAlreadyQuotedDefaultValueIsNotDoubleQuoted() {
        ModelField dateTimeField = ModelField.create(null, "estimatedDeliveryDate", "date-time", false);
        when(mockViewEntity.getAliasedEntity("OI", mockModelReader)).thenReturn(mockModelEntity);
        when(mockViewEntity.getAliasedField(mockModelEntity, "estimatedDeliveryDate", mockModelReader)).thenReturn(dateTimeField);

        ModelViewEntity.ComplexAliasField caf =
                new ModelViewEntity.ComplexAliasField("OI", "estimatedDeliveryDate", "'2026-06-30 12:34:56.789'", null);

        StringBuilder colNameBuffer = new StringBuilder();
        StringBuilder fieldTypeBuffer = new StringBuilder();
        caf.makeAliasColName(colNameBuffer, fieldTypeBuffer, mockViewEntity, mockModelReader);

        assertEquals("COALESCE(OI.ESTIMATED_DELIVERY_DATE,'2026-06-30 12:34:56.789')", colNameBuffer.toString());
    }

    @Test
    void makeAliasColNameNoDefaultValueProducesPlainColumnName() {
        ModelField dateTimeField = ModelField.create(null, "estimatedDeliveryDate", "date-time", false);
        when(mockViewEntity.getAliasedEntity("OI", mockModelReader)).thenReturn(mockModelEntity);
        when(mockViewEntity.getAliasedField(mockModelEntity, "estimatedDeliveryDate", mockModelReader)).thenReturn(dateTimeField);

        ModelViewEntity.ComplexAliasField caf =
                new ModelViewEntity.ComplexAliasField("OI", "estimatedDeliveryDate", null, "min");

        StringBuilder colNameBuffer = new StringBuilder();
        StringBuilder fieldTypeBuffer = new StringBuilder();
        caf.makeAliasColName(colNameBuffer, fieldTypeBuffer, mockViewEntity, mockModelReader);

        assertEquals("MIN(OI.ESTIMATED_DELIVERY_DATE)", colNameBuffer.toString());
    }

    @Test
    void makeAliasColNameSqlFunctionDefaultValueIsNotQuoted() {
        // Oracle requires TO_TIMESTAMP(...) — a function expression must pass through unmodified
        ModelField dateTimeField = ModelField.create(null, "estimatedDeliveryDate", "date-time", false);
        when(mockViewEntity.getAliasedEntity("OI", mockModelReader)).thenReturn(mockModelEntity);
        when(mockViewEntity.getAliasedField(mockModelEntity, "estimatedDeliveryDate", mockModelReader)).thenReturn(dateTimeField);

        ModelViewEntity.ComplexAliasField caf = new ModelViewEntity.ComplexAliasField(
                "OI", "estimatedDeliveryDate", "TO_TIMESTAMP('2026-06-30','YYYY-MM-DD')", null);

        StringBuilder colNameBuffer = new StringBuilder();
        StringBuilder fieldTypeBuffer = new StringBuilder();
        caf.makeAliasColName(colNameBuffer, fieldTypeBuffer, mockViewEntity, mockModelReader);

        assertEquals("COALESCE(OI.ESTIMATED_DELIVERY_DATE,TO_TIMESTAMP('2026-06-30','YYYY-MM-DD'))",
                colNameBuffer.toString());
    }
}
