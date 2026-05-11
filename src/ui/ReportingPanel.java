package ui;

import data.DataStore;
import java.awt.*;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import model.FineRecord;
import model.ParkingSession;

public class ReportingPanel extends JPanel {

    private final DataStore store;

    private JTable vehiclesTable;
    private JTable finesTable;
    private JLabel revenueLabel;
    private JLabel occupancyLabel;

    private DefaultTableModel vehiclesModel;
    private DefaultTableModel finesModel;

    public ReportingPanel(DataStore store) {
        this.store = store;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        initTopPanel();
        initCenterPanel();

        refreshStats(); // Initial load
    }

    // --- TOP PANEL: Revenue & Occupancy ---
    private void initTopPanel() {
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 30, 10));

        revenueLabel = new JLabel("Total Revenue: RM 0.00");
        revenueLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        topPanel.add(revenueLabel);

        occupancyLabel = new JLabel("Occupancy: 0 / 0");
        occupancyLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        topPanel.add(occupancyLabel);

        add(topPanel, BorderLayout.NORTH);
    }

    // --- CENTER PANEL: Vehicles & Fines ---
    private void initCenterPanel() {
        JPanel centerPanel = new JPanel(new GridLayout(1, 2, 15, 0));

        // Vehicles inside
        vehiclesModel = new DefaultTableModel(new String[]{"Plate", "Spot ID", "Entry Time"}, 0);
        vehiclesTable = new JTable(vehiclesModel);
        JScrollPane vehiclesScroll = new JScrollPane(vehiclesTable);
        vehiclesScroll.setBorder(BorderFactory.createTitledBorder("Vehicles Currently Inside"));
        centerPanel.add(vehiclesScroll);

        // Outstanding fines
        finesModel = new DefaultTableModel(new String[]{"Plate", "Reason", "Amount"}, 0);
        finesTable = new JTable(finesModel);
        JScrollPane finesScroll = new JScrollPane(finesTable);
        finesScroll.setBorder(BorderFactory.createTitledBorder("Outstanding Fines"));
        centerPanel.add(finesScroll);

        add(centerPanel, BorderLayout.CENTER);
    }

    // --- REFRESH ALL STATS ---
    public void refreshStats() {
        refreshVehicles();
        refreshFines();
        refreshRevenueAndOccupancy();
    }

    private void refreshVehicles() {
        vehiclesModel.setRowCount(0); // Clear old data
        List<ParkingSession> activeSessions = store.getAllActiveSessions();
        for (ParkingSession session : activeSessions) {
            vehiclesModel.addRow(new Object[]{
                    session.getPlate(),
                    session.getSpotId(),
                    session.getEntryTime()
            });
        }
    }

    private void refreshFines() {
        finesModel.setRowCount(0); // Clear old data
        List<FineRecord> unpaidFines = store.getAllUnpaidFines();
        for (FineRecord fine : unpaidFines) {
            finesModel.addRow(new Object[]{
                    fine.getPlate(),
                    fine.getReason().name(),
                    String.format("RM %.2f", fine.getAmount())
            });
        }
    }

    private void refreshRevenueAndOccupancy() {
        double totalRevenue = store.getTotalRevenue();
        revenueLabel.setText(String.format("Total Revenue: RM %.2f", totalRevenue));

        int occupied = store.getOccupiedSpotCount();
        int totalSpots = store.getTotalSpotCount();
        occupancyLabel.setText(String.format("Occupancy: %d / %d", occupied, totalSpots));
    }
}
