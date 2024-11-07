// Class C
package org.example;

public class Pet implements IAnimal {
    private String petType;

    public Pet(String petType) {
        this.petType = petType;
    }

    public String getPetType() {
        return petType;
    }

    @Override
    public void makeSound() {
        // Define sounds based on the pet type
        switch (petType.toLowerCase()) {
            case "dog":
                System.out.println("Dog: Woof woof");
                break;
            case "fish":
                // Fish are silent, so we print nothing or a message like "Fish is silent"
                System.out.println("Fish: silent");
                break;
            default:
                System.out.println(petType + " makes a sound!");
                break;
        }
    }

    @Override
    public String getInfo() {
        return "Pet type: " + petType;
    }
}