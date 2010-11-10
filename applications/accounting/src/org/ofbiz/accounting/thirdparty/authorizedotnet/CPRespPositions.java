package org.ofbiz.accounting.thirdparty.authorizedotnet;

import java.util.Map;

import javolution.util.FastMap;

public class CPRespPositions extends AuthorizeResponse.RespPositions {
    
    private static Map<String, Integer> positions = FastMap.newInstance();
    static {
        positions.put(AuthorizeResponse.RESPONSE_CODE, 2);
        positions.put(AuthorizeResponse.REASON_CODE, 3);
        positions.put(AuthorizeResponse.REASON_TEXT, 4);
        positions.put(AuthorizeResponse.AUTHORIZATION_CODE, 5);
        positions.put(AuthorizeResponse.AVS_RESULT_CODE, 6);
        positions.put(AuthorizeResponse.CVV_RESULT_CODE, 7);
        positions.put(AuthorizeResponse.TRANSACTION_ID, 8);
        positions.put(AuthorizeResponse.AMOUNT, 25);                
    }
    
    @Override
    public int getPosition(String name) {
        if (positions.containsKey(name)) {
            return positions.get(name);
        } else {
            return -1;
        }
    }
    
    @Override
    public String getApprovalString() {
        return "1";
    }
}
