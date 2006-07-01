
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
