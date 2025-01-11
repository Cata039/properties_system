package org.example;

import java.util.concurrent.*;
import java.util.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class Main {
    // Thread pool for parallel tasks
    private static final ExecutorService executorService = Executors.newFixedThreadPool(8);

    // Parallelized pet counting function
    public static Future<Integer> countPetsAsync(IProperty property) {
        return executorService.submit(() -> {
            System.out.println(Thread.currentThread().getName() + " is processing " + property.getAddress());
            if (property instanceof House) {
                return ((House) property).getPets().length;
            } else if (property instanceof Apartment) {
                return ((Apartment) property).countPets();
            }
            return 0;
        });
    }

    // Parallelized sorting function
    public static Future<List<IProperty>> sortPropertiesByFloorAsync(List<IProperty> properties) {
        return executorService.submit(() -> {
            properties.sort((p1, p2) -> {
                if (p1 instanceof House && p2 instanceof House) return 0;
                if (p1 instanceof House) return -1;
                if (p2 instanceof House) return 1;
                return Integer.compare(((Apartment) p1).getFloor(), ((Apartment) p2).getFloor());
            });
            return properties;
        });
    }

    //parallelized function
    public static Map<String, List<IProperty>> groupPropertiesByOwner(List<IProperty> properties) {
        Map<String, List<IProperty>> ownerGroups = new ConcurrentHashMap<>();  // Concurrent for thread safety

        try {
            // Submit tasks to group properties by owner
            List<Future<?>> futures = new ArrayList<>();
            for (IProperty property : properties) {
                Future<?> future = executorService.submit(() -> {
                    String ownerName = property instanceof House
                            ? ((House) property).getOwner().getName()
                            : ((Apartment) property).getOwner().getName();

                    // Log the processing
                    System.out.println(Thread.currentThread().getName() + " is processing " + property.getAddress());

                    // Add to the group
                    ownerGroups.computeIfAbsent(ownerName, k -> new ArrayList<>()).add(property);
                });
                futures.add(future);
            }

            // Ensure all grouping tasks complete
            for (Future<?> future : futures) {
                future.get();  // Wait for each task to finish
            }

            // Sort properties for each owner in parallel
            List<Future<?>> sortFutures = new ArrayList<>();
            for (Map.Entry<String, List<IProperty>> entry : ownerGroups.entrySet()) {
                Future<?> sortFuture = executorService.submit(() -> {
                    List<IProperty> propertiesForOwner = entry.getValue();
                    propertiesForOwner.sort((p1, p2) -> {
                        if (p1 instanceof House && p2 instanceof House) {
                            return ((House) p1).compareTo((House) p2);  // Sort by address for houses
                        } else if (p1 instanceof Apartment && p2 instanceof Apartment) {
                            return ((Apartment) p1).compareTo((Apartment) p2);  // Sort apartments
                        }
                        return 0;  // No sorting needed for mixed types
                    });
                });
                sortFutures.add(sortFuture);
            }

            // Ensure all sorting tasks complete
            for (Future<?> sortFuture : sortFutures) {
                sortFuture.get();  // Wait for sorting to finish
            }

        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        } finally {
            executorService.shutdown();  // Shutdown thread pool
        }

        return ownerGroups;
    }


    //parallelized Function to count pets by type
    public static Future<Map<String, Integer>> countPetsByTypeAsync(List<IProperty> properties) {
        return executorService.submit(() -> {
            Map<String, Integer> petCount = new ConcurrentHashMap<>();
            List<Future<?>> futures = new ArrayList<>();

            for (IProperty property : properties) {
                futures.add(executorService.submit(() -> {
                    Pet[] pets = property instanceof House
                            ? ((House) property).getPets()
                            : ((Apartment) property).getPets();

                    for (Pet pet : pets) {
                        String petType = pet.getPetType();
                        petCount.merge(petType, 1, Integer::sum);  // Thread-safe increment
                    }
                }));
            }

            for (Future<?> future : futures) {
                future.get();  // Wait for all threads to finish
            }

            return petCount;
        });
    }


    //Parallelized Function to group properties by pet type
    public static Future<Map<String, List<IProperty>>> groupPropertiesByPetTypeAsync(List<IProperty> properties) {
        return executorService.submit(() -> {
            Map<String, List<IProperty>> petTypeGroups = new ConcurrentHashMap<>();
            List<Future<?>> futures = new ArrayList<>();

            for (IProperty property : properties) {
                futures.add(executorService.submit(() -> {
                    Pet[] pets = property instanceof House
                            ? ((House) property).getPets()
                            : ((Apartment) property).getPets();

                    for (Pet pet : pets) {
                        String petType = pet.getPetType();
                        petTypeGroups.computeIfAbsent(petType, k -> Collections.synchronizedList(new ArrayList<>())).add(property);
                    }
                }));
            }

            for (Future<?> future : futures) {
                future.get();  // Wait for all tasks to complete
            }

            return petTypeGroups;
        });
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

    public static void terminateLease(String propertyAddress) {
        String query = "DELETE FROM Lease WHERE property_id = (SELECT id FROM Property WHERE address = ?)";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, propertyAddress);
            int rowsDeleted = stmt.executeUpdate();
            if (rowsDeleted > 0) {
                System.out.println("Lease for property " + propertyAddress + " terminated.");
            } else {
                System.out.println("No lease found for the provided property address.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    //    private static final ExecutorService executorService = Executors.newFixedThreadPool(8);
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    public static void main(String[] args) {
        try {
            List<Person> persons = getAllPersons();  // Fetch persons from the database
            List<IProperty> properties = getAllProperties();  // Fetch properties from the database
            List<Lease> leases = getAllLeases();  // Fetch leases from the database

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
                        try {
                            // Call .get() on the Future to retrieve the integer result
                            int numberOfPets = countPetsAsync(property).get();
                            System.out.println(address + ": " + numberOfPets);
                        } catch (InterruptedException | ExecutionException e) {
                            e.printStackTrace();
                        }
                    }
                    break;

                case "sort":
                    System.out.println("Starting sorting...");

                    // Measure start time
                    long startTime = System.currentTimeMillis();

                    // Call asynchronous sorting method
                    Future<List<IProperty>> sortedFuture = sortPropertiesByFloorAsync(properties);
                    List<IProperty> sortedProperties = sortedFuture.get();  // Wait for result

                    // Measure end time
                    long endTime = System.currentTimeMillis();
                    System.out.println("Time taken for sorting: " + (endTime - startTime) + " ms");

                    // Display sorted properties
                    System.out.println("Properties sorted by floor:");
                    for (IProperty property : sortedProperties) {
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
                    System.out.println("Counting pets by type asynchronously...");
                    Future<Map<String, Integer>> petCountsFuture = countPetsByTypeAsync(properties);
                    Map<String, Integer> petCounts = petCountsFuture.get();  // Wait for the result
                    for (String petType : petCounts.keySet()) {
                        System.out.println(petType + ": " + petCounts.get(petType));
                    }
                    break;
                case "group_by_pet":
                    System.out.println("Grouping properties by pet type asynchronously...");
                    Future<Map<String, List<IProperty>>> petGroupsFuture = groupPropertiesByPetTypeAsync(properties);
                    Map<String, List<IProperty>> petGroups = petGroupsFuture.get();  // Wait for the result
                    for (String petType : petGroups.keySet()) {
                        System.out.println(petType + ":");
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
                    List<Lease> currentLeases = getAllLeases();  // Fetch leases from the database
                    if (currentLeases.isEmpty()) {
                        System.out.println("No leases available to display or terminate.");
                        break;
                    }

                    System.out.println("Lease Details:");
                    for (Lease lease : currentLeases) {
                        System.out.println("Tenant Name: " + lease.getTenantName());
                        System.out.println("Start Date: " + lease.getStartDate());
                        System.out.println("End Date: " + lease.getEndDate());
                        System.out.println("Monthly Rent: $" + lease.getMonthlyRent());
                        System.out.println();
                    }

                    // Optionally add a prompt for termination or updates:
                    System.out.println("Do you want to terminate a lease? (yes/no)");
                    String response = scanner.nextLine().trim().toLowerCase();
                    if (response.equals("yes")) {
                        System.out.println("Enter property address for lease termination:");
                        String propertyAddress = scanner.nextLine();
                        terminateLease(propertyAddress);  // Function to terminate the lease in the database
                    }
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

                                while (!propertyType.equals("house") && !propertyType.equals("apartment")) {
                                    System.out.println("Invalid property type. Please enter 'house' or 'apartment':");
                                    propertyType = scanner.nextLine().toLowerCase();
                                }

                                System.out.println("Enter owner name:");
                                String ownerName = scanner.nextLine();

                                // Check if person exists or add them to the database
                                Person owner = persons.stream().filter(p -> p.getName().equalsIgnoreCase(ownerName))
                                        .findFirst()
                                        .orElseGet(() -> {
                                            Person newPerson = new Person(ownerName);
                                            addPersonToDatabase(newPerson);  // Add new person to the database
                                            persons.add(newPerson);  // Keep local list updated
                                            return newPerson;
                                        });

                                // Validate property address format
                                String address;
                                while (true) {
                                    System.out.println("Enter property address (format '123 Abc'):");
                                    address = scanner.nextLine();
                                    try {
                                        validateAddress(address);
                                        break;  // Exit loop if address is valid
                                    } catch (InvalidAddressFormatException e) {
                                        System.out.println(e.getMessage());  // Display validation error
                                    }
                                }

                                System.out.println("Enter number of pets:");
                                int petCount = Integer.parseInt(scanner.nextLine());
                                Pet[] pets = new Pet[petCount];
                                for (int i = 0; i < petCount; i++) {
                                    System.out.println("Enter pet type for pet " + (i + 1) + ":");
                                    pets[i] = new Pet(scanner.nextLine());
                                }

                                if (propertyType.equals("house")) {
                                    House house = new House(owner, pets, address, true);  // Ground level is always true for houses
                                    addPropertyToDatabase(house);  // Save house to the database
                                    System.out.println("House added.");
                                } else {
                                    System.out.println("Enter floor number (0-130):");
                                    int floor = Integer.parseInt(scanner.nextLine());
                                    Apartment apartment = new Apartment(owner, floor, address, pets);
                                    addPropertyToDatabase(apartment);  // Save apartment to the database
                                    System.out.println("Apartment added.");
                                }
                                break;

                            case "lease":
                                System.out.println("Enter tenant name:");
                                String leaseTenantName = scanner.nextLine();

                                Person tenant = persons.stream()
                                        .filter(p -> p.getName().equalsIgnoreCase(leaseTenantName))
                                        .findFirst()
                                        .orElse(null);

                                if (tenant == null) {
                                    System.out.println("Tenant does not exist. Please use the 'property' command first to register the tenant.");
                                    break;
                                }

                                System.out.println("Enter property address:");
                                String propertyAddress = scanner.nextLine();

                                boolean propertyExists = properties.stream()
                                        .anyMatch(p -> p.getAddress().equalsIgnoreCase(propertyAddress));

                                if (!propertyExists) {
                                    System.out.println("Property not found. Please add it first.");
                                    break;
                                }

                                System.out.println("Enter monthly rent:");
                                double monthlyRent = Double.parseDouble(scanner.nextLine());

                                Date leaseStartDate = null, leaseEndDate = null;
                                while (leaseStartDate == null) {
                                    try {
                                        System.out.println("Enter lease start date (yyyy-MM-dd):");
                                        leaseStartDate = validateAndParseDate(scanner.nextLine());
                                    } catch (ParseException e) {
                                        System.out.println("Invalid date format. Please use yyyy-MM-dd.");
                                    }
                                }

                                while (leaseEndDate == null) {
                                    try {
                                        System.out.println("Enter lease end date (yyyy-MM-dd):");
                                        leaseEndDate = validateAndParseDate(scanner.nextLine());
                                    } catch (ParseException e) {
                                        System.out.println("Invalid date format. Please use yyyy-MM-dd.");
                                    }
                                }

                                try {
                                    for (Lease existingLease : leases) {
                                        if (existingLease.getPropertyAddress().equals(propertyAddress) &&
                                                !(leaseEndDate.before(existingLease.getStartDate()) || leaseStartDate.after(existingLease.getEndDate()))) {
                                            throw new OverlappingLeaseException("Lease period overlaps with an existing lease.");
                                        }
                                    }

                                    Lease lease = new Lease(tenant.getName(), leaseStartDate, leaseEndDate, monthlyRent, propertyAddress);
                                    addLeaseToDatabase(lease);  // Save lease to the database
                                    System.out.println("Lease added.");
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
        }catch (Exception e) {
            e.printStackTrace();
        } finally {
            executorService.shutdown();
        }
    }

    // Fetch all persons from database
    private static List<Person> getAllPersons() {
        List<Person> persons = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement("SELECT * FROM Person");
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                persons.add(new Person(rs.getString("name")));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return persons;
    }

    // Fetch all properties from database
    private static List<IProperty> getAllProperties() {
        List<IProperty> properties = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(
                     "SELECT p.*, o.name AS owner_name FROM Property p JOIN Person o ON p.owner_id = o.id")) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Person owner = new Person(rs.getString("owner_name"));
                String type = rs.getString("type");
                String address = rs.getString("address");
                int floor = rs.getInt("floor");
                boolean isGround = rs.getBoolean("isGround");
                List<Pet> pets = getPetsForProperty(rs.getInt("id"));

                if ("House".equalsIgnoreCase(type)) {
                    properties.add(new House(owner, pets.toArray(new Pet[0]), address, isGround));
                } else {
                    properties.add(new Apartment(owner, floor, address, pets.toArray(new Pet[0])));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return properties;
    }

    // Fetch pets for a specific property
    private static List<Pet> getPetsForProperty(int propertyId) {
        List<Pet> pets = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement("SELECT type FROM Pet WHERE id IN (SELECT pet_id FROM PropertyPet WHERE property_id = ?)")) {
            stmt.setInt(1, propertyId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                pets.add(new Pet(rs.getString("type")));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return pets;
    }

    // Fetch all leases from database
    private static List<Lease> getAllLeases() {
        List<Lease> leases = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(
                     "SELECT l.*, t.name AS tenant_name, p.address FROM Lease l JOIN Person t ON l.tenant_id = t.id JOIN Property p ON l.property_id = p.id")) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                leases.add(new Lease(rs.getString("tenant_name"), rs.getDate("startDate"), rs.getDate("endDate"), rs.getDouble("monthlyRent"), rs.getString("address")));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return leases;
    }

    public static void addPersonToDatabase(Person person) {
        String query = "INSERT INTO Person (name) VALUES (?)";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, person.getName());
            stmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static void addPetToDatabase(Pet pet, String propertyAddress) {
        String petQuery = "INSERT IGNORE INTO Pet (type) VALUES (?)";
        String linkQuery = "INSERT INTO PropertyPet (property_id, pet_id) VALUES ((SELECT id FROM Property WHERE address = ?), (SELECT id FROM Pet WHERE type = ?))";

        try (Connection connection = DatabaseConnection.getConnection()) {
            try (PreparedStatement stmt = connection.prepareStatement(petQuery)) {
                stmt.setString(1, pet.getPetType());
                stmt.executeUpdate();
            }
            try (PreparedStatement stmt = connection.prepareStatement(linkQuery)) {
                stmt.setString(1, propertyAddress);
                stmt.setString(2, pet.getPetType());
                stmt.executeUpdate();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void addPropertyToDatabase(IProperty property) {
        String query = "INSERT INTO Property (address, owner_id, floor, isGround, type) VALUES (?, (SELECT id FROM Person WHERE name = ?), ?, ?, ?)";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, property.getAddress());
            stmt.setString(2, property.getOwner().getName());

            Pet[] pets = new Pet[0];  // Initialize empty array for pets

            if (property instanceof House) {
                House house = (House) property;
                pets = house.getPets();  // Use the getter method
                stmt.setNull(3, java.sql.Types.INTEGER);
                stmt.setBoolean(4, true);
                stmt.setString(5, "House");
            } else if (property instanceof Apartment) {
                Apartment apartment = (Apartment) property;
                pets = apartment.getPets();  // Use the getter method
                stmt.setInt(3, apartment.getFloor());
                stmt.setBoolean(4, false);
                stmt.setString(5, "Apartment");
            }

            stmt.executeUpdate();

            for (Pet pet : pets) {
                addPetToDatabase(pet, property.getAddress());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    public static void addLeaseToDatabase(Lease lease) {
        String query = "INSERT INTO Lease (tenant_id, property_id, startDate, endDate, monthlyRent) VALUES ((SELECT id FROM Person WHERE name = ?), (SELECT id FROM Property WHERE address = ?), ?, ?, ?)";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, lease.getTenantName());
            stmt.setString(2, lease.getPropertyAddress());
            stmt.setDate(3, new java.sql.Date(lease.getStartDate().getTime()));
            stmt.setDate(4, new java.sql.Date(lease.getEndDate().getTime()));
            stmt.setDouble(5, lease.getMonthlyRent());
            stmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



}