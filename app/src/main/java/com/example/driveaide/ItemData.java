package com.example.driveaide;

public class ItemData {
    String itemName;
    String itemValue;

    ItemData(String itemName, String itemValue) {
        this.itemName = itemName;
        this.itemValue = itemValue;
    }
    void changeValue(String itemValue) {
        this.itemValue = itemValue;
    }

    public String toString() {
        return ("(" + itemName + ", " + itemValue + ")");
    }
}
