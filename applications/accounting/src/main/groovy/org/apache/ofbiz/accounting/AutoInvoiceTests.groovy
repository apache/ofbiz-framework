package org.apache.ofbiz.accounting;

import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.entity.util.EntityQuery
import org.apache.ofbiz.service.testtools.OFBizTestCase
import org.apache.ofbiz.accounting.invoice.InvoiceWorker

class AutoInvoiceTests extends OFBizTestCase {
    public AutoInvoiceTests(String name) {
        super(name)
    }
    void testInvoiceWorkerGetInvoiceTotal(){

        String invoiceId="demo10000"
        BigDecimal amount = new BigDecimal('323.54')
        assertInvoiceTotal(invoiceId, amount)

        invoiceId="demo10001"
        amount = new BigDecimal('36.43')
        assertInvoiceTotal(invoiceId, amount)

        invoiceId="demo10002"
        amount = new BigDecimal('56.99')
        assertInvoiceTotal(invoiceId, amount)

        invoiceId="demo11000"
        amount = new BigDecimal('20.00')
        assertInvoiceTotal(invoiceId, amount)

        invoiceId="demo11001"
        amount = new BigDecimal('543.23')
        assertInvoiceTotal(invoiceId, amount)

        invoiceId="demo1200"
        amount = new BigDecimal('511.23')
        assertInvoiceTotal(invoiceId, amount)

        invoiceId="8000"
        amount = new BigDecimal('60.00')
        assertInvoiceTotal(invoiceId, amount)

        invoiceId="8001"
        amount = new BigDecimal('10.00')
        assertInvoiceTotal(invoiceId, amount)

        invoiceId="8002"
        amount = new BigDecimal('36.43')
        assertInvoiceTotal(invoiceId, amount)

        invoiceId="8003"
        amount = new BigDecimal('46.43')
        assertInvoiceTotal(invoiceId, amount)

        invoiceId="8004"
        amount = new BigDecimal('33.99')
        assertInvoiceTotal(invoiceId, amount)

        invoiceId="8100"
        amount = new BigDecimal('1320.00')
        assertInvoiceTotal(invoiceId, amount)

        invoiceId="8005"
        amount = new BigDecimal('33.99')
        assertInvoiceTotal(invoiceId, amount)

        invoiceId="8006"
        amount = new BigDecimal('46.43')
        assertInvoiceTotal(invoiceId, amount)

        invoiceId="8007"
        amount = new BigDecimal('36.43')
        assertInvoiceTotal(invoiceId, amount)

        invoiceId="8008"
        amount = new BigDecimal('48.00')
        assertInvoiceTotal(invoiceId, amount)

        invoiceId="8009"
        amount = new BigDecimal('127.09')
        assertInvoiceTotal(invoiceId, amount)

        invoiceId="8010"
        amount = new BigDecimal('179.97')
        assertInvoiceTotal(invoiceId, amount)
    }

    void assertInvoiceTotal(String invoiceId, BigDecimal amount){
        GenericValue invoice =  EntityQuery.use(delegator).from('Invoice').where('invoiceId', invoiceId).queryOne()
        BigDecimal invoiceTotal = InvoiceWorker.getInvoiceTotal(invoice)
        assert  invoiceTotal == amount
    }
}