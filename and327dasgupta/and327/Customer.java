import java.sql.*;
import java.util.Scanner;

public class Customer {

    // keep a reference to the DB connection and scanner
    // so we do not pass them around in every method
    private final Connection conn;
    private final Scanner input;

    public Customer(Connection conn, Scanner input) {
        this.conn = conn;
        this.input = input;
    }

    public void run() {
        // main loop for the customer menu
        while (true) {

            // show a quick preview to remind us who is in the system
            showCustomerPreview();

            System.out.println("\n--- Customer Menu ---");
            System.out.println("1. View details & history");
            System.out.println("2. Make a purchase");
            System.out.println("3. Manage payment methods");
            System.out.println("0. Back");
            System.out.print("Choice: ");

            int choice;
            try {
                // basic parsing of menu choice
                choice = Integer.parseInt(input.nextLine());
            } catch (NumberFormatException e) {
                // if user types text, just ignore and redraw
                continue;
            }

            // handle menu options
            if (choice == 0) return;            // go back to main program
            else if (choice == 1) viewCustomerDetails();
            else if (choice == 2) makePurchase();
            else if (choice == 3) managePaymentMethods();
            else System.out.println("Invalid choice.");
        }
    }

    private void showCustomerPreview() {
    // this method is just for debugging and grading
    // it shows all individuals and businesses and their total spending

    System.out.println("      CUSTOMER DATABASE PREVIEW");

    // first show individuals
    System.out.println("--- INDIVIDUALS ---");
    System.out.printf("%-5s | %-20s | %s%n", "ID", "Name", "Total Exp");

    String sqlIndiv =
        "SELECT c.customer_id, c.name, c.tot_expense " +
        "FROM CUSTOMER c JOIN INDIVIDUAL i ON c.customer_id = i.customer_id " +
        "ORDER BY c.customer_id";

    try (Statement stmt = conn.createStatement();
         ResultSet rs = stmt.executeQuery(sqlIndiv)) {

        while (rs.next()) {
            System.out.printf("%-5d | %-20s | $%.2f%n",
                rs.getInt("customer_id"),
                rs.getString("name"),
                rs.getDouble("tot_expense"));
        }

    } catch (SQLException e) {
        System.out.println("Error listing individuals: " + e.getMessage());
    }

    System.out.println();

    // now show businesses
    System.out.println("--- BUSINESSES ---");
    System.out.printf("%-5s | %-20s | %s%n", "ID", "Name", "Total Exp");

    // *** FIXED: removed the typo ORDERORDER ***
    String sqlBus =
        "SELECT c.customer_id, c.name, c.tot_expense " +
        "FROM CUSTOMER c JOIN BUSINESS b ON c.customer_id = b.customer_id " +
        "ORDER BY c.customer_id";

    try (Statement stmt = conn.createStatement();
         ResultSet rs = stmt.executeQuery(sqlBus)) {

        while (rs.next()) {
            System.out.printf("%-5d | %-20s | $%.2f%n",
                rs.getInt("customer_id"),
                rs.getString("name"),
                rs.getDouble("tot_expense"));
        }

    } catch (SQLException e) {
        System.out.println("Error listing businesses: " + e.getMessage());
    }
}

    // ============================================================
    // NEW METHOD: PREVIEW OF CATALOG FOR PURCHASES
    // ============================================================
    private void showCatalogPreview() {
        // simple preview of all catalog entries before the user purchases something
        System.out.println("\n--- AVAILABLE CATALOG ITEMS/SERVICES ---");
        System.out.printf("%-8s | %-10s | %-35s | %s%n",
                "CatID", "Vendor", "Description", "Price");
        System.out.println("---------------------------------------------------------------------");

        String sql =
            "SELECT catalog_id, vendor, description, price " +
            "FROM CATALOG ORDER BY catalog_id";

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                System.out.printf("%-8d | %-10s | %-35s | $%.2f%n",
                    rs.getInt(1),
                    rs.getString(2),
                    rs.getString(3),
                    rs.getDouble(4));
            }

        } catch (SQLException e) {
            System.out.println("Error showing catalog preview: " + e.getMessage());
        }

        System.out.println("---------------------------------------------------------------------");
    }
    // ============================================================

    private void viewCustomerDetails() {
        // this screen is for one specific customer
        System.out.print("Enter customer ID from list above: ");

        int id;
        try {
            id = Integer.parseInt(input.nextLine());
        } catch (Exception e) {
            // if user types junk, just cancel
            return;
        }

        // first show name + total expense from CUSTOMER
        try {
            String sql = "SELECT name, tot_expense FROM CUSTOMER WHERE customer_id = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                System.out.println("\n--- Details for " + rs.getString("name") + " ---");
                System.out.println("Total Expense: $" + rs.getDouble("tot_expense"));
            } else {
                System.out.println("Customer not found.");
                return;
            }
            rs.close();
            ps.close();

        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
            return;
        }

        // now show combined purchase history
        System.out.println("Purchase History:");
        String histSql =
            "SELECT p.purch_date, c.description, ic.quantity, ic.price_at_purchase " +
            "FROM PURCHASE p " +
            "JOIN ITEM_PURCHASE ip ON p.pur_id = ip.pur_id " +
            "JOIN ITEM_CONTAINS ic ON ip.pur_id = ic.pur_id " +
            "JOIN CATALOG c ON ic.catalog_id = c.catalog_id " +
            "WHERE p.customer_id = ? " +
            "UNION ALL " +
            "SELECT p.purch_date, c.description, sc.quantity, sc.price_at_purchase " +
            "FROM PURCHASE p " +
            "JOIN SERVICE_PURCHASE sp ON p.pur_id = sp.pur_id " +
            "JOIN SERVICE_CONTAINS sc ON sp.pur_id = sc.pur_id " +
            "JOIN CATALOG c ON sc.catalog_id = c.catalog_id " +
            "WHERE p.customer_id = ?";

        try (PreparedStatement ps = conn.prepareStatement(histSql)) {
            ps.setInt(1, id);
            ps.setInt(2, id);
            ResultSet rs = ps.executeQuery();

            boolean found = false;
            while (rs.next()) {
                found = true;
                System.out.println(" - " + rs.getDate(1) + " | " + rs.getString(2) +
                        " | Qty: " + rs.getInt(3) +
                        " | $" + rs.getDouble(4));
            }

            if (!found) {
                System.out.println("   (No purchases found)");
            }

        } catch (SQLException e) {
            System.out.println("Error history: " + e.getMessage());
        }
    }

    // small submenu for managing stored payment methods
    private void managePaymentMethods() {
        System.out.println("\n--- Payment Methods ---");
        System.out.println("1. View credit cards");
        System.out.println("2. Add credit card");
        System.out.println("3. View bank accounts");
        System.out.println("4. Add bank account");
        System.out.println("0. Back");
        System.out.print("Choice: ");

        int choice;
        try {
            choice = Integer.parseInt(input.nextLine());
        } catch (Exception e) {
            return;
        }

        System.out.print("Enter customer ID: ");
        int custId;
        try {
            custId = Integer.parseInt(input.nextLine());
        } catch (Exception e) {
            return;
        }

        // route to specific helper based on choice
        if (choice == 0) return;
        else if (choice == 1) viewCreditCards(custId);
        else if (choice == 2) addCreditCard(custId);
        else if (choice == 3) viewBankAccounts(custId);
        else if (choice == 4) addBankAccount(custId);
    }

    private void viewCreditCards(int custId) {
        // show all credit cards tied to this customer
        String sql =
            "SELECT card_id, card_num, exp_month, exp_year " +
            "FROM CREDIT_CARD WHERE customer_id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, custId);
            ResultSet rs = ps.executeQuery();

            System.out.println("\n--- Credit Cards on File ---");
            boolean found = false;

            while (rs.next()) {
                found = true;
                System.out.println("ID: " + rs.getInt("card_id") +
                                   " | Card#: " + rs.getString("card_num") +
                                   " | Exp: " + rs.getInt("exp_month") + "/" +
                                   rs.getInt("exp_year"));
            }

            if (!found) {
                System.out.println("No credit cards found.");
            }

        } catch (SQLException e) {
            System.out.println("Error listing credit cards: " + e.getMessage());
        }
    }

    private void addCreditCard(int custId) {
        // basic insert for credit card
        try {
            System.out.print("Enter card number: ");
            String num = input.nextLine();

            System.out.print("Enter exp month (MM): ");
            int mm = Integer.parseInt(input.nextLine());

            System.out.print("Enter exp year (YYYY): ");
            int yy = Integer.parseInt(input.nextLine());

            // simple id generation with MAX+1
            int ccId = generateNewId("CREDIT_CARD", "card_id");

            String sql =
                "INSERT INTO CREDIT_CARD (card_id, card_num, exp_month, exp_year, sec_code, customer_id) " +
                "VALUES (?, ?, ?, ?, NULL, ?)";

            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, ccId);
            ps.setString(2, num);
            ps.setInt(3, mm);
            ps.setInt(4, yy);
            ps.setInt(5, custId);
            ps.executeUpdate();

            System.out.println("Credit card added!");

        } catch (Exception e) {
            System.out.println("Error adding card: " + e.getMessage());
        }
    }

    private void viewBankAccounts(int custId) {
        // very similar to credit cards, just bank accounts
        String sql =
            "SELECT bank_id, route_num, acc_num " +
            "FROM BANK_ACC WHERE customer_id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, custId);
            ResultSet rs = ps.executeQuery();

            System.out.println("\n--- Bank Accounts on File ---");
            boolean found = false;

            while (rs.next()) {
                found = true;
                System.out.println("ID: " + rs.getInt("bank_id") +
                                   " | Routing: " + rs.getString("route_num") +
                                   " | Account: " + rs.getString("acc_num"));
            }

            if (!found) {
                System.out.println("No bank accounts found.");
            }

        } catch (SQLException e) {
            System.out.println("Error listing bank accounts: " + e.getMessage());
        }
    }

    private void addBankAccount(int custId) {
        // insert a new bank account row
        try {
            System.out.print("Enter routing number: ");
            String routing = input.nextLine();

            System.out.print("Enter account number: ");
            String acct = input.nextLine();

            int bankId = generateNewId("BANK_ACC", "bank_id");

            String sql =
                "INSERT INTO BANK_ACC (bank_id, route_num, acc_num, customer_id) " +
                "VALUES (?, ?, ?, ?)";

            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, bankId);
            ps.setString(2, routing);
            ps.setString(3, acct);
            ps.setInt(4, custId);
            ps.executeUpdate();

            System.out.println("Bank account added!");

        } catch (Exception e) {
            System.out.println("Error adding bank account: " + e.getMessage());
        }
    }

    // helper to print available installment plans
    private void showInstallmentPlans() {
        String sql = "SELECT install_id, terms, int_rate FROM INSTALLMENT";

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            System.out.println("\n--- Available Installment Plans ---");
            System.out.printf("%-5s | %-10s | %s%n", "ID", "Months", "Rate");
            System.out.println("--------------------------------");

            while (rs.next()) {
                System.out.printf("%-5d | %-10d | %.2f%%%n",
                    rs.getInt(1), rs.getInt(2), rs.getDouble(3));
            }

        } catch (SQLException e) {
            System.out.println("Error showing installment plans: " + e.getMessage());
        }
    }

    private void makePurchase() {
        // this is the longest method, basically does the whole purchase flow
        try {
            conn.setAutoCommit(false); // so the whole purchase is one transaction

            System.out.print("Enter customer ID: ");
            int custId = Integer.parseInt(input.nextLine());

            boolean isIndiv = false;
            boolean isBus = false;

            // check if this id is in INDIVIDUAL
            PreparedStatement psCheck =
                conn.prepareStatement("SELECT * FROM INDIVIDUAL WHERE customer_id=?");
            psCheck.setInt(1, custId);
            if (psCheck.executeQuery().next()) isIndiv = true;

            // check if this id is in BUSINESS
            psCheck =
                conn.prepareStatement("SELECT * FROM BUSINESS WHERE customer_id=?");
            psCheck.setInt(1, custId);
            if (psCheck.executeQuery().next()) isBus = true;

            if (!isIndiv && !isBus) {
                System.out.println("Customer ID not found.");
                conn.rollback();
                return;
            }

            // ==========================================
            // SHOW CATALOG PREVIEW BEFORE ASKING FOR ID
            // ==========================================
            showCatalogPreview();

            System.out.print("Enter catalog ID to purchase: ");
            int catId = Integer.parseInt(input.nextLine());

            boolean isItem = false;
            boolean isSvc = false;
            double price = 0;

            // check if this catalog entry is an ITEM
            psCheck = conn.prepareStatement("SELECT * FROM ITEM WHERE catalog_id=?");
            psCheck.setInt(1, catId);
            if (psCheck.executeQuery().next()) isItem = true;

            // check if this catalog entry is a SERVICE
            psCheck = conn.prepareStatement("SELECT * FROM SERVICE WHERE catalog_id=?");
            psCheck.setInt(1, catId);
            if (psCheck.executeQuery().next()) isSvc = true;

            // get price from CATALOG
            PreparedStatement psPrice =
                conn.prepareStatement("SELECT price FROM CATALOG WHERE catalog_id=?");
            psPrice.setInt(1, catId);
            ResultSet rsPrice = psPrice.executeQuery();

            if (rsPrice.next()) {
                price = rsPrice.getDouble(1);
            } else {
                System.out.println("Catalog ID not found.");
                conn.rollback();
                return;
            }

            // enforce ER rules
            if (isIndiv && !isItem) {
                System.out.println("Error: Individuals can only buy ITEMS.");
                conn.rollback();
                return;
            }
            if (isBus && !isSvc) {
                System.out.println("Error: Businesses can only buy SERVICES.");
                conn.rollback();
                return;
            }

            System.out.print("Enter quantity: ");
            int qty = Integer.parseInt(input.nextLine());

            // create a new purchase row
            int purId = generateNewId("PURCHASE", "pur_id");

            String insPur =
                "INSERT INTO PURCHASE (pur_id, purch_date, total, customer_id) " +
                "VALUES (?, SYSDATE, 0, ?)";
            PreparedStatement psPur = conn.prepareStatement(insPur);
            psPur.setInt(1, purId);
            psPur.setInt(2, custId);
            psPur.executeUpdate();

            if (isItem) {
                // item purchase path for individuals

                // show all credit cards for this customer
                System.out.println("\n--- Your Credit Cards ---");
                String sqlCards =
                    "SELECT card_id, card_num, exp_month, exp_year " +
                    "FROM CREDIT_CARD WHERE customer_id = ?";
                PreparedStatement psCards = conn.prepareStatement(sqlCards);
                psCards.setInt(1, custId);
                ResultSet rsCards = psCards.executeQuery();

                while (rsCards.next()) {
                    String fullNum = rsCards.getString(2);
                    String last4;

                    if (fullNum != null && fullNum.length() >= 4) {
                        last4 = fullNum.substring(fullNum.length() - 4);
                    } else {
                        last4 = fullNum;
                    }

                    System.out.printf("%d | **** %s | Exp: %d/%d%n",
                        rsCards.getInt(1),
                        last4,
                        rsCards.getInt(3),
                        rsCards.getInt(4));
                }

                System.out.print("\nEnter Credit Card ID (or 0 for Installment): ");
                int ccId = Integer.parseInt(input.nextLine());

                if (ccId > 0) {
                    String insItemPur =
                        "INSERT INTO ITEM_PURCHASE (pur_id, cc_id, install_id) VALUES (?, ?, NULL)";
                    PreparedStatement psIp = conn.prepareStatement(insItemPur);
                    psIp.setInt(1, purId);
                    psIp.setInt(2, ccId);
                    psIp.executeUpdate();
                } else {
                    showInstallmentPlans();

                    System.out.print("Enter Installment Plan ID: ");
                    int instId = Integer.parseInt(input.nextLine());

                    String insItemPur =
                        "INSERT INTO ITEM_PURCHASE (pur_id, cc_id, install_id) VALUES (?, NULL, ?)";
                    PreparedStatement psIp = conn.prepareStatement(insItemPur);
                    psIp.setInt(1, purId);
                    psIp.setInt(2, instId);
                    psIp.executeUpdate();
                }

                // add the item line
                String insLine =
                    "INSERT INTO ITEM_CONTAINS (pur_id, catalog_id, quantity, price_at_purchase) " +
                    "VALUES (?, ?, ?, ?)";
                PreparedStatement psLine = conn.prepareStatement(insLine);
                psLine.setInt(1, purId);
                psLine.setInt(2, catId);
                psLine.setInt(3, qty);
                psLine.setDouble(4, price);
                psLine.executeUpdate();

            } else {
                // service purchase path (business customers)

                // show bank accounts before asking for ID
                System.out.println("\n--- Your Bank Accounts ---");
                String sqlBanks =
                    "SELECT bank_id, route_num, acc_num " +
                    "FROM BANK_ACC WHERE customer_id = ?";
                PreparedStatement psBanks = conn.prepareStatement(sqlBanks);
                psBanks.setInt(1, custId);
                ResultSet rsBanks = psBanks.executeQuery();

                while (rsBanks.next()) {
                    System.out.printf("%d | Routing: %s | Account: %s%n",
                        rsBanks.getInt(1),
                        rsBanks.getString(2),
                        rsBanks.getString(3));
                }

                System.out.print("\nEnter Bank Account ID: ");
                int bankId = Integer.parseInt(input.nextLine());

                String insSvcPur =
                    "INSERT INTO SERVICE_PURCHASE (pur_id, bank_id) VALUES (?, ?)";
                PreparedStatement psSp = conn.prepareStatement(insSvcPur);
                psSp.setInt(1, purId);
                psSp.setInt(2, bankId);
                psSp.executeUpdate();

                // add service line row
                String insLine =
                    "INSERT INTO SERVICE_CONTAINS (pur_id, catalog_id, quantity, price_at_purchase) " +
                    "VALUES (?, ?, ?, ?)";
                PreparedStatement psLine = conn.prepareStatement(insLine);
                psLine.setInt(1, purId);
                psLine.setInt(2, catId);
                psLine.setInt(3, qty);
                psLine.setDouble(4, price);
                psLine.executeUpdate();
            }

            conn.commit();
            conn.setAutoCommit(true);
            System.out.println("Purchase successful!");

        } catch (Exception e) {
            try {
                conn.rollback();
                conn.setAutoCommit(true);
            } catch (SQLException ignored) {}
            System.out.println("Transaction failed: " + e.getMessage());
        }
    }

    private int generateNewId(String table, String pkCol) throws SQLException {
        // helper: MAX(pk) + 1 so we do not need Oracle sequences in Java
        String sql = "SELECT NVL(MAX(" + pkCol + "), 0) + 1 FROM " + table;

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            rs.next();
            return rs.getInt(1);
        }
    }
}
