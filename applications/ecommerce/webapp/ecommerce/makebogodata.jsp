<%--
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
--%>

<%@ page import="org.ofbiz.product.catalog.*" %>
<%@ page import="org.ofbiz.product.product.*" %>
<%@ page import="org.ofbiz.entity.*" %>
<%@ page import="org.ofbiz.base.util.*" %>
<%@ page import="java.util.*" %>

<jsp:useBean id="delegator" type="org.ofbiz.entity.GenericDelegator" scope="request" />

<%
  Iterator prods = UtilMisc.toIterator(delegator.findByAnd("Product", null, null));
  while(prods.hasNext())
  {
    GenericValue prod1 = (GenericValue)prods.next();
    KeywordSearch.induceKeywords(prod1);
  }
  
  if(request.getParameter("makeall") == null) {
%>Just added the keywords from all existing product info.
<br/><a href='makebogodata.jsp?makeall=true'>Create a LOT of products, categories, and keywords.</a>
<%
  }
  else {
    String[] wordBag = {"a", "product", "big", "ugly", "pretty", "small", "under", "over", "one", "two", "three", "four", "five", "six", "seven", "eight", "tree"};
    String[] longWordBag = {"b", "item", "little", "cute", "frightening", "massive", "top", "btoom", "bush", "shrub", "gadget"};

    for(int cat=1; cat<=400; cat++)
    {
      String parentId = cat<=20?"CATALOG1":"" + (cat/20);
      delegator.create("ProductCategory", UtilMisc.toMap("productCategoryId", "" + cat, "primaryParentCategoryId", parentId, "description", "Category " + cat));
      delegator.create("ProductCategoryRollup", UtilMisc.toMap("productCategoryId", "" + cat, "parentProductCategoryId", parentId));
      for(int prod=1; prod<=50; prod++)
      {
        String desc = "Cool Description";
        for(int i=0; i<10; i++) {
          int wordNum = (int)(Math.random()*(wordBag.length-1));
          desc += (" " + wordBag[wordNum]);
        }
        String longDesc = "Cool LONG Description";
        for(int i=0; i<50; i++) {
          int wordNum = (int)(Math.random()*(longWordBag.length-1));
          longDesc += (" " + longWordBag[wordNum]);
        }
        Double price = new Double(2.99 + prod);
        GenericValue product = delegator.create("Product", UtilMisc.toMap("productId", "" + (cat*100 + prod), "primaryProductCategoryId", "" + (cat), "productName", "Product " + "" + (cat*100 + prod), "description", desc, "longDescription", longDesc, "defaultPrice", price));
        KeywordSearch.induceKeywords(product);
        delegator.create("ProductCategoryMember", UtilMisc.toMap("productId", "" + (cat*100 + prod), "productCategoryId", "" + (cat)));
      }
    }
%>Created lots of products and categories and keywords.<%
  }
%>
