package builder;

import model.Floor;
import model.ParkingLot;
import model.ParkingSpot;
import model.Row;
import enums.SpotType;

//builds a ParkingLot step-by-step.

public class ParkingLotBuilder {

    private String name = "Parking Lot";
    private int numFloors = 1;
    private int rowsPerFloor = 1;
    private int spotsPerRow = 1;

    private int compactPerRow = 0;
    private int regularPerRow = 0;
    private int handicappedPerRow = 0;
    private int reservedPerRow = 0;

    // Step 1: Set parking lot name
    public ParkingLotBuilder setName(String name) {
        this.name = name;
        return this;
    }

    // Step 2: Set number of floors
    public ParkingLotBuilder setNumFloors(int numFloors) {
        this.numFloors = numFloors;
        return this;
    }

    // Step 3: Set rows per floor
    public ParkingLotBuilder setRowsPerFloor(int rowsPerFloor) {
        this.rowsPerFloor = rowsPerFloor;
        return this;
    }

    // Step 4: Set spots per row
    public ParkingLotBuilder setSpotsPerRow(int spotsPerRow) {
        this.spotsPerRow = spotsPerRow;
        return this;
    }

    // Step 5: Configure spot type distribution per row
    public ParkingLotBuilder setSpotDistributionPerRow(
            int compact, int regular, int handicapped, int reserved) {

        this.compactPerRow = compact;
        this.regularPerRow = regular;
        this.handicappedPerRow = handicapped;
        this.reservedPerRow = reserved;

        return this;
    }

    // Final Step: Build the ParkingLot object
    public ParkingLot build() {

        // Basic validation
        int total = compactPerRow + regularPerRow + handicappedPerRow + reservedPerRow;
        if (total != spotsPerRow) {
            throw new IllegalArgumentException(
                "Spot distribution per row must add up to spotsPerRow. " +
                "Expected " + spotsPerRow + " but got " + total
            );
        }

        ParkingLot lot = new ParkingLot(name);

        // Create floors
        for (int f = 1; f <= numFloors; f++) {
            Floor floor = new Floor(f);

            // Create rows in each floor
            for (int r = 1; r <= rowsPerFloor; r++) {
                Row row = new Row(r);

                // Add spots to row following distribution
                int sNo = 1;

                sNo = addSpots(row, f, r, sNo, SpotType.COMPACT, compactPerRow);
                sNo = addSpots(row, f, r, sNo, SpotType.REGULAR, regularPerRow);
                sNo = addSpots(row, f, r, sNo, SpotType.HANDICAPPED, handicappedPerRow);
                sNo = addSpots(row, f, r, sNo, SpotType.RESERVED, reservedPerRow);

                floor.addRow(row);
            }

            lot.addFloor(floor);
        }

        return lot;
    }

    // Helper method to add N spots of a given type
    private int addSpots(Row row, int floorNo, int rowNo, int startSpotNo,
                         SpotType type, int count) {

        int spotNo = startSpotNo;

        for (int i = 0; i < count; i++) {
            String spotId = "F" + floorNo + "-R" + rowNo + "-S" + spotNo;
            row.addSpot(new ParkingSpot(spotId, type));
            spotNo++;
        }

        return spotNo;
    }
}
