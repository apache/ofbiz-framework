package org.apache.ofbiz.ws.rs.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ofbiz.base.util.UtilMisc;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import groovy.lang.Binding;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.Response;

public final class RestApiUtilPaginationTest {

    @Test
    public void validatesAllowedSortFields() {
        List<String> validatedFields = RestApiUtil.validateSortFields("fieldOne,-fieldTwo",
                Set.of("fieldOne", "fieldTwo", "fieldThree"));

        assertEquals(List.of("fieldOne", "-fieldTwo"), validatedFields);
    }

    @Test
    public void rejectsUnsupportedSortFields() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                RestApiUtil.validateSortFields("fieldFour", Set.of("fieldOne", "fieldTwo")));

        assertEquals("Unsupported sort field: fieldFour", exception.getMessage());
    }

    @Test
    public void validatesSortSyntaxWithoutAllowlist() {
        List<String> validatedFields = RestApiUtil.validateSortFields("fieldOne,-fieldFour", null);

        assertEquals(List.of("fieldOne", "-fieldFour"), validatedFields);
    }

    @Test
    public void rejectsMalformedSortExpressionsWithEmptySegments() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                RestApiUtil.validateSortFields("fieldOne,,fieldTwo", Set.of("fieldOne", "fieldTwo")));

        assertEquals("Sort expression contains an empty field", exception.getMessage());
    }

    @Test
    public void rejectsMalformedSortExpressionsWithTrailingEmptySegment() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                RestApiUtil.validateSortFields("fieldOne,", Set.of("fieldOne", "fieldTwo")));

        assertEquals("Sort expression contains an empty field", exception.getMessage());
    }

    @Test
    public void rejectsMalformedSortExpressionsWithMultipleTrailingEmptySegments() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                RestApiUtil.validateSortFields("fieldOne,,", Set.of("fieldOne", "fieldTwo")));

        assertEquals("Sort expression contains an empty field", exception.getMessage());
    }

    @Test
    public void rejectsMalformedSortExpressionsWithBareDescendingPrefix() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                RestApiUtil.validateSortFields("-", Set.of("fieldOne", "fieldTwo")));

        assertEquals("Sort expression contains a malformed field", exception.getMessage());
    }

    @Test
    public void rejectsDuplicateSortFields() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                RestApiUtil.validateSortFields("fieldOne,-fieldOne", Set.of("fieldOne", "fieldTwo")));

        assertEquals("Duplicate sort field: fieldOne", exception.getMessage());
    }

    @Test
    public void resolvesOrderByWithFieldAliasesAndStableDefaults() {
        List<String> orderBy = RestApiUtil.resolveOrderBy("-displayName",
                UtilMisc.toMap("displayName", "entityName", "externalId", "entityId"),
                List.of("entityName", "entityId"));

        assertEquals(List.of("-entityName", "entityId"), orderBy);
    }

    @Test
    public void resolvesOrderByToDefaultsWhenSortMissing() {
        List<String> defaultOrderBy = List.of("entityName", "entityId");

        assertEquals(defaultOrderBy, RestApiUtil.resolveOrderBy(null,
                UtilMisc.toMap("externalId", "entityId"), defaultOrderBy));
    }

    @Test
    public void rejectsOrderByAliasesThatMapToDuplicateEntityFields() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                RestApiUtil.resolveOrderBy("displayName,-entityName",
                        UtilMisc.toMap("displayName", "entityName", "entityName", "entityName"),
                        List.of("entityId")));

        assertEquals("Duplicate sort field: entityName", exception.getMessage());
    }

    @Test
    public void sortsMapRowsByRestSortExpression() {
        List<Map<String, Object>> rows = List.of(
                UtilMisc.toMap("itemId", "ITEM-A", "locationId", "LOC1"),
                UtilMisc.toMap("itemId", "ITEM-C", "locationId", "LOC1"),
                UtilMisc.toMap("itemId", "ITEM-B", "locationId", "LOC1"));

        List<Map<String, Object>> sortedRows = RestApiUtil.sortMapRows(rows, "-itemId",
                Set.of("itemId", "locationId"), Comparator.comparing(row -> (String) row.get("itemId")));

        assertEquals(List.of("ITEM-C", "ITEM-B", "ITEM-A"), sortedRows.stream().map(row -> row.get("itemId")).toList());
    }

    @Test
    public void rejectsUnsupportedMapRowSortFields() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                RestApiUtil.sortMapRows(List.of(Map.of("itemId", "ITEM-A")), "notAField",
                        Set.of("itemId"), Comparator.comparing(row -> (String) row.get("itemId"))));

        assertEquals("Unsupported sort field: notAField", exception.getMessage());
    }

    @Test
    public void rejectsUnsupportedMapRowSortFieldsWhenRowsEmpty() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                RestApiUtil.sortMapRows(List.of(), "notAField",
                        Set.of("itemId"), Comparator.comparing(row -> (String) row.get("itemId"))));

        assertEquals("Unsupported sort field: notAField", exception.getMessage());
    }

    @Test
    public void returnsMutableSortedMapRows() {
        List<Map<String, Object>> rows = List.of(Map.of("itemId", "ITEM-A"));
        List<Map<String, Object>> sortedRows = RestApiUtil.sortMapRows(rows, "itemId",
                Set.of("itemId"), Comparator.comparing(row -> (String) row.get("itemId")));

        sortedRows.add(Map.of("itemId", "ITEM-B"));

        assertEquals(2, sortedRows.size());
    }

    @Test
    public void pagesAlreadyComputedLists() {
        List<String> page = RestApiUtil.pageList(List.of("a", "b", "c", "d"), 1, 2);

        assertEquals(List.of("c", "d"), page);
    }

    @Test
    public void returnsEmptyPageWhenComputedPageStartsAfterListEnd() {
        List<String> page = RestApiUtil.pageList(List.of("a", "b"), 2, 2);

        assertEquals(List.of(), page);
    }

    @Test
    public void returnsEmptyPageWhenComputedPageIndexOverflowsIntMultiplication() {
        List<String> page = RestApiUtil.pageList(List.of("a", "b"), Integer.MAX_VALUE, 100);

        assertEquals(List.of(), page);
    }

    @Test
    public void serializesAvailableRelationsAsHttpLinkHeader() {
        Map<String, Object> links = new LinkedHashMap<>();
        links.put("first", RestApiUtil.makeLinkMap("/rest/items?pageIndex=0&pageSize=20", Map.of("rel", "first")));
        links.put("next", RestApiUtil.makeLinkMap("/rest/items?pageIndex=2&pageSize=20", Map.of("rel", "next")));

        String header = RestApiUtil.toLinkHeaderValue(links);

        assertEquals("</rest/items?pageIndex=0&pageSize=20>; rel=\"first\", "
                + "</rest/items?pageIndex=2&pageSize=20>; rel=\"next\"", header);
    }

    @Test
    public void omitsIncompleteRelationsFromHttpLinkHeader() {
        Map<String, Object> links = new LinkedHashMap<>();
        links.put("self", RestApiUtil.makeLinkMap("/rest/items?pageIndex=0&pageSize=20", Map.of("rel", "self")));
        links.put("ignored", Map.of("href", "/rest/items?pageIndex=1&pageSize=20"));

        String header = RestApiUtil.toLinkHeaderValue(links);

        assertEquals("</rest/items?pageIndex=0&pageSize=20>; rel=\"self\"", header);
    }

    @Test
    public void returnsNullLinkHeaderWhenNoRelationsAvailable() {
        assertNull(RestApiUtil.toLinkHeaderValue(null));
        assertNull(RestApiUtil.toLinkHeaderValue(Map.of()));
    }

    @Test
    public void validatesAllowedFilterFields() {
        Map<String, Object> validatedFilters = RestApiUtil.validateFilterParameters(
                Map.of("filterOne", "valueOne", "filterTwo", "valueTwo"), Set.of("filterOne", "filterTwo"));

        assertEquals(Map.of("filterOne", "valueOne", "filterTwo", "valueTwo"), validatedFilters);
    }

    @Test
    public void preservesFiltersWhenAllowlistMissing() {
        Map<String, Object> validatedFilters = RestApiUtil.validateFilterParameters(
                Map.of("filterOne", "valueOne", "filterThree", "valueThree"), null);

        assertEquals(Map.of("filterOne", "valueOne", "filterThree", "valueThree"), validatedFilters);
    }

    @Test
    public void rejectsUnsupportedFilterFields() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                RestApiUtil.validateFilterParameters(
                        Map.of("filterOne", "valueOne", "filterThree", "valueThree"), Set.of("filterOne", "filterTwo")));

        assertEquals("Unsupported filter field: filterThree", exception.getMessage());
    }

    @Test
    public void rejectsRepeatedFilterValuesWhenFieldIsNotRepeatable() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                RestApiUtil.validateFilterParameters(
                        UtilMisc.toMap("statusId", List.of("STATE_CREATED", "STATE_RUNNING")),
                        Set.of("statusId"), null, null));

        assertEquals("Filter parameter does not support repeated values: statusId", exception.getMessage());
    }

    @Test
    public void preservesRepeatedFilterValuesWhenFieldIsRepeatable() {
        Map<String, Object> validatedFilters = RestApiUtil.validateFilterParameters(
                UtilMisc.toMap("statusId", List.of("STATE_CREATED", "STATE_RUNNING")),
                Set.of("statusId"), Set.of("statusId"), null);

        assertEquals(List.of("STATE_CREATED", "STATE_RUNNING"), validatedFilters.get("statusId"));
    }

    @Test
    public void rejectsInvalidFilterValueFormatWhenValidatorProvided() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                RestApiUtil.validateFilterParameters(
                        UtilMisc.toMap("pageSizeHint", "abc"),
                        Set.of("pageSizeHint"),
                        null,
                        UtilMisc.toMap("pageSizeHint", (RestApiUtil.FilterValueValidator) (fieldName, value) -> {
                            if (UtilMisc.toIntegerObject(value) == null) {
                                throw new IllegalArgumentException("Invalid integer value for filter field: " + fieldName);
                            }
                        })));

        assertEquals("Invalid integer value for filter field: pageSizeHint", exception.getMessage());
    }

    @Test
    public void successAddsHttpLinkHeaderWhenPaginationLinksPresent() {
        Map<String, Object> links = new LinkedHashMap<>();
        links.put("self", RestApiUtil.makeLinkMap("/rest/items?pageIndex=0&pageSize=20", Map.of("rel", "self")));
        links.put("next", RestApiUtil.makeLinkMap("/rest/items?pageIndex=1&pageSize=20", Map.of("rel", "next")));

        Response response = RestApiUtil.success("Success", Map.of("items", List.of(), "links", links));

        assertEquals("</rest/items?pageIndex=0&pageSize=20>; rel=\"self\", "
                + "</rest/items?pageIndex=1&pageSize=20>; rel=\"next\"", response.getHeaderString("Link"));
    }

    @Test
    public void successOmitsHttpLinkHeaderWhenPaginationLinksMissing() {
        Response response = RestApiUtil.success("Success", Map.of("items", List.of()));

        assertNull(response.getHeaderString("Link"));
    }

    @Test
    public void buildsRelativeRequestPathWithQueryString() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/rest/items");
        request.setQueryString("pageIndex=1&pageSize=1");

        assertEquals("/rest/items?pageIndex=1&pageSize=1", RestApiUtil.getRelativeRequestPath(request));
    }

    @Test
    public void returnsNullRelativeRequestPathWhenRequestMissing() {
        assertNull(RestApiUtil.getRelativeRequestPath((HttpServletRequest) null));
    }

    @Test
    public void buildsRelativeRequestPathFromGroovyBinding() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/rest/items");
        request.setQueryString("pageIndex=1&pageSize=1");
        Binding binding = new Binding(Map.of("request", request));

        assertEquals("/rest/items?pageIndex=1&pageSize=1", RestApiUtil.getRelativeRequestPath(binding));
    }

    @Test
    public void returnsNullRelativeRequestPathWhenGroovyBindingHasNoRequest() {
        assertNull(RestApiUtil.getRelativeRequestPath(new Binding()));
    }

    @Test
    public void returnsNullRelativeRequestPathWhenGroovyBindingHasNonServletRequest() {
        assertNull(RestApiUtil.getRelativeRequestPath(new Binding(Map.of("request", "notARequest"))));
    }
}
