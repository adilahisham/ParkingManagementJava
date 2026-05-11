package app;

import data.DataStore;
import data.SQLiteDataStore;
import fine.*;
import javax.swing.SwingUtilities;
import service.EntryService;
import service.ExitService;
import service.PaymentProcessor;

public class main {  // renamed to Main (standard convention)

    public static void main(String[] args) {
        // 1. Database Connection
        DataStore store = new SQLiteDataStore();
        store.connect();
        store.initSchema();

        // 2. Build and Seed Parking Lot
        ((SQLiteDataStore) store).initializeSpotsIfNeeded();
        seedParkingLot(store);
        ((SQLiteDataStore) store).syncSpotStatusFromSessions();

        // 3. Load the LAST CHOSEN fine scheme from database (persistent!)
        FineScheme activeScheme = loadLastChosenFineScheme(store);

        // 4. Initialize Services with the persisted scheme
        ExitService exitService = new ExitService(store, activeScheme);
        PaymentProcessor paymentProcessor = new PaymentProcessor(store);
        EntryService entryService = new EntryService(store);

        // 5. Cleanup Hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            store.close();
            System.out.println("Database connection closed safely.");
        }));

        // 6. Launch UI
        SwingUtilities.invokeLater(() -> {
            try {
                // Show Login First
                ui.LoginDialog loginDlg = new ui.LoginDialog(null, store);
                loginDlg.setVisible(true);

                if (loginDlg.isSucceeded()) {
                    String role = loginDlg.getAuthenticatedRole();
                    // Launch MainFrame with all dependencies
                    new ui.MainFrame(store, exitService, entryService, paymentProcessor, role).setVisible(true);
                } else {
                    System.exit(0);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private static void seedParkingLot(DataStore store) {
        builder.ParkingLotBuilder builder = new builder.ParkingLotBuilder()
            .setName("University Parking Lot")
            .setNumFloors(3)
            .setRowsPerFloor(2)
            .setSpotsPerRow(10)
            .setSpotDistributionPerRow(2, 6, 1, 1);  // compact, regular, handicapped, reserved

        model.ParkingLot lot = builder.build();

        int count = 0;
        for (model.Floor floor : lot.getFloors()) {
            for (model.Row row : floor.getRows()) {
                for (model.ParkingSpot spot : row.getSpots()) {
                    store.upsertSpot(spot);
                    count++;
                }
            }
        }
        System.out.println("Parking structure seeded: " + count + " spots.");
    }

    /**
     * Loads the last scheme chosen in AdminPanel from the database.
     * - Uses Progressive only on the very first run (and saves it).
     * - After that, always loads whatever the admin last selected.
     */
    private static FineScheme loadLastChosenFineScheme(DataStore store) {
        String savedSchemeName = store.getActiveFineScheme();

        // First run ever: no value saved → default to Progressive and save it
        if (savedSchemeName == null || savedSchemeName.trim().isEmpty()) {
            savedSchemeName = "Progressive (Tiered)";
            store.setActiveFineScheme(savedSchemeName);
            System.out.println("First run: No fine scheme saved yet → defaulted to Progressive (Tiered) and saved.");
        }

        // Map string → actual FineScheme instance
        return switch (savedSchemeName) {
            case "Fixed Fine (RM 50)"     -> new FixedFineScheme();
            case "Progressive (Tiered)"   -> new ProgressiveFineScheme();
            case "Hourly (RM 20/hr)"      -> new HourlyFineScheme();
            default -> {
                // Safety fallback if DB has invalid value
                System.out.println("Warning: Unknown scheme '" + savedSchemeName + "' in DB → using Progressive");
                yield new ProgressiveFineScheme();
            }
        };
    }
}