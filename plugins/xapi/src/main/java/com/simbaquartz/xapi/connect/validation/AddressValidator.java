package com.simbaquartz.xapi.connect.validation;

import com.fidelissd.zcp.xcommon.models.geo.PostalAddress;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.service.ServiceUtil;

import java.util.Map;

/**
 * Created by mande on 9/28/2019.
 */
public class AddressValidator {

    private static final String module = AddressValidator.class.getName();

    public static Map validateAddress(Delegator delegator, PostalAddress address){
        Map response = ServiceUtil.returnSuccess();
        //validate address
        //address1 is mandatory
        if(UtilValidate.isEmpty(address.getAddressLine1())){
            return ServiceUtil.returnError("Address line 1 is required.");
        }

        //city is mandatory
        if(UtilValidate.isEmpty(address.getCity())){
            return ServiceUtil.returnError("A valid city name is required to proceed.");
        }

        //stateCode is mandatory and must be a valid one
        String stateCode = address.getStateCode();
        if(UtilValidate.isEmpty(stateCode)){
            return ServiceUtil.returnError("A valid state code is required to proceed.");
        }else{
            //make sure state code is valid
            try {
                GenericValue stateGeo = EntityQuery.use(delegator).from("Geo")
                        .where("geoTypeId", "STATE", "geoId", stateCode).cache().queryOne();
                if ( UtilValidate.isEmpty(stateGeo) ) {
                    return ServiceUtil.returnError("Invalid state code. (" +stateCode+ ") is not a valid state code, please provide a valid state code.");
                }
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
            }
        }

        //countryCode is mandatory and must be a valid one
        String countryCode = address.getCountryCode();
        if(UtilValidate.isEmpty(countryCode)){
            return ServiceUtil.returnError("A valid country code is required to proceed.");
        }else{
            //make sure country code is valid
            try {
                GenericValue countryGeo = EntityQuery.use(delegator).from("Geo")
                        .where("geoTypeId", "COUNTRY", "geoId", countryCode).cache().queryOne();
                if ( UtilValidate.isEmpty(countryGeo) ) {
                    return ServiceUtil.returnError("Invalid country code. (" +countryCode+ ") is not a valid country code, please provide a valid country code.");
                }
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
            }
        }

        //postalCode is mandatory and must be a valid one
        if(UtilValidate.isEmpty(address.getPostalCode())){
            return ServiceUtil.returnError("A valid postal/zip code is required, please line 1 is required.");
        }

        return response;
    }
}
