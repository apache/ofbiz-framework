import java.sql.Timestamp;

import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.service.ServiceUtil;
import org.apache.ofbiz.entity.GenericValue;

public Map deleteRelationship() {
    final String module = "deleteRelationship";
    Map result = ServiceUtil.returnSuccess();

    String subordinatePartyId = parameters.subordinatePartyId;
    String roleTypeIdFrom = parameters.roleTypeIdFrom;
    String roleTypeIdTo = parameters.roleTypeIdTo;
    String partyIdFrom = parameters.partyIdFrom;
    Timestamp partyRelationshipTypeId = parameters.fromDate;

	//find all relationships and delete all
	Map searchCtx = [
		partyIdFrom : partyIdFrom,
		partyIdTo : subordinatePartyId, 
		roleTypeIdFrom : roleTypeIdFrom,
		roleTypeIdTo : roleTypeIdTo ,
		fromDate: fromDate
	];

    GenericValue existingRelationship = delegator.findOne("PartyRelationship", searchCtx, false);

	if(UtilValidate.isNotEmpty(existingRelationship))
	{
		//set thru date
		Timestamp thruDate = UtilDateTime.nowTimestamp();
		existingRelationship.set("thruDate", thruDate);

	    delegator.store(existingRelationship);
	}
    
    return result;
}