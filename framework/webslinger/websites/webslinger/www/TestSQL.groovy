import java.util.concurrent.Callable
import org.ofbiz.entity.sql.SQLUtil
import org.ofbiz.entity.transaction.TransactionUtil
response.contentType = 'text/html'
def delegator = request.delegator

def ec1 = SQLUtil.parseCondition("partyId = 'foo' AND partyTypeId = 'PARTY_GROUP'")
println("ec1=$ec1")
def ec2 = SQLUtil.parseCondition(ec1.toString())
println("ec2=$ec2")
//return

def sql = """
select
    a.partyId,
    a.partyTypeId as type,
	b.firstName,
	b.lastName,
    c.groupName
FROM
	Party a LEFT JOIN Person b ON a.partyId = b.partyId LEFT JOIN PartyGroup c on a.partyId = c.partyId
WHERE
    partyId = 'admin'
ORDER BY
    lastName
;
"""
def sqlSelect = SQLUtil.parseSelect(sql)

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
