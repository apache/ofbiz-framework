<?xml version="1.0" encoding="UTF-8"?>
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
<#--  xsi:schemaLocation="http://www.openapplications.org/002_confirm_bod_004 file:///C:/Documents%20and%20Settings/022523/My%20Documents/Vudu/XML%20Specs/OAG%20721/002_confirm_bod_004.xsd" -->
<n:CONFIRM_BOD_004 xmlns:n="http://www.openapplications.org/002_confirm_bod_004"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xmlns:os="http://www.openapplications.org/oagis_segments"
        xmlns:of="http://www.openapplications.org/oagis_fields">
  <os:CNTROLAREA>
    <os:BSR>
      <of:VERB>CONFIRM</of:VERB>
      <of:NOUN>BOD</of:NOUN>
      <of:REVISION>004</of:REVISION>
    </os:BSR>
    <os:SENDER>
      <of:LOGICALID>${logicalId}</of:LOGICALID>
      <of:COMPONENT>EXCEPTION</of:COMPONENT>
      <of:TASK>RECEIPT</of:TASK>
      <of:REFERENCEID>${referenceId}</of:REFERENCEID>
      <of:CONFIRMATION>1</of:CONFIRMATION>
      <of:LANGUAGE>ENG</of:LANGUAGE>
      <of:CODEPAGE>NONE</of:CODEPAGE>
      <of:AUTHID>${authId!}</of:AUTHID>
    </os:SENDER>
    <os:DATETIMEISO>${sentDate!}</os:DATETIMEISO>
  </os:CNTROLAREA>
  <n:DATAAREA>
    <n:CONFIRM_BOD>
      <n:CONFIRM>
        <os:CNTROLAREA>
          <os:SENDER>
            <of:LOGICALID>${errorLogicalId!}</of:LOGICALID>
            <of:COMPONENT>${errorComponent!}</of:COMPONENT>
            <of:TASK>${errorTask!}</of:TASK>
            <of:REFERENCEID>${errorReferenceId!}</of:REFERENCEID>
          </os:SENDER>
          <os:DATETIMEISO></os:DATETIMEISO>
        </os:CNTROLAREA>
        <of:ORIGREF>${origRef!}</of:ORIGREF>
        <#if errorMapList??>
          <#list errorMapList as errorMap>
            <n:CONFIRMMSG>
              <of:DESCRIPTN>${errorMap.description!?xml}</of:DESCRIPTN>
              <of:REASONCODE>${errorMap.reasonCode!?xml}</of:REASONCODE>
            </n:CONFIRMMSG>
          </#list>
        </#if>
      </n:CONFIRM>
    </n:CONFIRM_BOD>
  </n:DATAAREA>
</n:CONFIRM_BOD_004>
