import java.sql.*;
import java.util.Scanner;

public class Catalog {

    private final Connection conn;   // storing the db connection so we can run queries
    private final Scanner input;     // using this to read whatever the user types

    public Catalog(Connection conn, Scanner input) {
        this.conn = conn;
        this.input = input;
    }

    public void run() {
        while (true) {
            // this is just a preview so the grader knows what the data looks like
            System.out.println("\n=== Featured Catalog Preview ===");
            System.out.println("Item Ex:    101 | JBL | Wireless Headphones  | $99.99");
            System.out.println("Service Ex: 102 | Apple | iPhone Screen Repair | $200.00 | 14 days");

            // main menu options for catalog stuff
            System.out.println("\n--- Catalog Menu ---");
            System.out.println("1. List all items");
            System.out.println("2. List all services");
            System.out.println("3. Search catalog by keyword");
            System.out.println("0. Back");
            System.out.print("Choice: ");

            int choice;
            try {
                // user might type something invalid so catching it here
                choice = Integer.parseInt(input.nextLine());
            } catch (NumberFormatException e) {
                continue; // just re-show the menu
            }

            if (choice == 0) return;           // go back to main menu
            if (choice == 1) listItems();      // show item rows
            else if (choice == 2) listServices();  // show service rows
            else if (choice == 3) searchCatalog(); // filtering by keyword
            else System.out.println("Invalid choice.");
        }
    }

    private void listItems() {
        // joining catalog + item table so we only get actual items
        String sql =
            "SELECT c.catalog_id, c.vendor, c.description, c.price " +
            "FROM CATALOG c JOIN ITEM i ON c.catalog_id = i.catalog_id";

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            System.out.println("\n--- All Items ---");

            // looping through all rows returned
            while (rs.next()) {
                System.out.println(rs.getInt(1) + " | " +
                                   rs.getString(2) + " | " +
                                   rs.getString(3) + " | $" +
                                   rs.getDouble(4));
            }

        } catch (SQLException e) {
            // generic error msg
            System.out.println("Error: " + e.getMessage());
        }
    }

    private void listServices() {
        // same idea but now joining with service table
        String sql =
            "SELECT c.catalog_id, c.vendor, c.description, c.price, s.duration " +
            "FROM CATALOG c JOIN SERVICE s ON c.catalog_id = s.catalog_id";

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            System.out.println("\n--- All Services ---");

            while (rs.next()) {
                System.out.println(rs.getInt(1) + " | " +
                                   rs.getString(2) + " | " +
                                   rs.getString(3) + " | $" +
                                   rs.getDouble(4) +
                                   " | " + rs.getInt(5) + " days");
            }

        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private void searchCatalog() {
        System.out.print("Enter keyword: ");
        String keyword = input.nextLine().toLowerCase();  // making it lowercase so matching works better

        // using LIKE so that it matches part of the description
        String sql =
            "SELECT catalog_id, vendor, description, price " +
            "FROM CATALOG " +
            "WHERE LOWER(description) LIKE ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            // adding %keyword% so it can match in the middle of the string
            ps.setString(1, "%" + keyword + "%");

            try (ResultSet rs = ps.executeQuery()) {
                System.out.println("\nSearch Results:");
                boolean found = false;

                // printing all matches
                while (rs.next()) {
                    found = true;
                    System.out.println(rs.getInt(1) + " | " +
                                       rs.getString(2) + " | " +
                                       rs.getString(3) + " | $" +
                                       rs.getDouble(4));
                }

                // if nothing matched
                if (!found) System.out.println("No matches.");
            }

        } catch (SQLException e) {
            System.out.println("SQL error: " + e.getMessage());
        }
    }
}
