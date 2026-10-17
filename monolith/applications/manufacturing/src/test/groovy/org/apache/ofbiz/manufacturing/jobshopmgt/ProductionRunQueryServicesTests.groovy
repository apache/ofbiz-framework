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
package org.apache.ofbiz.manufacturing.jobshopmgt

import static org.junit.jupiter.api.Assertions.assertThrows

import org.apache.ofbiz.service.ServiceUtil
import org.apache.ofbiz.ws.rs.util.RestApiUtil
import org.apache.ofbiz.ws.rs.util.RestQueryOptions
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest

class ProductionRunQueryServicesTests {

    @Test
    void testProductionRunSortMapsFrameworkFields() {
        ProductionRunQueryServices services = new ProductionRunQueryServices()

        assert services.productionRunOrderBy('-status') == ['-currentStatusId', '-estimatedStartDate', 'workEffortId']
        assert services.productionRunOrderBy('productionRunId') == ['workEffortId', '-estimatedStartDate']
    }

    @Test
    void testProductionRunDefaultSortMatchesContract() {
        ProductionRunQueryServices services = new ProductionRunQueryServices()

        assert services.productionRunOrderBy(null) == ['-estimatedStartDate', 'workEffortId']
    }

    @Test
    void testProductionRunRejectsUnsupportedSortField() {
        ProductionRunQueryServices services = new ProductionRunQueryServices()

        IllegalArgumentException exception = assertThrows(IllegalArgumentException) {
            services.productionRunOrderBy('productName')
        }
        assert exception.message == 'Unsupported sort field: productName'
    }

    @Test
    void testFindProductionRunsRejectsUnsupportedSortBeforeEmptyProductNameResults() {
        ProductionRunQueryServices services = new ProductionRunQueryServices()
        services.binding.setVariable('parameters', [sort: 'productName', productName: 'NO_MATCH'])

        Map result = services.findProductionRuns()

        assert ServiceUtil.isError(result)
        assert ServiceUtil.getErrorMessage(result) == 'Unsupported sort field: productName'
    }

    @Test
    void testFrameworkRequestPathReturnsNullWithoutRequestBinding() {
        ProductionRunQueryServices services = new ProductionRunQueryServices()

        assert RestApiUtil.getRelativeRequestPath(services.binding) == null
    }

    @Test
    void testFrameworkNavigationMetadataAndLinksUseRequestPath() {
        ProductionRunQueryServices services = new ProductionRunQueryServices()
        MockHttpServletRequest request = new MockHttpServletRequest()
        request.setRequestURI('/rest/manufacturing/production-runs')
        request.setQueryString('pageIndex=1&pageSize=1&statusId=PRUN_CREATED')
        services.binding.setVariable('request', request)
        RestQueryOptions queryOptions = RestQueryOptions.fromParameters([
                pageIndex: 1,
                pageSize: 1,
                statusId: 'PRUN_CREATED'
        ])

        Map result = RestApiUtil.getPagedResult('productionRuns', [], queryOptions, 3L,
                RestApiUtil.getRelativeRequestPath(services.binding))

        assert result.previousPageCount == 1L
        assert result.nextPageCount == 1L
        assert result.links instanceof Map
        assert result.links.self.href == '/rest/manufacturing/production-runs?statusId=PRUN_CREATED&pageIndex=1&pageSize=1'
        assert result.links.prev.href == '/rest/manufacturing/production-runs?statusId=PRUN_CREATED&pageIndex=0&pageSize=1'
        assert result.links.next.href == '/rest/manufacturing/production-runs?statusId=PRUN_CREATED&pageIndex=2&pageSize=1'
    }

}
