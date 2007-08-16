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