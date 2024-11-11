// Class A
package org.example;
import java.io.Serializable;

class Person implements Serializable {
    String name;

    // Constructor to initialize the name
    public Person(String name) {
        this.name = name;
    }

    // Getter for the name property
    public String getName() {
        return name;
    }
}