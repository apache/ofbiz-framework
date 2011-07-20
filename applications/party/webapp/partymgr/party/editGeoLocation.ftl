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

<#assign defaultUrl = "https." + request.getServerName()>
<#assign defaultGogleMapKey = Static["org.ofbiz.base.util.UtilProperties"].getPropertyValue("general.properties", defaultUrl)>
<script src="http://maps.google.com/maps?file=api&amp;v=2&amp;key=${defaultGogleMapKey}" type="text/javascript"></script>
<script type="text/javascript">
    function load() {
        if (GBrowserIsCompatible()) {
            var map = new GMap2(document.getElementById("map"));
            map.addControl(new GSmallMapControl());
            map.addControl(new GMapTypeControl());
            var center = new GLatLng(${latitude?if_exists}, ${longitude?if_exists});
            map.setCenter(center, 15);
            geocoder = new GClientGeocoder();
            var marker = new GMarker(center, {draggable: true});  
            map.addOverlay(marker);
            document.getElementById("lat").value = center.lat().toFixed(5);
            document.getElementById("lng").value = center.lng().toFixed(5);

            GEvent.addListener(marker, "dragend", function() {
                var point = marker.getPoint();
                map.panTo(point);
                document.getElementById("lat").value = point.lat().toFixed(5);
                document.getElementById("lng").value = point.lng().toFixed(5);
            });

            GEvent.addListener(map, "moveend", function() {
                map.clearOverlays();
                var center = map.getCenter();
                var marker = new GMarker(center, {draggable: true});
                map.addOverlay(marker);
                document.getElementById("lat").value = center.lat().toFixed(5);
                document.getElementById("lng").value = center.lng().toFixed(5);
    
            GEvent.addListener(marker, "dragend", function() {
                var point =marker.getPoint();
                map.panTo(point);
                document.getElementById("lat").value = point.lat().toFixed(5);
                document.getElementById("lng").value = point.lng().toFixed(5);
            });
            });
        }
    }

    function showAddress(address) {
        var map = new GMap2(document.getElementById("map"));
        map.addControl(new GSmallMapControl());
        map.addControl(new GMapTypeControl());
        if (geocoder) {
            geocoder.getLatLng(
            address,
            function(point) {
                if (!point) {
                    alert(address + " not found");
                } else {
                    document.getElementById("lat").value = point.lat().toFixed(5);
                    document.getElementById("lng").value = point.lng().toFixed(5);
                    map.clearOverlays()
                    map.setCenter(point, 14);
                    var marker = new GMarker(point, {draggable: true});  
                    map.addOverlay(marker);

                    GEvent.addListener(marker, "dragend", function() {
                        var pt = marker.getPoint();
                        map.panTo(pt);
                        document.getElementById("lat").value = pt.lat().toFixed(5);
                        document.getElementById("lng").value = pt.lng().toFixed(5);
                    });

                    GEvent.addListener(map, "moveend", function() {
                        map.clearOverlays();
                        var center = map.getCenter();
                        var marker = new GMarker(center, {draggable: true});
                        map.addOverlay(marker);
                        document.getElementById("lat").value = center.lat().toFixed(5);
                        document.getElementById("lng").value = center.lng().toFixed(5);

                    GEvent.addListener(marker, "dragend", function() {
                        var pt = marker.getPoint();
                        map.panTo(pt);
                        document.getElementById("lat").value = pt.lat().toFixed(5);
                        document.getElementById("lng").value = pt.lng().toFixed(5);
                    });
                    });
                }
            });
        }
    }
</script>

<body onload="load()" onunload="GUnload()" >
    <center>
        <div align="center" id="map" style="border:1px solid #979797; background-color:#e5e3df; width:500px; height:450px; margin:2em auto;"><br/></div>
        <form action="#" onsubmit="showAddress(this.address.value); return false">
            <input type="text" size="50" name="address"/>
            <input type="submit" value="Search"/>
        </form>
        <br/><br/>
        <form id="updateMapForm" method="post" action="<@ofbizUrl>editGeoLocation</@ofbizUrl>">
            <input type="hidden" name="partyId" value="${partyId?if_exists}"/>
            <input type="hidden" name="geoPointId" value="${geoPointId?if_exists}"/>
            <input type="hidden"  name="lat" id="lat"/>
            <input type="hidden" name="lng" id="lng"/>
            <input type="submit" id="createMapButton" class="smallSubmit" value="${uiLabelMap.CommonSubmit}">
        </form>
        <br/><br/><br/>
    </center>
</body>
