

import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.condition.EntityExpr;
import org.apache.ofbiz.entity.condition.EntityConditionList;
import org.apache.ofbiz.service.ServiceUtil;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.collections.ResourceBundleMapWrapper;
import com.fidelissd.hierarchy.HierarchyUtils;

public Map createSupplierRelationship() {
    final String module = "createSupplierRelationship";
    Map result = ServiceUtil.returnSuccess();

    String supplierPartyId = parameters.supplierPartyId;

    result = runService("createPartyRelationship", [partyIdTo: supplierPartyId, roleTypeIdTo: "SUPPLIER", partyRelationshipTypeId: "SUPPLIER_REL", partyIdFrom: "FSD", roleTypeIdFrom: "DISTRIBUTOR"]);
    return result;
}
