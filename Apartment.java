package org.example;

import java.io.Serializable;

// Class E
public class Apartment implements IProperty, Comparable<Apartment> {
    private Person owner;      // The owner of the apartment
    private Pet[] pets;        // An array of pets associated with the apartment
    private String address;    // Address of the apartment
    private int floorNumber;   // Floor number of the apartment

    // Constructor to initialize the owner, floor number, and address
    public Apartment(Person owner, int floorNumber, String address, Pet[] pets) {
        this.owner = owner;
        this.floorNumber = floorNumber;
        this.address = address;
        this.pets = pets != null ? pets : new Pet[0]; // Initialize with provided pets or empty array
    }


    // Method to get the array of pets
    public Pet[] getPets() {
        return pets;
    }

    // Method to count the number of pets in the apartment
    public int countPets() {
        return pets.length;
    }

    // Implementation of getDescription method
    @Override
    public String getDescription() {
        return "This apartment is owned by " + owner.getName() + " and has " + getPetNames() + "."; // Use getPetNames() here
    }

    // Helper method to get pet names as a string
    private String getPetNames() {
        StringBuilder petNames = new StringBuilder();
        for (int i = 0; i < pets.length; i++) {
            petNames.append(pets[i].getPetType());
            if (i < pets.length - 1) {
                petNames.append(", "); // Add comma for all but last pet
            }
        }
        return petNames.toString();
    }

    public Person getOwner() {
        return owner; // Return the owner of the apartment
    }

    // Implementation of getAddress method
    @Override
    public String getAddress() {
        return address;
    }

    // All apartments are not ground-level houses
    @Override
    public boolean isGround() {
        return false; // Apartments cannot be ground-level houses
    }

    // Implementation of getFloor method
    @Override
    public int getFloor() {
        return floorNumber; // Return the floor number
    }

    // Implementation of compareTo method to compare based on address
    @Override
    public int compareTo(Apartment other) {
        return this.address.compareTo(other.address);
    }
}
