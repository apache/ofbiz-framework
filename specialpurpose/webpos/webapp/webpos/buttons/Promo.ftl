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

<table class="tableButtons" cellspacing="5">
    <tr>
        <td>
            <#if isOpen?default(false) == true>
                <a href="<@ofbizUrl>AddPromoCode</@ofbizUrl>" class="posButton">${uiLabelMap.WebPosButtonPromoCode}</a>
            <#else>
                <span class="disabled">${uiLabelMap.WebPosButtonPromoCode}</span>
            </#if>
        </td>
        <td>
            <a href="<@ofbizUrl>main</@ofbizUrl>" class="posButton">${uiLabelMap.WebPosButtonMain}</a>
        </td>
        <td>
            <a href="<@ofbizUrl>main</@ofbizUrl>" class="posButton">&nbsp;</a>
        </td>
    </tr>
</table>