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
<#-- ================================= Golbal Init ======================================-->
  <#if geoChart.id?has_content>
    <#assign id = geoChart.id>
  <#else>
    <#assign id = "map_canvas">
  </#if>

  <#if geoChart.center?has_content>
    <#assign center = geoChart.center>
    <#assign zoom = geoChart.center.zoom>
  <#elseif geoChart.points?has_content>
    <#assign center = geoChart.points[0]>
    <#assign zoom = 15> <#-- 0=World, 19=max zoom in -->
  <#else>
  <#-- hardcoded in GEOPT_ADDRESS_GOOGLE, simpler -->
  </#if>

<#-- ================================= Google Maps Init ======================================-->
  <#if geoChart.dataSourceId?has_content>
    <#if geoChart.dataSourceId?substring(geoChart.dataSourceId?length-6 , geoChart.dataSourceId?length) == "GOOGLE">
    <div id="${id}"
         style="border:1px solid #979797; background-color:#e5e3df; width:${geoChart.width}; height:${geoChart.height}; margin:2em auto;">
        <div style="padding:1em; color:gray;">${uiLabelMap.CommonLoading}</div>
    </div>
    <script src="https://maps.googleapis.com/maps/api/js?sensor=false" type="text/javascript"></script>
    </#if>

  <#-- ========================== Here we go with different types of maps renderer ===========================-->
    <#if geoChart.dataSourceId == "GEOPT_GOOGLE">
    <script type="text/javascript">
        function showAllMarkers(map, points) {
            if (points.length > 1) {
                var latlngbounds = new google.maps.LatLngBounds();
                for (var i = 0; i < latlngs.length; i++) {
                    latlngbounds.extend(latlngs[i]);
                }
                map.fitBounds(latlngbounds);
            }
        }

        var map = new google.maps.Map(document.getElementById("${id}"),
              <#if geoChart.points?has_content>
              {
                  center: new google.maps.LatLng(${center.lat}, ${center.lon}),
                  zoom: ${zoom},
                  mapTypeId: google.maps.MapTypeId.ROADMAP
        });
        <#list geoChart.points as point>
            var marker_${point_index} = new google.maps.Marker({
                  position: new google.maps.LatLng(${point.lat}, ${point.lon}),
                  map: map
            });
            <#if point.link?has_content>
                var infoWindow = new google.maps.InfoWindow();
                google.maps.event.addListener(marker_${point_index}, "click", function () {
                      infoWindow.setContent((
                              "<div style=\"width:210px; padding-right:10px;\"><a href=${point.link.url}>${point.link.label}</a></div>"));
                      infoWindow.setPosition(marker_${point_index}.getPosition());
                      infoWindow.open(map);
                });
            </#if>
        </#list>
        var latlngs = [
              <#list geoChart.points as point>
                  new google.maps.LatLng(${point.lat}, ${point.lon})<#if point_has_next>,</#if>
              </#list>];
        showAllMarkers(map, latlngs);
              </#if>
    </script>
    <#elseif  geoChart.dataSourceId == "GEOPT_YAHOO">
    <#elseif  geoChart.dataSourceId == "GEOPT_MICROSOFT">
    <#elseif  geoChart.dataSourceId == "GEOPT_MAPTP">
    <#elseif  geoChart.dataSourceId == "GEOPT_ADDRESS_GOOGLE">
    <script type="text/javascript">
        var geocoder = new google.maps.Geocoder();
        var map = new google.maps.Map(document.getElementById("${id}"),
                {
                    center: new google.maps.LatLng(38, 15),
                    zoom: 15, // 0=World, 19=max zoom in
                    mapTypeId: google.maps.MapTypeId.ROADMAP
                });
        geocoder.geocode({'address': "${pointAddress}"}, function (result, status) {
              if (status != google.maps.GeocoderStatus.OK) {
                  showErrorAlert("${uiLabelMap.CommonErrorMessage2}", "${uiLabelMap.CommonAddressNotFound}");
              } else {
                  var position = result[0].geometry.location;
                  map.setCenter(position);
                  map.fitBounds(result[0].geometry.viewport);
                  var marker = new google.maps.Marker({
                      position: position,
                      map: map
                  });
              }
        });
    </script>
    <#elseif geoChart.dataSourceId == "GEOPT_OSM">
    <div id="${id}" style="border:1px solid #979797; background-color:#e5e3df; width:${geoChart.width}; height:${geoChart.height}; margin:2em auto;"></div>
    <#--
    due to https://github.com/openlayers/openlayers/issues/1025
    rather use a local version loaded by framework/common/widget/CommonScreens.xml -->
    <#-- script src="//www.openlayers.org/api/OpenLayers.js"></script-->
    <script type="text/javascript">
        map = new OpenLayers.Map("${id}");
        map.addLayer(new OpenLayers.Layer.OSM());
        var zoom = ${zoom};
        var center = new OpenLayers.LonLat(${center.lon},${center.lat})
              .transform(new OpenLayers.Projection("EPSG:4326"), // transform from WGS 1984
                      map.getProjectionObject() // to Spherical Mercator Projection
        );
        var markers = new OpenLayers.Layer.Markers("Markers");
        map.addLayer(markers);
        <#if geoChart.points?has_content>
          <#list geoChart.points as point>
              markers.addMarker(new OpenLayers.Marker(new OpenLayers.LonLat(${point.lon},${point.lat}).transform(
                    new OpenLayers.Projection("EPSG:4326"), map.getProjectionObject())));
          </#list>
        </#if>
        map.addControl(new OpenLayers.Control.PanZoomBar());
        map.addControl(new OpenLayers.Control.NavToolbar());

        map.setCenter(center, zoom);
        var newBound = markers.getDataExtent();
        map.zoomToExtent(newBound);
    </script>
    </#if>
  </#if>
<#else>
  <h2>${uiLabelMap.CommonNoGeolocationAvailable}</h2>
</#if>
