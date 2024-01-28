
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.ofbiz.base.util.UtilMisc

String serverRootUrl = parameters._SERVER_ROOT_URL_
mapper = new ObjectMapper();

String partyId = userLogin.partyId
Map<String, Object> resp = dispatcher.runSync("initializeGoogleAuth",
        UtilMisc.toMap("userLogin", userLogin, "partyId", partyId, "serverRootUrl",serverRootUrl ));

System.out.println("oauthUrl: " + resp.get("oauthUrl"));

def output = [:]
output.put("oauthUrl", resp.get("oauthUrl"));
mapper.writeValue(response.getWriter(), output );

//context.oauthUrl = resp.get("oauthUrl");