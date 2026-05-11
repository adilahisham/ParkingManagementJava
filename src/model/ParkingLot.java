package model;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import enums.SpotStatus;
import enums.SpotType;

//represents the whole parking building
//contains multiple floors, and each floor contains rows and spots.


public class ParkingLot {

    private String name;
    private List<Floor> floors;

    // Constructor
    public ParkingLot(String name) {
        this.name = name;
        this.floors = new ArrayList<>();
    }

    // Add a floor
    public void addFloor(Floor floor) {
        this.floors.add(floor);
    }

    public String getName() {
        return name;
    }

    public List<Floor> getFloors() {
        return floors;
    }

    // Get all spots in the entire parking lot
    public List<ParkingSpot> getAllSpots() {
        List<ParkingSpot> all = new ArrayList<>();
        for (Floor f : floors) {
            all.addAll(f.getAllSpots());
        }
        return all;
    }

    // Find a spot by spotId
    public ParkingSpot findSpotById(String spotId) {
        for (Floor f : floors) {
            ParkingSpot spot = f.findSpotById(spotId);
            if (spot != null) {
                return spot;
            }
        }
        return null;
    }

    // Return available spots that match allowed types
    public List<ParkingSpot> getAvailableSpots(Set<SpotType> allowedTypes) {
        List<ParkingSpot> result = new ArrayList<>();

        for (ParkingSpot s : getAllSpots()) {
            if (s.getStatus() == SpotStatus.AVAILABLE && allowedTypes.contains(s.getType())) {
                result.add(s);
            }
        }
        return result;
    }

    // Helper: get occupancy rate (occupied / total)
    public double getOccupancyRate() {
        int total = getAllSpots().size();
        if (total == 0) return 0;

        int occupied = 0;
        for (ParkingSpot s : getAllSpots()) {
            if (s.getStatus() == SpotStatus.OCCUPIED) {
                occupied++;
            }
        }

        return (occupied * 1.0) / total; // 
    }

    // Helper: get number of occupied spots
    public int getOccupiedCount() {
        int occupied = 0;
        for (ParkingSpot s : getAllSpots()) {
            if (s.getStatus() == SpotStatus.OCCUPIED) {
                occupied++;
            }
        }
        return occupied;
    }

    // Optional helper: get number of available spots
    public int getAvailableCount() {
        return getAllSpots().size() - getOccupiedCount();
    }
}
