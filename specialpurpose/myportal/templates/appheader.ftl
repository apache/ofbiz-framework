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

<#assign selected = headerItem?default("void")>
<div id="app-navigation">
  <h2>My Personal Page</h2>
  <ul>
    <#if userLogin?has_content>  
        <#if getMainPage?has_content>
    	    <#list getMainPage as page>
           		<li<#if selected = "${page.portalPageId}"> class="selected"</#if>><a href="<@ofbizUrl>dashboardExample?portalPageId=${page.portalPageId}</@ofbizUrl>">${page.portalName}</a></li>
        	</#list>
    		<#else>
    			<#list getNA as page>
           			<li<#if selected = "${page.portalPageId}"> class="selected"</#if>><a href="<@ofbizUrl>dashboardExample?portalPageId=${page.portalPageId}</@ofbizUrl>">${page.portalName}</a></li>
        		</#list>
    	</#if>    		  	
    	<#if pages?has_content>
        	<#list pages as page>
            	<li<#if selected = "${page.portalPageId}"> class="selected"</#if>><a href="<@ofbizUrl>dashboardExample?portalPageId=${page.portalPageId}</@ofbizUrl>">${page.portalName}</a></li>
        	</#list>
        </#if>
        <li class="opposed"><a href="<@ofbizUrl>logout</@ofbizUrl>">${uiLabelMap.CommonLogout}</a></li>
        <#--li class="opposed"><a href="http://docs.ofbiz.org/display/OFBENDUSER/My+Page?decorator=printable" target="_blank">${uiLabelMap.CommonHelp}</a></li>-->      
        <li class="opposed"><a href="http://docs.ofbiz.org/display/OFBENDUSER/My+Page?decorator=printable" url-mode="plain" target-window="new">${uiLabelMap.CommonHelp}</a></li>
    	<li class="opposed"><a href="<@ofbizUrl>ManagePortalPages?originalPortalPageId=${originalPortalPageId}&amp;mainPortalPageId=MAINMYPORTAL</@ofbizUrl>">${uiLabelMap.CommonPreferences}</a></li>
    <#else>
        <li class="opposed"><a href="<@ofbizUrl>${checkLoginUrl?if_exists}</@ofbizUrl>">${uiLabelMap.CommonLogin}</a></li>
    </#if>
  </ul>
  <br class="clear" />
</div>