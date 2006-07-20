<#--
Copyright 2001-2006 The Apache Software Foundation

Licensed under the Apache License, Version 2.0 (the "License"); you may not
use this file except in compliance with the License. You may obtain a copy of
the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
License for the specific language governing permissions and limitations
under the License.
-->
<#assign geoFindMap = Static["org.ofbiz.base.util.UtilMisc"].toMap("geoTypeId", "COUNTRY")>
<#assign geoOrderList = Static["org.ofbiz.base.util.UtilMisc"].toList("geoName")>
<#assign countries = delegator.findByAndCache("Geo", geoFindMap, geoOrderList)>
<#list countries as country>
    <option value='${country.geoId}'>${country.geoName?default(country.geoId)}</option>
</#list>
