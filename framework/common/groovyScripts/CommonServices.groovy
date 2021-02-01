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

import java.math.RoundingMode
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.util.Calendar;

import org.apache.ofbiz.base.util.Debug
import org.apache.ofbiz.base.util.UtilDateTime
import org.apache.ofbiz.base.util.UtilProperties
import org.apache.ofbiz.common.CommonWorkers
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.service.ModelService
import org.apache.ofbiz.service.ServiceUtil
import org.apache.ofbiz.base.util.ObjectType
import org.apache.ofbiz.webapp.event.FileUploadProgressListener 

/**
 * Main permission logic
 */
def commonGenericPermission() {
    parameters.primaryPermission = "COMMON"
    Map result = run service: "genericBasePermissionCheck", with: parameters
    return result
}

/**
 * Convert UOM values
 */
def convertUom() {
    // base case: if both uomIds are the same, return the original value
    Map result = success()
    Timestamp asOfDate
    GenericValue uomConversion
    RoundingMode roundingMode
    int decimalScale
    BigDecimal convertedValue

    // setting up default parameters if not specified
    if (!parameters.defaultRoundingMode) {
        parameters.defaultRoundingMode = RoundingMode.HALF_EVEN
    }
    if (!parameters.defaultDecimalScale) {
        parameters.defaultDecimalScale = 2
    }

    if (parameters.uomId == parameters.uomIdTo) {
        result.convertedValue = parameters.originalValue
        return result
    }

    asOfDate = parameters.asOfDate ?: UtilDateTime.nowTimestamp()

    //first try the UomConversionDated entity
    Map condition = [uomId: parameters.uomId,
                     uomIdTo: parameters.uomIdTo]
    if (parameters.purposeEnumId) {
        condition.purposeEnumId = parameters.purposeEnumId
    }

    // sort by descending fromDate to get newest (biggest) first
    uomConversion = from("UomConversionDated")
            .where(condition)
            .orderBy("-fromDate")
            .filterByDate(asOfDate)
            .cache()
            .queryFirst()

    // if no conversion found with specified purpose, try w/o purpose
    if (!uomConversion) {
        if (parameters.purposeEnumId) {
            uomConversion = from("UomConversionDated")
                    .where(uomId: parameters.uomId,
                            uomIdTo: parameters.uomIdTo)
                    .orderBy("-fromDate")
                    .filterByDate(asOfDate)
                    .cache()
                    .queryFirst()
        }
    }

    // if not found, try the uom conversion entity
    if (!uomConversion) {
        uomConversion = from("UomConversion").where(parameters).cache().queryOne()
    }
    logVerbose("using conversion factor=${uomConversion.conversionFactor}")

    if (!uomConversion) {
        // if still no uom conversion entity, then no conversion is possible
        return error(UtilProperties.getMessage("CommonUiLabels", "CommonNoUomConversionFound", parameters.locale))
    }
    else {
        // Do custom conversion, if we have customMethodId
        if (uomConversion.customMethodId) { //custom conversion?
            logVerbose("using custom conversion customMethodId=${uomConversion.customMethodId}")
            Map customParms = parameters.convertUom
            customParms.uomConversion = uomConversion
            Map serviceResult = run service: "convertUomCustom", with: customParms
            convertedValue = serviceResult.convertedValue

            logVerbose("Custom UoM conversion returning convertedValue=${convertedValue}")
        }
        else { // not custom conversion
            // do the conversion
            if (parameters.originalValue && uomConversion.conversionFactor) {
                convertedValue = (parameters.originalValue).multiply(BigDecimal.valueOf(uomConversion.conversionFactor))
                convertedValue = convertedValue.setScale(15, RoundingMode.HALF_EVEN)
            }
        } //custom conversion?

        // round result, if UomConversion[Dated] so specifies
        decimalScale = uomConversion.decimalScale ?: parameters.defaultDecimalScale
        roundingMode = uomConversion.roundingMode ?: parameters.defaultRoundingMode
        if (parameters.defaultRoundingMode != roundingMode) {
            convertedValue = convertedValue.setScale(decimalScale, roundingMode)
        }
    } // no UomConversion or UomConversionDated found

    // all done
    result.convertedValue = convertedValue
    logVerbose("""Uom conversion of [${parameters.originalValue}] from [${parameters.uomId}]
                           to [${parameters.uomIdTo}] using conversion factor [${uomConversion.conversionFactor}],
                           result is [${convertedValue}]""")

    return result
}

// convertUomCustom: Dispatcher for calling Custom Method for UoM conversion
/**
 * Convert UOM values using CustomMethod
 */
def convertUomCustom() {
    Map result = success()
    Map uomConversion = parameters.uomConversion
    String customMethodId = uomConversion.customMethodId
    GenericValue customMethod = from("CustomMethod").where(customMethodId: customMethodId).cache().queryOne()
    if (!customMethod?.customMethodName) {
        return error(UtilProperties.getMessage("CommonUiLabels", "CommonNoCustomMethodName", parameters.locale))
    } else {
        logVerbose("calling custom method" + customMethod.customMethodName)
        Map serviceResult = run service: customMethod.customMethodName, with: [arguments: parameters]
        result.convertedValue = serviceResult.convertedValue
    }
    return result
}

/**
 * Look up progress made in File Upload process
 */
def getFileUploadProgressStatus() {
    FileUploadProgressListener uploadProgressListener = parameters.uploadProgressListener
    Map result = success()
    if (uploadProgressListener) {
        result.contentLength = uploadProgressListener.getContentLength()
        result.bytesRead = uploadProgressListener.getBytesRead()
        result.hasStarted = uploadProgressListener.hasStarted()

        result.readPercent = (result.bytesRead * 100) / result.contentLength
    }
    return result
}

/**
 * Get visual theme resources
 */
def getVisualThemeResources() {
    Map result = success()
    String visualThemeId = parameters.visualThemeId
    Map themeResources = parameters.themeResources ?: [:]
    List resourceList = from("VisualThemeResource")
            .where(visualThemeId: visualThemeId)
            .orderBy("resourceTypeEnumId", "sequenceId")
            .cache()
            .queryList()
    if (!resourceList) {
        // if not found use the good old initial ofbiz theme so the system will at least start up and will be usable
        logWarning("Could not find the ${visualThemeId} theme, reverting back to the good old OFBiz theme...")
        visualThemeId = UtilProperties.getPropertyValue("general", "VISUAL_THEME", "FLAT_GREY")
        resourceList = from("VisualThemeResource")
            .where(visualThemeId: visualThemeId)
            .orderBy("resourceTypeEnumId", "sequenceId")
            .cache()
            .queryList()
    }
    if (!resourceList) {
        return error(UtilProperties.getMessage("CommonUiLabels", "CommonVisualThemeResourcesNotFound", parameters.locale))
    }
    for (GenericValue resourceRecord : resourceList) {
        String resourceTypeEnumId = resourceRecord.resourceTypeEnumId
        String resourceValue = resourceRecord.resourceValue
        if (!resourceValue) {
            logWarning(UtilProperties.getMessage("CommonUiLabels", "CommonVisualThemeInvalidRecord", parameters.locale))
        }
        else {
            themeResources[resourceTypeEnumId] = [resouceTypeEnumId: resourceValue]
        }
    }
    result.themeResources = themeResources
    return result
}

/**
 * Returns a list of country
 */
def getCountryList() {
    Map result = success()
    List countryList = []
    List geoList = CommonWorkers.getCountryList(delegator)
    for (GenericValue countryGeo : geoList) {
        countryList << countryGeo.geoName + ": " + countryGeo.geoId
    }
    result.countryList = countryList
    return result
}

/**
 * set the state options for selected country
 */
def getAssociatedStateList() {
    Map result = success()
    List stateList = []

    List geoList = CommonWorkers.getAssociatedStateList(delegator, parameters.countryGeoId, parameters.listOrderBy)
    for (GenericValue stateGeo : geoList) {
        String stateName = stateGeo.geoName + ": " + stateGeo.geoId
        stateList << stateName
    }
    if (!stateList) {
        stateList << UtilProperties.getMessage("CommonUiLabels", "CommonNoStatesProvinces", parameters.locale)
    }
    result.stateList = stateList
    return result
}

/**
 * Link Geos to another Geo
 */
def linkGeos() {
    List oldGeoIds = from("GeoAssoc")
            .where(geoId: parameters.geoId,
                    geoAssocTypeId: parameters.geoAssocTypeId)
            .cache()
            .getFieldList('geoIdTo')
    // Old list contains current values
    for (String geoIdTo : parameters.geoIds) {
        if (!oldGeoIds?.contains(geoIdTo)) {
            // If it already exist, nothing to do and we keep it
            GenericValue oldGeoAssoc = from("GeoAssoc").where(geoId: parameters.geoId, geoIdTo: geoIdTo).queryOne()
            if (!oldGeoAssoc) {
                // Add as it does not exist
                GenericValue newGeoAssoc = makeValue("GeoAssoc", [
                    geoId : parameters.geoId,
                    geoIdTo : geoIdTo,
                    geoAssocTypeId : parameters.geoAssocTypeId
                ])
                newGeoAssoc.create()
            }
        }
    }
    return success()
}

/**
 * get related geos to a geo through a geoAssoc
 * @return
 */
def getRelatedGeos() {
    Map result = success()
    List geoList = from("GeoAssoc")
            .where(geoId: parameters.geoId, geoAssocTypeId: parameters.geoAssocTypeId)
            .getFieldList('geoIdTo')
    if (!geoList) {
        geoList << "____"
    }
    result.geoList = geoList
    return result
}

/**
 * Returns true if an UomConversion record exists
 */
def checkUomConversion() {
    Map result = success()
    result.exist = from("UomConversion").where(uomId: parameters.uomId, uomIdTo: parameters.uomIdTo).queryCount() == 1
    return result
}

/**
 * Returns true if an UomConversionDated record exists
 */
def checkUomConversionDated() {
    Map result = success()
    Map condition = [
        uomId: parameters.uomId,
        uomIdTo: parameters.uomIdTo
    ]
    if (parameters.purposeEnumId) {
        condition.purposeEnumId = parameters.purposeEnumId
    }
    result.exist = from("UomConversion").where(condition).filterByDate().queryCount() == 1
    return result
}

def getServerTimestamp() {
    Map result = success()
    result.serverTimestamp = UtilDateTime.nowTimestamp()
    return result
}

def getServerTimeZone() {
    Map result = success()
    result.serverTimeZone = TimeZone.getDefault().toZoneId().toString()
    return result
}

def getServerTimestampAsLong() {
    Map result = success()
    result.serverTimestamp = UtilDateTime.nowTimestamp().getTime()
    return result
}

/**
 * Create a KeywordThesaurus
 * @return
 */
def createKeywordThesaurus() {
    GenericValue newEntity = makeValue("KeywordThesaurus", parameters)
    newEntity.enteredKeyword = newEntity.enteredKeyword.toLowerCase()
    newEntity.alternateKeyword = newEntity.alternateKeyword.toLowerCase()
    newEntity.create()
    return success()
}

/**
 * Delete a complete Entry KeywordThesaurus
 * @return
 */
def deleteKeywordThesaurus() {
    GenericValue newEntity = makeValue("KeywordThesaurus")
    newEntity.enteredKeyword = parameters.enteredKeyword
    if (parameters.alternateKeyword) {
        newEntity.alternateKeyword = parameters.alternateKeyword
    }
    delegator.removeByAnd("KeywordThesaurus", newEntity)
    return success()
}

def createFuturePeriod() {
    applTypes = []
    grain = null
    intermediate = null
    parties = from('PartyAcctgPreference').where('enableAccounting','Y').queryIterator()
    while (party = parties.next()){
        parameters.organizationPartyId = party.partyId
        createCustomTimePeriod = from('SystemProperty')
            .where('systemResourceId','general', 'systemPropertyId','CustomTimePeriod.create').queryOne()
        if ("Y".equals(createCustomTimePeriod.systemPropertyValue)) {
            // get list of CustomTypePeriod types
            applTypes = from('SystemProperty')
                .where('systemResourceId','general', 'systemPropertyId','CustomTimePeriod.applType').queryOne()
            List types = Arrays.asList(applTypes.systemPropertyValue.split("\\s*,\\s*"))
            types.each{periodTypeId ->
                Calendar periodCal = Calendar.getInstance();
                systemPropertyId = "CustomTimePeriod." + periodTypeId + ".intermediate"
                applTypeInter = from('SystemProperty')
                    .where('systemResourceId','general', 'systemPropertyId',systemPropertyId).queryOne()
                if (applTypeInter) {
                    intermediate = applTypeInter.systemPropertyValue
                }
                // get grain for application type
                systemPropertyId = "CustomTimePeriod." + periodTypeId + ".grain"
                applTypeGrain = from('SystemProperty')
                    .where('systemResourceId','general', 'systemPropertyId',systemPropertyId).queryOne()
                if (applTypeGrain) {
                    grain = applTypeGrain.systemPropertyValue
                    if ("MONTH".equals(grain)) {
                        periodCal.add(Calendar.MONTH, 1)
                        monthName = new SimpleDateFormat("MMM").format(periodCal.getTime())
                        year = periodCal.get(Calendar.YEAR)
                        month = (periodCal.get(Calendar.MONTH) + 1).toString()
                        if ( 1 == month.length()) {
                            month = "0" + month
                        }
                        periodCal.set(Calendar.DATE, 1)
                        periodStart = new java.sql.Date(periodCal.getTimeInMillis())
                        periodStartDate = periodStart.toString() +" 00:00:00.000"
                        lastPeriodDay =  periodCal.getActualMaximum(Calendar.DAY_OF_MONTH); 
                        periodCal.set(Calendar.DATE, lastPeriodDay)
                        periodEnd = new java.sql.Date(periodCal.getTimeInMillis())
                        periodEndDate = periodEnd.toString() + " 23:59:59.999"
                        parameters.isClosed = "N"
                        // check whether the period for the year exists
                        yearStartDate = Timestamp.valueOf(year + "-01-01 00:00:00.000")
                        yearEndDate = Timestamp.valueOf(year + "-12-31 23:59:59.999")
                        yearPeriodType = periodTypeId + "_YEAR"
                        existingYear = from('CustomTimePeriod')
                            .where('fromDate',yearStartDate, 'thruDate',yearEndDate,'organizationPartyId',parameters.organizationPartyId, 'periodTypeId',yearPeriodType).queryFirst()
                        if (existingYear) {
                            parameters.parentPeriodId = existingYear.customTimePeriodId
                        } else {
                            parameters.fromDate = yearStartDate
                            parameters.thruDate = yearEndDate
                            parameters.periodTypeId = yearPeriodType
                            parameters.periodNum = year + "00"
                            parameters.periodName = year.toString()
                            // persist the future period
                            inMap = dispatcher.getDispatchContext().makeValidContext("createCustomTimePeriod", ModelService.IN_PARAM, parameters)
                            serviceResult = run service: "createCustomTimePeriod", with: inMap
                            if (!ServiceUtil.isSuccess(serviceResult)) {
                                return error(serviceResult.errorMessage)
                            } else {
                                parameters.parentPeriodId = serviceResult.customTimePeriodId
                            }
                        }
                        // check whether the future period exists
                        parameters.customTimePeriodId = year + month
                        parameters.periodNum = year + month
                        parameters.periodName = (year + "-" + monthName).toUpperCase()
                        fromDate = Timestamp.valueOf(periodStartDate)
                        thruDate = Timestamp.valueOf(periodEndDate)
                        parameters.periodTypeId = periodTypeId + "_" + grain
                        existingPeriod = from('CustomTimePeriod')
                            .where('fromDate',fromDate, 'thruDate',thruDate,'organizationPartyId',parameters.organizationPartyId, 'periodTypeId',parameters.periodTypeId).queryFirst()
                        if (!existingPeriod) {
                            parameters.fromDate = periodStartDate
                            parameters.thruDate = periodEndDate
                            // persist the future period
                            inMap = dispatcher.getDispatchContext().makeValidContext("createCustomTimePeriod", ModelService.IN_PARAM, parameters)
                            serviceResult = run service: "createCustomTimePeriod", with: inMap
                            if (!ServiceUtil.isSuccess(serviceResult)) return error(serviceResult.errorMessage)
                        }
                        parameters.parentPeriodId = ""
                    }
                }
            }
        }
    }
    parties.close()
}