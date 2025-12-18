# DatabaseProject

## Enterprise description:
The enterprise is LUShop, a fictional online marketplace. Customers connect to the website with creden-
tials and can browse and purchase items. For this project, we will focus solely on operations that impact
the financial aspects of LUShop. Hence, we shall concentrate on customers, managers, reporting, financing,
and items/services to be purchased. LUShop is an online service without physical locations.
LUShop offers a variety of operations:
• Accounts: Customers and Managers are users of LUShop. Their names are their access credentials (no
passwords). Customers can purchase items, apply for financing, and browse the catalog. Managers
populate the catalog and produce reports. Customers can be individuals or businesses. An individual
can purchase items, while a business can only buy services.
• Catalog: LUShop offers a variety of items and services that customers can purchase. These items/services
are maintained by LUShop’s managers. Items have an identifier, a searchable description, a price, and
a vendor. Services also have a duration.
• Financing: Services can only be paid through a bank account, while items can be purchased using
credit cards or installments. When a customer uses installments, different plans with different terms
and conditions are shown. Managers are responsible for populating these installment plans.
1
## Interfaces:
Before your “real” interface runs, prompt the user to enter the Oracle password for your Oracle account.
This keeps your password out of your Java source code. We shall be running a script at the project deadline
to change your password to something we know, so having your password in your Java code is insecure and
ensures that your final project will not execute for us when we evaluate it. DON’T DO IT!
You should implement the following interfaces:
• Customers. This interface handles customers’ information, payment methods, and records all purchases
and total expenses.
• Catalog. This interface allows customers to browse the catalog and purchase items/services.
• Manager. This interface allows managers to produce at least three aggregated reports.
You should implement your interfaces in one executable with an initial dialog, allowing the tester (that
is, a member of the course instructional team) to choose an interface.
***********
Each interface should include features to allow users to find the needed information. For example, when a
customer purchases an item, the list of credit cards on file is displayed rather than expecting the tester to
remember such information.
***********
You should implement ALL the listed interfaces. There are a lot of commonalities of features among the
various interfaces both in terms of the actions to be taken and the conditions to be tested. Take these into
account and modularize your code (in Java and via database functions, procedures, or triggers) so that each
individual interface has only a modest amount of interface-specific code.
Your README file should tell us about each interface. Each interface should be designed to be usable
by the customers or employees of the enterprise and not contain SQL jargon in its dialog.
