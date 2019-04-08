<?xml version="1.0" encoding="ASCII"?>
<!--This file was created automatically by html2xhtml-->
<!--from the HTML stylesheets.-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns="http://www.w3.org/1999/xhtml" version="1.0">

<!-- ********************************************************************
     $Id: keywords.xsl 1395307 2012-10-07 14:02:53Z jleroux $
     ********************************************************************

     This file is part of the XSL DocBook Stylesheet distribution.
     See ../README or http://docbook.sf.net/release/xsl/current/ for
     copyright and other information.

     ******************************************************************** -->

<xsl:template match="keywordset"/>
<xsl:template match="subjectset"/>

<!-- ==================================================================== -->

<xsl:template match="keywordset" mode="html.header">
  <meta name="keywords">
    <xsl:attribute name="content">
      <xsl:apply-templates select="keyword" mode="html.header"/>
    </xsl:attribute>
  </meta>
</xsl:template>

<xsl:template match="keyword" mode="html.header">
  <xsl:apply-templates/>
  <xsl:if test="following-sibling::keyword">, </xsl:if>
</xsl:template>

<!-- ==================================================================== -->

</xsl:stylesheet>
