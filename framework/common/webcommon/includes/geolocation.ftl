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
<#if geoChart?has_content>
    <#if geoChart.dataSourceId?has_content>
      <#if geoChart.dataSourceId == "GEOPT_GOOGLE">
        <div id="<#if geoChart.id?has_content>${geoChart.id}<#else>map_canvas</#if>" style="border:1px solid #979797; background-color:#e5e3df; width:${geoChart.width}; height:${geoChart.height}; margin:2em auto;">
          <div style="padding:1em; color:gray;">${uiLabelMap.CommonLoading}</div>
        </div>
        <#assign defaultUrl = "https." + request.getServerName()>
        <#assign defaultGogleMapKey = Static["org.ofbiz.base.util.UtilProperties"].getPropertyValue("general.properties", defaultUrl)>
        <script src="http://maps.google.com/maps?file=api&amp;v=2&amp;key=${defaultGogleMapKey}" type="text/javascript"></script>
        <script type="text/javascript"><!--
          if (GBrowserIsCompatible()) {
            var map = new GMap2(document.getElementById("<#if geoChart.id?has_content>${geoChart.id}<#else>map_canvas</#if>"));
            <#if geoChart.center?has_content>
              map.setCenter(new GLatLng(${geoChart.center.lat?c}, ${geoChart.center.lon?c}), ${geoChart.center.zoom});
            <#else>
              <#if geoChart.points?has_content>
                var latlng = [
                <#list geoChart.points as point>
                  new GLatLng(${point.lat?c}, ${point.lon?c})<#if point_has_next>,</#if>
                </#list>
                ];
                var latlngbounds = new GLatLngBounds();
                for (var i = 0; i < latlng.length; i++) {
                  latlngbounds.extend(latlng[i]);
                }
                map.setCenter(latlngbounds.getCenter(), Math.min (15, map.getBoundsZoomLevel(latlngbounds)));//reduce bounds zoom level to see all markers
              <#else>
                map.setCenter(new GLatLng(0, 0), 1);
                map.setZoom(15); // 0=World, 19=max zoom in
              </#if>
            </#if>
            <#if geoChart.controlUI?has_content && geoChart.controlUI == "small">
              map.addControl(new GSmallMapControl());
            <#else>
              map.setUIToDefault();
            </#if>
            <#if geoChart.points?has_content>
                <#list geoChart.points as point>
                  var marker_${point_index} = new GMarker(new GLatLng(${point.lat?c}, ${point.lon?c}));
                  map.addOverlay(marker_${point_index});
                  //map.addOverlay(new GMarker(new GLatLng(${point.lat?c}, ${point.lon?c})));
                  <#if point.link?has_content>
                      GEvent.addListener(marker_${point_index}, "click", function() {
                          marker_${point_index}.openInfoWindowHtml("<div style=\"width:210px; padding-right:10px;\"><a href=${point.link.url}>${point.link.label}</a></div>");
                      });
                  </#if>
                </#list>
            </#if>
          }
       --></script>
      <#elseif  geoChart.dataSourceId == "GEOPT_YAHOO">
      <#elseif  geoChart.dataSourceId == "GEOPT_MICROSOFT">
      <#elseif  geoChart.dataSourceId == "GEOPT_MAPTP">
      <#elseif  geoChart.dataSourceId == "GEOPT_ADDRESS_GOOGLE">
        <div id="<#if geoChart.id?has_content>${geoChart.id}<#else>map_canvas</#if>" style="border:1px solid #979797; background-color:#e5e3df; width:${geoChart.width}px; height:${geoChart.height}px; margin:2em auto;">
          <div style="padding:1em; color:gray;">${uiLabelMap.CommonLoading}</div>
        </div>
        <#assign defaultUrl = "https." + request.getServerName()>
        <#assign defaultGogleMapKey = Static["org.ofbiz.base.util.UtilProperties"].getPropertyValue("general.properties", defaultUrl)>
        <script src="http://maps.google.com/maps?file=api&amp;v=2&amp;key=${defaultGogleMapKey}" type="text/javascript"></script>
        <script type="text/javascript"><!--
          if (GBrowserIsCompatible()) {
            var geocoder = new GClientGeocoder();
            var map = new GMap2(document.getElementById("<#if geoChart.id?has_content>${geoChart.id}<#else>map_canvas</#if>"));
            geocoder.getLatLng("${pointAddress}", function(point) {
              if (!point) { showErrorAlert("${uiLabelMap.CommonErrorMessage2}","${uiLabelMap.CommonAddressNotFound}");}
              map.setUIToDefault();
              map.setCenter(point, 13);
              map.addOverlay(new GMarker(point));
              map.setZoom(15); // 0=World, 19=max zoom in
            });
          }
        --></script>
      <#elseif geoChart.dataSourceId == "GEOPT_OSM">
        <div id="<#if geoChart.id?has_content>${geoChart.id}<#else>map_canvas</#if>" style="border:1px solid #979797; background-color:#e5e3df; width:${geoChart.width}; height:${geoChart.height}; margin:2em auto;">
        </div>
        <script src="http://www.openlayers.org/api/OpenLayers.js"></script>
        <script>
          map = new OpenLayers.Map("<#if geoChart.id?has_content>${geoChart.id}<#else>map_canvas</#if>");
          map.addLayer(new OpenLayers.Layer.OSM());
          <#if geoChart.center?has_content>
            var zoom = ${geoChart.center.zoom};
            var center= new OpenLayers.LonLat(${geoChart.center.lon?c},${geoChart.center.lat?c})
              .transform(new OpenLayers.Projection("EPSG:4326"), // transform from WGS 1984
                         map.getProjectionObject() // to Spherical Mercator Projection
                         );
          </#if>
          var markers = new OpenLayers.Layer.Markers( "Markers" );
          map.addLayer(markers);
          <#if geoChart.points?has_content>
            <#list geoChart.points as point>
              markers.addMarker(new OpenLayers.Marker(new OpenLayers.LonLat(${point.lon?c} ,${point.lat?c}).transform(
                new OpenLayers.Projection("EPSG:4326"), map.getProjectionObject())));
            </#list>
          </#if>
          map.setCenter(center, zoom);
          map.setZoom(15); // 0=World, 19=max zoom in
        </script>
      </#if>
    </#if>
<#else>
  <h2>${uiLabelMap.CommonNoGeolocationAvailable}</h2>
</#if>
