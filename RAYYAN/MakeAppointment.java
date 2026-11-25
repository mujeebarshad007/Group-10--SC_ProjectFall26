import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.List;
import java.util.TreeSet;

// MakeAppointment class creates the Make Appointment screen
public class MakeAppointment extends JFrame {

    // Doctor data: {name, specialization, symptom, location}
    private final String[][] doctors = {
        {"Dr. Ahmed Khan",   "Cardiologist",      "Chest Pain", "Islamabad"},
        {"Dr. Sara Ali",     "Dermatologist",     "Skin Rash",  "Islamabad"},
        {"Dr. Usman Tariq",  "General Physician", "Fever",      "Rawalpindi"},
        {"Dr. Hina Malik",   "Dermatologist",     "Skin Rash",  "Rawalpindi"},
        {"Dr. Bilal Hussain","Neurologist",       "Headache",   "Lahore"},
        {"Dr. Ayesha Noor",  "General Physician", "Fever",      "Islamabad"}
    };

    // Time slots for morning and evening
    private final String[] morningSlots = {"09:00 AM", "10:00 AM", "11:00 AM"};
    private final String[] eveningSlots = {"04:00 PM", "05:00 PM", "06:00 PM", "07:00 PM"};

    // Components
    private JTextField nameField, dateField;
    private JRadioButton specRadio, symptomRadio, locationRadio;
    private JRadioButton morningRadio, eveningRadio;
    private JRadioButton confirmRadio, cancelRadio;
    private JButton submitButton;
    private JLabel valueLabel, detailsLabel;
    private JComboBox<String> valueBox, doctorBox, timeBox;
    private boolean loading = false; // Stops dropdown events while we refill them

    // Calendar data
    private LocalDate selectedDate = LocalDate.now().plusDays(1); // Default: tomorrow
    private LocalDate calendarMonth;  // Month currently shown in the calendar
    private JDialog calendarDialog;

    public MakeAppointment() {

        setTitle("PULSE - Make Appointment");
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Coloured banner at the top (theme)
        add(Theme.banner("Make Appointment",
            "Choose a doctor, pick a date and time, then confirm or cancel"), BorderLayout.NORTH);

        // Panel to hold all form components
        JPanel formPanel = new JPanel();
        formPanel.setLayout(new GridBagLayout());
        formPanel.setBackground(Color.WHITE);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(7, 10, 7, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Patient name
        nameField = new JTextField(20);
        addRow(formPanel, gbc, 0, requiredLabel("Patient Name"), nameField);

        // Radio buttons: Specialization is selected by default
        specRadio = new JRadioButton("Specialization", true);
        symptomRadio = new JRadioButton("Symptoms");
        locationRadio = new JRadioButton("Location");

        ButtonGroup searchGroup = new ButtonGroup(); // Only one can be selected
        searchGroup.add(specRadio);
        searchGroup.add(symptomRadio);
        searchGroup.add(locationRadio);

        JPanel radioPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        radioPanel.add(specRadio);
        radioPanel.add(symptomRadio);
        radioPanel.add(locationRadio);
        addRow(formPanel, gbc, 1, requiredLabel("Search Doctor By"), radioPanel);

        // Dropdown lists
        valueLabel = new JLabel();
        valueBox = new JComboBox<>();
        addRow(formPanel, gbc, 2, valueLabel, valueBox);

        doctorBox = new JComboBox<>();
        addRow(formPanel, gbc, 3, requiredLabel("Select Doctor"), doctorBox);

        // Doctor details (changes when the doctor changes)
        detailsLabel = new JLabel();
        addRow(formPanel, gbc, 4, new JLabel("Doctor Details"), detailsLabel);

        // Date: text field shows the selected date, button opens the calendar
        dateField = new JTextField(selectedDate.toString(), 10);
        dateField.setEditable(false); // Date can only be chosen from the calendar
        dateField.setBackground(new Color(245, 245, 245));
        JButton calendarButton = new Theme.PulseButton("Select Date", Theme.PRIMARY);

        JPanel datePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        datePanel.add(dateField);
        datePanel.add(Box.createHorizontalStrut(10));
        datePanel.add(calendarButton);
        addRow(formPanel, gbc, 5, requiredLabel("Appointment Date"), datePanel);

        // Radio buttons: Morning is selected by default
        morningRadio = new JRadioButton("Morning", true);
        eveningRadio = new JRadioButton("Evening");

        ButtonGroup dayGroup = new ButtonGroup();
        dayGroup.add(morningRadio);
        dayGroup.add(eveningRadio);

        JPanel dayPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        dayPanel.add(morningRadio);
        dayPanel.add(eveningRadio);
        addRow(formPanel, gbc, 6, requiredLabel("Appointment Time Of Day"), dayPanel);

        // Time dropdown (changes with Morning / Evening)
        timeBox = new JComboBox<>();
        addRow(formPanel, gbc, 7, requiredLabel("Appointment Time"), timeBox);

        // Radio buttons: choose to confirm or cancel the appointment (Confirm is default)
        confirmRadio = new JRadioButton("Confirm Appointment", true);
        cancelRadio = new JRadioButton("Cancel Appointment");

        ButtonGroup actionGroup = new ButtonGroup();
        actionGroup.add(confirmRadio);
        actionGroup.add(cancelRadio);

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        actionPanel.add(confirmRadio);
        actionPanel.add(cancelRadio);
        addRow(formPanel, gbc, 8, requiredLabel("Action"), actionPanel);

        // Applies the theme fonts and transparent panels to everything inside the form
        applyTheme(formPanel);
        detailsLabel.setForeground(Theme.PRIMARY);

        // White card on a light background (theme)
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(new CompoundBorder(new LineBorder(new Color(200, 215, 225)),
            new EmptyBorder(15, 40, 15, 40)));
        card.add(formPanel, BorderLayout.CENTER);

        // Keeps the card in the center without stretching
        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setBackground(Theme.BG);
        wrapper.add(card);
        add(wrapper, BorderLayout.CENTER);

        // Buttons at the bottom
        submitButton = new Theme.PulseButton("Confirm Appointment", Theme.ACCENT);
        JButton backButton = new Theme.PulseButton("Back", Theme.NEUTRAL);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.add(submitButton);
        buttonPanel.add(backButton);

        JLabel noteLabel = new JLabel("",
                SwingConstants.CENTER);
        noteLabel.setFont(Theme.BODY);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBackground(Theme.BG);
        bottomPanel.add(noteLabel, BorderLayout.NORTH);
        bottomPanel.add(buttonPanel, BorderLayout.CENTER);
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(8, 0, 16, 0));
        add(bottomPanel, BorderLayout.SOUTH);

        // Radio button change: reload the first dropdown
        specRadio.addActionListener(e -> loadValues());
        symptomRadio.addActionListener(e -> loadValues());
        locationRadio.addActionListener(e -> loadValues());

        // Dropdown change: reload the next dropdown
        valueBox.addActionListener(e -> { if (!loading) loadDoctors(); });
        doctorBox.addActionListener(e -> { if (!loading) showDetails(); });

        // Morning / Evening change: reload the time dropdown
        morningRadio.addActionListener(e -> loadTimes());
        eveningRadio.addActionListener(e -> loadTimes());

        // Opens the calendar window
        calendarButton.addActionListener(e -> openCalendar());

        // Action radio change: the button text changes (Confirm / Cancel)
        confirmRadio.addActionListener(e -> submitButton.setText("Confirm Appointment"));
        cancelRadio.addActionListener(e -> submitButton.setText("Cancel Appointment"));

        // Submit button: confirms or cancels the appointment
        submitButton.addActionListener(e -> {
            if (confirmRadio.isSelected()) {
                saveAppointment();
            } else {
                cancelAppointment();
            }
        });

        // Back button: returns to the welcome screen
        backButton.addActionListener(e -> {
            new MainFrame();
            dispose();
        });

        loadValues(); // Fills all dropdowns with default selections
        loadTimes();  // Fills the time dropdown (Morning by default)

        setVisible(true);
    }

    // Gives every label, radio button, text field and dropdown inside a panel the theme look
    private void applyTheme(Container container) {
        for (Component c : container.getComponents()) {
            if (c instanceof JPanel) {
                ((JPanel) c).setOpaque(false);
            }
            if (c instanceof JRadioButton) {
                ((JRadioButton) c).setOpaque(false);
            }
            if (c instanceof JLabel || c instanceof JRadioButton
                    || c instanceof JComboBox || c instanceof JTextField) {
                c.setFont(Theme.BODY);
            }
            if (c instanceof JPanel) {
                applyTheme((Container) c);
            }
        }
    }

    // Adds one row (label + field) to the form
    private void addRow(JPanel panel, GridBagConstraints gbc, int row, Component label, Component field) {
        gbc.gridy = row;
        gbc.gridx = 0;
        panel.add(label, gbc);
        gbc.gridx = 1;
        panel.add(field, gbc);
    }

    // Creates a label with a red star for required fields
    private JLabel requiredLabel(String text) {
        return new JLabel("<html>" + text + " <font color='red'>*</font></html>");
    }

    // Column of the doctor data to search: 1 = specialization, 2 = symptom, 3 = location
    private int column() {
        if (specRadio.isSelected()) return 1;
        if (symptomRadio.isSelected()) return 2;
        return 3;
    }

    private String searchName() {
        if (specRadio.isSelected()) return "Specialization";
        if (symptomRadio.isSelected()) return "Symptoms";
        return "Location";
    }

    // Fills the first dropdown (first option is selected by default)
    private void loadValues() {
        loading = true;
        valueBox.removeAllItems();
        TreeSet<String> values = new TreeSet<>();
        for (String[] d : doctors) values.add(d[column()]);
        for (String v : values) valueBox.addItem(v);
        valueBox.setSelectedIndex(0);
        loading = false;

        valueLabel.setText("<html>Select " + searchName() + " <font color='red'>*</font></html>");
        loadDoctors();
    }

    // Fills the doctor dropdown with matching doctors (first doctor selected by default)
    private void loadDoctors() {
        loading = true;
        doctorBox.removeAllItems();
        String value = (String) valueBox.getSelectedItem();
        for (String[] d : doctors) {
            if (d[column()].equals(value)) doctorBox.addItem(d[0]);
        }
        doctorBox.setSelectedIndex(0);
        loading = false;

        showDetails();
    }

    // Shows the details of the selected doctor
    private void showDetails() {
        String name = (String) doctorBox.getSelectedItem();
        for (String[] d : doctors) {
            if (d[0].equals(name)) {
                detailsLabel.setText(d[1] + " | " + d[2] + " | " + d[3]);
            }
        }
    }

    // Fills the time dropdown with morning or evening slots (first slot selected by default)
    private void loadTimes() {
        timeBox.removeAllItems();
        String[] slots = morningRadio.isSelected() ? morningSlots : eveningSlots;
        for (String s : slots) timeBox.addItem(s);
        timeBox.setSelectedIndex(0);
    }

    // Opens a small calendar window to choose the date
    private void openCalendar() {
        calendarMonth = selectedDate.withDayOfMonth(1); // Start with the month of the selected date

        calendarDialog = new JDialog(this, "Select Date", true);
        calendarDialog.setSize(420, 380);
        calendarDialog.setLocationRelativeTo(this);
        calendarDialog.setLayout(new BorderLayout());
        calendarDialog.getContentPane().setBackground(Color.WHITE);

        // Top bar: previous month, month name, next month
        JButton prevButton = new Theme.PulseButton("<", Theme.ACCENT);
        JButton nextButton = new Theme.PulseButton(">", Theme.ACCENT);
        JLabel monthLabel = new JLabel("", SwingConstants.CENTER);
        monthLabel.setFont(new Font("Arial", Font.BOLD, 16));
        monthLabel.setForeground(Color.WHITE);

        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        topBar.setBackground(Theme.PRIMARY);
        topBar.add(prevButton, BorderLayout.WEST);
        topBar.add(monthLabel, BorderLayout.CENTER);
        topBar.add(nextButton, BorderLayout.EAST);
        calendarDialog.add(topBar, BorderLayout.NORTH);

        // Panel that holds the day names and day buttons (7 columns)
        JPanel daysPanel = new JPanel(new GridLayout(0, 7, 5, 5));
        daysPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        daysPanel.setBackground(Color.WHITE);
        calendarDialog.add(daysPanel, BorderLayout.CENTER);

        prevButton.addActionListener(e -> {
            // Cannot go before the current month
            if (calendarMonth.isAfter(LocalDate.now().withDayOfMonth(1))) {
                calendarMonth = calendarMonth.minusMonths(1);
                fillCalendar(daysPanel, monthLabel);
            }
        });

        nextButton.addActionListener(e -> {
            calendarMonth = calendarMonth.plusMonths(1);
            fillCalendar(daysPanel, monthLabel);
        });

        fillCalendar(daysPanel, monthLabel);
        calendarDialog.setVisible(true);
    }

    // Draws the days of the month inside the calendar window
    private void fillCalendar(JPanel daysPanel, JLabel monthLabel) {
        daysPanel.removeAll();
        monthLabel.setText(calendarMonth.getMonth() + " " + calendarMonth.getYear());

        // Day names
        String[] dayNames = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
        for (String name : dayNames) {
            JLabel label = new JLabel(name, SwingConstants.CENTER);
            label.setFont(new Font("Arial", Font.BOLD, 12));
            label.setForeground(Theme.PRIMARY);
            daysPanel.add(label);
        }

        // Empty cells before the first day of the month
        int emptyCells = calendarMonth.getDayOfWeek().getValue() - 1; // Monday = 1
        for (int i = 0; i < emptyCells; i++) {
            daysPanel.add(new JLabel(""));
        }

        // One button for every day of the month
        for (int day = 1; day <= calendarMonth.lengthOfMonth(); day++) {
            LocalDate date = calendarMonth.withDayOfMonth(day);

            JButton dayButton = new JButton(String.valueOf(day));
            dayButton.setMargin(new Insets(2, 2, 2, 2));
            dayButton.setFocusPainted(false);
            dayButton.setOpaque(true);
            dayButton.setBackground(Color.WHITE);
            dayButton.setForeground(Theme.PRIMARY);

            // Today and past dates cannot be selected
            if (!date.isAfter(LocalDate.now())) {
                dayButton.setEnabled(false);
            }

            // Highlights the currently selected date
            if (date.equals(selectedDate)) {
                dayButton.setBackground(Theme.PRIMARY);
                dayButton.setForeground(Color.WHITE);
            }

            dayButton.addActionListener(e -> {
                selectedDate = date;
                dateField.setText(date.toString());
                calendarDialog.dispose(); // Closes the calendar
            });

            daysPanel.add(dayButton);
        }

        daysPanel.revalidate();
        daysPanel.repaint();
    }

    // Validates the form, asks for a folder and saves the appointment
    private void saveAppointment() {

        String patient = nameField.getText().trim();

        // Required field check
        if (patient.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter patient name");
            return;
        }

        String doctor = (String) doctorBox.getSelectedItem();
        String date = selectedDate.toString();
        String time = (String) timeBox.getSelectedItem();

        // Let the user choose the folder where the appointment will be saved
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select the folder to save the appointment");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            JOptionPane.showMessageDialog(this, "Appointment not saved. Please select a folder.");
            return;
        }

        // All appointments are added to Appointments.txt inside the chosen folder
        File file = new File(chooser.getSelectedFile(), "Appointments.txt");

        try (FileWriter writer = new FileWriter(file, true)) {
            writer.write("Patient Name: " + patient + "\n");
            writer.write("Doctor: " + doctor + "\n");
            writer.write("Date: " + date + "\n");
            writer.write("Time: " + time + "\n");
            writer.write("Status: Confirmed\n");
            writer.write("------------------------------\n");
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Could not save the appointment: " + ex.getMessage());
            return;
        }

        JOptionPane.showMessageDialog(this,
            "Appointment confirmed!\n\nPatient: " + patient + "\nDoctor: " + doctor
            + "\nDate: " + date + "\nTime: " + time
            + "\n\nSaved in: " + file.getAbsolutePath());

        nameField.setText(""); // Clears the name for the next appointment
    }

    // Cancels an appointment: finds it in Appointments.txt and marks it as Cancelled
    private void cancelAppointment() {

        String patient = nameField.getText().trim();

        // Required field check
        if (patient.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter patient name");
            return;
        }

        String doctor = (String) doctorBox.getSelectedItem();
        String date = selectedDate.toString();
        String time = (String) timeBox.getSelectedItem();

        // Asks the user to confirm before cancelling
        int choice = JOptionPane.showConfirmDialog(this,
            "Cancel this appointment?\n\nPatient: " + patient + "\nDoctor: " + doctor
            + "\nDate: " + date + "\nTime: " + time,
            "Confirm Cancellation", JOptionPane.YES_NO_OPTION);

        if (choice != JOptionPane.YES_OPTION) return;

        // Choose the folder where Appointments.txt was saved
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select the folder where Appointments.txt is saved");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            JOptionPane.showMessageDialog(this, "Appointment not cancelled. Please select a folder.");
            return;
        }

        File file = new File(chooser.getSelectedFile(), "Appointments.txt");

        if (!file.exists()) {
            JOptionPane.showMessageDialog(this, "Appointments.txt was not found in this folder.");
            return;
        }

        try {
            List<String> lines = Files.readAllLines(file.toPath(), Charset.defaultCharset());
            boolean found = false;

            // Every appointment uses 6 lines: name, doctor, date, time, status, separator
            for (int i = 0; i + 4 < lines.size(); i++) {
                if (lines.get(i).equalsIgnoreCase("Patient Name: " + patient)
                        && lines.get(i + 1).equals("Doctor: " + doctor)
                        && lines.get(i + 2).equals("Date: " + date)
                        && lines.get(i + 3).equals("Time: " + time)
                        && lines.get(i + 4).equals("Status: Confirmed")) {

                    lines.set(i + 4, "Status: Cancelled"); // Changes the status
                    found = true;
                    break;
                }
            }

            if (!found) {
                JOptionPane.showMessageDialog(this,
                    "No confirmed appointment found for these details.");
                return;
            }

            Files.write(file.toPath(), lines, Charset.defaultCharset()); // Saves the file again

        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Could not cancel the appointment: " + ex.getMessage());
            return;
        }

        JOptionPane.showMessageDialog(this,
            "Appointment cancelled successfully!\n\nPatient: " + patient + "\nDoctor: " + doctor
            + "\nDate: " + date + "\nTime: " + time);

        nameField.setText(""); // Clears the name for the next appointment
    }
}
