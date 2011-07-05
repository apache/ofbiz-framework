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
<html xmlns="http://www.w3.org/1999/xhtml">
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
        <title>${title}</title>
    </head>
    <body>
        <p>Hello ${person.firstName?if_exists} ${person.lastName?if_exists},</p>
        <p>Your backlog <b>${custRequest.description?if_exists} [${custRequest.custRequestId}]</b> 
        <#if informationMap.workEffortId?has_content>has been added to sprint <b>${informationMap.workEffortName?if_exists} [${informationMap.workEffortId?if_exists}]</b></#if><br /> 
            <#if informationMap.productId?has_content>The related product: <b>${informationMap.internalName?if_exists} [${informationMap.productId?if_exists}]</#if></b>.
        <br />
        <br />
        <#if custRequest.fromPartyId == partyIdTo>
        The complete information about this sprint can be found <a href="${StringUtil.wrapString(baseSecureUrl?if_exists)}/scrum/control/ViewProdBacklogItem?custRequestId=${custRequest.custRequestId?if_exists}">here.....</a>
        <br /><br />
        </#if>
        Regards.<br /><br />
        Thank you for your business.
    </body>
</html>
