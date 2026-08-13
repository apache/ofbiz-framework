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
package org.apache.ofbiz.manufacturing.api

import org.apache.ofbiz.ws.rs.util.RestApiUtil
import org.apache.ofbiz.ws.rs.util.RestQueryOptions
import org.apache.ofbiz.service.ServiceUtil
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest

class ManufacturingLookupServicesTests {

    @Test
    void testProductSortMapsFrameworkFields() {
        ManufacturingLookupServices services = new ManufacturingLookupServices()

        assert services.productLookupOrderBy('-productName') == ['-productName', 'productId']
    }

    @Test
    void testProductLookupRejectsUnsupportedSortWithoutQuery() {
        ManufacturingLookupServices services = new ManufacturingLookupServices()
        services.binding.setVariable('parameters', [sort: 'notAField'])

        Map result = services.findProductLookupOptions()

        assert ServiceUtil.isError(result)
        assert ServiceUtil.getErrorMessage(result) == 'Unsupported sort field: notAField'
    }

    @Test
    void testFacilitySortMapsFrameworkFields() {
        ManufacturingLookupServices services = new ManufacturingLookupServices()

        assert services.facilityLookupOrderBy('-facilityName') == ['-facilityName', 'facilityId']
    }

    @Test
    void testLookupDefaultSortsMatchContracts() {
        ManufacturingLookupServices services = new ManufacturingLookupServices()

        assert services.productLookupOrderBy(null) == ['productId']
        assert services.partyLookupOrderBy(null) == ['partyId']
        assert services.fixedAssetLookupOrderBy(null) == ['fixedAssetName', 'fixedAssetId']
        assert services.facilityLookupOrderBy(null) == ['facilityName', 'facilityId']
    }

    @Test
    void testFrameworkRequestPathReturnsNullWithoutRequestBinding() {
        ManufacturingLookupServices services = new ManufacturingLookupServices()

        assert RestApiUtil.getRelativeRequestPath(services.binding) == null
    }

    @Test
    void testFrameworkNavigationMetadataAndLinksUseRequestPath() {
        ManufacturingLookupServices services = new ManufacturingLookupServices()
        MockHttpServletRequest request = new MockHttpServletRequest()
        request.setRequestURI('/rest/manufacturing/lookups/products')
        request.setQueryString('pageIndex=1&pageSize=1&query=MAT')
        services.binding.setVariable('request', request)
        RestQueryOptions queryOptions = RestQueryOptions.fromParameters([
                pageIndex: 1,
                pageSize: 1,
                query: 'MAT'
        ])

        Map result = RestApiUtil.getPagedResult('products', [], queryOptions, 3L,
                RestApiUtil.getRelativeRequestPath(services.binding))

        assert result.previousPageCount == 1L
        assert result.nextPageCount == 1L
        assert result.links instanceof Map
        assert result.links.self.href == '/rest/manufacturing/lookups/products?query=MAT&pageIndex=1&pageSize=1'
        assert result.links.prev.href == '/rest/manufacturing/lookups/products?query=MAT&pageIndex=0&pageSize=1'
        assert result.links.next.href == '/rest/manufacturing/lookups/products?query=MAT&pageIndex=2&pageSize=1'
    }

}
