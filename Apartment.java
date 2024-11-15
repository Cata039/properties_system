// Class E
package org.example;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Apartment implements IProperty, Comparable<Apartment>, Serializable {
    private Person owner;      // The owner of the apartment
    private Pet[] pets;        // An array of pets associated with the apartment
    private String address;    // Address of the apartment
    private int floor;         // Floor number of the apartment

    // Constructor to initialize the owner, floor number, and address
    public Apartment(Person owner, int floorNumber, String address, Pet[] pets) {
        this.owner = owner;
        this.floor = floorNumber;
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

    @Override
    public String getDescription() {
        StringBuilder petDescription = new StringBuilder();
        Map<String, Integer> petCount = new HashMap<>();

        // Count pets by type
        for (Pet pet : pets) {
            petCount.put(pet.getPetType(), petCount.getOrDefault(pet.getPetType(), 0) + 1);
        }

        // Format pet description
        for (Map.Entry<String, Integer> entry : petCount.entrySet()) {
            petDescription.append(entry.getValue()).append(" ").append(entry.getKey());
            if (entry.getValue() > 1) petDescription.append("s"); // Pluralize if more than one
            petDescription.append(", ");
        }

        // Remove the trailing comma and space
        if (petDescription.length() > 0) {
            petDescription.setLength(petDescription.length() - 2);
        } else {
            petDescription.append("no pets");
        }

        return "The apartment on " + address + ", floor " + floor + " is owned by "
                + owner.getName() + ". " + owner.getName() + " has " + petDescription.toString() + ".";
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
        return floor; // Return the floor number
    }

    // Implementation of compareTo method to compare based on floor number
    @Override
    public int compareTo(Apartment other) {
        return Integer.compare(this.floor, other.floor);
    }

}
