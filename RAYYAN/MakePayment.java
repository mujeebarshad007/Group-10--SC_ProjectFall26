import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

// UC 05: Make Payment (Patient)
// Demo payment screen: it validates the entered information and records a simulated payment.
public class MakePayment extends JFrame {
    private static final int CONSULTATION_FEE = 2500;
    private final DefaultTableModel appointmentModel = new DefaultTableModel(
            new String[]{"Patient Name", "Doctor", "Date", "Time", "Status"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable appointmentTable = new JTable(appointmentModel);
    private final JComboBox<String> methodBox = new JComboBox<>(new String[]{
            "Cash", "JazzCash", "Easypaisa", "SadaPay", "Bank Transfer", "Debit/Credit Card"
    });
    private final JTextField accountField = new JTextField(22);
    private final JTextField referenceField = new JTextField(22);
    private final JLabel amountLabel = new JLabel("Rs. " + CONSULTATION_FEE);
    private final JLabel patientValue = valueLabel("Not selected");
    private final JLabel doctorValue = valueLabel("Not selected");
    private final JLabel appointmentValue = valueLabel("Not selected");
    private final JLabel detailsHint = new JLabel("Cash payment will be marked as paid at the clinic.");
    private final JLabel statusLabel = new JLabel("Select a confirmed appointment to continue.", SwingConstants.CENTER);
    private final List<String[]> appointments = new ArrayList<>();
    private File appointmentFolder;
    private String patientName, doctorName, appointmentDate, appointmentTime;

    public MakePayment() {
        setTitle("PULSE - Make Payment");
        setSize(1050, 720);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        add(Theme.banner("Make Payment", "Select your confirmed appointment, choose a payment method and receive your receipt"), BorderLayout.NORTH);

        JPanel main = new JPanel(new BorderLayout(14, 14));
        main.setBackground(Theme.BG);
        main.setBorder(new EmptyBorder(16, 18, 8, 18));

        JPanel appointmentCard = new JPanel(new BorderLayout(8, 8));
        appointmentCard.setBackground(Color.WHITE);
        appointmentCard.setBorder(new CompoundBorder(new LineBorder(new Color(200, 215, 225)), new EmptyBorder(12, 14, 12, 14)));
        JLabel appointmentHeading = new JLabel("Confirmed Appointments");
        appointmentHeading.setFont(Theme.HEADING);
        appointmentHeading.setForeground(Theme.PRIMARY);
        appointmentCard.add(appointmentHeading, BorderLayout.NORTH);
        appointmentTable.setRowHeight(27);
        appointmentTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        appointmentTable.getTableHeader().setReorderingAllowed(false);
        appointmentTable.getTableHeader().setBackground(Theme.PRIMARY);
        appointmentTable.getTableHeader().setForeground(Color.WHITE);
        appointmentTable.getTableHeader().setFont(Theme.BOLD);
        appointmentTable.setSelectionBackground(new Color(190, 225, 240));
        appointmentCard.add(new JScrollPane(appointmentTable), BorderLayout.CENTER);
        Theme.PulseButton loadButton = new Theme.PulseButton("Load Appointments", Theme.PRIMARY);
        JPanel loadPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        loadPanel.setOpaque(false);
        loadPanel.add(loadButton);
        appointmentCard.add(loadPanel, BorderLayout.SOUTH);

        JPanel paymentCard = new JPanel(new GridBagLayout());
        paymentCard.setBackground(Color.WHITE);
        paymentCard.setBorder(new CompoundBorder(new LineBorder(new Color(200, 215, 225)), new EmptyBorder(15, 18, 15, 18)));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(9, 8, 9, 8);
        g.fill = GridBagConstraints.HORIZONTAL;
        g.weightx = 1;
        JLabel paymentHeading = new JLabel("Payment Information");
        paymentHeading.setFont(Theme.HEADING);
        paymentHeading.setForeground(Theme.PRIMARY);
        g.gridx = 0; g.gridy = 0; g.gridwidth = 2;
        paymentCard.add(paymentHeading, g);
        g.gridwidth = 1;
        addRow(paymentCard, g, 1, "Selected Patient", patientValue);
        addRow(paymentCard, g, 2, "Doctor", doctorValue);
        addRow(paymentCard, g, 3, "Appointment", appointmentValue);
        amountLabel.setFont(new Font("Arial", Font.BOLD, 19));
        amountLabel.setForeground(Theme.ACCENT);
        addRow(paymentCard, g, 4, "Required Amount", amountLabel);
        methodBox.setFont(Theme.BODY);
        addRow(paymentCard, g, 5, "Payment Method", methodBox);
        accountField.setFont(Theme.BODY);
        addRow(paymentCard, g, 6, "Mobile / Account", accountField);
        referenceField.setFont(Theme.BODY);
        addRow(paymentCard, g, 7, "Transaction Reference", referenceField);
        detailsHint.setFont(Theme.BODY);
        detailsHint.setForeground(Theme.NEUTRAL);
        g.gridx = 0; g.gridy = 8; g.gridwidth = 2;
        paymentCard.add(detailsHint, g);
        JLabel securityNote = new JLabel("Just for Understandoing (Demo)");
        securityNote.setFont(new Font("Arial", Font.ITALIC, 12));
        securityNote.setForeground(Theme.NEUTRAL);
        g.gridy = 9;
        paymentCard.add(securityNote, g);

        JPanel center = new JPanel(new GridLayout(1, 2, 14, 0));
        center.setOpaque(false);
        center.add(appointmentCard);
        center.add(paymentCard);
        main.add(center, BorderLayout.CENTER);
        add(main, BorderLayout.CENTER);

        Theme.PulseButton payButton = new Theme.PulseButton("Confirm Payment", Theme.ACCENT);
        Theme.PulseButton clearButton = new Theme.PulseButton("Clear", Theme.NEUTRAL);
        Theme.PulseButton backButton = new Theme.PulseButton("Back", Theme.NEUTRAL);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 5));
        buttons.setOpaque(false);
        buttons.add(payButton); buttons.add(clearButton); buttons.add(backButton);
        statusLabel.setFont(Theme.BODY);
        statusLabel.setForeground(Theme.PRIMARY);
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setBackground(Theme.BG);
        bottom.setBorder(new EmptyBorder(4, 0, 12, 0));
        bottom.add(statusLabel, BorderLayout.NORTH);
        bottom.add(buttons, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);

        methodBox.addActionListener(e -> updateMethodFields());
        loadButton.addActionListener(e -> loadAppointments());
        payButton.addActionListener(e -> confirmPayment());
        clearButton.addActionListener(e -> { accountField.setText(""); referenceField.setText(""); appointmentTable.clearSelection(); statusLabel.setText("Form cleared."); });
        backButton.addActionListener(e -> { new MainFrame(); dispose(); });
        appointmentTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) showSelectedAppointment();
        });
        updateMethodFields();
        setVisible(true);
    }

    private JLabel valueLabel(String value) {
        JLabel label = new JLabel(value);
        label.setFont(Theme.BODY);
        label.setForeground(Theme.PRIMARY);
        return label;
    }

    private void addRow(JPanel panel, GridBagConstraints g, int row, String title, Component component) {
        JLabel label = new JLabel(title);
        label.setFont(Theme.BOLD);
        g.gridx = 0; g.gridy = row; g.weightx = 0.35;
        panel.add(label, g);
        g.gridx = 1; g.weightx = 0.65;
        panel.add(component, g);
    }

    private void loadAppointments() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select the folder containing Appointments.txt");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File file = new File(chooser.getSelectedFile(), "Appointments.txt");
        if (!file.isFile()) {
            JOptionPane.showMessageDialog(this, "Appointments.txt was not found in the selected folder.", "File Not Found", JOptionPane.WARNING_MESSAGE);
            return;
        }
        appointmentFolder = chooser.getSelectedFile();
        appointments.clear();
        appointmentModel.setRowCount(0);
        try {
            List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
            String patient = "", doctor = "", date = "", time = "", status = "";
            for (String line : lines) {
                if (line.startsWith("Patient Name: ")) patient = line.substring("Patient Name: ".length()).trim();
                else if (line.startsWith("Doctor: ")) doctor = line.substring("Doctor: ".length()).trim();
                else if (line.startsWith("Date: ")) date = line.substring("Date: ".length()).trim();
                else if (line.startsWith("Time: ")) time = line.substring("Time: ".length()).trim();
                else if (line.startsWith("Status: ")) status = line.substring("Status: ".length()).trim();
                else if (line.startsWith("---")) {
                    if ("Confirmed".equalsIgnoreCase(status)) {
                        String[] record = {patient, doctor, date, time, status};
                        appointments.add(record);
                        appointmentModel.addRow(record);
                    }
                    patient = doctor = date = time = status = "";
                }
            }
            statusLabel.setText(appointments.size() + " confirmed appointment(s) loaded.");
            if (appointments.isEmpty()) JOptionPane.showMessageDialog(this, "No confirmed appointments were found in this file.");
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Could not read appointments: " + ex.getMessage(), "Read Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showSelectedAppointment() {
        int row = appointmentTable.getSelectedRow();
        if (row < 0) return;
        String[] a = appointments.get(appointmentTable.convertRowIndexToModel(row));
        patientName = a[0]; doctorName = a[1]; appointmentDate = a[2]; appointmentTime = a[3];
        patientValue.setText(patientName);
        doctorValue.setText(doctorName);
        appointmentValue.setText(appointmentDate + " at " + appointmentTime);
        statusLabel.setText("Selected appointment for " + patientName + ". Required amount: Rs. " + CONSULTATION_FEE);
    }

    private void updateMethodFields() {
        String method = (String) methodBox.getSelectedItem();
        boolean cash = "Cash".equals(method);
        accountField.setEnabled(!cash);
        referenceField.setEnabled(!cash);
        accountField.setText("");
        referenceField.setText("");
        if (cash) detailsHint.setText("Cash payment will be marked as paid at the clinic.");
        else if ("JazzCash".equals(method) || "Easypaisa".equals(method)) detailsHint.setText("Enter your 11-digit mobile wallet number and transaction reference.");
        else if ("SadaPay".equals(method)) detailsHint.setText("Enter your SadaPay registered mobile/account number and transaction reference.");
        else if ("Bank Transfer".equals(method)) detailsHint.setText("Enter your bank account title/number and transaction reference.");
        else detailsHint.setText("Demo only: enter a card/account reference");
    }

    private void confirmPayment() {
        if (appointmentFolder == null || appointmentTable.getSelectedRow() < 0) {
            JOptionPane.showMessageDialog(this, "Please load and select a confirmed appointment first.", "Appointment Required", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String method = (String) methodBox.getSelectedItem();
        String account = accountField.getText().trim();
        String reference = referenceField.getText().trim();
        if (!"Cash".equals(method)) {
            if (account.isEmpty() || reference.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter the required payment information.", "Invalid Information", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (("JazzCash".equals(method) || "Easypaisa".equals(method)) && !account.matches("03\\d{9}")) {
                JOptionPane.showMessageDialog(this, "Please enter a valid 11-digit mobile number starting with 03.", "Invalid Mobile Number", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (reference.length() < 4) {
                JOptionPane.showMessageDialog(this, "Please enter a valid transaction reference (at least 4 characters).", "Invalid Reference", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }
        int confirm = JOptionPane.showConfirmDialog(this,
                "Confirm payment of Rs. " + CONSULTATION_FEE + " using " + method + "?",
                "Confirm Payment", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        String receiptNo = "PULSE-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        String paidAt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy hh:mm a"));
        String status = "Successful (Demo)";
        String receipt = "PULSE HEALTHCARE - PAYMENT RECEIPT\n"
                + "====================================\n"
                + "Receipt No: " + receiptNo + "\n"
                + "Patient: " + patientName + "\n"
                + "Doctor: " + doctorName + "\n"
                + "Appointment Date: " + appointmentDate + "\n"
                + "Appointment Time: " + appointmentTime + "\n"
                + "Payment Method: " + method + "\n"
                + "Account / Mobile: " + ("Cash".equals(method) ? "N/A" : account) + "\n"
                + "Transaction Reference: " + ("Cash".equals(method) ? "N/A" : reference) + "\n"
                + "Amount: Rs. " + CONSULTATION_FEE + "\n"
                + "Payment Status: " + status + "\n"
                + "Date & Time: " + paidAt + "\n"
                + "====================================\n"
                + "Thank you for choosing PULSE.\n"
                + "Note: This is a system-generated demo receipt.\n";
        File receiptFile = new File(appointmentFolder, "Receipt_" + receiptNo + ".txt");
        File paymentFile = new File(appointmentFolder, "Payments.txt");
        try {
            Files.write(paymentFile.toPath(), (receipt + "\n------------------------------\n").getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            Files.write(receiptFile.toPath(), receipt.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE_NEW);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Payment could not be saved: " + ex.getMessage(), "Save Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        statusLabel.setText("Payment record saved. Receipt: " + receiptFile.getAbsolutePath());
        JTextArea receiptArea = new JTextArea(receipt, 19, 45);
        receiptArea.setEditable(false);
        receiptArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        JOptionPane.showMessageDialog(this, new JScrollPane(receiptArea), "Payment Receipt - " + status, JOptionPane.INFORMATION_MESSAGE);
        int open = JOptionPane.showConfirmDialog(this, "Receipt saved successfully.\n\n" + receiptFile.getAbsolutePath() + "\n\nOpen the receipt file now?", "Receipt Saved", JOptionPane.YES_NO_OPTION);
        if (open == JOptionPane.YES_OPTION) {
            try { Desktop.getDesktop().open(receiptFile); }
            catch (Exception ex) { JOptionPane.showMessageDialog(this, "Receipt is saved at:\n" + receiptFile.getAbsolutePath()); }
        }
    }
}
