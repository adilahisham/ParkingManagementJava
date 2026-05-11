package model;

import java.util.ArrayList;
import java.util.List;

//represents a single level in the parking lot.

public class Floor {

    private int floorNo;
    private List<Row> rows;

    // Constructor
    public Floor(int floorNo) {
        this.floorNo = floorNo;
        this.rows = new ArrayList<>();
    }

    // Add a row to this floor
    public void addRow(Row row) {
        this.rows.add(row);
    }

    // Get floor number
    public int getFloorNo() {
        return floorNo;
    }

    // Return all rows
    public List<Row> getRows() {
        return rows;
    }

    // Get all spots in this floor
    public List<ParkingSpot> getAllSpots() {
        List<ParkingSpot> allSpots = new ArrayList<>();

        for (Row row : rows) {
            allSpots.addAll(row.getSpots());
        }

        return allSpots;
    }

    // Find a spot by ID inside this floor
    public ParkingSpot findSpotById(String spotId) {
        for (Row row : rows) {
            ParkingSpot spot = row.findSpotById(spotId);
            if (spot != null) {
                return spot;
            }
        }
        return null;
    }
}

