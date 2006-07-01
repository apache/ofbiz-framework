<?xml version="1.0" encoding="UTF-8"?>
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
        <ContentAssoc contentId="${contentId}" contentIdTo="CNTWIDGETS" contentAssocTypeId="SUBSITE" fromDate="${nowStamp?string("yyyy-MM-dd HH:mm:ss")}"/>
    </#if>
</#macro>

<#macro @element>
</#macro>
