import java.util.concurrent.Callable
import org.ofbiz.entity.sql.SQLUtil
import org.ofbiz.entity.transaction.TransactionUtil
response.contentType = 'text/html'
def delegator = request.delegator

def ec1 = SQLUtil.parseCondition("partyId = 'foo' AND partyTypeId = 'PARTY_GROUP'")
response.writer.println("ec1=$ec1<br />")
def ec2 = SQLUtil.parseCondition(ec1.toString())
response.writer.println("ec2=$ec2<br />")
//return

def sql = """
select
    a.partyId,
    a.partyTypeId as type,
	b.firstName,
	b.lastName,
    c.groupName
FROM
	Party a LEFT JOIN Person b USING partyId LEFT JOIN PartyGroup c USING partyId
RELATION TYPE one Party USING partyId
RELATION TYPE one Person USING partyId
RELATION TYPE one PartyGroup USING partyId
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
            response.writer.println("gv=$gv<br />")
            def party = gv.getRelatedOneCache('Party')
            def person = gv.getRelatedOneCache('Person')
            response.writer.println("\tparty=$party<br />")
            response.writer.println("\tperson=$person<br />")
        }
    } finally {
        if (eli != null) eli.close()
    }
}] as Callable)
