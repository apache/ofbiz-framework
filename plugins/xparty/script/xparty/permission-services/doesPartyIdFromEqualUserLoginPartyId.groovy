public Map doesPartyIdFromEqualUserLoginPartyIdPermissionCheck()
{
    final String module = "doesPartyIdFromEqualUserLoginPartyIdPermissionCheck";

	Map result = runService("doesPartyIdEqualUserLoginPartyIdPermissionCheck", [partyId: parameters.partyIdFrom]);
    
    return result;
}