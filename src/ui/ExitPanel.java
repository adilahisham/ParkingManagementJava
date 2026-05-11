package ui;

import data.DataStore;
import enums.FineReason;
import enums.PaymentMethod;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import model.FineRecord;
import model.ParkingSession;
import model.PaymentRecord;
import service.ExitService;
import service.PaymentProcessor;

public class ExitPanel extends JPanel {

    private static final DateTimeFormatter DISPLAY_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    private final DataStore store;
    private final ExitService exitService;
    private final PaymentProcessor paymentProcessor;
    private final AdminPanel adminPanel;
    private final ReportingPanel reportingPanel;

    private JTextField plateField;
    private JTextField exitTimeField;
    private JTextArea receiptArea;
    private DefaultListModel<String> listModel;
    private JButton processBtn;
    private JCheckBox hcCheckBox;
    private JLabel revenueLabel;

    private ParkingSession currentSession;
    private PaymentRecord previewRecord;

    public ExitPanel(DataStore store, ExitService exitService,
                     PaymentProcessor paymentProcessor,
                     AdminPanel adminPanel, ReportingPanel reportingPanel) {
        this.store = store;
        this.exitService = exitService;
        this.paymentProcessor = paymentProcessor;
        this.adminPanel = adminPanel;
        this.reportingPanel = reportingPanel;

        setLayout(new BorderLayout(15, 15));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        initTopPanel();
        initCenterPanel();
        initBottomPanel();
        refreshVehiclesInside();
    }

    private void initTopPanel() {
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        topPanel.add(new JLabel("License Plate:"));
        plateField = new JTextField(12);
        topPanel.add(plateField);


        topPanel.add(new JLabel("Exit Time (yyyy-MM-ddTHH:mm):"));
        exitTimeField = new JTextField(16);
        exitTimeField.setText(LocalDateTime.now().format(DISPLAY_FORMAT));
        topPanel.add(exitTimeField);

        JButton searchBtn = new JButton("Preview Exit");
        topPanel.add(searchBtn);
        add(topPanel, BorderLayout.NORTH);

        searchBtn.addActionListener(e -> previewVehicleExit());
    }

    private void previewVehicleExit() {
        String plate = plateField.getText().trim().toUpperCase().replace("O", "0");
        String exitText = exitTimeField.getText().trim();

        if (plate.isEmpty() || exitText.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter plate and exit time.");
            return;
        }

        try {
            LocalDateTime exitTime = LocalDateTime.parse(exitText);
            
            System.out.println("[DEBUG EXIT] Looking for plate: " + plate);
            currentSession = store.getOpenSessionByPlate(plate);

            if (currentSession == null) {
                System.out.println("[DEBUG EXIT] Session NOT found for plate: " + plate);
                // Add more info if possible
                System.out.println("[DEBUG EXIT] All active sessions: " + store.getAllActiveSessions().size());
                store.getAllActiveSessions().forEach(s -> 
                    System.out.println("  - Plate: " + s.getPlate() + ", Spot: " + s.getSpotId() + ", VIP: " + s.getVehicle().isVIP()));
                
                JOptionPane.showMessageDialog(this, "Vehicle not found or already exited!");
                receiptArea.setText("");
                processBtn.setEnabled(false);
                return;
            }

            System.out.println("[DEBUG EXIT] Session found! Spot: " + currentSession.getSpotId() + 
                            ", VIP: " + currentSession.getVehicle().isVIP() + 
                            ", Entry: " + currentSession.getEntryTime());

            previewRecord = exitService.previewExit(currentSession, exitTime);
            if (previewRecord != null) {
                displayReceipt(previewRecord);
                processBtn.setEnabled(true);
            }

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Invalid time format! Use yyyy-MM-ddTHH:mm");
        }
    }
    
    private void initCenterPanel() {
        JPanel centerPanel = new JPanel(new BorderLayout(15, 0));
        listModel = new DefaultListModel<>();
        JList<String> vehiclesList = new JList<>(listModel);

        vehiclesList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && vehiclesList.getSelectedValue() != null) {
                String selected = vehiclesList.getSelectedValue();
                plateField.setText(selected.split(" ")[0]);
            }
        });

        JScrollPane listScroll = new JScrollPane(vehiclesList);
        listScroll.setBorder(BorderFactory.createTitledBorder("Vehicles Inside"));
        listScroll.setPreferredSize(new Dimension(200, 0));
        centerPanel.add(listScroll, BorderLayout.WEST);

        receiptArea = new JTextArea();
        receiptArea.setFont(new Font("Monospaced", Font.PLAIN, 13));
        receiptArea.setEditable(false);
        JScrollPane receiptScroll = new JScrollPane(receiptArea);
        receiptScroll.setBorder(BorderFactory.createTitledBorder("Exit Receipt"));
        centerPanel.add(receiptScroll, BorderLayout.CENTER);

        add(centerPanel, BorderLayout.CENTER);
    }

    private void initBottomPanel() {
        JPanel bottomPanel = new JPanel(new BorderLayout());
        revenueLabel = new JLabel("Total Revenue: RM 0.00");
        revenueLabel.setFont(new Font("Arial", Font.BOLD, 14));
        bottomPanel.add(revenueLabel, BorderLayout.WEST);

        processBtn = new JButton("Confirm Payment & Exit");
        processBtn.setEnabled(false);
        processBtn.setBackground(new Color(52, 152, 219));
        processBtn.setForeground(Color.WHITE);
        processBtn.setPreferredSize(new Dimension(220, 40));
        bottomPanel.add(processBtn, BorderLayout.EAST);
        add(bottomPanel, BorderLayout.SOUTH);

        processBtn.addActionListener(e -> openPaymentDialog());
    }

    private void openPaymentDialog() {
        if (currentSession == null || previewRecord == null) return;

        LocalDateTime exitTime = previewRecord.getPaidTime();
        double parkingFee = previewRecord.getParkingFee();

        List<FineRecord> pastFines = store.getUnpaidFinesByPlate(currentSession.getVehicle().getPlate());

        long hours = previewRecord.getDurationHours();
        List<FineRecord> newFines = new ArrayList<>();
        if (hours > 24) {
            newFines.add(new FineRecord(currentSession.getVehicle().getPlate(),
                    FineReason.OVERSTAY_24H,
                    exitService.getActiveFineScheme().calculateFine(hours - 24),
                    exitTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                    false));
        }
        if (currentSession.getSpotId().contains("RES") && !currentSession.getVehicle().isVIP()) {
            newFines.add(new FineRecord(currentSession.getVehicle().getPlate(),
                    FineReason.RESERVED_VIOLATION,
                    100.0,
                    exitTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                    false));
        }

        List<FineRecord> allFines = new ArrayList<>();
        allFines.addAll(pastFines);
        allFines.addAll(newFines);

        double totalFines = allFines.stream().mapToDouble(FineRecord::getAmount).sum();

        // --- Payment Dialog ---
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Payment", true);
        dialog.setLayout(new GridLayout(5, 2, 10, 10));
        dialog.setSize(400, 230);
        dialog.setLocationRelativeTo(this);

        dialog.add(new JLabel("Payment Method:"));
        JComboBox<PaymentMethod> methodBox = new JComboBox<>(PaymentMethod.values());
        dialog.add(methodBox);

        dialog.add(new JLabel("Parking Fee (RM):"));
        dialog.add(new JTextField(String.format("%.2f", parkingFee)) {{ setEditable(false); }});

        dialog.add(new JLabel("Total Fines Due (RM):"));
        dialog.add(new JTextField(String.format("%.2f", totalFines)) {{ setEditable(false); }});

        dialog.add(new JLabel("Amount to Pay (RM):"));
        JTextField paidField = new JTextField(String.format("%.2f", parkingFee + totalFines));
        dialog.add(paidField);

        JButton confirmBtn = new JButton("Confirm Payment");
        dialog.add(new JLabel());
        dialog.add(confirmBtn);

        confirmBtn.addActionListener(ev -> {
            try {
                double typedAmount = Double.parseDouble(paidField.getText().trim());
                PaymentMethod method = (PaymentMethod) methodBox.getSelectedItem();

                if (typedAmount < parkingFee) {
                    JOptionPane.showMessageDialog(dialog,
                            "You must pay at least the full parking fee to exit!");
                    return;
                }

                double finePaid = 0.0;
                double remainingAmount = typedAmount - parkingFee;

                List<FineRecord> unpaidFines = store.getUnpaidFinesByPlate(currentSession.getVehicle().getPlate());
                for (FineRecord f : unpaidFines) {
                    if (remainingAmount <= 0) break;
                    double toPay = Math.min(f.getAmount(), remainingAmount);
                    store.reduceFineAmount(f, toPay);
                    finePaid += toPay;
                    remainingAmount -= toPay;
                }

                double totalPaid = parkingFee + finePaid;

                PaymentRecord payment = new PaymentRecord(
                        previewRecord.getTicketNo(),
                        previewRecord.getPlate(),
                        method,
                        exitTime,
                        previewRecord.getDurationHours(),
                        parkingFee,
                        finePaid,
                        totalPaid
                );

                // Confirm the exit and get the finalized payment record
                PaymentRecord finalized = exitService.confirmExit(currentSession, exitTime, payment, false);

                // Show the FINAL receipt immediately
                displayReceipt(finalized);

                // Nice message to the user
                JOptionPane.showMessageDialog(dialog, 
                    "Payment & Exit Successful!\nFinal receipt is now displayed in the main area.");

                dialog.dispose();

                refreshVehiclesInside();
                if (adminPanel != null) adminPanel.refreshStats();
                if (reportingPanel != null) reportingPanel.refreshStats();

                // Reset ONLY input fields — KEEP the receipt visible
                plateField.setText("");
                exitTimeField.setText(LocalDateTime.now().format(DISPLAY_FORMAT));
                processBtn.setEnabled(false);

                currentSession = null;
                previewRecord = null;

                // DO NOT call resetPanel() here — it would clear the receipt

            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "Invalid amount entered!");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Error processing payment: " + ex.getMessage());
            }
        });

        dialog.setVisible(true);
    }

    private void displayReceipt(PaymentRecord record) {
        boolean isPreview = record.getMethod() == null || record.getAmountPaid() == 0;

        String plate = record.getPlate();

        // Current unpaid fines from database (most accurate)
        List<FineRecord> unpaidFines = store.getUnpaidFinesByPlate(plate);
        double pastUnpaidTotal = unpaidFines.stream().mapToDouble(FineRecord::getAmount).sum();

        // For preview: calculate only NEW fines for this session
        double estimatedNewFines = 0.0;
        if (isPreview && record.getFinePaid() > 0) {
            estimatedNewFines = record.getFinePaid() - pastUnpaidTotal;
            // Prevent negative due to timing/rounding issues
            if (estimatedNewFines < 0) estimatedNewFines = 0.0;
        }

        // Entry time
        String entryTimeStr = currentSession != null ? currentSession.getEntryTime() : "N/A";

        StringBuilder sb = new StringBuilder();

        // Header
        if (isPreview) {
            sb.append("========= PARKING RECEIPT (PREVIEW) =========\n");
            sb.append("     Please review before payment      \n");
        } else {
            sb.append("===== FINAL PAYMENT RECEIPT =====\n");
            sb.append("         EXIT COMPLETED         \n");
        }

        sb.append("Ticket No     : ").append(record.getTicketNo()).append("\n");
        sb.append("Plate         : ").append(plate).append("\n");
        sb.append("Entry Time    : ").append(entryTimeStr).append("\n");
        sb.append("Exit Time     : ").append(record.getPaidTime().format(DISPLAY_FORMAT)).append("\n");
        sb.append("Duration      : ").append(record.getDurationHours()).append(" hours\n");
        sb.append("--------------------------------------------\n");

        // Parking fee
        sb.append("Parking Fee           RM ").append(String.format("%8.2f", record.getParkingFee())).append("\n");

        // Fines section
        if (isPreview) {
            sb.append("Estimated New Fines   RM ").append(String.format("%8.2f", estimatedNewFines)).append("\n");
            sb.append("  └─ Overstay / Reserved violation this stay\n");

            if (pastUnpaidTotal > 0) {
                sb.append("Past Unpaid Fines     RM ").append(String.format("%8.2f", pastUnpaidTotal)).append("\n");
                sb.append("  └─ From previous visits - must be settled\n");
            }

            double totalDue = record.getParkingFee() + estimatedNewFines + pastUnpaidTotal;
            sb.append("--------------------------------------------\n");
            sb.append("TOTAL TO PAY NOW      RM ").append(String.format("%8.2f", totalDue)).append("\n");
        } else {
            // Final receipt
            sb.append("Fines Paid This Time  RM ").append(String.format("%8.2f", record.getFinePaid())).append("\n");
            sb.append("  └─ Applied to current stay fines\n");

            if (pastUnpaidTotal > 0) {
                sb.append("Still Unpaid Fines    RM ").append(String.format("%8.2f", pastUnpaidTotal)).append("\n");
                sb.append("  Breakdown of remaining:\n");
                for (FineRecord f : unpaidFines) {
                    sb.append("     • ").append(String.format("%-20s", f.getReason().name()))
                    .append(" RM ").append(String.format("%6.2f", f.getAmount()))
                    .append("  (").append(f.getPaidAt() != null ? f.getPaidAt() : "—").append(")\n");
                }
            } else {
                sb.append("All fines cleared     RM 0.00\n");
            }

            sb.append("--------------------------------------------\n");
            sb.append("Total Amount Paid     RM ").append(String.format("%8.2f", record.getAmountPaid())).append("\n");

            double change = record.getAmountPaid() - (record.getParkingFee() + record.getFinePaid());
            if (change > 0) {
                sb.append("Change Returned       RM ").append(String.format("%8.2f", change)).append("\n");
            } else if (change < 0) {
                sb.append("Still Owing           RM ").append(String.format("%8.2f", -change)).append("\n");
            } else {
                sb.append("Exact payment - no change\n");
            }
        }

        sb.append("--------------------------------------------\n");
        sb.append("Payment Method: ").append(record.getMethod() != null ? record.getMethod() : "Not yet paid").append("\n");
        sb.append("============================================\n");

        if (!isPreview) {
            sb.append("       Thank you - Drive Safely!       \n");
        }

        receiptArea.setText(sb.toString());
    }

    public void refreshVehiclesInside() {
        listModel.clear();
        
        List<ParkingSession> active = store.getAllActiveSessions();
        System.out.println("[EXIT LIST DEBUG] Number of active sessions returned: " + active.size());
        
        active.forEach(session -> {
            String line = session.getPlate() + " (" + session.getSpotId() + ")";
            listModel.addElement(line);
            System.out.println("[EXIT LIST DEBUG] Added: " + line + " | VIP: " + session.getVehicle().isVIP());
        });

        double totalRev = store.getTotalRevenue();
        revenueLabel.setText(String.format("Total Revenue: RM %.2f", totalRev));
    }

    private void resetPanel() {
        //receiptArea.setText("");
        plateField.setText("");
        exitTimeField.setText(LocalDateTime.now().format(DISPLAY_FORMAT));
        processBtn.setEnabled(false);
        currentSession = null;
        previewRecord = null;
    }
}
