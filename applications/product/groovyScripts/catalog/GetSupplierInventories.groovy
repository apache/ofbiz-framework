import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;

exprList = [EntityCondition.makeCondition("availableToPromiseTotal", EntityOperator.GREATER_THAN, BigDecimal.ZERO),
            EntityCondition.makeCondition("partyId", EntityOperator.EQUALS, parameters.partyId)];
context.andCondition = EntityCondition.makeCondition(exprList, EntityOperator.AND);
 