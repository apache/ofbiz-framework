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
package org.apache.ofbiz.accounting.accounting

import org.apache.ofbiz.base.util.UtilDateTime
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.service.ServiceUtil
import org.apache.ofbiz.testtools.JunitJupiterTest
import org.apache.ofbiz.testtools.JupiterTestHelper
import java.sql.Timestamp
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test

@JunitJupiterTest
class FixedAssetTests implements JupiterTestHelper {

    @Test
    @Order(1)
    void testCreateFixedAssetRegistration() {
        String fixedAssetId = testParams.fixedAssetId ?: 'DEMO_VEHICLE_01'
        Map serviceCtx = [
                fixedAssetId: fixedAssetId,
                licenseNumber: testParams.licenseNumber ?: '123456',
                registrationNumber: testParams.registrationNumber ?: 'abcdef',
                registrationDate: UtilDateTime.toTimestamp('01/01/2020 00:00:00'),
                fromDate: UtilDateTime.nowTimestamp(),
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createFixedAssetRegistration', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue fixedAssetRegistration = from('FixedAssetRegistration')
                .where('fixedAssetId', fixedAssetId)
                .filterByDate().queryFirst()
        assert fixedAssetRegistration
    }
    @Test
    @Order(2)
    void testUpdateFixedAssetRegistration() {
        Timestamp fromDate = UtilDateTime.toTimestamp('04/01/2020 00:00:00')
        String fixedAssetId = testParams.fixedAssetId ?: 'DEMO_VEHICLE_01'
        Map serviceCtx = [
                fixedAssetId: fixedAssetId,
                licenseNumber: testParams.licenseNumber ?: 'updated-123456',
                registrationNumber: testParams.registrationNumber ?: 'updated-abcdef',
                registrationDate: UtilDateTime.toTimestamp('01/01/2020 00:00:00'),
                fromDate: fromDate,
                thruDate: UtilDateTime.nowTimestamp(),
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('updateFixedAssetRegistration', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue fixedAssetRegistration = from('FixedAssetRegistration')
                .where('fixedAssetId', fixedAssetId, 'fromDate', fromDate)
                .filterByDate().queryOne()
        assert !fixedAssetRegistration
    }
    @Test
    @Order(3)
    void testDeleteFixedAssetRegistration() {
        Timestamp fromDate = UtilDateTime.toTimestamp('04/01/2020 00:00:00')
        String fixedAssetId = testParams.fixedAssetId ?: 'DEMO_VEHICLE_01'
        Map serviceCtx = [
                fixedAssetId: fixedAssetId,
                fromDate: fromDate,
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('deleteFixedAssetRegistration', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue fixedAssetRegistration = from('FixedAssetRegistration')
                .where('fixedAssetId', fixedAssetId, 'fromDate', fromDate)
                .queryOne()
        assert !fixedAssetRegistration
    }
    @Test
    @Order(4)
    void testCreateFixedAssetMeter() {
        String fixedAssetId = testParams.fixedAssetId ?: 'DEMO_VEHICLE_01'
        String productMeterTypeId = testParams.productMeterTypeId ?: 'ODOMETER'
        Map serviceCtx = [
                fixedAssetId: fixedAssetId,
                productMeterTypeId: productMeterTypeId,
                readingDate: UtilDateTime.nowTimestamp(),
                meterValue: BigDecimal.valueOf(65),
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createFixedAssetMeter', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue fixedAssetMeter = from('FixedAssetMeter')
                .where('fixedAssetId', fixedAssetId, 'productMeterTypeId', productMeterTypeId)
                .queryFirst()
        assert fixedAssetMeter
    }
    @Test
    @Order(5)
    void testUpdateFixedAssetMeter() {
        Timestamp readingDate = UtilDateTime.toTimestamp('04/01/2020 00:00:00')
        String fixedAssetId = testParams.fixedAssetId ?: 'DEMO_VEHICLE_01'
        String productMeterTypeId = testParams.productMeterTypeId ?: 'ODOMETER'
        Map serviceCtx = [
                fixedAssetId: fixedAssetId,
                productMeterTypeId: productMeterTypeId,
                readingDate: readingDate,
                meterValue: BigDecimal.valueOf(85),
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('updateFixedAssetMeter', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue fixedAssetMeter = from('FixedAssetMeter')
                .where('fixedAssetId', fixedAssetId, 'productMeterTypeId', productMeterTypeId, 'readingDate', readingDate)
                .queryOne()
        assert fixedAssetMeter
    }
    @Test
    @Order(6)
    void testDeleteFixedAssetMeter() {
        Timestamp readingDate = UtilDateTime.toTimestamp('04/01/2020 00:00:00')
        String fixedAssetId = testParams.fixedAssetId ?: 'DEMO_VEHICLE_01'
        String productMeterTypeId = testParams.productMeterTypeId ?: 'ODOMETER'
        Map serviceCtx = [
                fixedAssetId: fixedAssetId,
                productMeterTypeId: productMeterTypeId,
                readingDate: readingDate,
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('deleteFixedAssetMeter', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue fixedAssetMeter = from('FixedAssetMeter')
                .where('fixedAssetId', fixedAssetId, 'productMeterTypeId', productMeterTypeId, 'readingDate', readingDate)
                .queryOne()
        assert !fixedAssetMeter
    }
    @Test
    @Order(7)
    void testCreateFixedAssetGeoPoint() {
        String fixedAssetId = testParams.fixedAssetId ?: 'DEMO_VEHICLE_01'
        String geoPointId = testParams.geoPointId ?: '9000'
        Map serviceCtx = [
                fixedAssetId: fixedAssetId,
                geoPointId: geoPointId,
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createFixedAssetGeoPoint', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue fixedAssetGeoPoint = from('FixedAssetGeoPoint')
                .where('fixedAssetId', fixedAssetId, 'geoPointId', geoPointId)
                .filterByDate().queryFirst()
        assert fixedAssetGeoPoint
    }

}
