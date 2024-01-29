package com.simbaquartz.xapi.connect.api.common;

/** Created by Admin on 7/18/17. */
public class ApiMessageConstants {
  public static final String MSG_INVALID_STORE_ID = "Invalid Store Id";
  public static final String MSG_INVALID_COMPANY_ID = "Invalid Company Id";
  public static final String MSG_ENTER_REQUIRED_DETAILS = "Please provide the Customer Name.";
  public static final String MSG_INVALID_EMAIL_FORMAT =
      "Invalid email format, please correct email format and try again, details: ";
  public static final String MSG_MISSING_EMAIL_ID =
      "Invalid email, we couldn't find an email with the input id: ";
  public static final String MSG_ORG_ALREADY_EXIST =
      "An organization is already registered with this DB ";
  public static final String MSG_INVALID_PHONE_AREA =
      "Invalid or empty area code, please provide a valid 3 digit number and try again, details: ";
  public static final String MSG_INVALID_PHONE_NUMBER =
      "Invalid or empty phone number, please provide a valid 7 digit number and try again, details: ";
  public static final String MSG_ERROR_CREATING_NEW_CUSTOMER_ADDRESS =
      "Error creating New Customer Address.";
  public static final String MSG_CONTACTMECH_DOES_NOT_EXISTS = "Invalid Contact Mech Id, details: ";
  public static final String MSG_ADDRESS_DELETED = "Address deleted successfully.";
  public static final String MSG_INVALID_CUSTOMER_ROLE =
      "Party does not have Customer Role, details: ";
  public static final String MSG_INTERNAL_SERVER_ERROR =
      "An error occurred while processing your request, please contact the support team.";
  public static final String MSG_REQ_TIMEZONE_FIELD = "Timezone id is missing in the request.";

  // supplier
  public static final String MSG_INVALID_SUPPLIER_ID = "Invalid Supplier Id, details: ";
  public static final String MSG_INVALID_SUPPLIER_ROLE =
      "Party does not have Supplier Role, details: ";
  public static final String MSG_REQ_SUPPLIER_ID_FIELD = "Supplier id is missing in the request.";
  public static final String MSG_ENTER_SUPP_REQUIRED_DETAILS = "Supplier must have a name.";
  public static final String ERROR_MSG_UPLOAD_ATTACHMENT_FAILED =
      "An error occured while trying to upload supplier attachment, please contact support.";
  public static final String MSG_INVALID_SUPPLIER_NAME =
      "Please enter the valid supplier name without special symbol or within 100 character length.";
  public static final String MSG_SUPPLIER_LENGTH_EXCEEDS =
      "Please enter the valid supplier name within 100 characters length.";
  public static final String MSG_INVALID_TERM_NAME =
      "Term name length exceeds 255 characters, Please enter a valid term name.";
  public static final String MSG_MISSING_TERM_DETAILS = "Please specify all term details.";

  // customer
  public static final String MSG_INVALID_CUSTOMER_ID = "Invalid Customer Id, details: ";
  public static final String MSG_REQ_CUSTOMER_ID_FIELD = "Customer id is missing in the request.";

  // For product
  public static final String MSG_ENTER_PRICE_LESS_THAN_COMPARE_PRICE =
      "Price can not be greater than compare price. Please fix the prices and try again, details: ";
  public static final String MSG_INVALID_PRODUCT_ID = "Invalid Product Id: ";
  public static final String MSG_PRODUCT_ALREADY_ADDED =
      "Product is already added in this categoryId, details: ";
  public static final String MSG_ENTER_PRODUCT_NAME = "Please enter Product Name to continue.";
  public static final String MSG_INVALID_STORE_PRODUCT_ID =
      "Invalid Store's ProductId. Store Doesn't have the Product Id: ";
  public static final String MSG_PRODUCT_ALREADY_EXISTS =
      "Product name already exists, Please choose another product name or validate your existing products list to avoid duplicates, details: ";
  public static final String ERROR_MSG_PRODUCT_IMAGE_UPLOAD_FAILED =
      "An error occured while trying to upload product image, please contact support.";
  public static final String ERROR_MSG_PRODUCT_VIDEO_UPLOAD_FAILED =
      "An error occured while trying to upload product video, please contact support.";

  // For discount
  public static final String DISCOUNT_DELETED_SUCCESSFULLY = "Discount deleted successfully.";
  public static final String DISCOUNT_NAME_ALREADY_EXISTS =
      "Discount name already exists, Please choose another name or validate your existing discounts list to avoid duplicates, details: ";
  public static final String INVALID_PAGE_NUMBER = "Invalid page number, starts with 1";
  public static final String MSG_ENTER_MANDETORY_DETAILS =
      "Discount must have a name, code, type and value.";
  public static final String DISCOUNT_CODE_ALREADY_EXISTS =
      "Discount code already exists, Please choose another code to avoid duplicates, details: ";
  public static final String MSG_DISCOUNT_DELETED = "Discount deleted successfully.";

  // For category
  public static final String MSG_INVALID_CATEGORY_ID = "Invalid Category Id, details: ";
  public static final String CATEGORY_NAME_ALREADY_EXISTS =
      "Category name already exists, Please choose another name to avoid duplicates, details: ";

  public static final String MSG_ADDRESS_EMPTY = "Address is Empty.";
  public static final String MSG_ENTER_FULL_ADDRESS_TO_UPDATE =
      "Please provide the full address to update the Address.";
  public static final String MSG_USER_ROLE_UNDEFINED =
      "Access denied, user tried to access a store where no role is defined.";
  public static final String MSG_INVALID_CURRENCY_UOM_ID =
      "Currency Unit Uom ID Entered was invalid.";
  public static final String MSG_INVALID_TIMEZONE_ID = "Time Zone ID Entered was invalid.";
  public static final String MSG_INVALID_WEIGHT_UOM_ID = "Weight Unit Uom ID Entered was invalid.";

  // For note
  public static final String MSG_INVALID_NOTE_ID = "Invalid Store Id, details: ";
  public static final String MSG_NOTE_EMPTY = "Note is Empty.";
  public static final String MSG_NOTE_DELETED = "Note deleted successfully.";
  public static final String MSG_MANDATORY_INPUT_FOR_CREATE_QUOTE_NOTE =
      "Mandatory Input quoteId or NoteInfo is missing in the request";
  public static final String MSG_NOTE_LENGTH = "Please enter note within 255 character length.";
  public static final String MSG_MISSING_NOTE_ID = "Missing Quote Note Id";

  // tag
  public static final String MSG_INVALID_TAG_ID = "Invalid Tag Id: ";
  public static final String TAG_NAME_ALREADY_EXISTS =
      "Tag name already exists. Please choose another name to avoid duplicates, details: ";
  public static final String MSG_INVALID_TAG_NAME =
      "Please enter the valid tag name without special symbol or within 255 character length.";
  public static final String MSG_MISSING_TAG_ID = "Missing Quote Tag Id";
  public static final String MSG_MISSING_CREATE_TAG_DETAILS =
      "Either tag name or tag color code is missing in the request.";

  // Quote
  public static final String MSG_INVALID_QUOTE_ID = "Invalid quote Id, details: ";
  public static final String MSG_ADD_POC_TO_REORDER = "Please add contacts to reorder";
  public static final String MSG_QUOTE_PROFIT_MARGIN = "Please add Profit Margin value";
  public static final String MSG_QUOTE_ID_EMPTY = "Please enter quote id to proceed.";
  public static final String MSG_REQ_QUOTE_ROLE_FIELD =
      "Either quote id, party id or role type id is missing in the request.";
  public static final String MSG_QUOTE_EMAIL_SEND_TO_REQUIRED =
      "Send To details required when sending quote email";
  public static final String MSG_QUOTE_EMAIL_SEND_ATLEAST_ONE_RECIPIENT_REQUIRED =
      "At least one email recipient is required for sending quote email. Please check your input and try again.";
  public static final String MSG_QUOTE_SUPP_PARTY = "Supplier Party Id is missing";
  public static final String MSG_QUOTE_ORG_PARTY = "Org Party Id is missing";
  public static final String MSG_EMPTY_COVER_MSG = "Please specify data resource id and email body";
  // Quote item
  public static final String MSG_QUOTE_ITEM_REORDERED =
      "Quote item  has been reordered successfully.";
  public static final String MSG_QUOTE_ITEM_DUPLICATED =
      "Quote item  has been duplicated successfully.";

  // employee
  public static final String MSG_INVALID_EMPLOYEE_ID = "Invalid Employee Id, details: ";
  public static final String MSG_INVALID_EMPLOYEE_ROLE =
      "Party does not have Employment Role, details: ";
  public static final String MSG_ENTER_EMP_REQUIRED_DETAILS =
      "Employee must have a first name, last name, role, email and party id.";
  public static final String MSG_GROUP_IDS = "Employee Group List is empty.";

  // access
  public static final String MSG_MISSING_APIKEY_KEY = "apiKey header key is missing.";
  public static final String MSG_MISSING_APIKEY_VALUE = "apiKey header value is missing.";
  public static final String MSG_VALID_APIKEY_VALUE = "Valid apiKey header value must be provided.";
  public static final String MSG_INVALID_USER_lOGIN_ID =
      "A valid account was not found for the input login id %s, please check your input and try again.";
  public static final String INVALID_USER_lOGIN_ID =
      "Invalid identity provided, unable to find an account to be assumed.";
  public static final String MSG_INVALID_PASSWORD = "Incorrect password";
  public static final String MSG_ACCOUNT_DUPLICATE =
      "An account already exists with the input details, please log in to proceed.";

  // refresh
  public static final String MSG_MISSING_REFRESH_TOKEN_KEY = "refreshToken header key is missing.";
  public static final String MSG_MISSING_REFRESH_TOKEN_VALUE =
      "refreshToken header value is missing.";
  public static final String MSG_VALID_EFRESH_TOKEN_VALUE =
      "Valid refreshToken header value must be provided.";

  // authenticationFilter
  public static final String MSG_MISSING_ACCESS_TOKEN_KEY = "AccessToken header key is missing.";
  public static final String MSG_MISSING_ACCESS_TOKEN_VALUE =
      "AccessToken header value is missing.";
  public static final String MSG_VALID_ACCESS_TOKEN_VALUE =
      "Please validate the AccessToken header value.";

  // task
  public static final String MSG_INVALID_PREFIX_ID = "Invalid Prefix Id, details: ";
  public static final String MSG_TASK_PREFIX_ID_LENGTH =
      "Task Id Prefix is too long, please choose a shorter one, details: ";
  public static final String MSG_TASK_ID_LENGTH =
      "Task Id is too long, please choose a shorter prefix, details: ";
  public static final String MSG_TASK_CATEGORY_ID = "Task category is not defined";
  public static final String MSG_INVALID_TASK_NAME =
      "Please enter the valid task name without special symbol or within 255 character length.";
  public static final String MSG_INVALID_TASK_NOTE = "Please enter task note to proceed.";
  public static final String MSG_ENTER_TASK_REQUIRED_DETAILS = "Task must have a name.";
  public static final String MSG_ENTER_TASK_REQUIREMENT_REQUIRED_DETAILS =
      "Task must have a task id and sequence id.";
  public static final String MSG_INVALID_TASK_ID = "Please enter the valid input, details: ";
  public static final String MSG_EMPTY_ASSIGNEE_ID = "Please enter the assignee id.";
  public static final String MSG_INVALID_TASK_LINK_URL = "Please enter link URL to proceed.";
  public static final String MSG_INVALID_TASK_URL_TYPE = "Please select type to proceed.";
  public static final String MSG_INVALID_TASK_TAG_NAME = "Please enter tag name to proceed.";
  public static final String MSG_LIMIT_TASK_TAG_NAME =
      "Please enter tag name less than 255 characters.";
  public static final String MSG_ASSOC_TASK_TAG = "Tag is already associated with the issue.";
  public static final String MSG_INVALID_TASK_LINK_ID = "Please enter id of the task to be linked.";
  public static final String MSG_INVALID_OPPORTUNITY_ID =
      "Please enter opportunity id to be linked.";
  public static final String MSG_INVALID_TASK_NOT_EXISTS =
      "Task with the entered input is not exists, please check the input and try again.";

  public static final String MSG_ENTER_TASK_NAME = "Task must have a name, please enter.";

  // event
  public static final String MSG_INVALID_EVENT_ID = "Please enter the valid Event Id.";
  public static final String MSG_INVALID_EVENT_TYPE_ID = "Please enter the valid Event Type Id.";
  public static final String MSG_REQ_EVENT_FIELDS =
      "Either event type Id, event name, estimated start date or estimated completion date is missing in the request.";
  public static final String MSG_NOTFOUND_EVENT_DETAILS = "Not found the Event details.";
  public static final String MSG_INVALID_EVENT_NAME =
      "Please enter the valid event name without special symbol or within 100 character length.";
  public static final String MSG_INVALID_EVENT_DESCRIPTION =
      "Please enter the valid event description without special symbol or within 255 character length.";
  public static final String MSG_INVALID_IS_PUBLIC = "Please enter the valid isPublic(Y/N) value.";

  // globalSearch
  public static final String MSG_MISSING_KEYWORD =
      "Enter the query parameter to search for lead, customer, deal etc.";

  // customer
  public static final String MSG_INVALID_CUSTOMER_NAME =
      "Please enter the valid customer name without special symbol or within 100 character length.";
  public static final String MSG_CUSTOMER_LENGTH_EXCEEDS =
      "Please enter the valid customer name within 100 characters length.";

  // project item
  public static final String MSG_REQ_PROJECT_ITEM_ROLE_FIELD =
      "Either project item id, party id or role type id is missing in the request.";
  public static final String MSG_REQ_PROJECT_ITEM_PARTY_FIELD =
      "Either project item id or party id is missing in the request.";
  public static final String MSG_REQ_TIME_LOG_FIELD =
      "Please provide task id or project id for which you want to log your time.";
  public static final String MSG_REQ_PROJECT_ITEM_WORK_LOG_FIELD =
      "Either project item id, party id, date is missing in the request.";
  public static final String MSG_REQ_TASK_WORK_LOG_FIELD =
      "Either logged for id or status or timeZoneId is missing in the request.";
  public static final String MSG_REQ_TASK_WORK_LOG_CHECKS =
      "Either party id or workInMilliSeconds is missing in the request.";
  public static final String MSG_REQ_PARTY_FIELD = "Party Id is missing in the request.";
  public static final String MSG_ASSIGNEE_EXISTS = "Assignee already exists, details: ";
  public static final String MSG_PROJECT_NOTE_DELETED = "Note deleted successfully.";

  public static final String MSG_REQ_MEMBER_FIELDS = "Please select atleast one member.";

  public static final String DEFAULT_QUOTE_EMAIL_TYPE_ID = "QUICK_QUOTE_EML";
  public static final String DEFAULT_QUOTE_MESSAGE_ID = "QT_CVR_LTR_MSG";

  // email
  public static final String MSG_EMAIL_ALREADY_VERFIED =
      "Email has been already verified for email id ";
  public static final String MSG_INVALID_EMAIL_ID = "Invalid Email Id, details: ";

  // lead link url
  public static final String MSG_LINK_ALREADY_EXISTS = "Link already exists.";

  // organization
  public static final String MSG_ORGANIZATION_NOT_FOUND = "Organization not found.";

  // tenant
  public static final String MSG_INVALID_TENAT_ID = "Invalid tenantId.";

  // password
  public static final String PASSWORD_NOT_MATCHING =
      "Your password and confirmation password do not match";

  // Contracts
  public static final String MISSING_CONT_START_DATE = "Contract start date is missing.";
  public static final String MISSING_CONT_END_DATE = "Contract end date is missing.";
  public static final String CONT_INVALID_START_DATE =
      "Please provide valid start date. Contract start date cannot be greater or equal to End date.";
  public static final String MISSING_CONT_TITLE = "Contract title is missing.";
  public static final String CONT_NOT_EXISTS = "Invalid customer contract: ";
  public static final String MISSING_CONT_PROD = "Contract product is missing.";
  public static final String MISSING_CONT_PROD_PRICE = "Contract product price is missing.";
  public static final String CONT_PROD_PRICE_POS =
      "Contract product price should be greater than zero.";
  public static final String CONT_NO_PROD = "Product not associated with agreement: ";

  public static final String ADDR_NOT_EXISTS = "No address exist for contactId: ";
  public static final String PROD_INFO_MISS = "Product information is not provided.";
  public static final String PROD_UNIT_COST_MISS = "Unit price is missing for productId: ";
  public static final String PROD_QTY_MISS = "Quantity is missing for productId: ";

  // party
  public static final String MSG_INVALID_PARTY_ID = "Invalid Party Id, details: ";
  public static final String MSG_LINKEDIN_ADDRESS_EXISTS =
      "LinkedIn account is already exists for party: ";
  public static final String MSG_FACEBOOK_EXISTS = "FaceBook account is already exists for party: ";
  public static final String MSG_TWITTER_EXISTS = "Twitter account is already exists for party: ";
  public static final String MSG_INSTAGRAM_ADDRESS_EXISTS =
      "Instagram account is already exists for party: ";
  public static final String MSG_GITHUB_ADDRESS_EXISTS =
      "Github account is already exists for party: ";

  public static final String MSG_INVALID_QUERY_PARAM = "Invalid query param, details: ";

  // tags
  public static final String MSG_MISSING_TAG_NAME = "Tag Name is missing.";

  public static final String MSG_MISSING_TAG_TYPE = "Tag Type is missing.";
  public static final String MSG_INVALID_TAG_TYPE = "Tag Type is invalid.";
  public static final String MSG_TAG_NAME_LENGTH_CHECK =
      "Please enter tag name less than 255 characters.";
  public static final String MSG_TAG_ALREADY_EXISTS = "Tag is already associated with the message.";

  // message templates
  public static final String MSG_SCHEDULE_CHECK = "Please set message schedule.";
  public static final String MSG_WEEKLY_CHECK = "Please set time and day for week.";
  public static final String MSG_MONTHLY_CHECK = "Please set time and day for month.";
  public static final String MSG_NAME_BODY_CHECK =
      "Please enter the name and description for message to proceed.";
  public static final String MSG_NAME_LENGTH_CHECK =
      "Please enter message name less than 255 characters.";
  public static final String MSG_SAME_EXISTS = "Message with same name already exists.";
  public static final String MSG_MISSING_SUPP_OR_TEMPLATE =
      "Either Supplier party or message templates are missing.";

  // calendar
  public static final String CALENDAR_DELETED_SUCCESSFULLY = "Calendar deleted successfully.";

  // task
  public static final String TASK_REQ_DELETED = "Task requirement deleted successfully.";
  public static final String TASK_ASSIGN_DELETED = "Assignee has been removed successfully.";
  public static final String TASK_LINK_DELETED = "Task Link has been removed successfully.";
  public static final String TASK_CREATED = "Task created successfully.";
  public static final String TASK_COMPLETED = "Task has been marked as done successfully.";
  public static final String TASK_REOPEN = "Task has been reopened successfully.";

  public static final String MEMBER_ROLE_UPDATE = "Member Role has been updated successfully.";
  public static final String MEMBER_ROLE_NAME_MISSING = "Role Name is missing";
  public static final String MEMBER_ROLE_ID_MISSING = "Role Id is missing";

  // section
  public static final String SECTION_DELETED = "Section has been deleted successfully.";
  public static final String PROJ_STATUS_DELETED = "Status has been deleted successfully.";
  public static final String PROJ_CATEGORY_DELETED = "Category has been deleted successfully.";
  public static final String PROJ_CONTENT_FOLDER_DELETED = "Folder has been deleted successfully.";

  public static final String MSG_REQ_ATTRIBUTES = "Missing attributes in the request.";

  public static final String ORDER_NOT_FOUND = "Order not found";
  public static final String MSG_INVALID_ORDER_TAG_NAME = "Please enter tag name to proceed.";

  public static final String MSG_REQ_PARTY_SKILL = "Skill Type Id is missing in the request.";
  public static final String MSG_REQ_COMPANY_FIELD = "Company Id is missing in the request.";
  public static final String MSG_REQ_CATEGORY_FIELD = "Category Id is missing in the request.";
  public static final String MSG_REQ_NAME_FIELD = "Name is missing in the request.";
  public static final String MSG_REQ_COLOR_FIELD = "Color Id is missing in the request.";
  public static final String MSG_REQ_ISDEFAULT_FIELD = "Is Default Id is missing in the request.";
  public static final String PERSON_QUALIFICATIONS_UPDATED="party Qualification(s) updated successfully";

  public static final String MSG_REQ_SUPERVISOR = "Supervisor Id is missing in the request.";

  //CustomField
  public static final String CUSTOM_FIELD_VALUE_ID_MISSING = "Please enter custom field value id to proceed.";
  public static final String CUSTOM_FIELD_NAME_MISSING = "Please enter field name and type to proceed.";
  public static final String CUSTOM_FIELD_ID_MISSING = "Custom field id is missing.";
  public static final String CUSTOM_ENTITY_TYPE_MISSING = "Custom entity type is missing.";

  public static final String REQUEST_ALREADY_ASSOCIATED_WITH_TAG = "Customer request is already have tag: ";

  public static final String MISSING_STANDARD_LANGUAGE_ID = "Language is missing";
  public static final String INVALID_STANDARD_LANGUAGE_ID = "Invalid Language: ";

  public static final String EXISTING_PARTY_LANGUAGE_ID = "Party is already associated with language: ";
  public static final String NO_EXISTING_PARTY_LANGUAGE = "Party is not having language: ";


  public static final String INVALID_LANGUAGE_PREFERENCE_NO = "Fluency, Read, Write, Spoken level should be between 1-10";

  public static final String MSG_INVALID_COLOR = "Invalid color with id: ";

  // Time Entry
  public static final String MSG_INVALID_TIME_ENTRY_ID = "Invalid time entry id ";

}
