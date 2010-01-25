import org.ofbiz.base.util.*;
import java.sql.*;
import com.ibm.icu.util.Calendar;

productStoreId = parameters.productStoreId;
Calendar cal = UtilDateTime.toCalendar(UtilDateTime.nowTimestamp());
int Week = cal.get(Calendar.WEEK_OF_YEAR);
int Year = cal.get(Calendar.YEAR);

birtParameters = [:];
try 
{
	birtParameters.productStoreId = productStoreId;
	birtParameters.Week = Week;
	birtParameters.Year = Year;
} catch (e) {
	Debug.logError(e, "");
}

request.setAttribute("birtParameters", birtParameters);

return "success";
