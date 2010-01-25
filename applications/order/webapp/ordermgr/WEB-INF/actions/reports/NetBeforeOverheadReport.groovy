import org.ofbiz.base.util.*;
import java.sql.*;
import java.sql.Timestamp;
import com.ibm.icu.util.Calendar;

productStoreId = parameters.productStoreId;
DateTime = UtilDateTime.nowTimestamp();
String DateStr = DateTime;
DateDay = DateStr.substring(0,10);
DateMonth = DateStr.substring(5,7);
DateYear = DateStr.substring(0,4);

if (DateMonth == "01"||DateMonth == "03"||DateMonth == "05"||DateMonth == "07"||DateMonth == "08"||DateMonth == "10"||DateMonth == "12")
{
	NunberDate = 31;
}
else if (DateMonth == "02")
{
	NunberDate = 29;
}
else
{
	NunberDate = 30;
}

birtParameters = [:];
try {
	birtParameters.productStoreId = productStoreId;
	birtParameters.DateDay = DateDay;
	birtParameters.DateMonth = DateMonth;
	birtParameters.DateYear = DateYear;
	birtParameters.NunberDate = NunberDate;
} catch (e) {
	Debug.logError(e, "");
}

request.setAttribute("birtParameters", birtParameters);

return "success";
