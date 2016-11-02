partyIdFrom = parameters.partyIdFrom
partyIdTo = parameters.partyIdTo
if (partyIdFrom) {
    context.partyNameResultFrom = runService("getPartyNameForDate", [partyId: partyIdFrom, compareDate: agreementDate, lastNameFirst: "Y"])
}
if (partyIdTo) {
    context.partyNameResultTo = runService("getPartyNameForDate", [partyId: partyIdTo, compareDate: agreementDate, lastNameFirst: "Y"])
}