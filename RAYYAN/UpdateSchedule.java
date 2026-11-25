import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

// UC 04: Update / Define Schedule (Doctor)
public class UpdateSchedule extends JFrame {

    private static final String FILE_NAME = "Schedules.txt"; // Saved in the project folder
    private static final DateTimeFormatter TIME_FORMAT =
        DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH);

    // Doctors (same doctors as the Make Appointment screen)
    private final String[] doctors = {
        "Dr. Ahmed Khan", "Dr. Sara Ali", "Dr. Usman Tariq",
        "Dr. Hina Malik", "Dr. Bilal Hussain", "Dr. Ayesha Noor"
    };

    // Every schedule is stored as: {doctor, date, start time, end time, status}
    private final List<String[]> schedules = new ArrayList<>();
    private List<String[]> doctorSchedules = new ArrayList<>(); // Schedules of the selected doctor

    // Components
    private JTextField dateField;
    private JComboBox<String> doctorBox, existingBox, startBox, endBox;
    private JRadioButton newRadio, updateRadio, availableRadio, notAvailableRadio;
    private JLabel tableInfoLabel;
    private boolean loading = false; // Stops dropdown events while we refill them

    private final DefaultTableModel tableModel = new DefaultTableModel(
            new String[]{"Date", "Start Time", "End Time", "Status"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable scheduleTable = new JTable(tableModel);

    // Calendar data
    private LocalDate selectedDate = LocalDate.now().plusDays(1); // Default: tomorrow
    private LocalDate calendarMonth;  // Month currently shown in the calendar
    private JDialog calendarDialog;

    public UpdateSchedule() {

        setTitle("PULSE - Update / Define Schedule");
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Coloured banner at the top (theme)
        add(Theme.banner("Update / Define Schedule",
            "View the existing schedule, then add a new one or update an existing one"),
            BorderLayout.NORTH);

        loadSchedules(); // Reads the saved schedules from the file

        // ---------------- Form ----------------
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(Color.WHITE);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(7, 10, 7, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Doctor dropdown (first doctor is selected by default)
        doctorBox = new JComboBox<>(doctors);
        addRow(formPanel, gbc, 0, requiredLabel("Doctor"), doctorBox);

        // Radio buttons: New Schedule is selected by default
        newRadio = new JRadioButton("New Schedule", true);
        updateRadio = new JRadioButton("Existing Schedule");
        ButtonGroup typeGroup = new ButtonGroup();
        typeGroup.add(newRadio);
        typeGroup.add(updateRadio);

        JPanel typePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        typePanel.add(newRadio);
        typePanel.add(updateRadio);
        addRow(formPanel, gbc, 1, requiredLabel("Schedule Type"), typePanel);

        // Existing schedules of the selected doctor (only used for Existing Schedule)
        existingBox = new JComboBox<>();
        existingBox.setEnabled(false);
        existingBox.setPrototypeDisplayValue("0000-00-00 | 00:00 AM - 00:00 PM | Not Available"); // Keeps the dropdown wide
        addRow(formPanel, gbc, 2, new JLabel("Select Existing Schedule"), existingBox);

        // Date: text field shows the selected date, button opens the calendar
        dateField = new JTextField(selectedDate.toString(), 10);
        dateField.setEditable(false); // Date can only be chosen from the calendar
        dateField.setBackground(new Color(245, 245, 245));
        JButton calendarButton = new Theme.PulseButton("Select Date", Theme.PRIMARY);

        JPanel datePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        datePanel.add(dateField);
        datePanel.add(Box.createHorizontalStrut(10));
        datePanel.add(calendarButton);
        addRow(formPanel, gbc, 3, requiredLabel("Schedule Date"), datePanel);

        // Start and end time dropdowns (default 09:00 AM to 05:00 PM)
        String[] times = buildTimes();
        startBox = new JComboBox<>(times);
        startBox.setSelectedItem("09:00 AM");
        addRow(formPanel, gbc, 4, requiredLabel("Start Time"), startBox);

        endBox = new JComboBox<>(times);
        endBox.setSelectedItem("05:00 PM");
        addRow(formPanel, gbc, 5, requiredLabel("End Time"), endBox);

        // Radio buttons: Available is selected by default
        availableRadio = new JRadioButton("Available", true);
        notAvailableRadio = new JRadioButton("Not Available");
        ButtonGroup statusGroup = new ButtonGroup();
        statusGroup.add(availableRadio);
        statusGroup.add(notAvailableRadio);

        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        statusPanel.add(availableRadio);
        statusPanel.add(notAvailableRadio);
        addRow(formPanel, gbc, 6, requiredLabel("Schedule Status"), statusPanel);

        applyTheme(formPanel);

        // White card on a light background (theme)
        JPanel formCard = new JPanel(new BorderLayout());
        formCard.setBackground(Color.WHITE);
        formCard.setBorder(new CompoundBorder(new LineBorder(new Color(200, 215, 225)),
            new EmptyBorder(15, 25, 15, 25)));
        formCard.add(formPanel, BorderLayout.NORTH);

        // ---------------- Existing schedule table ----------------
        scheduleTable.setRowHeight(24);
        scheduleTable.getTableHeader().setReorderingAllowed(false);
        scheduleTable.setGridColor(new Color(225, 232, 238));
        scheduleTable.setSelectionBackground(new Color(190, 225, 240));
        scheduleTable.setSelectionForeground(Color.BLACK);
        scheduleTable.getTableHeader().setBackground(Theme.PRIMARY);
        scheduleTable.getTableHeader().setForeground(Color.WHITE);
        scheduleTable.getTableHeader().setFont(Theme.BOLD);
        scheduleTable.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, false, row, column);
                if (!isSelected) c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(245, 250, 252));
                return c;
            }
        });

        JScrollPane tableScroll = new JScrollPane(scheduleTable);
        tableScroll.setBorder(new TitledBorder("Existing Schedule"));

        tableInfoLabel = new JLabel(" ");
        tableInfoLabel.setFont(Theme.BOLD);
        tableInfoLabel.setForeground(Theme.PRIMARY);
        tableInfoLabel.setBorder(new EmptyBorder(0, 6, 0, 6));

        JPanel tableCard = new JPanel(new BorderLayout(0, 6));
        tableCard.setBackground(Color.WHITE);
        tableCard.setBorder(new CompoundBorder(new LineBorder(new Color(200, 215, 225)),
            new EmptyBorder(10, 10, 10, 10)));
        tableCard.add(tableScroll, BorderLayout.CENTER);
        tableCard.add(tableInfoLabel, BorderLayout.SOUTH);

        // Form on the left, table on the right
        JPanel center = new JPanel(new GridLayout(1, 2, 15, 0));
        center.setBackground(Theme.BG);
        center.setBorder(new EmptyBorder(15, 20, 5, 20));
        center.add(formCard);
        center.add(tableCard);
        add(center, BorderLayout.CENTER);

        // ---------------- Buttons at the bottom ----------------
        JButton confirmButton = new Theme.PulseButton("Confirm Schedule", Theme.ACCENT);
        JButton backButton = new Theme.PulseButton("Back", Theme.NEUTRAL);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.add(confirmButton);
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

        // ---------------- Events ----------------
        doctorBox.addActionListener(e -> { if (!loading) onDoctorChanged(); });
        existingBox.addActionListener(e -> {
            if (!loading && updateRadio.isSelected()) fillFromExisting();
        });
        newRadio.addActionListener(e -> existingBox.setEnabled(false));
        updateRadio.addActionListener(e -> onExistingSelected());
        calendarButton.addActionListener(e -> openCalendar());
        confirmButton.addActionListener(e -> confirmSchedule());

        // Back button: returns to the welcome screen
        backButton.addActionListener(e -> {
            new MainFrame();
            dispose();
        });

        refreshSchedules(); // Shows the existing schedule of the first doctor

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

    // Creates the time list: 08:00 AM to 08:00 PM, every 30 minutes
    private String[] buildTimes() {
        List<String> list = new ArrayList<>();
        LocalTime time = LocalTime.of(8, 0);
        while (!time.isAfter(LocalTime.of(20, 0))) {
            list.add(time.format(TIME_FORMAT));
            time = time.plusMinutes(30);
        }
        return list.toArray(new String[0]);
    }

    // Text shown for one schedule
    private String describe(String[] s) {
        return s[1] + " | " + s[2] + " - " + s[3] + " | " + s[4];
    }

    // Doctor changed: show that doctor's schedule
    private void onDoctorChanged() {
        refreshSchedules();
        if (updateRadio.isSelected()) {
            if (doctorSchedules.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                    "No existing schedule found for this doctor.\nPlease define a new schedule.");
                newRadio.setSelected(true);
                existingBox.setEnabled(false);
            } else {
                fillFromExisting();
            }
        }
    }

    // Existing Schedule radio selected
    private void onExistingSelected() {
        if (doctorSchedules.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "No existing schedule found for this doctor.\nPlease define a new schedule.");
            newRadio.setSelected(true);
            existingBox.setEnabled(false);
            return;
        }
        existingBox.setEnabled(true);
        fillFromExisting();
    }

    // Shows the saved schedule of the selected doctor in the table and in the dropdown
    private void refreshSchedules() {
        String doctor = (String) doctorBox.getSelectedItem();
        doctorSchedules = schedulesOf(doctor);

        tableModel.setRowCount(0);
        loading = true;
        existingBox.removeAllItems();
        for (String[] s : doctorSchedules) {
            tableModel.addRow(new Object[]{s[1], s[2], s[3], s[4]});
            existingBox.addItem(describe(s));
        }
        if (existingBox.getItemCount() > 0) existingBox.setSelectedIndex(0);
        loading = false;

        if (doctorSchedules.isEmpty()) {
            tableInfoLabel.setText("No schedule defined yet for " + doctor + ".");
        } else {
            tableInfoLabel.setText(doctorSchedules.size() + " schedule(s) for " + doctor + ".");
        }
    }

    // Copies the selected existing schedule into the form fields
    private void fillFromExisting() {
        int index = existingBox.getSelectedIndex();
        if (index < 0 || index >= doctorSchedules.size()) return;

        String[] s = doctorSchedules.get(index);
        selectedDate = LocalDate.parse(s[1]);
        dateField.setText(s[1]);
        startBox.setSelectedItem(s[2]);
        endBox.setSelectedItem(s[3]);
        if (s[4].equals("Available")) availableRadio.setSelected(true);
        else notAvailableRadio.setSelected(true);
    }

    // All schedules of one doctor, sorted by date and start time
    private List<String[]> schedulesOf(String doctor) {
        List<String[]> list = new ArrayList<>();
        for (String[] s : schedules) {
            if (s[0].equals(doctor)) list.add(s);
        }
        list.sort((a, b) -> {
            int result = a[1].compareTo(b[1]);
            if (result != 0) return result;
            return LocalTime.parse(a[2], TIME_FORMAT).compareTo(LocalTime.parse(b[2], TIME_FORMAT));
        });
        return list;
    }

    // Validates, checks conflicts, confirms and saves the schedule
    private void confirmSchedule() {

        String doctor = (String) doctorBox.getSelectedItem();
        String date = selectedDate.toString();
        String start = (String) startBox.getSelectedItem();
        String end = (String) endBox.getSelectedItem();
        String status = availableRadio.isSelected() ? "Available" : "Not Available";

        LocalTime startTime = LocalTime.parse(start, TIME_FORMAT);
        LocalTime endTime = LocalTime.parse(end, TIME_FORMAT);

        // Alternate Scenario 1: invalid date or time
        if (!selectedDate.isAfter(LocalDate.now()) || !endTime.isAfter(startTime)) {
            JOptionPane.showMessageDialog(this,
                "Invalid date and time. Please enter valid info.\n\n"
                + "- The date must be a future date.\n"
                + "- The end time must be after the start time.",
                "Invalid Schedule", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Which schedule is being updated (null when defining a new one)
        String[] editing = null;
        if (updateRadio.isSelected()) {
            int index = existingBox.getSelectedIndex();
            if (index < 0) {
                JOptionPane.showMessageDialog(this, "Please select an existing schedule");
                return;
            }
            editing = doctorSchedules.get(index);
        }

        // Alternate Scenario 2: conflict with an existing schedule
        for (String[] s : schedules) {
            if (s == editing) continue; // The schedule being updated cannot conflict with itself
            if (!s[0].equals(doctor) || !s[1].equals(date)) continue;

            LocalTime oldStart = LocalTime.parse(s[2], TIME_FORMAT);
            LocalTime oldEnd = LocalTime.parse(s[3], TIME_FORMAT);

            if (startTime.isBefore(oldEnd) && endTime.isAfter(oldStart)) {
                JOptionPane.showMessageDialog(this,
                    "Schedule conflict!\nThis time overlaps with: " + describe(s)
                    + "\n\nChoose another time.",
                    "Schedule Conflict", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }

        // Asks the doctor to confirm
        int choice = JOptionPane.showConfirmDialog(this,
            "Confirm this schedule?\n\nDoctor: " + doctor + "\nDate: " + date
            + "\nTime: " + start + " - " + end + "\nStatus: " + status,
            "Confirm Schedule", JOptionPane.YES_NO_OPTION);
        if (choice != JOptionPane.YES_OPTION) return;

        // Saves the new or updated schedule
        String[] oldValues = null;
        String[] record;
        if (editing == null) {
            record = new String[]{doctor, date, start, end, status};
            schedules.add(record);
        } else {
            record = editing;
            oldValues = editing.clone();
            record[1] = date;
            record[2] = start;
            record[3] = end;
            record[4] = status;
        }

        try {
            saveSchedules();
        } catch (IOException ex) {
            // Undo the change if the file could not be written
            if (oldValues == null) schedules.remove(record);
            else System.arraycopy(oldValues, 0, record, 0, 5);
            JOptionPane.showMessageDialog(this, "Could not save the schedule: " + ex.getMessage());
            return;
        }

        JOptionPane.showMessageDialog(this,
            "Schedule updated successfully!\n\nDoctor: " + doctor + "\nDate: " + date
            + "\nTime: " + start + " - " + end + "\nStatus: " + status,
            "Schedule Updated", JOptionPane.INFORMATION_MESSAGE);

        newRadio.setSelected(true);
        existingBox.setEnabled(false);
        refreshSchedules(); // The updated schedule is displayed in the table
    }

    // Reads Schedules.txt (one schedule per line: doctor|date|start|end|status)
    private void loadSchedules() {
        try {
            if (!Files.exists(Paths.get(FILE_NAME))) return;
            for (String line : Files.readAllLines(Paths.get(FILE_NAME), StandardCharsets.UTF_8)) {
                String[] parts = line.split("\\|");
                if (parts.length == 5) schedules.add(parts);
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Could not read saved schedules: " + ex.getMessage());
        }
    }

    // Writes all schedules to Schedules.txt
    private void saveSchedules() throws IOException {
        List<String> lines = new ArrayList<>();
        for (String[] s : schedules) lines.add(String.join("|", s));
        Files.write(Paths.get(FILE_NAME), lines, StandardCharsets.UTF_8);
    }
}
