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


public class CreateLocationTest extends OFBizTestCase {
    public static final String module = CreateLocationTest.class.getName();


    public CreateLocationTest(String name) {
        super(name);
    }
    public void testCreateLocation() throws Exception {
        doTestCreateLocation("system");
    }

    private void doTestCreateLocation(String userLoginId) throws Exception {
        Debug.logInfo("service is testing: ", module);
        GenericValue userLogin = HierarchyUtils.getUserLogin(delegator, userLoginId);
        Map<String, Object> ctx = new HashMap<String, Object>();
        ctx.put("userLogin", userLogin);
        ctx.put("locationId", "1122");
        ctx.put("locationTypeId", "fax_number");
        ctx.put("name", "abc");
        ctx.put("locale", "a2");
        ctx.put("timeZone", "2-1-2017");
        ctx.put("latitude", "40");
        ctx.put("longitude", "30");
        Map<String, Object> resp = dispatcher.runSync("CreateLocation", ctx);
        assertTrue(ServiceUtil.isSuccess(resp));

    }

}



