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

import static org.junit.jupiter.api.Assertions.assertThrows

import org.apache.ofbiz.service.ServiceUtil
import org.apache.ofbiz.ws.rs.util.RestApiUtil
import org.apache.ofbiz.ws.rs.util.RestQueryOptions
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest

class BomManagementServicesTests {

    @Test
    void testBomProductSortMapsFrameworkFields() {
        BomManagementServices services = new BomManagementServices()

        assert services.bomProductOrderBy('-bomType') == ['-productAssocTypeId', 'productId']
    }

    @Test
    void testBomTypeSortMapsFrameworkFields() {
        BomManagementServices services = new BomManagementServices()

        assert services.bomTypeOrderBy('-productAssocTypeId') == ['-productAssocTypeId', 'description']
    }

    @Test
    void testBomDefaultSortsMatchContracts() {
        BomManagementServices services = new BomManagementServices()

        assert services.bomProductOrderBy(null) == ['productId', 'productAssocTypeId']
        assert services.bomTypeOrderBy(null) == ['description', 'productAssocTypeId']
    }

    @Test
    void testBomProductRejectsUnsupportedSortField() {
        BomManagementServices services = new BomManagementServices()

        IllegalArgumentException exception = assertThrows(IllegalArgumentException) {
            services.bomProductOrderBy('productName')
        }
        assert exception.message == 'Unsupported sort field: productName'
    }

    @Test
    void testFindBomProductsRejectsUnsupportedSortBeforeEmptyResults() {
        BomManagementServices services = new BomManagementServices()
        services.binding.setVariable('parameters', [sort: 'productName'])

        Map result = services.findBomProducts()

        assert ServiceUtil.isError(result)
        assert ServiceUtil.getErrorMessage(result) == 'Unsupported sort field: productName'
    }

    @Test
    void testFrameworkRequestPathReturnsNullWithoutRequestBinding() {
        BomManagementServices services = new BomManagementServices()

        assert RestApiUtil.getRelativeRequestPath(services.binding) == null
    }

    @Test
    void testFrameworkNavigationMetadataAndLinksUseRequestPath() {
        BomManagementServices services = new BomManagementServices()
        MockHttpServletRequest request = new MockHttpServletRequest()
        request.setRequestURI('/rest/manufacturing/boms/products')
        request.setQueryString('pageIndex=1&pageSize=1&productAssocTypeId=MANUF_COMPONENT')
        services.binding.setVariable('request', request)
        RestQueryOptions queryOptions = RestQueryOptions.fromParameters([
                pageIndex: 1,
                pageSize: 1,
                productAssocTypeId: 'MANUF_COMPONENT'
        ])

        Map result = RestApiUtil.getPagedResult('products', [], queryOptions, 3L,
                RestApiUtil.getRelativeRequestPath(services.binding))

        assert result.previousPageCount == 1L
        assert result.nextPageCount == 1L
        assert result.links instanceof Map
        assert result.links.self.href == '/rest/manufacturing/boms/products?productAssocTypeId=MANUF_COMPONENT&pageIndex=1&pageSize=1'
        assert result.links.prev.href == '/rest/manufacturing/boms/products?productAssocTypeId=MANUF_COMPONENT&pageIndex=0&pageSize=1'
        assert result.links.next.href == '/rest/manufacturing/boms/products?productAssocTypeId=MANUF_COMPONENT&pageIndex=2&pageSize=1'
    }

}
