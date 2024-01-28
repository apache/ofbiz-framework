package com.simbaquartz.xcommon.services.category;


import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;

public class CategoryServices {

  public static final String module = CategoryServices.class.getName();

  public static Map<String, Object> createSuppProductCategory(DispatchContext dctx,
      Map<String, Object> context) {
    Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
    Delegator delegator = dctx.getDelegator();
    GenericValue userLogin = (GenericValue) context.get("userLogin");
    String productCategoryId = null;
    String supplierPartyId = (String) context.get("supplierPartyId");
    String categoryName = (String) context.get("categoryName");
    String description = (String) context.get("description");
    Long sequenceNum = 0L;

    try {
      List<GenericValue> suppProductCategories = EntityQuery.use(delegator)
          .from("SupplierProductCategory")
          .where("supplierPartyId", supplierPartyId).queryList();
      if (UtilValidate.isNotEmpty(suppProductCategories)) {
        for (GenericValue suppProductCategory : suppProductCategories) {
          String categoryId = suppProductCategory.getString("productCategoryId");
          GenericValue productCategory = EntityQuery.use(delegator).from("ProductCategory")
              .where("productCategoryId", categoryId).queryOne();
          String categoryNameRec = productCategory.getString("categoryName");
          if (categoryName.equals(categoryNameRec)) {
            return ServiceUtil.returnError("Category name already exists");
          }
          Long seqNumFromRecord = (Long) suppProductCategory.get("sequenceNum");
          if (seqNumFromRecord > sequenceNum) {
            sequenceNum = seqNumFromRecord;
          }
        }
      }
      //increment sequence number
      sequenceNum++;

      productCategoryId = delegator.getNextSeqId("ProductCategory");
      GenericValue productCategoryCtx = delegator.makeValue("ProductCategory");
      productCategoryCtx.set("productCategoryId", productCategoryId);
      productCategoryCtx.set("categoryName", categoryName);
      productCategoryCtx.put("description", description);

      delegator.create(productCategoryCtx);

      //associate category with supplier
      if (UtilValidate.isNotEmpty(productCategoryId)) {
        GenericValue suppProductCategoryGroup = delegator.makeValue("SupplierProductCategory");
        suppProductCategoryGroup.set("productCategoryId", productCategoryId);
        suppProductCategoryGroup.set("supplierPartyId", supplierPartyId);
        suppProductCategoryGroup.put("sequenceNum", sequenceNum);

        delegator.create(suppProductCategoryGroup);
      }
      serviceResult.put("productCategoryId", productCategoryId);
    } catch (GenericEntityException e) {
      Debug.logError(e, module);
      return ServiceUtil.returnError(e.getMessage());
    }
    return serviceResult;
  }

  public static Map<String, Object> updateSuppProductCategory(DispatchContext dctx,
      Map<String, Object> context) {
    Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
    Delegator delegator = dctx.getDelegator();
    GenericValue userLogin = (GenericValue) context.get("userLogin");
    String productCategoryId = (String) context.get("productCategoryId");
    String categoryName = (String) context.get("categoryName");
    String description = (String) context.get("description");
    try {
      GenericValue productCategory = EntityQuery.use(delegator).from("ProductCategory")
          .where("productCategoryId", productCategoryId).queryOne();
      if (UtilValidate.isNotEmpty(productCategory)) {
        productCategory.set("categoryName", categoryName);
        productCategory.set("description", description);
        delegator.store(productCategory);
      }
    } catch (GenericEntityException e) {
      Debug.logError(e, module);
      return ServiceUtil.returnError(e.getMessage());
    }
    return serviceResult;
  }

  public static Map<String, Object> deleteSuppProductCategory(DispatchContext dctx,
      Map<String, Object> context) {
    Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
    Delegator delegator = dctx.getDelegator();
    GenericValue userLogin = (GenericValue) context.get("userLogin");
    String productCategoryId = (String) context.get("productCategoryId");

    try {

      List<GenericValue> productCategoryMembers = EntityQuery.use(delegator)
          .from("ProductCategoryMember").where(
              "productCategoryId", productCategoryId).queryList();
      for (GenericValue productCategoryMember : productCategoryMembers) {
        if (UtilValidate.isEmpty(productCategoryMember)) {
          String errorMsg = "No such record exists!";
          Debug.logError(errorMsg, module);
          return ServiceUtil.returnError(errorMsg);
        }
        delegator.removeValue(productCategoryMember);
      }

      GenericValue suppCategoryGroup = EntityQuery.use(delegator).from("SupplierProductCategory")
          .where(
              "productCategoryId", productCategoryId).queryOne();

      if (UtilValidate.isEmpty(suppCategoryGroup)) {
        String errorMsg = "No such group exists!";
        Debug.logError(errorMsg, module);
        return ServiceUtil.returnError(errorMsg);
      }
      delegator.removeValue(suppCategoryGroup);

      GenericValue productCategory = EntityQuery.use(delegator).from("ProductCategory").where(
          "productCategoryId", productCategoryId).queryOne();

      if (UtilValidate.isEmpty(productCategory)) {
        String errorMsg = "No such group exists!";
        Debug.logError(errorMsg, module);
        return ServiceUtil.returnError(errorMsg);
      }
      delegator.removeValue(productCategory);
    } catch (GenericEntityException e) {
      Debug.logError(e, module);
      return ServiceUtil.returnError(e.getMessage());
    }

    return serviceResult;
  }

  public static Map<String, Object> createSuppProductCategoryMember(DispatchContext dctx,
      Map<String, Object> context) {
    Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
    Delegator delegator = dctx.getDelegator();
    GenericValue userLogin = (GenericValue) context.get("userLogin");
    String productId = (String) context.get("productId");
    String productCategoryId = (String) context.get("productCategoryId");
    Timestamp fromDate = UtilDateTime.nowTimestamp();
    Timestamp thruDate = (Timestamp) context.get("thruDate");

    try {

      GenericValue suppProductCategoryMember = delegator.makeValue("ProductCategoryMember");
      suppProductCategoryMember.set("productId", productId);
      suppProductCategoryMember.set("productCategoryId", productCategoryId);
      suppProductCategoryMember.set("fromDate", fromDate);
      suppProductCategoryMember.put("thruDate", thruDate);

      delegator.create(suppProductCategoryMember);
    } catch (GenericEntityException e) {
      Debug.logError(e, module);
      return ServiceUtil.returnError(e.getMessage());
    }
    return serviceResult;
  }

  public static Map<String, Object> removeSuppProductCategoryMember(DispatchContext dctx,
      Map<String, Object> context) {
    Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
    Delegator delegator = dctx.getDelegator();
    GenericValue userLogin = (GenericValue) context.get("userLogin");
    String productId = (String) context.get("productId");
    String productCategoryId = (String) context.get("productCategoryId");

    try {

      GenericValue removeProductCategoryMember = EntityQuery.use(delegator)
          .from("ProductCategoryMember").where("productId", productId,
              "productCategoryId", productCategoryId).filterByDate().queryFirst();

      if (UtilValidate.isEmpty(removeProductCategoryMember)) {
        String errorMsg = "No such product exists!";
        Debug.logError(errorMsg, module);
        return ServiceUtil.returnError(errorMsg);
      }
      delegator.removeValue(removeProductCategoryMember);
    } catch (GenericEntityException e) {
      Debug.logError(e, module);
      return ServiceUtil.returnError(e.getMessage());
    }
    return serviceResult;
  }

  public static Map<String, Object> copySupplierPartyProductCategory(DispatchContext dctx,
      Map<String, Object> context) {
    Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
    Delegator delegator = dctx.getDelegator();
    GenericValue userLogin = (GenericValue) context.get("userLogin");
    String supplierPartyId = (String) context.get("supplierPartyId");
    List<String> productCategories = (List) context.get("productCategories");

    String productCategoryId = "";
    Long sequenceNum = 0L;
    String categoryName = "";
    String description = "";
    try {
      for (String productCategory : productCategories) {
        GenericValue existingSuppProductCategoryDetails = EntityQuery.use(delegator)
            .from("ProductCategory")
            .where("productCategoryId", productCategory).queryOne();

        categoryName = existingSuppProductCategoryDetails.getString("categoryName");
        description = existingSuppProductCategoryDetails.getString("description");

        boolean checkDuplicate = false;
        List<GenericValue> targetSupplierPartyCategoryGroups = EntityQuery.use(delegator)
            .from("SupplierProductCategory")
            .where("supplierPartyId", supplierPartyId).queryList();
        for (GenericValue targetSupplierPartyCategoryGroup : targetSupplierPartyCategoryGroups) {
          String categoryId = targetSupplierPartyCategoryGroup.getString("productCategoryId");

          //check duplicates
          GenericValue suppPartyCategoryGroup = EntityQuery.use(delegator).from("ProductCategory")
              .where("productCategoryId", categoryId).queryOne();
          String categoryNameRec = suppPartyCategoryGroup.getString("categoryName");
          if (categoryName.equals(categoryNameRec)) {
            checkDuplicate = true;
            continue;
          }
          Long seqNumFromRecord = (Long) targetSupplierPartyCategoryGroup.get("sequenceNum");
          if (seqNumFromRecord > sequenceNum) {
            sequenceNum = seqNumFromRecord;
          }
        }
        //increment sequence number
        sequenceNum++;
        if (!checkDuplicate) {
          productCategoryId = delegator.getNextSeqId("ProductCategory");
          GenericValue productCategoryGroup = delegator.makeValue("ProductCategory");
          productCategoryGroup.set("productCategoryId", productCategoryId);
          productCategoryGroup.set("categoryName", categoryName);
          productCategoryGroup.set("description", description);

          delegator.create(productCategoryGroup);

          GenericValue suppPartyCategoryGroup = delegator.makeValue("SupplierProductCategory");
          suppPartyCategoryGroup.set("productCategoryId", productCategoryId);
          suppPartyCategoryGroup.set("supplierPartyId", supplierPartyId);
          suppPartyCategoryGroup.set("sequenceNum", sequenceNum);

          delegator.create(suppPartyCategoryGroup);
        }
      }


    } catch (GenericEntityException e) {
      Debug.logError(e, module);
      return ServiceUtil.returnError(e.getMessage());
    }
    return serviceResult;
  }

  public static Map<String, Object> fsdCreateProductTerm(DispatchContext dctx,
      Map<String, Object> context) throws GenericEntityException, GenericServiceException {
    Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
    Delegator delegator = dctx.getDelegator();
    LocalDispatcher dispatcher = dctx.getDispatcher();
    GenericValue userLogin = (GenericValue) context.get("userLogin");
    String productId = (String) context.get("productId");
    String statusId = (String) context.get("statusId");
    String productContentTypeId = (String) context.get("productContentTypeId");
    String termName = (String) context.get("termName");
    String termText = (String) context.get("termText");
    GenericValue sysUserLogin = null;
    // createDataResourceAndText
    Map<String, Object> createDataResourceCtx = UtilMisc.toMap(
        "userLogin", userLogin,
        "statusId", "CTNT_PUBLISHED",
        "dataResourceTypeId", "ELECTRONIC_TEXT");
    Map<String, Object> createDataResourceAndTextResult = null;
    createDataResourceAndTextResult = dispatcher
        .runSync("extCreateDataResource", createDataResourceCtx);
    if (!ServiceUtil.isSuccess(createDataResourceAndTextResult)) {
      return createDataResourceAndTextResult;
    }
    String dataResourceId = (String) createDataResourceAndTextResult.get("dataResourceId");

    // createElectronicText
    Map<String, Object> createTermTextCtx = UtilMisc.toMap(
        "userLogin", userLogin,
        "textData", termText,
        "dataResourceId", dataResourceId,
        "dataResourceTypeId", "ELECTRONIC_TEXT");
    Map<String, Object> createTermTextCtxResult = null;
    createTermTextCtxResult = dispatcher.runSync("createElectronicText", createTermTextCtx);
    if (!ServiceUtil.isSuccess(createTermTextCtxResult)) {
      return createTermTextCtxResult;
    }
    // extCreateContent
    Map<String, Object> extCreateContentCtx = UtilMisc.toMap(
        "userLogin", userLogin,
        "dataResourceId", dataResourceId,
        "statusId", statusId,
        "contentTypeId", "TERMS_AND_CONDS",
        "description", termName);
    Map<String, Object> extCreateContentResult = null;
    extCreateContentResult = dispatcher.runSync("extCreateContent", extCreateContentCtx);
    if (!ServiceUtil.isSuccess(extCreateContentResult)) {
      return extCreateContentResult;
    }
    String contentId = (String) extCreateContentResult.get("contentId");
    // createProductContent
    sysUserLogin = delegator.findOne("UserLogin", UtilMisc.toMap("userLoginId", "system"), true);
    Map<String, Object> createProductContentCtx = UtilMisc.toMap(
        "userLogin", sysUserLogin,
        "productId", productId,
        "contentId", contentId,
        "productContentTypeId", productContentTypeId);
    Map<String, Object> createProductContentResult = null;

    createProductContentResult = dispatcher
        .runSync("createProductContent", createProductContentCtx);
    if (!ServiceUtil.isSuccess(createProductContentResult)) {
      return createProductContentResult;
    }

    return serviceResult;
  }

  public static Map<String, Object> fsdCreatePartyTerm(DispatchContext dctx,
      Map<String, Object> context) throws GenericEntityException, GenericServiceException {
    Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
    Delegator delegator = dctx.getDelegator();
    LocalDispatcher dispatcher = dctx.getDispatcher();
    GenericValue userLogin = (GenericValue) context.get("userLogin");
    String partyId = (String) context.get("partyId");
    String statusId = (String) context.get("statusId");
    String contentTypeId = (String) context.get("contentTypeId");
    String partyContentTypeId = (String) context.get("partyContentTypeId");
    String termName = (String) context.get("termName");
    String termText = (String) context.get("termText");
    String createdByUserLogin = (String) context.get("createdByUserLogin");
    GenericValue sysUserLogin = null;

    if (UtilValidate.isEmpty(contentTypeId)) {
      contentTypeId = "TERMS_AND_CONDS";
    }

    // createDataResourceAndText
    sysUserLogin = delegator.findOne("UserLogin", UtilMisc.toMap("userLoginId", "system"), true);
    Map<String, Object> createDataResourceCtx = UtilMisc.toMap(
        "userLogin", userLogin,
        "statusId", "CTNT_PUBLISHED",
        "dataResourceTypeId", "ELECTRONIC_TEXT");
    Map<String, Object> createDataResourceAndTextResult = null;

    createDataResourceAndTextResult = dispatcher
        .runSync("extCreateDataResource", createDataResourceCtx);
    if (!ServiceUtil.isSuccess(createDataResourceAndTextResult)) {
      return createDataResourceAndTextResult;
    }
    String dataResourceId = (String) createDataResourceAndTextResult.get("dataResourceId");

    // createElectronicText
    Map<String, Object> createTermTextCtx = UtilMisc.toMap(
        "userLogin", userLogin,
        "textData", termText,
        "dataResourceId", dataResourceId,
        "dataResourceTypeId", "ELECTRONIC_TEXT");
    Map<String, Object> createTermTextCtxResult = null;

    createTermTextCtxResult = dispatcher.runSync("createElectronicText", createTermTextCtx);
    if (!ServiceUtil.isSuccess(createTermTextCtxResult)) {
      return createTermTextCtxResult;
    }

    // extCreateContent
    Map<String, Object> extCreateContentCtx = UtilMisc.toMap(
        "userLogin", sysUserLogin,
        "dataResourceId", dataResourceId,
        "statusId", statusId,
        "contentTypeId", contentTypeId,
        "description", termName);
    Map<String, Object> extCreateContentResult = null;
    extCreateContentResult = dispatcher.runSync("extCreateContent", extCreateContentCtx);
    if (!ServiceUtil.isSuccess(extCreateContentResult)) {
      return extCreateContentResult;
    }
    String contentId = (String) extCreateContentResult.get("contentId");

    Map<String, Object> createPartyContentCtx = UtilMisc.toMap(
        "userLogin", sysUserLogin,
        "partyId", partyId,
        "contentId", contentId,
        "createdByUserLogin", createdByUserLogin,
        "partyContentTypeId", partyContentTypeId);
    Map<String, Object> createPartyContentResult = null;

    createPartyContentResult = dispatcher.runSync("createPartyContent", createPartyContentCtx);
    if (!ServiceUtil.isSuccess(createPartyContentResult)) {
      return createPartyContentResult;
    }
    serviceResult.put("contentId", contentId);
    return serviceResult;
  }

  public static Map<String, Object> fsdUpdatePartyContent(DispatchContext dctx,
      Map<String, Object> context) throws GenericEntityException, GenericServiceException {
    Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
    Delegator delegator = dctx.getDelegator();
    LocalDispatcher dispatcher = dctx.getDispatcher();
    GenericValue userLogin = (GenericValue) context.get("userLogin");
    String partyId = (String) context.get("partyId");
    String contentId = (String) context.get("contentId");
    String partyContentTypeId = (String) context.get("partyContentTypeId");

    try {
      GenericValue partyContent = EntityQuery.use(delegator).from("PartyContent")
          .where("partyId", partyId, "contentId", contentId, "partyContentTypeId",
              partyContentTypeId).queryFirst();
      if (UtilValidate.isNotEmpty(partyContent)) {
        partyContent.set("thruDate", UtilDateTime.nowTimestamp());
        delegator.store(partyContent);
      }
    } catch (GenericEntityException e) {
      Debug.logError(e, module);
      return ServiceUtil.returnError(e.getMessage());
    }
    return serviceResult;
  }
}
