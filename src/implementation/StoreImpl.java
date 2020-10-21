package implementation;

import StoreApp.StorePOA;
import implementation.entities.item.Item;
import implementation.entities.threads.ListItemThread;
import implementation.entities.threads.PurchaseItemThread;
import implementation.entities.threads.UpdateCustomerBudgetLogThread;
import implementation.utils.helpers.ClientHelper;
import implementation.utils.helpers.ManagerHelper;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.omg.CORBA.*;

public class StoreImpl extends StorePOA {

    private ORB orb;

    private ClientHelper clientHelper;
    private ManagerHelper managerHelper;

    private String provinceID;

    ////////////////////////////////////
    ///    Store data structures     ///
    ////////////////////////////////////
    private HashMap<String, List<Item>> inventory = new HashMap<>();
    private HashMap<String, HashMap<String, Date>> customerPurchaseLog = new HashMap<>();
    private HashMap<String, HashMap<String, Date>> customerReturnLog = new HashMap<>();
    private HashMap<String, Double> customerBudgetLog = new HashMap<>();
    private HashMap<String, List<String>> itemWaitList = new HashMap<>(); //String = itemID, List of customerIDs
    private List<Item> itemLog = new ArrayList<>();


    ////////////////////////////////////
    ///       UDP Related Ports      ///
    ////////////////////////////////////
    private static int quebecPurchaseItemUDPPort = 30000;
    private static int quebecListItemUDPPort = 30001;
    private static int quebecCustomerBudgetPort = 30002;

    private static int britishColumbiaPurchaseItemUDPPort = 30003;
    private static int britishColumbiaListItemUDPPort = 30004;
    private static int britishColumbiaCustomerBudgetPort = 30005;

    private static int ontarioPurchaseItemUDPPort = 30006;
    private static int ontarioListItemUDPPort = 30007;
    private static int ontarioCustomerBudgetPort = 30008;


    public StoreImpl(String provinceID) {
        this.provinceID = provinceID;
        clientHelper = new ClientHelper();
        managerHelper = new ManagerHelper();
        openAllPorts(this.provinceID);
    }
    @Override
    public String addItem(String managerID, String itemID, String itemName, int quantity, double price) {
        return getManagerHelper().addItem(managerID, itemID, itemName, quantity, price, this);
    }

    @Override
    public String removeItem(String managerID, String itemID, int quantity) {
        return getManagerHelper().removeItem(managerID, itemID, quantity, getInventory());
    }

    @Override
    public String listItemAvailability(String managerID) {
        return getManagerHelper().listItemAvailability(managerID, getInventory());
    }

    @Override
    public boolean purchaseItem(String customerID, String itemID, String dateOfPurchase) {
        return getClientHelper().purchaseItem(customerID, itemID, dateOfPurchase, this);
    }

    @Override
    public String findItem(String customerID, String itemName) {
        return getClientHelper().findItem(customerID, itemName, getInventory());
    }

    @Override
    public String returnItem(String customerID, String itemID, String dateOfReturn) {
        return getClientHelper().returnItem(customerID, itemID, dateOfReturn, this);
    }

    @Override
    public void requestUpdateOfCustomerBudgetLog(String customerID, double price) {

    }

    @Override
    public boolean waitList(String customerID, String itemID, String dateOfPurchase) {
        return false;
    }

    @Override
    public void shutdown() {
        orb.shutdown(false);
    }

    public void setORB(ORB orb_val) {
        orb = orb_val;
    }


    private void openAllPorts(String provinceID) {
        switch(provinceID.toLowerCase()){
            case "on":
                openListItemUDPPort(ontarioListItemUDPPort);
                openPurchaseItemUDPPort(ontarioPurchaseItemUDPPort);
                openUpdateCustomerBudgetLogUDPPort(ontarioCustomerBudgetPort);
                break;
            case "qc":
                openListItemUDPPort(quebecListItemUDPPort);
                openPurchaseItemUDPPort(quebecPurchaseItemUDPPort);
                openUpdateCustomerBudgetLogUDPPort(quebecCustomerBudgetPort);
                break;
            case "bc":
                openListItemUDPPort(britishColumbiaListItemUDPPort);
                openPurchaseItemUDPPort(britishColumbiaPurchaseItemUDPPort);
                //openUpdateCustomerBudgetLogUDPPort(britishColumbiaCustomerBudgetPort);
                break;
        }
    }

    public void openListItemUDPPort(int updPort) { //Receiving other stores request for a list of items
        DatagramSocket serverSocket = null;
        try {
            serverSocket = new DatagramSocket(updPort);
            System.out.println("Open List Item Port: "+updPort);
            byte[] receiveData = new byte[8];

            System.out.printf("Listening on udp:%s:%d%n", InetAddress.getLocalHost().getHostAddress(), updPort);
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            // System.out.println("Opening UDP ports for store");
            ListItemThread thread = new ListItemThread(serverSocket, receivePacket, this);
            thread.start();
        } catch (Exception e) {
            System.out.println("openListItemUDPPort \n"+ e);
            e.printStackTrace();
        } finally {
            //serverSocket.close();
        }
    }

    public void openPurchaseItemUDPPort(int updPort) { //When receiving requests to purchase items
        DatagramSocket serverSocket = null;
        try {
            System.out.printf("Listening on udp:%s:%d%n", InetAddress.getLocalHost().getHostAddress(), updPort);
            serverSocket = new DatagramSocket(updPort);
            System.out.println("Purchase Item UPD port: "+ updPort);
            byte[] receiveData = new byte[1024];

            System.out.printf("Listening on udp:%s:%d%n", InetAddress.getLocalHost().getHostAddress(), updPort);
            // System.out.println("Opening purchase UDP port"+updPort+"for store");
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            PurchaseItemThread thread = new PurchaseItemThread(serverSocket, receivePacket, this);
            thread.start();

        } catch (Exception e) {
            System.out.println("openPurchaseItemUDPPort \n"+ e);
            e.printStackTrace();
        } finally {
            //  serverSocket.close();
        }
    }

    public void openUpdateCustomerBudgetLogUDPPort(int updPort) { //When receiving requests to purchase items
        DatagramSocket serverSocket = null;
        try {
            serverSocket = new DatagramSocket(updPort);
            System.out.println("Update customer budget UPD port: "+ updPort);
            byte[] receiveData = new byte[1024];
            String sendString = "Update customer budget item port opened ...";
            byte[] sendData = sendString.getBytes("UTF-8");

            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            UpdateCustomerBudgetLogThread thread = new UpdateCustomerBudgetLogThread(serverSocket, receivePacket, this.customerBudgetLog);
            thread.start();

        } catch (Exception e) {
            System.out.println("openUpdateCustomerBudgetLogUDPPort \n"+ e);
            e.printStackTrace();
        } finally {
            //  serverSocket.close();
        }
    }

    ////////////////////////////////////
    ///     Getters and Setters      ///
    ////////////////////////////////////
    public HashMap<String, List<String>> getItemWaitList() {
        return this.itemWaitList;
    }
    public HashMap<String, List<Item>> getInventory() { return this.inventory; }
    public HashMap<String, Double> getCustomerBudgetLog() { return this.customerBudgetLog; }
    public HashMap<String, HashMap<String, Date>> getCustomerPurchaseLog() { return this.customerPurchaseLog; }
    public HashMap<String, HashMap<String, Date>> getCustomerReturnLog() { return this.customerReturnLog; }
    public List<Item> getItemLog() { return this.itemLog; }

    public ClientHelper getClientHelper() { return this.clientHelper; }
    public ManagerHelper getManagerHelper() { return this.managerHelper; }
}