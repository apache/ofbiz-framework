package org.apache.ofbiz.ws.rs.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.apache.ofbiz.base.util.UtilMisc;
import org.junit.jupiter.api.Test;

public final class RestQueryOptionsTest {

    @Test
    public void normalizesPagingAndCompatibilityAliases() {
        RestQueryOptions query = RestQueryOptions.fromParameters(UtilMisc.toMap(
                "VIEW_INDEX", "1",
                "VIEW_SIZE", "25",
                "sort", "-fieldOne"));

        assertEquals(1, query.getPageIndex());
        assertEquals(25, query.getPageSize());
        assertEquals("-fieldOne", query.getSort());
    }

    @Test
    public void rejectsInvalidPageSize() {
        assertThrows(IllegalArgumentException.class, () ->
                RestQueryOptions.fromParameters(UtilMisc.toMap("pageSize", "0")));
    }

    @Test
    public void rejectsMalformedPagingValues() {
        assertThrows(IllegalArgumentException.class, () ->
                RestQueryOptions.fromParameters(UtilMisc.toMap("pageIndex", "abc")));
        assertThrows(IllegalArgumentException.class, () ->
                RestQueryOptions.fromParameters(UtilMisc.toMap("pageSize", "xyz")));
    }

    @Test
    public void usesDefaultPagingWhenMissing() {
        RestQueryOptions query = RestQueryOptions.fromParameters(UtilMisc.toMap("filterOne", "valueOne"));

        assertEquals(RestQueryOptions.DEFAULT_PAGE_INDEX, query.getPageIndex());
        assertEquals(RestQueryOptions.DEFAULT_PAGE_SIZE, query.getPageSize());
        assertEquals("valueOne", query.getFilters().get("filterOne"));
    }

    @Test
    public void preservesFilterParameters() {
        RestQueryOptions query = RestQueryOptions.fromParameters(UtilMisc.toMap(
                "filterOne", "valueOne",
                "filterTwo", "valueTwo",
                "pageIndex", "0",
                "pageSize", "20"));

        assertEquals("valueOne", query.getFilters().get("filterOne"));
        assertEquals("valueTwo", query.getFilters().get("filterTwo"));
    }

    @Test
    public void normalizesPagingParametersDirectly() {
        RestQueryOptions query = RestQueryOptions.fromParameters(UtilMisc.toMap(
                "pageIndex", "2",
                "pageSize", "30",
                "sort", "fieldOne"));

        assertEquals(2, query.getPageIndex());
        assertEquals(30, query.getPageSize());
        assertEquals("fieldOne", query.getSort());
    }
}
