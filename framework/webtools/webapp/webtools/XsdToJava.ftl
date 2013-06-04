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
<#assign document = xsdRootElement.getOwnerDocument()>
<#assign documentElement = document.getDocumentElement()>
<#assign globalElements = Static["org.ofbiz.base.util.UtilXml"].childElementList(xsdRootElement, "xs:element")>
<#if globalElements?exists>
  <textarea name="java-source" cols="120" rows="50" readonly="readonly">
  <#assign abstractElementNames = []>
  <#assign allElements = []>
  <#list globalElements as globalElement>
    <#assign allElements = allElements + [globalElement]>
    <#if globalElement.getAttribute("abstract") == "true">
      <#assign abstractElementNames = abstractElementNames + [globalElement.getAttribute("name")]>
    </#if>
  </#list>
  <#list globalElements as globalElement>
    <@writeJavaFile globalElement />
  </#list>
  <#list abstractElementNames as abstractElementName>
    <@createFactory abstractElementName />
  </#list>
  <#if visitorClassName?exists && visitorClassName?has_content>
    <@createVisitor />
  </#if>
  </textarea>
</#if>

<#macro writeJavaFile xsdElement>
<@fileHeader />
<@writeClass xsdElement />
</#macro>

<#macro writeClass xsdElement>
<#local elementName = xsdElement.getAttribute("name")>
<#local className = Static["org.ofbiz.base.util.UtilXml"].nodeNameToJavaName(elementName, true)>
<#local complexTypeElement = Static["org.ofbiz.base.util.UtilXml"].firstChildElement(xsdElement, "complexType")?if_exists>
<#if complexTypeElement?exists && complexTypeElement?has_content>
  <#local attributeElements = Static["org.ofbiz.base.util.UtilXml"].childElementList(complexTypeElement, "xs:attribute")>
  <#local attributeGroupElements = Static["org.ofbiz.base.util.UtilXml"].childElementList(complexTypeElement, "xs:attributeGroup")>
  <#local sequenceElement = Static["org.ofbiz.base.util.UtilXml"].firstChildElement(complexTypeElement, "sequence")?if_exists>
</#if>
<#if sequenceElement?exists && sequenceElement?has_content>
  <#local childElements = Static["org.ofbiz.base.util.UtilXml"].childElementList(sequenceElement, "xs:element")>
</#if>
<#local globalAttributeGroups = Static["org.ofbiz.base.util.UtilXml"].childElementList(documentElement, "xs:attributeGroup")>
<@classDeclaration xsdElement />

<#-- Class field declarations -->
<#if attributeElements?exists>
  <#list attributeElements as attributeElement>
    <@attributeFieldDeclaration attributeElement />
  </#list>
</#if>
<#if attributeGroupElements?exists>
  <#list attributeGroupElements as attributeGroupElement>
    <#local attributeGroupName = attributeGroupElement.getAttribute("ref")>
    <#list globalAttributeGroups as globalAttributeGroup>
      <#if attributeGroupName == globalAttributeGroup.getAttribute("name")>
        <#local globalAttributeElements = Static["org.ofbiz.base.util.UtilXml"].childElementList(globalAttributeGroup, "xs:attribute")>
        <#list globalAttributeElements as attributeElement>
          <@attributeFieldDeclaration attributeElement />
        </#list>
      </#if>
    </#list>
  </#list>
</#if>
<#if childElements?exists>
  <#list childElements as childElement>
    <@elementFieldDeclaration childElement />
  </#list>
</#if>
<#if !(xsdElement.getAttribute("abstract") == "true")>

    public ${className}(Element element) {
  <#-- Class field assignments -->
  <#if attributeElements?exists>
    <#list attributeElements as attributeElement>
      <@attributeFieldAssignment attributeElement />
    </#list>
  </#if>
  <#if attributeGroupElements?exists>
    <#list attributeGroupElements as attributeGroupElement>
      <#local attributeGroupName = attributeGroupElement.getAttribute("ref")>
      <#list globalAttributeGroups as globalAttributeGroup>
        <#if attributeGroupName == globalAttributeGroup.getAttribute("name")>
          <#list globalAttributeElements as attributeElement>
            <@attributeFieldAssignment attributeElement />
          </#list>
        </#if>
      </#list>
    </#list>
  </#if>
  <#if childElements?exists>
    <#list childElements as childElement>
      <@elementFieldAssignment childElement />
    </#list>
  </#if>
    }
  <#-- Visitor implementation -->
  <#if visitorClassName?exists && visitorClassName?has_content>

    public void accept(${visitorClassName} visitor) throws Exception {
        visitor.visit(this);
    }
  </#if>
</#if>
<#-- Class field accessors -->
<#if attributeElements?exists>
  <#list attributeElements as attributeElement>
    <@attributeFieldAccessor attributeElement />
  </#list>
</#if>
<#if attributeGroupElements?exists>
  <#list attributeGroupElements as attributeGroupElement>
    <#local attributeGroupName = attributeGroupElement.getAttribute("ref")>
    <#list globalAttributeGroups as globalAttributeGroup>
      <#if attributeGroupName == globalAttributeGroup.getAttribute("name")>
        <#list globalAttributeElements as attributeElement>
          <@attributeFieldAccessor attributeElement />
        </#list>
      </#if>
    </#list>
  </#list>
</#if>
<#if childElements?exists>
  <#list childElements as childElement>
    <@elementFieldAccessor childElement />
  </#list>
</#if>
<#-- Nested elements/classes -->
<#if childElements?exists>
  <#list childElements as childElement>
    <#local elementName = childElement.getAttribute("name")>
    <#if elementName?has_content>
      <#assign allElements = allElements + [childElement]>
      <@writeClass childElement />
    </#if>
  </#list>
</#if>
}
</#macro>

<#macro fileHeader>
/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *******************************************************************************/
package ${packageName?default("org.ofbiz.*")};

import java.util.*;

import org.ofbiz.base.lang.ThreadSafe;
import org.ofbiz.base.util.UtilXml;
import org.w3c.dom.Element;
</#macro>

<#macro classDeclaration xsdElement>
<#--
<xs:element name="fooElement" minOccurs="0" maxOccurs="unbounded">
-->
<#local elementName = xsdElement.getAttribute("name")>
<#local className = Static["org.ofbiz.base.util.UtilXml"].nodeNameToJavaName(elementName, true)>
<#local abstract = (xsdElement.getAttribute("abstract") == "true")>
<#local substitutionGroup = xsdElement.getAttribute("substitutionGroup")>

/**
 * An object that models the <code>&amp;lt;${elementName}&amp;gt;</code> element.
 *
 * @see <code>${xsdFileName}</code>
 */
<#if abstract>
public interface ${className} {
<#else>
@ThreadSafe
public final class ${className}<#if substitutionGroup?has_content> implements ${substitutionGroup}</#if> {
</#if>
</#macro>

<#macro attributeFieldDeclaration xsdElement>
<#--
<xs:attribute name="fooAttribute" type="xs:someType" default="bar" use="required">
-->
<#local attributeName = xsdElement.getAttribute("name")>
<#local attributeType = xsdElement.getAttribute("type")>
<#local fieldName = Static["org.ofbiz.base.util.UtilXml"].nodeNameToJavaName(attributeName, false)>
    private final String ${fieldName};<#if attributeType?has_content> // type = ${attributeType}</#if>
</#macro>

<#macro attributeFieldAssignment xsdElement>
<#--
<xs:attribute name="fooAttribute" type="xs:someType" default="bar" use="required">
-->
<#local attributeName = xsdElement.getAttribute("name")>
<#local attributeType = xsdElement.getAttribute("type")>
<#local attributeUse = xsdElement.getAttribute("use")>
<#local attributeDefault = xsdElement.getAttribute("default")>
<#local fieldName = Static["org.ofbiz.base.util.UtilXml"].nodeNameToJavaName(attributeName, false)>
<#if attributeUse == "required">
        String ${fieldName} = element.getAttribute("${attributeName}").intern();
        if (${fieldName}.isEmpty()) {
            throw new ${exceptionClassName}("<" + element.getNodeName() + "> element ${attributeName} attribute is empty");
        }
        this.${fieldName} = ${fieldName};
<#else>
  <#if attributeDefault?has_content>
        String ${fieldName} = element.getAttribute("${attributeName}").intern();
        if (${fieldName}.isEmpty()) {
            ${fieldName} = "${attributeDefault}";
        }
        this.${fieldName} = ${fieldName};
  <#else>
        this.${fieldName} = element.getAttribute("${attributeName}").intern();
  </#if>
</#if>
</#macro>

<#macro attributeFieldAccessor xsdElement>
<#--
<xs:attribute name="fooAttribute" type="xs:someType" default="bar" use="required">
-->
<#local attributeName = xsdElement.getAttribute("name")>
<#local className = Static["org.ofbiz.base.util.UtilXml"].nodeNameToJavaName(attributeName, true)>
<#local fieldName = Static["org.ofbiz.base.util.UtilXml"].nodeNameToJavaName(attributeName, false)>

    /** Returns the value of the <code>${attributeName}</code> attribute. */
    public String get${className}() {
        return this.${fieldName};
    }
</#macro>

<#macro elementFieldDeclaration xsdElement>
<#--
<xs:element name="fooElement" minOccurs="0" maxOccurs="unbounded">
<xs:element minOccurs="0" maxOccurs="unbounded" ref="fooElement">
-->
<#local elementName = xsdElement.getAttribute("name")>
<#if !elementName?has_content>
  <#local elementName = xsdElement.getAttribute("ref")>
</#if>
<#local className = Static["org.ofbiz.base.util.UtilXml"].nodeNameToJavaName(elementName, true)>
<#local fieldName = Static["org.ofbiz.base.util.UtilXml"].nodeNameToJavaName(elementName, false)>
<#local isList = abstractElementNames?seq_contains(elementName)>
<#local maxOccurs = xsdElement.getAttribute("maxOccurs")>
<#if maxOccurs == "unbounded" || (maxOccurs?has_content && maxOccurs?number > 1)>
  <#local isList = true>
</#if>
<#if isList>
    private final List<${className}> ${fieldName}List; // <${elementName}>
<#else>
    private final Element ${fieldName}; // <${elementName}>
</#if>
</#macro>

<#macro elementFieldAssignment xsdElement>
<#--
<xs:element name="fooElement" minOccurs="0" maxOccurs="unbounded">
<xs:element minOccurs="0" maxOccurs="unbounded" ref="fooElement">
-->
<#local elementName = xsdElement.getAttribute("name")>
<#if !elementName?has_content>
  <#local elementName = xsdElement.getAttribute("ref")>
</#if>
<#local className = Static["org.ofbiz.base.util.UtilXml"].nodeNameToJavaName(elementName, true)>
<#local fieldName = Static["org.ofbiz.base.util.UtilXml"].nodeNameToJavaName(elementName, false)>
<#local minOccurs = xsdElement.getAttribute("minOccurs")>
<#local required = (!minOccurs?has_content || minOccurs?number > 0)>
<#local abstract = abstractElementNames?seq_contains(elementName)>
<#local isList = abstract>
<#local maxOccurs = xsdElement.getAttribute("maxOccurs")>
<#if maxOccurs == "unbounded" || (maxOccurs?has_content && maxOccurs?number > 1)>
  <#local isList = true>
</#if>
<#if isList>
  <#if abstract>
        List<? extends Element> ${fieldName}ElementList = UtilXml.childElementList(element);
  <#else>
        List<? extends Element> ${fieldName}ElementList = UtilXml.childElementList(element, "${elementName}");
  </#if>
        if (${fieldName}ElementList.isEmpty()) {
  <#if required>
            throw new ${exceptionClassName}("<" + element.getNodeName() + "> element child elements <${elementName}> are missing");
  <#else>
            this.${fieldName}List = Collections.emptyList();
  </#if>
        } else {
            List<${className}> ${fieldName}List = new ArrayList<${className}>(${fieldName}ElementList.size());
            for (Element ${fieldName}Element : ${fieldName}ElementList) {
  <#if abstract>
                ${fieldName}List.add(${className}Factory.create(${fieldName}Element));
  <#else>
                ${fieldName}List.add(new ${className}(${fieldName}Element));
  </#if>
            }
            this.${fieldName}List = Collections.unmodifiableList(${fieldName}List);
        }
<#else>
        Element ${fieldName}Element = UtilXml.firstChildElement(element, "${elementName}");
        if (${fieldName}Element == null) {
  <#if required>
            throw new ${exceptionClassName}("<" + element.getNodeName() + "> element child element <${elementName}> is missing");
  <#else>
            this.${fieldName} = null;
  </#if>
        } else {
            this.${fieldName} = new ${className}(${fieldName}Element);
        }
</#if>
</#macro>

<#macro elementFieldAccessor xsdElement>
<#--
<xs:element name="fooElement" minOccurs="0" maxOccurs="unbounded">
<xs:element minOccurs="0" maxOccurs="unbounded" ref="fooElement">
-->
<#local elementName = xsdElement.getAttribute("name")>
<#if !elementName?has_content>
  <#local elementName = xsdElement.getAttribute("ref")>
</#if>
<#local className = Static["org.ofbiz.base.util.UtilXml"].nodeNameToJavaName(elementName, true)>
<#local fieldName = Static["org.ofbiz.base.util.UtilXml"].nodeNameToJavaName(elementName, false)>
<#local minOccurs = xsdElement.getAttribute("minOccurs")>
<#local isList = abstractElementNames?seq_contains(elementName)>
<#local maxOccurs = xsdElement.getAttribute("maxOccurs")>
<#if maxOccurs == "unbounded" || (maxOccurs?has_content && maxOccurs?number > 1)>
  <#local isList = true>
</#if>
<#if isList>

    /** Returns the <code>&amp;lt;${elementName}&amp;gt;</code> child elements. */
    public List<${className}> get${className}List() {
        return this.${fieldName}List;
    }
<#else>

    /** Returns the <code>&amp;lt;${elementName}&amp;gt;</code> child element, or <code>null</code> if no child element was found. */
    public ${className} get${className}() {
        return this.${fieldName};
    }
</#if>
</#macro>

<#macro createFactory elementName>
<#local className = Static["org.ofbiz.base.util.UtilXml"].nodeNameToJavaName(elementName, true)>
<#local fieldName = Static["org.ofbiz.base.util.UtilXml"].nodeNameToJavaName(elementName, false)>
<@fileHeader />

/**
 * A ${className} factory.
 */
@ThreadSafe
public final class ${className}Factory {

    public static ${className} create(Element element) {
        String elementName = element.getNodeName();
  <#list allElements as globalElement>
    <#if globalElement.getAttribute("substitutionGroup") == elementName>
      <#local targetElementName = globalElement.getAttribute("name")>
      <#local className = Static["org.ofbiz.base.util.UtilXml"].nodeNameToJavaName(targetElementName, true)>
        if ("${targetElementName}".equals(elementName)) {
            return new ${className}(element);
        }
    </#if>
  </#list>
    }
}
</#macro>

<#macro createVisitor>
<@fileHeader />

/**
 * A model visitor.
 */
public interface ${visitorClassName} {

  <#list allElements as globalElement>
    <#local targetElementName = globalElement.getAttribute("name")>
    <#local abstract = abstractElementNames?seq_contains(targetElementName)>
    <#if !abstract>
    <#local targetClassName = Static["org.ofbiz.base.util.UtilXml"].nodeNameToJavaName(targetElementName, true)>
    void visit(${targetClassName} model) throws Exception;

    </#if>
  </#list>
}
</#macro>
