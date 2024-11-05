package org.example;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main {
    // Function to describe the house
    public static void describeHouse(House house) {
        System.out.println(house.getDescription());
    }

    // Function to count the number of pets
    public static int countPets(IProperty property) {
        if (property instanceof House) {
            return ((House) property).getPets().length; // Cast to House and count pets
        } else if (property instanceof Apartment) {
            return ((Apartment) property).countPets(); // Call countPets on Apartment
        }
        return 0; // If neither, return 0
    }

    // Function to sort properties by floor
    public static List<IProperty> sortPropertiesByFloor(List<IProperty> properties) {
        properties.sort((p1, p2) -> {
            // Handle sorting for House
            int floor1 = (p1 instanceof House) ? 0 : ((Apartment) p1).getFloor(); // Assume House is ground level
            int floor2 = (p2 instanceof House) ? 0 : ((Apartment) p2).getFloor(); // Assume House is ground level

            // Adjust comparison to account for House
            if (p1 instanceof House) {
                return (p2 instanceof House) ? 0 : -1; // All Houses come before Apartments
            } else if (p2 instanceof House) {
                return 1; // All Houses come before Apartments
            } else {
                return Integer.compare(floor1, floor2); // Compare floors of Apartments
            }
        });
        return properties;
    }

    // Function to group properties by owner
    public static Map<String, List<IProperty>> groupPropertiesByOwner(List<IProperty> properties) {
        Map<String, List<IProperty>> ownerGroups = new HashMap<>();
        for (IProperty property : properties) {
            String ownerName = property instanceof House
                    ? ((House) property).getOwner().getName()
                    : ((Apartment) property).getOwner().getName();

            ownerGroups.computeIfAbsent(ownerName, k -> new ArrayList<>()).add(property);
        }
        return ownerGroups;
    }

    // Function to count pets by type
    public static Map<String, Integer> countPetsByType(List<IProperty> properties) {
        Map<String, Integer> petCount = new HashMap<>();
        for (IProperty property : properties) {
            Pet[] pets = property instanceof House
                    ? ((House) property).getPets()
                    : ((Apartment) property).getPets();

            for (Pet pet : pets) {
                String petType = pet.getPetType();
                petCount.put(petType, petCount.getOrDefault(petType, 0) + 1);
            }
        }
        return petCount;
    }

    public static void main(String[] args) {
        // Create some people
        Person alice = new Person("Alice");
        Person bob = new Person("Bob");

        Pet[] aliceHousePets = {new Pet("Dog"), new Pet("Dog"), new Pet("Fish")};
        Pet[] aliceApartmentPets = {new Pet("Fish")}; // Alice's apartment pet
        Pet[] bobPets = {new Pet("Dog"), new Pet("Fish")}; // John's pets

        // Create houses and apartments
        House aliceHouse = new House(alice, aliceHousePets, "123 Main St", true);
        Apartment aliceApartment = new Apartment(alice, 6, "789 High St", aliceApartmentPets);
        Apartment bobApartment = new Apartment(bob, 5, "456 Oak St", bobPets);

        // Store properties in a list
        List<IProperty> properties = new ArrayList<>();
        properties.add(aliceHouse);
        properties.add(aliceApartment);
        properties.add(bobApartment);

        // Check which function to call based on the argument
        if (args.length > 0) {
            String command = args[0].toLowerCase();

            switch (command) {
                case "describe":
                    System.out.println("Description for Alice's house:");
                    System.out.println(aliceHouse.getDescription());
                    System.out.println("Address: " + aliceHouse.getAddress());
                    System.out.println("Is ground-level: " + aliceHouse.isGround());
                    System.out.println("Floor: " + aliceHouse.getFloor());
                    System.out.println();

                    System.out.println("Description for Alice's apartment:");
                    System.out.println(aliceApartment.getDescription());
                    System.out.println("Address: " + aliceApartment.getAddress());
                    System.out.println("Floor: " + aliceApartment.getFloor());
                    System.out.println();

                    System.out.println("Description for Bob's apartment:");
                    System.out.println(bobApartment.getDescription());
                    System.out.println("Address: " + bobApartment.getAddress());
                    System.out.println("Floor: " + bobApartment.getFloor());
                    break;

                case "count":
                    int numOfAliceHousePets = countPets(aliceHouse);
                    System.out.println("Number of pets in Alice's house: " + numOfAliceHousePets);

                    int numOfAliceApartmentPets = countPets(aliceApartment);
                    System.out.println("Number of pets in Alice's apartment: " + numOfAliceApartmentPets);

                    int numOfBobPets = countPets(bobApartment);
                    System.out.println("Number of pets in Bob's apartment: " + numOfBobPets);
                    break;

                case "sort":
                    sortPropertiesByFloor(properties);
                    System.out.println("Properties sorted by floor:");
                    for (IProperty property : properties) {
                        System.out.println(property.getDescription() + " on floor " + (property instanceof House ? "ground" : ((Apartment) property).getFloor()));
                    }
                    break;

                case "group":
                    Map<String, List<IProperty>> ownerGroups = groupPropertiesByOwner(properties);
                    System.out.println("Properties grouped by owner:");
                    for (String owner : ownerGroups.keySet()) {
                        System.out.println("Owner: " + owner);
                        for (IProperty property : ownerGroups.get(owner)) {
                            System.out.println("  - " + property.getDescription() + " at " + property.getAddress());
                        }
                    }
                    break;

                case "countpets":
                    Map<String, Integer> petCounts = countPetsByType(properties);
                    System.out.println("Pet count by type:");
                    for (String petType : petCounts.keySet()) {
                        System.out.println(petType + ": " + petCounts.get(petType));
                    }
                    break;

                default:
                    System.out.println("Invalid command. Please specify 'describe', 'count', 'sort', 'group', or 'countpets' as a command line argument.");
                    break;
            }
        } else {
            System.out.println("Please specify 'describe', 'count', 'sort', 'group', or 'countpets' as a command line argument.");
        }

        // Sort properties by address
        properties.sort(Comparator.comparing(IProperty::getAddress));

        // Get the property with the smallest address
        IProperty smallestAddressProperty = properties.get(0);
        System.out.println("The property with the smallest address is " + smallestAddressProperty.getDescription() + " at " + smallestAddressProperty.getAddress());


    }
}
