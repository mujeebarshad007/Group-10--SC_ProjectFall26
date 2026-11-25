PULSE - Search for Doctors + Manage Profile (all files in one folder)

Files:
  Main.java            - entry point
  MainFrame.java       - home screen (Search for Doctors / Manage Profile / Exit)
  Signup.java          - doctor search screen (use case 1)
  ProfileFrame.java    - Manage Doctor Profile + Manage Patient Profile (use cases 2 and 3)
  ProfileStore.java    - saves profiles to pulse_profiles.properties
  OnlineDiagnosisFrame.java - Online Diagnosis (use case 4)
  ConsultationStore.java - sample online appointments + saved consultations
  Doctor.java, DoctorDatabase.java, Theme.java - shared data and look

Manage Profile test IDs:
  Doctor ID : D101 ... D160 (existing sample doctor, details are pre-filled)
              or any new ID such as D500 (empty form = alternate scenario)
  Patient ID: P1001 (any new ID starts with an empty form)

Online Diagnosis test IDs:
  Doctor ID : D101, D107, D112 (online appointments). Patient: P1001, P1002, P1004 for prescriptions.

Run in VS Code:
  1. File > Open Folder... and choose THIS folder.
  2. Open Main.java and click Run.

Run from terminal (needs JDK):
  Linux/Mac:  sh run.sh
  Windows:    run.bat
