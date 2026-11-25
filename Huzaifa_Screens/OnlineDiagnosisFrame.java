import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

// Online Diagnosis use case.
// Flow: Doctor ID -> online appointments -> appointment details -> Join Consultation ->
//       consultation room (diagnosis + prescription) -> Save and Complete.
// There is no real video call, so the other side of the call (patient joining, connection
// failure) is simulated with the "Demo controls" in the consultation room.
public class OnlineDiagnosisFrame extends JFrame {

    private static final Color BORDER_GRAY = new Color(180, 195, 205);
    private static final Color HINT_GRAY = new Color(120, 130, 140);
    private static final Color WARN = new Color(200, 120, 0);
    private static final Color PANEL_BG = new Color(246, 250, 252);

    private String doctorId = "";
    private ConsultationStore.Appointment appt;
    private Component currentScreen;

    // consultation room state
    private Timer joinTimer, clockTimer, rejoinTimer;
    private boolean patientJoined, connectionLost;
    private int seconds;
    private JLabel doctorStatus, patientStatus, timerLabel, messageLabel, formError;
    private JTextArea symptomsArea, diagnosisArea, prescriptionArea, instructionsArea;
    private Theme.PulseButton saveButton, rejoinButton, patientJoinsButton, failButton;

    public OnlineDiagnosisFrame() {
        setTitle("PULSE - Online Diagnosis");
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        setSize(Math.min(1100, (int) (screen.width * 0.9)), Math.min(720, (int) (screen.height * 0.9)));
        setMinimumSize(new Dimension(Math.min(900, getWidth()), Math.min(600, getHeight())));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        add(Theme.banner("Online Diagnosis",
            "Consult the patient online, record the diagnosis and prescription"), BorderLayout.NORTH);

        showStart();
        setVisible(true);
    }

    // ---------------------------------------------------------------- helpers

    private void setScreen(JPanel card) {
        JPanel background = new JPanel(new GridBagLayout());
        background.setBackground(Theme.BG);
        background.add(card);
        JScrollPane scroll = new JScrollPane(background);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        if (currentScreen != null) remove(currentScreen);
        currentScreen = scroll;
        add(scroll, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    private JPanel newCard(int vertical, int horizontal) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Color.WHITE);
        card.setBorder(new CompoundBorder(new LineBorder(new Color(200, 215, 225)),
            new EmptyBorder(vertical, horizontal, vertical, horizontal)));
        return card;
    }

    private JLabel centered(String text, Font font, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(font);
        l.setForeground(color);
        l.setAlignmentX(Component.CENTER_ALIGNMENT);
        return l;
    }

    private JPanel buttonRow(Theme.PulseButton... buttons) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 0));
        row.setOpaque(false);
        for (Theme.PulseButton b : buttons) {
            b.setFont(new Font("Arial", Font.BOLD, 16));
            b.setBorder(new EmptyBorder(12, 30, 12, 30));
            row.add(b);
        }
        row.setAlignmentX(Component.CENTER_ALIGNMENT);
        return row;
    }

    private JTable styledTable(DefaultTableModel model) {
        JTable table = new JTable(model);
        table.setFont(new Font("Arial", Font.PLAIN, 15));
        table.setRowHeight(32);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setGridColor(new Color(220, 228, 234));
        table.setSelectionBackground(new Color(200, 225, 238));
        table.setSelectionForeground(Color.BLACK);
        JTableHeader header = table.getTableHeader();
        header.setFont(new Font("Arial", Font.BOLD, 15));
        header.setBackground(Theme.PRIMARY);
        header.setForeground(Color.WHITE);
        header.setReorderingAllowed(false);
        return table;
    }

    private DefaultTableModel readOnlyModel(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
    }

    private String html(String text) {
        String safe = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\n", "<br>");
        return "<html><div style='width:430px'>" + safe + "</div></html>";
    }

    private String now() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a", Locale.ENGLISH));
    }

    private void stopTimers() {
        if (joinTimer != null) joinTimer.stop();
        if (clockTimer != null) clockTimer.stop();
        if (rejoinTimer != null) rejoinTimer.stop();
    }

    // ---------------------------------------------------------------- screen 1: doctor ID

    private void showStart() {
        stopTimers();
        JPanel card = newCard(40, 80);

        card.add(centered("Online Diagnosis", new Font("Arial", Font.BOLD, 36), Theme.PRIMARY));
        card.add(Box.createVerticalStrut(10));
        card.add(centered("Doctors: enter your Doctor ID to see your online appointments",
            new Font("Arial", Font.PLAIN, 18), Color.DARK_GRAY));
        card.add(Box.createVerticalStrut(28));
        card.add(centered("Doctor ID", Theme.HEADING, Color.DARK_GRAY));
        card.add(Box.createVerticalStrut(8));

        JTextField idField = new JTextField(doctorId, 14);
        idField.setFont(new Font("Arial", Font.PLAIN, 20));
        idField.setHorizontalAlignment(JTextField.CENTER);
        idField.setBorder(new CompoundBorder(new LineBorder(BORDER_GRAY), new EmptyBorder(6, 10, 6, 10)));
        idField.setMaximumSize(new Dimension(300, 44));
        idField.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(idField);
        card.add(Box.createVerticalStrut(6));
        card.add(centered("Sample doctor IDs with appointments: D101, D107, D112",
            new Font("Arial", Font.PLAIN, 13), HINT_GRAY));
        card.add(Box.createVerticalStrut(14));
        JLabel error = centered(" ", new Font("Arial", Font.BOLD, 14), Theme.DANGER);
        card.add(error);
        card.add(Box.createVerticalStrut(18));

        Theme.PulseButton continueButton = new Theme.PulseButton("View Appointments", Theme.PRIMARY);
        Theme.PulseButton patientButton = new Theme.PulseButton("Patient: My Prescriptions", Theme.ACCENT);
        Theme.PulseButton homeButton = new Theme.PulseButton("Back to Home", Theme.NEUTRAL);

        Runnable proceed = () -> {
            String id = idField.getText().trim().toUpperCase();
            if (id.isEmpty()) {
                error.setText("Please enter your Doctor ID.");
            } else if (!id.matches("D\\d{3,6}")) {
                error.setText("A doctor ID must start with D followed by digits (e.g. D101).");
            } else if (ConsultationStore.forDoctor(id).isEmpty()) {
                error.setText("No online appointments found for Doctor ID " + id + ".");
            } else {
                doctorId = id;
                showAppointments();
            }
        };
        continueButton.addActionListener(e -> proceed.run());
        idField.addActionListener(e -> proceed.run());
        patientButton.addActionListener(e -> showPatientPrescriptions());
        homeButton.addActionListener(e -> {
            new MainFrame();
            dispose();
        });

        card.add(buttonRow(continueButton, patientButton, homeButton));
        setScreen(card);
    }

    // ---------------------------------------------------------------- screen 2: appointments

    private void showAppointments() {
        stopTimers();
        JPanel card = newCard(28, 40);
        card.add(centered("Online Appointments", new Font("Arial", Font.BOLD, 32), Theme.PRIMARY));
        card.add(Box.createVerticalStrut(6));
        card.add(centered(ConsultationStore.doctorTitle(doctorId) + "  (" + doctorId + ")",
            new Font("Arial", Font.PLAIN, 17), Theme.NEUTRAL));
        card.add(Box.createVerticalStrut(18));

        List<ConsultationStore.Appointment> list = ConsultationStore.forDoctor(doctorId);
        DefaultTableModel model = readOnlyModel("Appointment ID", "Patient", "Patient ID", "Date & Time", "Status");
        for (ConsultationStore.Appointment a : list)
            model.addRow(new Object[]{a.id, a.patientName, a.patientId, a.time, a.status});
        JTable table = styledTable(model);
        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(new LineBorder(BORDER_GRAY));
        sp.setPreferredSize(new Dimension(800, 230));
        sp.setMaximumSize(new Dimension(800, 230));
        sp.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(sp);
        card.add(Box.createVerticalStrut(10));

        JLabel error = centered("Select an appointment and click Open Appointment.",
            new Font("Arial", Font.PLAIN, 14), HINT_GRAY);
        card.add(error);
        card.add(Box.createVerticalStrut(16));

        Runnable open = () -> {
            int row = table.getSelectedRow();
            if (row < 0) {
                error.setText("Please select an appointment first.");
                error.setForeground(Theme.DANGER);
            } else {
                appt = list.get(row);
                showDetails();
            }
        };
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) open.run();
            }
        });

        Theme.PulseButton openButton = new Theme.PulseButton("Open Appointment", Theme.PRIMARY);
        Theme.PulseButton backButton = new Theme.PulseButton("Back", Theme.NEUTRAL);
        openButton.addActionListener(e -> open.run());
        backButton.addActionListener(e -> showStart());
        card.add(buttonRow(openButton, backButton));
        setScreen(card);
    }

    // ---------------------------------------------------------------- screen 3: appointment details

    private void showDetails() {
        stopTimers();
        JPanel card = newCard(30, 60);
        card.add(centered("Appointment Details", new Font("Arial", Font.BOLD, 32), Theme.PRIMARY));
        card.add(Box.createVerticalStrut(18));

        JPanel grid = new JPanel(new GridBagLayout());
        grid.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 10, 6, 10);
        c.anchor = GridBagConstraints.NORTHWEST;
        int row = 0;
        addRow(grid, c, row++, "Appointment ID", appt.id);
        addRow(grid, c, row++, "Doctor", ConsultationStore.doctorTitle(appt.doctorId) + " (" + appt.doctorId + ")");
        addRow(grid, c, row++, "Patient", appt.patientName + " (" + appt.patientId + ")");
        addRow(grid, c, row++, "Date & Time", appt.time);
        addRow(grid, c, row++, "Type", "Online consultation");
        addRow(grid, c, row++, "Symptoms at booking", appt.bookedSymptoms);
        addRow(grid, c, row++, "Status", appt.status);
        if (appt.isCompleted()) {
            addRow(grid, c, row++, "Completed on", appt.completedOn);
            addRow(grid, c, row++, "Diagnosis / advice", html(appt.diagnosis));
            addRow(grid, c, row++, "Prescription", html(appt.prescription.isEmpty() ? "None" : appt.prescription));
            addRow(grid, c, row++, "Instructions", html(appt.instructions.isEmpty() ? "None" : appt.instructions));
        }
        grid.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(grid);
        card.add(Box.createVerticalStrut(14));

        String text;
        Color color;
        boolean canJoin = false;
        if (appt.isCompleted()) {
            text = "This consultation is already completed.";
            color = Theme.ACCENT;
        } else if (!appt.timeArrived) {
            text = "The scheduled time has not arrived yet. You can join when it is time.";
            color = WARN;
        } else {
            text = "The scheduled time has arrived. You can join the consultation.";
            color = Theme.ACCENT;
            canJoin = true;
        }
        card.add(centered(text, new Font("Arial", Font.BOLD, 15), color));
        card.add(Box.createVerticalStrut(20));

        Theme.PulseButton join = new Theme.PulseButton("Join Consultation", Theme.ACCENT);
        Theme.PulseButton back = new Theme.PulseButton("Back", Theme.NEUTRAL);
        join.setEnabled(canJoin);
        join.addActionListener(e -> showRoom());
        back.addActionListener(e -> showAppointments());
        card.add(buttonRow(join, back));
        setScreen(card);
    }

    private void addRow(JPanel grid, GridBagConstraints c, int row, String label, String value) {
        c.gridy = row;
        c.gridx = 0;
        JLabel l = new JLabel(label + ":");
        l.setFont(new Font("Arial", Font.BOLD, 16));
        l.setForeground(Theme.NEUTRAL);
        grid.add(l, c);
        c.gridx = 1;
        JLabel v = new JLabel(value);
        v.setFont(new Font("Arial", Font.PLAIN, 16));
        grid.add(v, c);
    }

    // ---------------------------------------------------------------- screen 4: consultation room

    private void showRoom() {
        stopTimers();
        patientJoined = false;
        connectionLost = false;
        seconds = 0;

        JPanel card = newCard(20, 24);
        card.add(centered("Online Consultation", new Font("Arial", Font.BOLD, 28), Theme.PRIMARY));
        card.add(Box.createVerticalStrut(4));
        card.add(centered(appt.id + "  |  Patient: " + appt.patientName + " (" + appt.patientId + ")  |  " + appt.time,
            new Font("Arial", Font.PLAIN, 15), Theme.NEUTRAL));
        card.add(Box.createVerticalStrut(14));

        // ---- left: connection status + demo controls
        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setBackground(PANEL_BG);
        left.setBorder(new CompoundBorder(new LineBorder(new Color(200, 215, 225)), new EmptyBorder(14, 14, 14, 14)));
        left.setPreferredSize(new Dimension(300, 520));

        left.add(leftLabel("Connection", new Font("Arial", Font.BOLD, 18), Theme.PRIMARY));
        left.add(Box.createVerticalStrut(10));
        doctorStatus = leftLabel(" ", new Font("Arial", Font.BOLD, 15), Theme.ACCENT);
        patientStatus = leftLabel(" ", new Font("Arial", Font.BOLD, 15), WARN);
        timerLabel = leftLabel("Duration: 00:00", new Font("Arial", Font.PLAIN, 14), Theme.NEUTRAL);
        left.add(doctorStatus);
        left.add(Box.createVerticalStrut(4));
        left.add(patientStatus);
        left.add(Box.createVerticalStrut(4));
        left.add(timerLabel);
        left.add(Box.createVerticalStrut(12));
        messageLabel = leftLabel(" ", new Font("Arial", Font.PLAIN, 13), WARN);
        left.add(messageLabel);
        left.add(Box.createVerticalStrut(10));

        rejoinButton = leftButton("Rejoin Consultation", Theme.PRIMARY);
        rejoinButton.setVisible(false);
        rejoinButton.addActionListener(e -> rejoin());
        left.add(rejoinButton);
        left.add(Box.createVerticalStrut(16));

        left.add(leftLabel("Demo controls", new Font("Arial", Font.BOLD, 14), HINT_GRAY));
        left.add(leftLabel("<html>Simulates the other side of the call.</html>",
            new Font("Arial", Font.PLAIN, 12), HINT_GRAY));
        left.add(Box.createVerticalStrut(6));
        patientJoinsButton = leftButton("Patient Joins", Theme.NEUTRAL);
        failButton = leftButton("Connection Fails", Theme.DANGER);
        patientJoinsButton.addActionListener(e -> {
            patientJoined = true;
            refreshRoom();
        });
        failButton.addActionListener(e -> {
            connectionLost = true;
            refreshRoom();
            JOptionPane.showMessageDialog(this,
                "Connection error: the consultation connection was lost.\nClick \"Rejoin Consultation\" to reconnect.",
                "Connection Error", JOptionPane.ERROR_MESSAGE);
        });
        left.add(patientJoinsButton);
        left.add(Box.createVerticalStrut(6));
        left.add(failButton);
        left.add(Box.createVerticalGlue());

        Theme.PulseButton leave = leftButton("Leave Consultation", Theme.NEUTRAL);
        leave.addActionListener(e -> leaveRoom());
        left.add(leave);

        // ---- right: consultation form
        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setOpaque(false);

        symptomsArea = area(3, appt.bookedSymptoms);
        diagnosisArea = area(4, "");
        prescriptionArea = area(4, "");
        instructionsArea = area(3, "");

        form.add(formLabel("Patient Symptoms *"));
        form.add(wrap(symptomsArea));
        form.add(Box.createVerticalStrut(8));
        form.add(formLabel("Doctor's Diagnosis / Advice *"));
        form.add(wrap(diagnosisArea));
        form.add(Box.createVerticalStrut(8));
        form.add(formLabel("Prescription (if needed)"));
        form.add(wrap(prescriptionArea));
        form.add(Box.createVerticalStrut(8));
        form.add(formLabel("Instructions (if needed)"));
        form.add(wrap(instructionsArea));
        form.add(Box.createVerticalStrut(8));

        formError = new JLabel(" ");
        formError.setFont(new Font("Arial", Font.BOLD, 13));
        formError.setForeground(Theme.DANGER);
        formError.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(formError);
        form.add(Box.createVerticalStrut(6));

        saveButton = new Theme.PulseButton("Save and Complete", Theme.ACCENT);
        saveButton.setFont(new Font("Arial", Font.BOLD, 16));
        saveButton.setBorder(new EmptyBorder(11, 28, 11, 28));
        saveButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        saveButton.addActionListener(e -> saveAndComplete());
        form.add(saveButton);

        JPanel body = new JPanel(new BorderLayout(18, 0));
        body.setOpaque(false);
        body.add(left, BorderLayout.WEST);
        body.add(form, BorderLayout.CENTER);
        body.setPreferredSize(new Dimension(900, 540));
        body.setMaximumSize(new Dimension(900, 540));
        body.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(body);

        refreshRoom();
        setScreen(card);

        // The patient joins by himself after a few seconds (or use the demo button).
        joinTimer = new Timer(6000, e -> {
            if (!patientJoined) {
                patientJoined = true;
                refreshRoom();
            }
        });
        joinTimer.setRepeats(false);
        joinTimer.start();
        clockTimer = new Timer(1000, e -> {
            if (patientJoined && !connectionLost) {
                seconds++;
                timerLabel.setText(String.format("Duration: %02d:%02d", seconds / 60, seconds % 60));
            }
        });
        clockTimer.start();
    }

    private JLabel leftLabel(String text, Font font, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(font);
        l.setForeground(color);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private Theme.PulseButton leftButton(String text, Color color) {
        Theme.PulseButton b = new Theme.PulseButton(text, color);
        b.setFont(new Font("Arial", Font.BOLD, 14));
        b.setBorder(new EmptyBorder(9, 12, 9, 12));
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        return b;
    }

    private JLabel formLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Arial", Font.BOLD, 15));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        l.setBorder(new EmptyBorder(0, 0, 3, 0));
        return l;
    }

    private JTextArea area(int rows, String text) {
        JTextArea a = new JTextArea(text, rows, 30);
        a.setFont(new Font("Arial", Font.PLAIN, 15));
        a.setLineWrap(true);
        a.setWrapStyleWord(true);
        a.setBorder(new EmptyBorder(5, 8, 5, 8));
        return a;
    }

    private JScrollPane wrap(JTextArea a) {
        JScrollPane s = new JScrollPane(a);
        s.setBorder(new LineBorder(BORDER_GRAY));
        s.setAlignmentX(Component.LEFT_ALIGNMENT);
        return s;
    }

    // Updates the status labels and enables the form only while both sides are connected.
    private void refreshRoom() {
        boolean both = patientJoined && !connectionLost;

        if (connectionLost) {
            doctorStatus.setText("\u25CF Doctor: Connection lost");
            doctorStatus.setForeground(Theme.DANGER);
            patientStatus.setText("\u25CF Patient: Disconnected");
            patientStatus.setForeground(Theme.DANGER);
            messageLabel.setText("<html>Connection error. The consultation is paused. Click Rejoin to reconnect. "
                + "If you leave now, the consultation stays incomplete.</html>");
            messageLabel.setForeground(Theme.DANGER);
        } else if (!patientJoined) {
            doctorStatus.setText("\u25CF Doctor: Connected");
            doctorStatus.setForeground(Theme.ACCENT);
            patientStatus.setText("\u25CF Patient: Not joined yet");
            patientStatus.setForeground(WARN);
            messageLabel.setText("<html>The patient has not joined the consultation. You can wait for the "
                + "patient and continue when they join.</html>");
            messageLabel.setForeground(WARN);
        } else {
            doctorStatus.setText("\u25CF Doctor: Connected");
            doctorStatus.setForeground(Theme.ACCENT);
            patientStatus.setText("\u25CF Patient: Connected");
            patientStatus.setForeground(Theme.ACCENT);
            messageLabel.setText("<html>Both have joined. Discuss the symptoms, then enter the diagnosis.</html>");
            messageLabel.setForeground(Theme.ACCENT);
        }

        rejoinButton.setVisible(connectionLost);
        rejoinButton.setEnabled(true);
        rejoinButton.setText("Rejoin Consultation");
        patientJoinsButton.setEnabled(!patientJoined);
        failButton.setEnabled(both);

        for (JTextArea a : new JTextArea[]{symptomsArea, diagnosisArea, prescriptionArea, instructionsArea}) {
            a.setEditable(both);
            a.setBackground(both ? Color.WHITE : new Color(238, 241, 243));
        }
        saveButton.setEnabled(both);
        if (!both) formError.setText("The form is available once the patient has joined and the connection is active.");
        else if (formError.getText().startsWith("The form")) formError.setText(" ");
    }

    private void rejoin() {
        rejoinButton.setEnabled(false);
        rejoinButton.setText("Reconnecting...");
        rejoinTimer = new Timer(1500, e -> {
            connectionLost = false;
            refreshRoom();
        });
        rejoinTimer.setRepeats(false);
        rejoinTimer.start();
    }

    private void leaveRoom() {
        int choice = JOptionPane.showConfirmDialog(this,
            "Leave the consultation without completing it?\nIt will not be marked as completed.",
            "Leave Consultation", JOptionPane.YES_NO_OPTION);
        if (choice != JOptionPane.YES_OPTION) return;
        stopTimers();
        if (connectionLost) {            // could not reconnect -> consultation remains incomplete
            appt.status = "Incomplete";
            ConsultationStore.save(appt);
        }
        showDetails();
    }

    // ---------------------------------------------------------------- save and complete

    private void saveAndComplete() {
        String symptoms = symptomsArea.getText().trim();
        String diagnosis = diagnosisArea.getText().trim();
        String prescription = prescriptionArea.getText().trim();
        String instructions = instructionsArea.getText().trim();

        String error = null;
        if (symptoms.isEmpty()) error = "Patient symptoms are required.";
        else if (diagnosis.isEmpty()) error = "The doctor's diagnosis or advice is required.";
        else if (diagnosis.length() < 5) error = "Please enter a meaningful diagnosis (at least 5 characters).";
        else if (prescription.isEmpty() && !instructions.isEmpty())
            error = "Enter the prescription for these instructions, or clear the instructions.";
        else if (symptoms.length() > 1000 || diagnosis.length() > 1000
            || prescription.length() > 1000 || instructions.length() > 1000)
            error = "Each field can have at most 1000 characters.";
        if (error != null) {
            formError.setText(error);
            return;
        }
        formError.setText(" ");

        // the doctor reviews the details before saving
        String review = "Please review the consultation:\n\n"
            + "Symptoms: " + symptoms + "\n\n"
            + "Diagnosis / advice: " + diagnosis + "\n\n"
            + "Prescription: " + (prescription.isEmpty() ? "None" : prescription) + "\n\n"
            + "Instructions: " + (instructions.isEmpty() ? "None" : instructions) + "\n\n"
            + "Save and mark this consultation as completed?";
        JTextArea reviewArea = new JTextArea(review, 14, 38);
        reviewArea.setLineWrap(true);
        reviewArea.setWrapStyleWord(true);
        reviewArea.setEditable(false);
        reviewArea.setFont(new Font("Arial", Font.PLAIN, 14));
        int choice = JOptionPane.showConfirmDialog(this, new JScrollPane(reviewArea),
            "Review Consultation", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) return;

        String oldStatus = appt.status;
        appt.symptoms = symptoms;
        appt.diagnosis = diagnosis;
        appt.prescription = prescription;
        appt.instructions = instructions;
        appt.status = "Completed";
        appt.completedOn = now();
        if (!ConsultationStore.save(appt)) {
            appt.status = oldStatus;
            appt.completedOn = "";
            formError.setText("Could not save the consultation file. Check that the folder is writable.");
            return;
        }
        stopTimers();
        JOptionPane.showMessageDialog(this,
            "Consultation saved and marked as completed."
                + (prescription.isEmpty() ? "" : "\nThe prescription is now available to the patient."),
            "Consultation Completed", JOptionPane.INFORMATION_MESSAGE);
        showAppointments();
    }

    // ---------------------------------------------------------------- patient: view prescriptions

    private void showPatientPrescriptions() {
        stopTimers();
        JPanel card = newCard(28, 40);
        card.add(centered("My Prescriptions", new Font("Arial", Font.BOLD, 32), Theme.PRIMARY));
        card.add(Box.createVerticalStrut(6));
        card.add(centered("Patients: prescriptions issued after a completed online consultation",
            new Font("Arial", Font.PLAIN, 16), Theme.NEUTRAL));
        card.add(Box.createVerticalStrut(16));

        JTextField idField = new JTextField(10);
        idField.setFont(new Font("Arial", Font.PLAIN, 16));
        idField.setBorder(new CompoundBorder(new LineBorder(BORDER_GRAY), new EmptyBorder(5, 8, 5, 8)));
        Theme.PulseButton show = new Theme.PulseButton("Show", Theme.PRIMARY);
        JPanel top = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        top.setOpaque(false);
        JLabel idLabel = new JLabel("Patient ID:");
        idLabel.setFont(new Font("Arial", Font.BOLD, 16));
        top.add(idLabel);
        top.add(idField);
        top.add(show);
        top.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(top);
        card.add(Box.createVerticalStrut(6));
        JLabel msg = centered("Sample patient IDs: P1001, P1002, P1004 (a prescription appears after the doctor completes a consultation)",
            new Font("Arial", Font.PLAIN, 12), HINT_GRAY);
        card.add(msg);
        card.add(Box.createVerticalStrut(12));

        DefaultTableModel model = readOnlyModel("Appointment ID", "Doctor", "Date & Time", "Diagnosis");
        JTable table = styledTable(model);
        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(new LineBorder(BORDER_GRAY));
        sp.setPreferredSize(new Dimension(800, 150));
        sp.setMaximumSize(new Dimension(800, 150));
        sp.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(sp);
        card.add(Box.createVerticalStrut(10));

        JTextArea details = new JTextArea(8, 40);
        details.setEditable(false);
        details.setLineWrap(true);
        details.setWrapStyleWord(true);
        details.setFont(new Font("Arial", Font.PLAIN, 15));
        details.setBorder(new EmptyBorder(8, 10, 8, 10));
        JScrollPane dsp = new JScrollPane(details);
        dsp.setBorder(new LineBorder(BORDER_GRAY));
        dsp.setPreferredSize(new Dimension(800, 170));
        dsp.setMaximumSize(new Dimension(800, 170));
        dsp.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(dsp);
        card.add(Box.createVerticalStrut(16));

        List<ConsultationStore.Appointment> current = new ArrayList<>();
        Runnable lookup = () -> {
            String id = idField.getText().trim().toUpperCase();
            model.setRowCount(0);
            current.clear();
            details.setText("");
            if (!id.matches("P\\d{3,6}")) {
                msg.setText("Enter a valid patient ID (P followed by digits, e.g. P1001).");
                msg.setForeground(Theme.DANGER);
                return;
            }
            current.addAll(ConsultationStore.completedForPatient(id));
            for (ConsultationStore.Appointment a : current)
                model.addRow(new Object[]{a.id, ConsultationStore.doctorTitle(a.doctorId), a.completedOn, a.diagnosis});
            msg.setText(current.isEmpty() ? "No completed consultations found for " + id + "."
                : "Select a consultation to view the prescription.");
            msg.setForeground(current.isEmpty() ? WARN : HINT_GRAY);
        };
        show.addActionListener(e -> lookup.run());
        idField.addActionListener(e -> lookup.run());
        table.getSelectionModel().addListSelectionListener(e -> {
            int row = table.getSelectedRow();
            if (e.getValueIsAdjusting() || row < 0 || row >= current.size()) return;
            ConsultationStore.Appointment a = current.get(row);
            details.setText("Doctor: " + ConsultationStore.doctorTitle(a.doctorId)
                + "\nDate: " + a.completedOn
                + "\nSymptoms: " + a.symptoms
                + "\nDiagnosis / advice: " + a.diagnosis
                + "\n\nPrescription:\n" + (a.prescription.isEmpty() ? "None" : a.prescription)
                + "\n\nInstructions:\n" + (a.instructions.isEmpty() ? "None" : a.instructions));
            details.setCaretPosition(0);
        });

        Theme.PulseButton back = new Theme.PulseButton("Back", Theme.NEUTRAL);
        back.addActionListener(e -> showStart());
        card.add(buttonRow(back));
        setScreen(card);
    }
}
