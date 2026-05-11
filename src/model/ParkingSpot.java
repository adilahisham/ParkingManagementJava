package model;

import enums.SpotType;
import enums.SpotStatus;

// represents a single parking space in the parking lot
public class ParkingSpot {

    private String spotId;
    private SpotType type;
    private SpotStatus status;
    private String currentVehiclePlate;

    // Constructor
    public ParkingSpot(String spotId, SpotType type) {
        this.spotId = spotId;
        this.type = type;
        this.status = SpotStatus.AVAILABLE;  // default when created
        this.currentVehiclePlate = null;
    }

    // --- ADDED SETTERS FOR DATABASE SYNC ---
    
    public void setStatus(SpotStatus status) {
        this.status = status;
    }

    public void setCurrentVehiclePlate(String currentVehiclePlate) {
        this.currentVehiclePlate = currentVehiclePlate;
    }

    // --- EXISTING GETTERS ---
    
    public String getSpotId() {
        return spotId;
    }

    public SpotType getType() {
        return type;
    }

    public SpotStatus getStatus() {
        return status;
    }

    public String getCurrentVehiclePlate() {
        return currentVehiclePlate;
    }

    public double getHourlyRate() {
        return type.getHourlyRate(); // get rate from enum
    }

    // Check if spot is available
    public boolean isAvailable() {
        return status == SpotStatus.AVAILABLE;
    }

    // Occupy the spot (Logic for UI/Service)
    public void occupy(String plateNumber) {
        this.status = SpotStatus.OCCUPIED;
        this.currentVehiclePlate = plateNumber;
    }

    // Release the spot (Logic for UI/Service)
    public void release() {
        this.status = SpotStatus.AVAILABLE;
        this.currentVehiclePlate = null;
    }
}