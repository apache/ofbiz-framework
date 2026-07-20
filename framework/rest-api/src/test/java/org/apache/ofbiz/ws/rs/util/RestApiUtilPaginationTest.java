package org.apache.ofbiz.ws.rs.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

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
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> RestApiUtil.validateSortFields("fieldFour", Set.of("fieldOne", "fieldTwo")));

        assertEquals("Unsupported sort field: fieldFour", exception.getMessage());
    }

    @Test
    public void rejectsMalformedSortExpressionsWithEmptySegments() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> RestApiUtil.validateSortFields("fieldOne,,fieldTwo", Set.of("fieldOne", "fieldTwo")));

        assertEquals("Sort expression contains an empty field", exception.getMessage());
    }

    @Test
    public void rejectsMalformedSortExpressionsWithTrailingEmptySegment() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> RestApiUtil.validateSortFields("fieldOne,", Set.of("fieldOne", "fieldTwo")));

        assertEquals("Sort expression contains an empty field", exception.getMessage());
    }

    @Test
    public void rejectsMalformedSortExpressionsWithMultipleTrailingEmptySegments() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> RestApiUtil.validateSortFields("fieldOne,,", Set.of("fieldOne", "fieldTwo")));

        assertEquals("Sort expression contains an empty field", exception.getMessage());
    }

    @Test
    public void rejectsMalformedSortExpressionsWithBareDescendingPrefix() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> RestApiUtil.validateSortFields("-", Set.of("fieldOne", "fieldTwo")));

        assertEquals("Sort expression contains a malformed field", exception.getMessage());
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
}
