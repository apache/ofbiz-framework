<#--
 *  Copyright (c) 2003-2005 The Open For Business Project - www.ofbiz.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a
 *  copy of this software and associated documentation files (the "Software"),
 *  to deal in the Software without restriction, including without limitation
 *  the rights to use, copy, modify, merge, publish, distribute, sublicense,
 *  and/or sell copies of the Software, and to permit persons to whom the
 *  Software is furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included
 *  in all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 *  OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 *  IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 *  CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT
 *  OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 *  THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 *@author     Leon Torres (leon@opensourcestrategies.com)
-->
<?xml version="1.0" encoding="UTF-8"?>

<#-- FOP may require a library called JIMI to print certain graphical formats such as GIFs.  Jimi is a Sun library which cannot
be included in OFBIZ due to licensing incompatibility, but you can download it yourself at: http://java.sun.com/products/jimi/
and rename the ZIP file that comes with it as jimi-xxx.jar, then copy it into the same directory as fop.jar, which at this time 
is ${ofbiz.home}/framework/webapp/lib/ -->

<fo:root xmlns:fo="http://www.w3.org/1999/XSL/Format">
    <fo:layout-master-set>
        <#-- these margins are arbitrary, please redefine as you see fit -->
        <fo:simple-page-master master-name="main-page"
            margin-top="1in" margin-bottom="1in"
            margin-left="1in" margin-right="1in">
          <fo:region-body margin-top="3.5in" margin-bottom="1in"/>  <#-- main body -->
        </fo:simple-page-master>
  </fo:layout-master-set>
  
  <fo:page-sequence master-reference="main-page">
       <fo:flow flow-name="xsl-region-body">
       <#assign segments = Static["org.ofbiz.base.util.UtilHttp"].parseMultiFormData(parameters)>
       <#list segments as segment>
         <fo:block break-before="page"> <#-- this tells fop to put a page break before this content TODO: content-type must be dynamic -->
           <fo:external-graphic content-type="content-type:image/gif" src="<@ofbizUrl>viewShipmentLabel?shipmentId=${segment.shipmentId}&amp;shipmentRouteSegmentId=${segment.shipmentRouteSegmentId}&amp;shipmentPackageSeqId=${segment.shipmentPackageSeqId}</@ofbizUrl>"></fo:external-graphic>
         </fo:block>
      </#list>
      <#if segments.size() == 0>
        <fo:block>No Shipping Labels Selected</fo:block>
      </#if>
      </fo:flow>
  </fo:page-sequence>
</fo:root>
