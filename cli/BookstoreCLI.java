package cli;

import java.util.*;
import models.*;

public class BookstoreCLI implements CLIInterface {
    private Database db;
    private Scanner sc;

    public BookstoreCLI(Database db, Scanner sc) {
        this.db = db;
        this.sc = sc;
    }

    public void startCLI() {
        while (true) {
            printMenu();
            String choice_ = sc.next();
            if(!models.utils.verifyInput.isPositiveInteger(choice_)){
                System.out.println("[Error] Invalid operation, choose again.\n");
                continue;
            }
            System.out.println();
            int choice = Integer.parseInt(choice_);
            switch (choice) {
                case 1: optOrderUpdate(); break;
                case 2: optOrderQuery(); break;
                case 3: optPopularBook(); break;
                case 4: return;
                default: System.out.println("[Error] Invalid operation, choose again.\n");
            }
        }
    }

    private void printMenu() {
        System.out.println("-----Bookstore Operation-----");
        System.out.println(">1. Order Shipping Status Update");
        System.out.println(">2. Order Query");
        System.out.println(">3. Most Popular Books");
        System.out.println(">4. Back to the main menu");
        System.out.printf("Please Enter Your Query: ");
    }

    private void optOrderQuery() {
        while(true){
        printSubMenu1();
        String choice_ = sc.next();
            if(!models.utils.verifyInput.isPositiveInteger(choice_)){
                System.out.println("[Error] Invalid operation, choose again.\n");
                continue;
            }
            System.out.println();
            int choice = Integer.parseInt(choice_);
            switch (choice) {
                case 1: optOrderedOrder(); break;
                case 2: optShippedOrder(); break;
                case 3: optReceivedOrder(); break;
                case 4: return;
                default: System.out.println("[Error] Invalid operation, choose again.\n");
            }
    }
    }

    private void printSubMenu1() {
        System.out.println("-----Choose the Grouped Order-----");
        System.out.println(">1. Ordered Order");
        System.out.println(">2. Shipped Order");
        System.out.println(">3. Received Order");
        System.out.println(">4. Back to the bookstore operation menu");
        System.out.printf("Please Enter Your Query: ");
    }

    private void optOrderedOrder(){
        System.out.printf("Enter The User ID: ");
        String userID = sc.next();
        System.out.println("-----Ordered Order-----");
        db.printOrderedOrder(userID);
        System.out.println("-----------------------");
    }

    private void optShippedOrder(){
        System.out.printf("Enter The User ID: ");
        String userID = sc.next();
        System.out.println("-----Shipped Order-----");
        db.printShippedOrder(userID);
        System.out.println("-----------------------");
    }

    private void optReceivedOrder(){
        System.out.printf("Enter The User ID: ");
        String userID = sc.next();
        System.out.println("-----Received Order-----");
        db.printReceivedOrder(userID);
        System.out.println("-----------------------");
    }

    private void optPopularBook() {
        System.out.println("-----Most Popular Books TOP 10-----");
        db.printMostPopularBooks();
        System.out.println("-----------------------");
    }

    private void optOrderUpdate(){
        System.out.printf("Enter The Order ID: ");
        String orderID = sc.next();
        db.optOrderUpdate(orderID);
    }
}