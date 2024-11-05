package org.example;

//class B

public class House implements IProperty, Comparable<House> {
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

    // Implementation of getDescription method
    @Override
    public String getDescription() {
        return "This house is owned by " + owner.getName() + " and has " + getPetNames() + ".";
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
        return owner; // Return the owner of the house
    }

    // Implementation of getAddress method
    @Override
    public String getAddress() {
        return address;
    }

    // Implementation of isGround method
    @Override
    public boolean isGround() {
        return isGround; // true if it's a ground-level house
    }

    // Floor method is not applicable for houses, so return 0 or -1
    @Override
    public int getFloor() {
        return isGround ? 0 : -1; // Ground-level house returns 0, others return -1
    }

    // Getter method to return the array of pets
    public Pet[] getPets() {
        return pets;
    }

    // Method to add a pet to the house
    public void addPet(Pet pet) {
        Pet[] newPets = new Pet[pets.length + 1];
        System.arraycopy(pets, 0, newPets, 0, pets.length);
        newPets[pets.length] = pet;
        pets = newPets;
    }

    // Method to remove a pet from the house
    public boolean removePet(Pet pet) {
        boolean found = false;
        Pet[] newPets = new Pet[pets.length - 1];
        int index = 0;
        for (Pet p : pets) {
            if (!p.equals(pet)) {
                if (index < newPets.length) {
                    newPets[index++] = p;
                }
            } else {
                found = true; // Found the pet to remove
            }
        }
        if (found) {
            pets = newPets; // Update the pets array
        }
        return found;
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

    @Override
    public int compareTo(House other) {
        return this.address.compareTo(other.address);
    }
}
