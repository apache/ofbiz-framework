# Account Overview

An account is the root level object that holds information about an application account that may belong to an individual
or a business/company. Account does not have a new entity type defined in the system, instead a new party type id
has been added so all party related functionality can be leveraged (PartyContent, PartyAttributes etc.).

## Account Relationships

Any given account will have 
- A primary email address (stored as PartyContactMech with role type id as PRIMARY_ACC_EMAIL).
- An account owner (Person) for business accounts a person record will not be required.
- A company (PartyGroup) for individual accounts a company record will not be required.


## Account Creation

When creating a new user account using email, full name and password the default account type set up is Individual account.

## Account Types

Below are the supported account types
- Individual, sole proprietor or single-member LLC
- Non-profit organization
- Partnership firm
- Limited liability company (LLC)