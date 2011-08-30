package org.multibit.model;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.StringTokenizer;
import java.util.Vector;

import com.google.bitcoin.core.Address;

public class AddressBook {

    /**
     * the total receiving addresses known - from the address book (will include
     * keys that are in other wallets)
     */
    private Set<AddressBookData> candidateReceivingAddresses;

    /**
     * the actual receiving addresses exposed for this address book (only keys
     * that occur in this wallet)
     */
    private Vector<AddressBookData> receivingAddresses;
    private Vector<AddressBookData> sendingAddresses;

    private String ADDRESS_BOOK_FILENAME = "addressBook.csv";
    private String RECEIVE_ADDRESS_MARKER = "receive";
    private String SEND_ADDRESS_MARKER = "send";
    private String SEPARATOR = ",";

    private String ADDRESSBOOK_MAGIC_TEXT = "multiBit.addressBook";
    private String VERSION_TEXT = "1";

    public AddressBook() {
        candidateReceivingAddresses = new HashSet<AddressBookData>();
        receivingAddresses = new Vector<AddressBookData>();
        sendingAddresses = new Vector<AddressBookData>();

        loadFromFile();
    }

    public Vector<AddressBookData> getReceivingAddresses() {
        return receivingAddresses;
    }

    public void setReceivingAddresses(Vector<AddressBookData> receivingAddresses) {
        this.receivingAddresses = receivingAddresses;
    }

    public Vector<AddressBookData> getSendingAddresses() {
        return sendingAddresses;
    }

    public void setSendingAddresses(Vector<AddressBookData> sendingAddresses) {
        this.sendingAddresses = sendingAddresses;
    }

    /**
     * add a receiving address in the form of an AddressBookData, replacing the
     * label of any existing address
     * 
     * @param receivingAddress
     * @param addToCandidates
     *            - add to the list of candidate receiving addresses
     */
    public void addReceivingAddress(AddressBookData receivingAddress, boolean addToCandidates) {
        if (receivingAddress == null) {
            return;
        }

        Collection<AddressBookData> addressesToUse;
        if (addToCandidates) {
            addressesToUse = candidateReceivingAddresses;
        } else {
            addressesToUse = receivingAddresses;
        }

        boolean done = false;
        // check the address is not already in the set
        for (AddressBookData addressBookData : addressesToUse) {
            if (addressBookData.getAddress().equals(receivingAddress.getAddress())) {
                // just update label
                addressBookData.setLabel(receivingAddress.getLabel());
                done = true;
                break;
            }
        }

        if (!done) {
            addressesToUse.add(receivingAddress);
        }
    }

    /**
     * add a receiving address that belongs to a key of the current wallet
     * this will always be added and will take the label of any matching address
     * in the list of candidate addresses fron the address book
     * @param receivingAddress
     */
    public void addReceivingAddressOfKey(Address receivingAddress) {
        if (receivingAddress == null) {
            return;
        }

        if (!containsReceivingAddress(receivingAddress.toString())) {
            // see if there is a label in the candidate receiving addresses
            String label = "";
            
            for (AddressBookData addressBookData : candidateReceivingAddresses) {
                if (addressBookData.getAddress().equals(receivingAddress.toString())) {
                    label = addressBookData.getLabel();
                    break;
                }
            }
            receivingAddresses.add(new AddressBookData(label, receivingAddress.toString()));
       }
     }

    public boolean containsReceivingAddress(String receivingAddress) {
        boolean toReturn = false;
        // see if the receiving address is on the current list
        for (AddressBookData addressBookData : receivingAddresses) {
            if (addressBookData.getAddress().equals(receivingAddress.toString())) {
                // do nothing
                toReturn = true;
                break;
            }
        }

        return toReturn;
    }

    public void addSendingAddress(AddressBookData sendingAddress) {
        if (sendingAddress == null) {
            return;
        }

        boolean done = false;
        // check the address is not already in the Vector
        for (AddressBookData addressBookData : sendingAddresses) {
            if (addressBookData.getAddress().equals(sendingAddress.getAddress())) {
                // just update label
                addressBookData.setLabel(sendingAddress.getLabel());
                done = true;
                break;
            }
        }

        if (!done) {
            sendingAddresses.add(sendingAddress);
        }
    }

    public String lookupLabelForReceivingAddress(String address) {
        for (AddressBookData addressBookData : receivingAddresses) {
            if (addressBookData.getAddress().equals(address)) {
                return addressBookData.getLabel();
            }
        }

        return "";
    }

    public String lookupLabelForSendingAddress(String address) {
        for (AddressBookData addressBookData : sendingAddresses) {
            if (addressBookData.getAddress().equals(address)) {
                return addressBookData.getLabel();
            }
        }

        return "";
    }

    /**
     * write out the address book - a simple comma separated file format is used
     * - should probably be something like JSON
     */
    public void writeToFile() {
        try {
            // we write out the union of the candidate and actual receiving addresses
            HashMap<String, AddressBookData> allReceivingAddresses = new HashMap<String, AddressBookData>();
            if (candidateReceivingAddresses != null) {
                for(AddressBookData addressBookData : candidateReceivingAddresses) {
                    allReceivingAddresses.put(addressBookData.address, addressBookData);
                }
            }
            if (receivingAddresses != null) {
                for(AddressBookData addressBookData : receivingAddresses) {
                    allReceivingAddresses.put(addressBookData.address, addressBookData);
                }
            }
            
            // Create file
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter
                    (new FileOutputStream(ADDRESS_BOOK_FILENAME),"UTF8"));

            // write out the multibit addressbook identifier
            out.write(ADDRESSBOOK_MAGIC_TEXT + SEPARATOR + VERSION_TEXT + "\n");

            Collection<AddressBookData> receiveAddressValues = allReceivingAddresses.values();
            for (AddressBookData addressBookData : receiveAddressValues) {
                String columnOne = RECEIVE_ADDRESS_MARKER;
                String columnTwo = addressBookData.getAddress();
                String columnThree = addressBookData.getLabel();
                if (columnTwo == null) {
                    columnTwo = "";
                }
                out.write(columnOne + SEPARATOR + columnTwo + SEPARATOR + columnThree + "\n");
            }

            for (AddressBookData addressBookData : sendingAddresses) {
                String columnOne = SEND_ADDRESS_MARKER;
                String columnTwo = addressBookData.getAddress();
                String columnThree = addressBookData.getLabel();
                if (columnTwo == null) {
                    columnTwo = "";
                }
                out.write(columnOne + SEPARATOR + columnTwo + SEPARATOR + columnThree + "\n");
            }

            // Close the output stream
            out.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    public void loadFromFile() {
        try {
            // Read in the address book data
            FileInputStream fileInputStream = new FileInputStream(ADDRESS_BOOK_FILENAME);
            // Get the object of DataInputStream
            InputStream inputStream = new DataInputStream(fileInputStream);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, "UTF8"));
            String inputLine;

            // check the first line is what we expect
            String firstLine = bufferedReader.readLine();
            StringTokenizer tokenizer = new StringTokenizer(firstLine, SEPARATOR);
            int numberOfTokens = tokenizer.countTokens();
            if (numberOfTokens == 2) {
                String magicText = tokenizer.nextToken();
                String versionNumber = tokenizer.nextToken();
                if (!ADDRESSBOOK_MAGIC_TEXT.equals(magicText) || !VERSION_TEXT.equals(versionNumber)) {
                    // this is not an multibit address book
                    return;
                }
            } else {
                // this is not an multibit address book
                return;
            }

            // read the addresses
            while ((inputLine = bufferedReader.readLine()) != null) {
                StringTokenizer tokenizer2 = new StringTokenizer(inputLine, SEPARATOR);
                int numberOfTokens2 = tokenizer2.countTokens();
                String addressType = null;
                String address = null;
                String label = "";
                if (numberOfTokens2 == 2) {
                    addressType = tokenizer2.nextToken();
                    address = tokenizer2.nextToken();
                } else {
                    if (numberOfTokens2 == 3) {
                        addressType = tokenizer2.nextToken();
                        address = tokenizer2.nextToken();
                        label = tokenizer2.nextToken();
                    }
                }
                if (RECEIVE_ADDRESS_MARKER.equals(addressType)) {
                    addReceivingAddress(new AddressBookData(label, address), true);
                } else {
                    if (SEND_ADDRESS_MARKER.equals(addressType)) {
                        addSendingAddress(new AddressBookData(label, address));
                    }
                }
            }
            // Close the input stream
            inputStream.close();
        } catch (Exception e) {
            // Catch exception if any
            // may well not be a file - absorb exception
        }
    }
}