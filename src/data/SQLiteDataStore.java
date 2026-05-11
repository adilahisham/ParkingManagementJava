package data;

import enums.FineReason;
import enums.PaymentMethod;
import enums.SpotStatus;
import enums.SpotType;
import java.sql.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import model.FineRecord;
import model.ParkingSession;
import model.ParkingSpot;
import model.PaymentRecord;
import model.Vehicle;

public class SQLiteDataStore implements DataStore {

    private static final String DB_URL = "jdbc:sqlite:parking.db";
    private Connection conn;

    @Override
    public void connect() {
        try {
            this.conn = DriverManager.getConnection(DB_URL);
            if (this.conn != null) System.out.println("Connected to parking.db");
        } catch (SQLException e) {
            System.err.println("Connection failed: " + e.getMessage());
        }
    }

    @Override
    public void close() {
        try { if (conn != null) conn.close(); } catch (SQLException e) { e.printStackTrace(); }
    }

    @Override
    public void initSchema() {
        String[] tables = {
            """
            CREATE TABLE IF NOT EXISTS users (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                username VARCHAR(50) NOT NULL UNIQUE,
                password VARCHAR(50) NOT NULL,
                role VARCHAR(20) NOT NULL
            );
                   
            """,
            """
            CREATE TABLE IF NOT EXISTS parking_spot (
                spot_id TEXT PRIMARY KEY,
                spot_type TEXT NOT NULL,
                status TEXT NOT NULL,
                hourly_rate REAL NOT NULL,
                current_plate TEXT
            );
            """,
            """
            CREATE TABLE IF NOT EXISTS parking_session (
                session_id INTEGER PRIMARY KEY AUTOINCREMENT,
                ticket_no TEXT UNIQUE NOT NULL,
                plate TEXT NOT NULL,
                spot_id TEXT NOT NULL,
                vehicle_type TEXT,
                has_hc_card INTEGER,
                is_vip INTEGER,
                fine_scheme TEXT,
                entry_time TEXT NOT NULL,
                exit_time TEXT,
                duration_hours INTEGER,
                parking_fee REAL,
                FOREIGN KEY (spot_id) REFERENCES parking_spot(spot_id)
            );
            """,
            """
            CREATE TABLE IF NOT EXISTS fine (
                fine_id INTEGER PRIMARY KEY AUTOINCREMENT,
                plate TEXT NOT NULL,
                reason TEXT NOT NULL,
                amount REAL NOT NULL,
                issued_at TEXT NOT NULL,
                paid INTEGER NOT NULL DEFAULT 0,
                paid_at TEXT
            );
            """,
            """
            CREATE TABLE IF NOT EXISTS payment (
                payment_id INTEGER PRIMARY KEY AUTOINCREMENT,
                ticket_no TEXT NOT NULL,
                plate TEXT NOT NULL,
                method TEXT NOT NULL,
                paid_time TEXT NOT NULL,
                parking_fee REAL NOT NULL,
                fine_paid REAL NOT NULL,
                total_due REAL NOT NULL,
                amount_paid REAL NOT NULL,
                balance REAL NOT NULL
            );
            """,
            """
            CREATE TABLE IF NOT EXISTS config (
                key VARCHAR(50) PRIMARY KEY,
                value VARCHAR(100)
            );
            """,
        };

        try (Statement stmt = conn.createStatement()) {
            for (String sql : tables) stmt.execute(sql);
            System.out.println("Database tables ready.");
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @Override
    public void setActiveFineScheme(String scheme) {
        String sql = "INSERT INTO config(key, value) VALUES('active_fine_scheme', ?) " +
                    "ON CONFLICT(key) DO UPDATE SET value = ?;";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, scheme);
            stmt.setString(2, scheme);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getActiveFineScheme() {
        String sql = "SELECT value FROM config WHERE key='active_fine_scheme';";
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getString("value");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "Fixed Fine (RM 50)"; // default fallback
    }


    // --- Spot Management ---
    /**
     * Initializes all parking spots if they don't exist yet.
     * Called once at app startup to ensure full parking lot is seeded.
     */
    public void initializeSpotsIfNeeded() {
        // Define your full parking layout here (3 floors, 2 rows per floor, 10 spots per row)
        // Spot distribution: 2 Compact, 6 Regular, 1 Handicapped, 1 Reserved per row
        String[][] spotLayout = {
            // Floor 1, Row 1
            {"F1-R1-S1", "COMPACT"}, {"F1-R1-S2", "COMPACT"}, {"F1-R1-S3", "REGULAR"}, {"F1-R1-S4", "REGULAR"},
            {"F1-R1-S5", "REGULAR"}, {"F1-R1-S6", "REGULAR"}, {"F1-R1-S7", "REGULAR"}, {"F1-R1-S8", "REGULAR"},
            {"F1-R1-S9", "HANDICAPPED"}, {"F1-R1-S10", "RESERVED"},
            // Floor 1, Row 2
            {"F1-R2-S1", "COMPACT"}, {"F1-R2-S2", "COMPACT"}, {"F1-R2-S3", "REGULAR"}, {"F1-R2-S4", "REGULAR"},
            {"F1-R2-S5", "REGULAR"}, {"F1-R2-S6", "REGULAR"}, {"F1-R2-S7", "REGULAR"}, {"F1-R2-S8", "REGULAR"},
            {"F1-R2-S9", "HANDICAPPED"}, {"F1-R2-S10", "RESERVED"},
            // Floor 2, Row 1
            {"F2-R1-S1", "COMPACT"}, {"F2-R1-S2", "COMPACT"}, {"F2-R1-S3", "REGULAR"}, {"F2-R1-S4", "REGULAR"},
            {"F2-R1-S5", "REGULAR"}, {"F2-R1-S6", "REGULAR"}, {"F2-R1-S7", "REGULAR"}, {"F2-R1-S8", "REGULAR"},
            {"F2-R1-S9", "HANDICAPPED"}, {"F2-R1-S10", "RESERVED"},
            // Floor 2, Row 2
            {"F2-R2-S1", "COMPACT"}, {"F2-R2-S2", "COMPACT"}, {"F2-R2-S3", "REGULAR"}, {"F2-R2-S4", "REGULAR"},
            {"F2-R2-S5", "REGULAR"}, {"F2-R2-S6", "REGULAR"}, {"F2-R2-S7", "REGULAR"}, {"F2-R2-S8", "REGULAR"},
            {"F2-R2-S9", "HANDICAPPED"}, {"F2-R2-S10", "RESERVED"},
            // Floor 3, Row 1
            {"F3-R1-S1", "COMPACT"}, {"F3-R1-S2", "COMPACT"}, {"F3-R1-S3", "REGULAR"}, {"F3-R1-S4", "REGULAR"},
            {"F3-R1-S5", "REGULAR"}, {"F3-R1-S6", "REGULAR"}, {"F3-R1-S7", "REGULAR"}, {"F3-R1-S8", "REGULAR"},
            {"F3-R1-S9", "HANDICAPPED"}, {"F3-R1-S10", "RESERVED"},
            // Floor 3, Row 2
            {"F3-R2-S1", "COMPACT"}, {"F3-R2-S2", "COMPACT"}, {"F3-R2-S3", "REGULAR"}, {"F3-R2-S4", "REGULAR"},
            {"F3-R2-S5", "REGULAR"}, {"F3-R2-S6", "REGULAR"}, {"F3-R2-S7", "REGULAR"}, {"F3-R2-S8", "REGULAR"},
            {"F3-R2-S9", "HANDICAPPED"}, {"F3-R2-S10", "RESERVED"}
        };

        int createdCount = 0;
        for (String[] spotData : spotLayout) {
            String spotId = spotData[0];
            SpotType type = SpotType.valueOf(spotData[1]);

            if (!spotExists(spotId)) {
                ParkingSpot newSpot = new ParkingSpot(spotId, type);
                upsertSpot(newSpot);  // inserts with AVAILABLE status
                createdCount++;
                System.out.println("Created missing spot: " + spotId + " (" + type + ")");
            }
        }
        if (createdCount == 0) {
            System.out.println("All " + spotLayout.length + " spots already exist — no changes needed.");
        } else {
            System.out.println("Initialized " + createdCount + " missing spots. Total now: " + spotLayout.length);
        }
    }

    /**
     * Helper: Checks if a spot already exists in the database.
     */
    private boolean spotExists(String spotId) {
        String sql = "SELECT 1 FROM parking_spot WHERE spot_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, spotId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();  // true if row exists
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * On startup: ensure all open sessions have their spots marked OCCUPIED.
     * Call this after seeding/initializing spots.
     */
    public void syncSpotStatusFromSessions() {
        List<ParkingSession> openSessions = getAllActiveSessions();
        int fixedCount = 0;

        for (ParkingSession session : openSessions) {
            String spotId = session.getSpotId();
            String plate = session.getPlate();

            // Check current spot status
            ParkingSpot spot = getSpotById(spotId);  // add this helper if needed
            if (spot != null && spot.isAvailable()) {
                // Spot exists but is AVAILABLE → fix it
                setSpotOccupied(spotId, plate);
                fixedCount++;
                System.out.println("Fixed inconsistent status: Spot " + spotId + 
                                " marked OCCUPIED for plate " + plate);
            }
        }

        if (fixedCount == 0) {
            System.out.println("Spot statuses are consistent with open sessions.");
        } else {
            System.out.println("Fixed " + fixedCount + " inconsistent spot statuses.");
        }
    }

    private ParkingSpot getSpotById(String spotId) {
        String sql = "SELECT * FROM parking_spot WHERE spot_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, spotId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    SpotType type = SpotType.valueOf(rs.getString("spot_type").toUpperCase());
                    ParkingSpot spot = new ParkingSpot(spotId, type);
                    spot.setStatus(SpotStatus.valueOf(rs.getString("status").toUpperCase()));
                    spot.setCurrentVehiclePlate(rs.getString("current_plate"));
                    return spot;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void upsertSpot(ParkingSpot spot) {
        String sql = "INSERT OR REPLACE INTO parking_spot (spot_id, spot_type, status, hourly_rate, current_plate) VALUES (?, ?, ?, ?, ?);";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, spot.getSpotId());
            stmt.setString(2, spot.getType().name());
            stmt.setString(3, spot.getStatus().name());
            stmt.setDouble(4, spot.getHourlyRate());
            stmt.setString(5, spot.getCurrentVehiclePlate());
            stmt.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @Override
    public List<ParkingSpot> getAllSpots() {
        List<ParkingSpot> spots = new ArrayList<>();
        String sql = "SELECT * FROM parking_spot";
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                SpotType type = SpotType.valueOf(rs.getString("spot_type").toUpperCase());
                ParkingSpot spot = new ParkingSpot(rs.getString("spot_id"), type);
                spot.setStatus(SpotStatus.valueOf(rs.getString("status").toUpperCase()));
                spot.setCurrentVehiclePlate(rs.getString("current_plate"));
                spots.add(spot);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return spots;
    }

    @Override
    public List<ParkingSpot> findAvailableSpots(String type) {
        List<ParkingSpot> result = new ArrayList<>();
        String sql = "SELECT * FROM parking_spot WHERE spot_type = ? AND status = 'AVAILABLE'";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, type);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    SpotType spotType = SpotType.valueOf(rs.getString("spot_type").toUpperCase());
                    ParkingSpot spot = new ParkingSpot(rs.getString("spot_id"), spotType);
                    spot.setStatus(SpotStatus.AVAILABLE);
                    result.add(spot);
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return result;
    }

    @Override
    public void setSpotOccupied(String spotId, String plate) {
        String sql = "UPDATE parking_spot SET status = 'OCCUPIED', current_plate = ? WHERE spot_id = ?;";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, plate);
            stmt.setString(2, spotId);
            stmt.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @Override
    public void setSpotAvailable(String spotId) {
        String sql = "UPDATE parking_spot SET status = 'AVAILABLE', current_plate = NULL WHERE spot_id = ?;";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, spotId);
            stmt.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    // --- Session Management ---
// --- Create new session ---
    @Override
    public void createSession(ParkingSession session) {
        // Now includes fine_scheme column
        String sql = "INSERT INTO parking_session (ticket_no, plate, spot_id, entry_time, vehicle_type, has_hc_card, is_vip, fine_scheme) VALUES (?, ?, ?, ?, ?, ?, ?, ?);";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, session.getTicketNo());
            stmt.setString(2, session.getPlate());
            stmt.setString(3, session.getSpotId());
            stmt.setString(4, session.getEntryTime());
            stmt.setString(5, session.getVehicle().getType()); 
            stmt.setInt(6, session.getVehicle().hasHcCard() ? 1 : 0);
            stmt.setInt(7, session.getVehicle().isVIP() ? 1 : 0);
            
            // Use the fine scheme attached to the session object
            stmt.setString(8, session.getFineScheme()); 
            
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Reusable helper to map database rows back to Java objects.
     * This eliminates the "Memory Loss" for HC cards and Fine Schemes.
     */
    private ParkingSession mapResultSetToSession(ResultSet rs) throws SQLException {
        // Reconstruct Vehicle with actual data
        Vehicle vehicle = new Vehicle(
            rs.getString("plate"),
            rs.getString("vehicle_type"),
            rs.getInt("has_hc_card") == 1,
            rs.getInt("is_vip") == 1
        );

        // Reconstruct Session with the fine scheme stored at entry
        return new ParkingSession(
            rs.getString("ticket_no"),
            vehicle,
            rs.getString("spot_id"),
            rs.getString("entry_time"),
            rs.getString("fine_scheme") // No more hardcode!
        );
    }

    @Override
    public ParkingSession getOpenSessionByPlate(String plate) {
        String sql = "SELECT * FROM parking_session WHERE plate = ? AND exit_time IS NULL;";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, plate);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return mapResultSetToSession(rs);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    @Override
    public List<ParkingSession> getAllActiveSessions() {
        List<ParkingSession> sessions = new ArrayList<>();
        String sql = "SELECT * FROM parking_session WHERE exit_time IS NULL;";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                sessions.add(mapResultSetToSession(rs));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return sessions;
    }


    // --- Close session ---
    @Override
    public void closeSession(String ticketNo, String exitTimeISO, int durationHours, double parkingFee) {
        String sql = "UPDATE parking_session SET exit_time = ?, duration_hours = ?, parking_fee = ? WHERE ticket_no = ?;";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, exitTimeISO);
            stmt.setInt(2, durationHours);
            stmt.setDouble(3, parkingFee);
            stmt.setString(4, ticketNo);
            stmt.executeUpdate();
            System.out.println("Session " + ticketNo + " closed successfully.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    // --- Fine Management ---
    @Override
    public void addFine(FineRecord fine) {
        String sql = "INSERT INTO fine (plate, reason, amount, issued_at, paid) VALUES (?, ?, ?, ?, ?);";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, fine.getPlate());
            stmt.setString(2, fine.getReason().name());  // store enum name
            stmt.setDouble(3, fine.getAmount());
            stmt.setString(4, fine.getIssuedTime());
            stmt.setInt(5, fine.isPaid() ? 1 : 0);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<FineRecord> getUnpaidFinesByPlate(String plate) {
        List<FineRecord> fines = new ArrayList<>();
        String sql = "SELECT * FROM fine WHERE plate = ? AND paid = 0;";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, plate);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {

                    FineReason reason =
                            FineReason.valueOf(rs.getString("reason"));

                    fines.add(new FineRecord(
                            rs.getInt("fine_id"),
                            rs.getString("plate"),
                            reason,
                            rs.getDouble("amount"),
                            rs.getString("issued_at"),
                            rs.getInt("paid") != 0,
                            rs.getString("paid_at")
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return fines;
    }


    @Override
    public void markAllFinesPaid(String plate, String paidTimeISO) {
        String sql = "UPDATE fine SET paid = 1, paid_at = ? WHERE plate = ? AND paid = 0;";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, paidTimeISO);
            stmt.setString(2, plate);
            stmt.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @Override
    public void reduceFineAmount(FineRecord fine, double amount) {

        // First update the object
        fine.reduceAmount(amount);

        boolean fullyPaid = fine.getAmount() <= 0;

        String sql = "UPDATE fine SET amount = ?, paid = ?, paid_at = ? WHERE fine_id = ?;";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDouble(1, fine.getAmount());
            stmt.setInt(2, fullyPaid ? 1 : 0);
            stmt.setString(3,
                    fullyPaid ? java.time.LocalDateTime.now().toString()
                            : fine.getPaidAt());

            stmt.setInt(4, fine.getId());

            stmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    // --- Payment Management ---
    @Override
    public void createPayment(PaymentRecord payment) {
        String sql = """
            INSERT INTO payment
            (ticket_no, plate, method, paid_time, parking_fee, fine_paid, total_due, amount_paid, balance)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);
        """;
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, payment.getTicketNo());
            stmt.setString(2, payment.getPlate());
            
            // Convert enum to String
            stmt.setString(3, payment.getMethod().name());
            
            // Convert LocalDateTime to string in SQL format
            stmt.setString(4, payment.getPaidTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            
            stmt.setDouble(5, payment.getParkingFee());
            stmt.setDouble(6, payment.getFinePaid());
            stmt.setDouble(7, payment.getTotalDue());
            stmt.setDouble(8, payment.getAmountPaid());
            stmt.setDouble(9, payment.getBalance());
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    @Override
    public List<FineRecord> getAllUnpaidFines() {
        List<FineRecord> fines = new ArrayList<>();
        String sql = "SELECT * FROM fine WHERE paid = 0;";
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                FineReason reason = FineReason.valueOf(rs.getString("reason"));
                fines.add(new FineRecord(
                        rs.getInt("fine_id"),
                        rs.getString("plate"),
                        reason,
                        rs.getDouble("amount"),
                        rs.getString("issued_at"),
                        rs.getInt("paid") != 0,
                        rs.getString("paid_at")
                ));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return fines;
    }

    @Override
    public List<PaymentRecord> getPaymentsByTicket(String ticketNo) {

        List<PaymentRecord> payments = new ArrayList<>();

        String sql = "SELECT * FROM payment WHERE ticket_no = ?;";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, ticketNo);

            try (ResultSet rs = stmt.executeQuery()) {

                while (rs.next()) {

                    PaymentRecord record = new PaymentRecord(
                            rs.getString("ticket_no"),
                            rs.getString("plate"),
                            PaymentMethod.valueOf(rs.getString("method")),
                            rs.getTimestamp("paid_time").toLocalDateTime(),
                            0, // duration not stored in payment table
                            rs.getDouble("parking_fee"),
                            rs.getDouble("fine_paid"),
                            rs.getDouble("amount_paid")
                    );

                    payments.add(record);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return payments;
    }



    // --- Admin Stats ---
    @Override
    public double getTotalRevenue() {
        String sql = "SELECT SUM(amount_paid) FROM payment;";
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getDouble(1);
        } catch (SQLException e) { e.printStackTrace(); }
        return 0.0;
    }

    @Override
    public double getTotalUnpaidFines() {
        String sql = "SELECT SUM(amount) FROM fine WHERE paid = 0;";
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getDouble(1);
        } catch (SQLException e) { e.printStackTrace(); }
        return 0.0;
    }

    @Override
    public int getOccupiedSpotCount() {
        String sql = "SELECT COUNT(*) FROM parking_spot WHERE status = 'OCCUPIED';";
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    @Override
    public int getTotalSpotCount() {
        String sql = "SELECT COUNT(*) FROM parking_spot;";
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    // --- Auth ---
    @Override
    public String authenticate(String username, String password) {
        String sql = "SELECT role FROM users WHERE username = ? AND password = ?;";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setString(2, password);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getString("role");
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    @Override
    public Map<String, ParkingSession> getOccupiedSpotsMap() {
        java.util.Map<String, ParkingSession> map = new java.util.HashMap<>();
        // We reuse your existing method to get active sessions
        List<ParkingSession> activeSessions = getAllActiveSessions();
        
        for (ParkingSession session : activeSessions) {
            map.put(session.getSpotId(), session);
        }
        return map;
    }


    // Boilerplate for interface compatibility
    @Override public List<model.ParkingSpot> getAvailableSpots(String type) { return findAvailableSpots(type); }


}