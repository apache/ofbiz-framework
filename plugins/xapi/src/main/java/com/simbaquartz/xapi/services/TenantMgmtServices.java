package com.simbaquartz.xapi.services;

import com.fidelissd.zcp.xcommon.collections.FastList;
import org.apache.commons.lang.SystemUtils;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;

import static com.simbaquartz.xapi.connect.api.BaseApiService.delegator;


public class TenantMgmtServices {
    private static final String module = TenantMgmtServices.class.getName();


    /***
     * Ofbiz service to load seed data for given tenant
     * This is designed to be invoked as Async to avoid waiting for command to complete
     * @param dctx
     * @param context
     * @return
     */
    public static Map<String, Object> loadSeedDataForTenant(DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
        String tenantId = (String) context.get("tenantId");
        Debug.logInfo("Running ant command to load seed data for tenant: " + tenantId, module);
        try {
            runLoadSeedAntCommand(tenantId, "seed-initial");
            runLoadSeedAntCommand(tenantId, "seed");
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return ServiceUtil.returnFailure("Unable to load seed data for tenant "+ tenantId);
        }
        Debug.log("Loading seed data has been completed for tenant: "+ tenantId, module);
        return serviceResult;
    }

    /***
     * Ofbiz service to load seed data for all tenants
     * This is designed to be invoked as Async to avoid waiting for command to complete
     * @param dctx
     * @param context
     * @return
     */
    public static Map<String, Object> loadSeedDataOnAllTenants(DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        // Get all tenants, and initiate loading seed data
        try {
            List<GenericValue> tenants = delegator.findAll("Tenant", false);
            if(UtilValidate.isNotEmpty(tenants)) {
                for(GenericValue tenant: tenants) {
                    dispatcher.runAsync("loadSeedDataForTenant", UtilMisc.toMap("tenantId", tenant.getString("tenantId"),
                            "userLogin", userLogin), false);
                }
            }
        } catch (GenericEntityException | GenericServiceException e) {
            Debug.logError("Error loading seed-data for all tenants, error:" + e.getMessage(), module);
            e.printStackTrace();
        }
        return serviceResult;
    }

    private static void runLoadSeedAntCommand(String tenantId, String dataReader) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder();
        List<String> commands = FastList.newInstance();
        if (SystemUtils.IS_OS_WINDOWS) {
            commands.add("ant.bat");
        } else {
            commands.add("sh");
            commands.add("ant");
        }
        commands.add("load-tenant-reader");
        commands.add("-DtenantId=" + tenantId);
        commands.add("-Ddata-readers=" + dataReader);
        pb.redirectOutput(new File("runtime/logs/load-"+dataReader+"-"+tenantId+".log"));

        Debug.log(tenantId + ": seed-data load command: " + commands, module);
        pb.command(commands);
        Process p = pb.start();

        BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String s;
        while ((s = reader.readLine()) != null) {
            Debug.logInfo(">>>" + s, module);
        }
        p.waitFor();
    }
}
