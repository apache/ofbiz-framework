import java.util.concurrent.Callable
import org.ofbiz.entity.sql.Parser
import org.ofbiz.entity.transaction.TransactionUtil
response.contentType = 'text/html'
def delegator = request.delegator
def sql = """
select
	a.*,
	b.firstName,
	b.lastName
FROM
	Party a JOIN Person b ON a.partyId = b.partyId
WHERE
	a.partyId='admin'
OFFSET 5
LIMIT 10
;
"""
def parser = new Parser(new ByteArrayInputStream(sql.bytes))
def sqlSelect = parser.Select()

TransactionUtil.doNewTransaction("Test", [call: {
    def eli
    try {
        eli = sqlSelect.getEntityListIterator(delegator)
        def gv;
        while ((gv = eli.next()) != null) {
            println("gv=$gv")
        }
    } finally {
        if (eli != null) eli.close()
    }
}] as Callable)
