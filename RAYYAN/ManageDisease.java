import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Base64;

// UC 06: Manage Disease Information (Admin)
public class ManageDisease extends JFrame {

    private static final String FILE_NAME = "DiseaseInformation.txt";

    // Each disease is stored as: {name, symptoms, causes, treatment}
    private final List<String[]> diseases = new ArrayList<>();

    private JTextField nameField;
    private JTextArea symptomsArea, causesArea, treatmentArea;
    private JLabel messageLabel;

    private final DefaultTableModel tableModel = new DefaultTableModel(
            new String[]{"Disease Name", "Symptoms", "Causes", "Treatment Information"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    private final JTable diseaseTable = new JTable(tableModel);

    public ManageDisease() {
        setTitle("PULSE - Manage Disease Information");
        setSize(1100, 720);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        add(Theme.banner("Manage Disease Information",
                "Add new disease details or select an existing disease to update its information"),
                BorderLayout.NORTH);

        loadDiseases();

        // ---------------- Disease information form ----------------
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(Color.WHITE);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(7, 8, 7, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;

        nameField = new JTextField(22);
        addRow(formPanel, gbc, 0, requiredLabel("Disease Name"), nameField);

        symptomsArea = createTextArea();
        addRow(formPanel, gbc, 1, requiredLabel("Symptoms"), new JScrollPane(symptomsArea));

        causesArea = createTextArea();
        addRow(formPanel, gbc, 2, requiredLabel("Causes"), new JScrollPane(causesArea));

        treatmentArea = createTextArea();
        addRow(formPanel, gbc, 3, requiredLabel("Treatment Information"),
                new JScrollPane(treatmentArea));

        JLabel hint = new JLabel("<html><i>Select a disease from the table to load its details for updating.</i></html>");
        hint.setFont(Theme.BODY);
        hint.setForeground(Theme.NEUTRAL);
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        formPanel.add(hint, gbc);

        applyTheme(formPanel);

        JPanel formCard = new JPanel(new BorderLayout());
        formCard.setBackground(Color.WHITE);
        formCard.setBorder(new CompoundBorder(
                new LineBorder(new Color(200, 215, 225)),
                new EmptyBorder(15, 18, 15, 18)));
        formCard.add(formPanel, BorderLayout.CENTER);

        // ---------------- Existing disease records ----------------
        diseaseTable.setRowHeight(28);
        diseaseTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        diseaseTable.getTableHeader().setReorderingAllowed(false);
        diseaseTable.setGridColor(new Color(225, 232, 238));
        diseaseTable.setSelectionBackground(new Color(190, 225, 240));
        diseaseTable.setSelectionForeground(Color.BLACK);
        diseaseTable.getTableHeader().setBackground(Theme.PRIMARY);
        diseaseTable.getTableHeader().setForeground(Color.WHITE);
        diseaseTable.getTableHeader().setFont(Theme.BOLD);
        diseaseTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        diseaseTable.getColumnModel().getColumn(0).setPreferredWidth(130);
        diseaseTable.getColumnModel().getColumn(1).setPreferredWidth(170);
        diseaseTable.getColumnModel().getColumn(2).setPreferredWidth(150);
        diseaseTable.getColumnModel().getColumn(3).setPreferredWidth(190);

        JScrollPane tableScroll = new JScrollPane(diseaseTable);
        tableScroll.setBorder(new TitledBorder("Existing Disease Records"));

        JLabel tableInfo = new JLabel("Select a row to view or update a disease.");
        tableInfo.setFont(Theme.BOLD);
        tableInfo.setForeground(Theme.PRIMARY);

        JPanel tableCard = new JPanel(new BorderLayout(0, 8));
        tableCard.setBackground(Color.WHITE);
        tableCard.setBorder(new CompoundBorder(
                new LineBorder(new Color(200, 215, 225)),
                new EmptyBorder(10, 10, 10, 10)));
        tableCard.add(tableScroll, BorderLayout.CENTER);
        tableCard.add(tableInfo, BorderLayout.SOUTH);

        JPanel center = new JPanel(new GridLayout(1, 2, 15, 0));
        center.setBackground(Theme.BG);
        center.setBorder(new EmptyBorder(15, 18, 5, 18));
        center.add(formCard);
        center.add(tableCard);
        add(center, BorderLayout.CENTER);

        // ---------------- Buttons and status message ----------------
        JButton addButton = new Theme.PulseButton("Add Disease", Theme.ACCENT);
        JButton updateButton = new Theme.PulseButton("Update Disease", Theme.PRIMARY);
        JButton clearButton = new Theme.PulseButton("Clear", Theme.NEUTRAL);
        JButton backButton = new Theme.PulseButton("Back", Theme.NEUTRAL);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 4));
        buttonPanel.setOpaque(false);
        buttonPanel.add(addButton);
        buttonPanel.add(updateButton);
        buttonPanel.add(clearButton);
        buttonPanel.add(backButton);

        messageLabel = new JLabel(" ", SwingConstants.CENTER);
        messageLabel.setFont(Theme.BODY);
        messageLabel.setForeground(Theme.PRIMARY);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBackground(Theme.BG);
        bottomPanel.add(messageLabel, BorderLayout.NORTH);
        bottomPanel.add(buttonPanel, BorderLayout.CENTER);
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(7, 0, 13, 0));
        add(bottomPanel, BorderLayout.SOUTH);

        diseaseTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                loadSelectedDisease();
            }
        });

        addButton.addActionListener(e -> addDisease());
        updateButton.addActionListener(e -> updateDisease());
        clearButton.addActionListener(e -> clearForm());
        backButton.addActionListener(e -> {
            new MainFrame();
            dispose();
        });

        refreshTable();
        setVisible(true);
    }

    private JTextArea createTextArea() {
        JTextArea area = new JTextArea(4, 22);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(Theme.BODY);
        return area;
    }

    private JLabel requiredLabel(String text) {
        return new JLabel("<html>" + text + " <font color='red'>*</font></html>");
    }

    private void addRow(JPanel panel, GridBagConstraints gbc, int row,
                        Component label, Component field) {
        gbc.gridy = row;
        gbc.gridx = 0;
        gbc.weightx = 0;
        panel.add(label, gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(field, gbc);
    }

    private void applyTheme(Container container) {
        for (Component c : container.getComponents()) {
            if (c instanceof JPanel) {
                ((JPanel) c).setOpaque(false);
                applyTheme((Container) c);
            }
            if (c instanceof JLabel || c instanceof JTextField) {
                c.setFont(Theme.BODY);
            }
        }
    }

    private String[] readForm() {
        return new String[]{
                nameField.getText().trim(),
                symptomsArea.getText().trim(),
                causesArea.getText().trim(),
                treatmentArea.getText().trim()
        };
    }

    private boolean validateDisease(String[] disease) {
        for (String value : disease) {
            if (value.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "Please enter all disease information.",
                        "Invalid Information", JOptionPane.WARNING_MESSAGE);
                messageLabel.setText("Invalid information. Please enter all fields again.");
                return false;
            }
        }
        return true;
    }

    private int findDisease(String name) {
        for (int i = 0; i < diseases.size(); i++) {
            if (diseases.get(i)[0].equalsIgnoreCase(name)) {
                return i;
            }
        }
        return -1;
    }

    // Alternate Scenario 1: if the disease already exists, offer to update it.
    private void addDisease() {
        String[] disease = readForm();
        if (!validateDisease(disease)) return;

        int existingIndex = findDisease(disease[0]);
        if (existingIndex >= 0) {
            int choice = JOptionPane.showConfirmDialog(this,
                    "This disease record already exists.\nWould you like to update the existing record?",
                    "Disease Already Exists", JOptionPane.YES_NO_OPTION);
            if (choice == JOptionPane.YES_OPTION) {
                diseases.set(existingIndex, disease);
                saveAndRefresh("Disease information updated successfully.");
            } else {
                messageLabel.setText("Disease record already exists.");
            }
            return;
        }

        diseases.add(disease);
        saveAndRefresh("Disease information added successfully.");
        clearForm();
    }

    private void updateDisease() {
        String[] disease = readForm();
        if (!validateDisease(disease)) return;

        int selectedRow = diseaseTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this,
                    "Please select an existing disease record from the table first.",
                    "Select Disease", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int modelRow = diseaseTable.convertRowIndexToModel(selectedRow);
        String oldName = (String) tableModel.getValueAt(modelRow, 0);
        int diseaseIndex = findDisease(oldName);

        if (diseaseIndex < 0) {
            messageLabel.setText("The selected disease record could not be found.");
            return;
        }

        int duplicateIndex = findDisease(disease[0]);
        if (duplicateIndex >= 0 && duplicateIndex != diseaseIndex) {
            JOptionPane.showMessageDialog(this,
                    "Another disease already has this name. Please enter a different name.",
                    "Duplicate Disease Name", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int choice = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to update this disease information?",
                "Confirm Update", JOptionPane.YES_NO_OPTION);
        if (choice != JOptionPane.YES_OPTION) return;

        diseases.set(diseaseIndex, disease);
        saveAndRefresh("Disease information updated successfully.");
        clearForm();
    }

    private void loadSelectedDisease() {
        int selectedRow = diseaseTable.getSelectedRow();
        if (selectedRow < 0) return;

        int row = diseaseTable.convertRowIndexToModel(selectedRow);
        nameField.setText(String.valueOf(tableModel.getValueAt(row, 0)));
        symptomsArea.setText(String.valueOf(tableModel.getValueAt(row, 1)));
        causesArea.setText(String.valueOf(tableModel.getValueAt(row, 2)));
        treatmentArea.setText(String.valueOf(tableModel.getValueAt(row, 3)));
        messageLabel.setText("Disease record selected. You can update its information.");
    }

    private void clearForm() {
        nameField.setText("");
        symptomsArea.setText("");
        causesArea.setText("");
        treatmentArea.setText("");
        diseaseTable.clearSelection();
        messageLabel.setText(" ");
    }

    private void refreshTable() {
        tableModel.setRowCount(0);
        for (String[] disease : diseases) {
            tableModel.addRow(disease);
        }
    }

    private void saveAndRefresh(String successMessage) {
        try {
            saveDiseases();
            refreshTable();
            messageLabel.setText(successMessage);
            JOptionPane.showMessageDialog(this, successMessage,
                    "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this,
                    "Could not save disease information: " + ex.getMessage(),
                    "Save Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadDiseases() {
        try {
            if (!Files.exists(Paths.get(FILE_NAME))) return;

            for (String line : Files.readAllLines(Paths.get(FILE_NAME), StandardCharsets.UTF_8)) {
                // New records use Base64 so fields can safely contain | and multiple lines.
                if (line.startsWith("B64|")) {
                    String[] parts = line.substring(4).split("\\|", -1);
                    if (parts.length == 4) {
                        for (int i = 0; i < parts.length; i++) {
                            parts[i] = new String(Base64.getDecoder().decode(parts[i]), StandardCharsets.UTF_8);
                        }
                        diseases.add(parts);
                    }
                } else {
                    // Read records saved by the previous version for backward compatibility.
                    String[] parts = line.split("\\|", -1);
                    if (parts.length == 4) diseases.add(parts);
                }
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this,
                    "Could not read disease information: " + ex.getMessage(),
                    "File Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void saveDiseases() throws IOException {
        List<String> lines = new ArrayList<>();
        Base64.Encoder encoder = Base64.getEncoder();
        for (String[] disease : diseases) {
            String[] encoded = new String[disease.length];
            for (int i = 0; i < disease.length; i++) {
                encoded[i] = encoder.encodeToString(disease[i].getBytes(StandardCharsets.UTF_8));
            }
            lines.add("B64|" + String.join("|", encoded));
        }
        Files.write(Paths.get(FILE_NAME), lines, StandardCharsets.UTF_8);
    }
}
