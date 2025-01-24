package org.example;

import java.util.concurrent.*;
import java.util.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Main {
    // Thread pool for parallel tasks
    private static final ExecutorService executorService = Executors.newFixedThreadPool(8);

    // Parallelized pet counting function
    public static Future<Integer> countPetsAsync(IProperty property) {
        return executorService.submit(() -> {
            //System.out.println(Thread.currentThread().getName() + " is processing " + property.getAddress());
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

    // Parallelized Function to count pets by type from the database using PropertyPet table
    public static Future<Map<String, Integer>> countPetsByTypeAsync() {
        return executorService.submit(() -> {
            Map<String, Integer> petCount = new ConcurrentHashMap<>();

            // SQL query to count pets based on types from the PropertyPet table
            String query = """
            SELECT pe.type, COUNT(*) AS count 
            FROM PropertyPet pp
            JOIN Pet pe ON pp.pet_id = pe.id
            GROUP BY pe.type
        """;

            try (Connection connection = DatabaseConnection.getConnection();
                 PreparedStatement stmt = connection.prepareStatement(query);
                 ResultSet rs = stmt.executeQuery()) {

                while (rs.next()) {
                    String petType = rs.getString("type");
                    int count = rs.getInt("count");
                    petCount.put(petType, count);  // Add the count for each pet type
                }

            } catch (SQLException e) {
                e.printStackTrace();
            }

            return petCount;
        });
    }

    //Parallelized Function to group properties by pet type
    public static Future<Map<String, List<IProperty>>> groupPropertiesByPetTypeAsync() {
        return executorService.submit(() -> {
            Map<String, List<IProperty>> petTypeGroups = new ConcurrentHashMap<>();

            // Query to fetch pet types and corresponding property addresses
            String query = """
                SELECT p.address, p.type, p.owner_id, p.floor, p.isGround, pe.type AS petType, o.name AS owner_name
                FROM Property p
                JOIN PropertyPet pp ON p.id = pp.property_id
                JOIN Pet pe ON pp.pet_id = pe.id
                JOIN Person o ON p.owner_id = o.id
                """;

            try (Connection connection = DatabaseConnection.getConnection();
                 PreparedStatement stmt = connection.prepareStatement(query);
                 ResultSet rs = stmt.executeQuery()) {

                while (rs.next()) {
                    String address = rs.getString("address");
                    String petType = rs.getString("petType");
                    String ownerName = rs.getString("owner_name");
                    String type = rs.getString("type");
                    int floor = rs.getInt("floor");
                    boolean isGround = rs.getBoolean("isGround");

                    // Create owner and property
                    Person owner = new Person(ownerName);
                    IProperty property;
                    if ("House".equalsIgnoreCase(type)) {
                        property = new House(owner, new Pet[]{}, address, isGround);
                    } else {
                        property = new Apartment(owner, floor, address, new Pet[]{});
                    }

                    // Add property to the corresponding pet type group
                    petTypeGroups.computeIfAbsent(petType, k -> Collections.synchronizedList(new ArrayList<>())).add(property);
                }
            } catch (SQLException e) {
                e.printStackTrace();
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

    public static boolean isAddressValid(String address) {
        String query = "SELECT COUNT(*) AS count FROM Property WHERE LOWER(TRIM(address)) = LOWER(TRIM(?))";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, address.trim());
            System.out.println("Checking address: " + address.trim());
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                int count = rs.getInt("count");
                System.out.println("Address count in database: " + count);
                return count > 0; // If count > 0, address exists
            }
        } catch (Exception e) {
            System.out.println("Error validating address: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }


    public static void viewAllPetsForTenant(String propertyAddress) {
        String query = """
        SELECT pe.type 
        FROM PropertyPet pp
        JOIN Pet pe ON pp.pet_id = pe.id
        JOIN Property p ON pp.property_id = p.id
        WHERE p.address = ?
    """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, propertyAddress);
            ResultSet rs = stmt.executeQuery();

            System.out.println("\nYour Pets:");
            int count = 0;
            while (rs.next()) {
                count++;
                String petType = rs.getString("type");
                System.out.println(count + ". " + petType);
            }

            if (count == 0) {
                System.out.println("No pets found for this address.");
            }

        } catch (Exception e) {
            System.out.println("An error occurred while fetching your pets: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void addPetForTenant(Scanner scanner, String propertyAddress) {
        System.out.println("Enter the pet type (e.g., Dog, Cat, Bird, etc.):");
        String petType = scanner.nextLine().trim();

        try (Connection connection = DatabaseConnection.getTenantConnection()) {  // Now handled properly
            int petId = -1;

            // Check if the pet type already exists
            String getPetIdQuery = "SELECT id FROM Pet WHERE type = ?";
            try (PreparedStatement getPetIdStmt = connection.prepareStatement(getPetIdQuery)) {
                getPetIdStmt.setString(1, petType);
                ResultSet rs = getPetIdStmt.executeQuery();
                if (rs.next()) {
                    petId = rs.getInt("id");
                }
            }

            // If pet type does not exist, insert new pet type
            if (petId == -1) {
                String insertPetQuery = "INSERT INTO Pet (type) VALUES (?)";
                try (PreparedStatement insertPetStmt = connection.prepareStatement(insertPetQuery)) {
                    insertPetStmt.setString(1, petType);
                    insertPetStmt.executeUpdate();
                }

                // Retrieve the newly inserted pet ID
                try (PreparedStatement getPetIdStmt = connection.prepareStatement(getPetIdQuery)) {
                    getPetIdStmt.setString(1, petType);
                    ResultSet rs = getPetIdStmt.executeQuery();
                    if (rs.next()) {
                        petId = rs.getInt("id");
                    }
                }
            }

            // Link the pet to the property
            String linkPetToPropertyQuery = "INSERT INTO PropertyPet (property_id, pet_id) VALUES ((SELECT id FROM Property WHERE address = ? LIMIT 1), ?)";
            try (PreparedStatement linkStmt = connection.prepareStatement(linkPetToPropertyQuery)) {
                linkStmt.setString(1, propertyAddress);
                linkStmt.setInt(2, petId);
                int rowsAffected = linkStmt.executeUpdate();
                if (rowsAffected > 0) {
                    System.out.println("Pet added successfully.");
                } else {
                    System.out.println("Failed to add pet. Please check the property address.");
                }
            }

        } catch (Exception e) {  // Catching the generic Exception since getTenantConnection throws Exception
            System.out.println("An error occurred while adding the pet: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void deletePetForTenant(Scanner scanner, String propertyAddress) {
        String getPetsQuery = """
        SELECT pe.id, pe.type 
        FROM PropertyPet pp
        JOIN Pet pe ON pp.pet_id = pe.id
        JOIN Property p ON pp.property_id = p.id
        WHERE p.address = ?
    """;

        try (Connection connection = DatabaseConnection.getTenantConnection();  // Use tenant connection with proper handling
             PreparedStatement stmt = connection.prepareStatement(getPetsQuery)) {
            stmt.setString(1, propertyAddress);
            ResultSet rs = stmt.executeQuery();

            List<Integer> petIds = new ArrayList<>();
            System.out.println("\nYour Pets:");
            int count = 0;
            while (rs.next()) {
                count++;
                int petId = rs.getInt("id");
                String petType = rs.getString("type");
                petIds.add(petId);
                System.out.println(count + ". " + petType);
            }

            if (count == 0) {
                System.out.println("No pets found for this address.");
                return;
            }

            System.out.println("Enter the number of the pet you want to delete (or 0 to go back):");
            int choice = scanner.nextInt();
            scanner.nextLine(); // Consume newline

            if (choice == 0) {
                System.out.println("Going back...");
                return;
            }

            if (choice < 1 || choice > petIds.size()) {
                System.out.println("Invalid choice.");
                return;
            }

            int petIdToDelete = petIds.get(choice - 1);
            String deleteQuery = "DELETE FROM PropertyPet WHERE property_id = (SELECT id FROM Property WHERE address = ?) AND pet_id = ?";
            try (PreparedStatement deleteStmt = connection.prepareStatement(deleteQuery)) {
                deleteStmt.setString(1, propertyAddress);
                deleteStmt.setInt(2, petIdToDelete);
                int rowsDeleted = deleteStmt.executeUpdate();
                if (rowsDeleted > 0) {
                    System.out.println("Pet deleted successfully.");
                } else {
                    System.out.println("Failed to delete pet.");
                }
            }

        } catch (Exception e) {  // Catching the exception thrown by getTenantConnection
            System.out.println("An error occurred while deleting the pet: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void payRent(Scanner scanner, String propertyAddress) {
        System.out.println("Enter amount to pay:");
        double paymentAmount = scanner.nextDouble();
        scanner.nextLine(); // Consume newline

        String checkLeaseQuery = """
    SELECT l.monthlyRent, l.id, t.name AS tenant_name 
    FROM Lease l
    JOIN Person t ON l.tenant_id = t.id
    WHERE l.property_id = (SELECT id FROM Property WHERE address = ?)
    """;

        try (Connection connection = DatabaseConnection.getTenantConnection();  // Tenant connection
             PreparedStatement checkLeaseStmt = connection.prepareStatement(checkLeaseQuery)) {

            checkLeaseStmt.setString(1, propertyAddress);
            ResultSet rs = checkLeaseStmt.executeQuery();

            if (rs.next()) {
                double monthlyRent = rs.getDouble("monthlyRent");
                int leaseId = rs.getInt("id");
                String tenantName = rs.getString("tenant_name");  // Fetch the tenant's name

                // Check if payment has already been made for the current month
                String checkPaymentQuery = """
            SELECT COUNT(*) AS count 
            FROM RentPayments 
            WHERE lease_id = ? AND MONTH(payment_date) = MONTH(CURRENT_DATE()) AND YEAR(payment_date) = YEAR(CURRENT_DATE())
            """;
                try (PreparedStatement checkPaymentStmt = connection.prepareStatement(checkPaymentQuery)) {
                    checkPaymentStmt.setInt(1, leaseId);
                    ResultSet paymentRs = checkPaymentStmt.executeQuery();
                    if (paymentRs.next() && paymentRs.getInt("count") > 0) {
                        System.out.println("You have already paid the rent for this month.");
                        return;  // Exit if the rent has already been paid
                    }
                }

                validateMonthlyRent(monthlyRent);  // Validate that rent is positive

                if (paymentAmount == monthlyRent) {
                    String insertPaymentQuery = """
                INSERT INTO RentPayments (property_address, tenant_name, payment_date, amount_paid, lease_id) 
                VALUES (?, ?, NOW(), ?, ?)
                """;
                    try (PreparedStatement insertPaymentStmt = connection.prepareStatement(insertPaymentQuery)) {
                        insertPaymentStmt.setString(1, propertyAddress);
                        insertPaymentStmt.setString(2, tenantName);
                        insertPaymentStmt.setDouble(3, paymentAmount);
                        insertPaymentStmt.setInt(4, leaseId);

                        int rowsInserted = insertPaymentStmt.executeUpdate();
                        if (rowsInserted > 0) {
                            System.out.println("Payment successful! You have paid: $" + paymentAmount);
                        } else {
                            System.out.println("Failed to record payment.");
                        }
                    }
                } else {
                    System.out.println("Error: The payment amount must match the monthly rent: $" + monthlyRent);
                }
            } else {
                System.out.println("This property does not have a lease. Please contact the admin to create a lease before making a payment.");
            }

        } catch (Exception e) {  // Catching the general Exception since getTenantConnection throws Exception
            System.out.println("Error processing payment: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Validate that the floor number is within a reasonable range (for apartments)
    public static void validateFloorNumber(int floor) throws IllegalArgumentException {
        if (floor < 0 || floor > 130) {
            throw new IllegalArgumentException("Floor number must be between 0 and 130.");
        }
    }

    // Validate that the monthly rent is a positive value
    public static void validateMonthlyRent(double rent) throws IllegalArgumentException {
        if (rent <= 0) {
            throw new IllegalArgumentException("Monthly rent must be a positive value.");
        }
    }

    // Validate that the input is not empty
    public static void validateNotEmpty(String input, String fieldName) throws IllegalArgumentException {
        if (input == null || input.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " cannot be empty.");
        }
    }


    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        Scanner scanner = new Scanner(System.in);  // Scanner for user input

        System.out.println("Enter your role (admin/tenant): ");
        String role = scanner.nextLine().trim().toLowerCase();  // Read user role

        Connection connection = null;

        try {
            if (role.equals("admin")) {
                connection = DatabaseConnection.getAdminConnection();  // Admin connection
                System.out.println("Logged in as Admin.");
                adminMenu(scanner, connection);  // Admin menu

            } else if (role.equals("tenant")) {
                connection = DatabaseConnection.getTenantConnection();  // Tenant connection
                System.out.println("Logged in as Tenant.");
                tenantMenu(scanner, connection);  // Tenant menu

            } else {
                System.out.println("Invalid role. Please enter either 'admin' or 'tenant'.");
            }

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                if (connection != null) connection.close();  // Close connection
            } catch (Exception e) {
                e.printStackTrace();
            }
            scanner.close();  // Close scanner
        }
    }

    // Admin Menu
    private static void adminMenu(Scanner scanner, Connection connection) {
        int choice;
        do {
            System.out.println("\nAdmin Options:");
            System.out.println("1. Manage Pets");
            System.out.println("2. Manage Properties");
            System.out.println("3. Add Data");
            System.out.println("4. View Contracts");
            System.out.println("5. Exit");
            System.out.print("Enter your choice (1-5): ");
            choice = scanner.nextInt();
            scanner.nextLine();  // Consume newline

            switch (choice) {
                case 1:
                    System.out.println("Managing Pets...");
                    int petChoice = 0;

                    while (petChoice != 4) { // Exit the Manage Pets menu when user enters 4
                        System.out.println("\nManage Pets Options:");
                        System.out.println("1. Count Pets");
                        System.out.println("2. Group Properties by Pet Type");
                        System.out.println("3. Number of Pets per Property");
                        System.out.println("4. Back to Admin Menu");
                        System.out.print("Enter your choice (1 to 4): ");

                        if (scanner.hasNextInt()) {
                            petChoice = scanner.nextInt(); // Read pet management choice

                            switch (petChoice) {
                                case 1:  // Count Pets
                                    System.out.println("Counting pets by type asynchronously...");
                                    try {
                                        Future<Map<String, Integer>> petCountsFuture = countPetsByTypeAsync();  // Async call
                                        Map<String, Integer> petCounts = petCountsFuture.get();  // Wait for result

                                        System.out.println("Pet counts by type:");
                                        petCounts.forEach((type, count) -> System.out.println(type + ": " + count));
                                    } catch (InterruptedException e) {
                                        System.out.println("The operation was interrupted. Exiting...");
                                        Thread.currentThread().interrupt();  // Reset the interrupted status
                                    } catch (ExecutionException e) {
                                        System.out.println("An error occurred during asynchronous computation: " + e.getCause());
                                    }
                                    break;

                                case 2:  // Group Properties by Pet Type
                                    System.out.println("Grouping properties by pet type asynchronously...");

                                    try {
                                        Future<Map<String, List<IProperty>>> petGroupsFuture = groupPropertiesByPetTypeAsync();
                                        Map<String, List<IProperty>> petGroups = petGroupsFuture.get();  // Wait for the result

                                        System.out.println("Properties grouped by pet type:");
                                        for (String petType : petGroups.keySet()) {
                                            System.out.println(petType + ":");
                                            for (IProperty property : petGroups.get(petType)) {
                                                System.out.println(" - " + property.getAddress());
                                            }
                                        }
                                    } catch (InterruptedException e) {
                                        System.err.println("The operation was interrupted: " + e.getMessage());
                                        Thread.currentThread().interrupt();  // Restore interrupt status
                                    } catch (ExecutionException e) {
                                        System.err.println("An error occurred while grouping properties: " + e.getCause());
                                        e.printStackTrace();
                                    }
                                    break;

                                case 3: // Number of Pets per Property
                                    System.out.println("Number of pets per property:");

                                    // Fetch properties from the database
                                    List<IProperty> properties = getAllProperties();

                                    if (properties.isEmpty()) {
                                        System.out.println("No properties found.");
                                    } else {
                                        for (IProperty property : properties) {
                                            String address = property.getAddress();
                                            try {
                                                // Asynchronously count pets for the current property
                                                int numberOfPets = countPetsAsync(property).get();  // Future.get() waits for result
                                                System.out.println(address + ": " + numberOfPets + " pets");
                                            } catch (InterruptedException | ExecutionException e) {
                                                System.out.println("Error counting pets for property: " + address);
                                                e.printStackTrace();
                                            }
                                        }
                                    }
                                    break;

                                case 4:
                                    System.out.println("Returning to Admin Menu...");
                                    break;

                                default:
                                    System.out.println("Invalid choice. Please enter a number between 1 and 4.");
                            }
                        } else {
                            System.out.println("Invalid input. Please enter a number between 1 and 4.");
                            scanner.next(); // Consume invalid input
                        }
                    }
                    break;

                case 2:  // Manage Properties
                    System.out.println("Managing Properties...");
                    int managePropertiesChoice = 0;

                    while (managePropertiesChoice != 4) {  // Exit when the user selects 4
                        System.out.println("\nManage Properties Options:");
                        System.out.println("1. Properties Sorted by Floor");
                        System.out.println("2. Group Properties by Owner");
                        System.out.println("3. Contract Details");
                        System.out.println("4. Back to Admin Menu");
                        System.out.print("Enter your choice (1 to 4): ");

                        if (scanner.hasNextInt()) {
                            managePropertiesChoice = scanner.nextInt();  // Read user's choice
                            scanner.nextLine();  // Consume newline

                            switch (managePropertiesChoice) {
                                case 1:  // Sort Properties by Floor
                                    System.out.println("Starting sorting...");

                                    // Fetch properties from the database
                                    List<IProperty> properties = getAllProperties();  // Get properties from the DB

                                    if (properties.isEmpty()) {
                                        System.out.println("No properties found to sort.");
                                    } else {
                                        try {
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
                                        } catch (InterruptedException e) {
                                            System.out.println("The sorting operation was interrupted.");
                                            Thread.currentThread().interrupt();  // Restore interrupted status
                                        } catch (ExecutionException e) {
                                            System.out.println("An error occurred during sorting: " + e.getCause());
                                        }
                                    }
                                    break;

                                case 2: // Group Properties by Owner
                                    System.out.println("Grouping properties by owner...");
                                    List<IProperty> allproperties = getAllProperties();  // Initialize here

                                    if (allproperties == null || allproperties.isEmpty()) {
                                        System.out.println("No properties found.");
                                    } else {
                                        Map<String, List<IProperty>> ownerGroups = groupPropertiesByOwner(allproperties);
                                        System.out.println("Properties grouped by owner:");
                                        for (String owner : ownerGroups.keySet()) {
                                            System.out.println("Owner: " + owner);
                                            for (IProperty property : ownerGroups.get(owner)) {
                                                System.out.println("  - " + property.getAddress());
                                            }
                                        }
                                    }
                                    break;

                                case 3:  // Contract Details
                                    System.out.println("Displaying Contract Details...");
                                    List<Lease> currentLeases = getAllLeases();  // Fetch leases from the database
                                    if (currentLeases.isEmpty()) {
                                        System.out.println("No leases available to display or terminate.");
                                        break;
                                    }

                                    System.out.println("Lease Details:");
                                    for (Lease lease : currentLeases) {
                                        System.out.println("Tenant Name: " + lease.getTenantName());
                                        System.out.println("Property Address: " + lease.getPropertyAddress());
                                        System.out.println("Property Type: " + lease.getPropertyType());
                                        System.out.println("Start Date: " + lease.getStartDate());
                                        System.out.println("End Date: " + lease.getEndDate());
                                        System.out.println("Monthly Rent: $" + lease.getMonthlyRent());
                                        System.out.println();
                                    }

                                    // Prompt to terminate a lease
                                    System.out.println("Do you want to terminate a lease? (yes/no)");
                                    String response = scanner.nextLine().trim().toLowerCase();
                                    if (response.equals("yes")) {
                                        System.out.println("Enter property address for lease termination:");
                                        String propertyAddress = scanner.nextLine();
                                        terminateLease(propertyAddress);  // Terminate the lease
                                    }
                                    break;

                                case 4:
                                    System.out.println("Returning to Admin Menu...");
                                    break;

                                default:
                                    System.out.println("Invalid choice. Please enter a number between 1 and 4.");
                            }
                        } else {
                            System.out.println("Invalid input. Please enter a number between 1 and 4.");
                            scanner.next();  // Consume invalid input
                        }
                    }
                    break;

                case 3: // Add Data
                    System.out.println("Adding New Data...");
                    int addDataChoice = 0;
                    while (addDataChoice != 3) {
                        System.out.println("\nAdd Data Options:");
                        System.out.println("1. Add Property");
                        System.out.println("2. Add Lease");
                        System.out.println("3. Back to Admin Menu");
                        System.out.print("Enter your choice (1 to 3): ");

                        while (!scanner.hasNextInt()) {
                            System.out.println("Invalid input. Please enter a number between 1 and 3.");
                            scanner.next();
                        }
                        addDataChoice = scanner.nextInt();
                        scanner.nextLine(); // consume newline

                        switch (addDataChoice) {
                            case 1: // Add Property
                                System.out.println("Adding a New Property...");
                                String propertyType = "";
                                while (!(propertyType.equals("house") || propertyType.equals("apartment"))) {
                                    System.out.println("Enter property type (house/apartment):");
                                    propertyType = scanner.nextLine().trim().toLowerCase();
                                    if (!(propertyType.equals("house") || propertyType.equals("apartment"))) {
                                        System.out.println("Invalid property type. Please enter 'house' or 'apartment'.");
                                    }
                                }

                                System.out.println("Enter owner name:");
                                String ownerName = scanner.nextLine().trim();
                                validateNotEmpty(ownerName, "Owner Name");

                                List<Person> persons = getAllPersons();
                                Person owner = persons.stream().filter(p -> p.getName().equalsIgnoreCase(ownerName)).findFirst().orElse(null);
                                if (owner == null) {
                                    owner = new Person(ownerName);
                                    addPersonToDatabase(owner);
                                }

                                System.out.println("Enter property address (format '123 Abc'):");
                                String address = "";
                                while (true) {
                                    address = scanner.nextLine().trim();
                                    try {
                                        validateAddress(address);
                                        break;
                                    } catch (InvalidAddressFormatException e) {
                                        System.out.println(e.getMessage());
                                    }
                                }

                                int petCount = -1;
                                while (petCount < 0) {
                                    System.out.println("Enter number of pets:");
                                    if (scanner.hasNextInt()) {
                                        petCount = scanner.nextInt();
                                        scanner.nextLine();
                                        if (petCount < 0) {
                                            System.out.println("Number of pets cannot be negative.");
                                        }
                                    } else {
                                        System.out.println("Invalid input. Please enter a valid number.");
                                        scanner.next();
                                    }
                                }

                                Pet[] pets = new Pet[petCount];
                                for (int i = 0; i < petCount; i++) {
                                    System.out.println("Enter pet type for pet " + (i + 1) + ":");
                                    String petType = scanner.nextLine().trim();
                                    validateNotEmpty(petType, "Pet Type");
                                    pets[i] = new Pet(petType);
                                }

                                if (propertyType.equals("apartment")) {
                                    int floor = -1;
                                    while (floor < 0 || floor > 130) {
                                        System.out.println("Enter floor number (0-130):");
                                        if (scanner.hasNextInt()) {
                                            floor = scanner.nextInt();
                                            scanner.nextLine();
                                            if (floor < 0 || floor > 130) {
                                                System.out.println("Floor number must be between 0 and 130.");
                                            }
                                        } else {
                                            System.out.println("Invalid input. Please enter a number between 0 and 130.");
                                            scanner.next();
                                        }
                                    }
                                    Apartment apartment = new Apartment(owner, floor, address, pets);
                                    addPropertyToDatabase(apartment);
                                    System.out.println("Apartment added successfully.");
                                } else {
                                    House house = new House(owner, pets, address, true);
                                    addPropertyToDatabase(house);
                                    System.out.println("House added successfully.");
                                }
                                break;

                            case 2: // Add Lease
                                System.out.println("Adding a New Lease...");
                                String leaseTenantName = "";
                                while (leaseTenantName.isEmpty()) {
                                    System.out.println("Enter tenant name:");
                                    leaseTenantName = scanner.nextLine().trim();
                                    if (leaseTenantName.isEmpty()) {
                                        System.out.println("Tenant name cannot be empty. Please enter a valid name.");
                                    }
                                }

                                persons = getAllPersons();

// Assign `leaseTenantName` to a final variable for use in the lambda expression
                                final String finalLeaseTenantName = leaseTenantName;

                                Person tenant = persons.stream()
                                        .filter(p -> p.getName().equalsIgnoreCase(finalLeaseTenantName))
                                        .findFirst()
                                        .orElse(null);

                                while (tenant == null) {
                                    System.out.println("Tenant not found. Please enter an existing tenant name or add them as a person first:");
                                    leaseTenantName = scanner.nextLine().trim();
                                    if (leaseTenantName.isEmpty()) {
                                        System.out.println("Tenant name cannot be empty. Please enter a valid name.");
                                        continue;
                                    }
                                    final String updatedLeaseTenantName = leaseTenantName; // Make effectively final
                                    tenant = persons.stream()
                                            .filter(p -> p.getName().equalsIgnoreCase(updatedLeaseTenantName))
                                            .findFirst()
                                            .orElse(null);
                                }

                                System.out.println("Enter property address:");
                                List<IProperty> properties = getAllProperties();
                                String propertyAddress = "";
                                while (true) {
                                    propertyAddress = scanner.nextLine().trim();
                                    if (propertyAddress.isEmpty()) {
                                        System.out.println("Property address cannot be empty. Please enter a valid address.");
                                        continue;
                                    }
                                    final String finalPropertyAddress = propertyAddress;  // Effectively final for lambda
                                    if (properties.stream().anyMatch(p -> p.getAddress().equalsIgnoreCase(finalPropertyAddress))) {
                                        break;
                                    }
                                    System.out.println("Property not found. Please enter a valid property address:");
                                }

                                double monthlyRent = -1;
                                while (monthlyRent <= 0) {
                                    System.out.println("Enter monthly rent:");
                                    if (scanner.hasNextDouble()) {
                                        monthlyRent = scanner.nextDouble();
                                        scanner.nextLine();
                                        if (monthlyRent <= 0) {
                                            System.out.println("Monthly rent must be positive.");
                                        }
                                    } else {
                                        System.out.println("Invalid input. Please enter a valid rent amount.");
                                        scanner.next(); // Consume invalid input
                                    }
                                }

                                Date leaseStartDate = null, leaseEndDate = null;
                                while (leaseStartDate == null) {
                                    System.out.println("Enter lease start date (yyyy-MM-dd):");
                                    String dateInput = scanner.nextLine().trim();
                                    if (dateInput.isEmpty()) {
                                        System.out.println("Date cannot be empty. Please enter a valid date.");
                                        continue;
                                    }
                                    try {
                                        leaseStartDate = validateAndParseDate(dateInput);
                                    } catch (ParseException e) {
                                        System.out.println("Invalid date format. Please use yyyy-MM-dd.");
                                    }
                                }

                                while (leaseEndDate == null) {
                                    System.out.println("Enter lease end date (yyyy-MM-dd):");
                                    String dateInput = scanner.nextLine().trim();
                                    if (dateInput.isEmpty()) {
                                        System.out.println("Date cannot be empty. Please enter a valid date.");
                                        continue;
                                    }
                                    try {
                                        leaseEndDate = validateAndParseDate(dateInput);
                                    } catch (ParseException e) {
                                        System.out.println("Invalid date format. Please use yyyy-MM-dd.");
                                    }
                                }

                                Lease lease = new Lease(leaseTenantName, leaseStartDate, leaseEndDate, monthlyRent, propertyAddress);
                                addLeaseToDatabase(lease);
                                System.out.println("Lease added successfully.");
                                break;

                            case 3:
                                System.out.println("Returning to Admin Menu...");
                                break;

                            default:
                                System.out.println("Invalid choice. Please enter a number between 1 and 3.");
                        }
                    }
                    break;

                case 4:  // About All Properties
                    System.out.println("About All Properties...");
                    int aboutPropertiesChoice = 0;

                    while (aboutPropertiesChoice != 2) {  // Exit when user selects 3
                        System.out.println("\nAbout All Properties Options:");
                        System.out.println("1. Describe Properties");
                        System.out.println("2. Back to Admin Menu");
                        System.out.print("Enter your choice (1 to 2): ");

                        if (scanner.hasNextInt()) {
                            aboutPropertiesChoice = scanner.nextInt();  // Read user's choice
                            scanner.nextLine();  // Consume newline

                            switch (aboutPropertiesChoice) {
                                case 1:  // Describe Properties
                                    System.out.println("Describing all properties...");
                                    List<IProperty> properties = getAllProperties();  // Use the existing method to fetch properties from the DB

                                    if (properties.isEmpty()) {
                                        System.out.println("No properties found.");
                                    } else {
                                        for (IProperty property : properties) {
                                            System.out.println("Property: " + property.getDescription());  // Assuming getDescription() is implemented in IProperty
                                        }
                                    }
                                    break;

                                case 2:
                                    System.out.println("Returning to Admin Menu...");
                                    break;

                                default:
                                    System.out.println("Invalid choice. Please enter a number between 1 and 3.");
                            }
                        } else {
                            System.out.println("Invalid input. Please enter a number between 1 and 3.");
                            scanner.next();  // Consume invalid input
                        }
                    }
                    break;

                case 5:
                    System.out.println("Returning to Main Menu...");
                    break;

                default:
                    System.out.println("Invalid choice. Please enter a number between 1 and 5.");
            }
        } while (choice != 5);  // Exit menu when choice is 5
    }

    // Tenant Menu
    private static void tenantMenu(Scanner scanner, Connection connection) {
        String propertyAddress;
        do {
            System.out.println("Enter the property address where you live:");
            propertyAddress = scanner.nextLine().trim();  // Tenant's property address
            System.out.println("Checking address: " + propertyAddress);

            if (!isAddressValid(propertyAddress)) {
                System.out.println("Invalid address. Please enter a valid address or type 'exit' to quit.");
            } else {
                break;  // Exit the loop when a valid address is entered
            }

        } while (!propertyAddress.equalsIgnoreCase("exit"));

        if (propertyAddress.equalsIgnoreCase("exit")) {
            System.out.println("Exiting tenant menu...");
            return;  // Exit the tenant menu if the user chooses "exit"
        }

        int choice = -1;  // Initialize to an invalid value
        do {
            System.out.println("\nTenant Options:");
            System.out.println("1. View Pets");
            System.out.println("2. Add Pet");
            System.out.println("3. Delete Pet");
            System.out.println("4. Pay Rent");
            System.out.println("5. Exit");
            System.out.print("Enter your choice (1-5): ");

            try {
                // Validate that the input is an integer
                choice = Integer.parseInt(scanner.nextLine().trim());

                switch (choice) {
                    case 1:
                        viewAllPetsForTenant(propertyAddress);
                        break;
                    case 2:
                        addPetForTenant(scanner, propertyAddress);
                        break;
                    case 3:
                        deletePetForTenant(scanner, propertyAddress);
                        break;
                    case 4:
                        payRent(scanner, propertyAddress);
                        break;
                    case 5:
                        System.out.println("Exiting Tenant Menu...");
                        break;
                    default:
                        System.out.println("Invalid choice. Please enter a number between 1 and 5.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a valid number.");
            }
        } while (choice != 5);  // Exit menu when choice is 5
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
    public static List<Lease> getAllLeases() {
        List<Lease> leases = new ArrayList<>();
        String query = "SELECT l.*, t.name AS tenant_name, p.address, p.type, p.floor, p.isGround " +
                "FROM Lease l " +
                "JOIN Person t ON l.tenant_id = t.id " +
                "JOIN Property p ON l.property_id = p.id";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String tenantName = rs.getString("tenant_name");
                String address = rs.getString("address");
                String type = rs.getString("type");
                int floor = rs.getInt("floor");
                boolean isGround = rs.getBoolean("isGround");
                Date startDate = rs.getDate("startDate");
                Date endDate = rs.getDate("endDate");
                double monthlyRent = rs.getDouble("monthlyRent");

                // Create a description for the property type
                String propertyDescription = type.equals("House")
                        ? (isGround ? "House (Ground level)" : "House")
                        : "Apartment (Floor: " + floor + ")";

                Lease lease = new Lease(tenantName, startDate, endDate, monthlyRent, address);


                leases.add(lease);
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

    public static void addPetToDatabase(Pet pet, String propertyAddress) throws Exception {
        String checkPetTypeQuery = "SELECT id FROM Pet WHERE type = ?";
        String insertPetTypeQuery = "INSERT INTO Pet (type) VALUES (?)";
        String linkPetToPropertyQuery = "INSERT INTO PropertyPet (property_id, pet_id) VALUES ((SELECT id FROM Property WHERE address = ? LIMIT 1), ?)";

        try (Connection connection = DatabaseConnection.getConnection()) {
            int petId;

            // Check if the pet type already exists
            try (PreparedStatement checkPetTypeStmt = connection.prepareStatement(checkPetTypeQuery)) {
                checkPetTypeStmt.setString(1, pet.getPetType());
                ResultSet rs = checkPetTypeStmt.executeQuery();
                if (rs.next()) {
                    petId = rs.getInt("id");  // Pet type exists, get the pet ID
                } else {
                    // Insert new pet type if it doesn't exist
                    try (PreparedStatement insertPetTypeStmt = connection.prepareStatement(insertPetTypeQuery, PreparedStatement.RETURN_GENERATED_KEYS)) {
                        insertPetTypeStmt.setString(1, pet.getPetType());
                        insertPetTypeStmt.executeUpdate();

                        // Get the generated ID of the new pet type
                        ResultSet generatedKeys = insertPetTypeStmt.getGeneratedKeys();
                        if (generatedKeys.next()) {
                            petId = generatedKeys.getInt(1);
                        } else {
                            throw new SQLException("Failed to insert new pet type: " + pet.getPetType());
                        }
                    }
                }
            }

            // Link the pet to the property
            try (PreparedStatement linkStmt = connection.prepareStatement(linkPetToPropertyQuery)) {
                linkStmt.setString(1, propertyAddress);
                linkStmt.setInt(2, petId);
                int rowsAffected = linkStmt.executeUpdate();
                if (rowsAffected == 0) {
                    throw new SQLException("Failed to link pet to property. Property address may be invalid.");
                }
            }
        }
    }

    public static void addPropertyToDatabase(IProperty property) {
        String checkOwnerQuery = "SELECT id FROM Person WHERE name = ?";
        String insertOwnerQuery = "INSERT INTO Person (name) VALUES (?)";
        String insertPropertyQuery = "INSERT INTO Property (address, owner_id, floor, isGround, type) VALUES (?, ?, ?, ?, ?)";

        try (Connection connection = DatabaseConnection.getConnection()) {
            int ownerId;

            // Check if the owner exists in the Person table
            try (PreparedStatement checkOwnerStmt = connection.prepareStatement(checkOwnerQuery)) {
                checkOwnerStmt.setString(1, property.getOwner().getName());
                ResultSet rs = checkOwnerStmt.executeQuery();

                if (rs.next()) {
                    // Owner exists, get the owner ID
                    ownerId = rs.getInt("id");
                } else {
                    // Owner does not exist, insert new owner
                    try (PreparedStatement insertOwnerStmt = connection.prepareStatement(insertOwnerQuery, PreparedStatement.RETURN_GENERATED_KEYS)) {
                        insertOwnerStmt.setString(1, property.getOwner().getName());
                        insertOwnerStmt.executeUpdate();

                        // Get generated owner ID
                        ResultSet generatedKeys = insertOwnerStmt.getGeneratedKeys();
                        if (generatedKeys.next()) {
                            ownerId = generatedKeys.getInt(1);
                        } else {
                            throw new SQLException("Failed to retrieve owner ID after insertion.");
                        }
                    }
                }
            }

            // Insert the property using the owner ID
            try (PreparedStatement insertPropertyStmt = connection.prepareStatement(insertPropertyQuery)) {
                insertPropertyStmt.setString(1, property.getAddress());
                insertPropertyStmt.setInt(2, ownerId);

                if (property instanceof House) {
                    insertPropertyStmt.setNull(3, java.sql.Types.INTEGER);  // No floor for houses
                    insertPropertyStmt.setBoolean(4, true);  // Ground level for houses
                    insertPropertyStmt.setString(5, "House");
                } else if (property instanceof Apartment) {
                    Apartment apartment = (Apartment) property;
                    insertPropertyStmt.setInt(3, apartment.getFloor());
                    insertPropertyStmt.setBoolean(4, false);  // Not ground level for apartments
                    insertPropertyStmt.setString(5, "Apartment");
                }

                insertPropertyStmt.executeUpdate();

                // Link pets to the property
                for (Pet pet : property.getPets()) {
                    addPetToDatabase(pet, property.getAddress());
                }

                System.out.println("Property added successfully.");

            }
        } catch (SQLException e) {
            System.err.println("Error: " + e.getMessage());
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