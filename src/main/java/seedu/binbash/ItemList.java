package seedu.binbash;

import seedu.binbash.item.Item;
import seedu.binbash.item.OperationalItem;
import seedu.binbash.item.PerishableOperationalItem;
import seedu.binbash.item.PerishableRetailItem;
import seedu.binbash.item.RetailItem;
import seedu.binbash.command.RestockCommand;
import seedu.binbash.logger.BinBashLogger;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.logging.Logger;
import java.util.logging.Level;

public class ItemList {
    private static final Logger ITEMLIST_LOGGER = Logger.getLogger("ItemList");
    private static final BinBashLogger logger = new BinBashLogger(ItemList.class.getName());
    private double totalRevenue;
    private double totalCost;
    private final List<Item> itemList;

    public ItemList(ArrayList<Item> itemList) {
        this.itemList = itemList;
        ITEMLIST_LOGGER.setLevel(Level.WARNING);
        this.totalRevenue = 0;
        this.totalCost = 0;
    }

    private double getTotalRevenue() {
        double totalRevenue = 0;

        for (Item item: itemList) {
            if (item instanceof RetailItem) {
                // Downcast made only after checking if item is a RetailItem (and below) object.
                // TODO: Add an assert statement to verify code logic.
                RetailItem retailItem = (RetailItem) item;
                totalRevenue += (retailItem.getTotalUnitsSold() * retailItem.getItemSalePrice());
            }
        }

        return totalRevenue;
    }

    private double getTotalCost() {
        double totalCost = 0;

        for (Item item: itemList) {
            totalCost += (item.getTotalUnitsPurchased() * item.getItemCostPrice());
        }

        return totalCost;
    }

    public String getProfitMargin() {
        double totalCost = getTotalCost();
        double totalRevenue = getTotalRevenue();
        double netProfit = totalRevenue - totalCost;

        String output =
                String.format("Here are your metrics: " + System.lineSeparator()
                        + "\tTotal Cost: %.2f" + System.lineSeparator()
                        + "\tTotal Revenue: %.2f" + System.lineSeparator()
                        + "\tNet Profit: %.2f" + System.lineSeparator(),
                        totalCost,
                        totalRevenue,
                        netProfit);
        return output;
    }

    public List<Item> getItemList() {
        return itemList;
    }

    public int getItemCount() {
        return itemList.size();
    }

    public String addItem(String itemType, String itemName, String itemDescription, int itemQuantity,
                          LocalDate itemExpirationDate, double itemSalePrice, double itemCostPrice) {
        Item item;
        if (itemType.equals("retail") && !itemExpirationDate.equals(LocalDate.MIN)) {
            // Perishable Retail Item
            item = new PerishableRetailItem(itemName, itemDescription, itemQuantity,
                    itemExpirationDate, itemSalePrice, itemCostPrice);
        } else if (itemType.equals("retail") && itemExpirationDate.equals(LocalDate.MIN)) {
            // Non-perishable Retail Item
            item = new RetailItem(itemName, itemDescription, itemQuantity, itemSalePrice, itemCostPrice);
        } else if (itemType.equals("operational") && !itemExpirationDate.equals(LocalDate.MIN)) {
            // Perishable Operational Item
            item = new PerishableOperationalItem(itemName, itemDescription, itemQuantity,
                    itemExpirationDate, itemCostPrice);
        } else {
            // Non-perishable Operational Item
            item = new OperationalItem(itemName, itemDescription, itemQuantity, itemCostPrice);
        }

        int beforeSize = itemList.size();
        itemList.add(item);
        assert itemList.size() == (beforeSize + 1);

        String output = "Noted! I have added the following item into your inventory:" + System.lineSeparator()
                + System.lineSeparator() + item;
        return output;
    }

    public String updateItemQuantity(String itemName, int itemQuantity, String command) {
        String output = "Sorry, I can't find the item you are looking for.";

        for (Item item : itemList) {
            int newQuantity = item.getItemQuantity();

            if (!item.getItemName().trim().equals(itemName.trim())) {
                continue;
            }

            // Restocking item consists of (i) Updating itemQuantity, (ii) Updating totalUnitsPurchased
            if (command.trim().equals(RestockCommand.COMMAND.trim())) {
                newQuantity += itemQuantity;

                int totalUnitsPurchased = item.getTotalUnitsPurchased();
                item.setTotalUnitsPurchased(totalUnitsPurchased + itemQuantity);

            // Selling item consists of (i) Updating itemQuantity, (ii) Updating totalUnitsSold
            } else {
                newQuantity -= itemQuantity;

                // Stinky downcast?
                // I'm going off the assertion that only retail items (and its subclasses) can be sold.
                // TODO: Add an assert statement to verify code logic.
                RetailItem retailItem = (RetailItem)item;

                int totalUnitsSold = retailItem.getTotalUnitsSold();
                retailItem.setTotalUnitsSold(totalUnitsSold + itemQuantity);
            }
            item.setItemQuantity(newQuantity);
            output = "Great! I have updated the quantity of the item for you:" + System.lineSeparator()
                    + System.lineSeparator() + item;

        }
        return output;
    }

    public String deleteItem(int index) {
        logger.info("Attempting to delete an item");
        int beforeSize = itemList.size();
        Item tempItem = itemList.remove(index - 1);
        assert itemList.size() == (beforeSize - 1);

        String output = "Got it! I've removed the following item:" + System.lineSeparator()
                + System.lineSeparator() + tempItem;
        logger.info("An item has been deleted");
        return output;
    }

    public String deleteItem(String keyword) {
        int targetIndex = -1;
        Item currentItem;
        for (int i = 0 ; i < itemList.size(); i ++) {
            currentItem = itemList.get(i);
            if (currentItem.getItemName().trim().equals(keyword)) {
                ITEMLIST_LOGGER.log(Level.INFO, "first matching item at index " + i + " found.");
                targetIndex = i + 1;
                break;
            }
        }

        if (targetIndex == -1) {
            String output = "Item not found! Nothing was deleted!";
            return output;
        }

        return deleteItem(targetIndex);
    }

    public ArrayList<Item> searchItemList(String nameField, String descriptionField,
            double costPriceField, double salePriceField,
            LocalDate expiryDateField, int numberOfResults) {
        ArrayList<Item> filteredList = (ArrayList<Item>) itemList.stream() // filter through mandatory fields first
                .filter(item -> item.getItemName().contains(nameField))
                .filter(item -> item.getItemDescription().contains(descriptionField))
                .filter(item -> (costPriceField < 0) ? item.getItemCostPrice() < (-1 * costPriceField) : 
                        item.getItemCostPrice() > costPriceField)
                // then filter through optional fields
                .filter(item -> (salePriceField < 0) ?
                        item instanceof RetailItem && ((RetailItem) item).getItemSalePrice() < (-1 * salePriceField) :
                        salePriceField == 0 ||
                         (item instanceof RetailItem && ((RetailItem) item).getItemSalePrice() > salePriceField))
                .filter(item -> item instanceof PerishableRetailItem ?
                        LocalDate.parse(((PerishableRetailItem) item).getItemExpirationDate(),
                            DateTimeFormatter.ofPattern("dd-MM-yyyy"))
                        .isBefore(expiryDateField) : true)
                .limit(numberOfResults)
                .collect(Collectors.toList());
        assert filteredList.size() <= numberOfResults;
        return filteredList;
    }

    /**
     * Returns a string representation of all the items in the list. Each item's string
     * representation is obtained by calling its `toString` method.
     *
     * @return A concatenated string of all item representations in the list, each on a new line.
     */
    public String printList(List<Item> itemList) {
        int index = 1;
        String output = "";

        for (Item item: itemList) {
            output += index + ". " + item.toString() + System.lineSeparator() + System.lineSeparator();
            index++;
        }

        return output;
    }

    @Override
    public String toString() {
        return itemList.toString();
    }
}
