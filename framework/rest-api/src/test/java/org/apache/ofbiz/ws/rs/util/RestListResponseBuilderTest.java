package org.apache.ofbiz.ws.rs.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

public final class RestListResponseBuilderTest {

    @Test
    public void buildsMetadataAndNextLink() {
        List<Map<String, Object>> productionRuns = List.of(Map.of("workEffortId", "PR1001"));
        Map<String, Object> result = RestListResponseBuilder.forList("productionRuns", productionRuns)
                .pageIndex(0)
                .pageSize(20)
                .totalCount(54)
                .requestPath("/rest/production-runs?pageIndex=0&pageSize=20")
                .build();

        assertEquals(0, result.get("pageIndex"));
        assertEquals(20, result.get("pageSize"));
        assertEquals(54L, result.get("totalCount"));
        assertEquals(true, result.get("hasNext"));
        assertEquals(productionRuns, result.get("productionRuns"));

        Map<?, ?> links = (Map<?, ?>) result.get("links");
        assertNotNull(links.get("next"));

        Map<?, ?> nextLink = (Map<?, ?>) links.get("next");
        assertEquals("/rest/production-runs?pageIndex=1&pageSize=20", nextLink.get("href"));
        assertEquals("next", nextLink.get("rel"));
    }

    @Test
    public void buildsLinksFromPlainRequestPath() {
        Map<String, Object> result = RestApiUtil.getCollectionResult("items", List.of(), 0, 20, 21, "/rest/items");

        Map<?, ?> links = (Map<?, ?>) result.get("links");
        Map<?, ?> selfLink = (Map<?, ?>) links.get("self");
        Map<?, ?> nextLink = (Map<?, ?>) links.get("next");

        assertEquals("/rest/items?pageIndex=0&pageSize=20", selfLink.get("href"));
        assertEquals("/rest/items?pageIndex=1&pageSize=20", nextLink.get("href"));
    }

    @Test
    public void preservesRepeatedQueryParametersInGeneratedLinks() {
        Map<String, Object> result = RestApiUtil.getCollectionResult("items", List.of(), 0, 20, 21,
                "/rest/items?statusId=A&statusId=B&pageIndex=0&pageSize=20");

        Map<?, ?> links = (Map<?, ?>) result.get("links");
        Map<?, ?> nextLink = (Map<?, ?>) links.get("next");

        assertEquals("/rest/items?statusId=A&statusId=B&pageIndex=1&pageSize=20", nextLink.get("href"));
    }

    @Test
    public void reencodesSpecialCharactersInGeneratedLinks() {
        Map<String, Object> result = RestApiUtil.getCollectionResult("items", List.of(), 0, 20, 21,
                "/rest/items?search=red%20shirt&token=a%2Bb%25c%26d%3De&city=%E6%9D%B1%E4%BA%AC&pageIndex=0&pageSize=20");

        Map<?, ?> links = (Map<?, ?>) result.get("links");
        Map<?, ?> nextLink = (Map<?, ?>) links.get("next");
        String href = (String) nextLink.get("href");

        assertTrue(href.contains("search=red%20shirt"));
        assertTrue(href.contains("token=a%2Bb%25c%26d%3De"));
        assertTrue(href.contains("city=%E6%9D%B1%E4%BA%AC"));
        assertTrue(href.endsWith("pageIndex=1&pageSize=20"));
    }

    @Test
    public void includesTotalPagesAndBoundaryLinks() {
        Map<String, Object> result = RestListResponseBuilder.forList("items", List.of("A", "B"))
                .pageIndex(1)
                .pageSize(20)
                .totalCount(87)
                .requestPath("/rest/items?pageIndex=1&pageSize=20")
                .build();

        assertEquals(5L, result.get("totalPages"));
        assertEquals(1L, result.get("previousPageCount"));
        assertEquals(3L, result.get("nextPageCount"));

        Map<?, ?> links = (Map<?, ?>) result.get("links");
        Map<?, ?> firstLink = (Map<?, ?>) links.get("first");
        Map<?, ?> lastLink = (Map<?, ?>) links.get("last");

        assertEquals("/rest/items?pageIndex=0&pageSize=20", firstLink.get("href"));
        assertEquals("first", firstLink.get("rel"));
        assertEquals("/rest/items?pageIndex=4&pageSize=20", lastLink.get("href"));
        assertEquals("last", lastLink.get("rel"));
    }

    @Test
    public void omitsPrevLinkOnFirstPage() {
        Map<String, Object> result = RestListResponseBuilder.forList("items", List.of("A"))
                .pageIndex(0)
                .pageSize(20)
                .totalCount(40)
                .requestPath("/rest/items?pageIndex=0&pageSize=20")
                .build();

        Map<?, ?> links = (Map<?, ?>) result.get("links");

        assertFalse(links.containsKey("prev"));
        assertTrue(links.containsKey("next"));
        assertTrue(links.containsKey("first"));
        assertTrue(links.containsKey("last"));
    }

    @Test
    public void omitsBoundaryLinksWhenThereAreNoResults() {
        Map<String, Object> result = RestListResponseBuilder.forList("items", List.of())
                .pageIndex(0)
                .pageSize(20)
                .totalCount(0)
                .requestPath("/rest/items?pageIndex=0&pageSize=20")
                .build();

        Map<?, ?> links = (Map<?, ?>) result.get("links");

        assertNotNull(links.get("self"));
        assertFalse(links.containsKey("first"));
        assertFalse(links.containsKey("last"));
        assertFalse(links.containsKey("prev"));
        assertFalse(links.containsKey("next"));
    }

    @Test
    public void omitsBoundaryAndNavigationLinksWhenThereAreNoResultsOnLaterPage() {
        Map<String, Object> result = RestListResponseBuilder.forList("items", List.of())
                .pageIndex(3)
                .pageSize(20)
                .totalCount(0)
                .requestPath("/rest/items?pageIndex=3&pageSize=20")
                .build();

        Map<?, ?> links = (Map<?, ?>) result.get("links");

        assertNotNull(links.get("self"));
        assertFalse(links.containsKey("first"));
        assertFalse(links.containsKey("last"));
        assertFalse(links.containsKey("prev"));
        assertFalse(links.containsKey("next"));
    }

    @Test
    public void omitsLastLinkWhenLastPageIndexExceedsIntegerMaxValue() {
        long overflowTotalCount = ((long) Integer.MAX_VALUE + 2L) * 20L;
        Map<String, Object> result = RestListResponseBuilder.forList("items", List.of("A"))
                .pageIndex(0)
                .pageSize(20)
                .totalCount(overflowTotalCount)
                .requestPath("/rest/items?pageIndex=0&pageSize=20")
                .build();

        Map<?, ?> links = (Map<?, ?>) result.get("links");

        assertTrue(links.containsKey("first"));
        assertTrue(links.containsKey("next"));
        assertFalse(links.containsKey("last"));
    }

    @Test
    public void omitsNextLinkWhenNextPageIndexExceedsIntegerMaxValue() {
        long totalCountWithNextPage = ((long) Integer.MAX_VALUE + 2L) * 20L;
        Map<String, Object> result = RestListResponseBuilder.forList("items", List.of("A"))
                .pageIndex(Integer.MAX_VALUE)
                .pageSize(20)
                .totalCount(totalCountWithNextPage)
                .requestPath("/rest/items?pageIndex=2147483647&pageSize=20")
                .build();

        Map<?, ?> links = (Map<?, ?>) result.get("links");

        assertTrue(links.containsKey("self"));
        assertTrue(links.containsKey("first"));
        assertTrue(links.containsKey("prev"));
        assertFalse(links.containsKey("next"));
    }

    @Test
    public void omitsLinksWhenRequestPathMissing() {
        Map<String, Object> result = RestListResponseBuilder.forList("items", List.of("A"))
                .pageIndex(1)
                .pageSize(20)
                .totalCount(87)
                .build();

        assertFalse(result.containsKey("links"));
    }

    @Test
    public void includesZeroPreviousAndNextPageCountsWhenNoResults() {
        Map<String, Object> result = RestListResponseBuilder.forList("items", List.of())
                .pageIndex(0)
                .pageSize(20)
                .totalCount(0)
                .requestPath("/rest/items?pageIndex=0&pageSize=20")
                .build();

        assertEquals(0L, result.get("previousPageCount"));
        assertEquals(0L, result.get("nextPageCount"));
    }
}
