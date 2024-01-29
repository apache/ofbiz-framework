package com.simbaquartz.xparty.services;

import org.apache.cxf.service.Service;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public class StudentServices {
    private static final String module = StudentServices.class.getName();

    /**
     * Create student service to store student name and mobile-number
     *
     * @param dctx
     * @param context
     * @return
     */
    public static Map<String, Object> createStudent(DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
        Delegator delegator = dctx.getDelegator();
        try {
            String name = (String) context.get("name");
            String mobileNumber = (String) context.get("mobileNumber");
            String id = delegator.getNextSeqId("Student");
            GenericValue newRecord = delegator.makeValue("Student");
            newRecord.set("id", id);
            newRecord.set("name", name);
            newRecord.set("mobileNumber", mobileNumber);

            delegator.create(newRecord);
            serviceResult.put("id", id);
        } catch (GenericEntityException e) {
            Debug.logError(e, "Exception occured while calling student service" + e.getMessage(), module);
            return ServiceUtil.returnError(e.getMessage());
        }

        return serviceResult;

    }

    /**
     * Get value from student entity
     * @param dctx
     * @param context
     * @return
     */
    public static Map<String, Object> getStudent(DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
        Delegator delegator = dctx.getDelegator();
        String id = (String) context.get("id");
        Map<String, Object> student = new HashMap<>();
        GenericValue studentRecord;
        try {
            studentRecord = EntityQuery.use(delegator).from("Student").where("id", id).queryOne();
        } catch (GenericEntityException e) {
            String errorMessage = "An error occured while fetching student details";
            Debug.logError(e, errorMessage, module);
            return ServiceUtil.returnError(e.getMessage());
        }
        if (UtilValidate.isNotEmpty(studentRecord)) {
            student.put("id", studentRecord.getString("id"));
            student.put("name", studentRecord.getString("name"));
            student.put("mobileNumber", studentRecord.getString("mobileNumber"));
        }
        serviceResult.put("studentDetail", student);
        return serviceResult;
    }

    /**
     * Update student record to store new student name and mobile-number
     * @param dctx
     * @param context
     * @return
     * @throws Exception
     */
    public static Map<String, Object> updateStudent(DispatchContext dctx, Map<String, Object> context) throws Exception {
        Map<String, Object> result = ServiceUtil.returnSuccess();
        Delegator delegator = dctx.getDelegator();
        String id = (String) context.get("id");
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String name = (String) context.get("name");
        String mobileNumber = (String) context.get("mobileNumber");
        Boolean isUpdated = false;

        GenericValue studentRecord = EntityQuery.use(delegator).from("Student").where("id", id).queryFirst();
        if (UtilValidate.isNotEmpty(studentRecord)) {
            if (UtilValidate.isNotEmpty(name)) {
                isUpdated = true;
                studentRecord.set("name", name);
            }
            if (UtilValidate.isNotEmpty(mobileNumber)) {
                isUpdated = true;
                studentRecord.set("mobileNumber", mobileNumber);
            }
            if (isUpdated)
                studentRecord.store();
        }
        result.put("studentDetail", studentRecord);
        return result;
    }

    /**
     * Delete value from student entity using student id
     * @param dctx
     * @param context
     * @return
     * @throws Exception
     */
    public static Map<String, Object> deleteStudent(DispatchContext dctx, Map<String, Object> context) throws Exception {
        Map<String, Object> result = ServiceUtil.returnSuccess();
        Delegator delegator = dctx.getDelegator();
        String id = (String) context.get("id");

        GenericValue contractSubTypeGv = EntityQuery.use(delegator).from("Student").where("id", id).queryOne();
        if (UtilValidate.isNotEmpty(contractSubTypeGv)) {

            delegator.removeValue(contractSubTypeGv);
        }
        return result;

    }

    /**
     * Create invoiceFlag to demonstrate foreign key concepts
     * @param dctx
     * @param context
     * @return
     * @throws GenericEntityException
     */
    public static Map<String, Object> createInvoiceFlag(DispatchContext dctx, Map<String, Object> context) throws GenericEntityException {
            Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
            Delegator delegator = dctx.getDelegator();

            String invoiceId = (String) context.get("invoiceId");
            BigDecimal amount = (BigDecimal) context.get("amount");
            Boolean isFullyPaidAfterPayment = (Boolean) context.get("isFullyPaidAfterPayment");
            GenericValue newRecord = delegator.makeValue("InvoiceFlag");
            newRecord.set("invoiceId", invoiceId);
            newRecord.set("amount", amount);

        if (UtilValidate.isNotEmpty(isFullyPaidAfterPayment)) {
            if (isFullyPaidAfterPayment)
                newRecord.set("isFullyPaidAfterPayment", "Y");
            else
                newRecord.set("isFullyPaidAfterPayment", "N");
        }
        delegator.create(newRecord);
        return serviceResult;
    }

    public static Map<String, Object> createStudentNote(DispatchContext dctx, Map<String, Object> context) throws GenericServiceException, GenericEntityException {
        Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();

        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String noteInfo = (String) context.get("noteInfo");
        String studentId = (String) context.get("studentId");
        //creating quote attribute
        Map<String, Object> createStudentNote = UtilMisc.toMap(
                "userLogin", userLogin,
                "note",noteInfo);

        Map createNoteResp = dispatcher.runSync("createNote", createStudentNote);

        if( !ServiceUtil.isSuccess(createNoteResp) ) {
            String errorMessage = "An error occured while fetching student notes";
            Debug.logError(ServiceUtil.getErrorMessage(createNoteResp), errorMessage, module);
            return ServiceUtil.returnError(ServiceUtil.getErrorMessage(createNoteResp));
        }
        if(UtilValidate.isNotEmpty(createNoteResp)){
            String noteId = (String) createNoteResp.get("noteId");
            GenericValue newRecord = delegator.makeValue("StudentNote");
            newRecord.set("studentId", studentId);
            newRecord.set("noteId", noteId);
            newRecord.set("fromDate", UtilDateTime.nowTimestamp());

            delegator.create(newRecord);


        }
        return serviceResult;

    }

}

