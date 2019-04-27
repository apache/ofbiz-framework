import org.apache.ofbiz.base.util.UtilDateTime
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.entity.util.EntityQuery
import org.apache.ofbiz.service.ServiceUtil
import org.apache.ofbiz.testtools.GroovyScriptTestCase
class AutoAcctgInvoiceTests extends GroovyScriptTestCase {
    void testCreateInvoiceContent() {
        Map serviceCtx = [:]
        serviceCtx.invoiceId = '1008'
        serviceCtx.contentId = '1000'
        serviceCtx.invoiceContentTypeId = 'COMMENTS'
        serviceCtx.fromDate = UtilDateTime.nowTimestamp()
        serviceCtx.userLogin = EntityQuery.use(delegator).from('UserLogin').where('userLoginId', 'system').cache().queryOne()
        Map serviceResult = dispatcher.runSync('createInvoiceContent', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        GenericValue InvoiceContent = EntityQuery.use(delegator).from('InvoiceContent').where('invoiceId', serviceResult.invoiceId, 'contentId', serviceResult.contentId, 'invoiceContentTypeId', serviceResult.invoiceContentTypeId, 'fromDate',UtilDateTime.nowTimestamp()).queryList()
        assert invoiceContent.contentId == serviceResult.contentId
    }
}
