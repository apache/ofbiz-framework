<#ftl ns_prefixes={"ask":"http://www.automationgroups.com/dtd/ask/"}> 
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

<#recurse doc>


<#macro "ask:document">
  <#recurse .node>
</#macro>

<#macro "ask:title">
<h2 class="head1">${.node}</h2>
<p/>
</#macro>

<#macro "ask:body">
  <#recurse .node>
</#macro>

<#macro "ask:section">
  <#list .node?children as kid>
    <#if kid?node_name == "sectionTitle">
      <h3 class="head2"> <#recurse kid> </h3>
    <#else>
      ${kid.@@markup}
    </#if>
  </#list>
</#macro>

<#macro "ask:sectionTitle">
</#macro>

<#macro @text>
${.node?html}
</#macro>
<#--
<#macro content>
    <#assign contentId="ECMC" + .node.@id[0]/>
    <DataResource dataResourceId="${contentId}" dataResourceTypeId="ELECTRONIC_TEXT" dataTemplateTypeId="NONE" dataSourceId="ECM" mimeTypeId="text/html"/>
    <ElectronicText dataResourceId="${contentId}">
<textData><![CDATA[
${.node.content_text}
]]></textData>
    </ElectronicText>
    <Content contentId="${contentId}" contentTypeId="DOCUMENT" dataResourceId="${contentId}" contentName="${.node.@name[0]}" description="${.node.content_desc?html}" mimeTypeId="text/xml" templateDataResourceId="TPL_XML_MB"/>
</#macro>
-->

<#macro @element>
</#macro>
