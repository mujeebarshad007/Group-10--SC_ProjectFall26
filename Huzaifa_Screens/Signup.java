import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Consumer;

// "Search for Doctors" screen. Follows the sequence diagram:
//   searchDoctor() -> search interface
//   enterCriteria(specialization, location, name, feeRange) -> matching doctor list
//   selectDoctor(doctorID) -> doctor details, availability
//   alt [available]   proceedToBooking() -> appointment booking interface
//   alt [unavailable] doctor unavailable + alternative available doctors
//   alt [no match]    no matching doctors found
public class Signup extends JFrame {

    // ---------------------------------------------------------------- UI
    private final AutoField specialtyField;
    private final AutoField nameField;
    private final AutoField symptomsField;
    private final AutoField locationField;
    private final JComboBox<String> feeCombo =
        new JComboBox<>(new String[]{"Any", "0-2000", "2000-4000", "4000-6000", "6000-10000"});

    private final DefaultListModel<Doctor> listModel = new DefaultListModel<>();
    private final JList<Doctor> doctorList = new JList<>(listModel);
    private final JLabel statusLabel = new JLabel(" ");
    private final Map<Doctor, String> scoreNotes = new HashMap<>();

    // Right side switches between results and the booking interface
    private final CardLayout rightLayout = new CardLayout();
    private final JPanel rightPanel = new JPanel(rightLayout);
    private final JLabel bookingInfo = new JLabel(" ");
    private final JTextField patientField = new JTextField(18);
    private final JTextField phoneField = new JTextField(18);
    private final JTextField reasonField = new JTextField(18);
    private final JComboBox<String> dayCombo = new JComboBox<>();
    private final JComboBox<String> timeCombo = new JComboBox<>(
        new String[]{"09:00 AM", "10:30 AM", "12:00 PM", "02:00 PM", "04:30 PM", "06:00 PM"});
    private final Theme.PulseButton confirmButton = new Theme.PulseButton("Confirm Booking", Theme.ACCENT);
    private final java.util.Set<String> bookedSlots = new java.util.HashSet<>();
    private Doctor bookingDoctor;
    private final JEditorPane detailsPane = new JEditorPane("text/html", "");
    private final Theme.PulseButton searchButton = new Theme.PulseButton("Search", Theme.PRIMARY);

    public Signup() {
        setTitle("PULSE - Search for Doctors");
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        setSize(Math.min(1100, (int) (screen.width * 0.9)), Math.min(720, (int) (screen.height * 0.9)));
        setMinimumSize(new Dimension(Math.min(900, getWidth()), Math.min(600, getHeight())));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Autocomplete fields. Picking a doctor name or a symptom fills related fields.
        specialtyField = new AutoField(DoctorDatabase.SPECIALTIES, null);
        locationField = new AutoField(DoctorDatabase.LOCATIONS, null);
        nameField = new AutoField(DoctorDatabase.NAMES, picked -> {
            Doctor d = DoctorDatabase.findByName(picked);
            if (d != null) {
                specialtyField.fill(d.specialty);
                locationField.fill(d.location);
            }
        });
        symptomsField = new AutoField(DoctorDatabase.SYMPTOMS, picked -> {
            String spec = DoctorDatabase.SYMPTOM_TO_SPECIALTY.get(picked);
            if (spec != null) specialtyField.fill(spec);
        });

        setJMenuBar(buildMenuBar());
        add(Theme.banner("Search for Doctors",
            "Start typing - suggestions appear and fill in automatically"), BorderLayout.NORTH);

        JPanel body = new JPanel(new GridLayout(1, 2, 16, 0));
        body.setBackground(Theme.BG);
        body.setBorder(new EmptyBorder(16, 20, 16, 20));
        body.add(buildFormCard());
        rightPanel.setOpaque(false);
        rightPanel.add(buildResultsCard(), "results");
        rightPanel.add(buildBookingCard(), "booking");
        body.add(rightPanel);
        add(body, BorderLayout.CENTER);

        getRootPane().setDefaultButton(searchButton);
        showPlaceholder();
        setVisible(true);
    }

    private JMenuBar buildMenuBar() {
        JMenuBar bar = new JMenuBar();
        JMenu file = new JMenu("File");
        JMenuItem home = new JMenuItem("Home");
        JMenuItem exit = new JMenuItem("Exit");
        home.addActionListener(e -> goHome());
        exit.addActionListener(e -> System.exit(0));
        file.add(home);
        file.addSeparator();
        file.add(exit);

        JMenu help = new JMenu("Help");
        JMenuItem about = new JMenuItem("About");
        about.addActionListener(e -> JOptionPane.showMessageDialog(this,
            "PULSE Patient Portal\nSearch for doctors, check availability and book appointments.",
            "About PULSE", JOptionPane.INFORMATION_MESSAGE));
        help.add(about);

        bar.add(file);
        bar.add(help);
        return bar;
    }

    private Border cardBorder() {
        return new CompoundBorder(new LineBorder(new Color(200, 215, 225)),
            new EmptyBorder(18, 24, 18, 24));
    }

    // Left card: search criteria
    private JPanel buildFormCard() {
        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(cardBorder());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel heading = new JLabel("Find a Doctor");
        heading.setFont(Theme.HEADING);
        heading.setForeground(Theme.PRIMARY);

        JLabel hint = new JLabel("Type a few letters, then press Enter or click a suggestion");
        hint.setFont(new Font("Arial", Font.ITALIC, 12));
        hint.setForeground(Theme.NEUTRAL);

        feeCombo.setEditable(true);
        feeCombo.setFont(Theme.BODY);
        feeCombo.getEditor().getEditorComponent().setFont(Theme.BODY);
        feeCombo.setSelectedItem("Any");

        int row = 0;
        gbc.gridy = row++; gbc.insets = new Insets(0, 0, 2, 0); card.add(heading, gbc);
        gbc.gridy = row++; gbc.insets = new Insets(0, 0, 6, 0); card.add(hint, gbc);
        row = addRow(card, gbc, "Specialty", specialtyField, row);
        row = addRow(card, gbc, "Symptoms", symptomsField, row);
        row = addRow(card, gbc, "Doctor Name", nameField, row);
        row = addRow(card, gbc, "Location", locationField, row);
        row = addRow(card, gbc, "Fee Range (Rs.) - pick one or type e.g. 1000-5000", feeCombo, row);

        Theme.PulseButton clearButton = new Theme.PulseButton("Clear", Theme.NEUTRAL);
        Theme.PulseButton backButton = new Theme.PulseButton("Back", Theme.NEUTRAL);
        searchButton.addActionListener(e -> searchDoctors());
        clearButton.addActionListener(e -> clearFilters());
        backButton.addActionListener(e -> goHome());

        JPanel buttons = new JPanel(new GridLayout(1, 3, 10, 0));
        buttons.setOpaque(false);
        buttons.add(searchButton);
        buttons.add(clearButton);
        buttons.add(backButton);

        gbc.gridy = row++; gbc.ipady = 0; gbc.insets = new Insets(16, 0, 0, 0);
        card.add(buttons, gbc);

        gbc.gridy = row; gbc.weighty = 1; gbc.insets = new Insets(0, 0, 0, 0);
        card.add(Box.createVerticalGlue(), gbc);
        return card;
    }

    private int addRow(JPanel card, GridBagConstraints gbc, String label, JComponent field, int row) {
        JLabel l = new JLabel(label);
        l.setFont(Theme.BOLD);
        gbc.ipady = 0;
        gbc.gridy = row++; gbc.insets = new Insets(10, 0, 2, 0); card.add(l, gbc);
        gbc.ipady = 6;
        gbc.gridy = row++; gbc.insets = new Insets(0, 0, 2, 0); card.add(field, gbc);
        gbc.ipady = 0;
        return row;
    }

    // Right card: matching doctors, details and booking
    private JPanel buildResultsCard() {
        JPanel card = new JPanel(new BorderLayout(0, 10));
        card.setBackground(Color.WHITE);
        card.setBorder(cardBorder());

        JLabel heading = new JLabel("Matching Doctors");
        heading.setFont(Theme.HEADING);
        heading.setForeground(Theme.PRIMARY);
        statusLabel.setFont(new Font("Arial", Font.BOLD, 12));

        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.setOpaque(false);
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        top.add(heading);
        top.add(Box.createVerticalStrut(4));
        top.add(statusLabel);

        // Doctor list (selectDoctor)
        doctorList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        doctorList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean selected, boolean focus) {
                JLabel c = (JLabel) super.getListCellRendererComponent(list, value, index, selected, false);
                Doctor d = (Doctor) value;
                Color avail = d.available ? Theme.ACCENT : Theme.DANGER;
                String note = scoreNotes.containsKey(d) ? "<br>" + scoreNotes.get(d) : "";
                c.setText("<html><b>" + d.title() + "</b> &nbsp;|&nbsp; " + d.specialty
                    + "<br><font size='-1'>" + d.location + " &nbsp;|&nbsp; " + d.feeText()
                    + " &nbsp;|&nbsp; <font color='" + hex(avail) + "'>"
                    + (d.available ? "Available" : "Unavailable") + "</font>" + note + "</font></html>");
                c.setBorder(new EmptyBorder(6, 8, 6, 8));
                c.setBackground(selected ? Theme.INFO_BG : Color.WHITE);
                c.setForeground(Color.BLACK);
                return c;
            }
        });
        doctorList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && doctorList.getSelectedValue() != null) {
                showDetails(doctorList.getSelectedValue());
            }
        });

        detailsPane.setEditable(false);
        detailsPane.setBackground(Color.WHITE);

        JScrollPane listScroll = new JScrollPane(doctorList);
        listScroll.setPreferredSize(new Dimension(100, 150));
        listScroll.setBorder(titled("Search results (click a doctor)"));
        JScrollPane detailsScroll = new JScrollPane(detailsPane);
        detailsScroll.setPreferredSize(new Dimension(100, 150));
        detailsScroll.setBorder(titled("Doctor details and availability"));

        JPanel middle = new JPanel(new GridLayout(2, 1, 0, 10));
        middle.setOpaque(false);
        middle.add(listScroll);
        middle.add(detailsScroll);

        Theme.PulseButton bookButton = new Theme.PulseButton("Proceed to Booking", Theme.ACCENT);
        bookButton.addActionListener(e -> proceedToBooking());
        JPanel bottom = new JPanel(new GridLayout(1, 1));
        bottom.setOpaque(false);
        bottom.add(bookButton);

        card.add(top, BorderLayout.NORTH);
        card.add(middle, BorderLayout.CENTER);
        card.add(bottom, BorderLayout.SOUTH);
        return card;
    }

    private TitledBorder titled(String title) {
        TitledBorder tb = new TitledBorder(new LineBorder(new Color(200, 215, 225)), title);
        tb.setTitleFont(Theme.BOLD);
        tb.setTitleColor(Theme.PRIMARY);
        return tb;
    }

    // ------------------------------------------------------------- logic
    // enterCriteria(...) -> matching doctor list, or "no matching doctors found"
    private void searchDoctors() {
        String specialty = specialtyField.getText().trim();
        String name = nameField.getText().trim();
        String symptoms = symptomsField.getText().trim();
        String location = locationField.getText().trim();
        String fee = String.valueOf(feeCombo.getEditor().getItem()).trim();
        boolean anyFee = fee.isEmpty() || fee.equalsIgnoreCase("Any");

        if (specialty.isEmpty() && name.isEmpty() && symptoms.isEmpty() && location.isEmpty() && anyFee) {
            showError("Search Error", "Please enter at least one search criterion.");
            return;
        }
        if (!validText(specialty) || !validText(name) || !validText(symptoms) || !validText(location)) {
            showError("Search Error", "Please enter valid search text.");
            return;
        }

        int minFee = 0;
        int maxFee = Integer.MAX_VALUE;
        if (!anyFee) {
            if (!fee.matches("\\d+\\s*-\\s*\\d+")) {
                showError("Search Error", "Enter a valid fee range, for example 1000-5000.");
                return;
            }
            try {
                String[] parts = fee.split("\\s*-\\s*");
                minFee = Integer.parseInt(parts[0].trim());
                maxFee = Integer.parseInt(parts[1].trim());
            } catch (NumberFormatException ex) {
                showError("Search Error", "The fee range is too large. Try something like 1000-5000.");
                return;
            }
            if (minFee > maxFee) {
                showError("Search Error", "The minimum fee must not be greater than the maximum fee.");
                return;
            }
        }

        List<Doctor> matches = DoctorDatabase.search(specialty, name, symptoms, location, minFee, maxFee);
        scoreNotes.clear();

        if (!matches.isEmpty()) {
            showResults(matches, matches.size() + (matches.size() == 1 ? " doctor" : " doctors")
                + " found. Click a doctor to view details.", Theme.ACCENT);
            showPlaceholder();
            return;
        }

        // No doctor matches everything: show the closest matches instead of an empty list
        int active = (specialty.isEmpty() ? 0 : 1) + (name.isEmpty() ? 0 : 1)
            + (symptoms.isEmpty() ? 0 : 1) + (location.isEmpty() ? 0 : 1) + (anyFee ? 0 : 1);
        Map<Doctor, Integer> scores = new HashMap<>();
        List<Doctor> closest = DoctorDatabase.closest(specialty, name, symptoms, location,
            !anyFee, minFee, maxFee, scores);

        if (closest.isEmpty()) {
            showResults(matches, "No matching doctors found.", Theme.DANGER);
            detailsPane.setText(html("<span style='color:" + hex(Theme.DANGER)
                + "'><b>No matching doctors found.</b></span><br><br>"
                + "Check the spelling or pick from the dropdown suggestions."));
            return;
        }

        for (Doctor d : closest) {
            scoreNotes.put(d, "<font color='" + hex(Theme.PRIMARY) + "'>Matches "
                + scores.get(d) + " of " + active + " criteria</font>");
        }
        showResults(closest, "No doctor matches all criteria. Showing the closest matches:", Theme.PRIMARY);
        detailsPane.setText(html("<b>No doctor matches all of your criteria.</b><br><br>"
            + "The list shows doctors who match the most of what you entered. "
            + "Click a doctor to see their details, or change your criteria and search again."));
    }

    // selectDoctor(doctorID) -> doctor details, availability
    private void showDetails(Doctor d) {
        Color c = d.available ? Theme.ACCENT : Theme.DANGER;
        detailsPane.setText(html(
            "<span style='font-size:15px;color:" + hex(Theme.PRIMARY) + "'><b>" + d.title() + "</b></span><br>"
            + "<b>Specialty:</b> " + d.specialty + "<br>"
            + "<b>Qualifications:</b> " + d.qualifications + "<br>"
            + "<b>Experience:</b> " + d.experience + " years<br>"
            + "<b>Location:</b> " + d.location + " - " + d.hospital + "<br>"
            + "<b>Fee:</b> " + d.feeText() + "<br>"
            + "<b>Timings:</b> " + d.timings + "<br>"
            + "<b>Availability:</b> <span style='color:" + hex(c) + "'><b>"
            + (d.available ? "Available" : "Currently unavailable") + "</b></span>"));
        detailsPane.setCaretPosition(0);
    }

    // alt [available] -> booking interface ; alt [unavailable] -> alternatives
    private void proceedToBooking() {
        Doctor d = doctorList.getSelectedValue();
        if (d == null) {
            showError("Booking", "Search for a doctor and select one from the list first.");
            return;
        }
        if (d.available) {
            showBooking(d);
            return;
        }

        List<Doctor> alternatives = findAlternatives(d);
        if (alternatives.isEmpty()) {
            showResults(new ArrayList<>(), d.title() + " is unavailable. No alternatives found.", Theme.DANGER);
            detailsPane.setText(html("<span style='color:" + hex(Theme.DANGER) + "'><b>"
                + d.title() + " is currently unavailable.</b></span><br><br>"
                + "No alternative doctors are available right now. Please try again later."));
        } else {
            showResults(alternatives, d.title() + " is unavailable. Alternative available doctors:", Theme.DANGER);
            detailsPane.setText(html("<span style='color:" + hex(Theme.DANGER) + "'><b>"
                + d.title() + " is currently unavailable.</b></span><br><br>"
                + "Please select one of the alternative available doctors from the list."));
        }
    }

    private List<Doctor> findAlternatives(Doctor d) {
        return DoctorDatabase.alternatives(d);
    }

    // Appointment booking interface: shown inside this screen after proceedToBooking(doctorID)
    private JPanel buildBookingCard() {
        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(cardBorder());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel heading = new JLabel("Appointment Booking");
        heading.setFont(Theme.HEADING);
        heading.setForeground(Theme.PRIMARY);

        bookingInfo.setFont(Theme.BODY);
        bookingInfo.setOpaque(true);
        bookingInfo.setBackground(Theme.INFO_BG);
        bookingInfo.setBorder(new CompoundBorder(new LineBorder(Theme.INFO_BORDER),
            new EmptyBorder(8, 10, 8, 10)));

        patientField.setFont(Theme.BODY);
        phoneField.setFont(Theme.BODY);
        reasonField.setFont(Theme.BODY);
        dayCombo.setFont(Theme.BODY);
        timeCombo.setFont(Theme.BODY);

        Theme.PulseButton backToResults = new Theme.PulseButton("Back to Results", Theme.NEUTRAL);
        confirmButton.addActionListener(e -> confirmBooking());
        backToResults.addActionListener(e -> showRightCard("results"));
        JPanel buttons = new JPanel(new GridLayout(1, 2, 10, 0));
        buttons.setOpaque(false);
        buttons.add(confirmButton);
        buttons.add(backToResults);

        int row = 0;
        gbc.gridy = row++; gbc.insets = new Insets(0, 0, 8, 0); card.add(heading, gbc);
        gbc.gridy = row++; gbc.insets = new Insets(0, 0, 4, 0); card.add(bookingInfo, gbc);
        row = addRow(card, gbc, "Patient Name", patientField, row);
        row = addRow(card, gbc, "Phone Number", phoneField, row);
        JLabel dateLabel = new JLabel("Appointment Date");
        dateLabel.setFont(Theme.BOLD);
        JLabel timeLabel = new JLabel("Appointment Time");
        timeLabel.setFont(Theme.BOLD);
        JPanel dateTime = new JPanel(new GridLayout(2, 2, 10, 2));
        dateTime.setOpaque(false);
        dateTime.add(dateLabel);
        dateTime.add(timeLabel);
        dateTime.add(dayCombo);
        dateTime.add(timeCombo);
        gbc.ipady = 0;
        gbc.gridy = row++; gbc.insets = new Insets(10, 0, 2, 0); card.add(dateTime, gbc);
        row = addRow(card, gbc, "Reason for Visit (optional)", reasonField, row);
        gbc.gridy = row++; gbc.ipady = 0; gbc.insets = new Insets(16, 0, 0, 0); card.add(buttons, gbc);
        gbc.gridy = row; gbc.weighty = 1; gbc.insets = new Insets(0, 0, 0, 0);
        card.add(Box.createVerticalGlue(), gbc);
        return card;
    }

    // proceedToBooking(doctorID) -> appointment booking interface
    private void showBooking(Doctor d) {
        bookingDoctor = d;
        bookingInfo.setText("<html><b>" + d.title() + "</b> - " + d.specialty + "<br>"
            + d.location + " - " + d.hospital + "<br>Timings: " + d.timings
            + "<br>Fee: " + d.feeText() + "</html>");
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy");
        dayCombo.removeAllItems();
        for (int i = 1; i <= 7; i++) dayCombo.addItem(LocalDate.now().plusDays(i).format(fmt));
        timeCombo.setSelectedIndex(0);
        patientField.setText("");
        phoneField.setText("");
        reasonField.setText("");
        showRightCard("booking");
        patientField.requestFocusInWindow();
    }

    private void confirmBooking() {
        String patient = patientField.getText().trim();
        String phone = phoneField.getText().trim();
        String reason = reasonField.getText().trim();

        if (patient.isEmpty() || !patient.matches("[\\p{L} .'-]+")) {
            showError("Booking", "Please enter a valid patient name.");
            return;
        }
        if (!phone.matches("[0-9+\\- ]{10,15}")) {
            showError("Booking", "Please enter a valid phone number (10 to 15 digits).");
            return;
        }
        if (!validText(reason)) {
            showError("Booking", "Please enter a valid reason for the visit.");
            return;
        }

        String day = String.valueOf(dayCombo.getSelectedItem());
        String time = String.valueOf(timeCombo.getSelectedItem());
        if (!bookedSlots.add(bookingDoctor.id + "|" + day + "|" + time)) {
            showError("Booking", "That time slot is already booked. Please choose another time.");
            return;
        }

        JOptionPane.showMessageDialog(this,
            "Appointment confirmed!\n\nPatient: " + patient + "\nDoctor: " + bookingDoctor.title()
                + "\nDate: " + day + "\nTime: " + time + "\nFee: " + bookingDoctor.feeText(),
            "Booking Confirmed", JOptionPane.INFORMATION_MESSAGE);
        showRightCard("results");
        setStatus("Appointment booked with " + bookingDoctor.title() + " on " + day + " at " + time + ".",
            Theme.ACCENT);
    }

    // Switches the right side between the results list and the booking form
    private void showRightCard(String name) {
        rightLayout.show(rightPanel, name);
        getRootPane().setDefaultButton("booking".equals(name) ? confirmButton : searchButton);
    }

    private void clearFilters() {
        specialtyField.fill("");
        nameField.fill("");
        symptomsField.fill("");
        locationField.fill("");
        feeCombo.setSelectedItem("Any");
        showRightCard("results");
        listModel.clear();
        setStatus("Enter your criteria and press Search.", Theme.NEUTRAL);
        showPlaceholder();
        specialtyField.requestFocusInWindow();
    }

    // ----------------------------------------------------------- helpers
    private void showResults(List<Doctor> doctors, String status, Color color) {
        showRightCard("results");
        listModel.clear();
        for (Doctor d : doctors) listModel.addElement(d);
        setStatus(status, color);
    }

    private void showPlaceholder() {
        if (statusLabel.getText().trim().isEmpty()) {
            setStatus("Enter your criteria and press Search.", Theme.NEUTRAL);
        }
        detailsPane.setText(html("Doctor details and availability will appear here "
            + "after you search and select a doctor."));
    }

    private void setStatus(String text, Color color) {
        statusLabel.setText("<html><div style='width:380px'>" + text + "</div></html>");
        statusLabel.setForeground(color);
    }

    private String html(String inner) {
        return "<html><body style='font-family:Arial;font-size:13px;margin:6px'>" + inner + "</body></html>";
    }

    private static String hex(Color c) {
        return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
    }

    private boolean validText(String value) {
        return value.matches("[\\p{L}\\p{N} .,&()'-]*");
    }

    private void showError(String title, String message) {
        JOptionPane.showMessageDialog(this, message, title, JOptionPane.ERROR_MESSAGE);
    }

    private void goHome() {
        new MainFrame();
        dispose();
    }

    // ------------------------------------------------------ autocomplete
    // Text field with a suggestion dropdown. While typing it shows matching options,
    // completes the first match inline (the completed part is selected, so typing on
    // simply overwrites it) and calls onPick when a suggestion is accepted.
    private static class AutoField extends JTextField {
        private final List<String> options;
        private final Consumer<String> onPick;
        private final JPopupMenu popup = new JPopupMenu();
        private final DefaultListModel<String> model = new DefaultListModel<>();
        private final JList<String> list = new JList<>(model);
        private boolean updating;

        AutoField(List<String> options, Consumer<String> onPick) {
            super(18);
            this.options = options;
            this.onPick = onPick;
            setFont(Theme.BODY);

            list.setFont(Theme.BODY);
            list.setFocusable(false);
            list.setFixedCellHeight(26);
            list.setSelectionBackground(Theme.INFO_BG);
            list.setSelectionForeground(Color.BLACK);
            JScrollPane sp = new JScrollPane(list);
            sp.setBorder(new LineBorder(Theme.INFO_BORDER));
            sp.setFocusable(false);
            popup.setFocusable(false);
            popup.setBorder(BorderFactory.createEmptyBorder());
            popup.add(sp);

            getDocument().addDocumentListener(new DocumentListener() {
                @Override public void insertUpdate(DocumentEvent e) { changed(true); }
                @Override public void removeUpdate(DocumentEvent e) { changed(false); }
                @Override public void changedUpdate(DocumentEvent e) { }
            });

            list.addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) {
                    int i = list.locationToIndex(e.getPoint());
                    if (i >= 0) accept(model.get(i));
                }
            });
            list.addMouseMotionListener(new MouseMotionAdapter() {
                @Override public void mouseMoved(MouseEvent e) {
                    int i = list.locationToIndex(e.getPoint());
                    if (i >= 0) list.setSelectedIndex(i);
                }
            });

            // Clicking an empty field opens the full dropdown
            addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) {
                    if (getText().trim().isEmpty()) showAll();
                }
            });

            // Losing focus with a fully typed value counts as picking it
            addFocusListener(new FocusAdapter() {
                @Override public void focusLost(FocusEvent e) {
                    boolean wasOpen = popup.isVisible();
                    popup.setVisible(false);
                    if (!wasOpen) return;
                    String t = getText().trim();
                    for (String o : AutoField.this.options) {
                        if (o.equalsIgnoreCase(t)) { accept(o); break; }
                    }
                }
            });

            InputMap im = getInputMap(JComponent.WHEN_FOCUSED);
            ActionMap am = getActionMap();
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), "ac-down");
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), "ac-up");
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "ac-enter");
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "ac-esc");
            am.put("ac-down", new AbstractAction() {
                @Override public void actionPerformed(ActionEvent e) {
                    if (!popup.isVisible()) showAll(); else move(1);
                }
            });
            am.put("ac-up", new AbstractAction() {
                @Override public void actionPerformed(ActionEvent e) { if (popup.isVisible()) move(-1); }
            });
            am.put("ac-enter", new AbstractAction() {
                @Override public void actionPerformed(ActionEvent e) {
                    if (popup.isVisible() && list.getSelectedValue() != null) {
                        accept(list.getSelectedValue());
                    } else if (getRootPane() != null && getRootPane().getDefaultButton() != null) {
                        getRootPane().getDefaultButton().doClick();
                    }
                }
            });
            am.put("ac-esc", new AbstractAction() {
                @Override public void actionPerformed(ActionEvent e) { popup.setVisible(false); }
            });
        }

        // Sets the text without opening the dropdown
        void fill(String value) {
            updating = true;
            setText(value);
            setCaretPosition(value.length());
            updating = false;
            popup.setVisible(false);
        }

        private void accept(String value) {
            fill(value);
            if (onPick != null) onPick.accept(value);
        }

        private void changed(boolean typed) {
            if (!updating) SwingUtilities.invokeLater(() -> refresh(typed));
        }

        private void refresh(boolean typed) {
            if (updating || !isShowing()) return;
            String text = getText();
            if (text.trim().isEmpty()) {
                popup.setVisible(false);
                return;
            }
            String q = text.toLowerCase();
            model.clear();
            for (String o : options) if (o.toLowerCase().startsWith(q)) model.addElement(o);
            for (String o : options) {
                String ol = o.toLowerCase();
                if (!ol.startsWith(q) && ol.contains(q)) model.addElement(o);
            }
            if (model.isEmpty()) {
                popup.setVisible(false);
                return;
            }
            list.setSelectedIndex(0);
            showPopup();

            // Inline completion of the first match
            String first = model.get(0);
            if (typed && first.toLowerCase().startsWith(q) && first.length() > text.length()
                    && getCaretPosition() == text.length()) {
                int typedLength = text.length();
                updating = true;
                setText(first);
                select(typedLength, first.length());
                updating = false;
            }
        }

        private void showAll() {
            model.clear();
            for (String o : options) model.addElement(o);
            list.setSelectedIndex(0);
            showPopup();
        }

        private void showPopup() {
            int rows = Math.min(model.size(), 6);
            popup.setPopupSize(getWidth(), rows * 26 + 4);
            popup.show(this, 0, getHeight());
        }

        private void move(int delta) {
            int i = Math.max(0, Math.min(model.size() - 1, list.getSelectedIndex() + delta));
            list.setSelectedIndex(i);
            list.ensureIndexIsVisible(i);
        }
    }
}
