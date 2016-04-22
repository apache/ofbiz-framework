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

<#include "component://common/webcommon/includes/GoogleGeoLocation.ftl"/>

<body onload="load()">
    <center>
        <div align="center" id="map" style="border:1px solid #979797; background-color:#e5e3df; width:500px; height:450px; margin:2em auto;"><br/></div>
        <form action="#" onsubmit="showAddress(this.address.value); return false">
            <input type="text" size="50" name="address"/>
            <input type="submit" value="${uiLabelMap.CommonSearch}"/>
        </form>
        <br/><br/>
        <form id="updateMapForm" method="post" action="<@ofbizUrl>editGeoLocation</@ofbizUrl>">
            <input type="hidden" name="partyId" value="${partyId!}"/>
            <input type="hidden" name="geoPointId" value="${geoPointId!}"/>
            <input type="hidden" name="lat" id="lat"/>
            <input type="hidden" name="lng" id="lng"/>
            <input type="submit" id="createMapButton" class="smallSubmit" value="${uiLabelMap.CommonSubmit}">
        </form>
        <br/><br/><br/>
    </center>
</body>
