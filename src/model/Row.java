package model;

import java.util.ArrayList;
import java.util.List;
import enums.SpotStatus;

//represents one row on a floor

public class Row {

    private int rowNo;
    private List<ParkingSpot> spots;

    // Constructor: start with empty spot list
    public Row(int rowNo) {
        this.rowNo = rowNo;
        this.spots = new ArrayList<>();
    }

    // Add a spot into this row (Builder will use this)
    public void addSpot(ParkingSpot spot) {
        this.spots.add(spot);
    }

    // Get row number
    public int getRowNo() {
        return rowNo;
    }

    // Return all spots in this row
    public List<ParkingSpot> getSpots() {
        return spots;
    }

    // Find a spot by its spotId 
    public ParkingSpot findSpotById(String spotId) {
        for (ParkingSpot s : spots) {
            if (s.getSpotId().equals(spotId)) {
                return s;
            }
        }
        return null; // not found
    }

    // Return only available spots 
    public List<ParkingSpot> getAvailableSpots() {
        List<ParkingSpot> available = new ArrayList<>();
        for (ParkingSpot s : spots) {
            if (s.getStatus() == SpotStatus.AVAILABLE) {
                available.add(s);
            }
        }
        return available;
    }
}
