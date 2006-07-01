<?xml version="1.0" encoding="UTF-8"?>
<entity-engine-xml>
<DataSource dataSourceId="ECM" dataSourceTypeId="CONTENT_CREATION" description="Ecommerce Content"/>
<#recurse doc>
</entity-engine-xml>

<#macro contents>
<#recurse .node>
</#macro>

<#macro content>
    <#assign contentId="ECMC" + .node.@id[0]/>
    <DataResource dataResourceId="${contentId}" dataResourceTypeId="ELECTRONIC_TEXT" dataTemplateTypeId="NONE" dataSourceId="ECM" mimeTypeId="text/xml"/>
    <ElectronicText dataResourceId="${contentId}">
<textData><![CDATA[
${.node.content_text}
]]></textData>
    </ElectronicText>
    <Content contentId="${contentId}" contentTypeId="DOCUMENT" dataResourceId="${contentId}" contentName="${.node.@name[0]}" description="${.node.content_desc?html}" mimeTypeId="text/html" templateDataResourceId="TPL_XML_MB"/>
</#macro>

<#macro @element>
</#macro>
