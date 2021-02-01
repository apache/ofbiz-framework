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
package org.apache.ofbiz.accounting;

import org.apache.ofbiz.base.util.UtilDateTime
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.service.ServiceUtil
import org.apache.ofbiz.service.testtools.OFBizTestCase
import java.sql.Timestamp

class FixedAssetTests extends OFBizTestCase {
    public FixedAssetTests(String name) {
        super(name)
    }
    void testCreateFixedAssetRegistration() {
        Map serviceCtx = [
                fixedAssetId           : 'DEMO_VEHICLE_01',
                licenseNumber          : '123456',
                registrationNumber     : 'abcdef',
                registrationDate       : UtilDateTime.toTimestamp("01/01/2020 00:00:00"),
                fromDate               : UtilDateTime.nowTimestamp(),
                userLogin              : userLogin
        ]
        Map serviceResult = dispatcher.runSync('createFixedAssetRegistration', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue fixedAssetRegistration = from('FixedAssetRegistration')
                .where('fixedAssetId', 'DEMO_VEHICLE_01')
                .filterByDate().queryFirst();
        assert fixedAssetRegistration
    }
    void testUpdateFixedAssetRegistration() {
        Timestamp fromDate = UtilDateTime.toTimestamp("04/01/2020 00:00:00")
        Map serviceCtx = [
                fixedAssetId           : 'DEMO_VEHICLE_01',
                licenseNumber          : 'updated-123456',
                registrationNumber     : 'updated-abcdef',
                registrationDate       : UtilDateTime.toTimestamp("01/01/2020 00:00:00"),
                fromDate               : fromDate,
                thruDate               : UtilDateTime.nowTimestamp(),
                userLogin              : userLogin
        ]
        Map serviceResult = dispatcher.runSync('updateFixedAssetRegistration', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue fixedAssetRegistration = from('FixedAssetRegistration')
                .where('fixedAssetId', 'DEMO_VEHICLE_01', 'fromDate', fromDate)
                .filterByDate().queryOne();
        assert fixedAssetRegistration == null
    }
    void testDeleteFixedAssetRegistration() {
        Timestamp fromDate = UtilDateTime.toTimestamp("04/01/2020 00:00:00")
        Map serviceCtx = [
                fixedAssetId           : 'DEMO_VEHICLE_01',
                fromDate               : fromDate,
                userLogin              : userLogin
        ]
        Map serviceResult = dispatcher.runSync('deleteFixedAssetRegistration', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue fixedAssetRegistration = from('FixedAssetRegistration')
                .where('fixedAssetId', 'DEMO_VEHICLE_01', 'fromDate', fromDate)
                .queryOne();
        assert fixedAssetRegistration == null
    }
    void testCreateFixedAssetMeter() {
        Map serviceCtx = [
                fixedAssetId           : 'DEMO_VEHICLE_01',
                productMeterTypeId     : 'ODOMETER',
                readingDate            : UtilDateTime.nowTimestamp(),
                meterValue             : BigDecimal.valueOf(65),
                userLogin              : userLogin
        ]
        Map serviceResult = dispatcher.runSync('createFixedAssetMeter', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue fixedAssetMeter = from('FixedAssetMeter')
                .where('fixedAssetId', 'DEMO_VEHICLE_01', 'productMeterTypeId', 'ODOMETER')
                .queryFirst();
        assert fixedAssetMeter
    }
    void testUpdateFixedAssetMeter() {
        Timestamp readingDate = UtilDateTime.toTimestamp("04/01/2020 00:00:00")
        Map serviceCtx = [
                fixedAssetId           : 'DEMO_VEHICLE_01',
                productMeterTypeId     : 'ODOMETER',
                readingDate            : readingDate,
                meterValue             : BigDecimal.valueOf(85),
                userLogin              : userLogin
        ]
        Map serviceResult = dispatcher.runSync('updateFixedAssetMeter', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue fixedAssetMeter = from('FixedAssetMeter')
                .where('fixedAssetId', 'DEMO_VEHICLE_01', 'productMeterTypeId', 'ODOMETER', 'readingDate', readingDate)
                .queryOne();
        assert fixedAssetMeter
    }
    void testDeleteFixedAssetMeter() {
        Timestamp readingDate = UtilDateTime.toTimestamp("04/01/2020 00:00:00")
        Map serviceCtx = [
                fixedAssetId           : 'DEMO_VEHICLE_01',
                productMeterTypeId     : 'ODOMETER',
                readingDate            : readingDate,
                userLogin              : userLogin
        ]
        Map serviceResult = dispatcher.runSync('deleteFixedAssetMeter', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue fixedAssetMeter = from('FixedAssetMeter')
                .where('fixedAssetId', 'DEMO_VEHICLE_01', 'productMeterTypeId', 'ODOMETER', 'readingDate', readingDate)
                .queryOne();
        assert fixedAssetMeter == null
    }
    void testCreateFixedAssetGeoPoint() {
        Map serviceCtx = [
                fixedAssetId           : 'DEMO_VEHICLE_01',
                geoPointId             : '9000',
                userLogin              : userLogin
        ]
        Map serviceResult = dispatcher.runSync('createFixedAssetGeoPoint', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue fixedAssetGeoPoint = from('FixedAssetGeoPoint')
                .where('fixedAssetId', 'DEMO_VEHICLE_01', 'geoPointId', '9000')
                .filterByDate().queryFirst();
        assert fixedAssetGeoPoint
    }
}