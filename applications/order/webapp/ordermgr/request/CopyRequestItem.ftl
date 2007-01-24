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

<#if custRequestItem?exists>
<form action="<@ofbizUrl>copyCustRequestItem</@ofbizUrl>" method="post" style="margin: 0;">
    <input type="hidden" name="custRequestId" value="${custRequestItem.custRequestId}"/>
    <input type="hidden" name="custRequestItemSeqId" value="${custRequestItem.custRequestItemSeqId}"/>
    <div class="tabletext">
        <b>${uiLabelMap.OrderCopyCustRequestItem}:</b>
        ${uiLabelMap.OrderOrderQuoteItems}&nbsp;<input type="checkbox" class="checkBox" name="copyLinkedQuotes" value="Y"/>
    </div>
    <input type="submit" class="smallSubmit" value="${uiLabelMap.CommonCopy}"/>
</form>
</#if>
