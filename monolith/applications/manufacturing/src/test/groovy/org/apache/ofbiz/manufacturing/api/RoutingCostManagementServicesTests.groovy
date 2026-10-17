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

import org.apache.ofbiz.ws.rs.util.RestApiUtil
import org.apache.ofbiz.ws.rs.util.RestQueryOptions
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest

class RoutingCostManagementServicesTests {

    @Test
    void testRoutingSortMapsFrameworkFields() {
        RoutingCostManagementServices services = new RoutingCostManagementServices()

        assert services.routingOrderBy('-status') == ['-currentStatusId', 'workEffortId']
    }

    @Test
    void testRoutingTaskSortMapsFrameworkFields() {
        RoutingCostManagementServices services = new RoutingCostManagementServices()

        assert services.routingTaskOrderBy('-routingTaskId') == ['-workEffortId', 'workEffortName']
    }

    @Test
    void testRoutingPurposeTypeSortMapsFrameworkFields() {
        RoutingCostManagementServices services = new RoutingCostManagementServices()

        assert services.routingPurposeTypeOrderBy('-description') == ['-description', 'workEffortPurposeTypeId']
    }

    @Test
    void testRoutingDefaultSortsMatchContracts() {
        RoutingCostManagementServices services = new RoutingCostManagementServices()

        assert services.routingOrderBy(null) == ['workEffortId']
        assert services.routingTaskOrderBy(null) == ['workEffortName', 'workEffortId']
        assert services.routingPurposeTypeOrderBy(null) == ['workEffortPurposeTypeId']
    }

    @Test
    void testRoutingRejectsUnsupportedSortField() {
        RoutingCostManagementServices services = new RoutingCostManagementServices()

        IllegalArgumentException exception = assertThrows(IllegalArgumentException) {
            services.routingOrderBy('productId')
        }
        assert exception.message == 'Unsupported sort field: productId'
    }

    @Test
    void testFrameworkRequestPathReturnsNullWithoutRequestBinding() {
        RoutingCostManagementServices services = new RoutingCostManagementServices()

        assert RestApiUtil.getRelativeRequestPath(services.binding) == null
    }

    @Test
    void testFrameworkNavigationMetadataAndLinksUseRequestPath() {
        RoutingCostManagementServices services = new RoutingCostManagementServices()
        MockHttpServletRequest request = new MockHttpServletRequest()
        request.setRequestURI('/rest/manufacturing/routings')
        request.setQueryString('pageIndex=1&pageSize=1&currentStatusId=ROU_ACTIVE')
        services.binding.setVariable('request', request)
        RestQueryOptions queryOptions = RestQueryOptions.fromParameters([
                pageIndex: 1,
                pageSize: 1,
                currentStatusId: 'ROU_ACTIVE'
        ])

        Map result = RestApiUtil.getPagedResult('routings', [], queryOptions, 3L,
                RestApiUtil.getRelativeRequestPath(services.binding))

        assert result.previousPageCount == 1L
        assert result.nextPageCount == 1L
        assert result.links instanceof Map
        assert result.links.self.href == '/rest/manufacturing/routings?currentStatusId=ROU_ACTIVE&pageIndex=1&pageSize=1'
        assert result.links.prev.href == '/rest/manufacturing/routings?currentStatusId=ROU_ACTIVE&pageIndex=0&pageSize=1'
        assert result.links.next.href == '/rest/manufacturing/routings?currentStatusId=ROU_ACTIVE&pageIndex=2&pageSize=1'
    }

}
