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


<!DOCTYPE html>
<html>
  <head>
    <meta charset="utf-8" />
    <title>${applicationTitle?if_exists}</title>
    <link rel="stylesheet" href="/images/jquery/jquery.mobile-1.1.0-rc.1/jquery.mobile-1.1.0-rc.1.css" />
    <script src="/images/jquery/jquery-1.7.2.min.js" type="text/javascript"></script>
    <script src="/images/jquery/jquery.mobile-1.1.0-rc.1/jquery.mobile-1.1.0-rc.1.js" type="text/javascript"></script>
    <meta name="viewport" content="width=device-width, initial-scale=1">
  </head>
  <body>
    <div data-role="header">
      <a href="<@ofbizUrl>/menu?facilityId=${parameters.facilityId?if_exists}</@ofbizUrl>">Main</a>
      <h1>${title?if_exists}</h1>
    </div>
