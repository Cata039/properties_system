package org.example;

import java.io.*;

public class InputDevice {
    private BufferedReader reader;

    public InputDevice(String fileName) {
        try {
            reader = new BufferedReader(new FileReader(fileName));
        } catch (FileNotFoundException e) {
            System.out.println("Input file not found: " + fileName);
            e.printStackTrace();
        }
    }

    public String nextLine() {
        try {
            return reader.readLine();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void close() {
        try {
            if (reader != null) {
                reader.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
