package org.example;

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
            int floor1 = (p1 instanceof House) ? 0 : ((Apartment) p1).getFloor();
            int floor2 = (p2 instanceof House) ? 0 : ((Apartment) p2).getFloor();

            if (p1 instanceof House) {
                return (p2 instanceof House) ? 0 : -1;
            } else if (p2 instanceof House) {
                return 1;
            } else {
                return Integer.compare(floor1, floor2);
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

                case "smallest_adr":
                    properties.sort((p1, p2) -> {
                        // Extract the starting number from each address
                        int num1 = Integer.parseInt(p1.getAddress().split(" ")[0]);
                        int num2 = Integer.parseInt(p2.getAddress().split(" ")[0]);

                        // Compare based on the extracted numbers
                        return Integer.compare(num1, num2);
                    });

                    // Find the property with the smallest address after sorting
                    IProperty smallestAddressProperty = properties.get(0);
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
                        System.out.println("What would you like to add? (property/person/lease) or type 'exit' to go back: ");
                        String addType = scanner.nextLine().trim().toLowerCase();

                        if (addType.equals("exit")) {
                            System.out.println("Exiting add mode.");
                            break;
                        }

                        switch (addType) {
                            case "person":
                                System.out.println("Enter person name:");
                                String personName = scanner.nextLine();

                                persons.add(new Person(personName));
                                saveToJson(persons, properties, leases);
                                System.out.println("Person added.");
                                break;

                            case "property":
                                System.out.println("Enter property type (house/apartment):");
                                String propertyType = scanner.nextLine().toLowerCase();

                                System.out.println("Enter owner name:");
                                String ownerName = scanner.nextLine();

                                // Reuse or add the owner as a person
                                Person owner = persons.stream().filter(p -> p.getName().equalsIgnoreCase(ownerName))
                                        .findFirst()
                                        .orElse(new Person(ownerName));
                                if (!persons.contains(owner)) persons.add(owner);

                                System.out.println("Enter property address:");
                                String address = scanner.nextLine();

                                System.out.println("Enter number of pets:");
                                int petCount = Integer.parseInt(scanner.nextLine());
                                Pet[] pets = new Pet[petCount];
                                for (int i = 0; i < petCount; i++) {
                                    System.out.println("Enter pet type for pet " + (i + 1) + ":");
                                    String petType = scanner.nextLine();
                                    pets[i] = new Pet(petType);
                                }

                                if (propertyType.equals("house")) {
                                    System.out.println("Is it ground level? (true/false):");
                                    boolean isGround = Boolean.parseBoolean(scanner.nextLine());
                                    properties.add(new House(owner, pets, address, isGround));
                                } else if (propertyType.equals("apartment")) {
                                    System.out.println("Enter floor number:");
                                    int floor = Integer.parseInt(scanner.nextLine());
                                    properties.add(new Apartment(owner, floor, address, pets));
                                }
                                saveToJson(persons, properties, leases);
                                System.out.println("Property added.");
                                break;

                            case "lease":
                                System.out.println("Enter tenant name:");
                                String leaseTenantName = scanner.nextLine();

                                // Reuse or add the tenant as a person
                                Person tenant = persons.stream().filter(p -> p.getName().equalsIgnoreCase(leaseTenantName))
                                        .findFirst()
                                        .orElse(new Person(leaseTenantName));
                                if (!persons.contains(tenant)) persons.add(tenant);

                                try {
                                    System.out.println("Enter lease start date (yyyy-MM-dd):");
                                    Date leaseStartDate = dateFormat.parse(scanner.nextLine());

                                    System.out.println("Enter lease end date (yyyy-MM-dd):");
                                    Date leaseEndDate = dateFormat.parse(scanner.nextLine());

                                    System.out.println("Enter monthly rent:");
                                    double monthlyRent = Double.parseDouble(scanner.nextLine());

                                    // Check for overlapping leases for the same property
                                    System.out.println("Enter property address:");
                                    String propertyAddress = scanner.nextLine();

                                    for (Lease existingLease : leases) {
                                        if (existingLease.getPropertyAddress().equals(propertyAddress)) {
                                            Date existingEndDate = existingLease.getEndDate();

                                            // Ensure new lease starts only after existing lease ends
                                            if (!leaseStartDate.after(existingEndDate)) {
                                                throw new OverlappingLeaseException("Cannot lease property at " + propertyAddress
                                                        + " for the same period. New lease must start after " + existingEndDate);
                                            }
                                        }
                                    }

                                    // Add lease if no overlap
                                    leases.add(new Lease(leaseTenantName, leaseStartDate, leaseEndDate, monthlyRent, propertyAddress));
                                    saveToJson(persons, properties, leases);
                                    System.out.println("Lease added.");
                                } catch (ParseException e) {
                                    System.out.println("Invalid date format. Please enter dates in yyyy-MM-dd format.");
                                } catch (OverlappingLeaseException e) {
                                    System.out.println("Lease error: " + e.getMessage());
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
                leases.add(new Lease(
                        leaseObject.getString("tenant"),
                        dateFormat.parse(leaseObject.getString("startDate")),
                        dateFormat.parse(leaseObject.getString("endDate")),
                        leaseObject.getDouble("monthlyRent"),
                        leaseObject.getString("propertyAddress")
                ));
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