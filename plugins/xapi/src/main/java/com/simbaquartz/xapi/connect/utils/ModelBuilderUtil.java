/*
 *
 *  * *****************************************************************************************
 *  *  Copyright (c) SimbaQuartz  2016. - All Rights Reserved                                 *
 *  *  Unauthorized copying of this file, via any medium is strictly prohibited               *
 *  *  Proprietary and confidential                                                           *
 *  *  Written by Mandeep Sidhu <mandeep.sidhu@fidelissd.com>,  June, 2017                    *
 *  * ****************************************************************************************
 *
 */

package com.simbaquartz.xapi.connect.utils;

import static com.simbaquartz.xapi.connect.api.BaseApiService.delegator;
import static com.simbaquartz.xapi.connect.api.BaseApiService.dispatcher;

import com.simbaquartz.xapi.connect.api.security.LoggedInUser;
import com.simbaquartz.xapi.connect.models.Category;
import com.simbaquartz.xapi.connect.models.Discount;
import com.simbaquartz.xapi.connect.models.GetQuoteResponse;
import com.simbaquartz.xapi.connect.models.Invoice;
import com.simbaquartz.xapi.connect.models.ProjectItem;
import com.simbaquartz.xapi.connect.models.Quote;
import com.simbaquartz.xapi.connect.models.QuoteContact;
import com.simbaquartz.xapi.connect.models.QuoteItemShipGroup;
import com.simbaquartz.xapi.connect.models.QuoteTag;
import com.simbaquartz.xapi.connect.models.SalesPipeline;
import com.simbaquartz.xapi.connect.models.SearchQuoteResponse;
import com.simbaquartz.xapi.connect.models.Term;
import com.simbaquartz.xapi.connect.models.UrlHandle;
import com.simbaquartz.xapi.connect.models.collection.CollectionSearch;
import com.simbaquartz.xapi.connect.models.common.Color;
import com.simbaquartz.xapi.connect.models.common.DataCategory;
import com.simbaquartz.xapi.connect.models.common.Progress;
import com.simbaquartz.xapi.connect.models.note.Note;
import com.simbaquartz.xapi.connect.models.product.Product;
import com.simbaquartz.xapi.connect.models.product.ProductAttachment;
import com.simbaquartz.xapi.connect.models.product.ProductConfigurationOption;
import com.simbaquartz.xapi.connect.models.product.ProductConfigurationProduct;
import com.simbaquartz.xapi.connect.models.product.ProductConfigurations;
import com.simbaquartz.xapi.connect.models.product.ProductImage;
import com.simbaquartz.xapi.connect.models.quote.QuoteAttachment;
import com.simbaquartz.xapi.connect.models.supplier.Supplier;
import com.fidelissd.zcp.xcommon.collections.FastList;
import com.fidelissd.zcp.xcommon.collections.FastMap;
import com.fidelissd.zcp.xcommon.models.Phone;
import com.fidelissd.zcp.xcommon.models.WebAddress;
import com.fidelissd.zcp.xcommon.models.WebAddressModelBuilder;
import com.fidelissd.zcp.xcommon.models.account.User;
import com.fidelissd.zcp.xcommon.models.client.Employee;
import com.fidelissd.zcp.xcommon.models.company.Company;
import com.fidelissd.zcp.xcommon.models.email.EmailAddress;
import com.fidelissd.zcp.xcommon.models.email.EmailAddressModelBuilder;
import com.fidelissd.zcp.xcommon.models.geo.PostalAddress;
import com.fidelissd.zcp.xcommon.models.geo.Timezone;
import com.fidelissd.zcp.xcommon.models.geo.builder.GeoModelBuilder;
import com.fidelissd.zcp.xcommon.models.media.File;
import com.fidelissd.zcp.xcommon.models.people.Organization;
import com.fidelissd.zcp.xcommon.models.people.Person;
import com.fidelissd.zcp.xcommon.models.search.SearchResults;
import com.fidelissd.zcp.xcommon.services.CommonHelper;
import com.fidelissd.zcp.xcommon.services.contact.EmailTypesEnum;
import com.fidelissd.zcp.xcommon.util.AxUtilFormat;
import com.fidelissd.zcp.xcommon.util.ColorUtils;
import com.fidelissd.zcp.xcommon.util.TimestampUtil;
import com.fidelissd.zcp.xcommon.util.hierarchy.HierarchyUtils;
import com.simbaquartz.xgeo.utils.GeoUtil;
import com.simbaquartz.xparty.helpers.AxPartyHelper;
import com.simbaquartz.xparty.helpers.PartyPostalAddressHelper;
import com.simbaquartz.xparty.services.person.PersonServices;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.StringUtil;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericDelegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.product.product.ProductWorker;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;

/** Created by mande on 6/5/2017. */
public class ModelBuilderUtil {

  private static final String module = ModelBuilderUtil.class.getName();

  public static Product prepareProductModel(GenericValue productValue) {
    Product product = new Product();
    if (UtilValidate.isNotEmpty(productValue)) {
      product.setProductId(productValue.getString("productId"));
      product.setName(productValue.getString("productName"));
    }

    return product;
  }

  /**
   * Returns list of Product model.
   *
   * @param products List of products value in GenericValue format
   * @return List of Product model.
   */
  public static List<Product> prepareProductModel(List<GenericValue> products) {
    List<Product> productList = FastList.newInstance();
    if (UtilValidate.isNotEmpty(products)) {
      for (GenericValue product : products) {
        productList.add(prepareProductModel(product));
      }
    }

    return productList;
  }

  public static Category prepareCategoryModelForProducts(GenericValue categoryValue) {
    Category category = new Category();
    if (UtilValidate.isNotEmpty(categoryValue)) {
      category.setId(categoryValue.getString("productCategoryId"));
      category.setName(categoryValue.getString("categoryName"));
      category.setDescription(categoryValue.getString("description"));
    }

    return category;
  }

  public static Category prepareCategoryModelForProjects(GenericValue categoryValue) {
    Category category = new Category();
    if (UtilValidate.isNotEmpty(categoryValue)) {
      category.setId(categoryValue.getString("productCategoryId"));
      category.setName(categoryValue.getString("categoryName"));
      category.setDescription(categoryValue.getString("description"));
    }

    return category;
  }

  /**
   * @param projectCategory GenericValue of entity name ProjectCategoryAndColor
   * @param dispatcher
   * @param delegator
   * @param loggedInUser
   * @return
   */
  public static DataCategory prepareCategoryModelForProjects(
      GenericValue projectCategory,
      LocalDispatcher dispatcher,
      GenericDelegator delegator,
      LoggedInUser loggedInUser) {
    DataCategory category = new DataCategory();
    if (UtilValidate.isNotEmpty(projectCategory)) {
      category.setId(projectCategory.getString("categoryId"));
      String categoryName = projectCategory.getString("categoryOverride");
      //      If category name is empty try fetching from DataCategory.
      if (UtilValidate.isEmpty(categoryName)) {
        categoryName = projectCategory.getString("categoryName");
      }
      category.setName(categoryName);

      Color color = Color.builder().build();
      color.setId(projectCategory.getString("colorId"));
      color.setBackground(projectCategory.getString("backgroundColor"));
      color.setForeground(projectCategory.getString("foregroundColor"));
      category.setColor(color);
    }

    return category;
  }

  public static User prepareUserModel(
      GenericDelegator delegator, String partyId, LoggedInUser loggedInUser) {
    User user = new User();
    Map<String, Object> userDetails = AxPartyHelper.getPartyBasicDetails(delegator, partyId);
    if (UtilValidate.isNotEmpty(userDetails)) {
      user.setId(partyId);
      user.setDisplayName((String) userDetails.get("displayName"));
      user.setEmail((String) userDetails.get("email"));
      user.setPhotoUrl((String) userDetails.get("photoUrl"));

      if (loggedInUser.getPartyId().equals(userDetails.get("id"))) user.setSelf(true);
    }

    return user;
  }

  public static DataCategory prepareDataCategoryModel(Map projectCategory) {
    DataCategory category = new DataCategory();
    if (UtilValidate.isNotEmpty(projectCategory)) {
      category.setId((String) projectCategory.get("dataCategoryId"));
      category.setName((String) projectCategory.get("categoryName"));

      Color color = Color.builder().build();
      color.setBackground((String) projectCategory.get("categoryColor"));
      category.setColor(color);
    }

    return category;
  }

  /**
   * Returns list of Category model.
   *
   * @param categories List of products value in GenericValue format
   * @return List of Product model.
   */
  public static List<Category> prepareCategoryModelForProducts(List<GenericValue> categories) {
    List<Category> categoriesList = FastList.newInstance();
    if (UtilValidate.isNotEmpty(categories)) {
      for (GenericValue category : categories) {
        categoriesList.add(prepareCategoryModelForProducts(category));
      }
    }

    return categoriesList;
  }



  public static List<Phone> phoneModel(List<Map> phoneMaps) {
    List<Phone> phoneList = FastList.newInstance();
    for (Map phoneMap : phoneMaps) {
      Phone phone = new Phone();
      phone.setPhone((String) phoneMap.get("contactNumber"));
      phone.setCountryCode((String) phoneMap.get("countryCode"));
      phone.setAreaCode((String) phoneMap.get("areaCode"));
      phone.setPhoneFormatted((String) phoneMap.get("formattedPhoneNumberInUSFormat"));
      phoneList.add(phone);
    }
    return phoneList;
  }

  public static List<Phone> preparePhoneModel(List<Map> phoneMaps) {
    List<Phone> phoneList = FastList.newInstance();
    for (Map phoneMap : phoneMaps) {
      Phone phone = new Phone();
      if (UtilValidate.isNotEmpty(phoneMap.get("contactNumber"))) {
        phone.setPhone((String) phoneMap.get("contactNumber"));
      }
      if (UtilValidate.isNotEmpty(phoneMap.get("countryCode"))) {
        phone.setCountryCode((String) phoneMap.get("countryCode"));
      }
      if (UtilValidate.isNotEmpty(phoneMap.get("areaCode"))) {
        phone.setAreaCode((String) phoneMap.get("areaCode"));
      }
      if (UtilValidate.isNotEmpty(phoneMap.get("extension"))) {
        phone.setExtension((String) phoneMap.get("extension"));
      }
      if (UtilValidate.isNotEmpty(phoneMap.get("formattedPhoneNumberInUSFormat"))) {
        phone.setPhoneFormatted((String) phoneMap.get("formattedPhoneNumberInUSFormat"));
      }
      if (UtilValidate.isNotEmpty(phoneMap.get("contactMechId"))) {
        phone.setId((String) phoneMap.get("contactMechId"));
      }
      phoneList.add(phone);
    }
    return phoneList;
  }

  public static Phone preparePhoneModel(Map phoneMap) {
    Phone phone = new Phone();
    if (UtilValidate.isNotEmpty(phoneMap.get("contactNumber"))) {
      phone.setPhone((String) phoneMap.get("contactNumber"));
    }
    if (UtilValidate.isNotEmpty(phoneMap.get("countryCode"))) {
      phone.setCountryCode((String) phoneMap.get("countryCode"));
    }
    if (UtilValidate.isNotEmpty(phoneMap.get("areaCode"))) {
      phone.setAreaCode((String) phoneMap.get("areaCode"));
    }
    if (UtilValidate.isNotEmpty(phoneMap.get("contactMechId"))) {
      phone.setId((String) phoneMap.get("contactMechId"));
    }

    return phone;
  }


  public static Product prepareProductModel(GenericValue product, LocalDispatcher dispatcher) {

    Product productModel = new Product();
    Delegator delegator = product.getDelegator();

    String productId = null;
    productId = (String) product.get("productId");
    productModel.setProductId(productId);

    String productTypeId = null;
    productTypeId = (String) product.get("productTypeId");
    productModel.setProductType(productTypeId);

    String weightUomId = null;
    weightUomId = (String) product.get("weightUomId");
    productModel.setWeightUnit(weightUomId);

    productModel.setUpdatedAt((Timestamp) product.get("lastUpdatedStamp"));

    String productName = null;
    productName = (String) product.get("productName");
    productModel.setName(productName);

    productModel.setCreatedAt((Timestamp) product.get("createdStamp"));

    String descriptionHtml = null;
    descriptionHtml = (String) product.get("longDescription");
    productModel.setDescriptionHtml(descriptionHtml);

    String brandName = null;
    brandName = (String) product.get("brandName");
    productModel.setVendor(brandName);

    BigDecimal productWeight = null;
    productWeight = product.getBigDecimal("productWeight");
    productModel.setWeight(productWeight);

    Timestamp releaseDate = null;
    releaseDate = (Timestamp) product.get("releaseDate");
    productModel.setPublishedAt(releaseDate);

    Boolean isAvailable = false;
    if ((UtilValidate.isNotEmpty("introductionDate")
        || (UtilValidate.isNotEmpty("introductionDate")
            && UtilValidate.isNotEmpty("salesDiscontinuationDate")))) {
      Boolean isAvailableProduct = ProductWorker.isSellable(product);
      productModel.setIsAvailable(isAvailableProduct);
    }

    List<GenericValue> productPrices = null;
    try {
      productPrices =
          EntityQuery.use(delegator)
              .from("ProductPrice")
              .where("productId", productId)
              .queryList(); // TODO: message to constant conversion
    } catch (GenericEntityException e) {
      Debug.logError("Unable to fetch Product Prics for Given Product", module);
      e.printStackTrace();
    }
    BigDecimal listPrice = null;
    BigDecimal defaultPrice = null;
    BigDecimal compatitivePrice = null;
    for (GenericValue productPrice : productPrices) {
      if (productPrice.getString("productPriceTypeId").equals("LIST_PRICE")) {
        listPrice = (BigDecimal) productPrice.get("price");
      }
      if (productPrice.getString("productPriceTypeId").equals("DEFAULT_PRICE")) {
        defaultPrice = (BigDecimal) productPrice.get("price");
      }
      if (productPrice.getString("productPriceTypeId").equals("COMPETITIVE_PRICE")) {
        compatitivePrice = (BigDecimal) productPrice.get("price");
      }
    }

    productModel.setPrice(listPrice);
    productModel.setCompareAtPrice(compatitivePrice);

    List<GenericValue> goodIdentifications = FastList.newInstance();
    try {
      goodIdentifications =
          EntityQuery.use(delegator)
              .from("GoodIdentification")
              .where("productId", productId)
              .queryList();
    } catch (GenericEntityException e) {
      e.printStackTrace();
    }
    String sku = null;
    String barcode = null;
    /*for (GenericValue goodIdentification : goodIdentifications) {
      String goodIdentificationTypeId = (String) goodIdentification.get("goodIdentificationTypeId");
      if (goodIdentificationTypeId.equals(AxProductServices.PRODUCT_IDENTIFICATION_TYPE_SKU_ID)) {
        sku = goodIdentification.getString("idValue");
      }
      if (goodIdentificationTypeId.equals(
          AxProductServices.PRODUCT_IDENTIFICATION_TYPE_BARCODE_ID)) {
        barcode = goodIdentification.getString("idValue");
      }
    }*/

    productModel.setSku(sku);

    productModel.setBarcode(barcode);

    String taxable = (String) product.get("taxable");
    if (taxable.equalsIgnoreCase("Y")) {
      productModel.setTaxable(true);
    }
    if (taxable.equalsIgnoreCase("N")) {
      productModel.setTaxable(false);
    }

    // TODO: Find Relations of Following with Product

    String urlHandle = null;
    productModel.setUrlHandle(urlHandle);

    List<ProductImage> images = new ArrayList<ProductImage>();
    productModel.setImages(images);

    List<Product> variants = new ArrayList<Product>();
    productModel.setVariants(variants);

    Integer positionIndex = null;
    productModel.setPositionIndex(positionIndex);

    return productModel;
  }

  public static Note prepareNoteModel(Map noteMap) {
    Note note = new Note();
    if (UtilValidate.isNotEmpty(noteMap)) {
      note.setId((String) noteMap.get("noteId"));
      note.setNoteInfo((String) noteMap.get("noteInfo"));
      note.setLastModifiedAt((Timestamp) noteMap.get("lastUpdatedStamp"));
      if (UtilValidate.isNotEmpty(noteMap.get("noteName"))) {
        note.setNoteTitle((String) noteMap.get("noteName"));
      }

      if (UtilValidate.isNotEmpty(noteMap.get("createdStamp"))) {
        note.setCreatedAt((Timestamp) noteMap.get("createdStamp"));
      }
      if (UtilValidate.isNotEmpty(noteMap.get("createdBy"))) {
      //  note.setCreatedBy((String) noteMap.get("createdBy"));
      }
      if (UtilValidate.isNotEmpty(noteMap.get("noteParty"))) {
        note.setNoteParty((String) noteMap.get("noteParty"));
      }
      if (UtilValidate.isNotEmpty(noteMap.get("quoteId"))) {
        note.setNoteQuoteId((String) noteMap.get("quoteId"));
      }
    }
    return note;
  }

  /** Returns Quote role. */
  public static QuoteContact prepareRoleModel(Map quoteRoleMap) {
    QuoteContact quoteContact = new QuoteContact();
    if (UtilValidate.isNotEmpty(quoteRoleMap)) {
      quoteContact.setId((String) quoteRoleMap.get("quoteId"));
      quoteContact.setId((String) quoteRoleMap.get("partyId"));
      quoteContact.setRoleTypeId((String) quoteRoleMap.get("roleTypeId"));
      quoteContact.setDisplayName((String) quoteRoleMap.get("displayName"));
    }
    return quoteContact;
  }

  /** Returns list of Quote roles. */
  public static List<List<QuoteContact>> prepareQuoteRoleModel(Map quoteRoles) {
    List<List<QuoteContact>> quoteRoleList = FastList.newInstance();
    if (UtilValidate.isNotEmpty(quoteRoles)) {
      if (UtilValidate.isNotEmpty(quoteRoles.get("customerPOCList"))) {
        List<Map> customerRole = (List<Map>) quoteRoles.get("customerPOCList");
        quoteRoleList.add(prepareCustomerPOCModel(customerRole));
      }
      if (UtilValidate.isNotEmpty(quoteRoles.get("supplierPOCList"))) {
        List<Map> supplierRole = (List<Map>) quoteRoles.get("supplierPOCList");
        quoteRoleList.add(prepareSupplierPOCModel(supplierRole));
      }
      if (UtilValidate.isNotEmpty(quoteRoles.get("companyPOCList"))) {
        List<Map> companyRole = (List<Map>) quoteRoles.get("companyPOCList");
        quoteRoleList.add(prepareCompanyPOCModel(companyRole));
      }
    }

    return quoteRoleList;
  }

  public static List<QuoteContact> prepareCustomerPOCModel(List<Map> customerPoCMap) {
    List<QuoteContact> customerRoleList = FastList.newInstance();
    if (UtilValidate.isNotEmpty(customerPoCMap)) {
      for (Map poc : customerPoCMap) {
        QuoteContact quoteContact = new QuoteContact();
        quoteContact.setId((String) poc.get("partyId"));
        quoteContact.setDisplayName((String) poc.get("displayName"));
        quoteContact.setName((String) poc.get("partyName"));
        quoteContact.setRoleTypeId("CUSTOMER_POC");
        /** * TODO--fetch role type from database. * */
        customerRoleList.add(quoteContact);
      }
    }
    return customerRoleList;
  }

  public static List<QuoteContact> prepareSupplierPOCModel(List<Map> supplierPOCMap) {
    List<QuoteContact> supplierRoleList = FastList.newInstance();
    if (UtilValidate.isNotEmpty(supplierPOCMap)) {
      for (Map poc : supplierPOCMap) {
        QuoteContact quoteContact = new QuoteContact();
        quoteContact.setId((String) poc.get("partyId"));
        quoteContact.setName((String) poc.get("partyName"));
        quoteContact.setRoleTypeId("SUPPLIER_POC");
        /** * TODO--fetch role type from database. * */
        quoteContact.setDisplayName((String) poc.get("displayName"));
        supplierRoleList.add(quoteContact);
      }
    }
    return supplierRoleList;
  }

  public static List<QuoteContact> prepareCompanyPOCModel(List<Map> companyPOCMap) {
    List<QuoteContact> companyRoleList = FastList.newInstance();
    if (UtilValidate.isNotEmpty(companyPOCMap)) {
      for (Map poc : companyPOCMap) {
        QuoteContact quoteContact = new QuoteContact();
        quoteContact.setId((String) poc.get("partyId"));
        quoteContact.setName((String) poc.get("partyName"));
        quoteContact.setDisplayName((String) poc.get("displayName"));
        quoteContact.setRoleTypeId("COMPANY_POC");
        /** * TODO--fetch role type from database. * */
        companyRoleList.add(quoteContact);
      }
    }
    return companyRoleList;
  }

  public static List<Person> prepareSupplierContactModel(List<Map> supplierPOCMap) {
    List<Person> supplierRoleList = FastList.newInstance();
    if (UtilValidate.isNotEmpty(supplierPOCMap)) {
      for (Map poc : supplierPOCMap) {
        Person quoteRole = populateQuotePocDetails(poc);
        supplierRoleList.add(quoteRole);
      }
    }
    return supplierRoleList;
  }

  public static List<Person> prepareCustomerContactModel(List<Map> customerPoCMap) {
    List<Person> customerRoleList = FastList.newInstance();
    if (UtilValidate.isNotEmpty(customerPoCMap)) {
      for (Map poc : customerPoCMap) {
        Person quoteRole = populateQuotePocDetails(poc);
        customerRoleList.add(quoteRole);
      }
    }
    return customerRoleList;
  }

  public static List<Person> prepareCompanyContactModel(List<Map> companyPOCMap) {
    List<Person> companyRoleList = FastList.newInstance();
    if (UtilValidate.isNotEmpty(companyPOCMap)) {
      for (Map poc : companyPOCMap) {
        Person quoteRole = populateQuotePocDetails(poc);
        companyRoleList.add(quoteRole);
      }
    }
    return companyRoleList;
  }

  private static Person populateQuotePocDetails(Map poc) {
    Person quoteRole = new Person();
    if (UtilValidate.isNotEmpty(poc.get("partyId"))) {
      quoteRole.setId((String) poc.get("partyId"));
    }
    if (UtilValidate.isNotEmpty(poc.get("partyName"))) {
      quoteRole.setDisplayName((String) poc.get("partyName"));
    }
    if (UtilValidate.isNotEmpty(poc.get("displayName"))) {
      quoteRole.setDisplayName((String) poc.get("displayName"));
    }
    if (UtilValidate.isNotEmpty(poc.get("partyInitials"))) {
      quoteRole.setPartyInitials((String) poc.get("partyInitials"));
    }
    if (UtilValidate.isNotEmpty(poc.get("roleTypeId"))) {
      quoteRole.setRoleTypeId((String) poc.get("roleTypeId"));
    }

    List<Map> postalAddresses = (List<Map>) poc.get("postalAddresses");
    if (UtilValidate.isNotEmpty(postalAddresses)) {
      List<PostalAddress> allAddresses = FastList.newInstance();
      for (Map address : postalAddresses) {
        PostalAddress addressModel = GeoModelBuilder.buildPostalAddress(address);
        allAddresses.add(addressModel);
      }
      quoteRole.setAddresses(allAddresses);
    }

    List<Map> phoneNumbers = (List<Map>) poc.get("phoneNumbers");
    if (UtilValidate.isNotEmpty(phoneNumbers)) {
      List<Phone> phones = preparePhoneModel(phoneNumbers);
      quoteRole.setPhones(phones);
    }

    List<Map> emailAddress = (List<Map>) poc.get("emailAddress");
    if (UtilValidate.isNotEmpty(emailAddress)) {
      List<EmailAddress> emailAddresses = EmailAddressModelBuilder.build(emailAddress);
      quoteRole.setEmailAddress(emailAddresses);
    }

    List<Map> webAddress = (List<Map>) poc.get("webAddress");
    if (UtilValidate.isNotEmpty(webAddress)) {
      List<WebAddress> webAddresses = WebAddressModelBuilder.build(webAddress);
      quoteRole.setWebAddress(webAddresses);
    }

    if (UtilValidate.isNotEmpty(poc.get("publicResourceUrl"))) {
      quoteRole.setPublicResourceUrl(poc.get("publicResourceUrl").toString());
    }

    if (UtilValidate.isNotEmpty(poc.get("thumbNailUrl"))) {
      quoteRole.setThumbNailUrl(poc.get("thumbNailUrl").toString());
    }
    return quoteRole;
  }

  public static Quote prepareQuoteModel(Map quoteMap) {
    Quote quote = new Quote();
    if (UtilValidate.isNotEmpty(quoteMap)) {
      quote.setId((String) quoteMap.get("quoteId"));
      if (UtilValidate.isNotEmpty(quoteMap.get("quoteName"))) {
        quote.setName((String) quoteMap.get("quoteName"));
      }
      quote.setTypeId((String) quoteMap.get("quoteTypeId"));
      quote.setIssueDate((Timestamp) quoteMap.get("issueDate"));
      quote.setStatusId((String) quoteMap.get("statusId"));
      quote.setCurrencyUomId((String) quoteMap.get("currencyUomId"));
      quote.setSalesChannelEnumId((String) quoteMap.get("salesChannelEnumId"));
      quote.setRevenuePercent((BigDecimal) quoteMap.get("revenuePercent"));
      quote.setValidFromDate((Timestamp) quoteMap.get("validFromDate"));
      quote.setValidThruDate((Timestamp) quoteMap.get("validThruDate"));
      quote.setTotal((BigDecimal) quoteMap.get("quoteTotal"));
      quote.setProfitTotal((BigDecimal) quoteMap.get("profitTotal"));

      if (UtilValidate.isNotEmpty(quoteMap.get("quoteQualityScore"))) {
        quote.setQuoteQualityScore((Long) quoteMap.get("quoteQualityScore"));
      }

      /*Customer customer = new Customer();
      if (UtilValidate.isNotEmpty(quoteMap.get("accountName"))) {
        customer.setDisplayName((String) quoteMap.get("accountName"));
      }
      if (UtilValidate.isNotEmpty(quoteMap.get("quoteCustomerPartyId"))) {
        customer.setId((String) quoteMap.get("quoteCustomerPartyId"));
      }*/
      quote.setSupplierId((String) quoteMap.get("quoteSupplierPartyId"));
      if (UtilValidate.isNotEmpty(quoteMap.get("quoteSupplierPartyName"))) {
        quote.setSupplierName((String) quoteMap.get("quoteSupplierPartyName"));
      }

      List<Map> quoteProducts = (List<Map>) quoteMap.get("quoteProducts");
      List<Product> productList = returnProductModel(quoteProducts);
      Map quoteRolesMap = FastMap.newInstance();
      quoteRolesMap.put("customerPOCList", quoteMap.get("quoteCustomerPocInfoList"));
      quoteRolesMap.put("supplierPOCList", quoteMap.get("quoteSupplierPocInfoList"));
      quoteRolesMap.put("companyPOCList", quoteMap.get("quoteCompanyPocInfoList"));
    }
    return quote;
  }

  public static SearchQuoteResponse prepareSearchQuoteResponeModel(Map quoteMap) {
    SearchQuoteResponse quote = new SearchQuoteResponse();
    if (UtilValidate.isNotEmpty(quoteMap)) {
      if (UtilValidate.isNotEmpty(quoteMap.get("quoteId"))) {
        quote.setId((String) quoteMap.get("quoteId"));
      }
      if (UtilValidate.isNotEmpty(quoteMap.get("quoteName"))) {
        quote.setName((String) quoteMap.get("quoteName"));
      }
      if (UtilValidate.isNotEmpty(quoteMap.get("quoteTypeId"))) {
        quote.setTypeId((String) quoteMap.get("quoteTypeId"));
      }
      if (UtilValidate.isNotEmpty(quoteMap.get("issueDate"))) {
        quote.setIssueDate((Timestamp) quoteMap.get("issueDate"));
      }
      if (UtilValidate.isNotEmpty(quoteMap.get("statusId"))) {
        quote.setStatusId((String) quoteMap.get("statusId"));
      }
      if (UtilValidate.isNotEmpty(quoteMap.get("statusName"))) {
        quote.setStatus((String) quoteMap.get("statusName"));
      }
      if (UtilValidate.isNotEmpty(quoteMap.get("currencyUomId"))) {
        quote.setCurrencyUomId((String) quoteMap.get("currencyUomId"));
      }
      if (UtilValidate.isNotEmpty(quoteMap.get("salesChannelEnumId"))) {
        quote.setSalesChannelEnumId((String) quoteMap.get("salesChannelEnumId"));
      }
      if (UtilValidate.isNotEmpty(quoteMap.get("revenuePercent"))) {
        quote.setRevenuePercent((BigDecimal) quoteMap.get("revenuePercent"));
      }
      if (UtilValidate.isNotEmpty(quoteMap.get("validFromDate"))) {
        quote.setValidFromDate((Timestamp) quoteMap.get("validFromDate"));
      }
      if (UtilValidate.isNotEmpty(quoteMap.get("validThruDate"))) {
        quote.setValidThruDate((Timestamp) quoteMap.get("validThruDate"));
      }
      if (UtilValidate.isNotEmpty(quoteMap.get("costTotal"))) {
        quote.setCostTotal((BigDecimal) quoteMap.get("costTotal"));
      }
      if (UtilValidate.isNotEmpty(quoteMap.get("quoteTotal"))) {
        quote.setTotal((BigDecimal) quoteMap.get("quoteTotal"));
      }
      if (UtilValidate.isNotEmpty(quoteMap.get("profitTotal"))) {
        quote.setProfitTotal((BigDecimal) quoteMap.get("profitTotal"));
      }
      if (UtilValidate.isNotEmpty(quoteMap.get("quoteSolicitationNumber"))) {
        quote.setSolicitationNumber(quoteMap.get("quoteSolicitationNumber").toString());
      }

      if (UtilValidate.isNotEmpty(quoteMap.get("quoteQualityScore"))) {
        quote.setQuoteQualityScore((Long) quoteMap.get("quoteQualityScore"));
      }

      Company customer = new Company();
      if (UtilValidate.isNotEmpty(quoteMap.get("accountName"))) {
        customer.setName((String) quoteMap.get("accountName"));
      }
      if (UtilValidate.isNotEmpty(quoteMap.get("quoteCustomerPartyId"))) {
        customer.setId((String) quoteMap.get("quoteCustomerPartyId"));
      }

      List<Map> customerPhoneNumbers = (List<Map>) quoteMap.get("customerPhoneNumbers");
      if (UtilValidate.isNotEmpty(customerPhoneNumbers)) {
        List<Phone> phones = preparePhoneModel(customerPhoneNumbers);
        customer.setPhone(phones);
      }

      List<Map> customerEmailAddress = (List<Map>) quoteMap.get("customerEmailAddress");
      if (UtilValidate.isNotEmpty(customerEmailAddress)) {
        List<EmailAddress> emailAddresses = EmailAddressModelBuilder.build(customerEmailAddress);
        customer.setEmailAddress(emailAddresses);
      }

      GenericValue customerPostalAddress = (GenericValue) quoteMap.get("customerPostalAddresses");
      if (UtilValidate.isNotEmpty(customerPostalAddress)) {
        PostalAddress postalAddresses = GeoModelBuilder.buildPostalAddress(customerPostalAddress);
        customer.setAddress(UtilMisc.toList(postalAddresses));
      }
      quote.setCustomer(customer);

      Company vendor = new Company();
      if (UtilValidate.isNotEmpty(quoteMap.get("quoteSupplierPartyId"))) {
        vendor.setId((String) quoteMap.get("quoteSupplierPartyId"));
      }
      if (UtilValidate.isNotEmpty(quoteMap.get("quoteSupplierPartyName"))) {
        vendor.setName((String) quoteMap.get("quoteSupplierPartyName"));
      }

      List<Map> vendorPhoneNumbers = (List<Map>) quoteMap.get("vendorPhoneNumbers");
      if (UtilValidate.isNotEmpty(vendorPhoneNumbers)) {
        List<Phone> phones = preparePhoneModel(vendorPhoneNumbers);
        vendor.setPhone(phones);
      }

      List<Map> vendorEmailAddress = (List<Map>) quoteMap.get("vendorEmailAddress");
      if (UtilValidate.isNotEmpty(vendorEmailAddress)) {
        List<EmailAddress> emailAddresses = EmailAddressModelBuilder.build(vendorEmailAddress);
        vendor.setEmailAddress(emailAddresses);
      }

      quote.setVendor(vendor);

      List<Map> quoteProducts = (List<Map>) quoteMap.get("quoteProducts");
      List<Product> productList = returnProductModelFromSolr(quoteProducts);
      quote.setProducts(productList);

      List<Map> customerPOCList = (List<Map>) quoteMap.get("quoteCustomerPocInfoList");
      if (UtilValidate.isNotEmpty(customerPOCList)) {
        List<Person> customerContactList = prepareCustomerContactModel(customerPOCList);
        quote.setCustomerContact(customerContactList);
      }

      List<Map> vendorPOCList = (List<Map>) quoteMap.get("quoteSupplierPocInfoList");
      if (UtilValidate.isNotEmpty(vendorPOCList)) {
        List<Person> supplierContactList = prepareSupplierContactModel(vendorPOCList);
        quote.setVendorContact(supplierContactList);
      }

      List<Map> companyPOCList = (List<Map>) quoteMap.get("quoteCompanyPocInfoList");
      if (UtilValidate.isNotEmpty(companyPOCList)) {
        List<Person> companyContactList = prepareCompanyContactModel(companyPOCList);
        quote.setCompanyContact(companyContactList);
      }

      // Created & Last updated details
      if (UtilValidate.isNotEmpty(quoteMap.get("createdByPerson"))) {
        quote.setCreatedBy((Person) quoteMap.get("createdByPerson"));
      }

      if (UtilValidate.isNotEmpty(quoteMap.get("updatedByPerson"))) {
        quote.setLastUpdatedBy((Person) quoteMap.get("updatedByPerson"));
      }

      if (UtilValidate.isNotEmpty(quoteMap.get("quoteCreatedDate"))) {
        quote.setCreatedAt((Timestamp) quoteMap.get("quoteCreatedDate"));
      }

      if (UtilValidate.isNotEmpty(quoteMap.get("quoteLastModifiedDate"))) {
        quote.setUpdatedAt((Timestamp) quoteMap.get("quoteLastModifiedDate"));
      }
    }
    return quote;
  }

  public static GetQuoteResponse prepareGetQuoteResponseModel(Map quoteMap) {
    GetQuoteResponse quote = new GetQuoteResponse();
    if (UtilValidate.isNotEmpty(quoteMap)) {

      // quote basic details
      if (UtilValidate.isNotEmpty(quoteMap.get("quoteId"))) {
        quote.setId((String) quoteMap.get("quoteId"));
      }
      if (UtilValidate.isNotEmpty(quoteMap.get("quoteName"))) {
        quote.setName((String) quoteMap.get("quoteName"));
      }
      if (UtilValidate.isNotEmpty(quoteMap.get("quoteTypeId"))) {
        quote.setTypeId((String) quoteMap.get("quoteTypeId"));
      }
      if (UtilValidate.isNotEmpty(quoteMap.get("issueDate"))) {
        quote.setIssueDate((Timestamp) quoteMap.get("issueDate"));
      }
      if (UtilValidate.isNotEmpty(quoteMap.get("statusId"))) {
        quote.setStatusId((String) quoteMap.get("statusId"));
      }
      if (UtilValidate.isNotEmpty(quoteMap.get("currencyUomId"))) {
        quote.setCurrencyUomId((String) quoteMap.get("currencyUomId"));
      }
      if (UtilValidate.isNotEmpty(quoteMap.get("salesChannelEnumId"))) {
        quote.setSalesChannelEnumId((String) quoteMap.get("salesChannelEnumId"));
      }
      if (UtilValidate.isNotEmpty(quoteMap.get("revenuePercent"))) {
        quote.setRevenuePercent((BigDecimal) quoteMap.get("revenuePercent"));
      }
      if (UtilValidate.isNotEmpty(quoteMap.get("validFromDate"))) {
        quote.setValidFromDate((Timestamp) quoteMap.get("validFromDate"));
      }
      if (UtilValidate.isNotEmpty(quoteMap.get("validThruDate"))) {
        quote.setValidThruDate((Timestamp) quoteMap.get("validThruDate"));
      }
      if (UtilValidate.isNotEmpty(quoteMap.get("costTotal"))) {
        quote.setCostTotal((BigDecimal) quoteMap.get("costTotal"));
      }
      if (UtilValidate.isNotEmpty(quoteMap.get("quoteTotal"))) {
        quote.setTotal((BigDecimal) quoteMap.get("quoteTotal"));
      }
      if (UtilValidate.isNotEmpty(quoteMap.get("profitTotal"))) {
        quote.setProfitTotal((BigDecimal) quoteMap.get("profitTotal"));
      }
      if (UtilValidate.isNotEmpty(quoteMap.get("quoteSolicitationNumber"))) {
        quote.setSolicitationNumber(quoteMap.get("quoteSolicitationNumber").toString());
      }

      if (UtilValidate.isNotEmpty(quoteMap.get("quoteQualityScore"))) {
        quote.setQuoteQualityScore((Long) quoteMap.get("quoteQualityScore"));
      }

      if (UtilValidate.isNotEmpty(quoteMap.get("quoteStatusName"))) {
        quote.setQuoteStatusName((String) quoteMap.get("quoteStatusName"));
      }

      if (UtilValidate.isNotEmpty(quoteMap.get("quoteTypeName"))) {
        quote.setQuoteTypeName((String) quoteMap.get("quoteTypeName"));
      }

      // Created & Last updated details
      if (UtilValidate.isNotEmpty(quoteMap.get("createdByPerson"))) {
        quote.setCreatedBy((Person) quoteMap.get("createdByPerson"));
      }

      if (UtilValidate.isNotEmpty(quoteMap.get("quoteCreatedDate"))) {
        quote.setCreatedAt((Timestamp) quoteMap.get("quoteCreatedDate"));
      }

      if (UtilValidate.isNotEmpty(quoteMap.get("quoteLastModifiedDate"))) {
        quote.setUpdatedAt((Timestamp) quoteMap.get("quoteLastModifiedDate"));
      }

      if (UtilValidate.isNotEmpty(quoteMap.get("updatedByPerson"))) {
        quote.setLastUpdatedBy((Person) quoteMap.get("updatedByPerson"));
      }

      if (UtilValidate.isNotEmpty(quoteMap.get("form2237Number"))) {
        quote.setForm2237Number((String) quoteMap.get("form2237Number"));
      }

      List<Map> customerShippingLocations =
          (List<Map>) quoteMap.get("quoteCustomerShippingAddresses");
      List<PostalAddress> allCustomerShippingAddresses = FastList.newInstance();
      for (Map address : customerShippingLocations) {
        PostalAddress addressModel = GeoModelBuilder.buildPostalAddress(address);
        allCustomerShippingAddresses.add(addressModel);
      }
      quote.setQuoteCustomerShippingAddresses(allCustomerShippingAddresses);

      List<Map> vendorShippingLocations = (List<Map>) quoteMap.get("quoteVendorShippingAddresses");
      List<PostalAddress> allVendorShippingAddresses = FastList.newInstance();
      for (Map address : vendorShippingLocations) {
        PostalAddress addressModel = GeoModelBuilder.buildPostalAddress(address);
        allVendorShippingAddresses.add(addressModel);
      }
      quote.setQuoteVendorShippingAddresses(allVendorShippingAddresses);

      // customer details
      Map<String, Object> customerDetails = (Map<String, Object>) quoteMap.get("customerDetails");
      if (UtilValidate.isNotEmpty(customerDetails)) {
        Company customer = new Company();
        if (UtilValidate.isNotEmpty(customerDetails.get("partyName"))) {
          customer.setName((String) customerDetails.get("partyName"));
        }
        if (UtilValidate.isNotEmpty(customerDetails.get("thumbNailUrl"))) {
          customer.setPhotoUrl((String) customerDetails.get("thumbNailUrl"));
        }
        if (UtilValidate.isNotEmpty(customerDetails.get("partyId"))) {
          customer.setId((String) customerDetails.get("partyId"));
        }

        List<Map> postalAddresses = (List<Map>) customerDetails.get("postalAddresses");
        List<PostalAddress> allAddresses = FastList.newInstance();
        for (Map address : postalAddresses) {
          PostalAddress addressModel = GeoModelBuilder.buildPostalAddress(address);
          allAddresses.add(addressModel);
        }
        customer.setAddress(allAddresses);

        List<Map> phoneNumbers = (List<Map>) customerDetails.get("phoneNumbers");
        if (UtilValidate.isNotEmpty(phoneNumbers)) {
          List<Phone> phones = preparePhoneModel(phoneNumbers);
          customer.setPhone(phones);
        }

        List<Map> emailAddress = (List<Map>) customerDetails.get("emailAddress");
        if (UtilValidate.isNotEmpty(emailAddress)) {
          List<EmailAddress> emailAddresses = EmailAddressModelBuilder.build(emailAddress);
          customer.setEmailAddress(emailAddresses);
        }

        List<Map> webAddress = (List<Map>) customerDetails.get("webAddress");
        if (UtilValidate.isNotEmpty(webAddress)) {
          List<WebAddress> webAddresses = WebAddressModelBuilder.build(webAddress);
          customer.setWebAddress(webAddresses);
        }
        quote.setCustomer(customer);
      }

      // vendor details
      Map<String, Object> vendorDetails = (Map<String, Object>) quoteMap.get("vendorDetails");
      if (UtilValidate.isNotEmpty(vendorDetails)) {

        Company vendor = new Company();
        if (UtilValidate.isNotEmpty(vendorDetails.get("partyName"))) {
          vendor.setName((String) vendorDetails.get("partyName"));
        }
        if (UtilValidate.isNotEmpty(vendorDetails.get("thumbNailUrl"))) {
          vendor.setPhotoUrl((String) vendorDetails.get("thumbNailUrl"));
        }
        if (UtilValidate.isNotEmpty(vendorDetails.get("partyId"))) {
          vendor.setId((String) vendorDetails.get("partyId"));
        }

        List<Map> postalAddresses = (List<Map>) vendorDetails.get("postalAddresses");
        List<PostalAddress> allAddresses = FastList.newInstance();
        for (Map address : postalAddresses) {
          PostalAddress addressModel = GeoModelBuilder.buildPostalAddress(address);
          allAddresses.add(addressModel);
        }
        vendor.setAddress(allAddresses);

        List<Map> phoneNumbers = (List<Map>) vendorDetails.get("phoneNumbers");
        if (UtilValidate.isNotEmpty(phoneNumbers)) {
          List<Phone> phones = preparePhoneModel(phoneNumbers);
          vendor.setPhone(phones);
        }

        List<Map> emailAddress = (List<Map>) vendorDetails.get("emailAddress");
        if (UtilValidate.isNotEmpty(emailAddress)) {
          List<EmailAddress> emailAddresses = EmailAddressModelBuilder.build(emailAddress);
          vendor.setEmailAddress(emailAddresses);
        }

        List<Map> webAddress = (List<Map>) vendorDetails.get("webAddress");
        if (UtilValidate.isNotEmpty(webAddress)) {
          List<WebAddress> webAddresses = WebAddressModelBuilder.build(webAddress);
          vendor.setWebAddress(webAddresses);
        }
        quote.setVendor(vendor);
      }

      List<Map> quoteProducts = (List<Map>) quoteMap.get("quoteProducts");
      List<Product> productList = returnProductModelFromSolr(quoteProducts);
      quote.setProducts(productList);

      List<Map> customerPOCList = (List<Map>) quoteMap.get("quoteCustomerPocInfoList");
      if (UtilValidate.isNotEmpty(customerPOCList)) {
        List<Person> customerContactList = prepareCustomerContactModel(customerPOCList);
        quote.setCustomerContact(customerContactList);
      }

      List<Map> vendorPOCList = (List<Map>) quoteMap.get("quoteSupplierPocInfoList");
      if (UtilValidate.isNotEmpty(vendorPOCList)) {
        List<Person> supplierContactList = prepareSupplierContactModel(vendorPOCList);
        quote.setVendorContact(supplierContactList);
      }

      List<Map> companyPOCList = (List<Map>) quoteMap.get("quoteCompanyPocInfoList");
      if (UtilValidate.isNotEmpty(companyPOCList)) {
        List<Person> companyContactList = prepareCompanyContactModel(companyPOCList);
        quote.setCompanyContact(companyContactList);
      }

      List<Map> allQuoteNotes = (List<Map>) quoteMap.get("allQuoteNotes");
      List<Note> allNotes = FastList.newInstance();
      for (Map quoteNote : allQuoteNotes) {
        Note note = prepareNoteModel(quoteNote);
        allNotes.add(note);
      }
      quote.setNotes(allNotes);

      List<GenericValue> allQuoteTags = (List<GenericValue>) quoteMap.get("allQuoteTags");
      /*if (UtilValidate.isNotEmpty(allQuoteTags)) {
        List<QuoteTag> quoteTags = returnQuoteTagModel(allQuoteTags);
        quote.setQuoteTags(quoteTags);
      }*/

      // treat adjustment as line item
      GenericValue quoteObj = (GenericValue) quoteMap.get("quoteObj");
      if (UtilValidate.isNotEmpty(quoteObj)) {
        String treatAdjustmentsAsLineItem = quoteObj.getString("treatAdjustmentsAsLineItem");
        if (UtilValidate.isNotEmpty(treatAdjustmentsAsLineItem)
            && "Y".equalsIgnoreCase(treatAdjustmentsAsLineItem)) {
          quote.setTreatAdjustmentsAsLineItem(true);
        }
      }
    }
    return quote;
  }

  /** Returns list of products. */
  public static List<Product> returnProductModel(List<Map> quoteProducts) {
    List<Product> productList = FastList.newInstance();
    if (UtilValidate.isNotEmpty(quoteProducts)) {
      for (Map quoteProduct : quoteProducts) {
        Product product = new Product();
        product.setProductId((String) quoteProduct.get("productId"));
        product.setName((String) quoteProduct.get("productName"));
        productList.add(product);
      }
    }

    return productList;
  }

  /** Returns list of products from solr. */
  public static List<Product> returnProductModelFromSolr(List<Map> quoteProducts) {
    List<Product> productList = FastList.newInstance();
    if (UtilValidate.isNotEmpty(quoteProducts)) {
      for (Map quoteProduct : quoteProducts) {
        Product product = new Product();
        product.setProductId((String) quoteProduct.get("productId"));
        product.setName((String) quoteProduct.get("productName"));
        if (UtilValidate.isNotEmpty(quoteProduct.get("supplierProductId"))) {
          product.setSupplierProductId((String) quoteProduct.get("supplierProductId"));
        }
        if (UtilValidate.isNotEmpty(quoteProduct.get("productQuantity"))) {
          product.setQuantity(new BigDecimal(quoteProduct.get("productQuantity").toString()));
        }
        if (UtilValidate.isNotEmpty(quoteProduct.get("productCost"))) {
          product.setUnitCost(new BigDecimal(quoteProduct.get("productCost").toString()));
        }
        if (UtilValidate.isNotEmpty(quoteProduct.get("productUnitPrice"))) {
          product.setUnitPrice(new BigDecimal(quoteProduct.get("productUnitPrice").toString()));
        }

        productList.add(product);
      }
    }

    return productList;
  }



}
