import com.fidelissd.fsdOrderManager.orderentity.role.OrderEntityRole
import com.fidelissd.fsdParty.party.FsdPartyHelper
import com.fidelissd.hierarchy.HierarchyUtils
import com.simbaquartz.util.common.CommonMethods
import com.simbaquartz.xcommon.collections.FastList
import org.apache.ofbiz.base.util.UtilMisc
import org.apache.ofbiz.base.util.UtilProperties
import org.apache.ofbiz.base.util.UtilValidate
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.entity.util.EntityUtil
import org.apache.ofbiz.party.contact.ContactHelper
import org.apache.ofbiz.product.store.ProductStoreWorker
import org.apache.ofbiz.service.ServiceUtil

String userId = parameters.userId
String partyId = parameters.partyId
String threadId = parameters.threadId
String messageId = parameters.messageId

context.put("messageId",messageId);
context.put("threadId", threadId);
context.put("partyId", partyId);

productStoreId = ProductStoreWorker.getProductStoreId(request);
context.productStoreId = productStoreId;

Map syncGmailConversationCtx = [
        userId   : userId,
        partyId  : partyId,
        threadId : threadId,
        userLogin: userLogin
]

// 1. First sync the thread, so that we have up-to-date info
Map syncGmailConversationCtxResp = dispatcher.runSync("syncEmailMessageConversationThread", syncGmailConversationCtx)

if (!ServiceUtil.isSuccess(syncGmailConversationCtxResp)) {
    request.setAttribute("_ERROR_MESSAGE_", ServiceUtil.getErrorMessage(syncGmailConversationCtxResp))
    return "error"
}

// 2. Find CommEvent for message (messageId  = mailboxMessageId)
List<GenericValue> messageCommEvents = from("CommunicationEvent").where(["mailboxMessageId": messageId]).maxRows(5).queryList()
if(!UtilValidate.isNotEmpty(messageCommEvents)) {
    request.setAttribute("_ERROR_MESSAGE_", "Unable to find Comm Event for given message Id: " + messageId);
    return "error"
}

GenericValue commEvent = messageCommEvents.get(0);
context.commEvent = commEvent;

// 3. Prepare Task Details to be created - for review
List<GenericValue> commEventRoles = commEvent.getRelated("CommunicationEventRole", null, null, false);
context.commEventRoles = commEventRoles;

List taskCustomerPocs = FastList.newInstance()
List taskSupplierPocs = FastList.newInstance()
List taskCompanyPocs = FastList.newInstance()
List taskInternalPocs = FastList.newInstance()

for(GenericValue roleGv: commEventRoles) {
    String rolePartyId = roleGv.partyId;
    roleParty = from("Party").where(partyId : rolePartyId).queryOne();

    GenericValue draftTaskRoleGv = delegator.makeValidValue("TaskRole", UtilMisc.toMap("partyId", rolePartyId, "roleTypeId", roleGv.roleTypeId, "taskId", "DRAFT"))
    OrderEntityRole orderEntityRole = new OrderEntityRole(draftTaskRoleGv)
    if(UtilValidate.isNotEmpty(orderEntityRole.getPhoneNumberMap())) {
        CommonMethods.getPhoneInFormat(orderEntityRole)
    }

    boolean isSupplierPoc = HierarchyUtils.checkPartyRole(roleParty, "SUPPLIER_POC");
    boolean isCustomerPoc = HierarchyUtils.checkPartyRole(roleParty, "CONTRACTING_OFFICER");
    boolean isCompanyPoc = HierarchyUtils.checkPartyRole(roleParty, "ORDER_CLERK");

    if(isSupplierPoc) {
        taskSupplierPocs.add(orderEntityRole);
    } else if (isCustomerPoc) {
        taskCustomerPocs.add(orderEntityRole);
    } else if(isCompanyPoc) {
        taskCompanyPocs.add(orderEntityRole);
    } else {
        // Default everyone else as Internal-POC
        taskInternalPocs.add(orderEntityRole);
    }
}

context.taskCustomerPocs = taskCustomerPocs;
context.taskSupplierPocs = taskSupplierPocs;
context.taskCompanyPocs = taskCompanyPocs;
context.taskInternalPocs = taskInternalPocs;


List<GenericValue> commEventContents = commEvent.getRelated("CommEventContentAssoc", null, null, false);
context.commEventContents = commEventContents;

// Load other details
String companyPartyId = UtilProperties.getPropertyValue("general", "ORGANIZATION_PARTY");
Map<String, Object> employeePersonsServiceResponse = dispatcher.runSync("getEmployeePersons", [partyGroupPartyId : companyPartyId, userLogin : userLogin]);
List<GenericValue> personsList = null;
List fsdEmployees = [];
if( ServiceUtil.isSuccess(employeePersonsServiceResponse) ) {
    personsList = employeePersonsServiceResponse.employeePersonsList;

    personsList.each { GenericValue person ->
        GenericValue personParty = person.getRelatedOne("Party", false);
        displayName = FsdPartyHelper.getPartyName(delegator, person.partyId);
        GenericValue partyEmail = EntityUtil.getFirst(ContactHelper.getContactMechByType(personParty, "EMAIL_ADDRESS", false));

        String email = "";
        if(UtilValidate.isNotEmpty(partyEmail)){
            email = partyEmail.getString("infoString");
        }

        Map employeeMap = [
                partyId : person.partyId,
                displayName : displayName,
                email : email
        ];

        fsdEmployees.add(employeeMap);
    }
}

context.fsdEmployees = fsdEmployees;

return "success"
