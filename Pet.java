package org.example;

// Class C: Represents a pet with a type (e.g., dog, cat)
class Pet {
    private String petType; // Type of the pet (e.g., Dog, Cat)

    // Constructor to initialize the pet type
    public Pet(String petType) {
        this.petType = petType;
    }

    // Getter for the pet type
    public String getPetType() {
        return petType;
    }

    // Method to get pet information
    public String getPetInfo() {
        return "Pet is a " + petType; // Returns a general description of the pet
    }
}
