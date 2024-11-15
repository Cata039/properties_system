package org.example;

import java.io.FileNotFoundException;
import org.json.JSONArray;
import org.json.JSONObject;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.IOException;
import java.util.*;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.io.FileWriter;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
            if (p1 instanceof House && p2 instanceof House) {
                return 0; // Both are Houses, considered equal
            } else if (p1 instanceof House) {
                return -1; // House comes first
            } else if (p2 instanceof House) {
                return 1; // House comes last
            } else {
                // Both are Apartments, use compareTo method
                Apartment apartment1 = (Apartment) p1;
                Apartment apartment2 = (Apartment) p2;
                return apartment1.compareTo(apartment2);
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

            // Group properties by owner name
            ownerGroups.computeIfAbsent(ownerName, k -> new ArrayList<>()).add(property);
        }

        // Now, sort the properties for each owner
        for (Map.Entry<String, List<IProperty>> entry : ownerGroups.entrySet()) {
            List<IProperty> propertiesForOwner = entry.getValue();

            // Sort the properties within each owner's list
            propertiesForOwner.sort((p1, p2) -> {
                if (p1 instanceof House && p2 instanceof House) {
                    // Use the compareTo method of House to compare by address
                    return ((House) p1).compareTo((House) p2);
                }
                // Default comparison for Apartment (optional, based on other criteria)
                else if (p1 instanceof Apartment && p2 instanceof Apartment) {
                    return ((Apartment) p1).compareTo((Apartment) p2);
                }
                return 0; // No sorting needed for mixed types or when one of the properties is not comparable
            });
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

    // Function to group properties by pet type
    public static Map<String, List<IProperty>> groupPropertiesByPetType(List<IProperty> properties) {
        // Create a map to group properties by pet type
        Map<String, List<IProperty>> petTypeGroups = new HashMap<>();

        for (IProperty property : properties) {
            // Determine the pets for this property (based on the property type)
            Pet[] pets = null;

            // Check if it's a House or Apartment and retrieve pets accordingly
            if (property instanceof House) {
                pets = ((House) property).getPets();
            } else if (property instanceof Apartment) {
                pets = ((Apartment) property).getPets();
            }

            // If pets are found, group properties by pet type
            if (pets != null) {
                for (Pet pet : pets) {
                    // Get the pet type
                    String petType = pet.getPetType();

                    // Add property to the corresponding pet type group
                    petTypeGroups.computeIfAbsent(petType, k -> new ArrayList<>()).add(property);
                }
            }
        }

        return petTypeGroups;
    }

    public static void validateAddress(String address) throws InvalidAddressFormatException {
        String addressPattern = "^\\d+\\s+[A-Za-z]+$";
        if (!address.matches(addressPattern)) {
            throw new InvalidAddressFormatException("Invalid address format. Please enter the address in the format '123 Abc'.");
        }
    }

    public static Date validateAndParseDate(String dateStr) throws ParseException {
        // First, attempt to parse the date using the specified format
        try {
            Date date = dateFormat.parse(dateStr);
            // Additional date validation logic can be added here if needed (e.g., check for logical validity)
            return date;
        } catch (ParseException e) {
            throw new ParseException("Invalid date provided. Please check the date format and ensure it is a valid date.", 0);
        }
    }



    private static final String DATA_FILE = "data.json";
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    List<Lease> leases = loadLeasesFromJson(DATA_FILE);

    public static void main(String[] args) {
        // Load initial data from JSON files
        List<Person> persons = loadPersonsFromJson(DATA_FILE);
        List<IProperty> properties = loadPropertiesFromJson(DATA_FILE);
        List<Lease> leases = loadLeasesFromJson(DATA_FILE);

        Scanner scanner = new Scanner(System.in);
        if (args.length > 0) {
            String command = args[0].toLowerCase();

            switch (command) {
                case "describe":
                    for (IProperty property : properties) {
                        System.out.println(property.getDescription());
                    }
                    break;
                case "count":
                    System.out.println("Number of pets:");
                    for (IProperty property : properties) {
                        String address = property.getAddress();
                        int numberOfPets = countPets(property);
                        System.out.println(address + ": " + numberOfPets);
                    }
                    break;
                case "sort":
                    sortPropertiesByFloor(properties);
                    System.out.println("Properties sorted by floor:");
                    for (IProperty property : properties) {
                        String address = property.getAddress();
                        String owner = property instanceof House ? ((House) property).getOwner().getName() : ((Apartment) property).getOwner().getName();
                        String floorDescription = property instanceof House ? "ground" : String.valueOf(((Apartment) property).getFloor());
                        String typeDescription = property instanceof House ? "The house on " : "The apartment on ";

                        System.out.println(typeDescription + address + " owned by " + owner + " - floor " + floorDescription);
                    }
                    break;
                case "group":
                    Map<String, List<IProperty>> ownerGroups = groupPropertiesByOwner(properties);
                    System.out.println("Properties grouped by owner:");
                    for (String owner : ownerGroups.keySet()) {
                        System.out.println("Owner: " + owner);
                        for (IProperty property : ownerGroups.get(owner)) {
                            System.out.println("  - " + property.getAddress());
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
                case "group_by_pet":
                    // Group properties by pet type
                    Map<String, List<IProperty>> petGroups = groupPropertiesByPetType(properties);

                    System.out.println("Properties grouped by pet type:");

                    // Iterate through the pet groups and print out the properties for each pet type
                    for (String petType : petGroups.keySet()) {
                        System.out.println(petType + ":");

                        // For each pet type, print the properties that have this pet type
                        for (IProperty property : petGroups.get(petType)) {
                            System.out.println(" - " + property.getAddress());
                        }
                    }
                    break;
                case "smallest_adr":
                    // Sort properties by address using the compareTo method of House
                    properties.sort((p1, p2) -> {
                        // Check if both properties are instances of House
                        if (p1 instanceof House && p2 instanceof House) {
                            return ((House) p1).compareTo((House) p2); // Use the compareTo method to compare addresses
                        }
                        return 0; // If the properties are not houses, do nothing
                    });

                    // Find the property with the smallest address after sorting
                    IProperty smallestAddressProperty = properties.get(0); // Get the first element (smallest address)
                    System.out.println("The property with the smallest address is " + smallestAddressProperty.getAddress() + ". " + smallestAddressProperty.getDescription());
                    break;
                case "makesound":
                    Set<String> uniquePetTypes = new HashSet<>();

                    System.out.println("Animals making sounds:");
                    for (IProperty property : properties) {
                        Pet[] pets = property instanceof House
                                ? ((House) property).getPets()
                                : ((Apartment) property).getPets();

                        for (Pet pet : pets) {
                            String petType = pet.getPetType().toLowerCase();
                            if (!uniquePetTypes.contains(petType)) {
                                pet.makeSound();
                                uniquePetTypes.add(petType);
                            }
                        }
                    }
                    break;
                case "contract":
                    if (leases.isEmpty()) {
                        System.out.println("No leases available to display or terminate.");
                        break;
                    }

                    System.out.println("Lease Details:");
                    for (Lease lease : leases) {
                        System.out.println("Tenant Name: " + lease.getTenantName());
                        System.out.println("Start Date: " + lease.getStartDate());
                        System.out.println("End Date: " + lease.getEndDate());
                        System.out.println("Monthly Rent: $" + lease.getMonthlyRent());
                        System.out.println();

                    }
                    saveToJson(persons, properties, leases);
                    break;

                case "add":
                    while (true) {
                        System.out.println("What would you like to add? (property/lease) or type 'exit' to go back: ");
                        String addType = scanner.nextLine().trim().toLowerCase();

                        if (addType.equals("exit")) {
                            System.out.println("Exiting add mode.");
                            break;
                        }

                        switch (addType) {
                            case "property":
                                System.out.println("Enter property type (house/apartment):");
                                String propertyType = scanner.nextLine().toLowerCase();

                                // Validate property type input to be either house or apartment
                                while (!propertyType.equals("house") && !propertyType.equals("apartment")) {
                                    System.out.println("Invalid property type. Please enter 'house' or 'apartment':");
                                    propertyType = scanner.nextLine().toLowerCase();
                                }

                                System.out.println("Enter owner name:");
                                String ownerName = scanner.nextLine();

                                // Reuse or add the owner as a person
                                Person owner = persons.stream().filter(p -> p.getName().equalsIgnoreCase(ownerName))
                                        .findFirst()
                                        .orElseGet(() -> {
                                            Person newPerson = new Person(ownerName);
                                            persons.add(newPerson); // Add the new owner to persons
                                            return newPerson;
                                        });

                                // Validate address format
                                String address;
                                while (true) {
                                    System.out.println("Enter property address in the format '123 Abc':");
                                    address = scanner.nextLine();
                                    try {
                                        validateAddress(address); // Validate address format
                                        break; // Exit loop if address is valid
                                    } catch (InvalidAddressFormatException e) {
                                        System.out.println(e.getMessage()); // Display custom error message
                                    }
                                }

                                // Validate pet count to ensure it's a valid number
                                int petCount = 0;
                                while (true) {
                                    System.out.println("Enter number of pets:");
                                    try {
                                        petCount = Integer.parseInt(scanner.nextLine());
                                        if (petCount < 0) {
                                            System.out.println("Please enter a valid number greater than or equal to 0.");
                                            continue;
                                        }
                                        break; // Exit loop if petCount is valid
                                    } catch (NumberFormatException e) {
                                        System.out.println("Invalid input. Please enter a number.");
                                    }
                                }

                                Pet[] pets = new Pet[petCount];
                                for (int i = 0; i < petCount; i++) {
                                    System.out.println("Enter pet type for pet " + (i + 1) + ":");
                                    String petType = scanner.nextLine();
                                    pets[i] = new Pet(petType);
                                }

                                if (propertyType.equals("house")) {
                                    // Automatically set the ground level to true for houses
                                    boolean isGround = true; // Ground level is always true for houses
                                    properties.add(new House(owner, pets, address, isGround));
                                } else if (propertyType.equals("apartment")) {
                                    // Validate the floor number between 0 and 130
                                    int floor = -1;
                                    while (floor < 0 || floor > 130) {
                                        System.out.println("Enter floor number (0-130):");
                                        try {
                                            floor = Integer.parseInt(scanner.nextLine());
                                            if (floor < 0 || floor > 130) {
                                                System.out.println("Please enter a floor number between 0 and 130.");
                                            }
                                        } catch (NumberFormatException e) {
                                            System.out.println("Invalid input. Please enter a valid floor number.");
                                        }
                                    }
                                    properties.add(new Apartment(owner, floor, address, pets));
                                }
                                break;

                                case "lease":
                                System.out.println("Enter tenant name:");
                                String leaseTenantName = scanner.nextLine();

                                    // Check if tenant exists in the persons list
                                    Person tenant = persons.stream()
                                            .filter(p -> p.getName().equalsIgnoreCase(leaseTenantName))
                                            .findFirst()
                                            .orElse(null); // If tenant doesn't exist, this will return null

                                    if (tenant == null) {
                                        System.out.println("Tenant does not exist. Please use the 'property' command first to register the tenant.");
                                        break;  // Exit the lease case and return to the main loop
                                    }

                                    // Check if the tenant has a property assigned to them
                                    boolean tenantHasProperty = properties.stream()
                                            .anyMatch(p -> p.getOwner().getName().equalsIgnoreCase(tenant.getName())); // Compare the tenant's name with the owner's name

                                    if (!tenantHasProperty) {
                                        System.out.println("Tenant does not own any property. Please assign a property to the tenant first.");
                                        break; // Exit the lease case if the tenant has no property
                                    }

                                    System.out.println("Enter property address:");
                                    String propertyAddress = scanner.nextLine();

                                    // If the property does not exist in the system, throw PropertyNotFoundException
                                    boolean propertyExists = properties.stream()
                                            .anyMatch(p -> p.getAddress().equalsIgnoreCase(propertyAddress));  // Case insensitive comparison

                                    if (!propertyExists) {
                                        // Property does not exist
                                        try {
                                            throw new PropertyNotFoundException(propertyAddress);  // Throw the custom exception
                                        } catch (PropertyNotFoundException e) {
                                            System.out.println(e.getMessage());  // Display the message from the exception
                                            break;  // Exit the lease case
                                        }
                                    }

                                    System.out.println("Enter monthly rent:");
                                    double monthlyRent = Double.parseDouble(scanner.nextLine());

                                    Date leaseStartDate = null;
                                    Date leaseEndDate = null;

                                    // Loop to ensure valid lease start date input
                                    while (leaseStartDate == null) {
                                        try {
                                            System.out.println("Enter lease start date (yyyy-MM-dd):");
                                            leaseStartDate = validateAndParseDate(scanner.nextLine());  // Try to parse the date
                                        } catch (ParseException e) {
                                            System.out.println("Invalid date format. Please enter dates in yyyy-MM-dd format.");
                                        }
                                    }

                                    // Loop to ensure valid lease end date input
                                    while (leaseEndDate == null) {
                                        try {
                                            System.out.println("Enter lease end date (yyyy-MM-dd):");
                                            leaseEndDate = validateAndParseDate(scanner.nextLine());  // Try to parse the date
                                        } catch (ParseException e) {
                                            System.out.println("Invalid date format. Please enter dates in yyyy-MM-dd format.");
                                        }
                                    }

                                    try {
                                    // Check if the lease overlaps with existing leases for the same property
                                    for (Lease existingLease : leases) {
                                        if (existingLease.getPropertyAddress().equals(propertyAddress)) {
                                            Date existingStartDate = existingLease.getStartDate();
                                            Date existingEndDate = existingLease.getEndDate();

                                            // Check for overlap: new lease starts before existing lease ends and ends after it starts
                                            if (!(leaseEndDate.before(existingStartDate) || leaseStartDate.after(existingEndDate))) {
                                                throw new OverlappingLeaseException("Cannot lease property at " + propertyAddress
                                                        + " for the same period. New lease must start after the existing lease ends.");
                                            }
                                        }
                                    }

                                    // Add the new lease if no overlap
                                    leases.add(new Lease(tenant.getName(), leaseStartDate, leaseEndDate, monthlyRent, propertyAddress));
                                    saveToJson(persons, properties, leases);
                                    System.out.println("Lease added.");
                                } catch (OverlappingLeaseException e) {
                                    System.out.println("Lease error: " + e.getMessage());
                                } catch (IllegalArgumentException e) {
                                    System.out.println("Invalid date provided. Please check the date format and ensure it is a valid date.");
                                }
                                    break;
                        }
                    }
                    break;
                default:
                    System.out.println("Invalid command. Please specify 'describe', 'count', 'sort', 'group', 'countpets', 'smallestadr, or 'contract' as a command line argument.");
                    break;
            }
        } else {
            System.out.println("Please specify 'describe', 'count', 'sort', 'group', or 'countpets' as a command line argument.");
        }
    }

    private static List<Person> loadPersonsFromJson(String fileName) {
        List<Person> persons = new ArrayList<>();
        try {
            String content = new String(Files.readAllBytes(Paths.get(fileName)));
            JSONObject jsonObject = new JSONObject(content);
            JSONArray personArray = jsonObject.getJSONArray("persons");
            for (int i = 0; i < personArray.length(); i++) {
                JSONObject personObject = personArray.getJSONObject(i);
                persons.add(new Person(personObject.getString("name")));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return persons;
    }

    private static List<IProperty> loadPropertiesFromJson(String fileName) {
        List<IProperty> properties = new ArrayList<>();
        try {
            String content = new String(Files.readAllBytes(Paths.get(fileName)));
            JSONObject jsonObject = new JSONObject(content);
            JSONArray propertyArray = jsonObject.getJSONArray("properties");

            for (int i = 0; i < propertyArray.length(); i++) {
                JSONObject propertyObject = propertyArray.getJSONObject(i);
                String type = propertyObject.getString("type");
                String address = propertyObject.getString("address");
                Person owner = new Person(propertyObject.getString("owner"));

                JSONArray petsArray = propertyObject.optJSONArray("pets");
                Pet[] pets = new Pet[petsArray != null ? petsArray.length() : 0];
                if (petsArray != null) {
                    for (int j = 0; j < petsArray.length(); j++) {
                        pets[j] = new Pet(petsArray.getJSONObject(j).getString("type"));
                    }
                }

                if (type.equals("House")) {
                    properties.add(new House(owner, pets, address, propertyObject.getBoolean("occupied")));
                } else if (type.equals("Apartment")) {
                    properties.add(new Apartment(owner, propertyObject.getInt("floor"), address, pets));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return properties;
    }

    private static List<Lease> loadLeasesFromJson(String fileName) {
        List<Lease> leases = new ArrayList<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

        try {
            String content = new String(Files.readAllBytes(Paths.get(fileName)));
            JSONObject jsonObject = new JSONObject(content);
            JSONArray leaseArray = jsonObject.getJSONArray("leases");

            for (int i = 0; i < leaseArray.length(); i++) {
                JSONObject leaseObject = leaseArray.getJSONObject(i);

                // Extract fields with default values if missing
                String tenant = leaseObject.optString("tenant", "Unknown Tenant");
                Date startDate = dateFormat.parse(leaseObject.optString("startDate", "1900-01-01"));
                Date endDate = dateFormat.parse(leaseObject.optString("endDate", "1900-01-01"));
                double monthlyRent = leaseObject.optDouble("monthlyRent", 0.0);
                String propertyAddress = leaseObject.optString("propertyAddress", "Unknown Address"); // Extract property address

                // Pass all five arguments to the Lease constructor
                leases.add(new Lease(tenant, startDate, endDate, monthlyRent, propertyAddress));
            }
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
        return leases;
    }




    // Method to save the updated data back to the JSON file
    private static void saveToJson(List<Person> persons, List<IProperty> properties, List<Lease> leases) {
        JSONObject jsonObject = new JSONObject();

        // Convert persons to JSON
        JSONArray personArray = new JSONArray();
        for (Person person : persons) {
            JSONObject personObject = new JSONObject();
            personObject.put("name", person.getName());
            personArray.put(personObject);
        }
        jsonObject.put("persons", personArray);

        // Convert properties to JSON
        JSONArray propertyArray = new JSONArray();
        for (IProperty property : properties) {
            JSONObject propertyObject = new JSONObject();
            if (property instanceof House) {
                House house = (House) property;
                propertyObject.put("type", "House");
                propertyObject.put("address", house.getAddress());
                propertyObject.put("owner", house.getOwner().getName());
                propertyObject.put("occupied", house.isGround());
                JSONArray petArray = new JSONArray();
                for (Pet pet : house.getPets()) {
                    JSONObject petObject = new JSONObject();
                    petObject.put("type", pet.getPetType());
                    petArray.put(petObject);
                }
                propertyObject.put("pets", petArray);
            } else if (property instanceof Apartment) {
                Apartment apartment = (Apartment) property;
                propertyObject.put("type", "Apartment");
                propertyObject.put("address", apartment.getAddress());
                propertyObject.put("owner", apartment.getOwner().getName());
                propertyObject.put("floor", apartment.getFloor());
                JSONArray petArray = new JSONArray();
                for (Pet pet : apartment.getPets()) {
                    JSONObject petObject = new JSONObject();
                    petObject.put("type", pet.getPetType());
                    petArray.put(petObject);
                }
                propertyObject.put("pets", petArray);
            }
            propertyArray.put(propertyObject);
        }
        jsonObject.put("properties", propertyArray);

        // Convert leases to JSON
        JSONArray leaseArray = new JSONArray();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        for (Lease lease : leases) {
            JSONObject leaseObject = new JSONObject();
            leaseObject.put("tenant", lease.getTenantName());
            leaseObject.put("startDate", dateFormat.format(lease.getStartDate()));
            leaseObject.put("endDate", dateFormat.format(lease.getEndDate()));
            leaseObject.put("monthlyRent", lease.getMonthlyRent());
            leaseArray.put(leaseObject);
        }
        jsonObject.put("leases", leaseArray);

        // Write to file
        try (FileWriter file = new FileWriter(DATA_FILE)) {
            file.write(jsonObject.toString(4)); // Indent for readability
            System.out.println("Data saved to " + DATA_FILE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}