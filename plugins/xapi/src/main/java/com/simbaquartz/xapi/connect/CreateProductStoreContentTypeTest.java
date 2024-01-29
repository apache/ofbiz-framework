package com.simbaquartz.xapi.connect;

import com.fidelissd.zcp.xcommon.util.hierarchy.HierarchyUtils;
import java.util.HashMap;
import java.util.Map;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.service.ServiceUtil;
import org.apache.ofbiz.service.testtools.OFBizTestCase;


public class CreateProductStoreContentTypeTest extends OFBizTestCase {
    private static final String module =CreateProductStoreContentTypeTest.class.getName();

    public CreateProductStoreContentTypeTest(String name) {
        super(name);
    }
    public void testCreateProductStoreContentTypeTest() throws Exception {
        doTestCreateProductStoreContentTypeTest("system");
    }

    private void doTestCreateProductStoreContentTypeTest(String userLoginId) throws Exception {
        Debug.logInfo("service is testing: ", module);
        GenericValue userLogin = HierarchyUtils.getUserLogin(delegator, userLoginId);
        Map<String, Object> ctx = new HashMap<String, Object>();
        ctx.put("userLogin", userLogin);
        ctx.put("productStoreContentTypeId", "1122");
        ctx.put("parentTypeId", "12345");
        ctx.put("hasTable", "A");
        ctx.put("description", "product");
        Map<String, Object> resp = dispatcher.runSync("CreateProductStoreContentType", ctx);
        assertTrue(ServiceUtil.isSuccess(resp));

    }

}


