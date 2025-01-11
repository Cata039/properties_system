package org.example;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

public class UnitTest {

    private Person person;
    private Pet[] pets;
    private House house;
    private Apartment apartment;
    private Lease lease;

    @BeforeEach
    void setUp() {
        person = new Person("John Doe");
        pets = new Pet[]{new Pet("Dog"), new Pet("Cat")};
        house = new House(person, pets, "123 Main St", true);
        apartment = new Apartment(person, 3, "456 Elm St", pets);
        lease = new Lease("Jane Doe", new Date(2024, 1, 1), new Date(2025, 1, 1), 1200.00, "123 Main St");
    }

    // Constructor Tests
    @Test
    void testPersonConstructor() {
        assertEquals("John Doe", person.getName());
    }

    @Test
    void testPetConstructor() {
        assertEquals("Dog", pets[0].getPetType());
    }

    @Test
    void testHouseConstructor() {
        assertEquals("123 Main St", house.getAddress());
        assertTrue(house.isGround());
        assertEquals(2, house.getPets().length);
    }

    @Test
    void testApartmentConstructor() {
        assertEquals("456 Elm St", apartment.getAddress());
        assertEquals(3, apartment.getFloor());
        assertEquals(2, apartment.countPets());
    }

    @Test
    void testLeaseConstructor() {
        assertEquals("Jane Doe", lease.getTenantName());
        assertEquals(1200.00, lease.getMonthlyRent());
    }

    // Functionality Tests
    @Test
    void testCountPets() {
        assertEquals(2, house.getPets().length);
        assertEquals(2, apartment.countPets());
    }

    @Test
    void testGetDescriptionHouse() {
        String description = house.getDescription();
        assertTrue(description.contains("John Doe"));
        assertTrue(description.contains("Dog"));
        assertTrue(description.contains("ground level"));
    }

    @Test
    void testGetDescriptionApartment() {
        String description = apartment.getDescription();
        assertTrue(description.contains("John Doe"));
        assertTrue(description.contains("floor 3"));
        assertTrue(description.contains("Dog"));
    }

    @Test
    void testTerminateLease() {
        lease.terminateContract();
        assertTrue(lease.toString().contains("isTerminated=true"), "Expected lease termination message in toString()");
    }


    @Test
    void testCompareTo() {
        Apartment apt2 = new Apartment(person, 5, "789 Pine St", pets);
        assertTrue(apartment.compareTo(apt2) < 0);
    }

    @Test
    void testIsGroundHouse() {
        assertTrue(house.isGround());
    }

    @Test
    void testInvalidAddressThrowsException() {
        Exception exception = assertThrows(InvalidAddressFormatException.class, () -> Main.validateAddress("InvalidAddress"));
        assertTrue(exception.getMessage().contains("Invalid address format"));
    }

//    @Test
//    public void testThreadPool() throws Exception {
//        ExecutorService executor = Executors.newFixedThreadPool(4);
//        List<Future<Integer>> futures = new ArrayList<>();
//
//        for (int i = 0; i < 10; i++) {
//            futures.add(executor.submit(() -> {
//                Thread.sleep(200);  // Simulate delay
//                return 42;
//            }));
//        }
//
//        for (Future<Integer> future : futures) {
//            assertEquals(42, (int) future.get());
//        }
//
//        executor.shutdown();
//        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
//    }

}
