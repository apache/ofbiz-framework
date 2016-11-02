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
factory.findMatchingLabels(null, parameters.sourceFileName, parameters.sourceKey, null, false)
context.labels = factory.getLabels()
context.localesFound = factory.getLocalesFound()
context.filesFound = factory.getFilesFound()
context.componentNamesFound = factory.getComponentNamesFound()

if (parameters.sourceKey && parameters.sourceFileName) {
    context.label = context.labels.get(parameters.sourceKey + LabelManagerFactory.keySeparator + parameters.sourceFileName)
    context.titleProperty = "WebtoolsLabelManagerUpdate"
}
