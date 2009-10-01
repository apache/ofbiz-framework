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
	method="xml"
	indent="yes"
 />
 <xsl:param name="dbtype">pgsql</xsl:param>
 <xsl:param name="dbuser">ofbiz</xsl:param>
 <xsl:param name="dbpass">ofbiz</xsl:param>
 <xsl:param name="dbserver">127.0.0.1</xsl:param>
 <xsl:param name="dbport">5432</xsl:param>
 <xsl:param name="dbname">ofbiz</xsl:param>

 <xsl:variable name="datasource">
  <xsl:choose>
   <xsl:when test="$dbtype='pgsql'">localpostgres</xsl:when>
   <xsl:when test="$dbtype='mysql'">localmysql</xsl:when>
   <xsl:when test="$dbtype='derby'">localderby</xsl:when>
   <xsl:when test="$dbtype='derby'">localhsql</xsl:when>
  </xsl:choose>
 </xsl:variable>

 <xsl:template match="delegator[@name='default']/group-map/@datasource-name">
  <xsl:attribute name="datasource-name"><xsl:value-of select="$datasource"/></xsl:attribute>
 </xsl:template>

 <xsl:template match="delegator[@name='default-no-eca']/group-map/@datasource-name">
  <xsl:attribute name="datasource-name"><xsl:value-of select="$datasource"/></xsl:attribute>
 </xsl:template>

 <xsl:template match="datasource">
  <xsl:element name="{name()}"><xsl:apply-templates select="*|@*|text()|comment()"/></xsl:element>
 </xsl:template>

 <xsl:template name="datasource-common">
  <xsl:choose>
   <xsl:when test="$dbserver"><xsl:value-of select="$dbserver"/></xsl:when>
   <xsl:otherwise>127.0.0.1</xsl:otherwise>
  </xsl:choose>
  <xsl:if test="$dbport">
   <xsl:text>:</xsl:text>
   <xsl:value-of select="$dbport"/>
  </xsl:if>
  <xsl:text>/</xsl:text>
  <xsl:value-of select="$dbname"/>
 </xsl:template>

 <xsl:template match="datasource[@name=$datasource and $dbtype='derby']/inline-jdbc/@jdbc-uri">
  <xsl:attribute name="jdbc-uri"><xsl:value-of select="."/></xsl:attribute>
 </xsl:template>

 <xsl:template match="datasource[@name=$datasource and $dbtype='hsql']/inline-jdbc/@jdbc-uri">
  <xsl:attribute name="jdbc-uri"><xsl:value-of select="."/></xsl:attribute>
 </xsl:template>

 <xsl:template match="datasource[@name=$datasource and $dbtype='pgsql']/inline-jdbc/@jdbc-uri">
  <xsl:attribute name="jdbc-uri">
   <xsl:text>jdbc:postgresql://</xsl:text>
   <xsl:call-template name="datasource-common"/>
  </xsl:attribute>
 </xsl:template>

 <xsl:template match="datasource[@name=$datasource and $dbtype='mysql']/inline-jdbc/@jdbc-uri">
  <xsl:attribute name="jdbc-uri">
   <xsl:text>jdbc:mysql://</xsl:text>
   <xsl:call-template name="datasource-common"/>
   <xsl:text>?autoReconnect=true</xsl:text>
  </xsl:attribute>
 </xsl:template>

 <xsl:template match="datasource[@name=$datasource and $dbtype='derby']/inline-jdbc/@jdbc-username">
  <xsl:attribute name="jdbc-uri"><xsl:value-of select="."/></xsl:attribute>
 </xsl:template>

 <xsl:template match="datasource[@name=$datasource and $dbtype='hsql']/inline-jdbc/@jdbc-username">
  <xsl:attribute name="jdbc-uri"><xsl:value-of select="."/></xsl:attribute>
 </xsl:template>

 <xsl:template match="datasource[@name=$datasource]/inline-jdbc/@jdbc-username">
  <xsl:attribute name="jdbc-username"><xsl:value-of select="$dbuser"/></xsl:attribute>
 </xsl:template>

 <xsl:template match="datasource[@name=$datasource and $dbtype='derby']/inline-jdbc/@jdbc-password">
  <xsl:attribute name="jdbc-uri"><xsl:value-of select="."/></xsl:attribute>
 </xsl:template>

 <xsl:template match="datasource[@name=$datasource and $dbtype='hsql']/inline-jdbc/@jdbc-password">
  <xsl:attribute name="jdbc-uri"><xsl:value-of select="."/></xsl:attribute>
 </xsl:template>

 <xsl:template match="datasource[@name=$datasource]/inline-jdbc/@jdbc-password">
  <xsl:attribute name="jdbc-password"><xsl:value-of select="$dbpass"/></xsl:attribute>
 </xsl:template>
<!--
                jdbc-uri="jdbc:postgresql://$dbserver[:$dbport]/$dbname"
                jdbc-username="$dbuser"
                jdbc-password="$dbpass"

-->
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
