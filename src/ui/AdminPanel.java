package ui;

import data.DataStore;
import java.awt.*;
import javax.swing.*;
import model.ParkingSession;
import service.ExitService;

public class AdminPanel extends JPanel {

    private final ExitService exitService;
    private final DataStore store;

    // UI Components
    private JLabel lblOccupancy, lblRevenue, lblUnpaidFines;
    private DefaultListModel<String> vehiclesListModel;
    private JComboBox<String> schemeDropdown;
    private JPanel floorsContainer; // Container for the floor maps

    public AdminPanel(ExitService exitService, DataStore store) {
        this.exitService = exitService;
        this.store = store;

        setLayout(new BorderLayout(15, 15));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        initComponents();
        refreshStats(); 
    }

    private void initComponents() {
        initTopStats();
        
        // Main Dashboard Split: Left (List/Config) | Right (Floor Map)
        JPanel mainContent = new JPanel(new GridLayout(1, 2, 20, 0));
        
        // --- Left Side: Existing List and Config ---
        JPanel leftSide = new JPanel(new GridLayout(2, 1, 0, 20));
        
        vehiclesListModel = new DefaultListModel<>();
        JList<String> vehiclesList = new JList<>(vehiclesListModel);
        vehiclesList.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane vehicleScroll = new JScrollPane(vehiclesList);
        vehicleScroll.setBorder(BorderFactory.createTitledBorder("Vehicles Currently Parked"));
        leftSide.add(vehicleScroll);

        JPanel finePanel = new JPanel(new GridBagLayout());
        finePanel.setBorder(BorderFactory.createTitledBorder("System Configuration"));
        initFineConfig(finePanel);
        leftSide.add(finePanel);

        // --- Right Side: Floor Map View ---
        floorsContainer = new JPanel(new BorderLayout());
        floorsContainer.setBorder(BorderFactory.createTitledBorder("Parking Floor Map (Real-Time)"));
        
        mainContent.add(leftSide);
        mainContent.add(floorsContainer);

        add(mainContent, BorderLayout.CENTER);
        initBottomPanel();
    }

    private void initTopStats() {
        JPanel statsPanel = new JPanel(new GridLayout(1, 3, 20, 0));
        lblOccupancy = createStatCard(statsPanel, "Occupancy Rate", "0/60 (0%)", new Color(52, 152, 219));
        lblRevenue = createStatCard(statsPanel, "Total Revenue", "RM 0.00", new Color(46, 204, 113));
        lblUnpaidFines = createStatCard(statsPanel, "Unpaid Fines", "RM 0.00", new Color(231, 76, 60));
        add(statsPanel, BorderLayout.NORTH);
    }

    private JLabel createStatCard(JPanel parent, String title, String value, Color bgColor) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        card.setBackground(Color.WHITE);

        JLabel titleLabel = new JLabel(title, SwingConstants.CENTER);
        titleLabel.setOpaque(true);
        titleLabel.setBackground(bgColor);
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        titleLabel.setPreferredSize(new Dimension(0, 35));

        JLabel valLabel = new JLabel(value, SwingConstants.CENTER);
        valLabel.setFont(new Font("SansSerif", Font.BOLD, 22));
        valLabel.setPreferredSize(new Dimension(0, 80));

        card.add(titleLabel, BorderLayout.NORTH);
        card.add(valLabel, BorderLayout.CENTER);
        parent.add(card);
        return valLabel;
    }

    private void initFineConfig(JPanel finePanel) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        finePanel.add(new JLabel("Active Fine Policy:"), gbc);

        String[] options = {"Fixed Fine (RM 50)", "Progressive (Tiered)", "Hourly (RM 20/hr)"};
        schemeDropdown = new JComboBox<>(options);
        schemeDropdown.setSelectedItem(store.getActiveFineScheme());
        
        gbc.gridx = 1;
        finePanel.add(schemeDropdown, gbc);

        JButton btnApply = new JButton("Update Policy");
        btnApply.setBackground(new Color(44, 62, 80));
        btnApply.setForeground(Color.WHITE);
        gbc.gridy = 1; gbc.gridx = 0; gbc.gridwidth = 2;
        finePanel.add(btnApply, gbc);

        btnApply.addActionListener(e -> {
            String scheme = (String) schemeDropdown.getSelectedItem();
            store.setActiveFineScheme(scheme);
            JOptionPane.showMessageDialog(this, "Fine policy updated to: " + scheme);
            refreshStats();
        });
    }

    private void renderFloorMap() {
        floorsContainer.removeAll();
        JTabbedPane floorTabs = new JTabbedPane();

        java.util.Map<String, ParkingSession> activeSessions = store.getOccupiedSpotsMap(); 
        java.util.List<model.ParkingSpot> allSpots = store.getAllSpots();

        for (int f = 1; f <= 3; f++) {
            JPanel floorGrid = new JPanel(new GridLayout(0, 5, 10, 10));
            floorGrid.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
            floorGrid.setBackground(Color.WHITE);

            final String floorPrefix = "F" + f;
            
            allSpots.stream()
                .filter(s -> s.getSpotId().startsWith(floorPrefix)) // Changed getId() to getSpotId()
                .forEach(spot -> {
                    String id = spot.getSpotId(); // Using getSpotId()
                    boolean isOccupied = activeSessions.containsKey(id);
                    
                    // Handling SpotType enum: convert to string before substring
                    String typeStr = spot.getType().toString(); 
                    String statusText = isOccupied ? id + "OC..." : id + " (" + typeStr.substring(0,2) + "...)";
                    
                    JLabel spotLabel = new JLabel(statusText, SwingConstants.CENTER);
                    spotLabel.setOpaque(true);
                    spotLabel.setFont(new Font("SansSerif", Font.BOLD, 11));
                    spotLabel.setPreferredSize(new Dimension(110, 60));
                    spotLabel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));

                    if (isOccupied) {
                        spotLabel.setBackground(new Color(231, 76, 60)); // Red
                        spotLabel.setForeground(Color.WHITE);
                        spotLabel.setToolTipText("Occupied by: " + activeSessions.get(id).getPlate());
                    } else {
                        spotLabel.setBackground(new Color(46, 204, 113)); // Green
                        spotLabel.setForeground(Color.BLACK);
                        spotLabel.setToolTipText("Available (" + typeStr + ")");
                    }

                    floorGrid.add(spotLabel);
                });

            JScrollPane floorScroll = new JScrollPane(floorGrid);
            floorTabs.addTab("Floor " + f, floorScroll);
        }
        
        floorsContainer.add(floorTabs, BorderLayout.CENTER);
        floorsContainer.revalidate();
        floorsContainer.repaint();
    }

    private void initBottomPanel() {
        JButton refreshBtn = new JButton("Refresh Dashboard");
        refreshBtn.setPreferredSize(new Dimension(0, 45));
        refreshBtn.setFont(new Font("SansSerif", Font.BOLD, 12));
        refreshBtn.addActionListener(e -> refreshStats());
        add(refreshBtn, BorderLayout.SOUTH);
    }

    public void refreshStats() {
        // Update Stats
        lblRevenue.setText(String.format("RM %.2f", store.getTotalRevenue()));
        lblUnpaidFines.setText(String.format("RM %.2f", store.getTotalUnpaidFines()));
        
        int occupied = store.getOccupiedSpotCount();
        int total = store.getTotalSpotCount();
        double percent = (total > 0) ? (occupied * 100.0 / total) : 0;
        lblOccupancy.setText(String.format("%d/%d (%.1f%%)", occupied, total, percent));

        // Update Vehicle List
        vehiclesListModel.clear();
        store.getAllActiveSessions().forEach(session ->
                vehiclesListModel.addElement(String.format("%-10s | Spot: %s", 
                    session.getPlate(), session.getSpotId()))
        );
        
        // Re-render the visual map
        renderFloorMap();
    }
}