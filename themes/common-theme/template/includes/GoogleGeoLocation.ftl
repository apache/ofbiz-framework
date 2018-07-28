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

<script src="https://maps.googleapis.com/maps/api/js?sensor=false" type="application/javascript"></script>
<script type="application/javascript">
    function load() {
        var geocoder = new google.maps.Geocoder();
        var center = new google.maps.LatLng(${latitude!38}, ${longitude!15});
        var map = new google.maps.Map(document.getElementById("map"),
                {
                    center: center,
                    zoom: 15, // 0=World, 19=max zoom in
                    mapTypeId: google.maps.MapTypeId.ROADMAP
                });

        var marker = new google.maps.Marker({
            position: center,
            map: map,
            draggable: true
        });

        document.getElementById("lat").value = center.lat().toFixed(5);
        document.getElementById("lng").value = center.lng().toFixed(5);

        google.maps.event.addListener(marker, "dragend", function () {
            var point = marker.getPosition();
            map.panTo(point);
            document.getElementById("lat").value = point.lat().toFixed(5);
            document.getElementById("lng").value = point.lng().toFixed(5);
        });


        google.maps.event.addListener(map, "moveend", function () {
            map.clearOverlays();
            var center = map.getCenter();
            var marker = new GMarker(center, {draggable: true});
            map.addOverlay(marker);
            document.getElementById("lat").value = center.lat().toFixed(5);
            document.getElementById("lng").value = center.lng().toFixed(5);
        });

        google.maps.event.addListener(marker, "dragend", function () {
            var point = marker.getPoint();
            map.panTo(point);
            document.getElementById("lat").value = point.lat().toFixed(5);
            document.getElementById("lng").value = point.lng().toFixed(5);
        });
    }

    function showAddress(address) {
        var map = new google.maps.Map(document.getElementById("map"),
                {
                    center: new google.maps.LatLng(${latitude!38}, ${longitude!15}),
                    zoom: 15, // 0=World, 19=max zoom in
                    mapTypeId: google.maps.MapTypeId.ROADMAP
                });
        var geocoder = new google.maps.Geocoder();
        if (geocoder) {
            geocoder.geocode({'address': address}, function (result, status) {
                if (status != google.maps.GeocoderStatus.OK) {
                    showErrorAlert("${uiLabelMap.CommonErrorMessage2}", "${uiLabelMap.CommonAddressNotFound}");
                } else {
                    var point = result[0].geometry.location;
                    var lat = point.lat().toFixed(5);
                    var lng = point.lng().toFixed(5);
                    document.getElementById("lat").value = lat;
                    document.getElementById("lng").value = lng;
                    //map.clearOverlays()
                    map.setCenter(point, 14);

                    var marker = new google.maps.Marker({
                        position: new google.maps.LatLng(lat, lng),
                        map: map,
                        draggable: true
                    });

                    google.maps.event.addListener(marker, "dragend", function () {
                        var point = marker.getPosition();
                        map.panTo(point);
                        document.getElementById("lat").value = point.lat().toFixed(5);
                        document.getElementById("lng").value = point.lng().toFixed(5);
                    });

                    google.maps.event.addListener(map, "moveend", function () {
                        //map.clearOverlays();
                        var center = map.getCenter();
                        var marker = new google.maps.Marker(center, {draggable: true});
                        map.addOverlay(marker);
                        document.getElementById("lat").value = center.lat().toFixed(5);
                        document.getElementById("lng").value = center.lng().toFixed(5);
                    });

                    google.maps.event.addListener(marker, "dragend", function () {
                        var pt = marker.getPoint();
                        map.panTo(pt);
                        document.getElementById("lat").value = pt.lat().toFixed(5);
                        document.getElementById("lng").value = pt.lng().toFixed(5);
                    });
                }
            });
        }
    }
</script>
