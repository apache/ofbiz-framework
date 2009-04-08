/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

// A basic Google Map function to render a marker centered on a map with little text(s) and optionnal link(s) within...

function loadGoogleMap(lat,lgt, link1, text1, link2, text2) {
    if (GBrowserIsCompatible()) {
        lat = lat.replace(",","."); // For decimals separator, in French for instance, please add more if needed
        lgt = lgt.replace(",","."); // For decimals separator, in French for instance, please add more if needed
        map = new GMap2(document.getElementById("map"));
        map.setCenter(new GLatLng(lat, lgt), 13);
        marker = new GMarker(new GLatLng(lat, lgt));
        map.addControl(new GSmallMapControl());
        map.addOverlay(marker);
        info1 = typeof(link1)!="undefined" && typeof(text1)!="undefined";
        info2 = typeof(link2)!="undefined" && typeof(text2)!="undefined";
        html = '<div style="width:210px; padding-right:10px;">';
        if (info1 || info2) {
            if (info1) {
                html = html + '<a href=' + link1 + '>' + text1 + '</a>';
            }
            if (info2) {
                html = html + '<a href=' + link2 + '>' + text2 + '</a>';
            }
            html = html + '.</div>';
        } else {
            html = html + 'No specific information.</div>';
        }
        marker.openInfoWindowHtml(html);
    }
}