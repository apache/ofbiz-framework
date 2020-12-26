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

import org.apache.ofbiz.entity.Delegator
import org.apache.ofbiz.webtools.labelmanager.*

LabelManagerFactory factory = LabelManagerFactory.getInstance()
context.factory = factory
factory.findMatchingLabels(parameters.labelComponentName, parameters.labelFileName, parameters.labelKey, parameters.labelLocaleName, "Y".equals(parameters.onlyNotUsedLabels))
context.labels = factory.getLabels()
allLabels = factory.getLabelsList();
context.labelsList = allLabels;
context.localesFound = factory.getLocalesFound()
context.filesFound = factory.getFilesFound()
context.componentNamesFound = factory.getComponentNamesFound()
context.duplicatedLocalesLabels = factory.getDuplicatedLocalesLabels()
context.duplicatedLocalesLabelsList = factory.getDuplicatedLocalesLabelsList()
context.keySeparator = factory.KEY_SEPARATOR
if ("Y".equals(parameters.onlyNotUsedLabels)) {
    LabelReferences refsObject = new LabelReferences(delegator, factory)
    Map references = refsObject.getLabelReferences()
    context.references = references
    context.referencesList = references.keySet()
}

context.totalLabelsCount = allLabels.size();
