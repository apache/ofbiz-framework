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
        <hr class="sepbar"/>
        <div class="head2">${uiLabelMap.WorkEffortDuplicateWorkEffort}</div>
        <form action="<@ofbizUrl>DuplicateWorkEffort</@ofbizUrl>" method="post" style="margin: 0;">
            <input type="hidden" name="oldWorkEffortId" value="${workEffortId?if_exists}"/>
            <div>
                <span class="tabletext">${uiLabelMap.ProductDuplicateRemoveSelectedWithNewId}:</span>
                <input type="text" class="inputBox" size="20" maxlength="20" name="workEffortId"/>&nbsp;<input type="submit" class="smallSubmit" value="${uiLabelMap.CommonDuplicate}!"/>
            </div>
            <div class="tabletext">
                <b>${uiLabelMap.CommonDuplicate}:</b>
                ${uiLabelMap.FormFieldTitle_rate}&nbsp;<input type="checkbox" class="checkBox" name="duplicateWorkEffortAssignmentRates" value="Y" checked="checked"/>
                ${uiLabelMap.WorkEffortAssoc}&nbsp;<input type="checkbox" class="checkBox" name="duplicateWorkEffortAssocs" value="Y" checked="checked"/>
                ${uiLabelMap.ProductContent}&nbsp;<input type="checkbox" class="checkBox" name="duplicateWorkEffortContents" value="Y" checked="checked"/>
                ${uiLabelMap.WorkEffortNotes}&nbsp;<input type="checkbox" class="checkBox" name="duplicateWorkEffortNotes" value="Y" checked="checked"/>
            </div>
            <div class="tabletext">
                <b>${uiLabelMap.CommonRemove}:</b>
                ${uiLabelMap.FormFieldTitle_rate}&nbsp;<input type="checkbox" class="checkBox" name="removeWorkEffortAssignmentRates" value="Y"/>               
                ${uiLabelMap.WorkEffortAssoc}&nbsp;<input type="checkbox" class="checkBox" name="removeWorkEffortAssocs" value="Y"/>
                ${uiLabelMap.ProductContent}&nbsp;<input type="checkbox" class="checkBox" name="removeWorkEffortContents" value="Y"/>
                ${uiLabelMap.WorkEffortNotes}&nbsp;<input type="checkbox" class="checkBox" name="removeWorkEffortNotes" value="Y"/>

            </div>
        </form>
        <hr class="sepbar"/>
