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

import org.apache.ofbiz.entity.*
import org.apache.ofbiz.base.util.*
import org.apache.ofbiz.content.report.*

shipmentId = request.getParameter("shipmentId")
shipment = from("Shipment").where("shipmentId", shipmentId).queryOne()

if (shipment) {
    shipmentPackageRouteSegs = shipment.getRelated("ShipmentPackageRouteSeg", null, ['shipmentRouteSegmentId', 'shipmentPackageSeqId'], false)
    shipmentPackageDatas = [] as LinkedList
    if (shipmentPackageRouteSegs) {
        shipmentPackageRouteSegs.each { shipmentPackageRouteSeg ->
            shipmentPackages = shipmentPackageRouteSeg.getRelated("ShipmentPackage", null, ['shipmentPackageSeqId'], false)
            shipmentRouteSegment = shipmentPackageRouteSeg.getRelatedOne("ShipmentRouteSegment", false)
            if (shipmentPackages) {
                shipmentPackages.each { shipmentPackage ->
                    shipmentItemsDatas = [] as LinkedList
                    shipmentPackageContents = shipmentPackage.getRelated("ShipmentPackageContent", null, ['shipmentItemSeqId'], false)
                    if (shipmentPackageContents) {
                        shipmentPackageContents.each { shipmentPackageContent ->
                            shipmentItemsData = [:]
                            packageQuantity = shipmentPackageContent.getDouble("quantity")
                            shipmentItem = shipmentPackageContent.getRelatedOne("ShipmentItem", false)
                            if (shipmentItem) {
                                shippedQuantity = shipmentItem.getDouble("quantity")
                                shipmentItemsData.shipmentItem = shipmentItem
                                shipmentItemsData.shippedQuantity = shippedQuantity
                                shipmentItemsData.packageQuantity = packageQuantity
                                shipmentItemsDatas.add(shipmentItemsData)
                            }
                        }
                    }
                    shipmentPackageData = [:]
                    shipmentPackageData.shipmentPackage = shipmentPackage
                    shipmentPackageData.shipmentRouteSegment = shipmentRouteSegment
                    shipmentPackageData.shipmentItemsDatas = shipmentItemsDatas
                    shipmentPackageDatas.add(shipmentPackageData)
                }
            }
        }
    }
    context.shipmentPackageDatas = shipmentPackageDatas
}
context.shipmentId = shipmentId
context.shipment = shipment
