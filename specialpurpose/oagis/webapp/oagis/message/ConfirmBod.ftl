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

<n:CONFIRM_BOD_004 xmlns:n="http://www.openapplications.org/002_confirm_bod_004" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://www.openapplications.org/002_confirm_bod_004 file:///C:/Documents%20and%20Settings/022523/My%20Documents/Vudu/XML%20Specs/OAG%20721/002_confirm_bod_004.xsd" xmlns:N1="http://www.openapplications.org/oagis_segments" xmlns:N2="http://www.openapplications.org/oagis_fields">
  <N1:CNTROLAREA>
    <N1:BSR>
      <N2:VERB>CONFIRM</N2:VERB>
      <N2:NOUN>BOD</N2:NOUN>
      <N2:REVISION>004</N2:REVISION>
    </N1:BSR>
    <N1:SENDER>
      <N2:LOGICALID>${logicalId}</N2:LOGICALID>
      <N2:COMPONENT>EXCEPTION</N2:COMPONENT>
      <N2:TASK>RECEIPT</N2:TASK>
      <N2:REFERENCEID>${referenceId}</N2:REFERENCEID>
      <N2:CONFIRMATION>1</N2:CONFIRMATION>
      <N2:LANGUAGE>ENG</N2:LANGUAGE>
      <N2:CODEPAGE>NONE</N2:CODEPAGE>
      <N2:AUTHID>${authId?if_exists}</N2:AUTHID>
    </N1:SENDER>
    <N1:DATETIMEANY>${sentDate?if_exists}</N1:DATETIMEANY>
  </N1:CNTROLAREA>
  <n:DATAAREA>
    <n:CONFIRM_BOD>
      <n:CONFIRM>
        <N1:CNTROLAREA>
          <N1:SENDER>
            <N2:LOGICALID>${errorLogicalId?if_exists}</N2:LOGICALID>
            <N2:COMPONENT>${errorComponent?if_exists}</N2:COMPONENT>
            <N2:TASK>${errorTask?if_exists}</N2:TASK>
            <N2:REFERENCEID>${errorReferenceId?if_exists}</N2:REFERENCEID>
          </N1:SENDER>
          <N1:DATETIMEANY></N1:DATETIMEANY>
        </N1:CNTROLAREA>
        <N2:ORIGREF>${origRef?if_exists}</N2:ORIGREF>
        <n:CONFIRMMSG>
          <N2:DESCRIPTN>${errorDescription?if_exists}</N2:DESCRIPTN>
          <N2:REASONCODE>${errorReasonCode?if_exists}</N2:REASONCODE>
        </n:CONFIRMMSG>
      </n:CONFIRM>
    </n:CONFIRM_BOD>
  </n:DATAAREA>
</n:CONFIRM_BOD_004>
