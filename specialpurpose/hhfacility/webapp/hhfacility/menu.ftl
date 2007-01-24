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

<#assign facility = parameters.facility>
<span class="tabletext">
<ol>
<li><a accesskey="1" href="<@ofbizUrl>/receipt?facilityId=${facility.facilityId?if_exists}</@ofbizUrl>">Goods Receipt</a></li>
<li><a accesskey="2" href="<@ofbizUrl>/movement?facilityId=${facility.facilityId?if_exists}</@ofbizUrl>">Inventory Movement</a></li>
<li><a accesskey="3" href="<@ofbizUrl>/picking?facilityId=${facility.facilityId?if_exists}</@ofbizUrl>">Picking</a></li>
<li><a accesskey="4" href="<@ofbizUrl>/packing?facilityId=${facility.facilityId?if_exists}</@ofbizUrl>">Packing</a></li>
<li><a accesskey="5" href="<@ofbizUrl>/stocktake?facilityId=${facility.facilityId?if_exists}</@ofbizUrl>">Stocktake</a></li>
</ol>
</span>