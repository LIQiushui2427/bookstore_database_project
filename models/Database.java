package models;

import java.sql.*;
import java.io.*;
import java.util.*;
import models.utils.verifyInput;
import models.file.*;

public class Database {
    final String dbAddr = "jdbc:mysql://projgw.cse.cuhk.edu.hk:2633/test";
    final String dbUsername = "csci3170";
    final String dbPassword = "testfor3170";

    final String[] tableNames = {"customer", "book", "author", "order_details", "buy","order"};

    private Connection conn = null;
    private Random random = new Random();
    private verifyInput verify = new verifyInput();

    public void connect() throws ClassNotFoundException, SQLException {
        Class.forName("com.mysql.cj.jdbc.Driver");
        this.conn = DriverManager.getConnection(dbAddr, dbUsername, dbPassword);
    }

    // ======Main Menu Operations ======//
    public void countAndPrintAllRecordsInTables() {
        for (int i = 0; i < tableNames.length; i++) {
            try {
                PreparedStatement stmt;
                if(tableNames[i].equals("order")){
                    stmt = conn.prepareStatement("SELECT COUNT(*) FROM `order`");
                }
                else
                    stmt = conn.prepareStatement("SELECT COUNT(*) FROM " + tableNames[i]);
                ResultSet rs = stmt.executeQuery();
                rs.next();
                int count = rs.getInt(1);
                System.out.printf(tableNames[i] + ": " + count);
                if(i!=tableNames.length-1){
                    System.out.printf(", ");
                }
            } catch (SQLException e) {
                System.out.println(" [Error] No such table or tables are not initialized yet.");
            }
        }
        System.out.println();
    }

    // ====== DATABASE OPERATIONS =======

    //create tables
    public void createAllTables() throws SQLException{
        PreparedStatement[] stmts={
            conn.prepareStatement("CREATE TABLE customer (uid VARCHAR(45) NOT NULL, name VARCHAR(45) NOT NULL, address VARCHAR(100) NOT NULL, PRIMARY KEY (uid))"),
            conn.prepareStatement("CREATE TABLE book (isbn VARCHAR(45) NOT NULL, title VARCHAR(45) NOT NULL, price double NOT NULL, inventory_quantity int NOT NULL, PRIMARY KEY (isbn))"),
            conn.prepareStatement("CREATE TABLE author (isbn VARCHAR(45) NOT NULL, author_name VARCHAR(45) NOT NULL, PRIMARY KEY (isbn, author_name))"),
            conn.prepareStatement("CREATE TABLE order_details (oid VARCHAR(45) NOT NULL, order_time TIMESTAMP NOT NULL, order_quantity int NOT NULL, shipping_status VARCHAR(45) NOT NULL, PRIMARY KEY (oid))"),
            conn.prepareStatement("CREATE TABLE buy (uid VARCHAR(45) NOT NULL, isbn VARCHAR(45) NOT NULL, item_quantity int NOT NULL, PRIMARY KEY (uid, isbn, item_quantity))"),
            conn.prepareStatement("CREATE TABLE `order` (oid VARCHAR(45) NOT NULL, uid VARCHAR(45) NOT NULL, item_quantity int NOT NULL, isbn VARCHAR(45) NOT NULL, PRIMARY KEY (oid, uid, item_quantity, isbn))")
        };
        for (int i = 0; i < stmts.length; i++) {
            stmts[i].execute();
            System.out.println("Table " + tableNames[i] + " created.");
        }
    } 

    //delete tables
    public void deleteAllTables() throws SQLException{
        PreparedStatement[] stmts={
            //conn.prepareStatement("SET FOREIGN_KEY_CHECKS=0"),

            conn.prepareStatement("DROP TABLE customer"),
            conn.prepareStatement("DROP TABLE book"),
            conn.prepareStatement("DROP TABLE author"),
            conn.prepareStatement("DROP TABLE order_details"),
            conn.prepareStatement("DROP TABLE buy"),
            conn.prepareStatement("DROP TABLE `order`")
        };
        for (int i = 0; i < stmts.length; i++) {
            stmts[i].execute();
            System.out.println("Table " + tableNames[i] + " deleted.");
        }
    }

    //load data
    public void loadDataFromFiles(String folderPath) throws SQLException, IOException{
        System.out.println("Loading data from files...");
        InputDB(buy.class, folderPath + "/buy.csv");
        InputDB(book.class, folderPath + "/book.csv");
        InputDB(order.class, folderPath + "/order.csv");
    }

    private void InputDB(Class<?> type, String filePath) {
        try{
        BufferedReader br = new BufferedReader(new FileReader(filePath));
        String line = br.readLine();
        while (line != null) {
            FileInputDBInterface file= (FileInputDBInterface) type.newInstance();
            file.parseFromLine(line);
            file.insertToDB(conn);
            line=br.readLine();
        }
        br.close();
    }
        catch (Exception e) {
            System.out.println("[Error]"+e.getMessage());
        }
    }
    public void listUsers() throws SQLException{
        System.out.println("Listing all users... If you are not in the list, you cannot make any order.");
        PreparedStatement stmt = conn.prepareStatement("SELECT * FROM customer");
        ResultSet rs = stmt.executeQuery();
        while(rs.next()){
            System.out.println(rs.getString("uid") + " " + rs.getString("name") + " " + rs.getString("address"));
        }
    }
    // ======Customer Operations ======//
    public void listAllBooks() throws SQLException{
        System.out.println("Listing all books...");
        PreparedStatement stmt = conn.prepareStatement("SELECT * FROM book");
        ResultSet rs = stmt.executeQuery();
        while(rs.next()){
            System.out.println(rs.getString("isbn") + " " + rs.getString("title") + " " + rs.getString("price") + " " + rs.getString("inventory_quantity"));
        }
    }
    public void placeOrder(String userID, Scanner sc){

        String s = UUID.randomUUID().toString(); 
        String orderID=s.substring(0,8)+s.substring(9,13)+s.substring(14,18)+s.substring(19,23)+s.substring(24);


        System.out.println("Order placed successfully, your order ID is: " + orderID);

        Timestamp timestamp = new Timestamp(System.currentTimeMillis());


        while(true){
            System.out.printf("Enter The Book ISBN You Like Order. Type '0' to finish: ");
            String isbn = sc.next();
            if(isbn.equals("0")){
                break;
            }
            if(!verifyInput.isISBN(isbn)){
                System.out.println("Invalid ISBN, order failed.");
                break;
            }
            System.out.println("Got it, the book is:");
            printBookByISBN(isbn);
            System.out.printf("Enter The Quantity You Like Order. Tpye '0' to finish: ");
            String _quantity = sc.next();
            if(!verifyInput.isPositiveInteger(_quantity)){
                System.out.println("Invalid input, order failed.");
                break;
            }
            int quantity = Integer.parseInt(_quantity);
            if(placeOrderUtil(userID, orderID, isbn, quantity, timestamp)==1){
                break;
            }
        }
        
        System.out.println("---------------------------");
    }

    public int placeOrderUtil(String userID, String orderID, String isbn, int quantity, Timestamp timestamp){
        try (PreparedStatement stmt1 = conn.prepareStatement("SELECT inventory_quantity FROM book WHERE isbn = ?");
             PreparedStatement _stmt2 =  conn.prepareStatement("SELECT COUNT(*) FROM `order` WHERE isbn = CAST(? AS CHAR(20)) AND oid = CAST(? AS CHAR(20))");
             PreparedStatement stmt2 = conn.prepareStatement("INSERT INTO `order`(oid, uid, item_quantity, isbn) VALUES (?, ?, ?, ?)");
             PreparedStatement stmt2_ = conn.prepareStatement("UPDATE `order` SET item_quantity = item_quantity + ? WHERE oid = ? AND isbn = ?");
             PreparedStatement stmt3 = conn.prepareStatement("SELECT * FROM order_details WHERE oid = ?");
             PreparedStatement stmt4 = conn.prepareStatement("UPDATE order_details SET order_quantity = order_quantity + ? WHERE oid = ?");
             PreparedStatement stmt5 = conn.prepareStatement("INSERT INTO order_details VALUES (?, ?, ?, ?)");
             PreparedStatement stmt6 = conn.prepareStatement("UPDATE book SET inventory_quantity = inventory_quantity - ? WHERE isbn = ?");
             PreparedStatement stmt7 = conn.prepareStatement("INSERT INTO buy (uid, isbn, item_quantity) VALUES (?, ?, ?)");
             PreparedStatement stmt8 = conn.prepareStatement("SELECT * FROM buy WHERE uid = ?");
             PreparedStatement stmt9 = conn.prepareStatement("UPDATE buy SET item_quantity = item_quantity + ? WHERE uid = ? AND isbn = ?");
             ) {
            
            stmt1.setString(1, isbn);
            ResultSet rs1 = stmt1.executeQuery();
            if(rs1.next()){
                if(rs1.getInt("inventory_quantity")<quantity){
                    System.out.println("We have not enough book! Order Failed!");
                    return 1;
                }
            }
            else{
                System.out.println("The ISBN You Entered Is Not Exist! Order Failed!");
                return 1;
            }
            rs1.close();
            
            System.out.println("Order item Placed Successfull. Updating order table...");
            //check if order is in order table
            _stmt2.setString(1, isbn);
            _stmt2.setString(2, orderID);
            ResultSet rs = _stmt2.executeQuery();
            rs.next();
            if(rs.getInt(1)==0){
                System.out.println("Order is not in order table, inserting it...");
                stmt2.setString(1, orderID);
                stmt2.setString(2, userID);
                stmt2.setInt(3, quantity);
                stmt2.setString(4, isbn);
                stmt2.executeUpdate();
                System.out.println("Order table inserted successfully.");
            }
            else{
                System.out.println("Order is in order table, updating it...");
                stmt2_.setInt(1, quantity);
                stmt2_.setString(2, orderID);
                stmt2_.setString(3, isbn);
                stmt2_.executeUpdate();
                System.out.println("Order table updated successfully.");
            }

            rs.close();
            
    
            System.out.println("Updating order_details table...");
            stmt3.setString(1, orderID);
            ResultSet rs2 = stmt3.executeQuery();
            if(rs2.next()){
                System.out.println("Order is in order_details table, updating it...");
                stmt4.setInt(1, quantity);
                stmt4.setString(2, orderID);
                stmt4.executeUpdate();
                System.out.println("Order_details table updated successfully.");
            }
            else{
                System.out.println("Order is not in order_details table, inserting it...");
                stmt5.setString(1, orderID);
                stmt5.setTimestamp(2, timestamp);
                stmt5.setInt(3, quantity);
                stmt5.setString(4, "Ordered");

                stmt5.executeUpdate();
                System.out.println("Order_details table inserted successfully.");
            }
            rs2.close();
    
            System.out.println("Updating book table...");
            stmt6.setInt(1, quantity);
            stmt6.setString(2, isbn);
            stmt6.executeUpdate();
            System.out.println("Book table updated successfully.");
            
            //insert or update buy table
            System.out.println("Updating buy table...");
            //check if order is in buy table
            
            stmt8.setString(1, userID);
            ResultSet rs3 = stmt8.executeQuery();
            //search for isbn inside buy
            
            if(!rs3.next()){//if isbn is not in buy, insert it
                System.out.println("Order is not in buy table, inserting it...");
                stmt7.setString(1, userID);
                stmt7.setString(2, isbn);
                stmt7.setInt(3, quantity);
                stmt7.executeUpdate();
            }
            //if isbn is in buy, update it
            while(rs3.next()){
                if(rs3.getString("isbn").equals(isbn)){
                    System.out.println("Order is in buy table, updating it...");
                    stmt9.setInt(1, quantity);
                    stmt9.setString(2, userID);
                    stmt9.setString(3, isbn);
                    stmt9.executeUpdate();
                    System.out.println("Buy table updated successfully.");
                    return 0;
                }
            }
            
            System.out.println("Updating buy table successful.");
            return 0;
        }
        catch (Exception e) {
            System.out.println("[Error]"+e.getMessage());
            return 1;
        }
    }
    

    public void printHistoryOrders(String userID){
        try {
            //no record
            PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(oid) FROM order_details WHERE oid in (SELECT DISTINCT oid FROM `order` WHERE uid =?)");
            stmt.setString(1, userID);
            ResultSet num = stmt.executeQuery();
            num.next();
            int count = num.getInt(1);
            if(count==0) {
                System.out.println("|                             |");
                System.out.println("     No order record found.");
                System.out.println("|                             |");
                return;
            }

            //print order record
            System.out.println("|oid|Title|Order Quantity|Order Date|Order Status|");
            stmt = conn.prepareStatement("SELECT oid FROM order_details WHERE oid in (SELECT DISTINCT oid FROM `order` WHERE uid =?)");
            stmt.setString(1, userID);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String oid = rs.getString(1);
                stmt = conn.prepareStatement("SELECT order_time, shipping_status  FROM order_details WHERE oid = ?");
                stmt.setString(1, oid);
                ResultSet rs2 = stmt.executeQuery();
                rs2.next();
                Timestamp order_date = rs2.getTimestamp(1);
                String shipping_status = rs2.getString(2);
                stmt = conn.prepareStatement("SELECT isbn, item_quantity FROM `order` WHERE oid = ?");
                stmt.setString(1, oid);
                ResultSet rs3 = stmt.executeQuery();
                while(rs3.next()){
                    String isbn = rs3.getString(1);
                    int item_quantity = rs3.getInt(2);
                    stmt = conn.prepareStatement("SELECT title FROM book WHERE isbn = ?");
                    stmt.setString(1, isbn);
                    ResultSet rs4 = stmt.executeQuery();
                    rs4.next();
                    String title = rs4.getString(1);
                    System.out.println("|"+oid+"|"+title+"|"+item_quantity+"|"+TimeConv.timeToStr(order_date)+"|"+shipping_status+"|");
                }
            }
        }
        catch (Exception e) {
            System.out.println("[Error]"+e.getMessage());
        }
    }

    public void printBookListByISBN(String isbn){
        try {
            //no record
            PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(isbn) FROM book WHERE isbn Like '%" + isbn + "%'");
            ResultSet num = stmt.executeQuery();
            num.next();
            int count = num.getInt(1);
            if(count==0) {
                System.out.println("|                             |");
                System.out.println("     No book record found.");
                System.out.println("|                             |");
                return;
            }

            //print book record
            System.out.println("|ISBN|Title|Author|Price|Inventory Quantity|");
            stmt = conn.prepareStatement("SELECT isbn, title, price, inventory_quantity FROM book WHERE isbn Like '%" + isbn + "%'");
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String ISBN = rs.getString(1);
                printBookByISBN(ISBN);
            }
        }
        catch (Exception e) {
            System.out.println("[Error]"+e.getMessage());
        }
    }
    public void printBookByISBN(String ISBN) {
        try {
            System.out.println("| ISBN        | Title                                    | Author              | Price  | Inventory Quantity |");
            PreparedStatement stmt = conn.prepareStatement("SELECT isbn, title, price, inventory_quantity FROM book WHERE isbn = ?");
            stmt.setString(1, ISBN);
            ResultSet rs = stmt.executeQuery();
            rs.next();
            String isbn = rs.getString(1);
            String title = rs.getString(2);
            double price = rs.getDouble(3);
            int inventory_quantity = rs.getInt(4);
            stmt = conn.prepareStatement("SELECT author_name FROM author WHERE isbn = ?");
            stmt.setString(1, isbn);
            ResultSet rs2 = stmt.executeQuery();
            ArrayList<String> authors = new ArrayList<String>();
            while(rs2.next()){
                authors.add(rs2.getString(1));
            }
            System.out.printf("| %-12s| %-40s| %-20s| %-7.2f| %-19d|\n", isbn, title, String.join(",", authors), price, inventory_quantity);
        } catch (Exception e) {
            System.out.println("[Error] " + e.getMessage());
        }
    }
    
    public void searchBookListByTitle(String title){
        try {
            //no record
            PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(isbn) FROM book WHERE title Like '%" + title + "%'");
            ResultSet num = stmt.executeQuery();
            num.next();
            int count = num.getInt(1);
            if(count==0) {
                System.out.println("|                             |");
                System.out.println("     No book record found.");
                System.out.println("|                             |");
                return;
            }

            //print book record
            System.out.println("|ISBN|Title|Author|Price|Inventory Quantity|");
            stmt = conn.prepareStatement("SELECT isbn FROM book WHERE title Like '%" + title + "%'");
            ResultSet rs = stmt.executeQuery();
            while(rs.next()){
                String isbn = rs.getString(1);
                printBookByISBN(isbn);
            }
        }
        catch (Exception e) {
            System.out.println("[Error]"+e.getMessage());
        }
    }

    public void printBookListByAuthor(String author){
        try {
            //no record
            PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(isbn) FROM author WHERE author_name LIKE '%" + author + "%'");
            ResultSet num = stmt.executeQuery();
            num.next();
            int count = num.getInt(1);
            if(count==0) {
                System.out.println("|                             |");
                System.out.println("     No book record found.");
                System.out.println("|                             |");
                return;
            }

            //print book record
            System.out.println("|ISBN|Title|Author|Price|Inventory Quantity|");
            stmt = conn.prepareStatement("SELECT DISTINCT(isbn) FROM author WHERE author_name LIKE '%" + author + "%'");
            ResultSet rs = stmt.executeQuery();
            while(rs.next()){
                String isbn = rs.getString(1);
                printBookByISBN(isbn);
            }
        }
        catch (Exception e) {
            System.out.println("[Error]"+e.getMessage());
        }
    }

    // ======Bookstore Operations ======//
    public void printOrderedOrder(String userID){

        try {
            //no record
            PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(oid) FROM order_details WHERE shipping_status = 'Ordered' AND oid in (SELECT DISTINCT oid FROM `order` WHERE uid =?)");
            stmt.setString(1, userID);
            ResultSet num = stmt.executeQuery();
            num.next();
            int count = num.getInt(1);
            if(count==0) {
                System.out.println("|                             |");
                System.out.println("     No order record found.");
                System.out.println("|                             |");
                return;
            }

            System.out.println("Ordered Order Record:");
            System.out.println("|oid|Order Quantity|Order Date|");

            stmt = conn.prepareStatement("SELECT oid FROM order_details WHERE shipping_status = 'Shipped' AND oid IN (SELECT DISTINCT oid FROM `order` WHERE uid = ?)");
            stmt.setString(1, userID);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                String oid = rs.getString(1);
                int orderQuantity;
                Timestamp orderTime;

                // order quantity, order time
                stmt = conn.prepareStatement("SELECT order_quantity, order_time FROM order_details WHERE oid = ? AND shipping_status = 'Ordered' ORDER BY order_time DESC");
                stmt.setString(1, oid);
                ResultSet rs2 = stmt.executeQuery();
                rs2.next();
                orderQuantity = rs2.getInt(1);
                orderTime = rs2.getTimestamp(2);

                // print result
                System.out.printf("|%-10s|%15d|%20s|\n", oid, orderQuantity, TimeConv.timeToStr(orderTime));
            }
            System.out.println("End of Query\n");
        } catch (SQLException e) {
            System.out.println("[Error] Failed to list the records.\n");
        }
    }

    public void printShippedOrder(String userID){

        try {
            //no record
            PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(oid) FROM order_details WHERE shipping_status = 'Shipped' AND oid in (SELECT DISTINCT oid FROM `order` WHERE uid =?)");
            stmt.setString(1, userID);
            ResultSet num = stmt.executeQuery();
            num.next();
            int count = num.getInt(1);
            if(count==0) {
                System.out.println("|                                            |");
                System.out.println("| No order record found or invalid user ID.  |");
                System.out.println("|                                            |");
                return;
            }

            System.out.println("Ordered Shipped Record:");
            System.out.println("|oid|Order Quantity|Order Time|");
            stmt = conn.prepareStatement("SELECT oid FROM order_details WHERE shipping_status = 'Shipped' AND oid in (SELECT DISTINCT oid FROM `order` WHERE uid =?)");
            stmt.setString(1, userID);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                String oid = rs.getString(1); 
                int orderQuantity;
                Timestamp orderTime;

                //order quantity, order date
                stmt = conn.prepareStatement("SELECT order_quantity, order_time FROM order_details WHERE oid = ?  AND shipping_status = 'Shipped' ORDER BY order_time DESC");
                stmt.setString(1, oid);
                ResultSet rs2 = stmt.executeQuery();
                rs2.next();
                orderQuantity=rs2.getInt(1);
                orderTime=rs2.getTimestamp(2);

                //print result
                System.out.println("|" + oid + "|" + orderQuantity + "|" + TimeConv.timeToStr(orderTime) + "|");
            }
            System.out.println("End of Query\n");
        } catch (SQLException e) {
            System.out.println("[Error] Failed to list the records.\n");
        }
    }

    public void printReceivedOrder(String userID){

        try {
            //no record
            PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(oid) FROM order_details WHERE shipping_status = 'Received' AND oid in (SELECT DISTINCT oid FROM `order` WHERE uid =?)");
            stmt.setString(1, userID);
            ResultSet num = stmt.executeQuery();
            num.next();
            int count = num.getInt(1);
            if(count==0) {
                System.out.println("|                             |");
                System.out.println("    No order record found.");
                System.out.println("|                             |");
                return;
            }

            System.out.println("Ordered Received Record:");
            System.out.println("|oid|Order Quantity|Order Time|");
            stmt = conn.prepareStatement("SELECT oid FROM order_details WHERE shipping_status = 'Received' AND oid in (SELECT DISTINCT oid FROM `order` WHERE uid =?)");
            stmt.setString(1, userID);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                String oid = rs.getString(1); 
                int orderQuantity;
                Timestamp orderTime;

                //order quantity, ordeer date
                stmt = conn.prepareStatement("SELECT order_quantity, order_time FROM order_details WHERE oid = ?  AND shipping_status = 'Received' ORDER BY order_time DESC");
                stmt.setString(1, oid);
                ResultSet rs2 = stmt.executeQuery();
                rs2.next();
                orderQuantity=rs2.getInt(1);
                orderTime=rs2.getTimestamp(2);

                //print result
                System.out.println("|" + oid + "|" + orderQuantity + "|" + TimeConv.timeToStr(orderTime) + "|");
            }
            System.out.println("End of Query\n");
        } catch (SQLException e) {
            System.out.println("[Error] Failed to list the records.\n");
        }

    }

    public void printMostPopularBooks(){

        try {
            //no record
            PreparedStatement stmt = conn.prepareStatement("SELECT DISTINCT COUNT(isbn) FROM buy");
            ResultSet num = stmt.executeQuery();
            num.next();
            int count = num.getInt(1);
            if(count==0||count<10) {
                System.out.println("|                             |");
                System.out.println("       Need More Data :(");
                System.out.println("|                             |");
                return;
            }

            System.out.println("|Lending frequency|isbn|Title|Author|Price|Inventory Quantity|");
            stmt = conn.prepareStatement("SELECT SUM(item_quantity), isbn FROM buy GROUP BY isbn ORDER BY SUM(item_quantity) DESC LIMIT 10");
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                int frequency = rs.getInt(1);
                String isbn = rs.getString(2);
                String title;
                ArrayList<String> authors = new ArrayList<String>();
                double price;
                int inventory_quantity;

                stmt = conn.prepareStatement("SELECT author_name FROM author WHERE isbn = ?");
                stmt.setString(1, isbn);
                ResultSet authorRS = stmt.executeQuery();
                while (authorRS.next()) {
                    authors.add(authorRS.getString(1));
                }
                
                stmt = conn.prepareStatement("SELECT title, price, inventory_quantity FROM book WHERE isbn=?");
                stmt.setString(1, isbn);
                ResultSet rs2 = stmt.executeQuery();
                rs2.next();
                title=rs2.getString(1);
                price=rs2.getDouble(2);
                inventory_quantity=rs2.getInt(3);

                //print result
                System.out.printf("|" + frequency + "|" + isbn + "|");
                boolean first = true;
                for (String author : authors) {
                    System.out.printf(first ? author : ", " + author);
                    first = false;
                } 
                System.out.println("|" + title + "|"  +price + "|" + inventory_quantity + "|");
            }
            System.out.println("End of Query\n");
        } catch (SQLException e) {
            System.out.println("[Error] Failed to list the books.\n");
        }
    }

    public boolean verifyUser(String uid){
        try{
            PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(uid) FROM customer WHERE uid=?");
            stmt.setString(1,uid);
            ResultSet num = stmt.executeQuery();
            num.next();
            int count = num.getInt(1);
            if(count==0) {
                return false;
             }
            return true;
        } catch (SQLException e) {
            System.out.println("[Error] Failed to verify user.\n");
            return false;
        }
    }
    public void createUser(String userID,String userName,String userAddress){
        try{
            PreparedStatement stmt = conn.prepareStatement("INSERT INTO customer VALUES (?,?,?)");
            stmt.setString(1, userID);
            stmt.setString(2, userName);
            stmt.setString(3, userAddress);
            stmt.executeUpdate();
            System.out.println("User " + userID + " created.");
        }catch (SQLException e) {
            System.out.println(" [Error] " + e.getMessage());
        }
    }
    public void optOrderUpdate(String oid){
        try{
            PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(oid) FROM `order` WHERE oid=?");
            stmt.setString(1,oid);
            ResultSet num = stmt.executeQuery();
            num.next();
            int count = num.getInt(1);
            if(count==0) {
                System.out.println("Fail. NO such order. Please try again");
                return;
             }
            stmt=conn.prepareStatement("SELECT shipping_status FROM order_details WHERE oid=?");
            stmt.setString(1,oid);
            ResultSet rs = stmt.executeQuery();
            rs.next();
            String status = rs.getString(1);
            if(status.equals("Shipped")){
                System.out.println("Fail. Order has been shipped.");
            }
            else if(status.equals("Received")){
                System.out.println("Fail. Order has been received.");
            }
            else{
                stmt=conn.prepareStatement("SELECT order_time FROM order_details WHERE oid=?");
                stmt.setString(1,oid);
                rs = stmt.executeQuery();
                rs.next();
                Timestamp orderTime = rs.getTimestamp(1);
                Timestamp currentTime = new Timestamp(System.currentTimeMillis());
                long diff = currentTime.getTime() - orderTime.getTime();
                long diffSeconds = diff / 1000 % 60;
                if(diffSeconds>30){
                stmt=conn.prepareStatement("UPDATE order_details SET shipping_status='Shipped' WHERE oid=?");
                stmt.setString(1,oid);
                stmt.execute();
                System.out.println("Success. Order has been shipped.");}
                else{
                    System.out.println("Fail. Order has not been ordered for 30 seconds.");
                }
            }

        }
        catch(SQLException e){
            System.out.println("[Error] Failed to load order.\n");
        }
    }
}