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
        <p>Hello ${person.firstName!} ${person.lastName!},</p>
        <p>Your Sprint <b>${sprint.workEffortName!} [${sprint.workEffortId!}]</b> in project <b>${project.workEffortName!} [${project.workEffortId!}]</b> 
            of the product <b>${prodcut.internalName!} [${prodcut.productId!}]</b> has been started.
        <br />
        <br />
        The complete information about this sprint can be found <a href="${StringUtil.wrapString(baseSecureUrl!)}/scrum/control/ViewSprint?sprintId=${sprint.workEffortId!}">here.....</a>
        <br /><br />
        Regards.<br /><br />
        Thank you for your business.
    </body>
</html>
