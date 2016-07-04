<#--
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
-->
        <form action="<@ofbizUrl>DuplicateWorkEffort</@ofbizUrl>" method="post">
            <input type="hidden" name="oldWorkEffortId" value="${workEffortId!}"/>
            <div>
                <span class="label">${uiLabelMap.ProductDuplicateRemoveSelectedWithNewId}</span>
                <input type="text" size="20" maxlength="20" name="workEffortId"/>&nbsp;<input type="submit" class="smallSubmit" value="${uiLabelMap.CommonDuplicate}!"/>
            </div>
            <div>
                <span class="label">${uiLabelMap.CommonDuplicate}</span>
                <label>${uiLabelMap.FormFieldTitle_rate}&nbsp;<input type="checkbox" name="duplicateWorkEffortAssignmentRates" value="Y" checked="checked"/></label>
                <label>${uiLabelMap.WorkEffortAssoc}&nbsp;<input type="checkbox" name="duplicateWorkEffortAssocs" value="Y" checked="checked"/></label>
                <label>${uiLabelMap.ProductContent}&nbsp;<input type="checkbox" name="duplicateWorkEffortContents" value="Y" checked="checked"/></label>
                <label>${uiLabelMap.WorkEffortNotes}&nbsp;<input type="checkbox" name="duplicateWorkEffortNotes" value="Y" checked="checked"/></label>
            </div>
            <div>
                <span class="label">${uiLabelMap.CommonRemove}</span>
                <label>${uiLabelMap.FormFieldTitle_rate}&nbsp;<input type="checkbox" name="removeWorkEffortAssignmentRates" value="Y"/></label>
                <label>${uiLabelMap.WorkEffortAssoc}&nbsp;<input type="checkbox" name="removeWorkEffortAssocs" value="Y"/></label>
                <label>${uiLabelMap.ProductContent}&nbsp;<input type="checkbox" name="removeWorkEffortContents" value="Y"/></label>
                <label>${uiLabelMap.WorkEffortNotes}&nbsp;<input type="checkbox" name="removeWorkEffortNotes" value="Y"/></label>
            </div>
        </form>