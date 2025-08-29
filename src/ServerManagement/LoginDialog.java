package ServerManagement;

import javax.swing.*;
import java.awt.*;

public class LoginDialog extends JDialog {
    private static final long serialVersionUID = 1L;
    private JTextField tfUsername;
    private JPasswordField pfPassword;
    private JButton btnLogin;
    private JButton btnCancel;
    private boolean succeeded;

    public LoginDialog(Frame parent) {
        super(parent, "Admin Login", true); // 'true' makes it a modal dialog

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints cs = new GridBagConstraints();
        cs.fill = GridBagConstraints.HORIZONTAL;
        cs.insets = new Insets(5, 5, 5, 5);

        cs.gridx = 0;
        cs.gridy = 0;
        cs.gridwidth = 1;
        panel.add(new JLabel("Username:"), cs);

        cs.gridx = 1;
        cs.gridy = 0;
        cs.gridwidth = 2;
        tfUsername = new JTextField(20);
        panel.add(tfUsername, cs);

        cs.gridx = 0;
        cs.gridy = 1;
        cs.gridwidth = 1;
        panel.add(new JLabel("Password:"), cs);

        cs.gridx = 1;
        cs.gridy = 1;
        cs.gridwidth = 2;
        pfPassword = new JPasswordField(20);
        panel.add(pfPassword, cs);

        btnLogin = new JButton("Login");
        btnLogin.addActionListener(e -> onLogin());

        btnCancel = new JButton("Cancel");
        btnCancel.addActionListener(e -> dispose());
        
        getRootPane().setDefaultButton(btnLogin);

        JPanel bp = new JPanel();
        bp.add(btnLogin);
        bp.add(btnCancel);

        getContentPane().add(panel, BorderLayout.CENTER);
        getContentPane().add(bp, BorderLayout.PAGE_END);

        pack();
        setResizable(false);
        setLocationRelativeTo(parent);
    }

    private void onLogin() {
        // IMPORTANT: For a real application, use a secure way to store and check credentials.
        // This is just a simple hardcoded example.
        if (getUsername().equals("admin") && getPassword().equals("dlwltm")) {
            JOptionPane.showMessageDialog(this, "Login successful!", "Login", JOptionPane.INFORMATION_MESSAGE);
            succeeded = true;
            dispose(); // Close the dialog
        } else {
            JOptionPane.showMessageDialog(this, "Invalid username or password", "Login Failed", JOptionPane.ERROR_MESSAGE);
            // reset password field and stay on the dialog
            pfPassword.setText("");
            succeeded = false;
        }
    }

    public String getUsername() {
        return tfUsername.getText().trim();
    }

    public String getPassword() {
        return new String(pfPassword.getPassword());
    }

    public boolean isSucceeded() {
        return succeeded;
    }
}