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


public class CreateProductStoreContentTest extends OFBizTestCase {
    public static final String module =CreateProductStoreContentTest.class.getName();

    public CreateProductStoreContentTest(String name) {
        super(name);
    }
    public void testCreateProductStoreContent() throws Exception {
        doTestCreateProductStoreContent("system");
    }

    private void doTestCreateProductStoreContent(String userLoginId) throws Exception {
        Debug.logInfo("service is testing: ", module);
        GenericValue userLogin = HierarchyUtils.getUserLogin(delegator, userLoginId);
        Map<String, Object> ctx = new HashMap<String, Object>();
        ctx.put("userLogin", userLogin);
        ctx.put("productStoreId", "1122");
        ctx.put("contentId", "12345");
        ctx.put("productStoreContentTypeId", "fax_number");
        ctx.put("fromDate", "1-1-2017");
        ctx.put("thruDate", "2-2-2017");
        Map<String, Object> resp = dispatcher.runSync("CreateProductStoreContent", ctx);
        assertTrue(ServiceUtil.isSuccess(resp));

    }

}


