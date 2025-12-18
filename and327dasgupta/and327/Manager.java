import java.sql.*;
import java.util.Scanner;

public class Manager {

    // same pattern as Customer class
    // store the connection and the scanner once
    private final Connection conn;
    private final Scanner input;

    public Manager(Connection conn, Scanner input) {
        this.conn = conn;
        this.input = input;
    }

    public void run() {
        // main menu loop for manager functionality
        while (true) {
            System.out.println("\n--- Manager Menu ---");
            System.out.println("1. Report: Total spending by customer");
            System.out.println("2. Report: Total revenue by catalog item");
            System.out.println("3. Report: Purchases per day");
            System.out.println("4. List all managers");
            System.out.println("5. Manage catalog");
            System.out.println("6. Manage installment plans");
            System.out.println("0. Back");
            System.out.print("Choice: ");

            int choice;
            try {
                choice = Integer.parseInt(input.nextLine());
            } catch (NumberFormatException e) {
                // user typed something not numeric
                continue;
            }

            // dispatch based on menu selection
            if (choice == 0) return;
            else if (choice == 1) reportSpendingByCustomer();
            else if (choice == 2) reportRevenueByItem();
            else if (choice == 3) reportPurchasesByDay();
            else if (choice == 4) listManagers();
            else if (choice == 5) manageCatalog();
            else if (choice == 6) manageInstallmentPlans();
            else System.out.println("Invalid choice.");
        }
    }

    private void listManagers() {
        // quick helper to list rows from MANAGER table
        String sql = "SELECT manager_id, name FROM MANAGER";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            System.out.println("\n--- Registered Managers ---");
            while (rs.next()) {
                System.out.println(rs.getInt(1) + " | " + rs.getString(2));
            }
        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private void reportSpendingByCustomer() {
        // uses tot_expense column that is updated by our triggers
        String sql = "SELECT name, tot_expense FROM CUSTOMER ORDER BY tot_expense DESC";

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            System.out.println("\nTotal Spending by Customer:");
            System.out.printf("%-20s | %s%n", "Name", "Total");
            System.out.println("-------------------------------");

            while (rs.next()) {
                System.out.printf("%-20s | $%.2f%n",
                    rs.getString("name"), rs.getDouble("tot_expense"));
            }

        } catch (SQLException e) {
            System.out.println("SQL error: " + e.getMessage());
        }
    }

    private void reportRevenueByItem() {
        // combine item and service line items to compute revenue per catalog description
        String sql =
            "SELECT c.description, SUM(combined.qty * combined.sold_price) as revenue FROM (" +
            "  SELECT catalog_id, quantity as qty, price_at_purchase as sold_price FROM ITEM_CONTAINS " +
            "  UNION ALL " +
            "  SELECT catalog_id, quantity as qty, price_at_purchase as sold_price FROM SERVICE_CONTAINS " +
            ") combined " +
            "JOIN CATALOG c ON combined.catalog_id = c.catalog_id " +
            "GROUP BY c.description " +
            "ORDER BY revenue DESC";

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            System.out.println("\nRevenue by Item:");
            System.out.printf("%-30s | %s%n", "Item", "Revenue");
            System.out.println("-------------------------------------------");

            while (rs.next()) {
                System.out.printf("%-30s | $%.2f%n",
                    rs.getString(1), rs.getDouble(2));
            }

        } catch (SQLException e) {
            System.out.println("SQL error: " + e.getMessage());
        }
    }

    private void reportPurchasesByDay() {
        // group by TRUNC(purch_date) to ignore time part
        String sql =
            "SELECT TRUNC(purch_date), COUNT(*) " +
            "FROM PURCHASE " +
            "GROUP BY TRUNC(purch_date) " +
            "ORDER BY TRUNC(purch_date)";

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            System.out.println("\nPurchases per Day:");
            while (rs.next()) {
                System.out.println(rs.getDate(1) + " | " + rs.getInt(2));
            }

        } catch (SQLException e) {
            System.out.println("SQL error: " + e.getMessage());
        }
    }

    // ================== CATALOG MANAGEMENT SECTION ==================
    private void manageCatalog() {
        // small submenu for creating/updating/deleting catalog entries
        System.out.println("\n--- Catalog Management ---");
        System.out.println("1. Add new item");
        System.out.println("2. Add new service");
        System.out.println("3. Update item/service");
        System.out.println("0. Back");
        System.out.print("Choice: ");

        int c;
        try {
            c = Integer.parseInt(input.nextLine());
        } catch (Exception e) {
            return;
        }

        if (c == 0) return;
        else if (c == 1) addItem();
        else if (c == 2) addService();
        else if (c == 3) updateCatalog();   // upgraded version of this method
    }

    private void addItem() {
        // inserts CATALOG row + corresponding ITEM row
        try {
            System.out.print("Enter vendor: ");
            String vendor = input.nextLine();

            System.out.print("Enter description: ");
            String desc = input.nextLine();

            System.out.print("Enter price: ");
            double price = Double.parseDouble(input.nextLine());

            System.out.print("Enter manager ID: ");
            int managerId = Integer.parseInt(input.nextLine());

            // generate catalog id using helper
            int catId = generateNewId("CATALOG", "catalog_id");

            // insert into CATALOG
            String sql1 = "INSERT INTO CATALOG VALUES (?, ?, ?, ?, ?)";
            PreparedStatement ps1 = conn.prepareStatement(sql1);
            ps1.setInt(1, catId);
            ps1.setString(2, vendor);
            ps1.setString(3, desc);
            ps1.setDouble(4, price);
            ps1.setInt(5, managerId);
            ps1.executeUpdate();

            // insert into ITEM (subtype)
            String sql2 = "INSERT INTO ITEM VALUES (?)";
            PreparedStatement ps2 = conn.prepareStatement(sql2);
            ps2.setInt(1, catId);
            ps2.executeUpdate();

            System.out.println("Item added with ID: " + catId);

        } catch (Exception e) {
            System.out.println("Error adding item: " + e.getMessage());
        }
    }

    private void addService() {
        // similar flow as addItem but uses SERVICE table instead
        try {
            System.out.print("Enter vendor: ");
            String vendor = input.nextLine();

            System.out.print("Enter description: ");
            String desc = input.nextLine();

            System.out.print("Enter price: ");
            double price = Double.parseDouble(input.nextLine());

            System.out.print("Enter duration (in days): ");
            int duration = Integer.parseInt(input.nextLine());

            System.out.print("Enter manager ID: ");
            int managerId = Integer.parseInt(input.nextLine());

            int catId = generateNewId("CATALOG", "catalog_id");

            String sql1 = "INSERT INTO CATALOG VALUES (?, ?, ?, ?, ?)";
            PreparedStatement ps1 = conn.prepareStatement(sql1);
            ps1.setInt(1, catId);
            ps1.setString(2, vendor);
            ps1.setString(3, desc);
            ps1.setDouble(4, price);
            ps1.setInt(5, managerId);
            ps1.executeUpdate();

            String sql2 = "INSERT INTO SERVICE VALUES (?, ?)";
            PreparedStatement ps2 = conn.prepareStatement(sql2);
            ps2.setInt(1, catId);
            ps2.setInt(2, duration);
            ps2.executeUpdate();

            System.out.println("Service added with ID: " + catId);

        } catch (Exception e) {
            System.out.println("Error adding service: " + e.getMessage());
        }
    }

    // Priority 2: fully implemented updateCatalog based on the suggestion
    private void updateCatalog() {
        try {
            System.out.print("Enter catalog ID to update: ");
            int catId = Integer.parseInt(input.nextLine());

            // pull the current row from CATALOG so user sees before updating
            String sqlSelect = "SELECT vendor, description, price FROM CATALOG WHERE catalog_id = ?";
            PreparedStatement psSelect = conn.prepareStatement(sqlSelect);
            psSelect.setInt(1, catId);
            ResultSet rs = psSelect.executeQuery();

            if (!rs.next()) {
                // if no row comes back, id is not valid
                System.out.println("Catalog ID not found.");
                return;
            }

            // show existing values to the user
            System.out.println("Current: " +
                rs.getString("vendor") + " | " +
                rs.getString("description") + " | $" +
                rs.getDouble("price"));

            System.out.print("Enter new price (leave blank to skip): ");
            String priceStr = input.nextLine();

            // if user actually typed something, update the price
            if (!priceStr.trim().isEmpty()) {
                double newPrice = Double.parseDouble(priceStr);

                String sqlUpdate = "UPDATE CATALOG SET price = ? WHERE catalog_id = ?";
                PreparedStatement psUpdate = conn.prepareStatement(sqlUpdate);
                psUpdate.setDouble(1, newPrice);
                psUpdate.setInt(2, catId);
                psUpdate.executeUpdate();

                System.out.println("Price updated!");
            }

        } catch (Exception e) {
            System.out.println("Error updating catalog: " + e.getMessage());
        }
    }

    //INSTALLMENT PLAN SECTION
    private void manageInstallmentPlans() {
        // small menu for viewing and adding plans
        System.out.println("\n--- Installment Plan Management ---");
        System.out.println("1. View all plans");
        System.out.println("2. Add new plan");
        System.out.println("0. Back");
        System.out.print("Choice: ");

        int c;
        try {
            c = Integer.parseInt(input.nextLine());
        } catch (Exception e) {
            return;
        }

        if (c == 0) return;
        else if (c == 1) viewInstallmentPlans();
        else if (c == 2) addInstallmentPlan();
    }

    private void viewInstallmentPlans() {
        // simply dump install_id, terms, int_rate for all rows
        String sql = "SELECT install_id, terms, int_rate FROM INSTALLMENT";

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            System.out.println("\n--- Installment Plans ---");
            System.out.printf("%-5s | %-10s | %s%n", "ID", "Months", "Rate");
            System.out.println("--------------------------------");

            while (rs.next()) {
                System.out.printf("%-5d | %-10d | %.2f%%%n",
                    rs.getInt(1), rs.getInt(2), rs.getDouble(3));
            }

        } catch (SQLException e) {
            System.out.println("Error reading plans: " + e.getMessage());
        }
    }

    private void addInstallmentPlan() {
        // insert a new row into INSTALLMENT
        try {
            System.out.print("Enter terms (months): ");
            int terms = Integer.parseInt(input.nextLine());

            System.out.print("Enter interest rate: ");
            double rate = Double.parseDouble(input.nextLine());

            System.out.print("Enter manager ID: ");
            int managerId = Integer.parseInt(input.nextLine());

            int id = generateNewId("INSTALLMENT", "install_id");

            String sql =
                "INSERT INTO INSTALLMENT (install_id, terms, int_rate, manager_id) " +
                "VALUES (?, ?, ?, ?)";

            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, id);
            ps.setInt(2, terms);
            ps.setDouble(3, rate);
            ps.setInt(4, managerId);
            ps.executeUpdate();

            System.out.println("Installment plan added with ID: " + id);

        } catch (Exception e) {
            System.out.println("Error adding plan: " + e.getMessage());
        }
    }

    // helper to generate primary keys using MAX + 1
    // same pattern as Customer class so it stays consistent
    private int generateNewId(String table, String pkCol) throws SQLException {
        String sql = "SELECT NVL(MAX(" + pkCol + "), 0) + 1 FROM " + table;

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            rs.next();
            return rs.getInt(1);
        }
    }
}
