package ServerManagement;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.sql.Connection;
import java.util.Properties;

// ✅ 1. ADDED MISSING IMPORTS
import ServerManagement.dbmanage.DatabaseManager;
import ServerManagement.dbmanage.UserDAO;
import ServerManagement.dto.RegisterRequest;
import ServerManagement.config.ConfigLoader;

public class ServerManagerGUI extends JFrame {
	private static final long serialVersionUID = 1L;
	
    // GUI Components for Server Control
    private final JButton startButton;
    private final JButton stopButton;
    private final JTextField portField;
    private final JLabel statusLabel;
    private final JTextArea logArea;

    // ✅ 2. ADDED MISSING FIELD DECLARATIONS FOR CREATE USER TAB
    private JTextField idField, ipField, portField_user, databaseField; // Renamed to avoid conflict
    private JPasswordField passwordField;
    private JButton createUserButton;

    // Process Management
    private Process serverProcess;
    private final String jarPath = "C:\\Users\\wdl\\localdevelob\\ServerManagement\\lib\\security.jar";
    private final String configPath = "C:\\Users\\wdl\\localdevelob\\ServerManagement\\lib\\config.properties";

    public ServerManagerGUI() {
        super("External Server Manager");

        // ✅ 3. REORGANIZED CONSTRUCTOR
        // Step A: Initialize all individual components first.
        
        // -- Components for Server Control Tab --
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        portField = new JTextField(loadPortFromConfig(), 5);
        startButton = new JButton("Start Server");
        stopButton = new JButton("Stop Server");
        statusLabel = new JLabel("Status: Stopped");
        statusLabel.setForeground(Color.RED);
        controlPanel.add(new JLabel("Port:"));
        controlPanel.add(portField);
        controlPanel.add(startButton);
        controlPanel.add(stopButton);
        controlPanel.add(statusLabel);

        logArea = new JTextArea();
        logArea.setEditable(false);
        JScrollPane logScrollPane = new JScrollPane(logArea);
        
        // Step B: Build the panels for each tab.

        // -- Panel for Server Control Tab --
        JPanel serverControlPanel = new JPanel(new BorderLayout(10, 10));
        serverControlPanel.add(controlPanel, BorderLayout.NORTH);
        serverControlPanel.add(logScrollPane, BorderLayout.CENTER);
        
        // -- Panel for Create User Tab --
        JPanel createUserPanel = createCreateUserPanel();
        

        JPanel UserManagement = createCreateUserPanel();

        // Step C: Create the tabbed pane and add the panels to it.
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Server Control", serverControlPanel);
        tabbedPane.addTab("Create User", createUserPanel);
        tabbedPane.addTab("User Management", UserManagement);

        // Step D: Set the main layout and add the tabbed pane.
        setLayout(new BorderLayout());
        add(tabbedPane, BorderLayout.CENTER);

        // Step E: Set up button actions and finalize the window.
        startButton.addActionListener(e -> startServer());
        stopButton.addActionListener(e -> stopServer());
        createUserButton.addActionListener(e -> saveNewUser());

        updateUIState();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);
    }

    // This method to create the user panel now correctly references the declared fields
    private JPanel createCreateUserPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        idField = new JTextField(20);
        passwordField = new JPasswordField(20);
        ipField = new JTextField(20);
        portField_user = new JTextField(20); // Using the renamed variable
        databaseField = new JTextField(20);
        createUserButton = new JButton("Create User");

        gbc.gridx = 0; gbc.gridy = 0; panel.add(new JLabel("ID:"), gbc);
        gbc.gridx = 1; gbc.gridy = 0; panel.add(idField, gbc);
        gbc.gridx = 0; gbc.gridy = 1; panel.add(new JLabel("Password:"), gbc);
        gbc.gridx = 1; gbc.gridy = 1; panel.add(passwordField, gbc);
        gbc.gridx = 0; gbc.gridy = 2; panel.add(new JLabel("IP:"), gbc);
        gbc.gridx = 1; gbc.gridy = 2; panel.add(ipField, gbc);
        gbc.gridx = 0; gbc.gridy = 3; panel.add(new JLabel("Port:"), gbc);
        gbc.gridx = 1; gbc.gridy = 3; panel.add(portField_user, gbc); // Using the renamed variable
        gbc.gridx = 0; gbc.gridy = 4; panel.add(new JLabel("Database:"), gbc);
        gbc.gridx = 1; gbc.gridy = 4; panel.add(databaseField, gbc);
        
        gbc.gridx = 1; gbc.gridy = 5; gbc.anchor = GridBagConstraints.EAST;
        panel.add(createUserButton, gbc);

        return panel;
    }
    
    // Logic to save the user to the database
    private void saveNewUser() {
        // This part is fine, it just gets the data from the form
        RegisterRequest newUser = new RegisterRequest();
        newUser.setId(idField.getText());
        newUser.setPassword(new String(passwordField.getPassword()));
        newUser.setIp(ipField.getText());
        newUser.setPort(portField_user.getText());
        newUser.setDatabase(databaseField.getText());

        if (newUser.getId().isEmpty() || newUser.getPassword().isEmpty()) {
            JOptionPane.showMessageDialog(this, "ID and Password are required.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Disable the button to prevent multiple clicks
        createUserButton.setEnabled(false);

        // ✅ Create a new SwingWorker to perform the database operation in the background
        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {

            @Override
            protected Boolean doInBackground() throws Exception {
                // This code runs on a BACKGROUND THREAD.
                // It is safe to perform slow database operations here.
                DatabaseManager dbManager = new DatabaseManager(
                    ConfigLoader.getProperty("db.url"),
                    ConfigLoader.getProperty("db.user"),
                    ConfigLoader.getProperty("db.pass")
                );
                UserDAO userDAO = new UserDAO();

                try (Connection conn = dbManager.getConnection()) {
                    return userDAO.createUser(conn, newUser);
                }
            }

            @Override
            protected void done() {
                // This code runs on the UI THREAD (EDT) after doInBackground is finished.
                // It is safe to update the GUI here.
                try {
                    boolean success = get(); // Get the result from doInBackground()
                    if (success) {
                        JOptionPane.showMessageDialog(ServerManagerGUI.this, "User created successfully!");
                        // Clear the fields
                        idField.setText("");
                        passwordField.setText("");
                        ipField.setText("");
                        portField_user.setText("");
                        databaseField.setText("");
                    } else {
                        // This else block might be triggered if the user ID already exists
                        JOptionPane.showMessageDialog(ServerManagerGUI.this, "Failed to create user. The user might already exist.", "Creation Failed", JOptionPane.WARNING_MESSAGE);
                    }
                } catch (Exception ex) {
                    // This catches errors from the background thread (e.g., DB connection failed)
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(ServerManagerGUI.this, "Error creating user: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
                } finally {
                    // Re-enable the button, whether it succeeded or failed
                    createUserButton.setEnabled(true);
                }
            }
        };

        worker.execute(); // This starts the background task
    }

    // --- All other methods (startServer, stopServer, etc.) remain the same ---
    private void startServer() {
        savePortToConfig(portField.getText());
        ProcessBuilder pb = new ProcessBuilder("java", "-jar", jarPath);
        pb.redirectErrorStream(true);
        try {
            logArea.setText("");
            serverProcess = pb.start();
            log("Server process starting...");
            new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(serverProcess.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        log(line);
                    }
                } catch (IOException ioException) { /* Expected */ }
                SwingUtilities.invokeLater(this::updateUIState);
                log("Server process terminated.");
            }).start();
        } catch (IOException ex) {
            log("Error starting server: " + ex.getMessage());
        }
        updateUIState();
    }

    private void stopServer() {
        if (serverProcess != null && serverProcess.isAlive()) {
            serverProcess.destroy();
            log("Stop signal sent to server...");
        }
        updateUIState();
    }

    private void updateUIState() {
        boolean isRunning = (serverProcess != null && serverProcess.isAlive());
        startButton.setEnabled(!isRunning);
        portField.setEnabled(!isRunning);
        stopButton.setEnabled(isRunning);
        if (isRunning) {
            statusLabel.setText("Status: Running");
            statusLabel.setForeground(new Color(0, 153, 0));
        } else {
            statusLabel.setText("Status: Stopped");
            statusLabel.setForeground(Color.RED);
        }
    }
    
    private void log(String message) {
        SwingUtilities.invokeLater(() -> logArea.append(message + "\n"));
    }

    private void savePortToConfig(String port) {
        Properties prop = new Properties();
        File configFile = new File(configPath);
        try {
            if (configFile.exists()) {
                try (InputStream input = new FileInputStream(configFile)) {
                    prop.load(input);
                }
            }
            prop.setProperty("server.port", port);
            try (OutputStream output = new FileOutputStream(configFile)) {
                prop.store(output, "Server Configuration updated by Manager GUI");
                log("Configuration saved. Port set to " + port);
            }
        } catch (IOException io) {
            log("Error saving config: " + io.getMessage());
        }
    }

    private String loadPortFromConfig() {
        Properties prop = new Properties();
        try (InputStream input = new FileInputStream(configPath)) {
            prop.load(input);
            return prop.getProperty("server.port", "8120");
        } catch (IOException ex) {
            return "8120";
        }
    }

    public static void main(String[] args) {
    	SwingUtilities.invokeLater(() -> {
            // 1. Create and show the login dialog first.
            LoginDialog loginDialog = new LoginDialog(null);
            loginDialog.setVisible(true);

            // The code will pause here until the login dialog is closed.

            // 2. Check if the login was successful.
            if (loginDialog.isSucceeded()) {
                // If successful, create and show the main application window.
                ServerManagerGUI mainFrame = new ServerManagerGUI();
                mainFrame.setVisible(true);
            } else {
                // If not successful (e.g., user clicked cancel), the program exits.
                System.out.println("Login canceled or failed. Exiting application.");
            }
        });
    }
}