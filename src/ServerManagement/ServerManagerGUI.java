package ServerManagement;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;

// Imports for the "Create User" and "User Management" features
import ServerManagement.config.ConfigLoader;
import ServerManagement.dbmanage.DatabaseManager;
import ServerManagement.dbmanage.UserDAO;
import ServerManagement.dto.RegisterRequest;
import ServerManagement.dto.User;

public class ServerManagerGUI extends JFrame {
	private static final long serialVersionUID = 1L;
	
    // GUI Components for Server Control
    private final JButton startButton;
    private final JButton stopButton;
    private final JTextField portField;
    private final JLabel statusLabel;
    private final JTextArea logArea;

    // GUI Components for Create User
    private JTextField idField;
    private JPasswordField passwordField;
    private JTextField ipField;
    private JTextField portField_user;
    private JTextField databaseField;
    private JButton createUserButton;
    
    // ✅ NEW: GUI Components for User Management
    private JTable userTable;
    private DefaultTableModel tableModel;
    private JButton refreshUsersButton;
    private JButton updateUserButton;
    private JButton deleteUserButton;
    
    // ✅ NEW: GUI Components for Log Management
    private JList<File> logFileList;
    private DefaultListModel<File> logListModel;
    private JTextArea logContentArea;
    private JButton refreshLogListButton;

    // Process Management
    private Process serverProcess;
    private final String jarPath    = "C:\\Users\\wdl\\localdevelob\\ServerManagement\\lib\\security.jar";
    private final String configPath = "C:\\Users\\wdl\\localdevelob\\ServerManagement\\lib\\config.properties";

    public ServerManagerGUI() {
        super("Server & User Manager");

        // --- Step 1: Initialize components ---
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        portField           = new JTextField(loadPortFromConfig(), 5);
        startButton         = new JButton("Start Server");
        stopButton          = new JButton("Stop Server");
        statusLabel         = new JLabel("Status: Stopped");
        statusLabel.setForeground(Color.RED);
        controlPanel.add(new JLabel("Port:"));
        controlPanel.add(portField);
        controlPanel.add(startButton);
        controlPanel.add(stopButton);
        controlPanel.add(statusLabel);
        logArea = new JTextArea();
        logArea.setEditable(false);
        JScrollPane logScrollPane = new JScrollPane(logArea);

        // --- Step 2: Build panels for each tab ---
        JPanel serverControlPanel = new JPanel(new BorderLayout(10, 10));
        serverControlPanel.add(controlPanel, BorderLayout.NORTH);
        serverControlPanel.add(logScrollPane, BorderLayout.CENTER);
        
        JPanel createUserPanel = createCreateUserPanel();
        JPanel userManagementPanel = createServerManagementPanel();
        JPanel logManagementPanel = createLogManagementPanel();

        // --- Step 3: Create and add tabs ---
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Server Control", serverControlPanel);
        tabbedPane.addTab("Create User", createUserPanel);
        tabbedPane.addTab("User Management", userManagementPanel);
        tabbedPane.addTab("Log Management", logManagementPanel);

        // --- Step 4: Finalize layout and actions ---
        setLayout(new BorderLayout());
        add(tabbedPane, BorderLayout.CENTER);

        startButton.addActionListener(e -> startServer());
        stopButton.addActionListener(e -> stopServer());
        createUserButton.addActionListener(e -> saveNewUser());
        
        // ✅ NEW: Add ActionListeners for the new buttons
        refreshUsersButton.addActionListener(e -> loadUsers());
        refreshLogListButton.addActionListener(e -> loadLogFiles());
        deleteUserButton.addActionListener(e -> deleteSelectedUser());
        updateUserButton.addActionListener(e -> updateSelectedUser());

        updateUIState();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);
    }
    
    // This method creates the UI for the User Management tab
    private JPanel createServerManagementPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));

        String[] columnNames = {"ID", "IP", "Port", "Database", "Password Hash", "Refresh Token"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            private static final long serialVersionUID = 1L;
            @Override
            public boolean isCellEditable(int row, int column) {
                // Allow editing for IP, Port, and Database columns
                return column == 1 || column == 2 || column == 3;
            }
        };
        userTable = new JTable(tableModel);
        userTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane tableScrollPane = new JScrollPane(userTable);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        refreshUsersButton = new JButton("Refresh User List");
        updateUserButton   = new JButton("Update Selected");
        deleteUserButton   = new JButton("Delete Selected");
        
        updateUserButton.setEnabled(false);
        deleteUserButton.setEnabled(false);
        
        buttonPanel.add(refreshUsersButton);
        buttonPanel.add(updateUserButton);
        buttonPanel.add(deleteUserButton);

        userTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                boolean rowSelected = userTable.getSelectedRow() != -1;
                updateUserButton.setEnabled(rowSelected);
                deleteUserButton.setEnabled(rowSelected);
            }
        });

        panel.add(tableScrollPane, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        return panel;
    }
    
    private JPanel createLogManagementPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        
        // Log file list on the left
        logListModel = new DefaultListModel<>();
        logFileList = new JList<>(logListModel);
        JScrollPane listScrollPane = new JScrollPane(logFileList);
        listScrollPane.setPreferredSize(new Dimension(200, 0));

        // Log content view on the right
        logContentArea = new JTextArea();
        logContentArea.setEditable(false);
        logContentArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane contentScrollPane = new JScrollPane(logContentArea);
        
        // Split pane to make the view resizable
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, listScrollPane, contentScrollPane);
        splitPane.setDividerLocation(200);

        // Buttons panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        refreshLogListButton = new JButton("Refresh Log List");
        buttonPanel.add(refreshLogListButton);

        panel.add(splitPane, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        // Add listener to load file content when a file is selected
        logFileList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                File selectedFile = logFileList.getSelectedValue();
                if (selectedFile != null) {
                    loadLogFileContent(selectedFile);
                }
            }
        });
        
        return panel;
    }

    // ✅ NEW: Logic to find and list log files
    private void loadLogFiles() {
        refreshLogListButton.setEnabled(false);
        new SwingWorker<List<File>, Void>() {
            @Override
            protected List<File> doInBackground() throws Exception {
                File jarDir = new File(jarPath).getParentFile();
                File logsDir = new File(jarDir, "logs");
                if (logsDir.exists() && logsDir.isDirectory()) {
                    File[] files = logsDir.listFiles((dir, name) -> name.endsWith(".log"));
                    if (files != null) {
                        List<File> fileList = new ArrayList<>(Arrays.asList(files));
                        // Sort by most recent first
                        fileList.sort(Comparator.comparing(File::lastModified).reversed());
                        return fileList;
                    }
                }
                return Collections.emptyList();
            }

            @Override
            protected void done() {
                try {
                    List<File> files = get();
                    logListModel.clear();
                    for (File file : files) {
                        logListModel.addElement(file);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    refreshLogListButton.setEnabled(true);
                }
            }
        }.execute();
    }

    // ✅ NEW: Logic to read a selected log file and display its content
    private void loadLogFileContent(File logFile) {
        logContentArea.setText("Loading " + logFile.getName() + "...");
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                return Files.readString(Paths.get(logFile.toURI()));
            }
            @Override
            protected void done() {
                try {
                    logContentArea.setText(get());
                    logContentArea.setCaretPosition(0); // Scroll to top
                } catch (Exception e) {
                    logContentArea.setText("Error reading file: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }.execute();
    }
    
    // ✅ NEW: Logic to load all users into the JTable
    private void loadUsers() {
        refreshUsersButton.setEnabled(false);
        log("Loading users from database...");
        
        new SwingWorker<List<User>, Void>() {
            @Override
            protected List<User> doInBackground() throws Exception {
                DatabaseManager dbManager = new DatabaseManager(ConfigLoader.getProperty("db.url"), ConfigLoader.getProperty("db.user"), ConfigLoader.getProperty("db.pass"));
                try (Connection conn = dbManager.getConnection()) {
                    return new UserDAO().getAllUsers(conn);
                }
            }
            @Override
            protected void done() {
                try {
                    List<User> users = get();
                    tableModel.setRowCount(0); // Clear existing 
                    for (User user : users) {
                        tableModel.addRow(new Object[]{user.getId(), user.getIp(), user.getPort(), user.getDatabase(), user.getPassword(), user.getRefreshToken()});
                    }
                    log("User list loaded successfully (" + users.size() + " users found).");
                } catch (Exception e) {
                    log("Error loading users: " + e.getMessage());
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(ServerManagerGUI.this, "Error loading users: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                } finally {
                    refreshUsersButton.setEnabled(true);
                }
            }
        }.execute();
    }

    // ✅ NEW: Logic to delete the selected user
    private void deleteSelectedUser() {
        int selectedRow = userTable.getSelectedRow();
        if (selectedRow == -1) return;

        String userId = (String) tableModel.getValueAt(selectedRow, 0);
        int choice = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete user '" + userId + "'?", "Confirm Deletion", JOptionPane.YES_NO_OPTION);
        
        if (choice == JOptionPane.YES_OPTION) {
            deleteUserButton.setEnabled(false);
            new SwingWorker<Boolean, Void>() {
                @Override
                protected Boolean doInBackground() throws Exception {
                    DatabaseManager dbManager = new DatabaseManager(ConfigLoader.getProperty("db.url"), ConfigLoader.getProperty("db.user"), ConfigLoader.getProperty("db.pass"));
                    try (Connection conn = dbManager.getConnection()) {
                        return new UserDAO().deleteUser(conn, userId);
                    }
                }
                @Override
                protected void done() {
                    try {
                        if (get()) {
                            log("User '" + userId + "' deleted successfully.");
                            tableModel.removeRow(selectedRow);
                        } else {
                            throw new Exception("DAO returned false (user not found or could not be deleted).");
                        }
                    } catch (Exception e) {
                        log("Error deleting user: " + e.getMessage());
                        JOptionPane.showMessageDialog(ServerManagerGUI.this, "Error deleting user: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    } finally {
                        // The ListSelectionListener will automatically handle re-enabling the button
                    }
                }
            }.execute();
        }
    }

    // ✅ NEW: Logic to update the selected user with the values edited in the table
    private void updateSelectedUser() {
        int selectedRow = userTable.getSelectedRow();
        if (selectedRow == -1) return;

        if (userTable.isEditing()) {
            userTable.getCellEditor().stopCellEditing();
        }

        User user = new User();
        user.setId((String) tableModel.getValueAt(selectedRow, 0));
        user.setIp((String) tableModel.getValueAt(selectedRow, 1));
        user.setPort((String) tableModel.getValueAt(selectedRow, 2));
        user.setDatabase((String) tableModel.getValueAt(selectedRow, 3));
        
        updateUserButton.setEnabled(false);
        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                DatabaseManager dbManager = new DatabaseManager(ConfigLoader.getProperty("db.url"), ConfigLoader.getProperty("db.user"), ConfigLoader.getProperty("db.pass"));
                try (Connection conn = dbManager.getConnection()) {
                    return new UserDAO().updateUser(conn, user);
                }
            }
            @Override
            protected void done() {
                try {
                    if (get()) {
                        log("User '" + user.getId() + "' updated successfully.");
                        JOptionPane.showMessageDialog(ServerManagerGUI.this, "User updated successfully!");
                    } else {
                        throw new Exception("DAO returned false (user not found or could not be updated).");
                    }
                } catch (Exception e) {
                    log("Error updating user: " + e.getMessage());
                    JOptionPane.showMessageDialog(ServerManagerGUI.this, "Error updating user: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                } finally {
                    updateUserButton.setEnabled(true);
                }
            }
        }.execute();
    }
    
    private JPanel createCreateUserPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets       = new Insets(5, 5, 5, 5);
        gbc.fill         = GridBagConstraints.HORIZONTAL;
        idField          = new JTextField(20);
        passwordField    = new JPasswordField(20);
        ipField          = new JTextField(20);
        portField_user   = new JTextField(20);
        databaseField    = new JTextField(20);
        createUserButton = new JButton("Create User");
        gbc.gridx = 0; gbc.gridy = 0; panel.add(new JLabel("ID:"), gbc);
        gbc.gridx = 1; gbc.gridy = 0; panel.add(idField, gbc);
        gbc.gridx = 0; gbc.gridy = 1; panel.add(new JLabel("Password:"), gbc);
        gbc.gridx = 1; gbc.gridy = 1; panel.add(passwordField, gbc);
        gbc.gridx = 0; gbc.gridy = 2; panel.add(new JLabel("IP:"), gbc);
        gbc.gridx = 1; gbc.gridy = 2; panel.add(ipField, gbc);
        gbc.gridx = 0; gbc.gridy = 3; panel.add(new JLabel("Port:"), gbc);
        gbc.gridx = 1; gbc.gridy = 3; panel.add(portField_user, gbc);
        gbc.gridx = 0; gbc.gridy = 4; panel.add(new JLabel("Database:"), gbc);
        gbc.gridx = 1; gbc.gridy = 4; panel.add(databaseField, gbc);
        gbc.gridx = 1; gbc.gridy = 5; gbc.anchor = GridBagConstraints.EAST;
        panel.add(createUserButton, gbc);
        return panel;
    }
    private void saveNewUser() {
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
        createUserButton.setEnabled(false);
        log("Creating new user...");
        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                DatabaseManager dbManager = new DatabaseManager(ConfigLoader.getProperty("db.url"), ConfigLoader.getProperty("db.user"), ConfigLoader.getProperty("db.pass"));
                UserDAO userDAO = new UserDAO();
                try (Connection conn = dbManager.getConnection()) {
                    return userDAO.createUser(conn, newUser);
                }
            }
            @Override
            protected void done() {
                try {
                    boolean success = get();
                    if (success) {
                        JOptionPane.showMessageDialog(ServerManagerGUI.this, "User created successfully!");
                        log("User '" + newUser.getId() + "' created successfully.");
                        idField.setText("");
                        passwordField.setText("");
                        ipField.setText("");
                        portField_user.setText("");
                        databaseField.setText("");
                    } else {
                        throw new Exception("DAO returned false. User might already exist.");
                    }
                } catch (Exception ex) {
                    log("Error creating user: " + ex.getMessage());
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(ServerManagerGUI.this, "Error creating user: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
                } finally {
                    createUserButton.setEnabled(true);
                }
            }
        };
        worker.execute();
    }
    private void startServer() {
        savePortToConfig(portField.getText());
        ProcessBuilder pb = new ProcessBuilder("java", "-jar", jarPath);
        pb.redirectErrorStream(true);
        File workingDirectory = new File(jarPath).getParentFile();
        pb.directory(workingDirectory);
        try {
            logArea.setText("");
            serverProcess = pb.start();
            log("Server process starting in: " + workingDirectory.getAbsolutePath());
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
        SwingUtilities.invokeLater(() -> {
            logArea.append(message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
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
        File configFile = new File(configPath);
        try (InputStream input = new FileInputStream(configFile)) {
            prop.load(input);
            return prop.getProperty("server.port", "8120");
        } catch (IOException ex) {
            return "8120";
        }
    }
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            LoginDialog loginDialog = new LoginDialog(null);
            loginDialog.setVisible(true);
            if (loginDialog.isSucceeded()) {
                new ServerManagerGUI().setVisible(true);
            } else {
                System.exit(0);
            }
        });
    }
}