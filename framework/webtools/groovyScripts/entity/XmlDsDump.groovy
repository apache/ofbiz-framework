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

import java.util.*
import java.io.*
import org.apache.ofbiz.base.util.*
import org.apache.ofbiz.entity.model.*
import org.apache.ofbiz.entity.util.*
import org.apache.ofbiz.entity.transaction.*
import org.apache.ofbiz.entity.condition.*

outpath = parameters.outpath
filename = parameters.filename
maxRecStr = parameters.maxrecords
entitySyncId = parameters.entitySyncId
passedEntityNames = null
if (parameters.entityName) passedEntityNames = parameters.entityName instanceof Collection ? parameters.entityName as TreeSet : [parameters.entityName] as TreeSet

// get the max records per file setting and convert to a int
maxRecordsPerFile = 0
if (maxRecStr) {
    try {
        maxRecordsPerFile = Integer.parseInt(maxRecStr)
    }
    catch (Exception e) {
    }
}

preConfiguredSetName = parameters.preConfiguredSetName
if ("Product1".equals(preConfiguredSetName)) {
    passedEntityNames = new LinkedHashSet()
    passedEntityNames.add("DataResource")
    passedEntityNames.add("Facility")
    passedEntityNames.add("ProdCatalog")
    passedEntityNames.add("Product")
    passedEntityNames.add("ProductCategory")
    passedEntityNames.add("ProductFeatureCategory")
    passedEntityNames.add("ProductFeatureType")
    passedEntityNames.add("ProductPriceRule")
    passedEntityNames.add("ProductPromo")
} else if ("Product2".equals(preConfiguredSetName)) {
    passedEntityNames = new LinkedHashSet()
    passedEntityNames.add("Content")
    passedEntityNames.add("ElectronicText")
    passedEntityNames.add("FacilityLocation")
    passedEntityNames.add("ProdCatalogCategory")
    passedEntityNames.add("ProdCatalogRole")
    passedEntityNames.add("ProductAssoc")
    passedEntityNames.add("ProductAttribute")
    passedEntityNames.add("ProductCategoryMember")
    passedEntityNames.add("ProductCategoryRollup")
    passedEntityNames.add("ProductFacility")
    passedEntityNames.add("ProductFeature")
    passedEntityNames.add("ProductFeatureCategoryAppl")
    passedEntityNames.add("ProductKeyword")
    passedEntityNames.add("ProductPrice")
    passedEntityNames.add("ProductPriceAction")
    passedEntityNames.add("ProductPriceCond")
    passedEntityNames.add("ProductPromoCode")
    passedEntityNames.add("ProductPromoCategory")
    passedEntityNames.add("ProductPromoProduct")
    passedEntityNames.add("ProductPromoRule")
} else if ("Product3".equals(preConfiguredSetName)) {
    passedEntityNames = new LinkedHashSet()
    passedEntityNames.add("ProdCatalogInvFacility")
    passedEntityNames.add("ProductContent")
    passedEntityNames.add("ProductFacilityLocation")
    passedEntityNames.add("ProductFeatureAppl")
    passedEntityNames.add("ProductFeatureDataResource")
    passedEntityNames.add("ProductFeatureGroup")
    passedEntityNames.add("ProductPriceChange")
    passedEntityNames.add("ProductPromoAction")
    passedEntityNames.add("ProductPromoCodeEmail")
    passedEntityNames.add("ProductPromoCodeParty")
    passedEntityNames.add("ProductPromoCond")
} else if ("Product4".equals(preConfiguredSetName)) {
    passedEntityNames = new LinkedHashSet()
    passedEntityNames.add("InventoryItem")
    passedEntityNames.add("ProductFeatureCatGrpAppl")
    passedEntityNames.add("ProductFeatureGroupAppl")
} else if ("CatalogExport".equals(preConfiguredSetName)) {
    passedEntityNames = new LinkedHashSet()
    passedEntityNames.add("ProdCatalogCategoryType")
    passedEntityNames.add("ProdCatalog")
    passedEntityNames.add("ProductCategoryType")
    passedEntityNames.add("ProductCategory")
    passedEntityNames.add("ProductCategoryRollup")
    passedEntityNames.add("ProdCatalogCategory")
    passedEntityNames.add("ProductFeatureType")
    passedEntityNames.add("ProductFeatureCategory")

    passedEntityNames.add("DataResource")
    passedEntityNames.add("Content")
    passedEntityNames.add("ElectronicText")

    passedEntityNames.add("ProductType")
    passedEntityNames.add("Product")
    passedEntityNames.add("ProductAttribute")
    passedEntityNames.add("GoodIdentificationType")
    passedEntityNames.add("GoodIdentification")
    passedEntityNames.add("ProductPriceType")
    passedEntityNames.add("ProductPrice")

    passedEntityNames.add("ProductPriceRule")
    passedEntityNames.add("ProductPriceCond")
    passedEntityNames.add("ProductPriceAction")
    //passedEntityNames.add("ProductPriceChange")

    passedEntityNames.add("ProductPromo")
    passedEntityNames.add("ProductPromoCode")
    passedEntityNames.add("ProductPromoCategory")
    passedEntityNames.add("ProductPromoProduct")
    passedEntityNames.add("ProductPromoRule")
    passedEntityNames.add("ProductPromoAction")
    passedEntityNames.add("ProductPromoCodeEmail")
    passedEntityNames.add("ProductPromoCodeParty")
    passedEntityNames.add("ProductPromoCond")

    passedEntityNames.add("ProductCategoryMember")
    passedEntityNames.add("ProductAssoc")
    passedEntityNames.add("ProductContent")

    passedEntityNames.add("ProductFeature")
    passedEntityNames.add("ProductFeatureCategoryAppl")
    passedEntityNames.add("ProductFeatureAppl")
    passedEntityNames.add("ProductFeatureDataResource")
    passedEntityNames.add("ProductFeatureGroup")
    passedEntityNames.add("ProductFeatureCatGrpAppl")
    passedEntityNames.add("ProductFeatureGroupAppl")

    //passedEntityNames.add("ProductKeyword")
}

if (entitySyncId) {
    passedEntityNames = org.apache.ofbiz.entityext.synchronization.EntitySyncContext.getEntitySyncModelNamesToUse(dispatcher, entitySyncId)
}
checkAll = "true".equals(parameters.checkAll)
tobrowser = parameters.tobrowser != null
context.tobrowser = tobrowser

entityFromCond = null
entityThruCond = null
entityDateCond = null
if (entityFrom) {
    entityFromCond = EntityCondition.makeCondition("lastUpdatedTxStamp", EntityComparisonOperator.GREATER_THAN, entityFrom)
}
if (entityThru) {
    entityThruCond = EntityCondition.makeCondition("lastUpdatedTxStamp", EntityComparisonOperator.LESS_THAN, entityThru)
}
if (entityFromCond && entityThruCond) {
    entityDateCond = EntityCondition.makeCondition(entityFromCond, EntityJoinOperator.AND, entityThruCond)
} else if (entityFromCond) {
    entityDateCond = entityFromCond
} else if (entityThruCond) {
    entityDateCond = entityThruCond
}

reader = delegator.getModelReader()
modelEntities = reader.getEntityCache().values() as TreeSet
context.modelEntities = modelEntities

if (passedEntityNames) {
    if (tobrowser) {
        session.setAttribute("xmlrawdump_entitylist", passedEntityNames)
        session.setAttribute("entityDateCond", entityDateCond)
    } else {
        efo = new EntityFindOptions(true, EntityFindOptions.TYPE_SCROLL_INSENSITIVE, EntityFindOptions.CONCUR_READ_ONLY, true)
        numberOfEntities = passedEntityNames?.size() ?: 0
        context.numberOfEntities = numberOfEntities
        numberWritten = 0
    
        // single file
        if (filename && numberOfEntities) {
            if (outpath && !(filename.contains("/") && filename.contains("\\"))) {
                filename = outpath + File.separator + filename;
            }
            writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filename), "UTF-8")))
            writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
            writer.println("<entity-engine-xml>")
    
            passedEntityNames.each { curEntityName ->
                if (entityFrom) {
                    curModelEntity = reader.getModelEntity(curEntityName)
                    if (curModelEntity instanceof ModelViewEntity) {
                        return
                    }
                }
    
                beganTransaction = TransactionUtil.begin(3600)
                try {
                    me = reader.getModelEntity(curEntityName)
                    if (me.getNoAutoStamp() || me instanceof ModelViewEntity) {
                        values = delegator.find(curEntityName, null, null, null, me.getPkFieldNames(), efo)
                    } else {
                        values = delegator.find(curEntityName, entityDateCond, null, null, UtilMisc.toList("-createdTxStamp"), efo)
                    }
    
                    curNumberWritten = 0
                    while (value = values.next()) {
                        value.writeXmlText(writer, "")
                        numberWritten++
                        curNumberWritten++
                        if (curNumberWritten % 500 == 0 || curNumberWritten == 1) {
                            Debug.log("Records written [$curEntityName]: $curNumberWritten Total: $numberWritten")
                        }
                    }
                    values.close()
                    Debug.log("Wrote [$curNumberWritten] from entity : $curEntityName")
                    TransactionUtil.commit(beganTransaction)
                } catch (Exception e) {
                    errMsg = "Error reading data for XML export:"
                    Debug.logError(e, errMsg, "JSP")
                    TransactionUtil.rollback(beganTransaction, errMsg, e)
                }
            }
            writer.println("</entity-engine-xml>")
            writer.close()
            Debug.log("Total records written from all entities: $numberWritten")
            context.numberWritten = numberWritten
        }
    
        // multiple files in a directory
        results = []
        fileNumber = 1
        context.results = results
        if (outpath && !filename) {
            outdir = new File(outpath)
            if (!outdir.exists()) {
                outdir.mkdir()
            }
            if (outdir.isDirectory() && outdir.canWrite()) {
                passedEntityNames.each { curEntityName ->
                    numberWritten = 0
                    fileName = preConfiguredSetName ? UtilFormatOut.formatPaddedNumber((long) fileNumber, 3) + "_" : ""
                    fileName = fileName + curEntityName
    
                    values = null
                    beganTransaction = false
                    try {
                        beganTransaction = TransactionUtil.begin(3600)
    
                        me = delegator.getModelEntity(curEntityName)
                        if (me instanceof ModelViewEntity) {
                            results.add("[$fileNumber] [vvv] $curEntityName skipping view entity")
                            return
                        }
                        if (me.getNoAutoStamp() || me instanceof ModelViewEntity) {
                            values = delegator.find(curEntityName, null, null, null, me.getPkFieldNames(), efo)
                        } else {
                            values = delegator.find(curEntityName, entityDateCond, null, null, me.getPkFieldNames(), efo)
                        }
                        isFirst = true
                        writer = null
                        fileSplitNumber = 1
                        while (value = values.next()) {
                            //Don't bother writing the file if there's nothing
                            //to put into it
                            if (isFirst) {
                                writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(outdir, fileName +".xml")), "UTF-8")))
                                writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
                                writer.println("<entity-engine-xml>")
                                isFirst = false
                            }
                            value.writeXmlText(writer, "")
                            numberWritten++
    
                            // split into small files
                            if (maxRecordsPerFile > 0 && (numberWritten % maxRecordsPerFile == 0)) {
                                fileSplitNumber++
                                // close the file
                                writer.println("</entity-engine-xml>")
                                writer.close()
    
                                // create a new file
                                splitNumStr = UtilFormatOut.formatPaddedNumber((long) fileSplitNumber, 3)
                                writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(outdir, fileName + "_" + splitNumStr +".xml")), "UTF-8")))
                                writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
                                writer.println("<entity-engine-xml>")
                            }
    
                            if (numberWritten % 500 == 0 || numberWritten == 1) {
                                Debug.log("Records written [$curEntityName]: $numberWritten")
                            }
    
                        }
                        if (writer) {
                            writer.println("</entity-engine-xml>")
                            writer.close()
                            String thisResult = "[$fileNumber] [$numberWritten] $curEntityName wrote $numberWritten records"
                            Debug.log(thisResult)
                            results.add(thisResult)
                        } else {
                            thisResult = "[$fileNumber] [---] $curEntityName has no records, not writing file"
                            Debug.log(thisResult)
                            results.add(thisResult)
                        }
                        values.close()
                    } catch (Exception ex) {
                        if (values != null) {
                            values.close()
                        }
                        thisResult = "[$fileNumber] [xxx] Error when writing $curEntityName: $ex"
                        Debug.log(thisResult)
                        results.add(thisResult)
                        TransactionUtil.rollback(beganTransaction, thisResult, ex)
                    } finally {
                        // only commit the transaction if we started one... this will throw an exception if it fails
                        TransactionUtil.commit(beganTransaction)
                    }
                    fileNumber++
                }
            }
        }
    }
} else {
    context.numberOfEntities = 0
}
