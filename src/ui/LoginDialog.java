package ui;

import javax.swing.*;
import java.awt.*;
import data.DataStore; // 1. Added import

public class LoginDialog extends JDialog {
    private JTextField userField;
    private JPasswordField passField;
    private String authenticatedRole = null;
    private boolean succeeded = false;

    // 2. Updated constructor to accept DataStore
    public LoginDialog(Frame parent, DataStore store) { 
        super(parent, "Login - University Parking", true);
        setLayout(new GridBagLayout());
        getContentPane().setBackground(new Color(236, 240, 241));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);

        gbc.gridx = 0; gbc.gridy = 0;
        add(new JLabel("Username:"), gbc);
        userField = new JTextField(15);
        gbc.gridx = 1;
        add(userField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        add(new JLabel("Password:"), gbc);
        passField = new JPasswordField(15);
        gbc.gridx = 1;
        add(passField, gbc);

        JButton loginBtn = new JButton("Login");
        loginBtn.setBackground(new Color(41, 128, 185));
        loginBtn.setForeground(Color.WHITE);
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        add(loginBtn, gbc);

        // 3. UPDATED LINE 38: Use the 'store' object passed in
        loginBtn.addActionListener(e -> {
            String role = store.authenticate(userField.getText(), new String(passField.getPassword()));
            if (role != null) {
                authenticatedRole = role;
                succeeded = true;
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, "Invalid Credentials", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        pack();
        setLocationRelativeTo(parent);
    }

    public boolean isSucceeded() { return succeeded; }
    public String getAuthenticatedRole() { return authenticatedRole; }
}