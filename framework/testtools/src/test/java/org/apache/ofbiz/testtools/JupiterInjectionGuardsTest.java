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
package org.apache.ofbiz.testtools;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.service.LocalDispatcher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Exercises the fail-fast guards added to JupiterTestExtension directly (Concerns 2 and 3 of the
 * community discussion), without needing the full ofbiz --test container: the ThreadLocal bridge
 * and field/parameter reflection are set up and torn down by hand here.
 */
class JupiterInjectionGuardsTest {

    private final JupiterTestExtension extension = new JupiterTestExtension();

    @AfterEach
    void clearThreadLocals() {
        JupiterTestExtension.CURRENT_DELEGATOR.remove();
        JupiterTestExtension.CURRENT_DISPATCHER.remove();
    }

    @Test
    void correctlyNamedFieldsGetInjected() throws Exception {
        Delegator delegator = mock(Delegator.class);
        LocalDispatcher dispatcher = mock(LocalDispatcher.class);
        JupiterTestExtension.CURRENT_DELEGATOR.set(delegator);
        JupiterTestExtension.CURRENT_DISPATCHER.set(dispatcher);

        CorrectlyNamedFields instance = new CorrectlyNamedFields();
        extension.postProcessTestInstance(instance, null);

        assertThat(instance.delegator, sameInstance(delegator));
        assertThat(instance.dispatcher, sameInstance(dispatcher));
    }

    @Test
    void classWithNoDelegatorOrDispatcherFieldsIsUntouched() {
        JupiterTestExtension.CURRENT_DELEGATOR.set(mock(Delegator.class));
        JupiterTestExtension.CURRENT_DISPATCHER.set(mock(LocalDispatcher.class));

        assertDoesNotThrow(() -> extension.postProcessTestInstance(new NoRelevantFields(), null));
    }

    @Test
    void misnamedDelegatorFieldFailsFastInsteadOfSilentlySkipping() {
        JupiterTestExtension.CURRENT_DELEGATOR.set(mock(Delegator.class));

        IllegalStateException thrown = assertThrows(IllegalStateException.class, () ->
                extension.postProcessTestInstance(new MisnamedDelegatorField(), null));

        assertThat(thrown.getMessage(), containsString("myDelegator"));
        assertThat(thrown.getMessage(), containsString("'delegator'"));
    }

    @Test
    void namedFieldWithNoAvailableDelegatorFailsFastInsteadOfLeavingItNull() {
        // CURRENT_DELEGATOR intentionally left unset, simulating running outside the ofbiz --test
        // container or on a worker thread under (unsupported) parallel execution.
        IllegalStateException thrown = assertThrows(IllegalStateException.class, () ->
                extension.postProcessTestInstance(new CorrectlyNamedFields(), null));

        assertThat(thrown.getMessage(), containsString("delegator"));
    }

    @Test
    void parameterResolutionFailsFastWhenThreadLocalIsUnset() throws Exception {
        Method dummy = ParameterFixtures.class.getDeclaredMethod("dummy", Delegator.class);
        Parameter delegatorParameter = dummy.getParameters()[0];
        ParameterContext parameterContext = mock(ParameterContext.class);
        when(parameterContext.getParameter()).thenReturn(delegatorParameter);

        ParameterResolutionException thrown = assertThrows(ParameterResolutionException.class, () ->
                extension.resolveParameter(parameterContext, null));

        assertThat(thrown, instanceOf(ParameterResolutionException.class));
    }

    @Test
    void parameterResolutionSucceedsWhenThreadLocalIsSet() throws Exception {
        Delegator delegator = mock(Delegator.class);
        JupiterTestExtension.CURRENT_DELEGATOR.set(delegator);
        Method dummy = ParameterFixtures.class.getDeclaredMethod("dummy", Delegator.class);
        Parameter delegatorParameter = dummy.getParameters()[0];
        ParameterContext parameterContext = mock(ParameterContext.class);
        when(parameterContext.getParameter()).thenReturn(delegatorParameter);

        Object resolved = extension.resolveParameter(parameterContext, null);

        assertThat(resolved, sameInstance(delegator));
    }

    //ALLOW PUBLIC FIELDS
    static class CorrectlyNamedFields {
        Delegator delegator;
        LocalDispatcher dispatcher;
    }

    static class MisnamedDelegatorField {
        Delegator myDelegator;
    }

    static class NoRelevantFields {
        String name;
    }

    //FORBID PUBLIC FIELDS

    static class ParameterFixtures {
        void dummy(Delegator delegator) {
        }
    }
}
