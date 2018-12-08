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
    <#if "GOOGLE" == geoChart.dataSourceId?substring(geoChart.dataSourceId?length-6 , geoChart.dataSourceId?length)>
    <div id="${id}"
         style="border:1px solid #979797; background-color:#e5e3df; width:${geoChart.width}; height:${geoChart.height}; margin:2em auto;">
        <div style="padding:1em; color:gray;">${uiLabelMap.CommonLoading}</div>
    </div>
    <script src="https://maps.googleapis.com/maps/api/js?sensor=false" type="text/javascript"></script>
    </#if>

  <#-- ========================== Here we go with different types of maps renderer ===========================-->
    <#if "GEOPT_GOOGLE" == geoChart.dataSourceId>
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
    <#elseif  "GEOPT_YAHOO" == geoChart.dataSourceId>
    <#elseif  "GEOPT_MICROSOFT" == geoChart.dataSourceId>
    <#elseif  "GEOPT_MAPTP" == geoChart.dataSourceId>
    <#elseif  "GEOPT_ADDRESS_GOOGLE" == geoChart.dataSourceId>
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
    <#elseif "GEOPT_OSM" == geoChart.dataSourceId>
<div id="${id}" class="map" style="border:1px solid #979797; background-color:#e5e3df; width:${geoChart.width}; height:${geoChart.height}; margin:2em auto;"></div>
    <script type="application/javascript">
        var iconFeatures=[];

        <#if geoChart.points?has_content>
            <#list geoChart.points as point>
            iconFeatures.push(
                    new ol.Feature({
                        geometry: new ol.geom.Point(ol.proj.transform([${point.lon},${point.lat}],
                                'EPSG:4326', 'EPSG:900913'))
                    })
            );
            </#list>
        </#if>

        var vectorSource = new ol.source.Vector({
            features: iconFeatures
        });

        var iconStyle = new ol.style.Style({
            image: new ol.style.Icon(({
                anchor: [0.5, 25],
                anchorXUnits: 'fraction',
                anchorYUnits: 'pixels',
                opacity: 0.75,
                src: '<@ofbizContentUrl>/images/img/marker.png</@ofbizContentUrl>'
            }))
        });

        var vectorLayer = new ol.layer.Vector({
            source: vectorSource,
            style: iconStyle
        });

        var map = new ol.Map({
            target: '${id}',
            layers: [
                new ol.layer.Tile({
                    source: new ol.source.OSM()
                }),
                vectorLayer
            ],
            view: new ol.View({
                center: ol.proj.fromLonLat([${center.lon}, ${center.lat}]),
                zoom: ${zoom}
            }),
            controls: [
                new ol.control.Zoom(),
                new ol.control.ZoomSlider(),
                new ol.control.ZoomToExtent(),
                new ol.control.OverviewMap(),
                new ol.control.ScaleLine(),
                new ol.control.FullScreen()
            ]
        });

        // fit to show all markers (optional)
        map.getView().fit(vectorSource.getExtent(), map.getSize());
    </script>
    </#if>
  </#if>
<#else>
  <h2>${uiLabelMap.CommonNoGeolocationAvailable}</h2>
</#if>
