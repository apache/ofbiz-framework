<#ftl ns_prefixes={
    "D":"http://docbook.org/ns/docbook",
    "xl":"http://www.w3.org/1999/xlink"
    }>  
<#--^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^ must be at the top of the file........-->
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
<style>
<#if layoutSettings.VT_NAME[0] == "BIZZNESS_TIME">   
body{background:none;}
.left-border{float:left;width:25%;}
.contentarea{margin: 0 0 0 0.5em;padding:0 0 0 0.5em;}
.leftonly{float:none;min-height:25em;}
</#if>
</style>
<#macro para para>
<p>
  <#list para?children as child>
    <#if child?node_type = "text">
  ${child}
    <#elseif child?node_type = 'element' && child?node_name = "link">
  <a href="${child["@xl:href"]}">${child}</a>
    </#if>
  </#list>
  <br/><br/>            
</p>
</#macro>

<#macro section inSection first="no">
  <#list inSection.* as subSection>
    <#if subSection?node_name = "title">
      <#if first = "yes"> 
        <h1>${subSection}</h1>
      <#else>
        <h2>${subSection}</h2>
      </#if>
    <#elseif subSection?node_name = "para">
        <@para para=subSection/>
    <#elseif subSection?node_name = "section">
        <@section inSection=subSection/>
    </#if>
  </#list>
  <br/><br/>
</#macro>
<#if layoutSettings.VT_NAME[0] == "FLAT_GREY">
<#------------------------------------------->
<div class="contentarea">
  <div id="column-container">
    <div id="content-main-section">
    <@section inSection=doc.section first="yes"/>
    </div>
  </div>
</div>
<#elseif layoutSettings.VT_NAME[0] == "BIZZNESS_TIME">   
		<@section inSection=doc.section first="yes"/>
<#else><#-- other templates  -->          
<#----------------------------->
<div class="contentarea">
  <div id="column-container">
    <div id="content-main-section">
    <@section inSection=doc.section first="yes"/>
    </div>
  </div>
</div>

</#if>