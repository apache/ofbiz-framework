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


public class CreateProductStoreContactMechTest extends OFBizTestCase {
    public static final String module = CreateProductStoreContactMechTest.class.getName();


    public CreateProductStoreContactMechTest(String name) {
        super(name);
    }
    public void testCreateProductStoreContactMech() throws Exception {
        doTestCreateProductStoreContactMech("system");
    }

    private void doTestCreateProductStoreContactMech(String userLoginId) throws Exception {
        Debug.logInfo("service is testing: ", module);
        GenericValue userLogin = HierarchyUtils.getUserLogin(delegator, userLoginId);
        Map<String, Object> ctx = new HashMap<String, Object>();
        ctx.put("userLogin", userLogin);
        ctx.put("productStoreId", "1122");
        ctx.put("contactMechPurposeTypeId", "fax_number");
        ctx.put("contactMechId", "10063");
        Map<String, Object> resp = dispatcher.runSync("CreateProductStoreContactMech", ctx);
        assertTrue(ServiceUtil.isSuccess(resp));

    }

  }



