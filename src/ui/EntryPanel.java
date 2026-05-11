package ui;

import data.DataStore;
import enums.SpotType;
import java.awt.*;
import java.util.List;
import javax.swing.*;
import model.ParkingSpot;
import model.Vehicle;
import service.EntryService;

public class EntryPanel extends JPanel {
    private DataStore store;
    private EntryService entryService;
    private JTextField plateField;
    private JComboBox<String> typeCombo;
    private JCheckBox hcCheckBox;
    private JCheckBox vipCheckBox; // VIP checkbox
    private JPanel gridPanel;
    private String selectedSpotId = null; // Track the chosen spot

    public EntryPanel(DataStore store, EntryService entryService) {
        this.store = store;
        this.entryService = entryService;
        setLayout(new BorderLayout(20, 20));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // --- LEFT: Registration Form ---
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createTitledBorder("Vehicle Registration"));
        formPanel.setPreferredSize(new Dimension(350, 0));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;

        // License Plate Label & Field
        gbc.gridy = 0;
        formPanel.add(new JLabel("License Plate:"), gbc);
        plateField = new JTextField(15);
        gbc.gridy = 1;
        formPanel.add(plateField, gbc);

        // Vehicle Type Label & Combo
        gbc.gridy = 2;
        formPanel.add(new JLabel("Vehicle Type:"), gbc);
        String[] vehicleTypes = {"Motorcycle", "Car", "SUV/Truck", "Handicapped"};
        typeCombo = new JComboBox<>(vehicleTypes);
        typeCombo.addActionListener(e -> refreshSpotGrid());
        gbc.gridy = 3;
        formPanel.add(typeCombo, gbc);

        // HC Card Checkbox
        hcCheckBox = new JCheckBox("Has HC Card Holder?");
        gbc.gridy = 4;
        formPanel.add(hcCheckBox, gbc);

        // VIP Checkbox
        vipCheckBox = new JCheckBox("VIP Customer?");
        vipCheckBox.addActionListener(e -> refreshSpotGrid());
        gbc.gridy = 5;
        formPanel.add(vipCheckBox, gbc);

        // Confirm Button
        JButton confirmBtn = new JButton("Confirm & Print Ticket");
        confirmBtn.setBackground(new Color(46, 204, 113)); // Green
        confirmBtn.setForeground(Color.WHITE);
        confirmBtn.setFont(new Font("SansSerif", Font.BOLD, 14));
        gbc.gridy = 6;
        gbc.weighty = 1.0; 
        gbc.anchor = GridBagConstraints.NORTH;
        formPanel.add(confirmBtn, gbc);

        // --- Confirm Action Listener ---
        confirmBtn.addActionListener(e -> {
            String plate = plateField.getText().trim();
            if (plate.isEmpty() || selectedSpotId == null) {
                JOptionPane.showMessageDialog(this, "Please enter a plate and select a green spot!");
                return;
            }

            // Read HC checkbox
            boolean hasHcCard = hcCheckBox.isSelected();

            // Determine vehicle type
            String vehicleType;
            if (hasHcCard) {
                vehicleType = "HANDICAPPED";
            } else {
                vehicleType = typeCombo.getSelectedItem().toString();
            }

            boolean isVIP = vipCheckBox.isSelected();

            // Normalize plate
            String normalizedPlate = plate.toUpperCase().replace("O", "0");

            // Debug
            System.out.println("[ENTRY DEBUG] Plate: " + normalizedPlate);
            System.out.println("[ENTRY DEBUG] HC checked: " + hasHcCard);
            System.out.println("[ENTRY DEBUG] Vehicle type set to: " + vehicleType);
            System.out.println("[ENTRY DEBUG] VIP: " + isVIP);

            // CREATE VEHICLE - THIS LINE WAS WRONG
            Vehicle vehicle = new Vehicle(normalizedPlate, vehicleType, hasHcCard, isVIP);

            // Register entry
            String ticketNo = entryService.registerVehicleEntry(vehicle, selectedSpotId);

            if (ticketNo != null) {
                JOptionPane.showMessageDialog(this, "Entry Successful!\nTicket Printed: " + ticketNo);
                plateField.setText("");
                typeCombo.setSelectedIndex(0);
                hcCheckBox.setSelected(false);
                vipCheckBox.setSelected(false);
                selectedSpotId = null;
                refreshSpotGrid();
            } else {
                JOptionPane.showMessageDialog(this, 
                    "Error: This spot is not suitable for a " + vehicleType + "!");
            }
        });

        // --- RIGHT: Visual Spot Grid ---
        gridPanel = new JPanel(new GridLayout(0, 5, 10, 10));
        gridPanel.setBorder(BorderFactory.createTitledBorder("Available Parking Spots"));
        refreshSpotGrid();

        add(formPanel, BorderLayout.WEST);
        add(new JScrollPane(gridPanel), BorderLayout.CENTER);
    }

    // --- Refresh the parking spot grid ---
    private void refreshSpotGrid() {
        gridPanel.removeAll();
        List<ParkingSpot> allSpots = store.getAllSpots();
        System.out.println("EntryPanel: Loaded " + allSpots.size() + " spots from DB");

        int occupiedCount = 0;
        for (ParkingSpot spot : allSpots) {
            if (!spot.isAvailable()) {
                occupiedCount++;
                System.out.println("  OCCUPIED → " + spot.getSpotId() + " | Plate: " + 
                                spot.getCurrentVehiclePlate() + " | Type: " + spot.getType());
            }
        }
        System.out.println("Total occupied spots detected: " + occupiedCount);
        String selectedType = typeCombo.getSelectedItem().toString().toUpperCase();
        boolean isVIP = vipCheckBox.isSelected(); // VIP status

        for (ParkingSpot spot : store.getAllSpots()) {
            SpotType spotType = spot.getType(); // enum type
            JButton spotBtn = new JButton(spot.getSpotId() + " (" + spotType + ")");
            spotBtn.setPreferredSize(new Dimension(80, 50));

            if (!spot.isAvailable()) {
                spotBtn.setBackground(new Color(220, 53, 69)); // Red = Occupied
                spotBtn.setForeground(Color.WHITE);
                spotBtn.setText(spot.getSpotId() + "\nOCCUPIED");
                spotBtn.setEnabled(false);
            } else {
                boolean suitable;
                if (isVIP) {
                    // VIP: only RESERVED spots
                    suitable = spotType == SpotType.RESERVED;
                } else {
                    // Normal users: check suitability based on selected vehicle type
                    suitable = checkSuitability(selectedType, spotType);
                }

                if (suitable) {
                    spotBtn.setBackground(Color.GREEN);
                    spotBtn.addActionListener(e -> {
                        selectedSpotId = spot.getSpotId();
                        JOptionPane.showMessageDialog(this, "Selected Spot: " + selectedSpotId);
                    });
                } else {
                    spotBtn.setBackground(Color.GRAY); // Unsuitable
                    spotBtn.setEnabled(false);
                }
            }

            gridPanel.add(spotBtn);
        }

        gridPanel.revalidate();
        gridPanel.repaint();
    }



    // --- Check suitability based on vehicle type and spot type ---
    private boolean checkSuitability(String vType, SpotType spotType) {
        switch (vType) {
            case "MOTORCYCLE":
                return spotType == SpotType.COMPACT;
            case "CAR":
                return spotType == SpotType.COMPACT || spotType == SpotType.REGULAR;
            case "SUV/TRUCK":
                return spotType == SpotType.REGULAR;
            case "HANDICAPPED":
                return true; // HC can park anywhere
            default:
                return false;
        }
    }

}
