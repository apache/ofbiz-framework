package com.simbaquartz.xapi.connect;

import com.fidelissd.zcp.xcommon.util.hierarchy.HierarchyUtils;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.service.ServiceUtil;
import org.apache.ofbiz.service.testtools.OFBizTestCase;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import java.util.HashMap;
import java.util.Map;


public class CreateLocationContactMechTest extends OFBizTestCase {
    public static final String module = CreateLocationContactMechTest.class.getName();


    public CreateLocationContactMechTest(String name) {
        super(name);
    }
    public void testCreateLocationContactMech() throws Exception {
        doTestCreateLocationContactMech("system");
    }

    private void doTestCreateLocationContactMech(String userLoginId) throws Exception {
        Debug.logInfo("service is testing: ", module);
        GenericValue userLogin = HierarchyUtils.getUserLogin(delegator, userLoginId);
        Map<String, Object> ctx = new HashMap<String, Object>();
        ctx.put("userLogin", userLogin);
        ctx.put("locationId", "1122");
        ctx.put("contactMechPurposeTypeId", "fax_number");
        ctx.put("contactMechId", "1234");
        Map<String, Object> resp = dispatcher.runSync("CreateLocationContactMech", ctx);
        assertTrue(ServiceUtil.isSuccess(resp));

    }

}



