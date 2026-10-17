<img src="https://camo.githubusercontent.com/b313d4ec52b77b5024e2988aaf76720258233e69/68747470733a2f2f6f6662697a2e6170616368652e6f72672f696d616765732f6f6662697a5f6c6f676f2e706e67" alt="Apache OFBiz" />

# Accounting component

This component enables organisation to execute financial accounting and reporting.

- [ ] We need to either remove the information below or extend it to all other components. Project management is another such case...

Full featured accounting module and financial setup of internal organisations

    Accounting (general ledger, financial)
    Accounts Payable
    Accounts Receivable

## Features

    Invoicing
    AR & AP
    Tax
    General transactions

## Data sets
### seed	
Needs to be loaded first

    permissions configuration
    help screens configuration

### seed-initial	
Loaded after seed

    scheduled services

### extseed	
Loaded after seed-initial	
	
### demo	
Loaded after extseed. For security reason don't load credentials in production!

    demo permissions
    demo data