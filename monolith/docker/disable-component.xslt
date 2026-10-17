<?xml version="1.0" encoding="UTF-8"?>
<!--
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

<!--
    Copy all attributes and elements of the root ofbiz-component element in an XML document, changing the enabled
    attribute to false.
-->
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:template match="/">
        <xsl:element name="ofbiz-component">
            <xsl:copy-of select="ofbiz-component/@*"/>
            <xsl:attribute name="enabled">false</xsl:attribute>
            <xsl:copy-of select="ofbiz-component/node()"/>
        </xsl:element>
    </xsl:template>
</xsl:stylesheet>
