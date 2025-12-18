Author: Anisha Dasgupta 
Course: CSE 241 – Database Systems  
Project: Checkpoint 3 – Relational Schema and Data Population  
Date: October 2025

Description:
This project implements the relational schema and data population for the ER design created
earlier in the semester.  It models customers (individuals and businesses), managers, catalog
items and services, payment plans, and purchases.
“You should implement the following interfaces… Customers, Catalog, Manager.”
“Each interface should be in one executable with an initial dialog.”
- Main.java
- Customer.java
- Catalog.java
- Manager.java

Files:
   Data Population Folder:    
   1. RelationalSchema.sql 
      - Contains DROP TABLE, DROP SEQUENCES, CREATE TABLE, and SEQUENCES.
      - Includes PL/SQL Triggers to automatically update customer 'tot_expense'.
   2. DataPopulation.sql
      - Contains INSERT statements.

2. Source Code (and327/):
   - Main.java:     Entry point. Handles Oracle connection and main menu navigation.
   - Customer.java: Handles customer listing, detailed history view, and the 
                    "Make Purchase" transaction logic.
   - Catalog.java:  Allows searching and listing of Items and Services.
   - Manager.java:  Provides analytical reports (Revenue, Spending, Daily Activity).

1. Compile:
   cd and327
   javac -cp .:../ojdbc11.jar *.java
   cd ..
   jar cfm and327.jar manifest.txt -C and327 .

2. Run:
   java -jar and327.jar