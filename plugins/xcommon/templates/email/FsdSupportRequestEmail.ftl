<table border="0" cellpadding="0" cellspacing="0" width="100%">
	<tr>
		<td align="left"
			style="background: #fafafa; border-bottom: 1px solid #f4f3f3;"
			colspan="2">
			<!-- primary module -->
			<table border="0" cellpadding="0" cellspacing="0" width="90%">
				<tr>
					<td align="center"
						style="font: 25px arial, verdana, sans-serif; color: #333333; padding: 20px 15px 0px 15px; background-color: #fafafa;">
						Request Submittion Notification !!!</td>
				</tr>
				<tr>
					<td align="center"
						style="font: 12px arial, verdana, sans-serif; padding: 10px 45px 13px 45px; line-height: 20px;">
						A support request has been submitted. See below your request details : 
				</tr>

			</table> <!-- end primary module -->

		</td>
	</tr>
	<tr>
		<td style="vertical-align: top; background: #fafafa; border-bottom: 1px solid #f4f3f3;font: bold 15px arial, verdana, sans-serif; color: #333333; padding: 20px 15px 0px 15px; background-color: #fafafa;"
			width="30%">
			Requested By :		
		</td>
		<td style="vertical-align: top; background: #fafafa; border-bottom: 1px solid #f4f3f3; border-left: 1px dashed #C0C0C0;color: #333333; padding: 20px 15px 0px 15px; background-color: #fafafa;"
			width="70%">
			${requestorName?if_exists}
		</td>
	</tr>
	<tr>
		<td style="vertical-align: top; background: #fafafa; border-bottom: 1px solid #f4f3f3;font: bold 15px arial, verdana, sans-serif; color: #333333; padding: 20px 15px 0px 15px; background-color: #fafafa;"
			width="30%">
			Subject :		
		</td>
		<td style="vertical-align: top; background: #fafafa; border-bottom: 1px solid #f4f3f3; border-left: 1px dashed #C0C0C0;color: #333333; padding: 20px 15px 0px 15px; background-color: #fafafa;"
			width="70%">
			${requestSubject?if_exists}
		</td>
	</tr>
	<tr>
		<td style="vertical-align: top; background: #fafafa; border-bottom: 1px solid #f4f3f3;font: bold 15px arial, verdana, sans-serif; color: #333333; padding: 20px 15px 0px 15px; background-color: #fafafa;"
			width="30%">
			Details :		
		</td>
		<td style="vertical-align: top; background: #fafafa; border-bottom: 1px solid #f4f3f3; border-left: 1px dashed #C0C0C0;color: #333333; padding: 20px 15px 0px 15px; background-color: #fafafa;"
			width="70%">
			${requestDetails?if_exists}
		</td>
	</tr>
</table> <!-- primary module wrapper -->
