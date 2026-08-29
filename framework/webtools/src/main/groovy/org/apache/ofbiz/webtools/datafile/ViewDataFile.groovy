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
package org.apache.ofbiz.webtools.datafile

import org.apache.ofbiz.base.util.Debug
import org.apache.ofbiz.base.util.GeneralException
import org.apache.ofbiz.base.util.UtilProperties
import org.apache.ofbiz.base.util.UtilURL
import org.apache.ofbiz.datafile.DataFile
import org.apache.ofbiz.datafile.DataFile2EntityXml
import org.apache.ofbiz.datafile.ModelDataFileReader

uiLabelMap = UtilProperties.getResourceBundleMap('WebtoolsUiLabels', locale)
messages = []

// This screen builds URLs from raw request parameters and dereferences them; that fetch must
// never happen for a principal who lacks DATAFILE_MAINT, so the whole thing is gated here,
// ahead of any parameter handling, rather than left to the screen/template permission checks
// that only gate rendering after the fetch already ran.
if (security.hasPermission('DATAFILE_MAINT', session)) {
    dataFileSave = request.getParameter('DATAFILE_SAVE')

    entityXmlFileSave = request.getParameter('ENTITYXML_FILE_SAVE')

    dataFileLoc = request.getParameter('DATAFILE_LOCATION')
    definitionLoc = request.getParameter('DEFINITION_LOCATION')
    definitionName = request.getParameter('DEFINITION_NAME')
    dataFileIsUrl = null != request.getParameter('DATAFILE_IS_URL')
    definitionIsUrl = null != request.getParameter('DEFINITION_IS_URL')

    dataFileUrl = null
    try {
        dataFileUrl = dataFileIsUrl ? UtilURL.fromCheckedUrlString(dataFileLoc) : UtilURL.fromFilename(dataFileLoc)
    }
    catch (java.net.MalformedURLException | GeneralException e) {
        messages.add(e.getMessage())
    }

    definitionUrl = null
    try {
        definitionUrl = definitionIsUrl ? UtilURL.fromCheckedUrlString(definitionLoc) : UtilURL.fromFilename(definitionLoc)
    }
    catch (java.net.MalformedURLException | GeneralException e) {
        messages.add(e.getMessage())
    }

    definitionNames = null
    if (definitionUrl) {
        try {
            ModelDataFileReader reader = ModelDataFileReader.getModelDataFileReader(definitionUrl)
            if (reader) {
                definitionNames = ((Collection)reader.getDataFileNames()).iterator()
                context.put('definitionNames', definitionNames)
            }
        }
        catch (Exception e) {
            messages.add(e.getMessage())
        }
    }

    dataFile = null
    if (dataFileUrl && definitionUrl && definitionNames) {
        try {
            dataFile = DataFile.readFile(dataFileUrl, definitionUrl, definitionName)
            context.put('dataFile', dataFile)
        }
        catch (Exception e) {
            messages.add(e.getMessage())
            Debug.logError(e, 'Error reading data file', 'ViewDataFile.groovy')
        }
    }

    if (dataFile) {
        modelDataFile = dataFile.getModelDataFile()
        context.put('modelDataFile', modelDataFile)
    }

    if (dataFile && dataFileSave) {
        try {
            dataFile.writeDataFile(dataFileSave)
            messages.add(uiLabelMap.WebtoolsDataFileSavedTo + dataFileSave)
        }
        catch (Exception e) {
            messages.add(e.getMessage())
        }
    }

    if (dataFile && entityXmlFileSave) {
        try {
            //dataFile.writeDataFile(entityXmlFileSave)
            DataFile2EntityXml.writeToEntityXml(entityXmlFileSave, dataFile)
            messages.add(uiLabelMap.WebtoolsDataEntityFileSavedTo + entityXmlFileSave)
        }
        catch (Exception e) {
            messages.add(e.getMessage())
        }
    }
}
context.messages = messages
