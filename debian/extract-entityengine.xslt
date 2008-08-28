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
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
 xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	version='1.0'>
 <xsl:output
	method="text"
 />

 <xsl:param name="ofbizhome"/>
 <xsl:variable name="newline">
  <xsl:text>
</xsl:text>
 </xsl:variable>

 <xsl:template match="entity-config">
  <xsl:variable name="datasource"><xsl:value-of select="delegator[@name='default']/group-map/@datasource-name"/></xsl:variable>
  <xsl:text>ofbiz_dbtype="</xsl:text>
  <xsl:value-of select="substring($datasource, 6)"/>
  <xsl:text>"</xsl:text>
  <xsl:value-of select="$newline"/>
  <xsl:choose>
   <xsl:when test="$datasource = 'localderby'"/>
   <xsl:when test="$datasource = 'localhsql'"/>
   <xsl:when test="$datasource = 'localpostgres' or $datasource = 'localmysql'">
    <xsl:apply-templates select="datasource[@name=$datasource]/inline-jdbc">
     <xsl:with-param name="datasource"><xsl:value-of select="$datasource"/></xsl:with-param>
    </xsl:apply-templates>
   </xsl:when>
  </xsl:choose>
 </xsl:template>

 <xsl:template match="datasource/inline-jdbc">
  <xsl:variable name="rest1"><xsl:value-of select="substring(substring-after(substring-after(@jdbc-uri, ':'), ':'), 3)"/></xsl:variable>
  <xsl:text>ofbiz_dbname="</xsl:text>
  <xsl:value-of select="substring-after($rest1, '/')"/>
  <xsl:text>"</xsl:text>
  <xsl:value-of select="$newline"/>
  <xsl:variable name="rest2"><xsl:value-of select="substring-before($rest1, '/')"/></xsl:variable>
  <xsl:text>ofbiz_dbserver="</xsl:text>
  <xsl:choose>
   <xsl:when test="contains($rest2, ':')">
    <xsl:value-of select="substring-before($rest2, ':')"/>
    <xsl:text>"</xsl:text>
    <xsl:value-of select="$newline"/>
    <xsl:text>ofbiz_dbport="</xsl:text>
    <xsl:value-of select="substring-after($rest2, ':')"/>
   </xsl:when>
   <xsl:otherwise>
    <xsl:value-of select="$rest2"/>
   </xsl:otherwise>
  </xsl:choose>
  <xsl:text>"</xsl:text>
  <xsl:value-of select="$newline"/>
  <xsl:text>ofbiz_dbusername="</xsl:text>
  <xsl:value-of select="@jdbc-username"/>
  <xsl:text>"</xsl:text>
  <xsl:value-of select="$newline"/>
  <xsl:text>ofbiz_dbpassword="</xsl:text>
  <xsl:value-of select="@jdbc-password"/>
  <xsl:text>"</xsl:text>
  <xsl:value-of select="$newline"/>
 </xsl:template>

 <xsl:template match="*">
  <xsl:element name="{name()}"><xsl:apply-templates select="*|@*|text()|comment()"/></xsl:element>
 </xsl:template>

 <xsl:template match="text">
  <xsl:value-of select="."/>
 </xsl:template>

 <xsl:template match="@*">
  <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
 </xsl:template>

 <xsl:template match="comment()">
  <xsl:copy-of select="."/>
 </xsl:template>
</xsl:stylesheet>
