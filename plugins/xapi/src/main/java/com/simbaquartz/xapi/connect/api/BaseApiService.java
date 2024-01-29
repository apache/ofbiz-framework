package com.simbaquartz.xapi.connect.api;

import com.fidelissd.zcp.xcommon.util.hierarchy.HierarchyUtils;
import org.apache.ofbiz.entity.DelegatorFactory;
import org.apache.ofbiz.entity.GenericDelegator;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.service.GenericDispatcherFactory;
import org.apache.ofbiz.service.LocalDispatcher;

public interface BaseApiService {
	GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
    LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default",delegator);
    GenericValue sysUserLogin = HierarchyUtils.getSysUserLogin(delegator);
}
