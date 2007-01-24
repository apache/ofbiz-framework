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
<#assign forumRootId = "WebStoreCONTENT"/>

<#-- variable setup and worker calls -->
<#assign curCategoryId = requestAttributes.curCategoryId?if_exists>
<#assign forumTrailCsv=requestParameters.forumTrailCsv?if_exists/>
<#assign forumTrail=[]/>
<#assign firstContentId=""/>
<#if forumTrailCsv?has_content>
  <#assign forumTrail=Static["org.ofbiz.base.util.StringUtil"].split(forumTrailCsv, ",") />
  <#if 0 < forumTrail?size>
    <#assign firstContentId=forumTrail[0]?string/>
  </#if>
</#if>

<div class="screenlet">
    <div class="screenlet-header">
        <div class="boxhead">${uiLabelMap.ProductBrowseContent}</div>
    </div>
    <div class="screenlet-body">
        <#assign count_1=0/>
        <@loopSubContentCache subContentId=forumRootId viewIndex=0 viewSize=9999 orderBy="contentName" contentAssocTypeId="SUBSITE">
            <div class="browsecategorytext" style="margin-left: 10px">
               -&nbsp;<a href="<@ofbizUrl>showcontenttree?contentId=${subContentId}&amp;nodeTrailCsv=${subContentId}</@ofbizUrl>" class="browsecategorybutton">${content.contentName}</a>
            </div>
            <#assign count_1=(count_1 + 1)/>
        </@loopSubContentCache>
    </div>
</div>
