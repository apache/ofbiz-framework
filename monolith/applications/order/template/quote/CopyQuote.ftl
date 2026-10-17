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
<#if quote??>
<form action="<@ofbizUrl>copyQuote</@ofbizUrl>" method="post">
    <input type="hidden" name="quoteId" value="${quoteId}"/>
    <div>
        <span class="label">${uiLabelMap.OrderCopyQuote}</span>
        ${uiLabelMap.OrderOrderQuoteItems}&nbsp;<input type="checkbox" name="copyQuoteItems" value="Y" checked="checked" />
        ${uiLabelMap.OrderOrderQuoteAdjustments}&nbsp;<input type="checkbox" name="copyQuoteAdjustments" value="Y" checked="checked" />
        ${uiLabelMap.OrderOrderQuoteRoles}&nbsp;<input type="checkbox" name="copyQuoteRoles" value="Y" checked="checked" />
        ${uiLabelMap.OrderOrderQuoteAttributes}&nbsp;<input type="checkbox" name="copyQuoteAttributes" value="Y" checked="checked" />
        ${uiLabelMap.OrderOrderQuoteCoefficients}&nbsp;<input type="checkbox" name="copyQuoteCoefficients" value="Y" checked="checked" />
        ${uiLabelMap.OrderOrderQuoteTerms}&nbsp;<input type="checkbox" name="copyQuoteTerms" value="Y" checked="checked" />
    </div>
    <input type="submit" class="smallSubmit" value="${uiLabelMap.CommonCopy}"/>
</form>
</#if>