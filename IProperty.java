package org.example;

public interface IProperty {
    String getDescription(); // Describe the property
    String getAddress();     // Get the address of the property
    boolean isGround();      // Check if the property is a ground-level house or apartment
    int getFloor();          // Get the floor number (applicable for apartments)

    Person owner = null;
    public default Person getOwner() {
        return owner;
    }
}
