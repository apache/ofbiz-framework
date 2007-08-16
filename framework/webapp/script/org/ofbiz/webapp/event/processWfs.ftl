<#ftl ns_prefixes={"ogc":"http://ogc.org"}>
    <simple-method method-name="processWfs" short-description="Process a WFS request and return a simple method" login-required="false">

<#if doc?node_name == "Filter">
  <#visit doc/>
<#else>
  <#recurse doc/>
</#if>

</simple-method>


<#macro @element></#macro>


<#macro "ogc:Filter">
		        <entity-condition list-name="entityList" entity-name="${entityName}" filter-by-date="false" use-cache="false">
  <#recurse .node>
                </entity-condition>
                <field-to-request field-name="entityList"/>
</#macro>

<#macro "ogc:PropertyIsEqualTo"><#assign propName=.node["ogc:PropertyName"].@@text />
  <condition-expr field-name="${propName}" value="<@getLiteral nd=.node["ogc:Literal"] nm=propName/>"/>

</#macro>

<#macro "ogc:PropertyName"></#macro>
<#macro "ogc:Literal"></#macro>
<#macro getPropertyName nd>${nd.@@text}</#macro>
<#macro getLiteral nd nm>${(paramMap.nm)?default(nd.@@text)}</#macro>
