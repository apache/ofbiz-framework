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

import java.util.List;
import java.util.Set;

import org.apache.ofbiz.base.util.UtilXml;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;

/**
 * Exercises ModelTestSuite.selectTestCaseElements() directly: the piece of the case-filtering logic
 * that decides which of a suite's test-case/test-group elements run for a given {@code case=} filter,
 * with no Delegator/LocalDispatcher or class-loading involved - unlike the constructor itself (see
 * TestRunContainerTest's class javadoc for why that needs a full ofbiz --test container bootstrap).
 *
 * <p>The fixture mirrors every shape actually found in this repo's testdef files (a full repo-wide
 * scan backs each case here):
 * <ul>
 * <li>{@code data-load} - an {@code entity-xml action="load"} case, the one shape that genuinely
 * behaves like setup: additive, side-effect-free relative to every other case, safe to run
 * unconditionally. This is the only shape auto-included as a prerequisite.</li>
 * <li>{@code assert-data} - an {@code entity-xml action="assert"} case. Despite also being
 * {@code entity-xml}, this is a check, not a load - and in the real suite it is modeled on
 * (framework/service/testdef/servicetests.xml's *-assert-data cases), it verifies the side effects of
 * the {@code service-test} case declared immediately before it. Auto-including it without that
 * service-test would assert against data that was never created, turning a clean single-case run into
 * a spurious failure - so it is treated the same as an independent test, not as setup.</li>
 * <li>{@code service-call} - a {@code service-test} case, modeled on servicetests.xml's
 * service-dead-lock-retry-test/service-own-tx-sub-service-after-set-rollback-only-in-parent/
 * service-eca-global-event-exec: always a real functional test in every testdef file in this repo,
 * never data loading.</li>
 * <li>{@code first-tests}/{@code second-tests} - independent {@code jupiter-test-suite} classes, the
 * entitytests.xml shape this whole rule exists for (see caseFilterSkipsAnIndependentJupiterTestClass
 * DeclaredBeforeIt()'s comment).</li>
 * <li>{@code grouped-tests} - a {@code test-group}. A repo-wide scan of every {@code test-group} in
 * this codebase found 14 of 15 child elements are {@code jupiter-test-suite} (accounting/product/scrum
 * testdef files bundle several independent classes into one group) and only 1 is {@code entity-xml} -
 * so a test-group reads as "more independent classes," not setup, and is excluded like one.</li>
 * <li>{@code third-tests} - a {@code junit-test-suite} case, the legacy-JUnit-3 equivalent of
 * first-tests/second-tests.</li>
 * </ul>
 */
class ModelTestSuiteTest {

    private static final String SUITE_XML =
            "<test-suite suite-name=\"sample\">"
                    + "<test-case case-name=\"data-load\"><entity-xml action=\"load\" entity-xml-url=\"x\"/></test-case>"
                    + "<test-case case-name=\"assert-data\"><entity-xml action=\"assert\" entity-xml-url=\"x\"/></test-case>"
                    + "<test-case case-name=\"service-call\"><service-test service-name=\"x\"/></test-case>"
                    + "<test-case case-name=\"first-tests\"><jupiter-test-suite class-name=\"a.First\"/></test-case>"
                    + "<test-case case-name=\"second-tests\"><jupiter-test-suite class-name=\"a.Second\"/></test-case>"
                    + "<test-group case-name=\"grouped-tests\">"
                    + "<jupiter-test-suite class-name=\"a.GroupA\"/>"
                    + "<jupiter-test-suite class-name=\"a.GroupB\"/>"
                    + "</test-group>"
                    + "<test-case case-name=\"third-tests\"><junit-test-suite class-name=\"a.Third\"/></test-case>"
                    + "</test-suite>";

    @Test
    void nullCaseFilterSelectsEveryElementUnchanged() throws Exception {
        List<Element> selected = ModelTestSuite.selectTestCaseElements(caseElements(), null);

        assertThat(caseNamesOf(selected), contains(
                "data-load", "assert-data", "service-call", "first-tests", "second-tests", "grouped-tests", "third-tests"));
    }

    @Test
    void caseFilterIncludesAGenuineLoadCaseDeclaredBeforeIt() throws Exception {
        List<Element> selected = ModelTestSuite.selectTestCaseElements(caseElements(), "assert-data");

        assertThat(caseNamesOf(selected), contains("data-load", "assert-data"));
    }

    @Test
    void caseFilterExcludesAnEntityXmlAssertCaseThatIsNotTheTarget() throws Exception {
        // assert-data would run against data only service-call creates - since service-call is
        // itself excluded (it's a real test, not setup), running assert-data too would be a spurious
        // failure, not a harmless extra. Modeled on servicetests.xml's service-eca-global-event-exec
        // -assert-data, which checks service-eca-global-event-exec's side effects.
        List<Element> selected = ModelTestSuite.selectTestCaseElements(caseElements(), "first-tests");

        assertThat(caseNamesOf(selected), contains("data-load", "first-tests"));
    }

    @Test
    void caseFilterSkipsAnIndependentJupiterTestClassDeclaredBeforeIt() throws Exception {
        // Regression test: entitytests.xml bundles five independent jupiter-test-suite classes with
        // no data-load relationship between them. Requesting entity-crypto-tests (declared third)
        // must not also run the unrelated entity-tests/entity-util-tests classes ahead of it - that
        // silently multiplied a single-case run into the whole suite's worth of work, including
        // EntityTestSuite's deliberately slow bulk-operation tests.
        List<Element> selected = ModelTestSuite.selectTestCaseElements(caseElements(), "second-tests");

        assertThat(caseNamesOf(selected), contains("data-load", "second-tests"));
    }

    @Test
    void caseFilterExcludesEveryNonLoadShapeAheadOfTheTarget() throws Exception {
        // third-tests is declared last: only the one genuine entity-xml action="load" case ahead of
        // it survives. assert-data, service-call, first-tests, second-tests, and grouped-tests are
        // all excluded - none of them is data loading, whatever shape they otherwise take.
        List<Element> selected = ModelTestSuite.selectTestCaseElements(caseElements(), "third-tests");

        assertThat(caseNamesOf(selected), contains("data-load", "third-tests"));
    }

    @Test
    void caseFilterOnATestGroupSelectsOnlyThatGroup() throws Exception {
        List<Element> selected = ModelTestSuite.selectTestCaseElements(caseElements(), "grouped-tests");

        assertThat(caseNamesOf(selected), contains("data-load", "grouped-tests"));
    }

    @Test
    void caseFilterOnTheFirstElementSelectsOnlyThatElement() throws Exception {
        List<Element> selected = ModelTestSuite.selectTestCaseElements(caseElements(), "data-load");

        assertThat(caseNamesOf(selected), contains("data-load"));
    }

    @Test
    void caseFilterNotPresentInThisDocumentSelectsNothing() throws Exception {
        List<Element> selected = ModelTestSuite.selectTestCaseElements(caseElements(), "not-here-at-all");

        assertThat(selected, is(empty()));
    }

    private static List<Element> caseElements() throws Exception {
        Document document = UtilXml.readXmlDocument(SUITE_XML, false);
        return List.copyOf(UtilXml.childElementList(document.getDocumentElement(), Set.of("test-case", "test-group")));
    }

    private static List<String> caseNamesOf(List<Element> elements) {
        return elements.stream().map(element -> element.getAttribute("case-name")).toList();
    }
}
