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
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<#if locale?exists>
    <#assign initialLocale = locale.toString()>
<#else>
    <#assign initialLocale = "en">
</#if>

<head>
  <meta content="text/html; charset=UTF-8" http-equiv="Content-Type"/>
    <title>${(uiLabelMap.OfbizTitle)!"OFBiz, The Apache Open For Business Project"}</title>
    <link rel="shortcut icon" href="/ofbiz/images/favicon.ico">
    <script language="javascript" src="/images/jquery/jquery-1.8.2.min.js" type="text/javascript"></script>
    <script language="javascript" src="/images/fieldlookup.js" type="text/javascript"></script>
    <script language="javascript" src="/images/selectall.js" type="text/javascript"></script>
    <script language="javascript" src="/ofbiz/script/search.js" type="text/javascript"></script>
    <script language="javascript" src="/images/jquery/plugins/jcarousel/jquery.jcarousel.min.js"></script>
    <link rel="stylesheet" type="text/css" href="/images/jquery/plugins/jcarousel/skins/tango/skin.css" />


    <link rel="stylesheet" href="/ofbiz/images/global.css" type="text/css"/>

    <meta content="OFBiz_Thai for Thai user" name="Description"/>
    <meta content="Open Source ERP,Open Source CRM,Open Source E-Commerce,Open Source eCommerce,Open Source POS,Open Source SCM,Open Source MRP,Open Source CMMS,Open Source EAM,web services,workflow,ebusiness,e-business,ecommerce,e-commerce,automation,enterprise software,open source,entity engine,service engine,erp,crm,party,accounting,facility,supply,chain,management,catalog,order,project,task,work effort,financial,ledger,content management,customer,inventory" name="keywords"/>
</head>

