package org.cfbiz.rest.custom;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.jaxrs.ext.multipart.Multipart;
import org.apache.http.HttpRequest;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.entity.DelegatorFactory;
import org.apache.ofbiz.entity.GenericDelegator;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.service.GenericDispatcherFactory;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceDispatcher;
import org.apache.ofbiz.service.ServiceUtil;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import com.google.gson.*;

import io.swagger.annotations.ApiOperation;

@Path("/ping")
public class PingResource {
	@Context
	HttpHeaders headers;
	
	@Context
	HttpServletRequest request;
	@Context
	HttpServletResponse response;

	// @POST
	// @Produces(MediaType.APPLICATION_JSON)
	// @Path("/testpost")
	// public Response say(@Multipart(value = "person", type = "application/json") List<Object> objects) throws Exception {
	// 	Map m = new HashMap();
	// 	m.put("Test", "1");
	// 	m.put("listofObject", objects);
	// 	return Response.ok(new ObjectMapper().writeValueAsString(m)).build();
	// }

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/listServices")
	public Response get() throws Exception {
		System.out.println("inside the createPerson api");
		Map result = new HashMap();
		GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
		LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
		Set s = dispatcher.getDispatchContext().getAllServiceNames();
		Map inputParams = new HashMap();
		for (Iterator iterator = s.iterator(); iterator.hasNext();) {
			String object = (String) iterator.next();
			inputParams.put(object, dispatcher.getDispatchContext().getModelService(object).getInModelParamList());

		}
		result.put("list", s);
		result.put("params", inputParams);
		BufferedWriter out1 = new BufferedWriter(new FileWriter("/home/rahul_utkoor/Downloads/service_names.txt"));
		BufferedWriter out2 = new BufferedWriter(new FileWriter("/home/rahul_utkoor/Downloads/paramas.txt"));
//		Iterator it1 = s.iterator();
//		while(it1.hasNext()) {
//		    out1.write(it1.next());
//		    out1.newLine();
//		}
		out1.write(String.join("\n", s));
		out1.close();
		
//		for(Map entry : inputParams.entrySet()) {
//			out2.write(entry.getKey() + " , " + entry.getValue());
//		    out2.newLine();
//		}
//		Iterator it2 = inputParams.iterator();
//		while(it2.hasNext()) {
//		    out2.write(it2.next());
//		    out2.newLine();
//		}
		Gson gson = new Gson(); 
		String json = gson.toJson(inputParams);
		out2.write(json);
		out2.close();
		
		return Response.ok(new ObjectMapper().writeValueAsString(result)).build();
	}

	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/createProspect")
	public Response createProspect_(@Multipart(value = "createProspect", required = true, type = MediaType.APPLICATION_JSON) Map params) {
		System.out.println("inside the createProspect api");
		GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
		LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
		Map<String, Object> attributeMap = new HashMap<String, Object>();

		if(params.get("USER_FIRST_NAME") != null)
			attributeMap.put("USER_FIRST_NAME", params.get("USER_FIRST_NAME"));
		if(params.get("USER_LAST_NAME") != null)
			attributeMap.put("USER_LAST_NAME", params.get("USER_LAST_NAME"));
		if(params.get("USER_ADDRESS1") != null)
			attributeMap.put("USER_ADDRESS1", params.get("USER_ADDRESS1"));
		if(params.get("USER_CITY") != null)
			attributeMap.put("USER_CITY", params.get("USER_CITY"));
		if(params.get("USER_POSTAL_CODE") != null)
			attributeMap.put("USER_POSTAL_CODE", params.get("USER_POSTAL_CODE"));
		if(params.get("USER_COUNTRY") != null)
			attributeMap.put("USER_COUNTRY", params.get("USER_COUNTRY"));
		if(params.get("USER_STATE") != null)
			attributeMap.put("USER_STATE", params.get("USER_STATE"));
		if(params.get("USER_EMAIL") != null)
			attributeMap.put("USER_EMAIL", params.get("USER_EMAIL"));
		if(params.get("USERNAME") != null)
			attributeMap.put("USERNAME", params.get("USERNAME"));
		if(params.get("PASSWORD") != null)
			attributeMap.put("PASSWORD", params.get("PASSWORD"));
		if(params.get("CONFIRM_PASSWORD") != null)
			attributeMap.put("CONFIRM_PASSWORD", params.get("CONFIRM_PASSWORD"));
		if(params.get("PRODUCT_STORE_ID") != null)
			attributeMap.put("PRODUCT_STORE_ID", params.get("PRODUCT_STORE_ID"));
		if(params.get("userLogin") != null)
			attributeMap.put("userLogin", params.get("userLogin"));
		else {
			try {
				attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
			} catch (GenericEntityException e) {
				e.printStackTrace();
			}
		}
		try {
			Map result = dispatcher.runSync("restcreateCustomer", attributeMap);
			return Response.ok().entity(result).build();
		} catch(GenericServiceException e) {
			e.printStackTrace();
			return null;
		}
	}

	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/createCustomer")
	public Response createCustomer_(@Multipart(value = "createCustomer", required = true, type = MediaType.APPLICATION_JSON) Map params) {
		System.out.println("inside the createCustomer api");
		GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
		LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
		Map<String, Object> attributeMap = new HashMap<String, Object>();

		if(params.get("USER_FIRST_NAME") != null)
			attributeMap.put("USER_FIRST_NAME", params.get("USER_FIRST_NAME"));
		if(params.get("USER_LAST_NAME") != null)
			attributeMap.put("USER_LAST_NAME", params.get("USER_LAST_NAME"));
		if(params.get("USER_ADDRESS1") != null)
			attributeMap.put("USER_ADDRESS1", params.get("USER_ADDRESS1"));
		if(params.get("USER_CITY") != null)
			attributeMap.put("USER_CITY", params.get("USER_CITY"));
		if(params.get("USER_POSTAL_CODE") != null)
			attributeMap.put("USER_POSTAL_CODE", params.get("USER_POSTAL_CODE"));
		if(params.get("USER_COUNTRY") != null)
			attributeMap.put("USER_COUNTRY", params.get("USER_COUNTRY"));
		if(params.get("USER_STATE") != null)
			attributeMap.put("USER_STATE", params.get("USER_STATE"));
		if(params.get("USER_EMAIL") != null)
			attributeMap.put("USER_EMAIL", params.get("USER_EMAIL"));
		if(params.get("USERNAME") != null)
			attributeMap.put("USERNAME", params.get("USERNAME"));
		if(params.get("PASSWORD") != null)
			attributeMap.put("PASSWORD", params.get("PASSWORD"));
		if(params.get("CONFIRM_PASSWORD") != null)
			attributeMap.put("CONFIRM_PASSWORD", params.get("CONFIRM_PASSWORD"));
		if(params.get("PRODUCT_STORE_ID") != null)
			attributeMap.put("PRODUCT_STORE_ID", params.get("PRODUCT_STORE_ID"));
		if(params.get("userLogin") != null)
			attributeMap.put("userLogin", params.get("userLogin"));
		else {
			try {
				attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
			} catch (GenericEntityException e) {
				e.printStackTrace();
			}
		}
		try {
			Map result = dispatcher.runSync("restcreateCustomer", attributeMap);
			return Response.ok().entity(result).build();
		} catch(GenericServiceException e) {
			e.printStackTrace();
			return null;
		}
	}

	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/createPerson")
	public Response createPerson_(@Multipart(value = "createPerson", required = true, type = MediaType.APPLICATION_JSON) Map params) {
		System.out.println("inside the createPerson api");
		GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
		LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
		Map<String, Object> attributeMap = new HashMap<String, Object>();
		if(params.get("partyId") != null)
			attributeMap.put("partyId", params.get("partyId"));
		if(params.get("salutation") != null)
			attributeMap.put("salutation", params.get("salutation"));
		if(params.get("firstName") != null)
			attributeMap.put("firstName", params.get("firstName"));
		if(params.get("middleName") != null)
			attributeMap.put("middleName", params.get("middleName"));
		if(params.get("lastName") != null)
			attributeMap.put("lastName", params.get("lastName"));
		if(params.get("personalTitle") != null)
			attributeMap.put("personalTitle", params.get("personalTitle"));
		if(params.get("suffix") != null)
			attributeMap.put("suffix", params.get("suffix"));
		if(params.get("nickname") != null)
			attributeMap.put("nickname", params.get("nickname"));
		if(params.get("firstNameLocal") != null)
			attributeMap.put("firstNameLocal", params.get("firstNameLocal"));
		if(params.get("middleNameLocal") != null)
			attributeMap.put("middleNameLocal", params.get("middleNameLocal"));
		if(params.get("lastNameLocal") != null)
			attributeMap.put("lastNameLocal", params.get("lastNameLocal"));
		if(params.get("otherLocal") != null)
			attributeMap.put("otherLocal", params.get("otherLocal"));
		if(params.get("memberId") != null)
			attributeMap.put("memberId", params.get("memberId"));
		if(params.get("gender") != null)
			attributeMap.put("gender", params.get("gender"));
		if(params.get("birthDate") != null)
			attributeMap.put("birthDate", params.get("birthDate"));
		if(params.get("deceasedDate") != null)
			attributeMap.put("deceasedDate", params.get("deceasedDate"));
		if(params.get("height") != null)
			attributeMap.put("height", params.get("height"));
		if(params.get("weight") != null)
			attributeMap.put("weight", params.get("weight"));
		if(params.get("mothersMaidenName") != null)
			attributeMap.put("mothersMaidenName", params.get("mothersMaidenName"));
		if(params.get("maritalStatus") != null)
			attributeMap.put("maritalStatus", params.get("maritalStatus"));
		if(params.get("socialSecurityNumber") != null)
			attributeMap.put("socialSecurityNumber", params.get("socialSecurityNumber"));
		if(params.get("passportNumber") != null)
			attributeMap.put("passportNumber", params.get("passportNumber"));
		if(params.get("passportExpireDate") != null)
			attributeMap.put("passportExpireDate", params.get("passportExpireDate"));
		if(params.get("totalYearsWorkExperience") != null)
			attributeMap.put("totalYearsWorkExperience", params.get("totalYearsWorkExperience"));
		if(params.get("comments") != null)
			attributeMap.put("comments", params.get("comments"));
		if(params.get("employmentStatusEnumId") != null)
			attributeMap.put("employmentStatusEnumId", params.get("employmentStatusEnumId"));
		if(params.get("residenceStatusEnumId") != null)
			attributeMap.put("residenceStatusEnumId", params.get("residenceStatusEnumId"));
		if(params.get("occupation") != null)
			attributeMap.put("occupation", params.get("occupation"));
		if(params.get("yearsWithEmployer") != null)
			attributeMap.put("yearsWithEmployer", params.get("yearsWithEmployer"));
		if(params.get("monthsWithEmployer") != null)
			attributeMap.put("monthsWithEmployer", params.get("monthsWithEmployer"));
		if(params.get("existingCustomer") != null)
			attributeMap.put("existingCustomer", params.get("existingCustomer"));
		if(params.get("cardId") != null)
			attributeMap.put("cardId", params.get("cardId"));
		if(params.get("preferredCurrencyUomId") != null)
			attributeMap.put("preferredCurrencyUomId", params.get("preferredCurrencyUomId"));
		if(params.get("description") != null)
			attributeMap.put("description", params.get("description"));
		if(params.get("externalId") != null)
			attributeMap.put("externalId", params.get("externalId"));
		if(params.get("statusId") != null)
			attributeMap.put("statusId", params.get("statusId"));
		if(params.get("userLogin") != null)
			attributeMap.put("userLogin", params.get("userLogin"));
		else {
			try {
				attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
			} catch (GenericEntityException e) {
				e.printStackTrace();
			}
		}
		if(params.get("login.username") != null)
			attributeMap.put("login.username", params.get("login.username"));
		if(params.get("login.password") != null)
			attributeMap.put("login.password", params.get("login.password"));
		if(params.get("locale") != null)
			attributeMap.put("locale", params.get("locale"));
		if(params.get("timeZone") != null)
			attributeMap.put("timeZone", params.get("timeZone"));
		try {
			Map result = dispatcher.runSync("createPerson", attributeMap);
			return Response.ok().entity(result).build();
		} catch(GenericServiceException e) {
			e.printStackTrace();
			return null;
		}
	}

	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/createEmployee")
	public Response createEmployee_(@Multipart(value = "createEmployee", required = true, type = MediaType.APPLICATION_JSON) Map params) {
		GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
		LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
		Map<String, Object> attributeMap = new HashMap<String, Object>();
		if(params.get("salutation") != null)
			attributeMap.put("salutation", params.get("salutation"));
		if(params.get("firstName") != null)
			attributeMap.put("firstName", params.get("firstName"));
		if(params.get("middleName") != null)
			attributeMap.put("middleName", params.get("middleName"));
		if(params.get("lastName") != null)
			attributeMap.put("lastName", params.get("lastName"));
		if(params.get("personalTitle") != null)
			attributeMap.put("personalTitle", params.get("personalTitle"));
		if(params.get("suffix") != null)
			attributeMap.put("suffix", params.get("suffix"));
		if(params.get("nickname") != null)
			attributeMap.put("nickname", params.get("nickname"));
		if(params.get("firstNameLocal") != null)
			attributeMap.put("firstNameLocal", params.get("firstNameLocal"));
		if(params.get("middleNameLocal") != null)
			attributeMap.put("middleNameLocal", params.get("middleNameLocal"));
		if(params.get("lastNameLocal") != null)
			attributeMap.put("lastNameLocal", params.get("lastNameLocal"));
		if(params.get("otherLocal") != null)
			attributeMap.put("otherLocal", params.get("otherLocal"));
		if(params.get("memberId") != null)
			attributeMap.put("memberId", params.get("memberId"));
		if(params.get("gender") != null)
			attributeMap.put("gender", params.get("gender"));
		if(params.get("birthDate") != null)
			attributeMap.put("birthDate", params.get("birthDate"));
		if(params.get("deceasedDate") != null)
			attributeMap.put("deceasedDate", params.get("deceasedDate"));
		if(params.get("height") != null)
			attributeMap.put("height", params.get("height"));
		if(params.get("weight") != null)
			attributeMap.put("weight", params.get("weight"));
		if(params.get("mothersMaidenName") != null)
			attributeMap.put("mothersMaidenName", params.get("mothersMaidenName"));
		if(params.get("maritalStatus") != null)
			attributeMap.put("maritalStatus", params.get("maritalStatus"));
		if(params.get("socialSecurityNumber") != null)
			attributeMap.put("socialSecurityNumber", params.get("socialSecurityNumber"));
		if(params.get("passportNumber") != null)
			attributeMap.put("passportNumber", params.get("passportNumber"));
		if(params.get("passportExpireDate") != null)
			attributeMap.put("passportExpireDate", params.get("passportExpireDate"));
		if(params.get("totalYearsWorkExperience") != null)
			attributeMap.put("totalYearsWorkExperience", params.get("totalYearsWorkExperience"));
		if(params.get("comments") != null)
			attributeMap.put("comments", params.get("comments"));
		if(params.get("employmentStatusEnumId") != null)
			attributeMap.put("employmentStatusEnumId", params.get("employmentStatusEnumId"));
		if(params.get("residenceStatusEnumId") != null)
			attributeMap.put("residenceStatusEnumId", params.get("residenceStatusEnumId"));
		if(params.get("occupation") != null)
			attributeMap.put("occupation", params.get("occupation"));
		if(params.get("yearsWithEmployer") != null)
			attributeMap.put("yearsWithEmployer", params.get("yearsWithEmployer"));
		if(params.get("monthsWithEmployer") != null)
			attributeMap.put("monthsWithEmployer", params.get("monthsWithEmployer"));
		if(params.get("existingCustomer") != null)
			attributeMap.put("existingCustomer", params.get("existingCustomer"));
		if(params.get("cardId") != null)
			attributeMap.put("cardId", params.get("cardId"));
		if(params.get("toName") != null)
			attributeMap.put("toName", params.get("toName"));
		if(params.get("attnName") != null)
			attributeMap.put("attnName", params.get("attnName"));
		if(params.get("address1") != null)
			attributeMap.put("address1", params.get("address1"));
		if(params.get("address2") != null)
			attributeMap.put("address2", params.get("address2"));
		if(params.get("houseNumber") != null)
			attributeMap.put("houseNumber", params.get("houseNumber"));
		if(params.get("houseNumberExt") != null)
			attributeMap.put("houseNumberExt", params.get("houseNumberExt"));
		if(params.get("directions") != null)
			attributeMap.put("directions", params.get("directions"));
		if(params.get("city") != null)
			attributeMap.put("city", params.get("city"));
		if(params.get("cityGeoId") != null)
			attributeMap.put("cityGeoId", params.get("cityGeoId"));
		if(params.get("postalCode") != null)
			attributeMap.put("postalCode", params.get("postalCode"));
		if(params.get("postalCodeExt") != null)
			attributeMap.put("postalCodeExt", params.get("postalCodeExt"));
		if(params.get("countryGeoId") != null)
			attributeMap.put("countryGeoId", params.get("countryGeoId"));
		if(params.get("stateProvinceGeoId") != null)
			attributeMap.put("stateProvinceGeoId", params.get("stateProvinceGeoId"));
		if(params.get("countyGeoId") != null)
			attributeMap.put("countyGeoId", params.get("countyGeoId"));
		if(params.get("municipalityGeoId") != null)
			attributeMap.put("municipalityGeoId", params.get("municipalityGeoId"));
		if(params.get("postalCodeGeoId") != null)
			attributeMap.put("postalCodeGeoId", params.get("postalCodeGeoId"));
		if(params.get("geoPointId") != null)
			attributeMap.put("geoPointId", params.get("geoPointId"));
		if(params.get("countryCode") != null)
			attributeMap.put("countryCode", params.get("countryCode"));
		if(params.get("areaCode") != null)
			attributeMap.put("areaCode", params.get("areaCode"));
		if(params.get("contactNumber") != null)
			attributeMap.put("contactNumber", params.get("contactNumber"));
		if(params.get("askForName") != null)
			attributeMap.put("askForName", params.get("askForName"));
		if(params.get("emailAddress") != null)
			attributeMap.put("emailAddress", params.get("emailAddress"));
		if(params.get("fromDate") != null)
			attributeMap.put("fromDate", params.get("fromDate"));
		if(params.get("postalAddContactMechPurpTypeId") != null)
			attributeMap.put("postalAddContactMechPurpTypeId", params.get("postalAddContactMechPurpTypeId"));
		if(params.get("userLogin") != null)
			attributeMap.put("userLogin", params.get("userLogin"));
		else {
			try {
				attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
			} catch (GenericEntityException e) {
				e.printStackTrace();
			}
		}
		if(params.get("login.username") != null)
			attributeMap.put("login.username", params.get("login.username"));
		if(params.get("login.password") != null)
			attributeMap.put("login.password", params.get("login.password"));
		if(params.get("locale") != null)
			attributeMap.put("locale", params.get("locale"));
		if(params.get("timeZone") != null)
			attributeMap.put("timeZone", params.get("timeZone"));
		try {
			Map result = dispatcher.runSync("createEmployee", attributeMap);
			return Response.ok().entity(result).build();
		} catch(GenericServiceException e) {
			e.printStackTrace();
			return null;
		}
	}





@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/setPaymentStatus")
public Response setPaymentStatus_(@Multipart(value = "setPaymentStatus", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("paymentId") != null)
		attributeMap.put("paymentId", params.get("paymentId"));
	if(params.get("statusId") != null)
		attributeMap.put("statusId", params.get("statusId"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("setPaymentStatus", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}


@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/removePaymentApplication")
public Response removePaymentApplication_(@Multipart(value = "removePaymentApplication", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("paymentApplicationId") != null)
		attributeMap.put("paymentApplicationId", params.get("paymentApplicationId"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("removePaymentApplication", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/createFinAccount")
public Response createFinAccount_(@Multipart(value = "createFinAccount", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("finAccountId") != null)
		attributeMap.put("finAccountId", params.get("finAccountId"));
	if(params.get("finAccountTypeId") != null)
		attributeMap.put("finAccountTypeId", params.get("finAccountTypeId"));
	if(params.get("statusId") != null)
		attributeMap.put("statusId", params.get("statusId"));
	if(params.get("finAccountName") != null)
		attributeMap.put("finAccountName", params.get("finAccountName"));
	if(params.get("finAccountCode") != null)
		attributeMap.put("finAccountCode", params.get("finAccountCode"));
	if(params.get("finAccountPin") != null)
		attributeMap.put("finAccountPin", params.get("finAccountPin"));
	if(params.get("currencyUomId") != null)
		attributeMap.put("currencyUomId", params.get("currencyUomId"));
	if(params.get("organizationPartyId") != null)
		attributeMap.put("organizationPartyId", params.get("organizationPartyId"));
	if(params.get("ownerPartyId") != null)
		attributeMap.put("ownerPartyId", params.get("ownerPartyId"));
	if(params.get("postToGlAccountId") != null)
		attributeMap.put("postToGlAccountId", params.get("postToGlAccountId"));
	if(params.get("fromDate") != null)
		attributeMap.put("fromDate", params.get("fromDate"));
	if(params.get("thruDate") != null)
		attributeMap.put("thruDate", params.get("thruDate"));
	if(params.get("isRefundable") != null)
		attributeMap.put("isRefundable", params.get("isRefundable"));
	if(params.get("replenishPaymentId") != null)
		attributeMap.put("replenishPaymentId", params.get("replenishPaymentId"));
	if(params.get("replenishLevel") != null)
		attributeMap.put("replenishLevel", params.get("replenishLevel"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("createFinAccount", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/updateFinAccount")
public Response updateFinAccount_(@Multipart(value = "updateFinAccount", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("finAccountId") != null)
		attributeMap.put("finAccountId", params.get("finAccountId"));
	if(params.get("finAccountTypeId") != null)
		attributeMap.put("finAccountTypeId", params.get("finAccountTypeId"));
	if(params.get("statusId") != null)
		attributeMap.put("statusId", params.get("statusId"));
	if(params.get("finAccountName") != null)
		attributeMap.put("finAccountName", params.get("finAccountName"));
	if(params.get("finAccountCode") != null)
		attributeMap.put("finAccountCode", params.get("finAccountCode"));
	if(params.get("finAccountPin") != null)
		attributeMap.put("finAccountPin", params.get("finAccountPin"));
	if(params.get("currencyUomId") != null)
		attributeMap.put("currencyUomId", params.get("currencyUomId"));
	if(params.get("organizationPartyId") != null)
		attributeMap.put("organizationPartyId", params.get("organizationPartyId"));
	if(params.get("ownerPartyId") != null)
		attributeMap.put("ownerPartyId", params.get("ownerPartyId"));
	if(params.get("postToGlAccountId") != null)
		attributeMap.put("postToGlAccountId", params.get("postToGlAccountId"));
	if(params.get("fromDate") != null)
		attributeMap.put("fromDate", params.get("fromDate"));
	if(params.get("thruDate") != null)
		attributeMap.put("thruDate", params.get("thruDate"));
	if(params.get("isRefundable") != null)
		attributeMap.put("isRefundable", params.get("isRefundable"));
	if(params.get("replenishPaymentId") != null)
		attributeMap.put("replenishPaymentId", params.get("replenishPaymentId"));
	if(params.get("replenishLevel") != null)
		attributeMap.put("replenishLevel", params.get("replenishLevel"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("updateFinAccount", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/deleteFinAccount")
public Response deleteFinAccount_(@Multipart(value = "deleteFinAccount", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("finAccountId") != null)
		attributeMap.put("finAccountId", params.get("finAccountId"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("deleteFinAccount", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}


@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/createFinAccountTrans")
public Response createFinAccountTrans_(@Multipart(value = "createFinAccountTrans", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("finAccountTransTypeId") != null)
		attributeMap.put("finAccountTransTypeId", params.get("finAccountTransTypeId"));
	if(params.get("finAccountId") != null)
		attributeMap.put("finAccountId", params.get("finAccountId"));
	if(params.get("partyId") != null)
		attributeMap.put("partyId", params.get("partyId"));
	if(params.get("glReconciliationId") != null)
		attributeMap.put("glReconciliationId", params.get("glReconciliationId"));
	if(params.get("transactionDate") != null)
		attributeMap.put("transactionDate", params.get("transactionDate"));
	if(params.get("entryDate") != null)
		attributeMap.put("entryDate", params.get("entryDate"));
	if(params.get("amount") != null)
		attributeMap.put("amount", params.get("amount"));
	if(params.get("paymentId") != null)
		attributeMap.put("paymentId", params.get("paymentId"));
	if(params.get("orderId") != null)
		attributeMap.put("orderId", params.get("orderId"));
	if(params.get("orderItemSeqId") != null)
		attributeMap.put("orderItemSeqId", params.get("orderItemSeqId"));
	if(params.get("reasonEnumId") != null)
		attributeMap.put("reasonEnumId", params.get("reasonEnumId"));
	if(params.get("comments") != null)
		attributeMap.put("comments", params.get("comments"));
	if(params.get("statusId") != null)
		attributeMap.put("statusId", params.get("statusId"));
	if(params.get("glAccountId") != null)
		attributeMap.put("glAccountId", params.get("glAccountId"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("createFinAccountTrans", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/createFinAccountRole")
public Response createFinAccountRole_(@Multipart(value = "createFinAccountRole", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("finAccountId") != null)
		attributeMap.put("finAccountId", params.get("finAccountId"));
	if(params.get("partyId") != null)
		attributeMap.put("partyId", params.get("partyId"));
	if(params.get("roleTypeId") != null)
		attributeMap.put("roleTypeId", params.get("roleTypeId"));
	if(params.get("fromDate") != null)
		attributeMap.put("fromDate", params.get("fromDate"));
	if(params.get("thruDate") != null)
		attributeMap.put("thruDate", params.get("thruDate"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	if(params.get("fromDate") != null)
		attributeMap.put("fromDate", params.get("fromDate"));
	try {
		Map result = dispatcher.runSync("createFinAccountRole", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/updateFinAccountRole")
public Response updateFinAccountRole_(@Multipart(value = "updateFinAccountRole", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("finAccountId") != null)
		attributeMap.put("finAccountId", params.get("finAccountId"));
	if(params.get("partyId") != null)
		attributeMap.put("partyId", params.get("partyId"));
	if(params.get("roleTypeId") != null)
		attributeMap.put("roleTypeId", params.get("roleTypeId"));
	if(params.get("fromDate") != null)
		attributeMap.put("fromDate", params.get("fromDate"));
	if(params.get("thruDate") != null)
		attributeMap.put("thruDate", params.get("thruDate"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("updateFinAccountRole", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/deleteFinAccountRole")
public Response deleteFinAccountRole_(@Multipart(value = "deleteFinAccountRole", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("finAccountId") != null)
		attributeMap.put("finAccountId", params.get("finAccountId"));
	if(params.get("partyId") != null)
		attributeMap.put("partyId", params.get("partyId"));
	if(params.get("roleTypeId") != null)
		attributeMap.put("roleTypeId", params.get("roleTypeId"));
	if(params.get("fromDate") != null)
		attributeMap.put("fromDate", params.get("fromDate"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("deleteFinAccountRole", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/createFinAccountAuth")
public Response createFinAccountAuth_(@Multipart(value = "createFinAccountAuth", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("finAccountId") != null)
		attributeMap.put("finAccountId", params.get("finAccountId"));
	if(params.get("amount") != null)
		attributeMap.put("amount", params.get("amount"));
	if(params.get("currencyUomId") != null)
		attributeMap.put("currencyUomId", params.get("currencyUomId"));
	if(params.get("authorizationDate") != null)
		attributeMap.put("authorizationDate", params.get("authorizationDate"));
	if(params.get("fromDate") != null)
		attributeMap.put("fromDate", params.get("fromDate"));
	if(params.get("thruDate") != null)
		attributeMap.put("thruDate", params.get("thruDate"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("createFinAccountAuth", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/expireFinAccountAuth")
public Response expireFinAccountAuth_(@Multipart(value = "expireFinAccountAuth", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("finAccountAuthId") != null)
		attributeMap.put("finAccountAuthId", params.get("finAccountAuthId"));
	if(params.get("expireDateTime") != null)
		attributeMap.put("expireDateTime", params.get("expireDateTime"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("expireFinAccountAuth", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}


@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/createFixedAsset")
public Response createFixedAsset_(@Multipart(value = "createFixedAsset", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("fixedAssetId") != null)
		attributeMap.put("fixedAssetId", params.get("fixedAssetId"));
	if(params.get("fixedAssetTypeId") != null)
		attributeMap.put("fixedAssetTypeId", params.get("fixedAssetTypeId"));
	if(params.get("parentFixedAssetId") != null)
		attributeMap.put("parentFixedAssetId", params.get("parentFixedAssetId"));
	if(params.get("instanceOfProductId") != null)
		attributeMap.put("instanceOfProductId", params.get("instanceOfProductId"));
	if(params.get("classEnumId") != null)
		attributeMap.put("classEnumId", params.get("classEnumId"));
	if(params.get("partyId") != null)
		attributeMap.put("partyId", params.get("partyId"));
	if(params.get("roleTypeId") != null)
		attributeMap.put("roleTypeId", params.get("roleTypeId"));
	if(params.get("fixedAssetName") != null)
		attributeMap.put("fixedAssetName", params.get("fixedAssetName"));
	if(params.get("acquireOrderId") != null)
		attributeMap.put("acquireOrderId", params.get("acquireOrderId"));
	if(params.get("acquireOrderItemSeqId") != null)
		attributeMap.put("acquireOrderItemSeqId", params.get("acquireOrderItemSeqId"));
	if(params.get("dateAcquired") != null)
		attributeMap.put("dateAcquired", params.get("dateAcquired"));
	if(params.get("dateLastServiced") != null)
		attributeMap.put("dateLastServiced", params.get("dateLastServiced"));
	if(params.get("dateNextService") != null)
		attributeMap.put("dateNextService", params.get("dateNextService"));
	if(params.get("expectedEndOfLife") != null)
		attributeMap.put("expectedEndOfLife", params.get("expectedEndOfLife"));
	if(params.get("actualEndOfLife") != null)
		attributeMap.put("actualEndOfLife", params.get("actualEndOfLife"));
	if(params.get("productionCapacity") != null)
		attributeMap.put("productionCapacity", params.get("productionCapacity"));
	if(params.get("uomId") != null)
		attributeMap.put("uomId", params.get("uomId"));
	if(params.get("calendarId") != null)
		attributeMap.put("calendarId", params.get("calendarId"));
	if(params.get("serialNumber") != null)
		attributeMap.put("serialNumber", params.get("serialNumber"));
	if(params.get("locatedAtFacilityId") != null)
		attributeMap.put("locatedAtFacilityId", params.get("locatedAtFacilityId"));
	if(params.get("locatedAtLocationSeqId") != null)
		attributeMap.put("locatedAtLocationSeqId", params.get("locatedAtLocationSeqId"));
	if(params.get("salvageValue") != null)
		attributeMap.put("salvageValue", params.get("salvageValue"));
	if(params.get("depreciation") != null)
		attributeMap.put("depreciation", params.get("depreciation"));
	if(params.get("purchaseCost") != null)
		attributeMap.put("purchaseCost", params.get("purchaseCost"));
	if(params.get("purchaseCostUomId") != null)
		attributeMap.put("purchaseCostUomId", params.get("purchaseCostUomId"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	if(params.get("fixedAssetTypeId") != null)
		attributeMap.put("fixedAssetTypeId", params.get("fixedAssetTypeId"));
	try {
		Map result = dispatcher.runSync("createFixedAsset", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/createCostComponentCalc")
public Response createCostComponentCalc_(@Multipart(value = "createCostComponentCalc", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("description") != null)
		attributeMap.put("description", params.get("description"));
	if(params.get("costGlAccountTypeId") != null)
		attributeMap.put("costGlAccountTypeId", params.get("costGlAccountTypeId"));
	if(params.get("offsettingGlAccountTypeId") != null)
		attributeMap.put("offsettingGlAccountTypeId", params.get("offsettingGlAccountTypeId"));
	if(params.get("fixedCost") != null)
		attributeMap.put("fixedCost", params.get("fixedCost"));
	if(params.get("variableCost") != null)
		attributeMap.put("variableCost", params.get("variableCost"));
	if(params.get("perMilliSecond") != null)
		attributeMap.put("perMilliSecond", params.get("perMilliSecond"));
	if(params.get("currencyUomId") != null)
		attributeMap.put("currencyUomId", params.get("currencyUomId"));
	if(params.get("costCustomMethodId") != null)
		attributeMap.put("costCustomMethodId", params.get("costCustomMethodId"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("createCostComponentCalc", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/updateCostComponentCalc")
public Response updateCostComponentCalc_(@Multipart(value = "updateCostComponentCalc", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("costComponentCalcId") != null)
		attributeMap.put("costComponentCalcId", params.get("costComponentCalcId"));
	if(params.get("description") != null)
		attributeMap.put("description", params.get("description"));
	if(params.get("costGlAccountTypeId") != null)
		attributeMap.put("costGlAccountTypeId", params.get("costGlAccountTypeId"));
	if(params.get("offsettingGlAccountTypeId") != null)
		attributeMap.put("offsettingGlAccountTypeId", params.get("offsettingGlAccountTypeId"));
	if(params.get("fixedCost") != null)
		attributeMap.put("fixedCost", params.get("fixedCost"));
	if(params.get("variableCost") != null)
		attributeMap.put("variableCost", params.get("variableCost"));
	if(params.get("perMilliSecond") != null)
		attributeMap.put("perMilliSecond", params.get("perMilliSecond"));
	if(params.get("currencyUomId") != null)
		attributeMap.put("currencyUomId", params.get("currencyUomId"));
	if(params.get("costCustomMethodId") != null)
		attributeMap.put("costCustomMethodId", params.get("costCustomMethodId"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("updateCostComponentCalc", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/removeCostComponentCalc")
public Response removeCostComponentCalc_(@Multipart(value = "removeCostComponentCalc", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("costComponentCalcId") != null)
		attributeMap.put("costComponentCalcId", params.get("costComponentCalcId"));
	if(params.get("description") != null)
		attributeMap.put("description", params.get("description"));
	if(params.get("costGlAccountTypeId") != null)
		attributeMap.put("costGlAccountTypeId", params.get("costGlAccountTypeId"));
	if(params.get("offsettingGlAccountTypeId") != null)
		attributeMap.put("offsettingGlAccountTypeId", params.get("offsettingGlAccountTypeId"));
	if(params.get("fixedCost") != null)
		attributeMap.put("fixedCost", params.get("fixedCost"));
	if(params.get("variableCost") != null)
		attributeMap.put("variableCost", params.get("variableCost"));
	if(params.get("perMilliSecond") != null)
		attributeMap.put("perMilliSecond", params.get("perMilliSecond"));
	if(params.get("currencyUomId") != null)
		attributeMap.put("currencyUomId", params.get("currencyUomId"));
	if(params.get("costCustomMethodId") != null)
		attributeMap.put("costCustomMethodId", params.get("costCustomMethodId"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("removeCostComponentCalc", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}



@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/updateFixedAsset")
public Response updateFixedAsset_(@Multipart(value = "updateFixedAsset", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("fixedAssetId") != null)
		attributeMap.put("fixedAssetId", params.get("fixedAssetId"));
	if(params.get("fixedAssetTypeId") != null)
		attributeMap.put("fixedAssetTypeId", params.get("fixedAssetTypeId"));
	if(params.get("parentFixedAssetId") != null)
		attributeMap.put("parentFixedAssetId", params.get("parentFixedAssetId"));
	if(params.get("instanceOfProductId") != null)
		attributeMap.put("instanceOfProductId", params.get("instanceOfProductId"));
	if(params.get("classEnumId") != null)
		attributeMap.put("classEnumId", params.get("classEnumId"));
	if(params.get("partyId") != null)
		attributeMap.put("partyId", params.get("partyId"));
	if(params.get("roleTypeId") != null)
		attributeMap.put("roleTypeId", params.get("roleTypeId"));
	if(params.get("fixedAssetName") != null)
		attributeMap.put("fixedAssetName", params.get("fixedAssetName"));
	if(params.get("acquireOrderId") != null)
		attributeMap.put("acquireOrderId", params.get("acquireOrderId"));
	if(params.get("acquireOrderItemSeqId") != null)
		attributeMap.put("acquireOrderItemSeqId", params.get("acquireOrderItemSeqId"));
	if(params.get("dateAcquired") != null)
		attributeMap.put("dateAcquired", params.get("dateAcquired"));
	if(params.get("dateLastServiced") != null)
		attributeMap.put("dateLastServiced", params.get("dateLastServiced"));
	if(params.get("dateNextService") != null)
		attributeMap.put("dateNextService", params.get("dateNextService"));
	if(params.get("expectedEndOfLife") != null)
		attributeMap.put("expectedEndOfLife", params.get("expectedEndOfLife"));
	if(params.get("actualEndOfLife") != null)
		attributeMap.put("actualEndOfLife", params.get("actualEndOfLife"));
	if(params.get("productionCapacity") != null)
		attributeMap.put("productionCapacity", params.get("productionCapacity"));
	if(params.get("uomId") != null)
		attributeMap.put("uomId", params.get("uomId"));
	if(params.get("calendarId") != null)
		attributeMap.put("calendarId", params.get("calendarId"));
	if(params.get("serialNumber") != null)
		attributeMap.put("serialNumber", params.get("serialNumber"));
	if(params.get("locatedAtFacilityId") != null)
		attributeMap.put("locatedAtFacilityId", params.get("locatedAtFacilityId"));
	if(params.get("locatedAtLocationSeqId") != null)
		attributeMap.put("locatedAtLocationSeqId", params.get("locatedAtLocationSeqId"));
	if(params.get("salvageValue") != null)
		attributeMap.put("salvageValue", params.get("salvageValue"));
	if(params.get("depreciation") != null)
		attributeMap.put("depreciation", params.get("depreciation"));
	if(params.get("purchaseCost") != null)
		attributeMap.put("purchaseCost", params.get("purchaseCost"));
	if(params.get("purchaseCostUomId") != null)
		attributeMap.put("purchaseCostUomId", params.get("purchaseCostUomId"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	if(params.get("fixedAssetTypeId") != null)
		attributeMap.put("fixedAssetTypeId", params.get("fixedAssetTypeId"));
	try {
		Map result = dispatcher.runSync("updateFixedAsset", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/addFixedAssetProduct")
public Response addFixedAssetProduct_(@Multipart(value = "addFixedAssetProduct", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("fixedAssetId") != null)
		attributeMap.put("fixedAssetId", params.get("fixedAssetId"));
	if(params.get("productId") != null)
		attributeMap.put("productId", params.get("productId"));
	if(params.get("fixedAssetProductTypeId") != null)
		attributeMap.put("fixedAssetProductTypeId", params.get("fixedAssetProductTypeId"));
	if(params.get("fromDate") != null)
		attributeMap.put("fromDate", params.get("fromDate"));
	if(params.get("thruDate") != null)
		attributeMap.put("thruDate", params.get("thruDate"));
	if(params.get("comments") != null)
		attributeMap.put("comments", params.get("comments"));
	if(params.get("sequenceNum") != null)
		attributeMap.put("sequenceNum", params.get("sequenceNum"));
	if(params.get("quantity") != null)
		attributeMap.put("quantity", params.get("quantity"));
	if(params.get("quantityUomId") != null)
		attributeMap.put("quantityUomId", params.get("quantityUomId"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	if(params.get("fromDate") != null)
		attributeMap.put("fromDate", params.get("fromDate"));
	try {
		Map result = dispatcher.runSync("addFixedAssetProduct", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/updateFixedAssetProduct")
public Response updateFixedAssetProduct_(@Multipart(value = "updateFixedAssetProduct", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("fixedAssetId") != null)
		attributeMap.put("fixedAssetId", params.get("fixedAssetId"));
	if(params.get("productId") != null)
		attributeMap.put("productId", params.get("productId"));
	if(params.get("fixedAssetProductTypeId") != null)
		attributeMap.put("fixedAssetProductTypeId", params.get("fixedAssetProductTypeId"));
	if(params.get("fromDate") != null)
		attributeMap.put("fromDate", params.get("fromDate"));
	if(params.get("thruDate") != null)
		attributeMap.put("thruDate", params.get("thruDate"));
	if(params.get("comments") != null)
		attributeMap.put("comments", params.get("comments"));
	if(params.get("sequenceNum") != null)
		attributeMap.put("sequenceNum", params.get("sequenceNum"));
	if(params.get("quantity") != null)
		attributeMap.put("quantity", params.get("quantity"));
	if(params.get("quantityUomId") != null)
		attributeMap.put("quantityUomId", params.get("quantityUomId"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("updateFixedAssetProduct", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/removeFixedAssetProduct")
public Response removeFixedAssetProduct_(@Multipart(value = "removeFixedAssetProduct", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("fixedAssetId") != null)
		attributeMap.put("fixedAssetId", params.get("fixedAssetId"));
	if(params.get("productId") != null)
		attributeMap.put("productId", params.get("productId"));
	if(params.get("fixedAssetProductTypeId") != null)
		attributeMap.put("fixedAssetProductTypeId", params.get("fixedAssetProductTypeId"));
	if(params.get("fromDate") != null)
		attributeMap.put("fromDate", params.get("fromDate"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("removeFixedAssetProduct", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/createFixedAssetStdCost")
public Response createFixedAssetStdCost_(@Multipart(value = "createFixedAssetStdCost", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("fixedAssetId") != null)
		attributeMap.put("fixedAssetId", params.get("fixedAssetId"));
	if(params.get("fixedAssetStdCostTypeId") != null)
		attributeMap.put("fixedAssetStdCostTypeId", params.get("fixedAssetStdCostTypeId"));
	if(params.get("fromDate") != null)
		attributeMap.put("fromDate", params.get("fromDate"));
	if(params.get("thruDate") != null)
		attributeMap.put("thruDate", params.get("thruDate"));
	if(params.get("amountUomId") != null)
		attributeMap.put("amountUomId", params.get("amountUomId"));
	if(params.get("amount") != null)
		attributeMap.put("amount", params.get("amount"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("createFixedAssetStdCost", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/updateFixedAssetStdCost")
public Response updateFixedAssetStdCost_(@Multipart(value = "updateFixedAssetStdCost", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("fixedAssetId") != null)
		attributeMap.put("fixedAssetId", params.get("fixedAssetId"));
	if(params.get("fixedAssetStdCostTypeId") != null)
		attributeMap.put("fixedAssetStdCostTypeId", params.get("fixedAssetStdCostTypeId"));
	if(params.get("fromDate") != null)
		attributeMap.put("fromDate", params.get("fromDate"));
	if(params.get("thruDate") != null)
		attributeMap.put("thruDate", params.get("thruDate"));
	if(params.get("amountUomId") != null)
		attributeMap.put("amountUomId", params.get("amountUomId"));
	if(params.get("amount") != null)
		attributeMap.put("amount", params.get("amount"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("updateFixedAssetStdCost", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/cancelFixedAssetStdCost")
public Response cancelFixedAssetStdCost_(@Multipart(value = "cancelFixedAssetStdCost", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("fixedAssetId") != null)
		attributeMap.put("fixedAssetId", params.get("fixedAssetId"));
	if(params.get("fixedAssetStdCostTypeId") != null)
		attributeMap.put("fixedAssetStdCostTypeId", params.get("fixedAssetStdCostTypeId"));
	if(params.get("fromDate") != null)
		attributeMap.put("fromDate", params.get("fromDate"));
	if(params.get("thruDate") != null)
		attributeMap.put("thruDate", params.get("thruDate"));
	if(params.get("amountUomId") != null)
		attributeMap.put("amountUomId", params.get("amountUomId"));
	if(params.get("amount") != null)
		attributeMap.put("amount", params.get("amount"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("cancelFixedAssetStdCost", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/createFixedAssetIdent")
public Response createFixedAssetIdent_(@Multipart(value = "createFixedAssetIdent", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("fixedAssetId") != null)
		attributeMap.put("fixedAssetId", params.get("fixedAssetId"));
	if(params.get("fixedAssetIdentTypeId") != null)
		attributeMap.put("fixedAssetIdentTypeId", params.get("fixedAssetIdentTypeId"));
	if(params.get("idValue") != null)
		attributeMap.put("idValue", params.get("idValue"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("createFixedAssetIdent", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/updateFixedAssetIdent")
public Response updateFixedAssetIdent_(@Multipart(value = "updateFixedAssetIdent", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("fixedAssetId") != null)
		attributeMap.put("fixedAssetId", params.get("fixedAssetId"));
	if(params.get("fixedAssetIdentTypeId") != null)
		attributeMap.put("fixedAssetIdentTypeId", params.get("fixedAssetIdentTypeId"));
	if(params.get("idValue") != null)
		attributeMap.put("idValue", params.get("idValue"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("updateFixedAssetIdent", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/removeFixedAssetIdent")
public Response removeFixedAssetIdent_(@Multipart(value = "removeFixedAssetIdent", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("fixedAssetId") != null)
		attributeMap.put("fixedAssetId", params.get("fixedAssetId"));
	if(params.get("fixedAssetIdentTypeId") != null)
		attributeMap.put("fixedAssetIdentTypeId", params.get("fixedAssetIdentTypeId"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("removeFixedAssetIdent", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/createFixedAssetRegistration")
public Response createFixedAssetRegistration_(@Multipart(value = "createFixedAssetRegistration", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("fixedAssetId") != null)
		attributeMap.put("fixedAssetId", params.get("fixedAssetId"));
	if(params.get("fromDate") != null)
		attributeMap.put("fromDate", params.get("fromDate"));
	if(params.get("thruDate") != null)
		attributeMap.put("thruDate", params.get("thruDate"));
	if(params.get("registrationDate") != null)
		attributeMap.put("registrationDate", params.get("registrationDate"));
	if(params.get("govAgencyPartyId") != null)
		attributeMap.put("govAgencyPartyId", params.get("govAgencyPartyId"));
	if(params.get("registrationNumber") != null)
		attributeMap.put("registrationNumber", params.get("registrationNumber"));
	if(params.get("licenseNumber") != null)
		attributeMap.put("licenseNumber", params.get("licenseNumber"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	if(params.get("fromDate") != null)
		attributeMap.put("fromDate", params.get("fromDate"));
	try {
		Map result = dispatcher.runSync("createFixedAssetRegistration", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/updateFixedAssetRegistration")
public Response updateFixedAssetRegistration_(@Multipart(value = "updateFixedAssetRegistration", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("fixedAssetId") != null)
		attributeMap.put("fixedAssetId", params.get("fixedAssetId"));
	if(params.get("fromDate") != null)
		attributeMap.put("fromDate", params.get("fromDate"));
	if(params.get("thruDate") != null)
		attributeMap.put("thruDate", params.get("thruDate"));
	if(params.get("registrationDate") != null)
		attributeMap.put("registrationDate", params.get("registrationDate"));
	if(params.get("govAgencyPartyId") != null)
		attributeMap.put("govAgencyPartyId", params.get("govAgencyPartyId"));
	if(params.get("registrationNumber") != null)
		attributeMap.put("registrationNumber", params.get("registrationNumber"));
	if(params.get("licenseNumber") != null)
		attributeMap.put("licenseNumber", params.get("licenseNumber"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("updateFixedAssetRegistration", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/deleteFixedAssetRegistration")
public Response deleteFixedAssetRegistration_(@Multipart(value = "deleteFixedAssetRegistration", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("fixedAssetId") != null)
		attributeMap.put("fixedAssetId", params.get("fixedAssetId"));
	if(params.get("fromDate") != null)
		attributeMap.put("fromDate", params.get("fromDate"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("deleteFixedAssetRegistration", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/createFixedAssetMaint")
public Response createFixedAssetMaint_(@Multipart(value = "createFixedAssetMaint", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("fixedAssetId") != null)
		attributeMap.put("fixedAssetId", params.get("fixedAssetId"));
	if(params.get("statusId") != null)
		attributeMap.put("statusId", params.get("statusId"));
	if(params.get("productMaintTypeId") != null)
		attributeMap.put("productMaintTypeId", params.get("productMaintTypeId"));
	if(params.get("productMaintSeqId") != null)
		attributeMap.put("productMaintSeqId", params.get("productMaintSeqId"));
	if(params.get("scheduleWorkEffortId") != null)
		attributeMap.put("scheduleWorkEffortId", params.get("scheduleWorkEffortId"));
	if(params.get("intervalQuantity") != null)
		attributeMap.put("intervalQuantity", params.get("intervalQuantity"));
	if(params.get("intervalUomId") != null)
		attributeMap.put("intervalUomId", params.get("intervalUomId"));
	if(params.get("intervalMeterTypeId") != null)
		attributeMap.put("intervalMeterTypeId", params.get("intervalMeterTypeId"));
	if(params.get("purchaseOrderId") != null)
		attributeMap.put("purchaseOrderId", params.get("purchaseOrderId"));
	if(params.get("estimatedStartDate") != null)
		attributeMap.put("estimatedStartDate", params.get("estimatedStartDate"));
	if(params.get("estimatedCompletionDate") != null)
		attributeMap.put("estimatedCompletionDate", params.get("estimatedCompletionDate"));
	if(params.get("maintTemplateWorkEffortId") != null)
		attributeMap.put("maintTemplateWorkEffortId", params.get("maintTemplateWorkEffortId"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("createFixedAssetMaint", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/updateFixedAssetMaint")
public Response updateFixedAssetMaint_(@Multipart(value = "updateFixedAssetMaint", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("fixedAssetId") != null)
		attributeMap.put("fixedAssetId", params.get("fixedAssetId"));
	if(params.get("maintHistSeqId") != null)
		attributeMap.put("maintHistSeqId", params.get("maintHistSeqId"));
	if(params.get("statusId") != null)
		attributeMap.put("statusId", params.get("statusId"));
	if(params.get("productMaintTypeId") != null)
		attributeMap.put("productMaintTypeId", params.get("productMaintTypeId"));
	if(params.get("productMaintSeqId") != null)
		attributeMap.put("productMaintSeqId", params.get("productMaintSeqId"));
	if(params.get("scheduleWorkEffortId") != null)
		attributeMap.put("scheduleWorkEffortId", params.get("scheduleWorkEffortId"));
	if(params.get("intervalQuantity") != null)
		attributeMap.put("intervalQuantity", params.get("intervalQuantity"));
	if(params.get("intervalUomId") != null)
		attributeMap.put("intervalUomId", params.get("intervalUomId"));
	if(params.get("intervalMeterTypeId") != null)
		attributeMap.put("intervalMeterTypeId", params.get("intervalMeterTypeId"));
	if(params.get("purchaseOrderId") != null)
		attributeMap.put("purchaseOrderId", params.get("purchaseOrderId"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("updateFixedAssetMaint", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/deleteFixedAssetMaint")
public Response deleteFixedAssetMaint_(@Multipart(value = "deleteFixedAssetMaint", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("fixedAssetId") != null)
		attributeMap.put("fixedAssetId", params.get("fixedAssetId"));
	if(params.get("maintHistSeqId") != null)
		attributeMap.put("maintHistSeqId", params.get("maintHistSeqId"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("deleteFixedAssetMaint", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/createFixedAssetMeter")
public Response createFixedAssetMeter_(@Multipart(value = "createFixedAssetMeter", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("fixedAssetId") != null)
		attributeMap.put("fixedAssetId", params.get("fixedAssetId"));
	if(params.get("productMeterTypeId") != null)
		attributeMap.put("productMeterTypeId", params.get("productMeterTypeId"));
	if(params.get("readingDate") != null)
		attributeMap.put("readingDate", params.get("readingDate"));
	if(params.get("meterValue") != null)
		attributeMap.put("meterValue", params.get("meterValue"));
	if(params.get("readingReasonEnumId") != null)
		attributeMap.put("readingReasonEnumId", params.get("readingReasonEnumId"));
	if(params.get("maintHistSeqId") != null)
		attributeMap.put("maintHistSeqId", params.get("maintHistSeqId"));
	if(params.get("workEffortId") != null)
		attributeMap.put("workEffortId", params.get("workEffortId"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("createFixedAssetMeter", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/updateFixedAssetMeter")
public Response updateFixedAssetMeter_(@Multipart(value = "updateFixedAssetMeter", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("fixedAssetId") != null)
		attributeMap.put("fixedAssetId", params.get("fixedAssetId"));
	if(params.get("productMeterTypeId") != null)
		attributeMap.put("productMeterTypeId", params.get("productMeterTypeId"));
	if(params.get("readingDate") != null)
		attributeMap.put("readingDate", params.get("readingDate"));
	if(params.get("meterValue") != null)
		attributeMap.put("meterValue", params.get("meterValue"));
	if(params.get("readingReasonEnumId") != null)
		attributeMap.put("readingReasonEnumId", params.get("readingReasonEnumId"));
	if(params.get("maintHistSeqId") != null)
		attributeMap.put("maintHistSeqId", params.get("maintHistSeqId"));
	if(params.get("workEffortId") != null)
		attributeMap.put("workEffortId", params.get("workEffortId"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("updateFixedAssetMeter", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/deleteFixedAssetMeter")
public Response deleteFixedAssetMeter_(@Multipart(value = "deleteFixedAssetMeter", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("fixedAssetId") != null)
		attributeMap.put("fixedAssetId", params.get("fixedAssetId"));
	if(params.get("productMeterTypeId") != null)
		attributeMap.put("productMeterTypeId", params.get("productMeterTypeId"));
	if(params.get("readingDate") != null)
		attributeMap.put("readingDate", params.get("readingDate"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("deleteFixedAssetMeter", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/createFixedAssetMaintOrder")
public Response createFixedAssetMaintOrder_(@Multipart(value = "createFixedAssetMaintOrder", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("fixedAssetId") != null)
		attributeMap.put("fixedAssetId", params.get("fixedAssetId"));
	if(params.get("maintHistSeqId") != null)
		attributeMap.put("maintHistSeqId", params.get("maintHistSeqId"));
	if(params.get("orderId") != null)
		attributeMap.put("orderId", params.get("orderId"));
	if(params.get("orderItemSeqId") != null)
		attributeMap.put("orderItemSeqId", params.get("orderItemSeqId"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("createFixedAssetMaintOrder", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/deleteFixedAssetMaintOrder")
public Response deleteFixedAssetMaintOrder_(@Multipart(value = "deleteFixedAssetMaintOrder", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("fixedAssetId") != null)
		attributeMap.put("fixedAssetId", params.get("fixedAssetId"));
	if(params.get("maintHistSeqId") != null)
		attributeMap.put("maintHistSeqId", params.get("maintHistSeqId"));
	if(params.get("orderId") != null)
		attributeMap.put("orderId", params.get("orderId"));
	if(params.get("orderItemSeqId") != null)
		attributeMap.put("orderItemSeqId", params.get("orderItemSeqId"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("deleteFixedAssetMaintOrder", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/createPartyFixedAssetAssignment")
public Response createPartyFixedAssetAssignment_(@Multipart(value = "createPartyFixedAssetAssignment", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("partyId") != null)
		attributeMap.put("partyId", params.get("partyId"));
	if(params.get("roleTypeId") != null)
		attributeMap.put("roleTypeId", params.get("roleTypeId"));
	if(params.get("fixedAssetId") != null)
		attributeMap.put("fixedAssetId", params.get("fixedAssetId"));
	if(params.get("fromDate") != null)
		attributeMap.put("fromDate", params.get("fromDate"));
	if(params.get("thruDate") != null)
		attributeMap.put("thruDate", params.get("thruDate"));
	if(params.get("allocatedDate") != null)
		attributeMap.put("allocatedDate", params.get("allocatedDate"));
	if(params.get("statusId") != null)
		attributeMap.put("statusId", params.get("statusId"));
	if(params.get("comments") != null)
		attributeMap.put("comments", params.get("comments"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	if(params.get("fromDate") != null)
		attributeMap.put("fromDate", params.get("fromDate"));
	try {
		Map result = dispatcher.runSync("createPartyFixedAssetAssignment", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/updatePartyFixedAssetAssignment")
public Response updatePartyFixedAssetAssignment_(@Multipart(value = "updatePartyFixedAssetAssignment", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("partyId") != null)
		attributeMap.put("partyId", params.get("partyId"));
	if(params.get("roleTypeId") != null)
		attributeMap.put("roleTypeId", params.get("roleTypeId"));
	if(params.get("fixedAssetId") != null)
		attributeMap.put("fixedAssetId", params.get("fixedAssetId"));
	if(params.get("fromDate") != null)
		attributeMap.put("fromDate", params.get("fromDate"));
	if(params.get("thruDate") != null)
		attributeMap.put("thruDate", params.get("thruDate"));
	if(params.get("allocatedDate") != null)
		attributeMap.put("allocatedDate", params.get("allocatedDate"));
	if(params.get("statusId") != null)
		attributeMap.put("statusId", params.get("statusId"));
	if(params.get("comments") != null)
		attributeMap.put("comments", params.get("comments"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("updatePartyFixedAssetAssignment", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/deletePartyFixedAssetAssignment")
public Response deletePartyFixedAssetAssignment_(@Multipart(value = "deletePartyFixedAssetAssignment", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("partyId") != null)
		attributeMap.put("partyId", params.get("partyId"));
	if(params.get("roleTypeId") != null)
		attributeMap.put("roleTypeId", params.get("roleTypeId"));
	if(params.get("fixedAssetId") != null)
		attributeMap.put("fixedAssetId", params.get("fixedAssetId"));
	if(params.get("fromDate") != null)
		attributeMap.put("fromDate", params.get("fromDate"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("deletePartyFixedAssetAssignment", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/createFixedAssetDepMethod")
public Response createFixedAssetDepMethod_(@Multipart(value = "createFixedAssetDepMethod", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("depreciationCustomMethodId") != null)
		attributeMap.put("depreciationCustomMethodId", params.get("depreciationCustomMethodId"));
	if(params.get("fixedAssetId") != null)
		attributeMap.put("fixedAssetId", params.get("fixedAssetId"));
	if(params.get("fromDate") != null)
		attributeMap.put("fromDate", params.get("fromDate"));
	if(params.get("thruDate") != null)
		attributeMap.put("thruDate", params.get("thruDate"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("createFixedAssetDepMethod", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/updateFixedAssetDepMethod")
public Response updateFixedAssetDepMethod_(@Multipart(value = "updateFixedAssetDepMethod", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("depreciationCustomMethodId") != null)
		attributeMap.put("depreciationCustomMethodId", params.get("depreciationCustomMethodId"));
	if(params.get("fixedAssetId") != null)
		attributeMap.put("fixedAssetId", params.get("fixedAssetId"));
	if(params.get("fromDate") != null)
		attributeMap.put("fromDate", params.get("fromDate"));
	if(params.get("thruDate") != null)
		attributeMap.put("thruDate", params.get("thruDate"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("updateFixedAssetDepMethod", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/deleteFixedAssetDepMethod")
public Response deleteFixedAssetDepMethod_(@Multipart(value = "deleteFixedAssetDepMethod", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("depreciationCustomMethodId") != null)
		attributeMap.put("depreciationCustomMethodId", params.get("depreciationCustomMethodId"));
	if(params.get("fixedAssetId") != null)
		attributeMap.put("fixedAssetId", params.get("fixedAssetId"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("deleteFixedAssetDepMethod", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/createFixedAssetTypeGlAccount")
public Response createFixedAssetTypeGlAccount_(@Multipart(value = "createFixedAssetTypeGlAccount", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("assetGlAccountId") != null)
		attributeMap.put("assetGlAccountId", params.get("assetGlAccountId"));
	if(params.get("accDepGlAccountId") != null)
		attributeMap.put("accDepGlAccountId", params.get("accDepGlAccountId"));
	if(params.get("depGlAccountId") != null)
		attributeMap.put("depGlAccountId", params.get("depGlAccountId"));
	if(params.get("profitGlAccountId") != null)
		attributeMap.put("profitGlAccountId", params.get("profitGlAccountId"));
	if(params.get("lossGlAccountId") != null)
		attributeMap.put("lossGlAccountId", params.get("lossGlAccountId"));
	if(params.get("fixedAssetTypeId") != null)
		attributeMap.put("fixedAssetTypeId", params.get("fixedAssetTypeId"));
	if(params.get("fixedAssetId") != null)
		attributeMap.put("fixedAssetId", params.get("fixedAssetId"));
	if(params.get("organizationPartyId") != null)
		attributeMap.put("organizationPartyId", params.get("organizationPartyId"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("createFixedAssetTypeGlAccount", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/deleteFixedAssetTypeGlAccount")
public Response deleteFixedAssetTypeGlAccount_(@Multipart(value = "deleteFixedAssetTypeGlAccount", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("fixedAssetTypeId") != null)
		attributeMap.put("fixedAssetTypeId", params.get("fixedAssetTypeId"));
	if(params.get("fixedAssetId") != null)
		attributeMap.put("fixedAssetId", params.get("fixedAssetId"));
	if(params.get("organizationPartyId") != null)
		attributeMap.put("organizationPartyId", params.get("organizationPartyId"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("deleteFixedAssetTypeGlAccount", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/setFinAccountTransStatus")
public Response setFinAccountTransStatus_(@Multipart(value = "setFinAccountTransStatus", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("finAccountTransId") != null)
		attributeMap.put("finAccountTransId", params.get("finAccountTransId"));
	if(params.get("statusId") != null)
		attributeMap.put("statusId", params.get("statusId"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("setFinAccountTransStatus", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/createServiceCredit")
public Response createServiceCredit_(@Multipart(value = "createServiceCredit", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("finAccountId") != null)
		attributeMap.put("finAccountId", params.get("finAccountId"));
	if(params.get("finAccountName") != null)
		attributeMap.put("finAccountName", params.get("finAccountName"));
	if(params.get("reasonEnumId") != null)
		attributeMap.put("reasonEnumId", params.get("reasonEnumId"));
	if(params.get("comments") != null)
		attributeMap.put("comments", params.get("comments"));
	if(params.get("partyId") != null)
		attributeMap.put("partyId", params.get("partyId"));
	if(params.get("amount") != null)
		attributeMap.put("amount", params.get("amount"));
	if(params.get("currencyUomId") != null)
		attributeMap.put("currencyUomId", params.get("currencyUomId"));
	if(params.get("productStoreId") != null)
		attributeMap.put("productStoreId", params.get("productStoreId"));
	if(params.get("finAccountTypeId") != null)
		attributeMap.put("finAccountTypeId", params.get("finAccountTypeId"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("createServiceCredit", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/depositWithdrawPayments")
public Response depositWithdrawPayments_(@Multipart(value = "depositWithdrawPayments", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("paymentIds") != null)
		attributeMap.put("paymentIds", params.get("paymentIds"));
	if(params.get("finAccountId") != null)
		attributeMap.put("finAccountId", params.get("finAccountId"));
	if(params.get("groupInOneTransaction") != null)
		attributeMap.put("groupInOneTransaction", params.get("groupInOneTransaction"));
	if(params.get("paymentGroupTypeId") != null)
		attributeMap.put("paymentGroupTypeId", params.get("paymentGroupTypeId"));
	if(params.get("paymentGroupName") != null)
		attributeMap.put("paymentGroupName", params.get("paymentGroupName"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("depositWithdrawPayments", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/createPaymentAndFinAccountTrans")
public Response createPaymentAndFinAccountTrans_(@Multipart(value = "createPaymentAndFinAccountTrans", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("isDepositWithDrawPayment") != null)
		attributeMap.put("isDepositWithDrawPayment", params.get("isDepositWithDrawPayment"));
	if(params.get("finAccountTransTypeId") != null)
		attributeMap.put("finAccountTransTypeId", params.get("finAccountTransTypeId"));
	if(params.get("paymentGroupTypeId") != null)
		attributeMap.put("paymentGroupTypeId", params.get("paymentGroupTypeId"));
	if(params.get("paymentMethodId") != null)
		attributeMap.put("paymentMethodId", params.get("paymentMethodId"));
	if(params.get("paymentId") != null)
		attributeMap.put("paymentId", params.get("paymentId"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	if(params.get("paymentTypeId") != null)
		attributeMap.put("paymentTypeId", params.get("paymentTypeId"));
	if(params.get("paymentMethodTypeId") != null)
		attributeMap.put("paymentMethodTypeId", params.get("paymentMethodTypeId"));
	if(params.get("paymentGatewayResponseId") != null)
		attributeMap.put("paymentGatewayResponseId", params.get("paymentGatewayResponseId"));
	if(params.get("paymentPreferenceId") != null)
		attributeMap.put("paymentPreferenceId", params.get("paymentPreferenceId"));
	if(params.get("partyIdFrom") != null)
		attributeMap.put("partyIdFrom", params.get("partyIdFrom"));
	if(params.get("partyIdTo") != null)
		attributeMap.put("partyIdTo", params.get("partyIdTo"));
	if(params.get("roleTypeIdTo") != null)
		attributeMap.put("roleTypeIdTo", params.get("roleTypeIdTo"));
	if(params.get("statusId") != null)
		attributeMap.put("statusId", params.get("statusId"));
	if(params.get("effectiveDate") != null)
		attributeMap.put("effectiveDate", params.get("effectiveDate"));
	if(params.get("paymentRefNum") != null)
		attributeMap.put("paymentRefNum", params.get("paymentRefNum"));
	if(params.get("amount") != null)
		attributeMap.put("amount", params.get("amount"));
	if(params.get("currencyUomId") != null)
		attributeMap.put("currencyUomId", params.get("currencyUomId"));
	if(params.get("comments") != null)
		attributeMap.put("comments", params.get("comments"));
	if(params.get("finAccountTransId") != null)
		attributeMap.put("finAccountTransId", params.get("finAccountTransId"));
	if(params.get("overrideGlAccountId") != null)
		attributeMap.put("overrideGlAccountId", params.get("overrideGlAccountId"));
	if(params.get("actualCurrencyAmount") != null)
		attributeMap.put("actualCurrencyAmount", params.get("actualCurrencyAmount"));
	if(params.get("actualCurrencyUomId") != null)
		attributeMap.put("actualCurrencyUomId", params.get("actualCurrencyUomId"));
	try {
		Map result = dispatcher.runSync("createPaymentAndFinAccountTrans", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/removeFinAccountTransFromReconciliation")
public Response removeFinAccountTransFromReconciliation_(@Multipart(value = "removeFinAccountTransFromReconciliation", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("finAccountTransId") != null)
		attributeMap.put("finAccountTransId", params.get("finAccountTransId"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("removeFinAccountTransFromReconciliation", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}


@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/createGlReconciliation")
public Response createGlReconciliation_(@Multipart(value = "createGlReconciliation", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("glReconciliationName") != null)
		attributeMap.put("glReconciliationName", params.get("glReconciliationName"));
	if(params.get("description") != null)
		attributeMap.put("description", params.get("description"));
	if(params.get("createdDate") != null)
		attributeMap.put("createdDate", params.get("createdDate"));
	if(params.get("lastModifiedDate") != null)
		attributeMap.put("lastModifiedDate", params.get("lastModifiedDate"));
	if(params.get("glAccountId") != null)
		attributeMap.put("glAccountId", params.get("glAccountId"));
	if(params.get("statusId") != null)
		attributeMap.put("statusId", params.get("statusId"));
	if(params.get("organizationPartyId") != null)
		attributeMap.put("organizationPartyId", params.get("organizationPartyId"));
	if(params.get("reconciledBalance") != null)
		attributeMap.put("reconciledBalance", params.get("reconciledBalance"));
	if(params.get("openingBalance") != null)
		attributeMap.put("openingBalance", params.get("openingBalance"));
	if(params.get("reconciledDate") != null)
		attributeMap.put("reconciledDate", params.get("reconciledDate"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	if(params.get("glReconciliationName") != null)
		attributeMap.put("glReconciliationName", params.get("glReconciliationName"));
	try {
		Map result = dispatcher.runSync("createGlReconciliation", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}


@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/updateGlReconciliation")
public Response updateGlReconciliation_(@Multipart(value = "updateGlReconciliation", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("glReconciliationId") != null)
		attributeMap.put("glReconciliationId", params.get("glReconciliationId"));
	if(params.get("glReconciliationName") != null)
		attributeMap.put("glReconciliationName", params.get("glReconciliationName"));
	if(params.get("description") != null)
		attributeMap.put("description", params.get("description"));
	if(params.get("createdDate") != null)
		attributeMap.put("createdDate", params.get("createdDate"));
	if(params.get("lastModifiedDate") != null)
		attributeMap.put("lastModifiedDate", params.get("lastModifiedDate"));
	if(params.get("glAccountId") != null)
		attributeMap.put("glAccountId", params.get("glAccountId"));
	if(params.get("statusId") != null)
		attributeMap.put("statusId", params.get("statusId"));
	if(params.get("organizationPartyId") != null)
		attributeMap.put("organizationPartyId", params.get("organizationPartyId"));
	if(params.get("reconciledBalance") != null)
		attributeMap.put("reconciledBalance", params.get("reconciledBalance"));
	if(params.get("openingBalance") != null)
		attributeMap.put("openingBalance", params.get("openingBalance"));
	if(params.get("reconciledDate") != null)
		attributeMap.put("reconciledDate", params.get("reconciledDate"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("updateGlReconciliation", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}
@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/cancelBankReconciliation")
public Response cancelBankReconciliation_(@Multipart(value = "cancelBankReconciliation", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("glReconciliationId") != null)
		attributeMap.put("glReconciliationId", params.get("glReconciliationId"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("cancelBankReconciliation", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/createPaymentApplication")
public Response createPaymentApplication_(@Multipart(value = "createPaymentApplication", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("paymentId") != null)
		attributeMap.put("paymentId", params.get("paymentId"));
	if(params.get("toPaymentId") != null)
		attributeMap.put("toPaymentId", params.get("toPaymentId"));
	if(params.get("invoiceId") != null)
		attributeMap.put("invoiceId", params.get("invoiceId"));
	if(params.get("billingAccountId") != null)
		attributeMap.put("billingAccountId", params.get("billingAccountId"));
	if(params.get("taxAuthGeoId") != null)
		attributeMap.put("taxAuthGeoId", params.get("taxAuthGeoId"));
	if(params.get("amountApplied") != null)
		attributeMap.put("amountApplied", params.get("amountApplied"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("createPaymentApplication", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}



@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/createPaymentAndApplication")
public Response createPaymentAndApplication_(@Multipart(value = "createPaymentAndApplication", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("paymentTypeId") != null)
		attributeMap.put("paymentTypeId", params.get("paymentTypeId"));
	if(params.get("paymentMethodTypeId") != null)
		attributeMap.put("paymentMethodTypeId", params.get("paymentMethodTypeId"));
	if(params.get("paymentMethodId") != null)
		attributeMap.put("paymentMethodId", params.get("paymentMethodId"));
	if(params.get("paymentGatewayResponseId") != null)
		attributeMap.put("paymentGatewayResponseId", params.get("paymentGatewayResponseId"));
	if(params.get("paymentPreferenceId") != null)
		attributeMap.put("paymentPreferenceId", params.get("paymentPreferenceId"));
	if(params.get("partyIdFrom") != null)
		attributeMap.put("partyIdFrom", params.get("partyIdFrom"));
	if(params.get("partyIdTo") != null)
		attributeMap.put("partyIdTo", params.get("partyIdTo"));
	if(params.get("roleTypeIdTo") != null)
		attributeMap.put("roleTypeIdTo", params.get("roleTypeIdTo"));
	if(params.get("statusId") != null)
		attributeMap.put("statusId", params.get("statusId"));
	if(params.get("effectiveDate") != null)
		attributeMap.put("effectiveDate", params.get("effectiveDate"));
	if(params.get("paymentRefNum") != null)
		attributeMap.put("paymentRefNum", params.get("paymentRefNum"));
	if(params.get("amount") != null)
		attributeMap.put("amount", params.get("amount"));
	if(params.get("currencyUomId") != null)
		attributeMap.put("currencyUomId", params.get("currencyUomId"));
	if(params.get("comments") != null)
		attributeMap.put("comments", params.get("comments"));
	if(params.get("finAccountTransId") != null)
		attributeMap.put("finAccountTransId", params.get("finAccountTransId"));
	if(params.get("overrideGlAccountId") != null)
		attributeMap.put("overrideGlAccountId", params.get("overrideGlAccountId"));
	if(params.get("actualCurrencyAmount") != null)
		attributeMap.put("actualCurrencyAmount", params.get("actualCurrencyAmount"));
	if(params.get("actualCurrencyUomId") != null)
		attributeMap.put("actualCurrencyUomId", params.get("actualCurrencyUomId"));
	if(params.get("paymentId") != null)
		attributeMap.put("paymentId", params.get("paymentId"));
	if(params.get("invoiceId") != null)
		attributeMap.put("invoiceId", params.get("invoiceId"));
	if(params.get("invoiceItemSeqId") != null)
		attributeMap.put("invoiceItemSeqId", params.get("invoiceItemSeqId"));
	if(params.get("billingAccountId") != null)
		attributeMap.put("billingAccountId", params.get("billingAccountId"));
	if(params.get("overrideGlAccountId") != null)
		attributeMap.put("overrideGlAccountId", params.get("overrideGlAccountId"));
	if(params.get("taxAuthGeoId") != null)
		attributeMap.put("taxAuthGeoId", params.get("taxAuthGeoId"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	if(params.get("paymentTypeId") != null)
		attributeMap.put("paymentTypeId", params.get("paymentTypeId"));
	if(params.get("partyIdFrom") != null)
		attributeMap.put("partyIdFrom", params.get("partyIdFrom"));
	if(params.get("partyIdTo") != null)
		attributeMap.put("partyIdTo", params.get("partyIdTo"));
	if(params.get("statusId") != null)
		attributeMap.put("statusId", params.get("statusId"));
	if(params.get("amount") != null)
		attributeMap.put("amount", params.get("amount"));
	try {
		Map result = dispatcher.runSync("createPaymentAndApplication", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/updatePaymentMethodType")
public Response updatePaymentMethodType_(@Multipart(value = "updatePaymentMethodType", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("paymentMethodTypeId") != null)
		attributeMap.put("paymentMethodTypeId", params.get("paymentMethodTypeId"));
	if(params.get("description") != null)
		attributeMap.put("description", params.get("description"));
	if(params.get("defaultGlAccountId") != null)
		attributeMap.put("defaultGlAccountId", params.get("defaultGlAccountId"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("updatePaymentMethodType", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}



@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/updatePaymentGroupMember")
public Response updatePaymentGroupMember_(@Multipart(value = "updatePaymentGroupMember", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("paymentGroupId") != null)
		attributeMap.put("paymentGroupId", params.get("paymentGroupId"));
	if(params.get("paymentId") != null)
		attributeMap.put("paymentId", params.get("paymentId"));
	if(params.get("fromDate") != null)
		attributeMap.put("fromDate", params.get("fromDate"));
	if(params.get("thruDate") != null)
		attributeMap.put("thruDate", params.get("thruDate"));
	if(params.get("sequenceNum") != null)
		attributeMap.put("sequenceNum", params.get("sequenceNum"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("updatePaymentGroupMember", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/expirePaymentGroupMember")
public Response expirePaymentGroupMember_(@Multipart(value = "expirePaymentGroupMember", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("paymentGroupId") != null)
		attributeMap.put("paymentGroupId", params.get("paymentGroupId"));
	if(params.get("paymentId") != null)
		attributeMap.put("paymentId", params.get("paymentId"));
	if(params.get("fromDate") != null)
		attributeMap.put("fromDate", params.get("fromDate"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("expirePaymentGroupMember", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/updatePayment")
public Response updatePayment_(@Multipart(value = "updatePayment", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("paymentId") != null)
		attributeMap.put("paymentId", params.get("paymentId"));
	if(params.get("paymentTypeId") != null)
		attributeMap.put("paymentTypeId", params.get("paymentTypeId"));
	if(params.get("paymentMethodTypeId") != null)
		attributeMap.put("paymentMethodTypeId", params.get("paymentMethodTypeId"));
	if(params.get("paymentMethodId") != null)
		attributeMap.put("paymentMethodId", params.get("paymentMethodId"));
	if(params.get("paymentGatewayResponseId") != null)
		attributeMap.put("paymentGatewayResponseId", params.get("paymentGatewayResponseId"));
	if(params.get("paymentPreferenceId") != null)
		attributeMap.put("paymentPreferenceId", params.get("paymentPreferenceId"));
	if(params.get("partyIdFrom") != null)
		attributeMap.put("partyIdFrom", params.get("partyIdFrom"));
	if(params.get("partyIdTo") != null)
		attributeMap.put("partyIdTo", params.get("partyIdTo"));
	if(params.get("roleTypeIdTo") != null)
		attributeMap.put("roleTypeIdTo", params.get("roleTypeIdTo"));
	if(params.get("statusId") != null)
		attributeMap.put("statusId", params.get("statusId"));
	if(params.get("effectiveDate") != null)
		attributeMap.put("effectiveDate", params.get("effectiveDate"));
	if(params.get("paymentRefNum") != null)
		attributeMap.put("paymentRefNum", params.get("paymentRefNum"));
	if(params.get("amount") != null)
		attributeMap.put("amount", params.get("amount"));
	if(params.get("currencyUomId") != null)
		attributeMap.put("currencyUomId", params.get("currencyUomId"));
	if(params.get("comments") != null)
		attributeMap.put("comments", params.get("comments"));
	if(params.get("finAccountTransId") != null)
		attributeMap.put("finAccountTransId", params.get("finAccountTransId"));
	if(params.get("overrideGlAccountId") != null)
		attributeMap.put("overrideGlAccountId", params.get("overrideGlAccountId"));
	if(params.get("actualCurrencyAmount") != null)
		attributeMap.put("actualCurrencyAmount", params.get("actualCurrencyAmount"));
	if(params.get("actualCurrencyUomId") != null)
		attributeMap.put("actualCurrencyUomId", params.get("actualCurrencyUomId"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("updatePayment", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}


@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/updatePaymentGroup")
public Response updatePaymentGroup_(@Multipart(value = "updatePaymentGroup", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("paymentGroupId") != null)
		attributeMap.put("paymentGroupId", params.get("paymentGroupId"));
	if(params.get("paymentGroupTypeId") != null)
		attributeMap.put("paymentGroupTypeId", params.get("paymentGroupTypeId"));
	if(params.get("paymentGroupName") != null)
		attributeMap.put("paymentGroupName", params.get("paymentGroupName"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("updatePaymentGroup", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/deletePaymentGroup")
public Response deletePaymentGroup_(@Multipart(value = "deletePaymentGroup", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("paymentGroupId") != null)
		attributeMap.put("paymentGroupId", params.get("paymentGroupId"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("deletePaymentGroup", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/createPaymentGroup")
public Response createPaymentGroup_(@Multipart(value = "createPaymentGroup", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("paymentGroupTypeId") != null)
		attributeMap.put("paymentGroupTypeId", params.get("paymentGroupTypeId"));
	if(params.get("paymentGroupName") != null)
		attributeMap.put("paymentGroupName", params.get("paymentGroupName"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	if(params.get("paymentGroupTypeId") != null)
		attributeMap.put("paymentGroupTypeId", params.get("paymentGroupTypeId"));
	if(params.get("paymentGroupName") != null)
		attributeMap.put("paymentGroupName", params.get("paymentGroupName"));
	try {
		Map result = dispatcher.runSync("createPaymentGroup", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}


@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/createPaymentGroupMember")
public Response createPaymentGroupMember_(@Multipart(value = "createPaymentGroupMember", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("paymentGroupId") != null)
		attributeMap.put("paymentGroupId", params.get("paymentGroupId"));
	if(params.get("paymentId") != null)
		attributeMap.put("paymentId", params.get("paymentId"));
	if(params.get("fromDate") != null)
		attributeMap.put("fromDate", params.get("fromDate"));
	if(params.get("thruDate") != null)
		attributeMap.put("thruDate", params.get("thruDate"));
	if(params.get("sequenceNum") != null)
		attributeMap.put("sequenceNum", params.get("sequenceNum"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	if(params.get("fromDate") != null)
		attributeMap.put("fromDate", params.get("fromDate"));
	try {
		Map result = dispatcher.runSync("createPaymentGroupMember", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}


	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/createPartyGroup")
	public Response createPartyGroup_(@Multipart(value = "createPartyGroup", required = true, type = MediaType.APPLICATION_JSON) Map params) {
		GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
		LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
		Map<String, Object> attributeMap = new HashMap<String, Object>();
		if(params.get("partyId") != null)
			attributeMap.put("partyId", params.get("partyId"));
		if(params.get("groupName") != null)
			attributeMap.put("groupName", params.get("groupName"));
		if(params.get("groupNameLocal") != null)
			attributeMap.put("groupNameLocal", params.get("groupNameLocal"));
		if(params.get("officeSiteName") != null)
			attributeMap.put("officeSiteName", params.get("officeSiteName"));
		if(params.get("annualRevenue") != null)
			attributeMap.put("annualRevenue", params.get("annualRevenue"));
		if(params.get("numEmployees") != null)
			attributeMap.put("numEmployees", params.get("numEmployees"));
		if(params.get("tickerSymbol") != null)
			attributeMap.put("tickerSymbol", params.get("tickerSymbol"));
		if(params.get("comments") != null)
			attributeMap.put("comments", params.get("comments"));
		if(params.get("logoImageUrl") != null)
			attributeMap.put("logoImageUrl", params.get("logoImageUrl"));
		if(params.get("partyTypeId") != null)
			attributeMap.put("partyTypeId", params.get("partyTypeId"));
		if(params.get("description") != null)
			attributeMap.put("description", params.get("description"));
		if(params.get("preferredCurrencyUomId") != null)
			attributeMap.put("preferredCurrencyUomId", params.get("preferredCurrencyUomId"));
		if(params.get("externalId") != null)
			attributeMap.put("externalId", params.get("externalId"));
		if(params.get("statusId") != null)
			attributeMap.put("statusId", params.get("statusId"));
		if(params.get("userLogin") != null)
			attributeMap.put("userLogin", params.get("userLogin"));
		else {
			try {
				attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
			} catch (GenericEntityException e) {
				e.printStackTrace();
			}
		}
		if(params.get("login.username") != null)
			attributeMap.put("login.username", params.get("login.username"));
		if(params.get("login.password") != null)
			attributeMap.put("login.password", params.get("login.password"));
		if(params.get("locale") != null)
			attributeMap.put("locale", params.get("locale"));
		if(params.get("timeZone") != null)
			attributeMap.put("timeZone", params.get("timeZone"));
		if(params.get("groupName") != null)
			attributeMap.put("groupName", params.get("groupName"));
		if(params.get("comments") != null)
			attributeMap.put("comments", params.get("comments"));
		try {
			Map result = dispatcher.runSync("createPartyGroup", attributeMap);
			return Response.ok().entity(result).build();
		} catch(GenericServiceException e) {
			e.printStackTrace();
			return null;
		}
	}

	// party --> my communications
	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/updateCommunicationEvent")
	public Response updateCommunicationEvent_(@Multipart(value = "updateCommunicationEvent", required = true, type = MediaType.APPLICATION_JSON) Map params) {
		GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
		LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
		Map<String, Object> attributeMap = new HashMap<String, Object>();
		if(params.get("communicationEventId") != null)
			attributeMap.put("communicationEventId", params.get("communicationEventId"));
		if(params.get("communicationEventTypeId") != null)
			attributeMap.put("communicationEventTypeId", params.get("communicationEventTypeId"));
		if(params.get("origCommEventId") != null)
			attributeMap.put("origCommEventId", params.get("origCommEventId"));
		if(params.get("parentCommEventId") != null)
			attributeMap.put("parentCommEventId", params.get("parentCommEventId"));
		if(params.get("statusId") != null)
			attributeMap.put("statusId", params.get("statusId"));
		if(params.get("contactMechTypeId") != null)
			attributeMap.put("contactMechTypeId", params.get("contactMechTypeId"));
		if(params.get("contactMechIdFrom") != null)
			attributeMap.put("contactMechIdFrom", params.get("contactMechIdFrom"));
		if(params.get("contactMechIdTo") != null)
			attributeMap.put("contactMechIdTo", params.get("contactMechIdTo"));
		if(params.get("roleTypeIdFrom") != null)
			attributeMap.put("roleTypeIdFrom", params.get("roleTypeIdFrom"));
		if(params.get("roleTypeIdTo") != null)
			attributeMap.put("roleTypeIdTo", params.get("roleTypeIdTo"));
		if(params.get("partyIdFrom") != null)
			attributeMap.put("partyIdFrom", params.get("partyIdFrom"));
		if(params.get("partyIdTo") != null)
			attributeMap.put("partyIdTo", params.get("partyIdTo"));
		if(params.get("entryDate") != null)
			attributeMap.put("entryDate", params.get("entryDate"));
		if(params.get("datetimeStarted") != null)
			attributeMap.put("datetimeStarted", params.get("datetimeStarted"));
		if(params.get("datetimeEnded") != null)
			attributeMap.put("datetimeEnded", params.get("datetimeEnded"));
		if(params.get("subject") != null)
			attributeMap.put("subject", params.get("subject"));
		if(params.get("contentMimeTypeId") != null)
			attributeMap.put("contentMimeTypeId", params.get("contentMimeTypeId"));
		if(params.get("content") != null)
			attributeMap.put("content", params.get("content"));
		if(params.get("note") != null)
			attributeMap.put("note", params.get("note"));
		if(params.get("reasonEnumId") != null)
			attributeMap.put("reasonEnumId", params.get("reasonEnumId"));
		if(params.get("contactListId") != null)
			attributeMap.put("contactListId", params.get("contactListId"));
		if(params.get("headerString") != null)
			attributeMap.put("headerString", params.get("headerString"));
		if(params.get("fromString") != null)
			attributeMap.put("fromString", params.get("fromString"));
		if(params.get("toString") != null)
			attributeMap.put("toString", params.get("toString"));
		if(params.get("ccString") != null)
			attributeMap.put("ccString", params.get("ccString"));
		if(params.get("bccString") != null)
			attributeMap.put("bccString", params.get("bccString"));
		if(params.get("messageId") != null)
			attributeMap.put("messageId", params.get("messageId"));
		if(params.get("contactMechPurposeTypeIdFrom") != null)
			attributeMap.put("contactMechPurposeTypeIdFrom", params.get("contactMechPurposeTypeIdFrom"));
		if(params.get("userLogin") != null)
			attributeMap.put("userLogin", params.get("userLogin"));
		else {
			try {
				attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
			} catch (GenericEntityException e) {
				e.printStackTrace();
			}
		}
		if(params.get("login.username") != null)
			attributeMap.put("login.username", params.get("login.username"));
		if(params.get("login.password") != null)
			attributeMap.put("login.password", params.get("login.password"));
		if(params.get("locale") != null)
			attributeMap.put("locale", params.get("locale"));
		if(params.get("timeZone") != null)
			attributeMap.put("timeZone", params.get("timeZone"));
		if(params.get("messageId") != null)
			attributeMap.put("messageId", params.get("messageId"));
		if(params.get("content") != null)
			attributeMap.put("content", params.get("content"));
		if(params.get("subject") != null)
			attributeMap.put("subject", params.get("subject"));
		try {
			Map result = dispatcher.runSync("updateCommunicationEvent", attributeMap);
			return Response.ok().entity(result).build();
		} catch(GenericServiceException e) {
			e.printStackTrace();
			return null;
		}
	}

	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/createCommunicationEventRole")
	public Response createCommunicationEventRole_(@Multipart(value = "createCommunicationEventRole", required = true, type = MediaType.APPLICATION_JSON) Map params) {
		GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
		LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
		Map<String, Object> attributeMap = new HashMap<String, Object>();
		if(params.get("userLogin") != null)
			attributeMap.put("userLogin", params.get("userLogin"));
		else {
			try {
				attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
			} catch (GenericEntityException e) {
				e.printStackTrace();
			}
		}
		if(params.get("login.username") != null)
			attributeMap.put("login.username", params.get("login.username"));
		if(params.get("login.password") != null)
			attributeMap.put("login.password", params.get("login.password"));
		if(params.get("locale") != null)
			attributeMap.put("locale", params.get("locale"));
		if(params.get("timeZone") != null)
			attributeMap.put("timeZone", params.get("timeZone"));
		if(params.get("communicationEventId") != null)
			attributeMap.put("communicationEventId", params.get("communicationEventId"));
		if(params.get("partyId") != null)
			attributeMap.put("partyId", params.get("partyId"));
		if(params.get("roleTypeId") != null)
			attributeMap.put("roleTypeId", params.get("roleTypeId"));
		if(params.get("contactMechId") != null)
			attributeMap.put("contactMechId", params.get("contactMechId"));
		if(params.get("statusId") != null)
			attributeMap.put("statusId", params.get("statusId"));
		try {
			Map result = dispatcher.runSync("createCommunicationEventRole", attributeMap);
			return Response.ok().entity(result).build();
		} catch(GenericServiceException e) {
			e.printStackTrace();
			return null;
		}
	}

	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/createCommunicationEvent")
	public Response createCommunicationEvent_(@Multipart(value = "createCommunicationEvent", required = true, type = MediaType.APPLICATION_JSON) Map params) {
		GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
		LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
		Map<String, Object> attributeMap = new HashMap<String, Object>();
		if(params.get("userLogin") != null)
			attributeMap.put("userLogin", params.get("userLogin"));
		else {
			try {
				attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
			} catch (GenericEntityException e) {
				e.printStackTrace();
			}
		}
		if(params.get("login.username") != null)
			attributeMap.put("login.username", params.get("login.username"));
		if(params.get("login.password") != null)
			attributeMap.put("login.password", params.get("login.password"));
		if(params.get("locale") != null)
			attributeMap.put("locale", params.get("locale"));
		if(params.get("timeZone") != null)
			attributeMap.put("timeZone", params.get("timeZone"));
		if(params.get("communicationEventTypeId") != null)
			attributeMap.put("communicationEventTypeId", params.get("communicationEventTypeId"));
		if(params.get("origCommEventId") != null)
			attributeMap.put("origCommEventId", params.get("origCommEventId"));
		if(params.get("parentCommEventId") != null)
			attributeMap.put("parentCommEventId", params.get("parentCommEventId"));
		if(params.get("statusId") != null)
			attributeMap.put("statusId", params.get("statusId"));
		if(params.get("contactMechTypeId") != null)
			attributeMap.put("contactMechTypeId", params.get("contactMechTypeId"));
		if(params.get("contactMechIdFrom") != null)
			attributeMap.put("contactMechIdFrom", params.get("contactMechIdFrom"));
		if(params.get("contactMechIdTo") != null)
			attributeMap.put("contactMechIdTo", params.get("contactMechIdTo"));
		if(params.get("roleTypeIdFrom") != null)
			attributeMap.put("roleTypeIdFrom", params.get("roleTypeIdFrom"));
		if(params.get("roleTypeIdTo") != null)
			attributeMap.put("roleTypeIdTo", params.get("roleTypeIdTo"));
		if(params.get("partyIdFrom") != null)
			attributeMap.put("partyIdFrom", params.get("partyIdFrom"));
		if(params.get("partyIdTo") != null)
			attributeMap.put("partyIdTo", params.get("partyIdTo"));
		if(params.get("entryDate") != null)
			attributeMap.put("entryDate", params.get("entryDate"));
		if(params.get("datetimeStarted") != null)
			attributeMap.put("datetimeStarted", params.get("datetimeStarted"));
		if(params.get("datetimeEnded") != null)
			attributeMap.put("datetimeEnded", params.get("datetimeEnded"));
		if(params.get("subject") != null)
			attributeMap.put("subject", params.get("subject"));
		if(params.get("contentMimeTypeId") != null)
			attributeMap.put("contentMimeTypeId", params.get("contentMimeTypeId"));
		if(params.get("content") != null)
			attributeMap.put("content", params.get("content"));
		if(params.get("note") != null)
			attributeMap.put("note", params.get("note"));
		if(params.get("reasonEnumId") != null)
			attributeMap.put("reasonEnumId", params.get("reasonEnumId"));
		if(params.get("contactListId") != null)
			attributeMap.put("contactListId", params.get("contactListId"));
		if(params.get("headerString") != null)
			attributeMap.put("headerString", params.get("headerString"));
		if(params.get("fromString") != null)
			attributeMap.put("fromString", params.get("fromString"));
		if(params.get("toString") != null)
			attributeMap.put("toString", params.get("toString"));
		if(params.get("ccString") != null)
			attributeMap.put("ccString", params.get("ccString"));
		if(params.get("bccString") != null)
			attributeMap.put("bccString", params.get("bccString"));
		if(params.get("messageId") != null)
			attributeMap.put("messageId", params.get("messageId"));
		if(params.get("communicationEventId") != null)
			attributeMap.put("communicationEventId", params.get("communicationEventId"));
		if(params.get("productId") != null)
			attributeMap.put("productId", params.get("productId"));
		if(params.get("orderId") != null)
			attributeMap.put("orderId", params.get("orderId"));
		if(params.get("custRequestId") != null)
			attributeMap.put("custRequestId", params.get("custRequestId"));
		if(params.get("action") != null)
			attributeMap.put("action", params.get("action"));
		try {
			Map result = dispatcher.runSync("createCommunicationEvent", attributeMap);
			return Response.ok().entity(result).build();
		} catch(GenericServiceException e) {
			e.printStackTrace();
			return null;
		}
	}

	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/createPartyClassificationGroup")
	public Response createPartyClassificationGroup_(@Multipart(value = "createPartyClassificationGroup", required = true, type = MediaType.APPLICATION_JSON) Map params) {
		GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
		LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
		Map<String, Object> attributeMap = new HashMap<String, Object>();
		if(params.get("partyClassificationTypeId") != null)
			attributeMap.put("partyClassificationTypeId", params.get("partyClassificationTypeId"));
		if(params.get("parentGroupId") != null)
			attributeMap.put("parentGroupId", params.get("parentGroupId"));
		if(params.get("description") != null)
			attributeMap.put("description", params.get("description"));
		if(params.get("userLogin") != null)
			attributeMap.put("userLogin", params.get("userLogin"));
		else {
			try {
				attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
			} catch (GenericEntityException e) {
				e.printStackTrace();
			}
		}
		if(params.get("login.username") != null)
			attributeMap.put("login.username", params.get("login.username"));
		if(params.get("login.password") != null)
			attributeMap.put("login.password", params.get("login.password"));
		if(params.get("locale") != null)
			attributeMap.put("locale", params.get("locale"));
		if(params.get("timeZone") != null)
			attributeMap.put("timeZone", params.get("timeZone"));
		try {
			Map result = dispatcher.runSync("createPartyClassificationGroup", attributeMap);
			return Response.ok().entity(result).build();
		} catch(GenericServiceException e) {
			e.printStackTrace();
			return null;
		}
	}

	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/createPartyClassification")
	public Response createPartyClassification_(@Multipart(value = "createPartyClassification", required = true, type = MediaType.APPLICATION_JSON) Map params) {
		GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
		LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
		Map<String, Object> attributeMap = new HashMap<String, Object>();
		if(params.get("partyId") != null)
			attributeMap.put("partyId", params.get("partyId"));
		if(params.get("partyClassificationGroupId") != null)
			attributeMap.put("partyClassificationGroupId", params.get("partyClassificationGroupId"));
		if(params.get("fromDate") != null)
			attributeMap.put("fromDate", params.get("fromDate"));
		if(params.get("thruDate") != null)
			attributeMap.put("thruDate", params.get("thruDate"));
		if(params.get("userLogin") != null)
			attributeMap.put("userLogin", params.get("userLogin"));
		else {
			try {
				attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
			} catch (GenericEntityException e) {
				e.printStackTrace();
			}
		}
		if(params.get("login.username") != null)
			attributeMap.put("login.username", params.get("login.username"));
		if(params.get("login.password") != null)
			attributeMap.put("login.password", params.get("login.password"));
		if(params.get("locale") != null)
			attributeMap.put("locale", params.get("locale"));
		if(params.get("timeZone") != null)
			attributeMap.put("timeZone", params.get("timeZone"));
		if(params.get("fromDate") != null)
			attributeMap.put("fromDate", params.get("fromDate"));
		try {
			Map result = dispatcher.runSync("createPartyClassification", attributeMap);
			return Response.ok().entity(result).build();
		} catch(GenericServiceException e) {
			e.printStackTrace();
			return null;
		}
	}

	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/updateAgreement")
	public Response updateAgreement_(@Multipart(value = "updateAgreement", required = true, type = MediaType.APPLICATION_JSON) Map params) {
		GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
		LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
		Map<String, Object> attributeMap = new HashMap<String, Object>();
		if(params.get("agreementId") != null)
			attributeMap.put("agreementId", params.get("agreementId"));
		if(params.get("productId") != null)
			attributeMap.put("productId", params.get("productId"));
		if(params.get("partyIdFrom") != null)
			attributeMap.put("partyIdFrom", params.get("partyIdFrom"));
		if(params.get("partyIdTo") != null)
			attributeMap.put("partyIdTo", params.get("partyIdTo"));
		if(params.get("roleTypeIdFrom") != null)
			attributeMap.put("roleTypeIdFrom", params.get("roleTypeIdFrom"));
		if(params.get("roleTypeIdTo") != null)
			attributeMap.put("roleTypeIdTo", params.get("roleTypeIdTo"));
		if(params.get("agreementTypeId") != null)
			attributeMap.put("agreementTypeId", params.get("agreementTypeId"));
		if(params.get("agreementDate") != null)
			attributeMap.put("agreementDate", params.get("agreementDate"));
		if(params.get("fromDate") != null)
			attributeMap.put("fromDate", params.get("fromDate"));
		if(params.get("thruDate") != null)
			attributeMap.put("thruDate", params.get("thruDate"));
		if(params.get("description") != null)
			attributeMap.put("description", params.get("description"));
		if(params.get("textData") != null)
			attributeMap.put("textData", params.get("textData"));
		if(params.get("userLogin") != null)
			attributeMap.put("userLogin", params.get("userLogin"));
		else {
			try {
				attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
			} catch (GenericEntityException e) {
				e.printStackTrace();
			}
		}
		if(params.get("login.username") != null)
			attributeMap.put("login.username", params.get("login.username"));
		if(params.get("login.password") != null)
			attributeMap.put("login.password", params.get("login.password"));
		if(params.get("locale") != null)
			attributeMap.put("locale", params.get("locale"));
		if(params.get("timeZone") != null)
			attributeMap.put("timeZone", params.get("timeZone"));
		if(params.get("textData") != null)
			attributeMap.put("textData", params.get("textData"));
		try {
			Map result = dispatcher.runSync("updateAgreement", attributeMap);
			return Response.ok().entity(result).build();
		} catch(GenericServiceException e) {
			e.printStackTrace();
			return null;
		}
	}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/updateAgreementItem")
public Response updateAgreementItem_(@Multipart(value = "updateAgreementItem", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("agreementId") != null)
		attributeMap.put("agreementId", params.get("agreementId"));
	if(params.get("agreementItemSeqId") != null)
		attributeMap.put("agreementItemSeqId", params.get("agreementItemSeqId"));
	if(params.get("agreementItemTypeId") != null)
		attributeMap.put("agreementItemTypeId", params.get("agreementItemTypeId"));
	if(params.get("currencyUomId") != null)
		attributeMap.put("currencyUomId", params.get("currencyUomId"));
	if(params.get("agreementText") != null)
		attributeMap.put("agreementText", params.get("agreementText"));
	if(params.get("agreementImage") != null)
		attributeMap.put("agreementImage", params.get("agreementImage"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	if(params.get("agreementText") != null)
		attributeMap.put("agreementText", params.get("agreementText"));
	try {
		Map result = dispatcher.runSync("updateAgreementItem", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}



@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/removeAgreementItem")
public Response removeAgreementItem_(@Multipart(value = "removeAgreementItem", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("agreementId") != null)
		attributeMap.put("agreementId", params.get("agreementId"));
	if(params.get("agreementItemSeqId") != null)
		attributeMap.put("agreementItemSeqId", params.get("agreementItemSeqId"));
	if(params.get("agreementItemTypeId") != null)
		attributeMap.put("agreementItemTypeId", params.get("agreementItemTypeId"));
	if(params.get("currencyUomId") != null)
		attributeMap.put("currencyUomId", params.get("currencyUomId"));
	if(params.get("agreementText") != null)
		attributeMap.put("agreementText", params.get("agreementText"));
	if(params.get("agreementImage") != null)
		attributeMap.put("agreementImage", params.get("agreementImage"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("removeAgreementItem", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}


	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/createAgreement")
	public Response createAgreement_(@Multipart(value = "createAgreement", required = true, type = MediaType.APPLICATION_JSON) Map params) {
		GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
		LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
		Map<String, Object> attributeMap = new HashMap<String, Object>();
		if(params.get("productId") != null)
			attributeMap.put("productId", params.get("productId"));
		if(params.get("partyIdFrom") != null)
			attributeMap.put("partyIdFrom", params.get("partyIdFrom"));
		if(params.get("partyIdTo") != null)
			attributeMap.put("partyIdTo", params.get("partyIdTo"));
		if(params.get("roleTypeIdFrom") != null)
			attributeMap.put("roleTypeIdFrom", params.get("roleTypeIdFrom"));
		if(params.get("roleTypeIdTo") != null)
			attributeMap.put("roleTypeIdTo", params.get("roleTypeIdTo"));
		if(params.get("agreementTypeId") != null)
			attributeMap.put("agreementTypeId", params.get("agreementTypeId"));
		if(params.get("agreementDate") != null)
			attributeMap.put("agreementDate", params.get("agreementDate"));
		if(params.get("fromDate") != null)
			attributeMap.put("fromDate", params.get("fromDate"));
		if(params.get("thruDate") != null)
			attributeMap.put("thruDate", params.get("thruDate"));
		if(params.get("description") != null)
			attributeMap.put("description", params.get("description"));
		if(params.get("textData") != null)
			attributeMap.put("textData", params.get("textData"));
		if(params.get("userLogin") != null)
			attributeMap.put("userLogin", params.get("userLogin"));
		else {
			try {
				attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
			} catch (GenericEntityException e) {
				e.printStackTrace();
			}
		}
		if(params.get("login.username") != null)
			attributeMap.put("login.username", params.get("login.username"));
		if(params.get("login.password") != null)
			attributeMap.put("login.password", params.get("login.password"));
		if(params.get("locale") != null)
			attributeMap.put("locale", params.get("locale"));
		if(params.get("timeZone") != null)
			attributeMap.put("timeZone", params.get("timeZone"));
		if(params.get("textData") != null)
			attributeMap.put("textData", params.get("textData"));
		try {
			Map result = dispatcher.runSync("createAgreement", attributeMap);
			return Response.ok().entity(result).build();
		} catch(GenericServiceException e) {
			e.printStackTrace();
			return null;
		}
	}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/createAgreementTerm")
public Response createAgreementTerm_(@Multipart(value = "createAgreementTerm", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("agreementTermId") != null)
		attributeMap.put("agreementTermId", params.get("agreementTermId"));
	if(params.get("termTypeId") != null)
		attributeMap.put("termTypeId", params.get("termTypeId"));
	if(params.get("agreementId") != null)
		attributeMap.put("agreementId", params.get("agreementId"));
	if(params.get("agreementItemSeqId") != null)
		attributeMap.put("agreementItemSeqId", params.get("agreementItemSeqId"));
	if(params.get("invoiceItemTypeId") != null)
		attributeMap.put("invoiceItemTypeId", params.get("invoiceItemTypeId"));
	if(params.get("fromDate") != null)
		attributeMap.put("fromDate", params.get("fromDate"));
	if(params.get("thruDate") != null)
		attributeMap.put("thruDate", params.get("thruDate"));
	if(params.get("termValue") != null)
		attributeMap.put("termValue", params.get("termValue"));
	if(params.get("termDays") != null)
		attributeMap.put("termDays", params.get("termDays"));
	if(params.get("textValue") != null)
		attributeMap.put("textValue", params.get("textValue"));
	if(params.get("minQuantity") != null)
		attributeMap.put("minQuantity", params.get("minQuantity"));
	if(params.get("maxQuantity") != null)
		attributeMap.put("maxQuantity", params.get("maxQuantity"));
	if(params.get("description") != null)
		attributeMap.put("description", params.get("description"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	if(params.get("textValue") != null)
		attributeMap.put("textValue", params.get("textValue"));
	try {
		Map result = dispatcher.runSync("createAgreementTerm", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/cancelAgreement")
public Response cancelAgreement_(@Multipart(value = "cancelAgreement", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("agreementId") != null)
		attributeMap.put("agreementId", params.get("agreementId"));
	if(params.get("productId") != null)
		attributeMap.put("productId", params.get("productId"));
	if(params.get("partyIdFrom") != null)
		attributeMap.put("partyIdFrom", params.get("partyIdFrom"));
	if(params.get("partyIdTo") != null)
		attributeMap.put("partyIdTo", params.get("partyIdTo"));
	if(params.get("roleTypeIdFrom") != null)
		attributeMap.put("roleTypeIdFrom", params.get("roleTypeIdFrom"));
	if(params.get("roleTypeIdTo") != null)
		attributeMap.put("roleTypeIdTo", params.get("roleTypeIdTo"));
	if(params.get("agreementTypeId") != null)
		attributeMap.put("agreementTypeId", params.get("agreementTypeId"));
	if(params.get("agreementDate") != null)
		attributeMap.put("agreementDate", params.get("agreementDate"));
	if(params.get("fromDate") != null)
		attributeMap.put("fromDate", params.get("fromDate"));
	if(params.get("thruDate") != null)
		attributeMap.put("thruDate", params.get("thruDate"));
	if(params.get("description") != null)
		attributeMap.put("description", params.get("description"));
	if(params.get("textData") != null)
		attributeMap.put("textData", params.get("textData"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("cancelAgreement", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/updateAgreementTerm")
public Response updateAgreementTerm_(@Multipart(value = "updateAgreementTerm", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("agreementTermId") != null)
		attributeMap.put("agreementTermId", params.get("agreementTermId"));
	if(params.get("termTypeId") != null)
		attributeMap.put("termTypeId", params.get("termTypeId"));
	if(params.get("agreementId") != null)
		attributeMap.put("agreementId", params.get("agreementId"));
	if(params.get("agreementItemSeqId") != null)
		attributeMap.put("agreementItemSeqId", params.get("agreementItemSeqId"));
	if(params.get("invoiceItemTypeId") != null)
		attributeMap.put("invoiceItemTypeId", params.get("invoiceItemTypeId"));
	if(params.get("fromDate") != null)
		attributeMap.put("fromDate", params.get("fromDate"));
	if(params.get("thruDate") != null)
		attributeMap.put("thruDate", params.get("thruDate"));
	if(params.get("termValue") != null)
		attributeMap.put("termValue", params.get("termValue"));
	if(params.get("termDays") != null)
		attributeMap.put("termDays", params.get("termDays"));
	if(params.get("textValue") != null)
		attributeMap.put("textValue", params.get("textValue"));
	if(params.get("minQuantity") != null)
		attributeMap.put("minQuantity", params.get("minQuantity"));
	if(params.get("maxQuantity") != null)
		attributeMap.put("maxQuantity", params.get("maxQuantity"));
	if(params.get("description") != null)
		attributeMap.put("description", params.get("description"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	if(params.get("textValue") != null)
		attributeMap.put("textValue", params.get("textValue"));
	try {
		Map result = dispatcher.runSync("updateAgreementTerm", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/deleteAgreementTerm")
public Response deleteAgreementTerm_(@Multipart(value = "deleteAgreementTerm", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("agreementTermId") != null)
		attributeMap.put("agreementTermId", params.get("agreementTermId"));
	if(params.get("termTypeId") != null)
		attributeMap.put("termTypeId", params.get("termTypeId"));
	if(params.get("agreementId") != null)
		attributeMap.put("agreementId", params.get("agreementId"));
	if(params.get("agreementItemSeqId") != null)
		attributeMap.put("agreementItemSeqId", params.get("agreementItemSeqId"));
	if(params.get("invoiceItemTypeId") != null)
		attributeMap.put("invoiceItemTypeId", params.get("invoiceItemTypeId"));
	if(params.get("fromDate") != null)
		attributeMap.put("fromDate", params.get("fromDate"));
	if(params.get("thruDate") != null)
		attributeMap.put("thruDate", params.get("thruDate"));
	if(params.get("termValue") != null)
		attributeMap.put("termValue", params.get("termValue"));
	if(params.get("termDays") != null)
		attributeMap.put("termDays", params.get("termDays"));
	if(params.get("textValue") != null)
		attributeMap.put("textValue", params.get("textValue"));
	if(params.get("minQuantity") != null)
		attributeMap.put("minQuantity", params.get("minQuantity"));
	if(params.get("maxQuantity") != null)
		attributeMap.put("maxQuantity", params.get("maxQuantity"));
	if(params.get("description") != null)
		attributeMap.put("description", params.get("description"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("deleteAgreementTerm", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}


@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/copyAgreement")
public Response copyAgreement_(@Multipart(value = "copyAgreement", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("agreementId") != null)
		attributeMap.put("agreementId", params.get("agreementId"));
	if(params.get("copyAgreementTerms") != null)
		attributeMap.put("copyAgreementTerms", params.get("copyAgreementTerms"));
	if(params.get("copyAgreementProducts") != null)
		attributeMap.put("copyAgreementProducts", params.get("copyAgreementProducts"));
	if(params.get("copyAgreementParties") != null)
		attributeMap.put("copyAgreementParties", params.get("copyAgreementParties"));
	if(params.get("copyAgreementFacilities") != null)
		attributeMap.put("copyAgreementFacilities", params.get("copyAgreementFacilities"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("copyAgreement", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/createVendor")
public Response createVendor_(@Multipart(value = "createVendor", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("partyId") != null)
		attributeMap.put("partyId", params.get("partyId"));
	if(params.get("manifestCompanyName") != null)
		attributeMap.put("manifestCompanyName", params.get("manifestCompanyName"));
	if(params.get("manifestCompanyTitle") != null)
		attributeMap.put("manifestCompanyTitle", params.get("manifestCompanyTitle"));
	if(params.get("manifestLogoUrl") != null)
		attributeMap.put("manifestLogoUrl", params.get("manifestLogoUrl"));
	if(params.get("manifestPolicies") != null)
		attributeMap.put("manifestPolicies", params.get("manifestPolicies"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("createVendor", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}
@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/updateVendor")
public Response updateVendor_(@Multipart(value = "updateVendor", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("partyId") != null)
		attributeMap.put("partyId", params.get("partyId"));
	if(params.get("manifestCompanyName") != null)
		attributeMap.put("manifestCompanyName", params.get("manifestCompanyName"));
	if(params.get("manifestCompanyTitle") != null)
		attributeMap.put("manifestCompanyTitle", params.get("manifestCompanyTitle"));
	if(params.get("manifestLogoUrl") != null)
		attributeMap.put("manifestLogoUrl", params.get("manifestLogoUrl"));
	if(params.get("manifestPolicies") != null)
		attributeMap.put("manifestPolicies", params.get("manifestPolicies"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("updateVendor", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}


@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/createAgreementItem")
public Response createAgreementItem_(@Multipart(value = "createAgreementItem", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("agreementId") != null)
		attributeMap.put("agreementId", params.get("agreementId"));
	if(params.get("agreementItemSeqId") != null)
		attributeMap.put("agreementItemSeqId", params.get("agreementItemSeqId"));
	if(params.get("agreementItemTypeId") != null)
		attributeMap.put("agreementItemTypeId", params.get("agreementItemTypeId"));
	if(params.get("currencyUomId") != null)
		attributeMap.put("currencyUomId", params.get("currencyUomId"));
	if(params.get("agreementText") != null)
		attributeMap.put("agreementText", params.get("agreementText"));
	if(params.get("agreementImage") != null)
		attributeMap.put("agreementImage", params.get("agreementImage"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	if(params.get("agreementText") != null)
		attributeMap.put("agreementText", params.get("agreementText"));
	if(params.get("agreementItemSeqId") != null)
		attributeMap.put("agreementItemSeqId", params.get("agreementItemSeqId"));
	try {
		Map result = dispatcher.runSync("createAgreementItem", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}


@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/createAgreementPromoAppl")
public Response createAgreementPromoAppl_(@Multipart(value = "createAgreementPromoAppl", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("agreementId") != null)
		attributeMap.put("agreementId", params.get("agreementId"));
	if(params.get("agreementItemSeqId") != null)
		attributeMap.put("agreementItemSeqId", params.get("agreementItemSeqId"));
	if(params.get("productPromoId") != null)
		attributeMap.put("productPromoId", params.get("productPromoId"));
	if(params.get("fromDate") != null)
		attributeMap.put("fromDate", params.get("fromDate"));
	if(params.get("thruDate") != null)
		attributeMap.put("thruDate", params.get("thruDate"));
	if(params.get("sequenceNum") != null)
		attributeMap.put("sequenceNum", params.get("sequenceNum"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("createAgreementPromoAppl", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/createAgreementProductAppl")
public Response createAgreementProductAppl_(@Multipart(value = "createAgreementProductAppl", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("agreementId") != null)
		attributeMap.put("agreementId", params.get("agreementId"));
	if(params.get("agreementItemSeqId") != null)
		attributeMap.put("agreementItemSeqId", params.get("agreementItemSeqId"));
	if(params.get("productId") != null)
		attributeMap.put("productId", params.get("productId"));
	if(params.get("price") != null)
		attributeMap.put("price", params.get("price"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("createAgreementProductAppl", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}


@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/createAgreementFacilityAppl")
public Response createAgreementFacilityAppl_(@Multipart(value = "createAgreementFacilityAppl", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("agreementId") != null)
		attributeMap.put("agreementId", params.get("agreementId"));
	if(params.get("agreementItemSeqId") != null)
		attributeMap.put("agreementItemSeqId", params.get("agreementItemSeqId"));
	if(params.get("facilityId") != null)
		attributeMap.put("facilityId", params.get("facilityId"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("createAgreementFacilityAppl", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/createAgreementPartyApplic")
public Response createAgreementPartyApplic_(@Multipart(value = "createAgreementPartyApplic", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("agreementId") != null)
		attributeMap.put("agreementId", params.get("agreementId"));
	if(params.get("agreementItemSeqId") != null)
		attributeMap.put("agreementItemSeqId", params.get("agreementItemSeqId"));
	if(params.get("partyId") != null)
		attributeMap.put("partyId", params.get("partyId"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("createAgreementPartyApplic", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/createAgreementGeographicalApplic")
public Response createAgreementGeographicalApplic_(@Multipart(value = "createAgreementGeographicalApplic", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("agreementId") != null)
		attributeMap.put("agreementId", params.get("agreementId"));
	if(params.get("agreementItemSeqId") != null)
		attributeMap.put("agreementItemSeqId", params.get("agreementItemSeqId"));
	if(params.get("geoId") != null)
		attributeMap.put("geoId", params.get("geoId"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("createAgreementGeographicalApplic", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}



@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/updateAgreementRole")
public Response updateAgreementRole_(@Multipart(value = "updateAgreementRole", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("agreementId") != null)
		attributeMap.put("agreementId", params.get("agreementId"));
	if(params.get("partyId") != null)
		attributeMap.put("partyId", params.get("partyId"));
	if(params.get("roleTypeId") != null)
		attributeMap.put("roleTypeId", params.get("roleTypeId"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("updateAgreementRole", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/deleteAgreementRole")
public Response deleteAgreementRole_(@Multipart(value = "deleteAgreementRole", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("agreementId") != null)
		attributeMap.put("agreementId", params.get("agreementId"));
	if(params.get("partyId") != null)
		attributeMap.put("partyId", params.get("partyId"));
	if(params.get("roleTypeId") != null)
		attributeMap.put("roleTypeId", params.get("roleTypeId"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("deleteAgreementRole", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/removeAgreementContent")
public Response removeAgreementContent_(@Multipart(value = "removeAgreementContent", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("agreementId") != null)
		attributeMap.put("agreementId", params.get("agreementId"));
	if(params.get("agreementItemSeqId") != null)
		attributeMap.put("agreementItemSeqId", params.get("agreementItemSeqId"));
	if(params.get("agreementContentTypeId") != null)
		attributeMap.put("agreementContentTypeId", params.get("agreementContentTypeId"));
	if(params.get("contentId") != null)
		attributeMap.put("contentId", params.get("contentId"));
	if(params.get("fromDate") != null)
		attributeMap.put("fromDate", params.get("fromDate"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("removeAgreementContent", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/createAgreementRole")
public Response createAgreementRole_(@Multipart(value = "createAgreementRole", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("agreementId") != null)
		attributeMap.put("agreementId", params.get("agreementId"));
	if(params.get("partyId") != null)
		attributeMap.put("partyId", params.get("partyId"));
	if(params.get("roleTypeId") != null)
		attributeMap.put("roleTypeId", params.get("roleTypeId"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("createAgreementRole", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/uploadAgreementContentFile")
public Response uploadAgreementContentFile_(@Multipart(value = "uploadAgreementContentFile", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	if(params.get("dataResourceTypeId") != null)
		attributeMap.put("dataResourceTypeId", params.get("dataResourceTypeId"));
	if(params.get("dataTemplateTypeId") != null)
		attributeMap.put("dataTemplateTypeId", params.get("dataTemplateTypeId"));
	if(params.get("dataCategoryId") != null)
		attributeMap.put("dataCategoryId", params.get("dataCategoryId"));
	if(params.get("dataSourceId") != null)
		attributeMap.put("dataSourceId", params.get("dataSourceId"));
	if(params.get("statusId") != null)
		attributeMap.put("statusId", params.get("statusId"));
	if(params.get("dataResourceName") != null)
		attributeMap.put("dataResourceName", params.get("dataResourceName"));
	if(params.get("localeString") != null)
		attributeMap.put("localeString", params.get("localeString"));
	if(params.get("mimeTypeId") != null)
		attributeMap.put("mimeTypeId", params.get("mimeTypeId"));
	if(params.get("characterSetId") != null)
		attributeMap.put("characterSetId", params.get("characterSetId"));
	if(params.get("objectInfo") != null)
		attributeMap.put("objectInfo", params.get("objectInfo"));
	if(params.get("surveyId") != null)
		attributeMap.put("surveyId", params.get("surveyId"));
	if(params.get("surveyResponseId") != null)
		attributeMap.put("surveyResponseId", params.get("surveyResponseId"));
	if(params.get("relatedDetailId") != null)
		attributeMap.put("relatedDetailId", params.get("relatedDetailId"));
	if(params.get("isPublic") != null)
		attributeMap.put("isPublic", params.get("isPublic"));
	if(params.get("createdDate") != null)
		attributeMap.put("createdDate", params.get("createdDate"));
	if(params.get("createdByUserLogin") != null)
		attributeMap.put("createdByUserLogin", params.get("createdByUserLogin"));
	if(params.get("lastModifiedDate") != null)
		attributeMap.put("lastModifiedDate", params.get("lastModifiedDate"));
	if(params.get("lastModifiedByUserLogin") != null)
		attributeMap.put("lastModifiedByUserLogin", params.get("lastModifiedByUserLogin"));
	if(params.get("dataResourceId") != null)
		attributeMap.put("dataResourceId", params.get("dataResourceId"));
	if(params.get("targetOperationList") != null)
		attributeMap.put("targetOperationList", params.get("targetOperationList"));
	if(params.get("contentPurposeList") != null)
		attributeMap.put("contentPurposeList", params.get("contentPurposeList"));
	if(params.get("skipPermissionCheck") != null)
		attributeMap.put("skipPermissionCheck", params.get("skipPermissionCheck"));
	if(params.get("roleTypeId") != null)
		attributeMap.put("roleTypeId", params.get("roleTypeId"));
	if(params.get("partyId") != null)
		attributeMap.put("partyId", params.get("partyId"));
	if(params.get("uploadedFile") != null)
		attributeMap.put("uploadedFile", params.get("uploadedFile"));
	if(params.get("rootDir") != null)
		attributeMap.put("rootDir", params.get("rootDir"));
	if(params.get("_uploadedFile_fileName") != null)
		attributeMap.put("_uploadedFile_fileName", params.get("_uploadedFile_fileName"));
	if(params.get("_uploadedFile_contentType") != null)
		attributeMap.put("_uploadedFile_contentType", params.get("_uploadedFile_contentType"));
	if(params.get("contentId") != null)
		attributeMap.put("contentId", params.get("contentId"));
	if(params.get("contentTypeId") != null)
		attributeMap.put("contentTypeId", params.get("contentTypeId"));
	if(params.get("ownerContentId") != null)
		attributeMap.put("ownerContentId", params.get("ownerContentId"));
	if(params.get("decoratorContentId") != null)
		attributeMap.put("decoratorContentId", params.get("decoratorContentId"));
	if(params.get("instanceOfContentId") != null)
		attributeMap.put("instanceOfContentId", params.get("instanceOfContentId"));
	if(params.get("templateDataResourceId") != null)
		attributeMap.put("templateDataResourceId", params.get("templateDataResourceId"));
	if(params.get("privilegeEnumId") != null)
		attributeMap.put("privilegeEnumId", params.get("privilegeEnumId"));
	if(params.get("serviceName") != null)
		attributeMap.put("serviceName", params.get("serviceName"));
	if(params.get("customMethodId") != null)
		attributeMap.put("customMethodId", params.get("customMethodId"));
	if(params.get("contentName") != null)
		attributeMap.put("contentName", params.get("contentName"));
	if(params.get("description") != null)
		attributeMap.put("description", params.get("description"));
	if(params.get("childLeafCount") != null)
		attributeMap.put("childLeafCount", params.get("childLeafCount"));
	if(params.get("childBranchCount") != null)
		attributeMap.put("childBranchCount", params.get("childBranchCount"));
	if(params.get("targetOperationString") != null)
		attributeMap.put("targetOperationString", params.get("targetOperationString"));
	if(params.get("contentPurposeString") != null)
		attributeMap.put("contentPurposeString", params.get("contentPurposeString"));
	if(params.get("displayFailCond") != null)
		attributeMap.put("displayFailCond", params.get("displayFailCond"));
	if(params.get("roleTypeList") != null)
		attributeMap.put("roleTypeList", params.get("roleTypeList"));
	if(params.get("contentPurposeTypeId") != null)
		attributeMap.put("contentPurposeTypeId", params.get("contentPurposeTypeId"));
	if(params.get("contentAssocTypeId") != null)
		attributeMap.put("contentAssocTypeId", params.get("contentAssocTypeId"));
	if(params.get("contentIdFrom") != null)
		attributeMap.put("contentIdFrom", params.get("contentIdFrom"));
	if(params.get("contentIdTo") != null)
		attributeMap.put("contentIdTo", params.get("contentIdTo"));
	if(params.get("mapKey") != null)
		attributeMap.put("mapKey", params.get("mapKey"));
	if(params.get("fromDate") != null)
		attributeMap.put("fromDate", params.get("fromDate"));
	if(params.get("sequenceNum") != null)
		attributeMap.put("sequenceNum", params.get("sequenceNum"));
	if(params.get("agreementId") != null)
		attributeMap.put("agreementId", params.get("agreementId"));
	if(params.get("agreementItemSeqId") != null)
		attributeMap.put("agreementItemSeqId", params.get("agreementItemSeqId"));
	if(params.get("agreementContentTypeId") != null)
		attributeMap.put("agreementContentTypeId", params.get("agreementContentTypeId"));
	if(params.get("thruDate") != null)
		attributeMap.put("thruDate", params.get("thruDate"));
	try {
		Map result = dispatcher.runSync("uploadAgreementContentFile", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/createAgreementWorkEffortApplic")
public Response createAgreementWorkEffortApplic_(@Multipart(value = "createAgreementWorkEffortApplic", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("agreementId") != null)
		attributeMap.put("agreementId", params.get("agreementId"));
	if(params.get("agreementItemSeqId") != null)
		attributeMap.put("agreementItemSeqId", params.get("agreementItemSeqId"));
	if(params.get("workEffortId") != null)
		attributeMap.put("workEffortId", params.get("workEffortId"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	if(params.get("agreementItemSeqId") != null)
		attributeMap.put("agreementItemSeqId", params.get("agreementItemSeqId"));
	try {
		Map result = dispatcher.runSync("createAgreementWorkEffortApplic", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}


@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/deleteAgreementWorkEffortApplic")
public Response deleteAgreementWorkEffortApplic_(@Multipart(value = "deleteAgreementWorkEffortApplic", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("agreementId") != null)
		attributeMap.put("agreementId", params.get("agreementId"));
	if(params.get("agreementItemSeqId") != null)
		attributeMap.put("agreementItemSeqId", params.get("agreementItemSeqId"));
	if(params.get("workEffortId") != null)
		attributeMap.put("workEffortId", params.get("workEffortId"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("deleteAgreementWorkEffortApplic", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}

	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/createInvoice")
	public Response createInvoice_(@Multipart(value = "createInvoice", required = true, type = MediaType.APPLICATION_JSON) Map params) {
		GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
		LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
		Map<String, Object> attributeMap = new HashMap<String, Object>();
		if(params.get("invoiceId") != null)
			attributeMap.put("invoiceId", params.get("invoiceId"));
		if(params.get("invoiceTypeId") != null)
			attributeMap.put("invoiceTypeId", params.get("invoiceTypeId"));
		if(params.get("partyIdFrom") != null)
			attributeMap.put("partyIdFrom", params.get("partyIdFrom"));
		if(params.get("partyId") != null)
			attributeMap.put("partyId", params.get("partyId"));
		if(params.get("roleTypeId") != null)
			attributeMap.put("roleTypeId", params.get("roleTypeId"));
		if(params.get("statusId") != null)
			attributeMap.put("statusId", params.get("statusId"));
		if(params.get("billingAccountId") != null)
			attributeMap.put("billingAccountId", params.get("billingAccountId"));
		if(params.get("contactMechId") != null)
			attributeMap.put("contactMechId", params.get("contactMechId"));
		if(params.get("invoiceDate") != null)
			attributeMap.put("invoiceDate", params.get("invoiceDate"));
		if(params.get("dueDate") != null)
			attributeMap.put("dueDate", params.get("dueDate"));
		if(params.get("paidDate") != null)
			attributeMap.put("paidDate", params.get("paidDate"));
		if(params.get("invoiceMessage") != null)
			attributeMap.put("invoiceMessage", params.get("invoiceMessage"));
		if(params.get("referenceNumber") != null)
			attributeMap.put("referenceNumber", params.get("referenceNumber"));
		if(params.get("description") != null)
			attributeMap.put("description", params.get("description"));
		if(params.get("currencyUomId") != null)
			attributeMap.put("currencyUomId", params.get("currencyUomId"));
		if(params.get("recurrenceInfoId") != null)
			attributeMap.put("recurrenceInfoId", params.get("recurrenceInfoId"));
		if(params.get("userLogin") != null)
			attributeMap.put("userLogin", params.get("userLogin"));
		else {
			try {
				attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
			} catch (GenericEntityException e) {
				e.printStackTrace();
			}
		}
		if(params.get("login.username") != null)
			attributeMap.put("login.username", params.get("login.username"));
		if(params.get("login.password") != null)
			attributeMap.put("login.password", params.get("login.password"));
		if(params.get("locale") != null)
			attributeMap.put("locale", params.get("locale"));
		if(params.get("timeZone") != null)
			attributeMap.put("timeZone", params.get("timeZone"));
		if(params.get("invoiceTypeId") != null)
			attributeMap.put("invoiceTypeId", params.get("invoiceTypeId"));
		if(params.get("partyIdFrom") != null)
			attributeMap.put("partyIdFrom", params.get("partyIdFrom"));
		if(params.get("partyId") != null)
			attributeMap.put("partyId", params.get("partyId"));
		if(params.get("description") != null)
			attributeMap.put("description", params.get("description"));
		if(params.get("invoiceMessage") != null)
			attributeMap.put("invoiceMessage", params.get("invoiceMessage"));
		try {
			Map result = dispatcher.runSync("createInvoice", attributeMap);
			return Response.ok().entity(result).build();
		} catch(GenericServiceException e) {
			e.printStackTrace();
			return null;
		}
	}


@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/massChangeInvoiceStatus")
public Response massChangeInvoiceStatus_(@Multipart(value = "massChangeInvoiceStatus", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("invoiceIds") != null)
		attributeMap.put("invoiceIds", params.get("invoiceIds"));
	if(params.get("statusId") != null)
		attributeMap.put("statusId", params.get("statusId"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("massChangeInvoiceStatus", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}


@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/copyInvoice")
public Response copyInvoice_(@Multipart(value = "copyInvoice", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("invoiceId") != null)
		attributeMap.put("invoiceId", params.get("invoiceId"));
	if(params.get("invoiceIdToCopyFrom") != null)
		attributeMap.put("invoiceIdToCopyFrom", params.get("invoiceIdToCopyFrom"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("copyInvoice", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/createPartyTaxAuthInfo")
public Response createPartyTaxAuthInfo_(@Multipart(value = "createPartyTaxAuthInfo", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("partyId") != null)
		attributeMap.put("partyId", params.get("partyId"));
	if(params.get("taxAuthGeoId") != null)
		attributeMap.put("taxAuthGeoId", params.get("taxAuthGeoId"));
	if(params.get("taxAuthPartyId") != null)
		attributeMap.put("taxAuthPartyId", params.get("taxAuthPartyId"));
	if(params.get("fromDate") != null)
		attributeMap.put("fromDate", params.get("fromDate"));
	if(params.get("thruDate") != null)
		attributeMap.put("thruDate", params.get("thruDate"));
	if(params.get("partyTaxId") != null)
		attributeMap.put("partyTaxId", params.get("partyTaxId"));
	if(params.get("isExempt") != null)
		attributeMap.put("isExempt", params.get("isExempt"));
	if(params.get("isNexus") != null)
		attributeMap.put("isNexus", params.get("isNexus"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	if(params.get("fromDate") != null)
		attributeMap.put("fromDate", params.get("fromDate"));
	try {
		Map result = dispatcher.runSync("createPartyTaxAuthInfo", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/updatePartyTaxAuthInfo")
public Response updatePartyTaxAuthInfo_(@Multipart(value = "updatePartyTaxAuthInfo", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("partyId") != null)
		attributeMap.put("partyId", params.get("partyId"));
	if(params.get("taxAuthGeoId") != null)
		attributeMap.put("taxAuthGeoId", params.get("taxAuthGeoId"));
	if(params.get("taxAuthPartyId") != null)
		attributeMap.put("taxAuthPartyId", params.get("taxAuthPartyId"));
	if(params.get("fromDate") != null)
		attributeMap.put("fromDate", params.get("fromDate"));
	if(params.get("thruDate") != null)
		attributeMap.put("thruDate", params.get("thruDate"));
	if(params.get("partyTaxId") != null)
		attributeMap.put("partyTaxId", params.get("partyTaxId"));
	if(params.get("isExempt") != null)
		attributeMap.put("isExempt", params.get("isExempt"));
	if(params.get("isNexus") != null)
		attributeMap.put("isNexus", params.get("isNexus"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("updatePartyTaxAuthInfo", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/deletePartyTaxAuthInfo")
public Response deletePartyTaxAuthInfo_(@Multipart(value = "deletePartyTaxAuthInfo", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("partyId") != null)
		attributeMap.put("partyId", params.get("partyId"));
	if(params.get("taxAuthGeoId") != null)
		attributeMap.put("taxAuthGeoId", params.get("taxAuthGeoId"));
	if(params.get("taxAuthPartyId") != null)
		attributeMap.put("taxAuthPartyId", params.get("taxAuthPartyId"));
	if(params.get("fromDate") != null)
		attributeMap.put("fromDate", params.get("fromDate"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("deletePartyTaxAuthInfo", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}


@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/createTaxAuthorityRateProduct")
public Response createTaxAuthorityRateProduct_(@Multipart(value = "createTaxAuthorityRateProduct", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("taxAuthGeoId") != null)
		attributeMap.put("taxAuthGeoId", params.get("taxAuthGeoId"));
	if(params.get("taxAuthPartyId") != null)
		attributeMap.put("taxAuthPartyId", params.get("taxAuthPartyId"));
	if(params.get("taxAuthorityRateTypeId") != null)
		attributeMap.put("taxAuthorityRateTypeId", params.get("taxAuthorityRateTypeId"));
	if(params.get("productStoreId") != null)
		attributeMap.put("productStoreId", params.get("productStoreId"));
	if(params.get("productCategoryId") != null)
		attributeMap.put("productCategoryId", params.get("productCategoryId"));
	if(params.get("titleTransferEnumId") != null)
		attributeMap.put("titleTransferEnumId", params.get("titleTransferEnumId"));
	if(params.get("minItemPrice") != null)
		attributeMap.put("minItemPrice", params.get("minItemPrice"));
	if(params.get("minPurchase") != null)
		attributeMap.put("minPurchase", params.get("minPurchase"));
	if(params.get("taxShipping") != null)
		attributeMap.put("taxShipping", params.get("taxShipping"));
	if(params.get("taxPercentage") != null)
		attributeMap.put("taxPercentage", params.get("taxPercentage"));
	if(params.get("taxPromotions") != null)
		attributeMap.put("taxPromotions", params.get("taxPromotions"));
	if(params.get("fromDate") != null)
		attributeMap.put("fromDate", params.get("fromDate"));
	if(params.get("thruDate") != null)
		attributeMap.put("thruDate", params.get("thruDate"));
	if(params.get("description") != null)
		attributeMap.put("description", params.get("description"));
	if(params.get("isTaxInShippingPrice") != null)
		attributeMap.put("isTaxInShippingPrice", params.get("isTaxInShippingPrice"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("createTaxAuthorityRateProduct", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/updateTaxAuthorityRateProduct")
public Response updateTaxAuthorityRateProduct_(@Multipart(value = "updateTaxAuthorityRateProduct", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("taxAuthorityRateSeqId") != null)
		attributeMap.put("taxAuthorityRateSeqId", params.get("taxAuthorityRateSeqId"));
	if(params.get("taxAuthGeoId") != null)
		attributeMap.put("taxAuthGeoId", params.get("taxAuthGeoId"));
	if(params.get("taxAuthPartyId") != null)
		attributeMap.put("taxAuthPartyId", params.get("taxAuthPartyId"));
	if(params.get("taxAuthorityRateTypeId") != null)
		attributeMap.put("taxAuthorityRateTypeId", params.get("taxAuthorityRateTypeId"));
	if(params.get("productStoreId") != null)
		attributeMap.put("productStoreId", params.get("productStoreId"));
	if(params.get("productCategoryId") != null)
		attributeMap.put("productCategoryId", params.get("productCategoryId"));
	if(params.get("titleTransferEnumId") != null)
		attributeMap.put("titleTransferEnumId", params.get("titleTransferEnumId"));
	if(params.get("minItemPrice") != null)
		attributeMap.put("minItemPrice", params.get("minItemPrice"));
	if(params.get("minPurchase") != null)
		attributeMap.put("minPurchase", params.get("minPurchase"));
	if(params.get("taxShipping") != null)
		attributeMap.put("taxShipping", params.get("taxShipping"));
	if(params.get("taxPercentage") != null)
		attributeMap.put("taxPercentage", params.get("taxPercentage"));
	if(params.get("taxPromotions") != null)
		attributeMap.put("taxPromotions", params.get("taxPromotions"));
	if(params.get("fromDate") != null)
		attributeMap.put("fromDate", params.get("fromDate"));
	if(params.get("thruDate") != null)
		attributeMap.put("thruDate", params.get("thruDate"));
	if(params.get("description") != null)
		attributeMap.put("description", params.get("description"));
	if(params.get("isTaxInShippingPrice") != null)
		attributeMap.put("isTaxInShippingPrice", params.get("isTaxInShippingPrice"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("updateTaxAuthorityRateProduct", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/deleteTaxAuthorityRateProduct")
public Response deleteTaxAuthorityRateProduct_(@Multipart(value = "deleteTaxAuthorityRateProduct", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("taxAuthorityRateSeqId") != null)
		attributeMap.put("taxAuthorityRateSeqId", params.get("taxAuthorityRateSeqId"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("deleteTaxAuthorityRateProduct", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}


@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/createTaxAuthorityGlAccount")
public Response createTaxAuthorityGlAccount_(@Multipart(value = "createTaxAuthorityGlAccount", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("taxAuthGeoId") != null)
		attributeMap.put("taxAuthGeoId", params.get("taxAuthGeoId"));
	if(params.get("taxAuthPartyId") != null)
		attributeMap.put("taxAuthPartyId", params.get("taxAuthPartyId"));
	if(params.get("organizationPartyId") != null)
		attributeMap.put("organizationPartyId", params.get("organizationPartyId"));
	if(params.get("glAccountId") != null)
		attributeMap.put("glAccountId", params.get("glAccountId"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("createTaxAuthorityGlAccount", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/updateTaxAuthorityGlAccount")
public Response updateTaxAuthorityGlAccount_(@Multipart(value = "updateTaxAuthorityGlAccount", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("taxAuthGeoId") != null)
		attributeMap.put("taxAuthGeoId", params.get("taxAuthGeoId"));
	if(params.get("taxAuthPartyId") != null)
		attributeMap.put("taxAuthPartyId", params.get("taxAuthPartyId"));
	if(params.get("organizationPartyId") != null)
		attributeMap.put("organizationPartyId", params.get("organizationPartyId"));
	if(params.get("glAccountId") != null)
		attributeMap.put("glAccountId", params.get("glAccountId"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("updateTaxAuthorityGlAccount", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/deleteTaxAuthorityGlAccount")
public Response deleteTaxAuthorityGlAccount_(@Multipart(value = "deleteTaxAuthorityGlAccount", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("taxAuthGeoId") != null)
		attributeMap.put("taxAuthGeoId", params.get("taxAuthGeoId"));
	if(params.get("taxAuthPartyId") != null)
		attributeMap.put("taxAuthPartyId", params.get("taxAuthPartyId"));
	if(params.get("organizationPartyId") != null)
		attributeMap.put("organizationPartyId", params.get("organizationPartyId"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("deleteTaxAuthorityGlAccount", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}


@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/createTaxAuthorityAssoc")
public Response createTaxAuthorityAssoc_(@Multipart(value = "createTaxAuthorityAssoc", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("taxAuthGeoId") != null)
		attributeMap.put("taxAuthGeoId", params.get("taxAuthGeoId"));
	if(params.get("taxAuthPartyId") != null)
		attributeMap.put("taxAuthPartyId", params.get("taxAuthPartyId"));
	if(params.get("toTaxAuthGeoId") != null)
		attributeMap.put("toTaxAuthGeoId", params.get("toTaxAuthGeoId"));
	if(params.get("toTaxAuthPartyId") != null)
		attributeMap.put("toTaxAuthPartyId", params.get("toTaxAuthPartyId"));
	if(params.get("fromDate") != null)
		attributeMap.put("fromDate", params.get("fromDate"));
	if(params.get("thruDate") != null)
		attributeMap.put("thruDate", params.get("thruDate"));
	if(params.get("taxAuthorityAssocTypeId") != null)
		attributeMap.put("taxAuthorityAssocTypeId", params.get("taxAuthorityAssocTypeId"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	if(params.get("fromDate") != null)
		attributeMap.put("fromDate", params.get("fromDate"));
	try {
		Map result = dispatcher.runSync("createTaxAuthorityAssoc", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/updateTaxAuthorityAssoc")
public Response updateTaxAuthorityAssoc_(@Multipart(value = "updateTaxAuthorityAssoc", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("taxAuthGeoId") != null)
		attributeMap.put("taxAuthGeoId", params.get("taxAuthGeoId"));
	if(params.get("taxAuthPartyId") != null)
		attributeMap.put("taxAuthPartyId", params.get("taxAuthPartyId"));
	if(params.get("toTaxAuthGeoId") != null)
		attributeMap.put("toTaxAuthGeoId", params.get("toTaxAuthGeoId"));
	if(params.get("toTaxAuthPartyId") != null)
		attributeMap.put("toTaxAuthPartyId", params.get("toTaxAuthPartyId"));
	if(params.get("fromDate") != null)
		attributeMap.put("fromDate", params.get("fromDate"));
	if(params.get("thruDate") != null)
		attributeMap.put("thruDate", params.get("thruDate"));
	if(params.get("taxAuthorityAssocTypeId") != null)
		attributeMap.put("taxAuthorityAssocTypeId", params.get("taxAuthorityAssocTypeId"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("updateTaxAuthorityAssoc", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/deleteTaxAuthorityAssoc")
public Response deleteTaxAuthorityAssoc_(@Multipart(value = "deleteTaxAuthorityAssoc", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("taxAuthGeoId") != null)
		attributeMap.put("taxAuthGeoId", params.get("taxAuthGeoId"));
	if(params.get("taxAuthPartyId") != null)
		attributeMap.put("taxAuthPartyId", params.get("taxAuthPartyId"));
	if(params.get("toTaxAuthGeoId") != null)
		attributeMap.put("toTaxAuthGeoId", params.get("toTaxAuthGeoId"));
	if(params.get("toTaxAuthPartyId") != null)
		attributeMap.put("toTaxAuthPartyId", params.get("toTaxAuthPartyId"));
	if(params.get("fromDate") != null)
		attributeMap.put("fromDate", params.get("fromDate"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("deleteTaxAuthorityAssoc", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}


@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/createTaxAuthorityCategory")
public Response createTaxAuthorityCategory_(@Multipart(value = "createTaxAuthorityCategory", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("taxAuthGeoId") != null)
		attributeMap.put("taxAuthGeoId", params.get("taxAuthGeoId"));
	if(params.get("taxAuthPartyId") != null)
		attributeMap.put("taxAuthPartyId", params.get("taxAuthPartyId"));
	if(params.get("productCategoryId") != null)
		attributeMap.put("productCategoryId", params.get("productCategoryId"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("createTaxAuthorityCategory", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/updateTaxAuthorityCategory")
public Response updateTaxAuthorityCategory_(@Multipart(value = "updateTaxAuthorityCategory", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("taxAuthGeoId") != null)
		attributeMap.put("taxAuthGeoId", params.get("taxAuthGeoId"));
	if(params.get("taxAuthPartyId") != null)
		attributeMap.put("taxAuthPartyId", params.get("taxAuthPartyId"));
	if(params.get("productCategoryId") != null)
		attributeMap.put("productCategoryId", params.get("productCategoryId"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("updateTaxAuthorityCategory", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/deleteTaxAuthorityCategory")
public Response deleteTaxAuthorityCategory_(@Multipart(value = "deleteTaxAuthorityCategory", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("taxAuthGeoId") != null)
		attributeMap.put("taxAuthGeoId", params.get("taxAuthGeoId"));
	if(params.get("taxAuthPartyId") != null)
		attributeMap.put("taxAuthPartyId", params.get("taxAuthPartyId"));
	if(params.get("productCategoryId") != null)
		attributeMap.put("productCategoryId", params.get("productCategoryId"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("deleteTaxAuthorityCategory", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/addPaymentMethodTypeGlAssignment")
public Response addPaymentMethodTypeGlAssignment_(@Multipart(value = "addPaymentMethodTypeGlAssignment", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("paymentMethodTypeId") != null)
		attributeMap.put("paymentMethodTypeId", params.get("paymentMethodTypeId"));
	if(params.get("organizationPartyId") != null)
		attributeMap.put("organizationPartyId", params.get("organizationPartyId"));
	if(params.get("glAccountId") != null)
		attributeMap.put("glAccountId", params.get("glAccountId"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("addPaymentMethodTypeGlAssignment", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/removePaymentMethodTypeGlAssignment")
public Response removePaymentMethodTypeGlAssignment_(@Multipart(value = "removePaymentMethodTypeGlAssignment", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("paymentMethodTypeId") != null)
		attributeMap.put("paymentMethodTypeId", params.get("paymentMethodTypeId"));
	if(params.get("organizationPartyId") != null)
		attributeMap.put("organizationPartyId", params.get("organizationPartyId"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("removePaymentMethodTypeGlAssignment", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/addPaymentTypeGlAssignment")
public Response addPaymentTypeGlAssignment_(@Multipart(value = "addPaymentTypeGlAssignment", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("paymentTypeId") != null)
		attributeMap.put("paymentTypeId", params.get("paymentTypeId"));
	if(params.get("organizationPartyId") != null)
		attributeMap.put("organizationPartyId", params.get("organizationPartyId"));
	if(params.get("glAccountTypeId") != null)
		attributeMap.put("glAccountTypeId", params.get("glAccountTypeId"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("addPaymentTypeGlAssignment", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}




@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/removePaymentTypeGlAssignment")
public Response removePaymentTypeGlAssignment_(@Multipart(value = "removePaymentTypeGlAssignment", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("paymentTypeId") != null)
		attributeMap.put("paymentTypeId", params.get("paymentTypeId"));
	if(params.get("organizationPartyId") != null)
		attributeMap.put("organizationPartyId", params.get("organizationPartyId"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("removePaymentTypeGlAssignment", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}
@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/createInvoiceItem")
public Response createInvoiceItem_(@Multipart(value = "createInvoiceItem", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("invoiceId") != null)
		attributeMap.put("invoiceId", params.get("invoiceId"));
	if(params.get("invoiceItemSeqId") != null)
		attributeMap.put("invoiceItemSeqId", params.get("invoiceItemSeqId"));
	if(params.get("invoiceItemTypeId") != null)
		attributeMap.put("invoiceItemTypeId", params.get("invoiceItemTypeId"));
	if(params.get("overrideGlAccountId") != null)
		attributeMap.put("overrideGlAccountId", params.get("overrideGlAccountId"));
	if(params.get("overrideOrgPartyId") != null)
		attributeMap.put("overrideOrgPartyId", params.get("overrideOrgPartyId"));
	if(params.get("inventoryItemId") != null)
		attributeMap.put("inventoryItemId", params.get("inventoryItemId"));
	if(params.get("productId") != null)
		attributeMap.put("productId", params.get("productId"));
	if(params.get("productFeatureId") != null)
		attributeMap.put("productFeatureId", params.get("productFeatureId"));
	if(params.get("parentInvoiceId") != null)
		attributeMap.put("parentInvoiceId", params.get("parentInvoiceId"));
	if(params.get("parentInvoiceItemSeqId") != null)
		attributeMap.put("parentInvoiceItemSeqId", params.get("parentInvoiceItemSeqId"));
	if(params.get("uomId") != null)
		attributeMap.put("uomId", params.get("uomId"));
	if(params.get("taxableFlag") != null)
		attributeMap.put("taxableFlag", params.get("taxableFlag"));
	if(params.get("quantity") != null)
		attributeMap.put("quantity", params.get("quantity"));
	if(params.get("amount") != null)
		attributeMap.put("amount", params.get("amount"));
	if(params.get("description") != null)
		attributeMap.put("description", params.get("description"));
	if(params.get("taxAuthPartyId") != null)
		attributeMap.put("taxAuthPartyId", params.get("taxAuthPartyId"));
	if(params.get("taxAuthGeoId") != null)
		attributeMap.put("taxAuthGeoId", params.get("taxAuthGeoId"));
	if(params.get("taxAuthorityRateSeqId") != null)
		attributeMap.put("taxAuthorityRateSeqId", params.get("taxAuthorityRateSeqId"));
	if(params.get("salesOpportunityId") != null)
		attributeMap.put("salesOpportunityId", params.get("salesOpportunityId"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	if(params.get("invoiceItemSeqId") != null)
		attributeMap.put("invoiceItemSeqId", params.get("invoiceItemSeqId"));
	if(params.get("description") != null)
		attributeMap.put("description", params.get("description"));
	try {
		Map result = dispatcher.runSync("createInvoiceItem", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}


@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/createTaxAuthority")
public Response createTaxAuthority_(@Multipart(value = "createTaxAuthority", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("taxAuthGeoId") != null)
		attributeMap.put("taxAuthGeoId", params.get("taxAuthGeoId"));
	if(params.get("taxAuthPartyId") != null)
		attributeMap.put("taxAuthPartyId", params.get("taxAuthPartyId"));
	if(params.get("requireTaxIdForExemption") != null)
		attributeMap.put("requireTaxIdForExemption", params.get("requireTaxIdForExemption"));
	if(params.get("taxIdFormatPattern") != null)
		attributeMap.put("taxIdFormatPattern", params.get("taxIdFormatPattern"));
	if(params.get("includeTaxInPrice") != null)
		attributeMap.put("includeTaxInPrice", params.get("includeTaxInPrice"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("createTaxAuthority", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/updateTaxAuthority")
public Response updateTaxAuthority_(@Multipart(value = "updateTaxAuthority", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("taxAuthGeoId") != null)
		attributeMap.put("taxAuthGeoId", params.get("taxAuthGeoId"));
	if(params.get("taxAuthPartyId") != null)
		attributeMap.put("taxAuthPartyId", params.get("taxAuthPartyId"));
	if(params.get("requireTaxIdForExemption") != null)
		attributeMap.put("requireTaxIdForExemption", params.get("requireTaxIdForExemption"));
	if(params.get("taxIdFormatPattern") != null)
		attributeMap.put("taxIdFormatPattern", params.get("taxIdFormatPattern"));
	if(params.get("includeTaxInPrice") != null)
		attributeMap.put("includeTaxInPrice", params.get("includeTaxInPrice"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("updateTaxAuthority", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}
@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/createPartyAcctgPreference")
public Response createPartyAcctgPreference_(@Multipart(value = "createPartyAcctgPreference", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("partyId") != null)
		attributeMap.put("partyId", params.get("partyId"));
	if(params.get("fiscalYearStartMonth") != null)
		attributeMap.put("fiscalYearStartMonth", params.get("fiscalYearStartMonth"));
	if(params.get("fiscalYearStartDay") != null)
		attributeMap.put("fiscalYearStartDay", params.get("fiscalYearStartDay"));
	if(params.get("taxFormId") != null)
		attributeMap.put("taxFormId", params.get("taxFormId"));
	if(params.get("cogsMethodId") != null)
		attributeMap.put("cogsMethodId", params.get("cogsMethodId"));
	if(params.get("baseCurrencyUomId") != null)
		attributeMap.put("baseCurrencyUomId", params.get("baseCurrencyUomId"));
	if(params.get("invoiceSeqCustMethId") != null)
		attributeMap.put("invoiceSeqCustMethId", params.get("invoiceSeqCustMethId"));
	if(params.get("invoiceIdPrefix") != null)
		attributeMap.put("invoiceIdPrefix", params.get("invoiceIdPrefix"));
	if(params.get("lastInvoiceNumber") != null)
		attributeMap.put("lastInvoiceNumber", params.get("lastInvoiceNumber"));
	if(params.get("lastInvoiceRestartDate") != null)
		attributeMap.put("lastInvoiceRestartDate", params.get("lastInvoiceRestartDate"));
	if(params.get("useInvoiceIdForReturns") != null)
		attributeMap.put("useInvoiceIdForReturns", params.get("useInvoiceIdForReturns"));
	if(params.get("quoteSeqCustMethId") != null)
		attributeMap.put("quoteSeqCustMethId", params.get("quoteSeqCustMethId"));
	if(params.get("quoteIdPrefix") != null)
		attributeMap.put("quoteIdPrefix", params.get("quoteIdPrefix"));
	if(params.get("lastQuoteNumber") != null)
		attributeMap.put("lastQuoteNumber", params.get("lastQuoteNumber"));
	if(params.get("orderSeqCustMethId") != null)
		attributeMap.put("orderSeqCustMethId", params.get("orderSeqCustMethId"));
	if(params.get("orderIdPrefix") != null)
		attributeMap.put("orderIdPrefix", params.get("orderIdPrefix"));
	if(params.get("lastOrderNumber") != null)
		attributeMap.put("lastOrderNumber", params.get("lastOrderNumber"));
	if(params.get("refundPaymentMethodId") != null)
		attributeMap.put("refundPaymentMethodId", params.get("refundPaymentMethodId"));
	if(params.get("errorGlJournalId") != null)
		attributeMap.put("errorGlJournalId", params.get("errorGlJournalId"));
	if(params.get("oldInvoiceSequenceEnumId") != null)
		attributeMap.put("oldInvoiceSequenceEnumId", params.get("oldInvoiceSequenceEnumId"));
	if(params.get("oldOrderSequenceEnumId") != null)
		attributeMap.put("oldOrderSequenceEnumId", params.get("oldOrderSequenceEnumId"));
	if(params.get("oldQuoteSequenceEnumId") != null)
		attributeMap.put("oldQuoteSequenceEnumId", params.get("oldQuoteSequenceEnumId"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("createPartyAcctgPreference", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/updatePartyAcctgPreference")
public Response updatePartyAcctgPreference_(@Multipart(value = "updatePartyAcctgPreference", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("partyId") != null)
		attributeMap.put("partyId", params.get("partyId"));
	if(params.get("refundPaymentMethodId") != null)
		attributeMap.put("refundPaymentMethodId", params.get("refundPaymentMethodId"));
	if(params.get("errorGlJournalId") != null)
		attributeMap.put("errorGlJournalId", params.get("errorGlJournalId"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("updatePartyAcctgPreference", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}


@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/manualForcedCcTransaction")
public Response manualForcedCcTransaction_(@Multipart(value = "manualForcedCcTransaction", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("paymentMethodTypeId") != null)
		attributeMap.put("paymentMethodTypeId", params.get("paymentMethodTypeId"));
	if(params.get("productStoreId") != null)
		attributeMap.put("productStoreId", params.get("productStoreId"));
	if(params.get("transactionType") != null)
		attributeMap.put("transactionType", params.get("transactionType"));
	if(params.get("companyNameOnCard") != null)
		attributeMap.put("companyNameOnCard", params.get("companyNameOnCard"));
	if(params.get("titleOnCard") != null)
		attributeMap.put("titleOnCard", params.get("titleOnCard"));
	if(params.get("firstNameOnCard") != null)
		attributeMap.put("firstNameOnCard", params.get("firstNameOnCard"));
	if(params.get("middleNameOnCard") != null)
		attributeMap.put("middleNameOnCard", params.get("middleNameOnCard"));
	if(params.get("lastNameOnCard") != null)
		attributeMap.put("lastNameOnCard", params.get("lastNameOnCard"));
	if(params.get("suffixOnCard") != null)
		attributeMap.put("suffixOnCard", params.get("suffixOnCard"));
	if(params.get("cardType") != null)
		attributeMap.put("cardType", params.get("cardType"));
	if(params.get("cardNumber") != null)
		attributeMap.put("cardNumber", params.get("cardNumber"));
	if(params.get("cardSecurityCode") != null)
		attributeMap.put("cardSecurityCode", params.get("cardSecurityCode"));
	if(params.get("expMonth") != null)
		attributeMap.put("expMonth", params.get("expMonth"));
	if(params.get("expYear") != null)
		attributeMap.put("expYear", params.get("expYear"));
	if(params.get("infoString") != null)
		attributeMap.put("infoString", params.get("infoString"));
	if(params.get("address1") != null)
		attributeMap.put("address1", params.get("address1"));
	if(params.get("address2") != null)
		attributeMap.put("address2", params.get("address2"));
	if(params.get("city") != null)
		attributeMap.put("city", params.get("city"));
	if(params.get("stateProvinceGeoId") != null)
		attributeMap.put("stateProvinceGeoId", params.get("stateProvinceGeoId"));
	if(params.get("postalCode") != null)
		attributeMap.put("postalCode", params.get("postalCode"));
	if(params.get("countryGeoId") != null)
		attributeMap.put("countryGeoId", params.get("countryGeoId"));
	if(params.get("amount") != null)
		attributeMap.put("amount", params.get("amount"));
	if(params.get("referenceCode") != null)
		attributeMap.put("referenceCode", params.get("referenceCode"));
	if(params.get("orderPaymentPreferenceId") != null)
		attributeMap.put("orderPaymentPreferenceId", params.get("orderPaymentPreferenceId"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("manualForcedCcTransaction", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}
@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/authOrderPaymentPreference")
public Response authOrderPaymentPreference_(@Multipart(value = "authOrderPaymentPreference", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("orderPaymentPreferenceId") != null)
		attributeMap.put("orderPaymentPreferenceId", params.get("orderPaymentPreferenceId"));
	if(params.get("overrideAmount") != null)
		attributeMap.put("overrideAmount", params.get("overrideAmount"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("authOrderPaymentPreference", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/releaseOrderPaymentPreference")
public Response releaseOrderPaymentPreference_(@Multipart(value = "releaseOrderPaymentPreference", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("orderPaymentPreferenceId") != null)
		attributeMap.put("orderPaymentPreferenceId", params.get("orderPaymentPreferenceId"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("releaseOrderPaymentPreference", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/updatePaymentGatewayConfig")
public Response updatePaymentGatewayConfig_(@Multipart(value = "updatePaymentGatewayConfig", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("paymentGatewayConfigId") != null)
		attributeMap.put("paymentGatewayConfigId", params.get("paymentGatewayConfigId"));
	if(params.get("paymentGatewayConfigTypeId") != null)
		attributeMap.put("paymentGatewayConfigTypeId", params.get("paymentGatewayConfigTypeId"));
	if(params.get("description") != null)
		attributeMap.put("description", params.get("description"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("updatePaymentGatewayConfig", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/updatePaymentGatewayConfigiDEAL")
public Response updatePaymentGatewayConfigiDEAL_(@Multipart(value = "updatePaymentGatewayConfigiDEAL", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("paymentGatewayConfigId") != null)
		attributeMap.put("paymentGatewayConfigId", params.get("paymentGatewayConfigId"));
	if(params.get("merchantId") != null)
		attributeMap.put("merchantId", params.get("merchantId"));
	if(params.get("merchantSubId") != null)
		attributeMap.put("merchantSubId", params.get("merchantSubId"));
	if(params.get("merchantReturnURL") != null)
		attributeMap.put("merchantReturnURL", params.get("merchantReturnURL"));
	if(params.get("acquirerURL") != null)
		attributeMap.put("acquirerURL", params.get("acquirerURL"));
	if(params.get("acquirerTimeout") != null)
		attributeMap.put("acquirerTimeout", params.get("acquirerTimeout"));
	if(params.get("privateCert") != null)
		attributeMap.put("privateCert", params.get("privateCert"));
	if(params.get("acquirerKeyStoreFilename") != null)
		attributeMap.put("acquirerKeyStoreFilename", params.get("acquirerKeyStoreFilename"));
	if(params.get("acquirerKeyStorePassword") != null)
		attributeMap.put("acquirerKeyStorePassword", params.get("acquirerKeyStorePassword"));
	if(params.get("merchantKeyStoreFilename") != null)
		attributeMap.put("merchantKeyStoreFilename", params.get("merchantKeyStoreFilename"));
	if(params.get("merchantKeyStorePassword") != null)
		attributeMap.put("merchantKeyStorePassword", params.get("merchantKeyStorePassword"));
	if(params.get("expirationPeriod") != null)
		attributeMap.put("expirationPeriod", params.get("expirationPeriod"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("updatePaymentGatewayConfigiDEAL", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}


@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/updatePaymentGatewayConfigSagePay")
public Response updatePaymentGatewayConfigSagePay_(@Multipart(value = "updatePaymentGatewayConfigSagePay", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("paymentGatewayConfigId") != null)
		attributeMap.put("paymentGatewayConfigId", params.get("paymentGatewayConfigId"));
	if(params.get("vendor") != null)
		attributeMap.put("vendor", params.get("vendor"));
	if(params.get("productionHost") != null)
		attributeMap.put("productionHost", params.get("productionHost"));
	if(params.get("testingHost") != null)
		attributeMap.put("testingHost", params.get("testingHost"));
	if(params.get("sagePayMode") != null)
		attributeMap.put("sagePayMode", params.get("sagePayMode"));
	if(params.get("protocolVersion") != null)
		attributeMap.put("protocolVersion", params.get("protocolVersion"));
	if(params.get("authenticationTransType") != null)
		attributeMap.put("authenticationTransType", params.get("authenticationTransType"));
	if(params.get("authenticationUrl") != null)
		attributeMap.put("authenticationUrl", params.get("authenticationUrl"));
	if(params.get("authoriseTransType") != null)
		attributeMap.put("authoriseTransType", params.get("authoriseTransType"));
	if(params.get("authoriseUrl") != null)
		attributeMap.put("authoriseUrl", params.get("authoriseUrl"));
	if(params.get("releaseTransType") != null)
		attributeMap.put("releaseTransType", params.get("releaseTransType"));
	if(params.get("releaseUrl") != null)
		attributeMap.put("releaseUrl", params.get("releaseUrl"));
	if(params.get("voidUrl") != null)
		attributeMap.put("voidUrl", params.get("voidUrl"));
	if(params.get("refundUrl") != null)
		attributeMap.put("refundUrl", params.get("refundUrl"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("updatePaymentGatewayConfigSagePay", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}


@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/updatePaymentGatewayConfigAuthorizeNet")
public Response updatePaymentGatewayConfigAuthorizeNet_(@Multipart(value = "updatePaymentGatewayConfigAuthorizeNet", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("paymentGatewayConfigId") != null)
		attributeMap.put("paymentGatewayConfigId", params.get("paymentGatewayConfigId"));
	if(params.get("transactionUrl") != null)
		attributeMap.put("transactionUrl", params.get("transactionUrl"));
	if(params.get("certificateAlias") != null)
		attributeMap.put("certificateAlias", params.get("certificateAlias"));
	if(params.get("apiVersion") != null)
		attributeMap.put("apiVersion", params.get("apiVersion"));
	if(params.get("delimitedData") != null)
		attributeMap.put("delimitedData", params.get("delimitedData"));
	if(params.get("delimiterChar") != null)
		attributeMap.put("delimiterChar", params.get("delimiterChar"));
	if(params.get("cpVersion") != null)
		attributeMap.put("cpVersion", params.get("cpVersion"));
	if(params.get("cpMarketType") != null)
		attributeMap.put("cpMarketType", params.get("cpMarketType"));
	if(params.get("cpDeviceType") != null)
		attributeMap.put("cpDeviceType", params.get("cpDeviceType"));
	if(params.get("method") != null)
		attributeMap.put("method", params.get("method"));
	if(params.get("emailCustomer") != null)
		attributeMap.put("emailCustomer", params.get("emailCustomer"));
	if(params.get("emailMerchant") != null)
		attributeMap.put("emailMerchant", params.get("emailMerchant"));
	if(params.get("testMode") != null)
		attributeMap.put("testMode", params.get("testMode"));
	if(params.get("relayResponse") != null)
		attributeMap.put("relayResponse", params.get("relayResponse"));
	if(params.get("tranKey") != null)
		attributeMap.put("tranKey", params.get("tranKey"));
	if(params.get("userId") != null)
		attributeMap.put("userId", params.get("userId"));
	if(params.get("pwd") != null)
		attributeMap.put("pwd", params.get("pwd"));
	if(params.get("transDescription") != null)
		attributeMap.put("transDescription", params.get("transDescription"));
	if(params.get("duplicateWindow") != null)
		attributeMap.put("duplicateWindow", params.get("duplicateWindow"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("updatePaymentGatewayConfigAuthorizeNet", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/updatePaymentGatewayConfigCyberSource")
public Response updatePaymentGatewayConfigCyberSource_(@Multipart(value = "updatePaymentGatewayConfigCyberSource", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("paymentGatewayConfigId") != null)
		attributeMap.put("paymentGatewayConfigId", params.get("paymentGatewayConfigId"));
	if(params.get("merchantId") != null)
		attributeMap.put("merchantId", params.get("merchantId"));
	if(params.get("apiVersion") != null)
		attributeMap.put("apiVersion", params.get("apiVersion"));
	if(params.get("production") != null)
		attributeMap.put("production", params.get("production"));
	if(params.get("keysDir") != null)
		attributeMap.put("keysDir", params.get("keysDir"));
	if(params.get("keysFile") != null)
		attributeMap.put("keysFile", params.get("keysFile"));
	if(params.get("logEnabled") != null)
		attributeMap.put("logEnabled", params.get("logEnabled"));
	if(params.get("logDir") != null)
		attributeMap.put("logDir", params.get("logDir"));
	if(params.get("logFile") != null)
		attributeMap.put("logFile", params.get("logFile"));
	if(params.get("logSize") != null)
		attributeMap.put("logSize", params.get("logSize"));
	if(params.get("merchantDescr") != null)
		attributeMap.put("merchantDescr", params.get("merchantDescr"));
	if(params.get("merchantContact") != null)
		attributeMap.put("merchantContact", params.get("merchantContact"));
	if(params.get("autoBill") != null)
		attributeMap.put("autoBill", params.get("autoBill"));
	if(params.get("enableDav") != null)
		attributeMap.put("enableDav", params.get("enableDav"));
	if(params.get("fraudScore") != null)
		attributeMap.put("fraudScore", params.get("fraudScore"));
	if(params.get("ignoreAvs") != null)
		attributeMap.put("ignoreAvs", params.get("ignoreAvs"));
	if(params.get("disableBillAvs") != null)
		attributeMap.put("disableBillAvs", params.get("disableBillAvs"));
	if(params.get("avsDeclineCodes") != null)
		attributeMap.put("avsDeclineCodes", params.get("avsDeclineCodes"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("updatePaymentGatewayConfigCyberSource", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}


@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/updatePaymentGatewayConfigPayflowPro")
public Response updatePaymentGatewayConfigPayflowPro_(@Multipart(value = "updatePaymentGatewayConfigPayflowPro", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("paymentGatewayConfigId") != null)
		attributeMap.put("paymentGatewayConfigId", params.get("paymentGatewayConfigId"));
	if(params.get("certsPath") != null)
		attributeMap.put("certsPath", params.get("certsPath"));
	if(params.get("hostAddress") != null)
		attributeMap.put("hostAddress", params.get("hostAddress"));
	if(params.get("hostPort") != null)
		attributeMap.put("hostPort", params.get("hostPort"));
	if(params.get("timeout") != null)
		attributeMap.put("timeout", params.get("timeout"));
	if(params.get("proxyAddress") != null)
		attributeMap.put("proxyAddress", params.get("proxyAddress"));
	if(params.get("proxyPort") != null)
		attributeMap.put("proxyPort", params.get("proxyPort"));
	if(params.get("proxyLogon") != null)
		attributeMap.put("proxyLogon", params.get("proxyLogon"));
	if(params.get("proxyPassword") != null)
		attributeMap.put("proxyPassword", params.get("proxyPassword"));
	if(params.get("vendor") != null)
		attributeMap.put("vendor", params.get("vendor"));
	if(params.get("userId") != null)
		attributeMap.put("userId", params.get("userId"));
	if(params.get("pwd") != null)
		attributeMap.put("pwd", params.get("pwd"));
	if(params.get("partner") != null)
		attributeMap.put("partner", params.get("partner"));
	if(params.get("checkAvs") != null)
		attributeMap.put("checkAvs", params.get("checkAvs"));
	if(params.get("checkCvv2") != null)
		attributeMap.put("checkCvv2", params.get("checkCvv2"));
	if(params.get("preAuth") != null)
		attributeMap.put("preAuth", params.get("preAuth"));
	if(params.get("enableTransmit") != null)
		attributeMap.put("enableTransmit", params.get("enableTransmit"));
	if(params.get("logFileName") != null)
		attributeMap.put("logFileName", params.get("logFileName"));
	if(params.get("loggingLevel") != null)
		attributeMap.put("loggingLevel", params.get("loggingLevel"));
	if(params.get("maxLogFileSize") != null)
		attributeMap.put("maxLogFileSize", params.get("maxLogFileSize"));
	if(params.get("stackTraceOn") != null)
		attributeMap.put("stackTraceOn", params.get("stackTraceOn"));
	if(params.get("redirectUrl") != null)
		attributeMap.put("redirectUrl", params.get("redirectUrl"));
	if(params.get("returnUrl") != null)
		attributeMap.put("returnUrl", params.get("returnUrl"));
	if(params.get("cancelReturnUrl") != null)
		attributeMap.put("cancelReturnUrl", params.get("cancelReturnUrl"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("updatePaymentGatewayConfigPayflowPro", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}


@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/updatePaymentGatewayConfigPayPal")
public Response updatePaymentGatewayConfigPayPal_(@Multipart(value = "updatePaymentGatewayConfigPayPal", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("paymentGatewayConfigId") != null)
		attributeMap.put("paymentGatewayConfigId", params.get("paymentGatewayConfigId"));
	if(params.get("businessEmail") != null)
		attributeMap.put("businessEmail", params.get("businessEmail"));
	if(params.get("apiUserName") != null)
		attributeMap.put("apiUserName", params.get("apiUserName"));
	if(params.get("apiPassword") != null)
		attributeMap.put("apiPassword", params.get("apiPassword"));
	if(params.get("apiSignature") != null)
		attributeMap.put("apiSignature", params.get("apiSignature"));
	if(params.get("apiEnvironment") != null)
		attributeMap.put("apiEnvironment", params.get("apiEnvironment"));
	if(params.get("notifyUrl") != null)
		attributeMap.put("notifyUrl", params.get("notifyUrl"));
	if(params.get("returnUrl") != null)
		attributeMap.put("returnUrl", params.get("returnUrl"));
	if(params.get("cancelReturnUrl") != null)
		attributeMap.put("cancelReturnUrl", params.get("cancelReturnUrl"));
	if(params.get("imageUrl") != null)
		attributeMap.put("imageUrl", params.get("imageUrl"));
	if(params.get("confirmTemplate") != null)
		attributeMap.put("confirmTemplate", params.get("confirmTemplate"));
	if(params.get("redirectUrl") != null)
		attributeMap.put("redirectUrl", params.get("redirectUrl"));
	if(params.get("confirmUrl") != null)
		attributeMap.put("confirmUrl", params.get("confirmUrl"));
	if(params.get("shippingCallbackUrl") != null)
		attributeMap.put("shippingCallbackUrl", params.get("shippingCallbackUrl"));
	if(params.get("requireConfirmedShipping") != null)
		attributeMap.put("requireConfirmedShipping", params.get("requireConfirmedShipping"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("updatePaymentGatewayConfigPayPal", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/updatePaymentGatewayConfigSecurePay")
public Response updatePaymentGatewayConfigSecurePay_(@Multipart(value = "updatePaymentGatewayConfigSecurePay", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("paymentGatewayConfigId") != null)
		attributeMap.put("paymentGatewayConfigId", params.get("paymentGatewayConfigId"));
	if(params.get("merchantId") != null)
		attributeMap.put("merchantId", params.get("merchantId"));
	if(params.get("pwd") != null)
		attributeMap.put("pwd", params.get("pwd"));
	if(params.get("serverURL") != null)
		attributeMap.put("serverURL", params.get("serverURL"));
	if(params.get("processTimeout") != null)
		attributeMap.put("processTimeout", params.get("processTimeout"));
	if(params.get("enableAmountRound") != null)
		attributeMap.put("enableAmountRound", params.get("enableAmountRound"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("updatePaymentGatewayConfigSecurePay", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}


@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/updatePaymentGatewayConfigEway")
public Response updatePaymentGatewayConfigEway_(@Multipart(value = "updatePaymentGatewayConfigEway", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("paymentGatewayConfigId") != null)
		attributeMap.put("paymentGatewayConfigId", params.get("paymentGatewayConfigId"));
	if(params.get("customerId") != null)
		attributeMap.put("customerId", params.get("customerId"));
	if(params.get("refundPwd") != null)
		attributeMap.put("refundPwd", params.get("refundPwd"));
	if(params.get("testMode") != null)
		attributeMap.put("testMode", params.get("testMode"));
	if(params.get("enableCvn") != null)
		attributeMap.put("enableCvn", params.get("enableCvn"));
	if(params.get("enableBeagle") != null)
		attributeMap.put("enableBeagle", params.get("enableBeagle"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("updatePaymentGatewayConfigEway", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}


@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/updatePaymentGatewayConfigClearCommerce")
public Response updatePaymentGatewayConfigClearCommerce_(@Multipart(value = "updatePaymentGatewayConfigClearCommerce", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("paymentGatewayConfigId") != null)
		attributeMap.put("paymentGatewayConfigId", params.get("paymentGatewayConfigId"));
	if(params.get("sourceId") != null)
		attributeMap.put("sourceId", params.get("sourceId"));
	if(params.get("groupId") != null)
		attributeMap.put("groupId", params.get("groupId"));
	if(params.get("clientId") != null)
		attributeMap.put("clientId", params.get("clientId"));
	if(params.get("username") != null)
		attributeMap.put("username", params.get("username"));
	if(params.get("pwd") != null)
		attributeMap.put("pwd", params.get("pwd"));
	if(params.get("userAlias") != null)
		attributeMap.put("userAlias", params.get("userAlias"));
	if(params.get("effectiveAlias") != null)
		attributeMap.put("effectiveAlias", params.get("effectiveAlias"));
	if(params.get("processMode") != null)
		attributeMap.put("processMode", params.get("processMode"));
	if(params.get("serverURL") != null)
		attributeMap.put("serverURL", params.get("serverURL"));
	if(params.get("enableCVM") != null)
		attributeMap.put("enableCVM", params.get("enableCVM"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("updatePaymentGatewayConfigClearCommerce", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/updatePaymentGatewayConfigWorldPay")
public Response updatePaymentGatewayConfigWorldPay_(@Multipart(value = "updatePaymentGatewayConfigWorldPay", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("paymentGatewayConfigId") != null)
		attributeMap.put("paymentGatewayConfigId", params.get("paymentGatewayConfigId"));
	if(params.get("redirectUrl") != null)
		attributeMap.put("redirectUrl", params.get("redirectUrl"));
	if(params.get("instId") != null)
		attributeMap.put("instId", params.get("instId"));
	if(params.get("authMode") != null)
		attributeMap.put("authMode", params.get("authMode"));
	if(params.get("fixContact") != null)
		attributeMap.put("fixContact", params.get("fixContact"));
	if(params.get("hideContact") != null)
		attributeMap.put("hideContact", params.get("hideContact"));
	if(params.get("hideCurrency") != null)
		attributeMap.put("hideCurrency", params.get("hideCurrency"));
	if(params.get("langId") != null)
		attributeMap.put("langId", params.get("langId"));
	if(params.get("noLanguageMenu") != null)
		attributeMap.put("noLanguageMenu", params.get("noLanguageMenu"));
	if(params.get("withDelivery") != null)
		attributeMap.put("withDelivery", params.get("withDelivery"));
	if(params.get("testMode") != null)
		attributeMap.put("testMode", params.get("testMode"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("updatePaymentGatewayConfigWorldPay", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}


@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/updatePaymentGatewayConfigType")
public Response updatePaymentGatewayConfigType_(@Multipart(value = "updatePaymentGatewayConfigType", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("paymentGatewayConfigTypeId") != null)
		attributeMap.put("paymentGatewayConfigTypeId", params.get("paymentGatewayConfigTypeId"));
	if(params.get("parentTypeId") != null)
		attributeMap.put("parentTypeId", params.get("parentTypeId"));
	if(params.get("hasTable") != null)
		attributeMap.put("hasTable", params.get("hasTable"));
	if(params.get("description") != null)
		attributeMap.put("description", params.get("description"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("updatePaymentGatewayConfigType", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}


@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/capturePaymentsByInvoice")
public Response capturePaymentsByInvoice_(@Multipart(value = "capturePaymentsByInvoice", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("invoiceId") != null)
		attributeMap.put("invoiceId", params.get("invoiceId"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("capturePaymentsByInvoice", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/captureOrderPayments")
public Response captureOrderPayments_(@Multipart(value = "captureOrderPayments", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("orderId") != null)
		attributeMap.put("orderId", params.get("orderId"));
	if(params.get("invoiceId") != null)
		attributeMap.put("invoiceId", params.get("invoiceId"));
	if(params.get("billingAccountId") != null)
		attributeMap.put("billingAccountId", params.get("billingAccountId"));
	if(params.get("captureAmount") != null)
		attributeMap.put("captureAmount", params.get("captureAmount"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("captureOrderPayments", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/refundOrderPaymentPreference")
public Response refundOrderPaymentPreference_(@Multipart(value = "refundOrderPaymentPreference", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("orderPaymentPreferenceId") != null)
		attributeMap.put("orderPaymentPreferenceId", params.get("orderPaymentPreferenceId"));
	if(params.get("amount") != null)
		attributeMap.put("amount", params.get("amount"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("refundOrderPaymentPreference", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/removeInvoiceItem")
public Response removeInvoiceItem_(@Multipart(value = "removeInvoiceItem", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("invoiceId") != null)
		attributeMap.put("invoiceId", params.get("invoiceId"));
	if(params.get("invoiceItemSeqId") != null)
		attributeMap.put("invoiceItemSeqId", params.get("invoiceItemSeqId"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("removeInvoiceItem", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}



@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/updateRateAmount")
public Response updateRateAmount_(@Multipart(value = "updateRateAmount", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("rateTypeId") != null)
		attributeMap.put("rateTypeId", params.get("rateTypeId"));
	if(params.get("rateCurrencyUomId") != null)
		attributeMap.put("rateCurrencyUomId", params.get("rateCurrencyUomId"));
	if(params.get("periodTypeId") != null)
		attributeMap.put("periodTypeId", params.get("periodTypeId"));
	if(params.get("workEffortId") != null)
		attributeMap.put("workEffortId", params.get("workEffortId"));
	if(params.get("partyId") != null)
		attributeMap.put("partyId", params.get("partyId"));
	if(params.get("emplPositionTypeId") != null)
		attributeMap.put("emplPositionTypeId", params.get("emplPositionTypeId"));
	if(params.get("fromDate") != null)
		attributeMap.put("fromDate", params.get("fromDate"));
	if(params.get("thruDate") != null)
		attributeMap.put("thruDate", params.get("thruDate"));
	if(params.get("rateAmount") != null)
		attributeMap.put("rateAmount", params.get("rateAmount"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	if(params.get("rateTypeId") != null)
		attributeMap.put("rateTypeId", params.get("rateTypeId"));
	if(params.get("rateAmount") != null)
		attributeMap.put("rateAmount", params.get("rateAmount"));
	try {
		Map result = dispatcher.runSync("updateRateAmount", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}
@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/updateFXConversion")
public Response updateFXConversion_(@Multipart(value = "updateFXConversion", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("uomId") != null)
		attributeMap.put("uomId", params.get("uomId"));
	if(params.get("uomIdTo") != null)
		attributeMap.put("uomIdTo", params.get("uomIdTo"));
	if(params.get("conversionFactor") != null)
		attributeMap.put("conversionFactor", params.get("conversionFactor"));
	if(params.get("purposeEnumId") != null)
		attributeMap.put("purposeEnumId", params.get("purposeEnumId"));
	if(params.get("asOfTimestamp") != null)
		attributeMap.put("asOfTimestamp", params.get("asOfTimestamp"));
	if(params.get("fromDate") != null)
		attributeMap.put("fromDate", params.get("fromDate"));
	if(params.get("thruDate") != null)
		attributeMap.put("thruDate", params.get("thruDate"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("updateFXConversion", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}


@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/convertUom")
public Response convertUom_(@Multipart(value = "convertUom", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("uomId") != null)
		attributeMap.put("uomId", params.get("uomId"));
	if(params.get("uomIdTo") != null)
		attributeMap.put("uomIdTo", params.get("uomIdTo"));
	if(params.get("asOfDate") != null)
		attributeMap.put("asOfDate", params.get("asOfDate"));
	if(params.get("originalValue") != null)
		attributeMap.put("originalValue", params.get("originalValue"));
	if(params.get("conversionParameters") != null)
		attributeMap.put("conversionParameters", params.get("conversionParameters"));
	if(params.get("purposeEnumId") != null)
		attributeMap.put("purposeEnumId", params.get("purposeEnumId"));
	if(params.get("defaultDecimalScale") != null)
		attributeMap.put("defaultDecimalScale", params.get("defaultDecimalScale"));
	if(params.get("defaultRoundingMode") != null)
		attributeMap.put("defaultRoundingMode", params.get("defaultRoundingMode"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("convertUom", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/deleteRateAmount")
public Response deleteRateAmount_(@Multipart(value = "deleteRateAmount", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("rateTypeId") != null)
		attributeMap.put("rateTypeId", params.get("rateTypeId"));
	if(params.get("rateCurrencyUomId") != null)
		attributeMap.put("rateCurrencyUomId", params.get("rateCurrencyUomId"));
	if(params.get("periodTypeId") != null)
		attributeMap.put("periodTypeId", params.get("periodTypeId"));
	if(params.get("workEffortId") != null)
		attributeMap.put("workEffortId", params.get("workEffortId"));
	if(params.get("partyId") != null)
		attributeMap.put("partyId", params.get("partyId"));
	if(params.get("emplPositionTypeId") != null)
		attributeMap.put("emplPositionTypeId", params.get("emplPositionTypeId"));
	if(params.get("fromDate") != null)
		attributeMap.put("fromDate", params.get("fromDate"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	if(params.get("rateTypeId") != null)
		attributeMap.put("rateTypeId", params.get("rateTypeId"));
	if(params.get("fromDate") != null)
		attributeMap.put("fromDate", params.get("fromDate"));
	try {
		Map result = dispatcher.runSync("deleteRateAmount", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}


@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/updateInvoiceItemType")
public Response updateInvoiceItemType_(@Multipart(value = "updateInvoiceItemType", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("invoiceItemTypeId") != null)
		attributeMap.put("invoiceItemTypeId", params.get("invoiceItemTypeId"));
	if(params.get("parentTypeId") != null)
		attributeMap.put("parentTypeId", params.get("parentTypeId"));
	if(params.get("hasTable") != null)
		attributeMap.put("hasTable", params.get("hasTable"));
	if(params.get("description") != null)
		attributeMap.put("description", params.get("description"));
	if(params.get("defaultGlAccountId") != null)
		attributeMap.put("defaultGlAccountId", params.get("defaultGlAccountId"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("updateInvoiceItemType", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}


@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/sendInvoicePerEmail")
public Response sendInvoicePerEmail_(@Multipart(value = "sendInvoicePerEmail", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("invoiceId") != null)
		attributeMap.put("invoiceId", params.get("invoiceId"));
	if(params.get("sendFrom") != null)
		attributeMap.put("sendFrom", params.get("sendFrom"));
	if(params.get("sendTo") != null)
		attributeMap.put("sendTo", params.get("sendTo"));
	if(params.get("sendCc") != null)
		attributeMap.put("sendCc", params.get("sendCc"));
	if(params.get("subject") != null)
		attributeMap.put("subject", params.get("subject"));
	if(params.get("bodyText") != null)
		attributeMap.put("bodyText", params.get("bodyText"));
	if(params.get("other") != null)
		attributeMap.put("other", params.get("other"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("sendInvoicePerEmail", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/updateInvoice")
public Response updateInvoice_(@Multipart(value = "updateInvoice", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("invoiceId") != null)
		attributeMap.put("invoiceId", params.get("invoiceId"));
	if(params.get("invoiceTypeId") != null)
		attributeMap.put("invoiceTypeId", params.get("invoiceTypeId"));
	if(params.get("partyIdFrom") != null)
		attributeMap.put("partyIdFrom", params.get("partyIdFrom"));
	if(params.get("partyId") != null)
		attributeMap.put("partyId", params.get("partyId"));
	if(params.get("roleTypeId") != null)
		attributeMap.put("roleTypeId", params.get("roleTypeId"));
	if(params.get("statusId") != null)
		attributeMap.put("statusId", params.get("statusId"));
	if(params.get("billingAccountId") != null)
		attributeMap.put("billingAccountId", params.get("billingAccountId"));
	if(params.get("contactMechId") != null)
		attributeMap.put("contactMechId", params.get("contactMechId"));
	if(params.get("invoiceDate") != null)
		attributeMap.put("invoiceDate", params.get("invoiceDate"));
	if(params.get("dueDate") != null)
		attributeMap.put("dueDate", params.get("dueDate"));
	if(params.get("paidDate") != null)
		attributeMap.put("paidDate", params.get("paidDate"));
	if(params.get("invoiceMessage") != null)
		attributeMap.put("invoiceMessage", params.get("invoiceMessage"));
	if(params.get("referenceNumber") != null)
		attributeMap.put("referenceNumber", params.get("referenceNumber"));
	if(params.get("description") != null)
		attributeMap.put("description", params.get("description"));
	if(params.get("currencyUomId") != null)
		attributeMap.put("currencyUomId", params.get("currencyUomId"));
	if(params.get("recurrenceInfoId") != null)
		attributeMap.put("recurrenceInfoId", params.get("recurrenceInfoId"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	if(params.get("description") != null)
		attributeMap.put("description", params.get("description"));
	if(params.get("invoiceMessage") != null)
		attributeMap.put("invoiceMessage", params.get("invoiceMessage"));
	try {
		Map result = dispatcher.runSync("updateInvoice", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/createInvoiceRole")
public Response createInvoiceRole_(@Multipart(value = "createInvoiceRole", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("invoiceId") != null)
		attributeMap.put("invoiceId", params.get("invoiceId"));
	if(params.get("partyId") != null)
		attributeMap.put("partyId", params.get("partyId"));
	if(params.get("roleTypeId") != null)
		attributeMap.put("roleTypeId", params.get("roleTypeId"));
	if(params.get("datetimePerformed") != null)
		attributeMap.put("datetimePerformed", params.get("datetimePerformed"));
	if(params.get("percentage") != null)
		attributeMap.put("percentage", params.get("percentage"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("createInvoiceRole", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/removeInvoiceRole")
public Response removeInvoiceRole_(@Multipart(value = "removeInvoiceRole", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("invoiceId") != null)
		attributeMap.put("invoiceId", params.get("invoiceId"));
	if(params.get("partyId") != null)
		attributeMap.put("partyId", params.get("partyId"));
	if(params.get("roleTypeId") != null)
		attributeMap.put("roleTypeId", params.get("roleTypeId"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("removeInvoiceRole", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/createInvoiceTerm")
public Response createInvoiceTerm_(@Multipart(value = "createInvoiceTerm", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("termTypeId") != null)
		attributeMap.put("termTypeId", params.get("termTypeId"));
	if(params.get("invoiceId") != null)
		attributeMap.put("invoiceId", params.get("invoiceId"));
	if(params.get("invoiceItemSeqId") != null)
		attributeMap.put("invoiceItemSeqId", params.get("invoiceItemSeqId"));
	if(params.get("termValue") != null)
		attributeMap.put("termValue", params.get("termValue"));
	if(params.get("termDays") != null)
		attributeMap.put("termDays", params.get("termDays"));
	if(params.get("textValue") != null)
		attributeMap.put("textValue", params.get("textValue"));
	if(params.get("description") != null)
		attributeMap.put("description", params.get("description"));
	if(params.get("uomId") != null)
		attributeMap.put("uomId", params.get("uomId"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("createInvoiceTerm", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/setInvoiceStatus")
public Response setInvoiceStatus_(@Multipart(value = "setInvoiceStatus", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("invoiceId") != null)
		attributeMap.put("invoiceId", params.get("invoiceId"));
	if(params.get("statusId") != null)
		attributeMap.put("statusId", params.get("statusId"));
	if(params.get("statusDate") != null)
		attributeMap.put("statusDate", params.get("statusDate"));
	if(params.get("paidDate") != null)
		attributeMap.put("paidDate", params.get("paidDate"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("setInvoiceStatus", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}

	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/createAddressMatchMap")
	public Response createAddressMatchMap_(@Multipart(value = "createAddressMatchMap", required = true, type = MediaType.APPLICATION_JSON) Map params) {
		GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
		LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
		Map<String, Object> attributeMap = new HashMap<String, Object>();
		if(params.get("mapKey") != null)
			attributeMap.put("mapKey", params.get("mapKey"));
		if(params.get("mapValue") != null)
			attributeMap.put("mapValue", params.get("mapValue"));
		if(params.get("sequenceNum") != null)
			attributeMap.put("sequenceNum", params.get("sequenceNum"));
		if(params.get("userLogin") != null)
			attributeMap.put("userLogin", params.get("userLogin"));
		else {
			try {
				attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
			} catch (GenericEntityException e) {
				e.printStackTrace();
			}
		}
		if(params.get("login.username") != null)
			attributeMap.put("login.username", params.get("login.username"));
		if(params.get("login.password") != null)
			attributeMap.put("login.password", params.get("login.password"));
		if(params.get("locale") != null)
			attributeMap.put("locale", params.get("locale"));
		if(params.get("timeZone") != null)
			attributeMap.put("timeZone", params.get("timeZone"));
		try {
			Map result = dispatcher.runSync("createAddressMatchMap", attributeMap);
			return Response.ok().entity(result).build();
		} catch(GenericServiceException e) {
			e.printStackTrace();
			return null;
		}
	}

	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/createPartyInvitation")
	public Response createPartyInvitation_(@Multipart(value = "createPartyInvitation", required = true, type = MediaType.APPLICATION_JSON) Map params) {
		GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
		LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
		Map<String, Object> attributeMap = new HashMap<String, Object>();
		if(params.get("partyIdFrom") != null)
			attributeMap.put("partyIdFrom", params.get("partyIdFrom"));
		if(params.get("partyId") != null)
			attributeMap.put("partyId", params.get("partyId"));
		if(params.get("toName") != null)
			attributeMap.put("toName", params.get("toName"));
		if(params.get("emailAddress") != null)
			attributeMap.put("emailAddress", params.get("emailAddress"));
		if(params.get("statusId") != null)
			attributeMap.put("statusId", params.get("statusId"));
		if(params.get("lastInviteDate") != null)
			attributeMap.put("lastInviteDate", params.get("lastInviteDate"));
		if(params.get("userLogin") != null)
			attributeMap.put("userLogin", params.get("userLogin"));
		else {
			try {
				attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
			} catch (GenericEntityException e) {
				e.printStackTrace();
			}
		}
		if(params.get("login.username") != null)
			attributeMap.put("login.username", params.get("login.username"));
		if(params.get("login.password") != null)
			attributeMap.put("login.password", params.get("login.password"));
		if(params.get("locale") != null)
			attributeMap.put("locale", params.get("locale"));
		if(params.get("timeZone") != null)
			attributeMap.put("timeZone", params.get("timeZone"));
		try {
			Map result = dispatcher.runSync("createPartyInvitation", attributeMap);
			return Response.ok().entity(result).build();
		} catch(GenericServiceException e) {
			e.printStackTrace();
			return null;
		}
	}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/createBillingAccount")
public Response createBillingAccount_(@Multipart(value = "createBillingAccount", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("accountLimit") != null)
		attributeMap.put("accountLimit", params.get("accountLimit"));
	if(params.get("accountCurrencyUomId") != null)
		attributeMap.put("accountCurrencyUomId", params.get("accountCurrencyUomId"));
	if(params.get("contactMechId") != null)
		attributeMap.put("contactMechId", params.get("contactMechId"));
	if(params.get("fromDate") != null)
		attributeMap.put("fromDate", params.get("fromDate"));
	if(params.get("thruDate") != null)
		attributeMap.put("thruDate", params.get("thruDate"));
	if(params.get("description") != null)
		attributeMap.put("description", params.get("description"));
	if(params.get("externalAccountId") != null)
		attributeMap.put("externalAccountId", params.get("externalAccountId"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("createBillingAccount", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/updateBillingAccount")
public Response updateBillingAccount_(@Multipart(value = "updateBillingAccount", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("billingAccountId") != null)
		attributeMap.put("billingAccountId", params.get("billingAccountId"));
	if(params.get("accountLimit") != null)
		attributeMap.put("accountLimit", params.get("accountLimit"));
	if(params.get("accountCurrencyUomId") != null)
		attributeMap.put("accountCurrencyUomId", params.get("accountCurrencyUomId"));
	if(params.get("contactMechId") != null)
		attributeMap.put("contactMechId", params.get("contactMechId"));
	if(params.get("fromDate") != null)
		attributeMap.put("fromDate", params.get("fromDate"));
	if(params.get("thruDate") != null)
		attributeMap.put("thruDate", params.get("thruDate"));
	if(params.get("description") != null)
		attributeMap.put("description", params.get("description"));
	if(params.get("externalAccountId") != null)
		attributeMap.put("externalAccountId", params.get("externalAccountId"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("updateBillingAccount", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}



@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/createProductGlAccount")
public Response createProductGlAccount_(@Multipart(value = "createProductGlAccount", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("productId") != null)
		attributeMap.put("productId", params.get("productId"));
	if(params.get("organizationPartyId") != null)
		attributeMap.put("organizationPartyId", params.get("organizationPartyId"));
	if(params.get("glAccountTypeId") != null)
		attributeMap.put("glAccountTypeId", params.get("glAccountTypeId"));
	if(params.get("glAccountId") != null)
		attributeMap.put("glAccountId", params.get("glAccountId"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("createProductGlAccount", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/updateProductGlAccount")
public Response updateProductGlAccount_(@Multipart(value = "updateProductGlAccount", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("productId") != null)
		attributeMap.put("productId", params.get("productId"));
	if(params.get("organizationPartyId") != null)
		attributeMap.put("organizationPartyId", params.get("organizationPartyId"));
	if(params.get("glAccountTypeId") != null)
		attributeMap.put("glAccountTypeId", params.get("glAccountTypeId"));
	if(params.get("glAccountId") != null)
		attributeMap.put("glAccountId", params.get("glAccountId"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("updateProductGlAccount", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/deleteProductGlAccount")
public Response deleteProductGlAccount_(@Multipart(value = "deleteProductGlAccount", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("productId") != null)
		attributeMap.put("productId", params.get("productId"));
	if(params.get("organizationPartyId") != null)
		attributeMap.put("organizationPartyId", params.get("organizationPartyId"));
	if(params.get("glAccountTypeId") != null)
		attributeMap.put("glAccountTypeId", params.get("glAccountTypeId"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("deleteProductGlAccount", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}


@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/createBillingAccountRole")
public Response createBillingAccountRole_(@Multipart(value = "createBillingAccountRole", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("billingAccountId") != null)
		attributeMap.put("billingAccountId", params.get("billingAccountId"));
	if(params.get("partyId") != null)
		attributeMap.put("partyId", params.get("partyId"));
	if(params.get("roleTypeId") != null)
		attributeMap.put("roleTypeId", params.get("roleTypeId"));
	if(params.get("fromDate") != null)
		attributeMap.put("fromDate", params.get("fromDate"));
	if(params.get("thruDate") != null)
		attributeMap.put("thruDate", params.get("thruDate"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	if(params.get("fromDate") != null)
		attributeMap.put("fromDate", params.get("fromDate"));
	try {
		Map result = dispatcher.runSync("createBillingAccountRole", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/updateBillingAccountRole")
public Response updateBillingAccountRole_(@Multipart(value = "updateBillingAccountRole", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("billingAccountId") != null)
		attributeMap.put("billingAccountId", params.get("billingAccountId"));
	if(params.get("partyId") != null)
		attributeMap.put("partyId", params.get("partyId"));
	if(params.get("roleTypeId") != null)
		attributeMap.put("roleTypeId", params.get("roleTypeId"));
	if(params.get("fromDate") != null)
		attributeMap.put("fromDate", params.get("fromDate"));
	if(params.get("thruDate") != null)
		attributeMap.put("thruDate", params.get("thruDate"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("updateBillingAccountRole", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/removeBillingAccountRole")
public Response removeBillingAccountRole_(@Multipart(value = "removeBillingAccountRole", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("billingAccountId") != null)
		attributeMap.put("billingAccountId", params.get("billingAccountId"));
	if(params.get("partyId") != null)
		attributeMap.put("partyId", params.get("partyId"));
	if(params.get("roleTypeId") != null)
		attributeMap.put("roleTypeId", params.get("roleTypeId"));
	if(params.get("fromDate") != null)
		attributeMap.put("fromDate", params.get("fromDate"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("removeBillingAccountRole", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/createBillingAccountAndRole")
public Response createBillingAccountAndRole_(@Multipart(value = "createBillingAccountAndRole", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	if(params.get("accountLimit") != null)
		attributeMap.put("accountLimit", params.get("accountLimit"));
	if(params.get("accountCurrencyUomId") != null)
		attributeMap.put("accountCurrencyUomId", params.get("accountCurrencyUomId"));
	if(params.get("contactMechId") != null)
		attributeMap.put("contactMechId", params.get("contactMechId"));
	if(params.get("fromDate") != null)
		attributeMap.put("fromDate", params.get("fromDate"));
	if(params.get("thruDate") != null)
		attributeMap.put("thruDate", params.get("thruDate"));
	if(params.get("description") != null)
		attributeMap.put("description", params.get("description"));
	if(params.get("externalAccountId") != null)
		attributeMap.put("externalAccountId", params.get("externalAccountId"));
	if(params.get("partyId") != null)
		attributeMap.put("partyId", params.get("partyId"));
	if(params.get("roleTypeId") != null)
		attributeMap.put("roleTypeId", params.get("roleTypeId"));
	if(params.get("billingAccountId") != null)
		attributeMap.put("billingAccountId", params.get("billingAccountId"));
	try {
		Map result = dispatcher.runSync("createBillingAccountAndRole", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/createBillingAccountTerm")
public Response createBillingAccountTerm_(@Multipart(value = "createBillingAccountTerm", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("billingAccountId") != null)
		attributeMap.put("billingAccountId", params.get("billingAccountId"));
	if(params.get("termTypeId") != null)
		attributeMap.put("termTypeId", params.get("termTypeId"));
	if(params.get("termValue") != null)
		attributeMap.put("termValue", params.get("termValue"));
	if(params.get("termDays") != null)
		attributeMap.put("termDays", params.get("termDays"));
	if(params.get("uomId") != null)
		attributeMap.put("uomId", params.get("uomId"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	if(params.get("termTypeId") != null)
		attributeMap.put("termTypeId", params.get("termTypeId"));
	if(params.get("billingAccountId") != null)
		attributeMap.put("billingAccountId", params.get("billingAccountId"));
	try {
		Map result = dispatcher.runSync("createBillingAccountTerm", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/updateBillingAccountTerm")
public Response updateBillingAccountTerm_(@Multipart(value = "updateBillingAccountTerm", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("billingAccountTermId") != null)
		attributeMap.put("billingAccountTermId", params.get("billingAccountTermId"));
	if(params.get("billingAccountId") != null)
		attributeMap.put("billingAccountId", params.get("billingAccountId"));
	if(params.get("termTypeId") != null)
		attributeMap.put("termTypeId", params.get("termTypeId"));
	if(params.get("termValue") != null)
		attributeMap.put("termValue", params.get("termValue"));
	if(params.get("termDays") != null)
		attributeMap.put("termDays", params.get("termDays"));
	if(params.get("uomId") != null)
		attributeMap.put("uomId", params.get("uomId"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	if(params.get("termTypeId") != null)
		attributeMap.put("termTypeId", params.get("termTypeId"));
	if(params.get("billingAccountId") != null)
		attributeMap.put("billingAccountId", params.get("billingAccountId"));
	try {
		Map result = dispatcher.runSync("updateBillingAccountTerm", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/createGlAccount")
public Response createGlAccount_(@Multipart(value = "createGlAccount", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("glAccountTypeId") != null)
		attributeMap.put("glAccountTypeId", params.get("glAccountTypeId"));
	if(params.get("glAccountClassId") != null)
		attributeMap.put("glAccountClassId", params.get("glAccountClassId"));
	if(params.get("glResourceTypeId") != null)
		attributeMap.put("glResourceTypeId", params.get("glResourceTypeId"));
	if(params.get("glXbrlClassId") != null)
		attributeMap.put("glXbrlClassId", params.get("glXbrlClassId"));
	if(params.get("parentGlAccountId") != null)
		attributeMap.put("parentGlAccountId", params.get("parentGlAccountId"));
	if(params.get("accountCode") != null)
		attributeMap.put("accountCode", params.get("accountCode"));
	if(params.get("accountName") != null)
		attributeMap.put("accountName", params.get("accountName"));
	if(params.get("description") != null)
		attributeMap.put("description", params.get("description"));
	if(params.get("productId") != null)
		attributeMap.put("productId", params.get("productId"));
	if(params.get("externalId") != null)
		attributeMap.put("externalId", params.get("externalId"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	if(params.get("glAccountTypeId") != null)
		attributeMap.put("glAccountTypeId", params.get("glAccountTypeId"));
	if(params.get("glAccountClassId") != null)
		attributeMap.put("glAccountClassId", params.get("glAccountClassId"));
	if(params.get("glResourceTypeId") != null)
		attributeMap.put("glResourceTypeId", params.get("glResourceTypeId"));
	if(params.get("accountName") != null)
		attributeMap.put("accountName", params.get("accountName"));
	try {
		Map result = dispatcher.runSync("createGlAccount", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/updateGlAccount")
public Response updateGlAccount_(@Multipart(value = "updateGlAccount", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("glAccountId") != null)
		attributeMap.put("glAccountId", params.get("glAccountId"));
	if(params.get("glAccountTypeId") != null)
		attributeMap.put("glAccountTypeId", params.get("glAccountTypeId"));
	if(params.get("glAccountClassId") != null)
		attributeMap.put("glAccountClassId", params.get("glAccountClassId"));
	if(params.get("glResourceTypeId") != null)
		attributeMap.put("glResourceTypeId", params.get("glResourceTypeId"));
	if(params.get("glXbrlClassId") != null)
		attributeMap.put("glXbrlClassId", params.get("glXbrlClassId"));
	if(params.get("parentGlAccountId") != null)
		attributeMap.put("parentGlAccountId", params.get("parentGlAccountId"));
	if(params.get("accountCode") != null)
		attributeMap.put("accountCode", params.get("accountCode"));
	if(params.get("accountName") != null)
		attributeMap.put("accountName", params.get("accountName"));
	if(params.get("description") != null)
		attributeMap.put("description", params.get("description"));
	if(params.get("productId") != null)
		attributeMap.put("productId", params.get("productId"));
	if(params.get("externalId") != null)
		attributeMap.put("externalId", params.get("externalId"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("updateGlAccount", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/createGlAccountOrganization")
public Response createGlAccountOrganization_(@Multipart(value = "createGlAccountOrganization", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("glAccountId") != null)
		attributeMap.put("glAccountId", params.get("glAccountId"));
	if(params.get("organizationPartyId") != null)
		attributeMap.put("organizationPartyId", params.get("organizationPartyId"));
	if(params.get("roleTypeId") != null)
		attributeMap.put("roleTypeId", params.get("roleTypeId"));
	if(params.get("fromDate") != null)
		attributeMap.put("fromDate", params.get("fromDate"));
	if(params.get("thruDate") != null)
		attributeMap.put("thruDate", params.get("thruDate"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("createGlAccountOrganization", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}


@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/createCustomTimePeriod")
public Response createCustomTimePeriod_(@Multipart(value = "createCustomTimePeriod", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("parentPeriodId") != null)
		attributeMap.put("parentPeriodId", params.get("parentPeriodId"));
	if(params.get("periodTypeId") != null)
		attributeMap.put("periodTypeId", params.get("periodTypeId"));
	if(params.get("periodNum") != null)
		attributeMap.put("periodNum", params.get("periodNum"));
	if(params.get("periodName") != null)
		attributeMap.put("periodName", params.get("periodName"));
	if(params.get("fromDate") != null)
		attributeMap.put("fromDate", params.get("fromDate"));
	if(params.get("thruDate") != null)
		attributeMap.put("thruDate", params.get("thruDate"));
	if(params.get("isClosed") != null)
		attributeMap.put("isClosed", params.get("isClosed"));
	if(params.get("organizationPartyId") != null)
		attributeMap.put("organizationPartyId", params.get("organizationPartyId"));
	if(params.get("organizationPartyId") != null)
		attributeMap.put("organizationPartyId", params.get("organizationPartyId"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	if(params.get("fromDate") != null)
		attributeMap.put("fromDate", params.get("fromDate"));
	if(params.get("thruDate") != null)
		attributeMap.put("thruDate", params.get("thruDate"));
	if(params.get("periodTypeId") != null)
		attributeMap.put("periodTypeId", params.get("periodTypeId"));
	try {
		Map result = dispatcher.runSync("createCustomTimePeriod", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/updateCustomTimePeriod")
public Response updateCustomTimePeriod_(@Multipart(value = "updateCustomTimePeriod", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("customTimePeriodId") != null)
		attributeMap.put("customTimePeriodId", params.get("customTimePeriodId"));
	if(params.get("parentPeriodId") != null)
		attributeMap.put("parentPeriodId", params.get("parentPeriodId"));
	if(params.get("periodTypeId") != null)
		attributeMap.put("periodTypeId", params.get("periodTypeId"));
	if(params.get("periodNum") != null)
		attributeMap.put("periodNum", params.get("periodNum"));
	if(params.get("periodName") != null)
		attributeMap.put("periodName", params.get("periodName"));
	if(params.get("fromDate") != null)
		attributeMap.put("fromDate", params.get("fromDate"));
	if(params.get("thruDate") != null)
		attributeMap.put("thruDate", params.get("thruDate"));
	if(params.get("isClosed") != null)
		attributeMap.put("isClosed", params.get("isClosed"));
	if(params.get("organizationPartyId") != null)
		attributeMap.put("organizationPartyId", params.get("organizationPartyId"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("updateCustomTimePeriod", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/deleteCustomTimePeriod")
public Response deleteCustomTimePeriod_(@Multipart(value = "deleteCustomTimePeriod", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("customTimePeriodId") != null)
		attributeMap.put("customTimePeriodId", params.get("customTimePeriodId"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("deleteCustomTimePeriod", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}


@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/quickCreateAcctgTransAndEntries")
public Response quickCreateAcctgTransAndEntries_(@Multipart(value = "quickCreateAcctgTransAndEntries", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("acctgTransTypeId") != null)
		attributeMap.put("acctgTransTypeId", params.get("acctgTransTypeId"));
	if(params.get("description") != null)
		attributeMap.put("description", params.get("description"));
	if(params.get("transactionDate") != null)
		attributeMap.put("transactionDate", params.get("transactionDate"));
	if(params.get("isPosted") != null)
		attributeMap.put("isPosted", params.get("isPosted"));
	if(params.get("postedDate") != null)
		attributeMap.put("postedDate", params.get("postedDate"));
	if(params.get("scheduledPostingDate") != null)
		attributeMap.put("scheduledPostingDate", params.get("scheduledPostingDate"));
	if(params.get("glJournalId") != null)
		attributeMap.put("glJournalId", params.get("glJournalId"));
	if(params.get("glFiscalTypeId") != null)
		attributeMap.put("glFiscalTypeId", params.get("glFiscalTypeId"));
	if(params.get("voucherRef") != null)
		attributeMap.put("voucherRef", params.get("voucherRef"));
	if(params.get("voucherDate") != null)
		attributeMap.put("voucherDate", params.get("voucherDate"));
	if(params.get("groupStatusId") != null)
		attributeMap.put("groupStatusId", params.get("groupStatusId"));
	if(params.get("fixedAssetId") != null)
		attributeMap.put("fixedAssetId", params.get("fixedAssetId"));
	if(params.get("inventoryItemId") != null)
		attributeMap.put("inventoryItemId", params.get("inventoryItemId"));
	if(params.get("physicalInventoryId") != null)
		attributeMap.put("physicalInventoryId", params.get("physicalInventoryId"));
	if(params.get("partyId") != null)
		attributeMap.put("partyId", params.get("partyId"));
	if(params.get("roleTypeId") != null)
		attributeMap.put("roleTypeId", params.get("roleTypeId"));
	if(params.get("invoiceId") != null)
		attributeMap.put("invoiceId", params.get("invoiceId"));
	if(params.get("paymentId") != null)
		attributeMap.put("paymentId", params.get("paymentId"));
	if(params.get("finAccountTransId") != null)
		attributeMap.put("finAccountTransId", params.get("finAccountTransId"));
	if(params.get("shipmentId") != null)
		attributeMap.put("shipmentId", params.get("shipmentId"));
	if(params.get("receiptId") != null)
		attributeMap.put("receiptId", params.get("receiptId"));
	if(params.get("workEffortId") != null)
		attributeMap.put("workEffortId", params.get("workEffortId"));
	if(params.get("theirAcctgTransId") != null)
		attributeMap.put("theirAcctgTransId", params.get("theirAcctgTransId"));
	if(params.get("createdDate") != null)
		attributeMap.put("createdDate", params.get("createdDate"));
	if(params.get("createdByUserLogin") != null)
		attributeMap.put("createdByUserLogin", params.get("createdByUserLogin"));
	if(params.get("lastModifiedDate") != null)
		attributeMap.put("lastModifiedDate", params.get("lastModifiedDate"));
	if(params.get("lastModifiedByUserLogin") != null)
		attributeMap.put("lastModifiedByUserLogin", params.get("lastModifiedByUserLogin"));
	if(params.get("acctgTransEntryTypeId") != null)
		attributeMap.put("acctgTransEntryTypeId", params.get("acctgTransEntryTypeId"));
	if(params.get("description") != null)
		attributeMap.put("description", params.get("description"));
	if(params.get("voucherRef") != null)
		attributeMap.put("voucherRef", params.get("voucherRef"));
	if(params.get("partyId") != null)
		attributeMap.put("partyId", params.get("partyId"));
	if(params.get("roleTypeId") != null)
		attributeMap.put("roleTypeId", params.get("roleTypeId"));
	if(params.get("theirPartyId") != null)
		attributeMap.put("theirPartyId", params.get("theirPartyId"));
	if(params.get("productId") != null)
		attributeMap.put("productId", params.get("productId"));
	if(params.get("theirProductId") != null)
		attributeMap.put("theirProductId", params.get("theirProductId"));
	if(params.get("inventoryItemId") != null)
		attributeMap.put("inventoryItemId", params.get("inventoryItemId"));
	if(params.get("glAccountTypeId") != null)
		attributeMap.put("glAccountTypeId", params.get("glAccountTypeId"));
	if(params.get("organizationPartyId") != null)
		attributeMap.put("organizationPartyId", params.get("organizationPartyId"));
	if(params.get("amount") != null)
		attributeMap.put("amount", params.get("amount"));
	if(params.get("currencyUomId") != null)
		attributeMap.put("currencyUomId", params.get("currencyUomId"));
	if(params.get("origAmount") != null)
		attributeMap.put("origAmount", params.get("origAmount"));
	if(params.get("origCurrencyUomId") != null)
		attributeMap.put("origCurrencyUomId", params.get("origCurrencyUomId"));
	if(params.get("dueDate") != null)
		attributeMap.put("dueDate", params.get("dueDate"));
	if(params.get("groupId") != null)
		attributeMap.put("groupId", params.get("groupId"));
	if(params.get("taxId") != null)
		attributeMap.put("taxId", params.get("taxId"));
	if(params.get("reconcileStatusId") != null)
		attributeMap.put("reconcileStatusId", params.get("reconcileStatusId"));
	if(params.get("settlementTermId") != null)
		attributeMap.put("settlementTermId", params.get("settlementTermId"));
	if(params.get("isSummary") != null)
		attributeMap.put("isSummary", params.get("isSummary"));
	if(params.get("debitGlAccountId") != null)
		attributeMap.put("debitGlAccountId", params.get("debitGlAccountId"));
	if(params.get("creditGlAccountId") != null)
		attributeMap.put("creditGlAccountId", params.get("creditGlAccountId"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	if(params.get("amount") != null)
		attributeMap.put("amount", params.get("amount"));
	try {
		Map result = dispatcher.runSync("quickCreateAcctgTransAndEntries", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/createGlJournal")
public Response createGlJournal_(@Multipart(value = "createGlJournal", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("glJournalName") != null)
		attributeMap.put("glJournalName", params.get("glJournalName"));
	if(params.get("organizationPartyId") != null)
		attributeMap.put("organizationPartyId", params.get("organizationPartyId"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	if(params.get("organizationPartyId") != null)
		attributeMap.put("organizationPartyId", params.get("organizationPartyId"));
	try {
		Map result = dispatcher.runSync("createGlJournal", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/updateGlJournal")
public Response updateGlJournal_(@Multipart(value = "updateGlJournal", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("glJournalId") != null)
		attributeMap.put("glJournalId", params.get("glJournalId"));
	if(params.get("glJournalName") != null)
		attributeMap.put("glJournalName", params.get("glJournalName"));
	if(params.get("organizationPartyId") != null)
		attributeMap.put("organizationPartyId", params.get("organizationPartyId"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("updateGlJournal", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/deleteGlJournal")
public Response deleteGlJournal_(@Multipart(value = "deleteGlJournal", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("glJournalId") != null)
		attributeMap.put("glJournalId", params.get("glJournalId"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("deleteGlJournal", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}


@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/completeAcctgTransEntries")
public Response completeAcctgTransEntries_(@Multipart(value = "completeAcctgTransEntries", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("acctgTransId") != null)
		attributeMap.put("acctgTransId", params.get("acctgTransId"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("completeAcctgTransEntries", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/createAcctgTrans")
public Response createAcctgTrans_(@Multipart(value = "createAcctgTrans", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	if(params.get("acctgTransTypeId") != null)
		attributeMap.put("acctgTransTypeId", params.get("acctgTransTypeId"));
	if(params.get("description") != null)
		attributeMap.put("description", params.get("description"));
	if(params.get("transactionDate") != null)
		attributeMap.put("transactionDate", params.get("transactionDate"));
	if(params.get("scheduledPostingDate") != null)
		attributeMap.put("scheduledPostingDate", params.get("scheduledPostingDate"));
	if(params.get("glJournalId") != null)
		attributeMap.put("glJournalId", params.get("glJournalId"));
	if(params.get("glFiscalTypeId") != null)
		attributeMap.put("glFiscalTypeId", params.get("glFiscalTypeId"));
	if(params.get("voucherRef") != null)
		attributeMap.put("voucherRef", params.get("voucherRef"));
	if(params.get("voucherDate") != null)
		attributeMap.put("voucherDate", params.get("voucherDate"));
	if(params.get("groupStatusId") != null)
		attributeMap.put("groupStatusId", params.get("groupStatusId"));
	if(params.get("fixedAssetId") != null)
		attributeMap.put("fixedAssetId", params.get("fixedAssetId"));
	if(params.get("inventoryItemId") != null)
		attributeMap.put("inventoryItemId", params.get("inventoryItemId"));
	if(params.get("physicalInventoryId") != null)
		attributeMap.put("physicalInventoryId", params.get("physicalInventoryId"));
	if(params.get("partyId") != null)
		attributeMap.put("partyId", params.get("partyId"));
	if(params.get("roleTypeId") != null)
		attributeMap.put("roleTypeId", params.get("roleTypeId"));
	if(params.get("invoiceId") != null)
		attributeMap.put("invoiceId", params.get("invoiceId"));
	if(params.get("paymentId") != null)
		attributeMap.put("paymentId", params.get("paymentId"));
	if(params.get("finAccountTransId") != null)
		attributeMap.put("finAccountTransId", params.get("finAccountTransId"));
	if(params.get("shipmentId") != null)
		attributeMap.put("shipmentId", params.get("shipmentId"));
	if(params.get("receiptId") != null)
		attributeMap.put("receiptId", params.get("receiptId"));
	if(params.get("workEffortId") != null)
		attributeMap.put("workEffortId", params.get("workEffortId"));
	if(params.get("theirAcctgTransId") != null)
		attributeMap.put("theirAcctgTransId", params.get("theirAcctgTransId"));
	if(params.get("createdDate") != null)
		attributeMap.put("createdDate", params.get("createdDate"));
	if(params.get("lastModifiedDate") != null)
		attributeMap.put("lastModifiedDate", params.get("lastModifiedDate"));
	try {
		Map result = dispatcher.runSync("createAcctgTrans", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/updateAcctgTrans")
public Response updateAcctgTrans_(@Multipart(value = "updateAcctgTrans", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("acctgTransId") != null)
		attributeMap.put("acctgTransId", params.get("acctgTransId"));
	if(params.get("acctgTransTypeId") != null)
		attributeMap.put("acctgTransTypeId", params.get("acctgTransTypeId"));
	if(params.get("description") != null)
		attributeMap.put("description", params.get("description"));
	if(params.get("transactionDate") != null)
		attributeMap.put("transactionDate", params.get("transactionDate"));
	if(params.get("isPosted") != null)
		attributeMap.put("isPosted", params.get("isPosted"));
	if(params.get("postedDate") != null)
		attributeMap.put("postedDate", params.get("postedDate"));
	if(params.get("scheduledPostingDate") != null)
		attributeMap.put("scheduledPostingDate", params.get("scheduledPostingDate"));
	if(params.get("glJournalId") != null)
		attributeMap.put("glJournalId", params.get("glJournalId"));
	if(params.get("glFiscalTypeId") != null)
		attributeMap.put("glFiscalTypeId", params.get("glFiscalTypeId"));
	if(params.get("voucherRef") != null)
		attributeMap.put("voucherRef", params.get("voucherRef"));
	if(params.get("voucherDate") != null)
		attributeMap.put("voucherDate", params.get("voucherDate"));
	if(params.get("groupStatusId") != null)
		attributeMap.put("groupStatusId", params.get("groupStatusId"));
	if(params.get("fixedAssetId") != null)
		attributeMap.put("fixedAssetId", params.get("fixedAssetId"));
	if(params.get("inventoryItemId") != null)
		attributeMap.put("inventoryItemId", params.get("inventoryItemId"));
	if(params.get("physicalInventoryId") != null)
		attributeMap.put("physicalInventoryId", params.get("physicalInventoryId"));
	if(params.get("partyId") != null)
		attributeMap.put("partyId", params.get("partyId"));
	if(params.get("roleTypeId") != null)
		attributeMap.put("roleTypeId", params.get("roleTypeId"));
	if(params.get("invoiceId") != null)
		attributeMap.put("invoiceId", params.get("invoiceId"));
	if(params.get("paymentId") != null)
		attributeMap.put("paymentId", params.get("paymentId"));
	if(params.get("finAccountTransId") != null)
		attributeMap.put("finAccountTransId", params.get("finAccountTransId"));
	if(params.get("shipmentId") != null)
		attributeMap.put("shipmentId", params.get("shipmentId"));
	if(params.get("receiptId") != null)
		attributeMap.put("receiptId", params.get("receiptId"));
	if(params.get("workEffortId") != null)
		attributeMap.put("workEffortId", params.get("workEffortId"));
	if(params.get("theirAcctgTransId") != null)
		attributeMap.put("theirAcctgTransId", params.get("theirAcctgTransId"));
	if(params.get("createdDate") != null)
		attributeMap.put("createdDate", params.get("createdDate"));
	if(params.get("createdByUserLogin") != null)
		attributeMap.put("createdByUserLogin", params.get("createdByUserLogin"));
	if(params.get("lastModifiedDate") != null)
		attributeMap.put("lastModifiedDate", params.get("lastModifiedDate"));
	if(params.get("lastModifiedByUserLogin") != null)
		attributeMap.put("lastModifiedByUserLogin", params.get("lastModifiedByUserLogin"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("updateAcctgTrans", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/createAcctgTransEntry")
public Response createAcctgTransEntry_(@Multipart(value = "createAcctgTransEntry", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("acctgTransId") != null)
		attributeMap.put("acctgTransId", params.get("acctgTransId"));
	if(params.get("purposeEnumId") != null)
		attributeMap.put("purposeEnumId", params.get("purposeEnumId"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	if(params.get("acctgTransEntryTypeId") != null)
		attributeMap.put("acctgTransEntryTypeId", params.get("acctgTransEntryTypeId"));
	if(params.get("description") != null)
		attributeMap.put("description", params.get("description"));
	if(params.get("voucherRef") != null)
		attributeMap.put("voucherRef", params.get("voucherRef"));
	if(params.get("partyId") != null)
		attributeMap.put("partyId", params.get("partyId"));
	if(params.get("roleTypeId") != null)
		attributeMap.put("roleTypeId", params.get("roleTypeId"));
	if(params.get("theirPartyId") != null)
		attributeMap.put("theirPartyId", params.get("theirPartyId"));
	if(params.get("productId") != null)
		attributeMap.put("productId", params.get("productId"));
	if(params.get("theirProductId") != null)
		attributeMap.put("theirProductId", params.get("theirProductId"));
	if(params.get("inventoryItemId") != null)
		attributeMap.put("inventoryItemId", params.get("inventoryItemId"));
	if(params.get("glAccountTypeId") != null)
		attributeMap.put("glAccountTypeId", params.get("glAccountTypeId"));
	if(params.get("glAccountId") != null)
		attributeMap.put("glAccountId", params.get("glAccountId"));
	if(params.get("organizationPartyId") != null)
		attributeMap.put("organizationPartyId", params.get("organizationPartyId"));
	if(params.get("amount") != null)
		attributeMap.put("amount", params.get("amount"));
	if(params.get("currencyUomId") != null)
		attributeMap.put("currencyUomId", params.get("currencyUomId"));
	if(params.get("origAmount") != null)
		attributeMap.put("origAmount", params.get("origAmount"));
	if(params.get("origCurrencyUomId") != null)
		attributeMap.put("origCurrencyUomId", params.get("origCurrencyUomId"));
	if(params.get("debitCreditFlag") != null)
		attributeMap.put("debitCreditFlag", params.get("debitCreditFlag"));
	if(params.get("dueDate") != null)
		attributeMap.put("dueDate", params.get("dueDate"));
	if(params.get("groupId") != null)
		attributeMap.put("groupId", params.get("groupId"));
	if(params.get("taxId") != null)
		attributeMap.put("taxId", params.get("taxId"));
	if(params.get("settlementTermId") != null)
		attributeMap.put("settlementTermId", params.get("settlementTermId"));
	if(params.get("isSummary") != null)
		attributeMap.put("isSummary", params.get("isSummary"));
	try {
		Map result = dispatcher.runSync("createAcctgTransEntry", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/updateAcctgTransEntry")
public Response updateAcctgTransEntry_(@Multipart(value = "updateAcctgTransEntry", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("acctgTransId") != null)
		attributeMap.put("acctgTransId", params.get("acctgTransId"));
	if(params.get("acctgTransEntrySeqId") != null)
		attributeMap.put("acctgTransEntrySeqId", params.get("acctgTransEntrySeqId"));
	if(params.get("acctgTransEntryTypeId") != null)
		attributeMap.put("acctgTransEntryTypeId", params.get("acctgTransEntryTypeId"));
	if(params.get("description") != null)
		attributeMap.put("description", params.get("description"));
	if(params.get("voucherRef") != null)
		attributeMap.put("voucherRef", params.get("voucherRef"));
	if(params.get("partyId") != null)
		attributeMap.put("partyId", params.get("partyId"));
	if(params.get("roleTypeId") != null)
		attributeMap.put("roleTypeId", params.get("roleTypeId"));
	if(params.get("theirPartyId") != null)
		attributeMap.put("theirPartyId", params.get("theirPartyId"));
	if(params.get("productId") != null)
		attributeMap.put("productId", params.get("productId"));
	if(params.get("theirProductId") != null)
		attributeMap.put("theirProductId", params.get("theirProductId"));
	if(params.get("inventoryItemId") != null)
		attributeMap.put("inventoryItemId", params.get("inventoryItemId"));
	if(params.get("glAccountTypeId") != null)
		attributeMap.put("glAccountTypeId", params.get("glAccountTypeId"));
	if(params.get("glAccountId") != null)
		attributeMap.put("glAccountId", params.get("glAccountId"));
	if(params.get("organizationPartyId") != null)
		attributeMap.put("organizationPartyId", params.get("organizationPartyId"));
	if(params.get("amount") != null)
		attributeMap.put("amount", params.get("amount"));
	if(params.get("currencyUomId") != null)
		attributeMap.put("currencyUomId", params.get("currencyUomId"));
	if(params.get("origAmount") != null)
		attributeMap.put("origAmount", params.get("origAmount"));
	if(params.get("origCurrencyUomId") != null)
		attributeMap.put("origCurrencyUomId", params.get("origCurrencyUomId"));
	if(params.get("debitCreditFlag") != null)
		attributeMap.put("debitCreditFlag", params.get("debitCreditFlag"));
	if(params.get("dueDate") != null)
		attributeMap.put("dueDate", params.get("dueDate"));
	if(params.get("groupId") != null)
		attributeMap.put("groupId", params.get("groupId"));
	if(params.get("taxId") != null)
		attributeMap.put("taxId", params.get("taxId"));
	if(params.get("reconcileStatusId") != null)
		attributeMap.put("reconcileStatusId", params.get("reconcileStatusId"));
	if(params.get("settlementTermId") != null)
		attributeMap.put("settlementTermId", params.get("settlementTermId"));
	if(params.get("isSummary") != null)
		attributeMap.put("isSummary", params.get("isSummary"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("updateAcctgTransEntry", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/deleteAcctgTransEntry")
public Response deleteAcctgTransEntry_(@Multipart(value = "deleteAcctgTransEntry", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("acctgTransId") != null)
		attributeMap.put("acctgTransId", params.get("acctgTransId"));
	if(params.get("acctgTransEntrySeqId") != null)
		attributeMap.put("acctgTransEntrySeqId", params.get("acctgTransEntrySeqId"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("deleteAcctgTransEntry", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/postAcctgTrans")
public Response postAcctgTrans_(@Multipart(value = "postAcctgTrans", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("acctgTransId") != null)
		attributeMap.put("acctgTransId", params.get("acctgTransId"));
	if(params.get("verifyOnly") != null)
		attributeMap.put("verifyOnly", params.get("verifyOnly"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("postAcctgTrans", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}


@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/setAcctgCompany")
public Response setAcctgCompany_(@Multipart(value = "setAcctgCompany", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("organizationPartyId") != null)
		attributeMap.put("organizationPartyId", params.get("organizationPartyId"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("setAcctgCompany", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/closeFinancialTimePeriod")
public Response closeFinancialTimePeriod_(@Multipart(value = "closeFinancialTimePeriod", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("customTimePeriodId") != null)
		attributeMap.put("customTimePeriodId", params.get("customTimePeriodId"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("closeFinancialTimePeriod", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}


@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/createFinAccountTypeGlAccount")
public Response createFinAccountTypeGlAccount_(@Multipart(value = "createFinAccountTypeGlAccount", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("finAccountTypeId") != null)
		attributeMap.put("finAccountTypeId", params.get("finAccountTypeId"));
	if(params.get("organizationPartyId") != null)
		attributeMap.put("organizationPartyId", params.get("organizationPartyId"));
	if(params.get("glAccountId") != null)
		attributeMap.put("glAccountId", params.get("glAccountId"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("createFinAccountTypeGlAccount", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/addInvoiceItemTypeGlAssignment")
public Response addInvoiceItemTypeGlAssignment_(@Multipart(value = "addInvoiceItemTypeGlAssignment", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("invoiceItemTypeId") != null)
		attributeMap.put("invoiceItemTypeId", params.get("invoiceItemTypeId"));
	if(params.get("organizationPartyId") != null)
		attributeMap.put("organizationPartyId", params.get("organizationPartyId"));
	if(params.get("glAccountId") != null)
		attributeMap.put("glAccountId", params.get("glAccountId"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("addInvoiceItemTypeGlAssignment", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/removeInvoiceItemTypeGlAssignment")
public Response removeInvoiceItemTypeGlAssignment_(@Multipart(value = "removeInvoiceItemTypeGlAssignment", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("invoiceItemTypeId") != null)
		attributeMap.put("invoiceItemTypeId", params.get("invoiceItemTypeId"));
	if(params.get("organizationPartyId") != null)
		attributeMap.put("organizationPartyId", params.get("organizationPartyId"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("removeInvoiceItemTypeGlAssignment", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}


@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/updateFinAccountTypeGlAccount")
public Response updateFinAccountTypeGlAccount_(@Multipart(value = "updateFinAccountTypeGlAccount", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("finAccountTypeId") != null)
		attributeMap.put("finAccountTypeId", params.get("finAccountTypeId"));
	if(params.get("organizationPartyId") != null)
		attributeMap.put("organizationPartyId", params.get("organizationPartyId"));
	if(params.get("glAccountId") != null)
		attributeMap.put("glAccountId", params.get("glAccountId"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("updateFinAccountTypeGlAccount", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/deleteFinAccountTypeGlAccount")
public Response deleteFinAccountTypeGlAccount_(@Multipart(value = "deleteFinAccountTypeGlAccount", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("finAccountTypeId") != null)
		attributeMap.put("finAccountTypeId", params.get("finAccountTypeId"));
	if(params.get("organizationPartyId") != null)
		attributeMap.put("organizationPartyId", params.get("organizationPartyId"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("deleteFinAccountTypeGlAccount", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}




@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/createVarianceReasonGlAccount")
public Response createVarianceReasonGlAccount_(@Multipart(value = "createVarianceReasonGlAccount", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("varianceReasonId") != null)
		attributeMap.put("varianceReasonId", params.get("varianceReasonId"));
	if(params.get("organizationPartyId") != null)
		attributeMap.put("organizationPartyId", params.get("organizationPartyId"));
	if(params.get("glAccountId") != null)
		attributeMap.put("glAccountId", params.get("glAccountId"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("createVarianceReasonGlAccount", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/createCreditCardTypeGlAccount")
public Response createCreditCardTypeGlAccount_(@Multipart(value = "createCreditCardTypeGlAccount", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("cardType") != null)
		attributeMap.put("cardType", params.get("cardType"));
	if(params.get("organizationPartyId") != null)
		attributeMap.put("organizationPartyId", params.get("organizationPartyId"));
	if(params.get("glAccountId") != null)
		attributeMap.put("glAccountId", params.get("glAccountId"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("createCreditCardTypeGlAccount", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/updateCreditCardTypeGlAccount")
public Response updateCreditCardTypeGlAccount_(@Multipart(value = "updateCreditCardTypeGlAccount", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("cardType") != null)
		attributeMap.put("cardType", params.get("cardType"));
	if(params.get("organizationPartyId") != null)
		attributeMap.put("organizationPartyId", params.get("organizationPartyId"));
	if(params.get("glAccountId") != null)
		attributeMap.put("glAccountId", params.get("glAccountId"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("updateCreditCardTypeGlAccount", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/deleteCreditCardTypeGlAccount")
public Response deleteCreditCardTypeGlAccount_(@Multipart(value = "deleteCreditCardTypeGlAccount", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("cardType") != null)
		attributeMap.put("cardType", params.get("cardType"));
	if(params.get("organizationPartyId") != null)
		attributeMap.put("organizationPartyId", params.get("organizationPartyId"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("deleteCreditCardTypeGlAccount", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}


@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/updateVarianceReasonGlAccount")
public Response updateVarianceReasonGlAccount_(@Multipart(value = "updateVarianceReasonGlAccount", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("varianceReasonId") != null)
		attributeMap.put("varianceReasonId", params.get("varianceReasonId"));
	if(params.get("organizationPartyId") != null)
		attributeMap.put("organizationPartyId", params.get("organizationPartyId"));
	if(params.get("glAccountId") != null)
		attributeMap.put("glAccountId", params.get("glAccountId"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("updateVarianceReasonGlAccount", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/deleteVarianceReasonGlAccount")
public Response deleteVarianceReasonGlAccount_(@Multipart(value = "deleteVarianceReasonGlAccount", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("varianceReasonId") != null)
		attributeMap.put("varianceReasonId", params.get("varianceReasonId"));
	if(params.get("organizationPartyId") != null)
		attributeMap.put("organizationPartyId", params.get("organizationPartyId"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("deleteVarianceReasonGlAccount", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/copyAcctgTransAndEntries")
public Response copyAcctgTransAndEntries_(@Multipart(value = "copyAcctgTransAndEntries", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("fromAcctgTransId") != null)
		attributeMap.put("fromAcctgTransId", params.get("fromAcctgTransId"));
	if(params.get("revert") != null)
		attributeMap.put("revert", params.get("revert"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("copyAcctgTransAndEntries", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/createPartyGlAccount")
public Response createPartyGlAccount_(@Multipart(value = "createPartyGlAccount", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("organizationPartyId") != null)
		attributeMap.put("organizationPartyId", params.get("organizationPartyId"));
	if(params.get("partyId") != null)
		attributeMap.put("partyId", params.get("partyId"));
	if(params.get("roleTypeId") != null)
		attributeMap.put("roleTypeId", params.get("roleTypeId"));
	if(params.get("glAccountTypeId") != null)
		attributeMap.put("glAccountTypeId", params.get("glAccountTypeId"));
	if(params.get("glAccountId") != null)
		attributeMap.put("glAccountId", params.get("glAccountId"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("createPartyGlAccount", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/updatePartyGlAccount")
public Response updatePartyGlAccount_(@Multipart(value = "updatePartyGlAccount", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("organizationPartyId") != null)
		attributeMap.put("organizationPartyId", params.get("organizationPartyId"));
	if(params.get("partyId") != null)
		attributeMap.put("partyId", params.get("partyId"));
	if(params.get("roleTypeId") != null)
		attributeMap.put("roleTypeId", params.get("roleTypeId"));
	if(params.get("glAccountTypeId") != null)
		attributeMap.put("glAccountTypeId", params.get("glAccountTypeId"));
	if(params.get("glAccountId") != null)
		attributeMap.put("glAccountId", params.get("glAccountId"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("updatePartyGlAccount", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/deletePartyGlAccount")
public Response deletePartyGlAccount_(@Multipart(value = "deletePartyGlAccount", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("organizationPartyId") != null)
		attributeMap.put("organizationPartyId", params.get("organizationPartyId"));
	if(params.get("partyId") != null)
		attributeMap.put("partyId", params.get("partyId"));
	if(params.get("roleTypeId") != null)
		attributeMap.put("roleTypeId", params.get("roleTypeId"));
	if(params.get("glAccountTypeId") != null)
		attributeMap.put("glAccountTypeId", params.get("glAccountTypeId"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("deletePartyGlAccount", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/createGlAccountCategory")
public Response createGlAccountCategory_(@Multipart(value = "createGlAccountCategory", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("glAccountCategoryTypeId") != null)
		attributeMap.put("glAccountCategoryTypeId", params.get("glAccountCategoryTypeId"));
	if(params.get("description") != null)
		attributeMap.put("description", params.get("description"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("createGlAccountCategory", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/updateGlAccountCategory")
public Response updateGlAccountCategory_(@Multipart(value = "updateGlAccountCategory", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("glAccountCategoryId") != null)
		attributeMap.put("glAccountCategoryId", params.get("glAccountCategoryId"));
	if(params.get("glAccountCategoryTypeId") != null)
		attributeMap.put("glAccountCategoryTypeId", params.get("glAccountCategoryTypeId"));
	if(params.get("description") != null)
		attributeMap.put("description", params.get("description"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("updateGlAccountCategory", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/createGlAccountCategoryMember")
public Response createGlAccountCategoryMember_(@Multipart(value = "createGlAccountCategoryMember", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("glAccountId") != null)
		attributeMap.put("glAccountId", params.get("glAccountId"));
	if(params.get("glAccountCategoryId") != null)
		attributeMap.put("glAccountCategoryId", params.get("glAccountCategoryId"));
	if(params.get("fromDate") != null)
		attributeMap.put("fromDate", params.get("fromDate"));
	if(params.get("thruDate") != null)
		attributeMap.put("thruDate", params.get("thruDate"));
	if(params.get("amountPercentage") != null)
		attributeMap.put("amountPercentage", params.get("amountPercentage"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	if(params.get("fromDate") != null)
		attributeMap.put("fromDate", params.get("fromDate"));
	try {
		Map result = dispatcher.runSync("createGlAccountCategoryMember", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/deleteGlAccountCategoryMember")
public Response deleteGlAccountCategoryMember_(@Multipart(value = "deleteGlAccountCategoryMember", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("glAccountId") != null)
		attributeMap.put("glAccountId", params.get("glAccountId"));
	if(params.get("glAccountCategoryId") != null)
		attributeMap.put("glAccountCategoryId", params.get("glAccountCategoryId"));
	if(params.get("fromDate") != null)
		attributeMap.put("fromDate", params.get("fromDate"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("deleteGlAccountCategoryMember", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}


@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/createGlAccountTypeDefault")
public Response createGlAccountTypeDefault_(@Multipart(value = "createGlAccountTypeDefault", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("glAccountTypeId") != null)
		attributeMap.put("glAccountTypeId", params.get("glAccountTypeId"));
	if(params.get("organizationPartyId") != null)
		attributeMap.put("organizationPartyId", params.get("organizationPartyId"));
	if(params.get("glAccountId") != null)
		attributeMap.put("glAccountId", params.get("glAccountId"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("createGlAccountTypeDefault", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/removeGlAccountTypeDefault")
public Response removeGlAccountTypeDefault_(@Multipart(value = "removeGlAccountTypeDefault", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("glAccountTypeId") != null)
		attributeMap.put("glAccountTypeId", params.get("glAccountTypeId"));
	if(params.get("organizationPartyId") != null)
		attributeMap.put("organizationPartyId", params.get("organizationPartyId"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("removeGlAccountTypeDefault", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}
@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/createProductCategoryGlAccount")
public Response createProductCategoryGlAccount_(@Multipart(value = "createProductCategoryGlAccount", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("productCategoryId") != null)
		attributeMap.put("productCategoryId", params.get("productCategoryId"));
	if(params.get("organizationPartyId") != null)
		attributeMap.put("organizationPartyId", params.get("organizationPartyId"));
	if(params.get("glAccountTypeId") != null)
		attributeMap.put("glAccountTypeId", params.get("glAccountTypeId"));
	if(params.get("glAccountId") != null)
		attributeMap.put("glAccountId", params.get("glAccountId"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("createProductCategoryGlAccount", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/updateProductCategoryGlAccount")
public Response updateProductCategoryGlAccount_(@Multipart(value = "updateProductCategoryGlAccount", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("productCategoryId") != null)
		attributeMap.put("productCategoryId", params.get("productCategoryId"));
	if(params.get("organizationPartyId") != null)
		attributeMap.put("organizationPartyId", params.get("organizationPartyId"));
	if(params.get("glAccountTypeId") != null)
		attributeMap.put("glAccountTypeId", params.get("glAccountTypeId"));
	if(params.get("glAccountId") != null)
		attributeMap.put("glAccountId", params.get("glAccountId"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("updateProductCategoryGlAccount", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/deleteProductCategoryGlAccount")
public Response deleteProductCategoryGlAccount_(@Multipart(value = "deleteProductCategoryGlAccount", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("productCategoryId") != null)
		attributeMap.put("productCategoryId", params.get("productCategoryId"));
	if(params.get("organizationPartyId") != null)
		attributeMap.put("organizationPartyId", params.get("organizationPartyId"));
	if(params.get("glAccountTypeId") != null)
		attributeMap.put("glAccountTypeId", params.get("glAccountTypeId"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("deleteProductCategoryGlAccount", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}


@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/updateGlAccountCategoryMember")
public Response updateGlAccountCategoryMember_(@Multipart(value = "updateGlAccountCategoryMember", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("glAccountId") != null)
		attributeMap.put("glAccountId", params.get("glAccountId"));
	if(params.get("glAccountCategoryId") != null)
		attributeMap.put("glAccountCategoryId", params.get("glAccountCategoryId"));
	if(params.get("fromDate") != null)
		attributeMap.put("fromDate", params.get("fromDate"));
	if(params.get("thruDate") != null)
		attributeMap.put("thruDate", params.get("thruDate"));
	if(params.get("amountPercentage") != null)
		attributeMap.put("amountPercentage", params.get("amountPercentage"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("updateGlAccountCategoryMember", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}



@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("/removeBillingAccountTerm")
public Response removeBillingAccountTerm_(@Multipart(value = "removeBillingAccountTerm", required = true, type = MediaType.APPLICATION_JSON) Map params) {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	Map<String, Object> attributeMap = new HashMap<String, Object>();
	if(params.get("billingAccountTermId") != null)
		attributeMap.put("billingAccountTermId", params.get("billingAccountTermId"));
	if(params.get("userLogin") != null)
		attributeMap.put("userLogin", params.get("userLogin"));
	else {
		try {
			attributeMap.put("userLogin", (GenericValue) EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "admin").queryFirst());
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
	}
	if(params.get("login.username") != null)
		attributeMap.put("login.username", params.get("login.username"));
	if(params.get("login.password") != null)
		attributeMap.put("login.password", params.get("login.password"));
	if(params.get("locale") != null)
		attributeMap.put("locale", params.get("locale"));
	if(params.get("timeZone") != null)
		attributeMap.put("timeZone", params.get("timeZone"));
	try {
		Map result = dispatcher.runSync("removeBillingAccountTerm", attributeMap);
		return Response.ok().entity(result).build();
	} catch(GenericServiceException e) {
		e.printStackTrace();
		return null;
	}
}



	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("{message}")
	public Response sayHello(@PathParam("message") String message) throws Exception {
		String username = null;
		String password = null;

		try {
			username = headers.getRequestHeader("login.username").get(0);
			password = headers.getRequestHeader("login.password").get(0);
		} catch (NullPointerException e) {
			return Response.serverError().entity("Problem reading http header(s): login.username or login.password")
					.build();
		}

		if (username == null || password == null) {
			return Response.serverError().entity("Problem reading http header(s): login.username or login.password")
					.build();
		}
		// LocalDispatcher dispatcher = ctx.getDispatcher();
		GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
		LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);

		Map<String, String> paramMap = UtilMisc.toMap("idToFind", "10000", "login.username", username, "login.password",
				password);
		Debug.log("In Ping Message rEst Service");

		Map<String, Object> result = new HashMap();

		try {

			result = UtilMisc.toMap("message", message, "login.username", username, "login.password", password);
			result = dispatcher.runSync("findPartiesById", paramMap);
		} catch (Exception e1) {
			Debug.logError(e1, PingResource.class.getName());
			result = UtilMisc.toMap("message", e1.toString(), "error", "yes");
			return Response.serverError().entity(new ObjectMapper().writeValueAsString(result)).build();
		}
		Debug.log("In Ping Message rEst Service-- Captured Result");

		if (ServiceUtil.isSuccess(result)) {

			return Response.ok(new ObjectMapper().writeValueAsString(result), "application/json").build();
		}

		if (ServiceUtil.isError(result) || ServiceUtil.isFailure(result)) {
			return Response.serverError().entity(ServiceUtil.getErrorMessage(result)).build();
		}

		// shouldn't ever get here ... should we?
		throw new RuntimeException("Invalid ");
	}

	// @GET
	// @Produces(MediaType.APPLICATION_JSON)
	// @Path("/getPerson")
	// public Response createPersons(@PathParam("message") String message, @QueryParam("	") String partyId)
	// 		throws Exception {
	// 	String username = null;
	// 	String password = null;

	// 	try {
	// 		username = headers.getRequestHeader("login.username").get(0);
	// 		password = headers.getRequestHeader("login.password").get(0);
	// 	} catch (NullPointerException e) {
	// 		return Response.serverError().entity("Problem reading http header(s): login.username or login.password")
	// 				.build();
	// 	}

	// 	if (username == null || password == null) {
	// 		return Response.serverError().entity("Problem reading http header(s): login.username or login.password")
	// 				.build();
	// 	}
	// 	// LocalDispatcher dispatcher = ctx.getDispatcher();
	// 	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
	// 	LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
	// 	Debug.log("got Party ID to find--" + partyId);
	// 	if (partyId == null) {
	// 		partyId = "10000";
	// 	}
	// 	GenericValue d = EntityQuery.use(delegator).from("Person").where("partyId", partyId).cache().queryOne();
	// 	System.out.println(d);
	// 	Map<String, String> paramMap = UtilMisc.toMap("idToFind", partyId, "login.username", username, "login.password",
	// 			password);
	// 	Debug.log("In Ping Message rEst Service");

	// 	Map<String, Object> result = new HashMap();

	// 	try {

	// 		result = UtilMisc.toMap("message", message, "login.username", username, "login.password", password,
	// 				"searchAllId", "s");
	// 		result = dispatcher.runSync("findPartiesById", paramMap);
	// 		result.put("PartyPerson", dispatcher.runSync("findPartyService", paramMap));
	// 	} catch (Exception e1) {
	// 		Debug.logError(e1, PingResource.class.getName());
	// 		result = UtilMisc.toMap("message", e1.toString(), "error", "yes");
	// 		return Response.serverError().entity(new ObjectMapper().writeValueAsString(result)).build();
	// 	}
	// 	Debug.log("In Ping Message rEst Service-- Captured Result");

	// 	if (ServiceUtil.isSuccess(result)) {

	// 		return Response.ok(new ObjectMapper().writeValueAsString(result), "application/json").build();
	// 	}

	// 	if (ServiceUtil.isError(result) || ServiceUtil.isFailure(result)) {
	// 		return Response.serverError().entity(ServiceUtil.getErrorMessage(result)).build();
	// 	}

	// 	// shouldn't ever get here ... should we?
	// 	throw new RuntimeException("Invalid ");
	// }
}
