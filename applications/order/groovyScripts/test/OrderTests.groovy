import org.apache.ofbiz.entity.util.EntityQuery
import org.apache.ofbiz.service.ServiceUtil
import org.apache.ofbiz.testtools.GroovyScriptTestCase

class OrderTests extends GroovyScriptTestCase {
    void testAddRequirementTask() {
        Map serviceCtx = [
            requirementId: '1000',
            workEffortId: '9000',
            userLogin: EntityQuery.use(delegator).from('UserLogin').where('userLoginId', 'system').cache().queryOne()
        ]
        Map serviceResult = dispatcher.runSync("addRequirementTask", serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
    }
    void testCreateReturnAdjustment() {
        Map serviceCtx = [
            amount: '2.0000',
            returnId: '1009',
            userLogin: EntityQuery.use(delegator).from('UserLogin').where('userLoginId', 'system').cache().queryOne()
        ]
        Map serviceResult = dispatcher.runSync('createReturnAdjustment', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        assert serviceResult.returnAdjustmentId != null
    }
    void testQuickReturnOrder() {
        Map serviceCtx = [
            orderId: 'TEST_DEMO10090',
            returnHeaderTypeId: 'CUSTOMER_RETURN',
            userLogin: EntityQuery.use(delegator).from('UserLogin').where('userLoginId', 'system').cache().queryOne()
        ]
        Map serviceResult = dispatcher.runSync('quickReturnOrder', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        assert serviceResult.returnId != null
    }
    void testCreateReturnAndItemOrAdjustment() {
        Map serviceCtx = [
            orderId: 'DEMO10090',
            returnId: '1009',
            userLogin: EntityQuery.use(delegator).from('UserLogin').where('userLoginId', 'system').cache().queryOne()
        ]
        Map serviceResult = dispatcher.runSync('createReturnAndItemOrAdjustment', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        assert serviceResult.returnAdjustmentId != null
    }
    void testCheckReturnComplete() {
        Map serviceCtx = [
            amount: '2.0000',
            returnId: '1009',
            userLogin: EntityQuery.use(delegator).from('UserLogin').where('userLoginId', 'system').cache().queryOne()
        ]
        Map serviceResult = dispatcher.runSync('checkReturnComplete', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        assert serviceResult.statusId != null
    }
    void testCheckPaymentAmountForRefund() {
        Map serviceCtx = [
            returnId: '1009',
            userLogin: EntityQuery.use(delegator).from('UserLogin').where('userLoginId', 'system').cache().queryOne()
        ]
        Map serviceResult = dispatcher.runSync('checkPaymentAmountForRefund', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
    }
    //TODO: This can be moved to a different file
    void testCheckCreateProductRequirementForFacility() {
        Map serviceCtx = [
                facilityId: 'WebStoreWarehouse',
                orderItemSeqId: '00001',
                userLogin: EntityQuery.use(delegator).from('UserLogin').where('userLoginId', 'system').cache().queryOne()
        ]
        Map serviceResult = dispatcher.runSync('checkCreateProductRequirementForFacility', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
    }
}