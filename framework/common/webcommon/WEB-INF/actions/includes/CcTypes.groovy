import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;

context.creditCardTypes = delegator.findList("Enumeration", EntityCondition.makeCondition("enumTypeId", EntityOperator.EQUALS, "CREDIT_CARD_TYPE"), 
        ["enumId", "enumCode"] as Set, null, null, false);