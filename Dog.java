package org.example;

//class D
public class Dog extends Pet implements IAnimal{
    private String breed;

    // Constructor to initialize the pet type and breed
    public Dog(String breed) {
        super("Dog");  // Set the pet type to Dog
        this.breed = breed;
    }


    // Implementation of getInfo from IAnimal interface
    @Override
    public String getInfo() {
        return "This is a " + breed + " dog."; // Returns a description of the dog
    }

    // Implementation of makeSound from IAnimal interface
    @Override
    public void makeSound() {
        System.out.println("Woof! Woof!"); // Print the sound a dog makes
    }

    // override getPetInfo to include more details
    @Override
    public String getPetInfo() {
        return getInfo(); // Call the getInfo method for description
    }
}
