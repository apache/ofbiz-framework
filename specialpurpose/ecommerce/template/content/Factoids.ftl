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
<#assign factoidRootId = "WebStoreFACTOID"/>

<#-- variable setup and worker calls -->
<#assign curCategoryId = requestAttributes.curCategoryId!>
<#assign factoidTrailCsv = requestParameters.factoidTrailCsv!/>
<#assign factoidTrail=[]/>
<#assign firstContentId=""/>
<#if factoidTrailCsv?has_content>
  <#assign factoidTrail=Static["org.ofbiz.base.util.StringUtil"].split(factoidTrailCsv, ",") />
  <#if 0 < factoidTrail?size>
    <#assign firstContentId=factoidTrail[0]?string/>
  </#if>
</#if>

<div id="factoids" class="screenlet">
  <div class="screenlet-title-bar">
    <ul>
      <li class="h3">${uiLabelMap.EcommerceFactoids}</li>
    </ul>
    <br class="clear"/>
  </div>
  <div class="screenlet-body">
    <ul class="browsecategorylist">
      <#assign count_1=0/>
      <@limitedSubContent contentId=factoidRootId viewIndex=0 viewSize=9999 orderBy="contentName" limitSize="2">
        <li class="browsecategorytext">
          <@renderSubContentCache subContentId=subContentId/>
        </li>
        <#assign count_1=(count_1 + 1)/>
      </@limitedSubContent>
    </ul>
  </div>
</div>