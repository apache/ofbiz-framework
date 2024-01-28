import com.fasterxml.jackson.databind.ObjectMapper

def testS3ConnectivityContext = [userLogin: userLogin]
Map serviceResult = run service: "testS3Connectivity", with: testS3ConnectivityContext
mapper = new ObjectMapper()

mapper.writeValue(response.getWriter(), serviceResult)
return "success"