package com.simbaquartz.xcommon.models.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import com.simbaquartz.xcommon.models.search.BeanSearchCriteria;
import javax.ws.rs.QueryParam;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * For searching invitations sent out.
 */
@Data
@EqualsAndHashCode(callSuper=false)
@JsonIgnoreProperties(ignoreUnknown = true)
public class InvitationSearchCriteria extends BeanSearchCriteria {
    /**
     * Owner id of the invitation, inviter's id
     */
    @QueryParam("invitedBy")
    private String invitedById;

    /**
     * The organization id the invitee is requested to join
     */
    @QueryParam("invitedToOrg")
    private String invitedToOrgId;

    /**
     * The team id invitee is requested to join
     */
    @QueryParam("invitedToTeam")
    private String invitedToTeamId;

    /**
     * Name of the person invited
     */
    @QueryParam("inviteeName")
    private String inviteeName;

    /**
     * Email address where the invitation was sent.
     */
    @QueryParam("inviteeEmail")
    private String inviteeEmail;

    /**
     * Status of the invitation one of [accepted, pending, any]
     */
    @QueryParam("status")
    private String status = "pending";
    /**
     * Sort field by
     */
    @QueryParam("sortBy")
    private String sortBy = "createdDate";
}
