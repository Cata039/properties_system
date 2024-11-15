package org.example;

public class PropertyNotFoundException extends Exception {
  public PropertyNotFoundException(String propertyAddress) {
    super("Property not found: " + propertyAddress);
  }
}
