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
<div align="center">
<h3>Welcome to The Scrum Component Demonstration</h3>

This component is based on the following documents: <br/>
<a href="http://www.scrum.org/scrumguides/" target="_BLANK">Scrum guide in various languages.</a> <br/>
<a href="http://www.scrumalliance.org/pages/what_is_scrum" target="_BLANK">Scrum in 30 seconds.</a><br/>
<a href="http://www.softhouse.se/Uploades/Scrum_eng_webb.pdf" target="_BLANK">Scrum in 5 minutes</a><br/>

The table shows the demo user's which are setup in the demo data for this component.<br/>  
When you click on the User Login ID,the system will login to the Scrum Component of that User Login ID <br/><br/>
</div>

<table>
    <tr>
        <th>USER LOGIN ID</th>
        <th>DESCRIPTION</th>
    </tr>
    <tr>
        <td>
        <a href="/scrum/control/main?USERNAME=scrumadmin&PASSWORD=ofbiz&JavaScriptEnabled=Y" class="buttontext">Scrum Admin</a><br/>
        </td>
        <td>
            <b>Scrum Admin</b><br/>
            The Scrum Admin is a userlogin who has full control and can execute any function in the Scrum Component.
        </td>
    </tr>
    <tr>
        <td>
        <a href="/scrum/control/main?USERNAME=scrummaster&PASSWORD=ofbiz&JavaScriptEnabled=Y" class="buttontext">Scrum Master</td>
        <td>
            <b>Scrum Master</b><br/>
            The Scrummaster can create the project and sprint and will put the product backlog into the sprint,<br/>
            can assign the tasks to the scrummember, add member to the project and sprint,and can manage timesheets.
        <td>
    </tr>
    <tr>
        <td>
        <a href="/scrum/control/main?USERNAME=productowner&PASSWORD=ofbiz&JavaScriptEnabled=Y" class="buttontext">Product Owner</a><br/>
        <a href="/scrum/control/main?USERNAME=productowner2&PASSWORD=ofbiz&JavaScriptEnabled=Y" class="buttontext">Product Owner2</a><br/>
        </td>
        <td>
            <b>Product Owner</b><br/>
            The Product Owner can maintain his products, find products and see all products that belong to this Product Owner. <br/>
            Product Owner can add and re-arrange the product backlog and look at the project/sprint overview and detail.<br/>
        </td>
    </tr>
    <tr>
        <td>
        <a href="/scrum/control/main?USERNAME=scrumteam1&PASSWORD=ofbiz&JavaScriptEnabled=Y" class="buttontext">Scrumteam1</a><br/>
        <a href="/scrum/control/main?USERNAME=scrumteam2&PASSWORD=ofbiz&JavaScriptEnabled=Y" class="buttontext">Scrumteam2</a><br/>
        <a href="/scrum/control/main?USERNAME=scrumteam3&PASSWORD=ofbiz&JavaScriptEnabled=Y" class="buttontext">Scrumteam3</a><br/>
        <a href="/scrum/control/main?USERNAME=scrumteam4&PASSWORD=ofbiz&JavaScriptEnabled=Y" class="buttontext">Scrumteam4</a><br/>
        </td>
        <td>
            <b>Scrumteam</b><br/>
            The Scrumteam can read the product information, see all products that the Scrumteam is working on.<br/>
            The Scrumteam can see and update the task information in a sprint.
        </td>
    </tr>
    <tr>
        <td>
        <a href="/scrum/control/main?USERNAME=testadmin&PASSWORD=ofbiz&JavaScriptEnabled=Y" class="buttontext">Testadmin</a>
        </td>
        <td>
            <b>Opentest</b><br/>
            This is the QA function in the system. This function will show all tasks which are complete but no final test was yet done.<br/>
            If the test was ok, the backlogitem will be be set to 'completed'<br/>
            However when the test failed the QA person can add error tasks to indicate what did not work yet.
        </td>
    </tr>
    <tr>
        <td>
        <a href="/scrum/control/main?USERNAME=DemoStakeholder&PASSWORD=ofbiz&JavaScriptEnabled=Y" class="buttontext">DemoStakeholder</a>
        </td>
        <td>
            <b>The Stakeholder</b><br/>
            The DemoStakeholder can view the Stakeholder's product and project/sprints only.<br/>
            This is for people belonging to the same company as the product owner and are interested in how the development is going.
        </td>
    </tr>
</table>