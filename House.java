//class B
package org.example;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class House implements IProperty, Comparable<House>, Serializable {
    private Person owner;          // The owner of the house
    private Pet[] pets;            // An array of pets associated with the house
    private String address;        // Address of the house
    private boolean isGround;      // Indicates if it's a ground-level house

    // Constructor to initialize the owner, pets, address, and ground status
    public House(Person owner, Pet[] pets, String address, boolean isGround) {
        this.owner = owner;
        this.pets = pets;
        this.address = address;
        this.isGround = isGround;
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

        return "The house on " + address + " is owned by " + owner.getName() + ". "
                + (isGround ? "It is on ground level" : "It is not on ground level") + ". "
                + owner.getName() + " has " + petDescription.toString() + ".";
    }

    // Helper method to get pet names as a string (not used in the main description method)
    private String getPetNames() {
        StringBuilder petNames = new StringBuilder();
        for (int i = 0; i < pets.length; i++) {
            petNames.append(pets[i].getPetType());
            if (i < pets.length - 1) {
                petNames.append(", "); // Add comma for all but the last pet
            }
        }
        return petNames.toString();
    }

    public Person getOwner() {
        return owner;
    }

    @Override
    public String getAddress() {
        return address;
    }

    // Indicates if the house is on the ground level
    @Override
    public boolean isGround() {
        return isGround;
    }

    // Return 0 for ground level, or -1 if not applicable
    @Override
    public int getFloor() {
        return isGround ? 0 : -1;
    }

    public Pet[] getPets() {
        return this.pets;  // Return the pets array
    }

    // Override toString for easy printing of house details
    @Override
    public String toString() {
        return "House{" +
                "owner=" + owner.getName() +
                ", address='" + address + '\'' +
                ", isGround=" + isGround +
                ", numberOfPets=" + pets.length +
                '}';
    }

    // Implementing compareTo based on the starting number of the address
    @Override
    public int compareTo(House other) {
        // Extract the starting number from each address (before the first space)
        int num1 = Integer.parseInt(this.address.split(" ")[0]);
        int num2 = Integer.parseInt(other.address.split(" ")[0]);

        // Compare based on the extracted numbers
        return Integer.compare(num1, num2);
    }

}
