import org.ofbiz.entity.*;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.base.util.*;
import org.ofbiz.base.util.string.*;
import org.ofbiz.party.contact.ContactHelper;
import org.ofbiz.common.geo.*;

if (partyId) {
    context.partyId = partyId;
    latestGeoPoint = GeoWorker.findLatestGeoPoint(delegator, "PartyAndGeoPoint", "partyId", partyId, null, null);
    if (latestGeoPoint) {
        context.geoPointId = latestGeoPoint.geoPointId;
        context.latitude = latestGeoPoint.latitude;
        context.longitude = latestGeoPoint.longitude;
    } else {
        context.latitude = 0;
        context.longitude = 0;
    }
}
