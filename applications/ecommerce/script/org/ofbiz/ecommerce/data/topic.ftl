<?xml version="1.0" encoding="UTF-8"?>
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

<entity-engine-xml>
<#recurse doc>
</entity-engine-xml>

<#macro topics>
<#recurse .node>
</#macro>

<#macro topic>
    <#assign contentId="ECMT" + .node.@id[0]/>
    <Content contentId="${contentId}" contentTypeId="WEB_SITE_PUB_PT" contentName="${.node.topic_heading}" description="${.node.topic_desc?html}" ownerContentId=""/>
    <#assign internalName=.node.@name[0]/>
    <#assign internalNameParts=internalName?split(".")/>
    <#assign firstPart=internalNameParts[0] />
    <#assign nowStamp=Static["org.ofbiz.base.util.UtilDateTime"].nowTimestamp()/>
    <#if firstPart == "WIDGETS">
        <ContentAssoc contentId="CNTWIDGETS" contentIdTo="${contentId}" contentAssocTypeId="SUB_CONTENT" fromDate="${nowStamp?string("yyyy-MM-dd HH:mm:ss")}"/>
    </#if>
</#macro>

<#macro @element>
</#macro>
