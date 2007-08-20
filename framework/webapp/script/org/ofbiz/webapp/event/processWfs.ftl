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
