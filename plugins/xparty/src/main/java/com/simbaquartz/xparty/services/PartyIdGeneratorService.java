package com.simbaquartz.xparty.services;

import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;

import java.util.Map;
import java.util.Random;

/**
 * PartyHelper
 */
public class PartyIdGeneratorService
{
    public static final String module = PartyIdGeneratorService.class.getName();

    /**
     * Generate a unique partyId based on given firstname and last name
     *
     * @param dctx    The DispatchContext that this service is operating in.
     * @param context Map containing the input parameters.
     * @return Map with the result of the service, the output parameters.
     */

    public static Map<String, Object> generatePartyIdFromName(DispatchContext dctx, Map<String, ? extends Object> context)
    {
        Map<String, Object> result = ServiceUtil.returnSuccess();
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String firstName = (String) context.get("firstName");
        String middleName = (String) context.get("middleName");
        String lastName = (String) context.get("lastName");
        StringBuilder partyIdBuffer = new StringBuilder();
        // Ensure partyId isn't longer than 20 characters

        partyIdBuffer.append(getFirstNCharacters(firstName, 9)).append(".");
        partyIdBuffer.append(getFirstNCharacters(lastName, 10));
        String partyId = partyIdBuffer.toString();
        partyId = partyId.toLowerCase();

        int num = 1;
        int threshold = 100;
        if(!isPartyIdUnique(delegator, partyId)) {
            String partyIdTemp = partyId;
            while(!isPartyIdUnique(delegator, partyIdTemp)) {
                if(partyId.length()+Integer.valueOf(num).toString().length()>20)
                {
                    int numLength = Integer.valueOf(num).toString().length();
                    int idx = 20 - numLength;
                    partyIdTemp = getFirstNCharacters(partyId, idx) + num;
                }
                else
                {
                    partyIdTemp = partyId + num;
                }

                num += 1;
                if(num > threshold) {
                    // If crossed threshold, append a random 5 digit number to avoid infinite loop
                    partyIdTemp = getFirstNCharacters(partyId, 15) + generateFiveDigitRandomNumber();
                    break;
                }
            }
            partyId = partyIdTemp;
        }

        result.put("partyId", partyId);
        return result;
    }

    private static boolean isPartyIdUnique(Delegator delegator, String partyId) {
        try {
            GenericValue party = delegator.findOne("Party", UtilMisc.toMap("partyId", partyId), false);
            if(party!=null) return false;
        } catch (GenericEntityException e) {
            e.printStackTrace();
        }
        return true;
    }

    public static int generateFiveDigitRandomNumber() {
        Random r = new Random( System.currentTimeMillis() );
        return 10000 + r.nextInt(20000);
    }

    private static String getFirstNCharacters(String str, int n) {
        String nCharsStr = "";
        if(str!=null && !str.isEmpty()) {
            if(str.length()>n) {
                nCharsStr = str.substring(0, n);
            } else nCharsStr = str;
        }
        return nCharsStr;
    }
}
