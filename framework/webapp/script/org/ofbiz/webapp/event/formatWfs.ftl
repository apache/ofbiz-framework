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

<?xml version="1.0" ?>
<wfs:FeatureCollection
    xmlns="http://www.hotwaxmedia.com/granite"
    xmlns:wfs="http://www.opengis.net/wfs"
    xmlns:gml="http://www.opengis.net/gml"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    >

<#list entityList as entity>
  <gml:featureMember>
      <#assign entityName=entity.getEntityName()/>
      <${entityName} fid="E${entity_index}">
      <#assign keyMap=entity.getAllFields()/>
      <#assign keys=keyMap.keySet()/>
      <#assign thisLongitude=""/>
      <#assign thisLatitude=""/>
      <#list keys as key>
        <#assign val = (entity.get(key))?default("")/>
        <#if key=="longitude"><#assign thisLongitude=val/></#if>
        <#if key == "latitude"><#assign thisLatitude=val/></#if>
        <#if key=="longitude" || key == "latitude">
            <#if thisLongitude?has_content && thisLatitude?has_content>
                <geoTemp>
                    <gml:Point srsName="4326">
                        <gml:pos>${thisLongitude} ${thisLatitude}</gml:pos>
                    </gml:Point>
                </geoTemp>
            </#if>
        <#else>
        <${key}>${val}</${key}>
        </#if>
      </#list>
      </${entityName}>
  </gml:featureMember>
</#list>
</wfs:FeatureCollection>
