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


public class CreateLocationTypeTest extends OFBizTestCase {
    public static final String module = CreateLocationTypeTest.class.getName();


    public CreateLocationTypeTest(String name) {
        super(name);
    }
    public void testCreateLocationTypeTest() throws Exception {
        doTestCreateLocationTypeTest("system");
    }

    private void doTestCreateLocationTypeTest(String userLoginId) throws Exception {
        Debug.logInfo("service is testing: ", module);
        GenericValue userLogin = HierarchyUtils.getUserLogin(delegator, userLoginId);
        Map<String, Object> ctx = new HashMap<String, Object>();
        ctx.put("userLogin", userLogin);
        ctx.put("locationTypeId", "1122");
        ctx.put("parentTypeId", "1122");
        ctx.put("description", "abc");
        Map<String, Object> resp = dispatcher.runSync("CreateLocationType", ctx);
        assertTrue(ServiceUtil.isSuccess(resp));

    }

}



