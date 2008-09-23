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
            <a href="<@ofbizUrl>main</@ofbizUrl>" class="posButton">${uiLabelMap.WebPosButtonMain}</a>
        </td>
        <td>
            &nbsp;
        </td>
        <td>
            <#if isManagerLoggedIn?default(false) == true>
                <a href="<@ofbizUrl>Manager</@ofbizUrl>" class="posButton">${uiLabelMap.WebPosButtonManager}</a>
            <#else>
                <span class="disabled">${uiLabelMap.WebPosButtonManager}</span>
            </#if>
        </td>
    </tr>
    <tr>
        <td>
            <a href="<@ofbizUrl>Promo</@ofbizUrl>" class="posButton">${uiLabelMap.WebPosButtonPromo}</a>
        </td>
        <td>
            &nbsp;
        </td>
        <td>
            <#if (shoppingCartSize > 0)>
                <a href="<@ofbizUrl>Payment</@ofbizUrl>" class="posButton">${uiLabelMap.WebPosButtonPayment}</a>
            <#else>
                <span class="disabled">${uiLabelMap.WebPosButtonPayment}</span>
            </#if>
        </td>
    </tr>
    <tr>
        <td>
            <#if (shoppingCartSize > 0)>
                <a href="javascript:document.cartform.submit();" class="posButton">${uiLabelMap.WebPosRecalculateCart}</a>                
            <#else>
                <span class="disabled">${uiLabelMap.WebPosRecalculateCart}</span>
            </#if>
        </td>
        <td>
            <#if (shoppingCartSize > 0)>
                <a href="<@ofbizUrl>EmptyCart</@ofbizUrl>" class="posButton">${uiLabelMap.WebPosEmptyCart}</a>                
            <#else>
                <span class="disabled">${uiLabelMap.WebPosEmptyCart}</span>
            </#if>
        </td>
        <td>
            <#if (shoppingCartSize > 0)>
                <a href="javascript:removeSelected();" class="posButton">${uiLabelMap.WebPosRemoveSelected}</a>
            <#else>                
                <span class="disabled">${uiLabelMap.WebPosRemoveSelected}</span>
            </#if>
        </td>
    </tr>
    <tr>
        <td>
            <a href="<@ofbizUrl>AddItem?add_product_id=GZ-1001&quantity=1</@ofbizUrl>" class="posButton">NAN GIZMO</a>
        </td>
        <td>
            <a href="<@ofbizUrl>AddItem?add_product_id=GZ-5005&quantity=1</@ofbizUrl>" class="posButton">PURPLE GIZMO</a>
        </td>
        <td>
            <a href="<@ofbizUrl>AddItem?add_product_id=GZ-2644&quantity=1</@ofbizUrl>" class="posButton">ROUND GIZMO</a>
        </td>
    </tr>
    <tr>
        <td>
            <a href="<@ofbizUrl>AddItem?add_product_id=GZ-2002&quantity=1</@ofbizUrl>" class="posButton">SQUARE GIZMO</a>
        </td>
        <td>
            <a href="<@ofbizUrl>AddItem?add_product_id=GZ-7000&quantity=1</@ofbizUrl>" class="posButton">MASSIVE GIZMO</a>
        </td>
        <td>
            <a href="<@ofbizUrl>AddItem?add_product_id=WG-5569&quantity=1</@ofbizUrl>" class="posButton">TINY WIDGET</a>
        </td>
    </tr>
    <tr>
        <td>
            <a href="<@ofbizUrl>AddItem?add_product_id=GZ-1004&quantity=1</@ofbizUrl>" class="posButton">RAINBOW GIZMO</a>
        </td>
        <td>
            <a href="<@ofbizUrl>AddItem?add_product_id=GZ-1005&quantity=1</@ofbizUrl>" class="posButton">NIT GIZMO</a>
        </td>
        <td>
            <a href="<@ofbizUrl>AddItem?add_product_id=GZ-8544&quantity=1</@ofbizUrl>" class="posButton">BIG GIZMO</a>
        </td>
    </tr>
    <tr>
        <td>
            <a href="<@ofbizUrl>AddItem?add_product_id=WG-1111&quantity=1</@ofbizUrl>" class="posButton">MICRO WIDGET</a>
        </td>
        <td>
            &nbsp;
        </td>
        <td>
            <#if userLogin?has_content && userLogin.userLoginId != "anonymous">
                <a href="<@ofbizUrl>Logout</@ofbizUrl>" class="posButton">${uiLabelMap.WebPosButtonLogout}</a>
            <#else/>
                <a href="<@ofbizUrl>${checkLoginUrl}</@ofbizUrl>" class="posButton">${uiLabelMap.WebPosButtonLogin}</a>
            </#if>
        </td>
    </tr>
</table>