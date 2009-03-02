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
            <#if isManagerLoggedIn?default(false) == true && isOpen?default(false) == false>
                <a href="<@ofbizUrl>ManagerOpenTerminal</@ofbizUrl>" class="posButton">${uiLabelMap.WebPosButtonOpenTerminal}</a>
            <#else>
                <span class="disabled">${uiLabelMap.WebPosButtonOpenTerminal}</span>
            </#if>
        </td>
        <td>
            <#if isManagerLoggedIn?default(false) == true && isOpen?default(false) == true>
                <a href="<@ofbizUrl>ManagerCloseTerminal</@ofbizUrl>" class="posButton">${uiLabelMap.WebPosButtonCloseTerminal}</a>
            <#else>
                <span class="disabled">${uiLabelMap.WebPosButtonCloseTerminal}</span>
            </#if>
        </td>
        <td>
            <#if isManagerLoggedIn?default(false) == true && isOpen?default(false) == true>
                <a href="<@ofbizUrl>ManagerVoidOrder</@ofbizUrl>" class="posButton">${uiLabelMap.WebPosButtonVoidOrder}</a>
            <#else>
                <span class="disabled">${uiLabelMap.WebPosButtonVoidOrder}</span>
            </#if>
        </td>
    </tr>
    <tr>
        <td>
            <#if isManagerLoggedIn?default(false) == true>
                <a href="<@ofbizUrl>Shutdown</@ofbizUrl>" class="posButton">${uiLabelMap.WebPosButtonShutdown}</a>
            <#else>
                <span class="disabled">${uiLabelMap.WebPosButtonShutdown}</span>
            </#if>
        </td>
        <td>
            <#if isManagerLoggedIn?default(false) == true && isOpen?default(false) == true>
                <a href="<@ofbizUrl>ManagerPaidOutAndIn?type=IN</@ofbizUrl>" class="posButton">${uiLabelMap.WebPosButtonPaidIn}</a>
            <#else>
                <span class="disabled">${uiLabelMap.WebPosButtonPaidIn}</span>
            </#if>
        </td>
        <td>
            <#if isManagerLoggedIn?default(false) == true && isOpen?default(false) == true>
                <a href="<@ofbizUrl>ManagerPaidOutAndIn?type=OUT</@ofbizUrl>" class="posButton">${uiLabelMap.WebPosButtonPaidOut}</a>
            <#else>
                <span class="disabled">${uiLabelMap.WebPosButtonPaidOut}</span>
            </#if>
        </td>
    </tr>
    <tr>
        <td>
            <a href="<@ofbizUrl>ManagerModifyPrice</@ofbizUrl>" class="posButton">${uiLabelMap.WebPosButtonModifyPrice}</a>
        </td>
        <td>
            <a href="<@ofbizUrl>main</@ofbizUrl>" class="posButton">${uiLabelMap.WebPosButtonMain}</a>
        </td>
        <td>
            &nbsp;
        </td>
    </tr>
</table>