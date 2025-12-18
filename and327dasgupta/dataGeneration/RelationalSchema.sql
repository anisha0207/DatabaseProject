-- DROP STATEMENTS
DROP TABLE SERVICE_CONTAINS CASCADE CONSTRAINTS;
DROP TABLE ITEM_CONTAINS CASCADE CONSTRAINTS;
DROP TABLE SERVICE_PURCHASE CASCADE CONSTRAINTS;
DROP TABLE ITEM_PURCHASE CASCADE CONSTRAINTS;
DROP TABLE PURCHASE CASCADE CONSTRAINTS;
DROP TABLE INSTALLMENT CASCADE CONSTRAINTS;
DROP TABLE BANK_ACC CASCADE CONSTRAINTS;
DROP TABLE CREDIT_CARD CASCADE CONSTRAINTS;
DROP TABLE SERVICE CASCADE CONSTRAINTS;
DROP TABLE ITEM CASCADE CONSTRAINTS;
DROP TABLE CATALOG CASCADE CONSTRAINTS;
DROP TABLE BUSINESS CASCADE CONSTRAINTS;
DROP TABLE INDIVIDUAL CASCADE CONSTRAINTS;
DROP TABLE CUSTOMER CASCADE CONSTRAINTS;
DROP TABLE MANAGER CASCADE CONSTRAINTS;
DROP SEQUENCE purchase_seq;
DROP SEQUENCE catalog_seq;
DROP SEQUENCE customer_seq;
DROP SEQUENCE card_seq;
DROP SEQUENCE bank_seq;
DROP SEQUENCE install_seq;

-- Core Entities

-- Manager table (for Maintains and Updates relationships)
CREATE TABLE MANAGER (
  manager_id NUMBER       PRIMARY KEY,
  name       VARCHAR2(80) NOT NULL
);

-- Customer table
CREATE TABLE CUSTOMER (
  customer_id NUMBER        PRIMARY KEY,
  name        VARCHAR2(80)  NOT NULL,
  tot_expense NUMBER(10,2)  DEFAULT 0 NOT NULL 
                            CHECK (tot_expense >= 0)
);

-- Individual subtype
CREATE TABLE INDIVIDUAL (
  customer_id NUMBER PRIMARY KEY,
  CONSTRAINT fk_indiv_cust FOREIGN KEY (customer_id) REFERENCES CUSTOMER(customer_id)
);

-- Business subtype
CREATE TABLE BUSINESS (
  customer_id NUMBER PRIMARY KEY,
  CONSTRAINT fk_bus_cust FOREIGN KEY (customer_id) REFERENCES CUSTOMER(customer_id)
);

-- Catalog and Subtypes

CREATE TABLE CATALOG (
  catalog_id  NUMBER        PRIMARY KEY,
  vendor      VARCHAR2(80),
  description VARCHAR2(200),
  price       NUMBER(10,2)  NOT NULL CHECK (price >= 0),
  manager_id  NUMBER        NOT NULL,
  CONSTRAINT fk_cat_man FOREIGN KEY (manager_id) REFERENCES MANAGER(manager_id)
);

CREATE TABLE ITEM (
  catalog_id NUMBER PRIMARY KEY,
  CONSTRAINT fk_item_cat FOREIGN KEY (catalog_id) REFERENCES CATALOG(catalog_id)
);

CREATE TABLE SERVICE (
  catalog_id NUMBER    PRIMARY KEY,
  duration   NUMBER(5) NOT NULL CHECK (duration > 0),
  CONSTRAINT fk_svc_cat FOREIGN KEY (catalog_id) REFERENCES CATALOG(catalog_id)
);

-- Payment Plans (Right side of ER)

CREATE TABLE CREDIT_CARD (
  card_id      NUMBER       PRIMARY KEY,
  card_num     VARCHAR2(25) NOT NULL,
  exp_month    NUMBER(2)    NOT NULL,
  exp_year     NUMBER(4)    NOT NULL,
  sec_code     NUMBER(4),
  customer_id  NUMBER,
  CONSTRAINT fk_cc_cust FOREIGN KEY (customer_id) REFERENCES CUSTOMER(customer_id)
);

CREATE TABLE INSTALLMENT (
  install_id    NUMBER      PRIMARY KEY,
  terms         NUMBER(3)   NOT NULL, 
  int_rate      NUMBER(5,2) NOT NULL, 
  manager_id    NUMBER      NOT NULL, 
  CONSTRAINT fk_inst_man FOREIGN KEY (manager_id) REFERENCES MANAGER(manager_id)
);

CREATE TABLE BANK_ACC (
  bank_id     NUMBER       PRIMARY KEY,
  route_num   VARCHAR2(20) NOT NULL,
  acc_num     VARCHAR2(30) NOT NULL,
  customer_id NUMBER,
  CONSTRAINT fk_bank_cust FOREIGN KEY (customer_id) REFERENCES CUSTOMER(customer_id)
);

-- Purchase Hierarchy 

CREATE TABLE PURCHASE (
  pur_id       NUMBER       PRIMARY KEY,
  purch_date   DATE         NOT NULL, 
  total        NUMBER(10,2) DEFAULT 0,
  customer_id  NUMBER       NOT NULL,
  CONSTRAINT fk_pur_cust FOREIGN KEY (customer_id) REFERENCES CUSTOMER(customer_id)
);

-- Item Purchase Subclass
CREATE TABLE ITEM_PURCHASE (
  pur_id      NUMBER PRIMARY KEY,
  cc_id       NUMBER,
  install_id  NUMBER,
  
  CONSTRAINT fk_ip_pur FOREIGN KEY (pur_id) REFERENCES PURCHASE(pur_id),
  CONSTRAINT fk_ip_cc  FOREIGN KEY (cc_id)  REFERENCES CREDIT_CARD(card_id),
  CONSTRAINT fk_ip_in  FOREIGN KEY (install_id) REFERENCES INSTALLMENT(install_id),

  CONSTRAINT chk_item_pay CHECK (
    (cc_id IS NOT NULL AND install_id IS NULL) OR 
    (cc_id IS NULL AND install_id IS NOT NULL)
  )
);

-- Service Purchase Subclass
CREATE TABLE SERVICE_PURCHASE (
  pur_id   NUMBER PRIMARY KEY,
  bank_id  NUMBER NOT NULL, 

  CONSTRAINT fk_sp_pur  FOREIGN KEY (pur_id) REFERENCES PURCHASE(pur_id),
  CONSTRAINT fk_sp_bank FOREIGN KEY (bank_id) REFERENCES BANK_ACC(bank_id)
);

-- Line Items (Contains relationships)

CREATE TABLE ITEM_CONTAINS (
  pur_id      NUMBER NOT NULL,
  catalog_id  NUMBER NOT NULL,
  quantity    NUMBER NOT NULL,
  price_at_purchase NUMBER(10,2), 
  CONSTRAINT pk_item_con PRIMARY KEY (pur_id, catalog_id),
  CONSTRAINT fk_ic_pur   FOREIGN KEY (pur_id) REFERENCES ITEM_PURCHASE(pur_id),
  CONSTRAINT fk_ic_item  FOREIGN KEY (catalog_id) REFERENCES ITEM(catalog_id)
);

CREATE TABLE SERVICE_CONTAINS (
  pur_id      NUMBER NOT NULL,
  catalog_id  NUMBER NOT NULL,
  quantity    NUMBER NOT NULL,
  price_at_purchase NUMBER(10,2),
  CONSTRAINT pk_svc_con PRIMARY KEY (pur_id, catalog_id),
  CONSTRAINT fk_sc_pur  FOREIGN KEY (pur_id) REFERENCES SERVICE_PURCHASE(pur_id),
  CONSTRAINT fk_sc_svc  FOREIGN KEY (catalog_id) REFERENCES SERVICE(catalog_id)
);

-- Indexes

CREATE INDEX idx_catalog_desc ON CATALOG(LOWER(description));
CREATE INDEX idx_purchase_cust ON PURCHASE(customer_id);
CREATE INDEX idx_purchase_date ON PURCHASE(purch_date);

-- Sequences

CREATE SEQUENCE purchase_seq START WITH 2000;
CREATE SEQUENCE catalog_seq  START WITH 200;
CREATE SEQUENCE customer_seq START WITH 100;

CREATE SEQUENCE card_seq     START WITH 600;
CREATE SEQUENCE bank_seq     START WITH 700;
CREATE SEQUENCE install_seq  START WITH 800;

-- Triggers

CREATE OR REPLACE TRIGGER update_exp_item
AFTER INSERT ON ITEM_CONTAINS
FOR EACH ROW
BEGIN
    UPDATE CUSTOMER
    SET tot_expense = tot_expense + (:NEW.quantity * :NEW.price_at_purchase)
    WHERE customer_id = (SELECT customer_id FROM PURCHASE WHERE pur_id = :NEW.pur_id);
END;
/

CREATE OR REPLACE TRIGGER update_exp_svc
AFTER INSERT ON SERVICE_CONTAINS
FOR EACH ROW
BEGIN
    UPDATE CUSTOMER
    SET tot_expense = tot_expense + (:NEW.quantity * :NEW.price_at_purchase)
    WHERE customer_id = (SELECT customer_id FROM PURCHASE WHERE pur_id = :NEW.pur_id);
END;
/

COMMIT;
