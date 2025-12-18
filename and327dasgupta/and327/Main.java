import java.sql.*;
import java.io.Console; 
import java.util.Scanner;

public class Main {

    // db url given by professor
    private static final String DB_URL =
        "jdbc:oracle:thin:@//rocordb01.cse.lehigh.edu:1522/cse241pdb";

    public static void main(String[] args) {

        Scanner input = new Scanner(System.in);
        String userId;
        String pass;

        // asking user for oracle login
        System.out.print("Enter Oracle user id: ");
        userId = input.nextLine();

        // if we're in terminal, hide password; if running in IDE, fallback
        Console console = System.console();

        if (console == null) {
            // IDE mode, so can't hide password (prof warned us)
            System.out.println("Enter Oracle password for " + userId + " (visible because IDE):");
            pass = input.nextLine();
        } else {
            // proper console hiding password
            char[] p = console.readPassword("Enter Oracle password for " + userId + ": ");
            pass = new String(p);
        }

        try (Connection conn = DriverManager.getConnection(DB_URL, userId, pass)) {

            System.out.println("\nConnected successfully as: " + userId);

            // quick check to see how many tables are in my schema
            try (Statement s = conn.createStatement();
                 ResultSet r = s.executeQuery("SELECT count(*) FROM user_tables")) {
                if(r.next()) {
                    System.out.println("Detected " + r.getInt(1) + " tables in your schema.");
                }
            }

            // main program loop (just menus)
            while (true) {
                System.out.println("\n==== LUShop Main Menu ====");
                System.out.println("1. Customer interface");
                System.out.println("2. Catalog interface");
                System.out.println("3. Manager interface");
                System.out.println("0. Exit");
                System.out.print("Enter choice: ");

                int choice;
                try {
                    choice = Integer.parseInt(input.nextLine()); 
                } catch (NumberFormatException e) {
                    System.out.println("Please input an integer.");
                    continue;
                }

                if (choice == 0) break;
                else if (choice == 1) {
                    // jump into customer menu
                    new Customer(conn, input).run();
                }
                else if (choice == 2) {
                    new Catalog(conn, input).run();
                }
                else if (choice == 3) {
                    new Manager(conn, input).run();
                }
                else {
                    System.out.println("Invalid input.");
                }
            }

        } catch (SQLException sqle) {
            // usually this means VPN wasn't on or credentials wrong
            System.out.println("Connection Error: " + sqle.getMessage());
        } finally {
            input.close(); // done reading input
        }

        System.out.println("Goodbye.");
    }
}
