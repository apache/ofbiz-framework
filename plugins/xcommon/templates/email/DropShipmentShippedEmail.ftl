<table border="0" cellpadding="0" cellspacing="0" width="600" >
	<tr>
		<td width="100%" 
			style="padding: 10px; font-family: Arial, Helvetica, sans-serif; font-weight: normal; font-size: 28px; line-height: 32px; color: #1a1a1a; font-weight: normal;">
			Shipment Shipped Notification !!!</td>
	</tr>
	<tr>
		<td valign="top" id="welcomeText" style="padding: 10px;">
			<p>Items in your order below have shipped</p>
		</td>
	</tr>
	<tr>
		<td style="padding: 10px;"">
			<table cellpadding="2" cellspacing="0" width="100%" border="1"
				style="font-size: 13px;">
				<tr class="heading">
					<td style="font-weight:bold;">Order ID</td>
					<td style="font-weight:bold;">${orderId}</td>
				</tr>
					<td style="font-weight:bold;">Shipment ID</td>
					<td>${shipmentId}</td>
				</tr>
				</tr>
					<td style="font-weight:bold;">Shipping Carrier</td>
					<#if shipment.shipmentCarrierPartyId?exists>
			            <#assign shipmentCarrier = delegator.findOne("PartyGroup",Static["org.ofbiz.base.util.UtilMisc"].toMap("partyId", shipment.shipmentCarrierPartyId), true)?if_exists />
			            <#assign shipmentCarrierName = shipmentCarrier.groupName?if_exists />
		            </#if>
					<td>${shipmentCarrierName?default("Not yet available")}</td>
				</tr>
				</tr>
					<td style="font-weight:bold;">Tracking Code</td>
					<td>${shipment.shipmentTrackingCode?default("Not yet available")}</td>
				</tr>
				</tr>
                    <td style="font-weight:bold;">Solicitation Number</td>
                    <td>${solicitationNumber?default("Not yet available")}</td>
                </tr>
				
			</table>
		</td>
	</tr>
</table> 
