# Hospital Emergency Priority System (HEPS)

**Mini Project Report — Bachelor of Technology in Computer Science & Engineering**  
**School of Computing, Department of Computer Science & Engineering**  
**Kalasalingam Academy of Research and Education (Deemed to be University)**  
*Krishnankoil, Tamil Nadu, India — November 2026*  

**Course:** Object Oriented Programming using Java (CSE203) — Academic Year 2026–2027 (Odd Semester)  

---

## 👥 Project Team & Supervision

| Register Number | Student Name | Role / Contribution |
| :--- | :--- | :--- |
| **99250040297** | **Jamal Khan I** | Project Lead, Queue & Architecture Implementation |
| **99250040005** | **Ajmal Khan I** | Core JavaFX UI & Screen Controllers |
| **99250040307** | **Jeeva Darwin M** | MySQL Database, DAO Layer & JDBC Transactions |
| **99250040290** | **Harish S** | Triage Rule Engine, Safety Checks & NEWS2 Service |
| **99250040098** | **Beteeshia John** | Automated JUnit 5 Testing & Integration Verification |

* **Faculty Guide:** Ms. S. Shanmugapriya, Assistant Professor, Dept. of CSE  
* **Head of Department:** Dr. R. Raja Subramanian, M.E., Ph.D., Professor & Head, Dept. of CSE  

---

## 📋 Abstract

Hospital emergency rooms are busy environments where every minute matters. Most queues encountered in daily life, such as bank counters or ticket windows, serve individuals on a **First-Come, First-Served (FIFO)** basis. In an emergency room, this rule is clinically unsafe because a patient arriving later with a life-threatening illness must not wait behind patients with minor complaints.

The **Hospital Emergency Priority System (HEPS)** is a desktop application developed in Java to solve this critical healthcare challenge. It registers patients at reception, collects vital physiological measurements (pulse, blood oxygen saturation, blood pressure, respiratory rate, and temperature), and automatically categorizes urgency. 

Waiting patients are managed dynamically using an in-memory **Priority Queue** implemented as a binary min-heap (`PriorityQueue<Patient>`), ensuring the most critically ill patient is always positioned at the head of the queue. Patients presenting with equal urgency are scheduled fairly by arrival sequence. To protect patients with lower urgency from being neglected during peak arrival periods, a background anti-starvation thread continuously evaluates waiting durations and promotes patients automatically.

HEPS also coordinates the complete emergency visit:
* Allocates one of 12 dedicated treatment bays across 4 specialized zones with live occupancy and cleaning timers.
* Suggests suitable on-duty physicians matching medical specialties and workload.
* Records diagnostic laboratory/imaging orders and alerts on abnormal results.
* Captures structured clinical treatment SOAP notes.
* Prints an instant triage ticket with barcodes and 2D QR codes (ZXing).
* Generates a tamper-proof PDF clinical discharge summary (Apache PDFBox).
* Enforces Role-Based Access Control (RBAC) with salted password hashing (jBCrypt) and an immutable audit log.
* Backed by persistent relational storage via MySQL 8 and JDBC PreparedStatements.
* Verified by **37 automated JUnit 5 test cases** ensuring complete clinical and architectural reliability.

---

## 📌 Problem Statement & Clinical Motivation

### Limitations of the Existing Manual System:
In many emergency departments, triage and patient flow are handled manually through notebooks, paper slips, and dry-erase whiteboards:
1. **Paper slips and notebooks:** Prone to physical damage, misplacement, or illegible handwriting.
2. **First-Come, First-Served Bias:** Seriously ill patients wait behind non-urgent arrivals if token numbers are assigned sequentially.
3. **Absence of Serial Re-assessment:** Waiting patients are rarely re-evaluated systematically; deteriorating conditions are easily missed.
4. **Lack of Digital Audit Trail:** Inability to accurately record exact timestamps for arrival, triage assessment, doctor consultation, and discharge.
5. **Opaque Departmental Visibility:** Bed occupancy, cleaning status, and doctor workloads are not globally synchronized across staff.

### Comparison: Existing System vs. Proposed System (HEPS)

| Parameter | Existing (Manual) System | Proposed System (HEPS) |
| :--- | :--- | :--- |
| **Treatment Order** | Order of arrival (FIFO) | Clinical urgency first, arrival time second |
| **Waiting Patients** | Not checked again systematically | Background scan every 15 seconds; auto-promoted if delayed |
| **Record Storage** | Paper registers and manual slips | Persistent MySQL relational database |
| **Bed Allotment** | Written on whiteboards | Live visual board of 12 bays with 15-minute cleaning timers |
| **Tests & Notes** | Handwritten slips (easily lost) | Digital entry on workstation with timestamps |
| **Clinical Safety** | Relies entirely on individual memory | Safety rules + NEWS2 early warning score evaluate every patient |
| **Documents** | Hand-written physical receipts | Auto-printed barcode/QR tickets and generated PDF summaries |

---

## 🎯 Clinical Urgency Levels & Target Times

HEPS classifies clinical arrivals into four distinct urgency tiers with predefined service level agreement (SLA) target times:

| Urgency Level | Clinical Meaning | Clinical Examples | Target Waiting Time |
| :--- | :--- | :--- | :--- |
| **Critical (Level 1)** | Immediate life threat | Cardiac arrest, acute STEMI, respiratory arrest, severe trauma, SpO2 < 90% | Immediate (0 minutes) |
| **High (Level 2)** | Serious, urgent care | Bone fractures, acute asthma, high fever with weakness, systolic BP > 200 | Within 15 minutes |
| **Medium (Level 3)**| Stable, needs treatment | Sprains, lacerations requiring suturing, abdominal pain | Within 60 minutes |
| **Low (Level 4)** | Minor, non-urgent problem| Mild cold, superficial abrasions, minor skin rash | Within 120 minutes |

---

## 🛡️ Anti-Starvation Protection (Waiting-Time Check)

A common flaw in pure priority queuing is **starvation**, where lower-priority patients wait indefinitely if urgent patients arrive continuously. HEPS resolves this through an automated anti-starvation algorithm executed every 15 seconds by a dedicated background daemon:
* A **Medium (Level 3)** patient who has waited for more than **45 minutes** is promoted to **High (Level 2)** and flagged as `escalated`.
* A **Low (Level 4)** patient who has waited for more than **90 minutes** is promoted to **Medium (Level 3)** and flagged as `escalated`.
* When any patient is promoted, the queue is rebuilt and re-sorted automatically, guaranteeing fair progress through the queue.

---

## 🧩 System Architecture & Modules

HEPS is organized into a **4-Layer Clean Architecture**:
1. **Presentation Layer (JavaFX & FXML):** 16 responsive screens styled with a centralized CSS stylesheet and controlled by dedicated Java controller classes.
2. **Business Logic Layer (Services):** Core algorithms including the binary min-heap priority queue, NEWS2 calculation, safety rules, patient state machine, doctor routing, and bay management.
3. **Data Access Layer (DAO):** Decoupled interfaces (`PatientDao`, `DoctorDao`, `TreatmentRecordDao`, `OrderDao`) abstracting SQL operations via JDBC `PreparedStatements`.
4. **Database Layer (MySQL):** Relational tables maintaining patient records, treatment encounters, orders, bed statuses, staff accounts, and audit trails.

### The 14 System Modules:

1. **Login & Role-Based Access Control:** Secure user authentication with BCrypt hashing and role-based screen navigation.
2. **Command Center:** Executive overview displaying total arrivals, active waiting queue, bed occupancy, and alert summaries.
3. **Patient Registration Wizard:** Multi-step intake collecting demographic identity, arrival mode, and chief complaint.
4. **Emergency Priority Queue:** Live display of prioritized patients showing wait times, assigned bays, and escalation flags.
5. **Bay Allocation Board:** Live tracking of 12 emergency bays across Resus, Acute, and Sub-Acute zones with occupancy and cleaning timers.
6. **Doctor Workspace:** Integrated physician station displaying the 5-step circular journey stepper, vitals trend chart, latest vitals grid, and active orders.
7. **Orders & Lab Results:** Diagnostic order entry (STAT lab, radiology, procedures) with flags for abnormal indicators.
8. **Clinical SOAP Notes:** Structured clinical encounter documentation (Subjective, Objective, Assessment, Plan) with live character counters.
9. **Disposition & Discharge:** Bed disposition recording, clinical outcome capture, and automated PDF discharge summary creation.
10. **Patient Records & History:** Searchable archive of prior patient visits, medical history, and discharge notes.
11. **Clinical Alerts Center:** Real-time alert notifications for critical arrivals, physiological deterioration, and SLA delays.
12. **Reports & Analytics:** Acuity distribution pie charts, hourly volume bar charts with time axes, and SLA compliance benchmark metrics.
13. **Admin Console:** User account management, role assignment, active staff switches, and chronological audit logs.
14. **Triage Ticket & Barcode:** Instant generation of printable triage ticket cards containing barcodes and 2D QR codes.

### User Roles & Responsibilities:

| User Role | Primary Work & Permissions |
| :--- | :--- |
| **Receptionist** | Patient registration, demographic updates, ticket printing, and record lookup. |
| **Triage Nurse** | Vital signs entry, urgency confirmation, safety rule evaluation, and bay assignment. |
| **Doctor** | Clinical examination, vitals review, SOAP notes, diagnostic orders, and final disposition. |
| **Charge Nurse** | Bay board management, bed cleaning supervision, alerts monitoring, and mass casualty activation. |
| **Administrator** | Staff account provisioning, system configuration, performance reports, and security audit inspection. |

---

## ☕ Object-Oriented Programming (OOP) & Design Patterns

The system demonstrates practical application of fundamental OOP principles and software design patterns:

### Core OOP Principles:
* **Encapsulation:** Domain entities (`Patient`, `Vitals`, `Doctor`, `Bay`) keep fields strictly private with validated getters and setters to reject invalid physiological inputs (e.g., negative ages or impossible heart rates).
* **Inheritance:** Custom exceptions (`InvalidStateTransitionException`, `InvalidPatientDataException`, `DatabaseOperationException`) extend standard Java exception hierarchies.
* **Polymorphism:** The `PatientPriorityComparator` implements `Comparator<Patient>`, allowing `java.util.PriorityQueue` to sort patient instances polymorphically without knowing internal details.
* **Abstraction:** Services interact exclusively with DAO interfaces (`PatientDao`, `TreatmentRecordDao`), completely hiding JDBC SQL queries and database connectivity mechanics.

### Design Patterns Applied:

| Design Pattern | Where It Is Used | Architectural Benefit |
| :--- | :--- | :--- |
| **Singleton** | `QueueService`, `BayService`, `UserSession`, `DBConnection` | Ensures a single shared instance manages queue states and database pools globally. |
| **State Pattern** | `PatientStateMachine` | Enforces valid status progressions and blocks illegal state jumps. |
| **Strategy Pattern** | `PatientPriorityComparator` | Decouples the 3-tier prioritization algorithm from the queue container. |
| **Data Access Object (DAO)** | `PatientDaoImpl`, `DoctorDaoImpl`, `TreatmentRecordDaoImpl` | Isolates SQL persistence code from application business rules. |
| **Observer Pattern** | JavaFX `ObservableList` and UI TableViews | Automatic UI synchronization whenever underlying collection items change. |

---

## 📊 Data Structures & Algorithms

### In-Memory Min-Heap (`PriorityQueue<Patient>`):
* Implemented using `java.util.PriorityQueue<Patient>`.
* Guarantees $O(1)$ time complexity for inspecting the next patient (`peek()`), and $O(\log n)$ for adding (`offer()`) or serving (`poll()`) a patient.

### 3-Tier Priority Comparator (`PatientPriorityComparator`):
Triage order is determined by three evaluation rules applied in strict sequence:
1. **Rule 1 (Urgency Rank):** Patients with a lower rank number (1 = Critical, 2 = High, 3 = Medium, 4 = Low) are served first.
2. **Rule 2 (Anti-Starvation Escalation):** If two patients share the same urgency tier, a patient promoted by the waiting-time check is placed ahead.
3. **Rule 3 (Arrival Order - FIFO):** If urgency and escalation status are identical, the patient with the earlier arrival timestamp is served first.

### Supporting Collections:
* `HashMap<String, Patient>`: Constant-time $O(1)$ lookup of active patients by unique token string.
* `EnumMap<PatientStatus, Set<PatientStatus>>`: Validated status transition graph.
* `EnumSet<PatientStatus>`: Compact, high-performance bit-vector representation of allowed target states.
* `ArrayList<Patient>`: Ordered snapshot generation for table viewing and reporting without altering heap structures.

---

## 🗄️ Database Architecture & Relational Tables

The MySQL database (`hospital_emergency_db`) maintains 6 primary tables with foreign key constraints:

| Table Name | Purpose | Key Columns |
| :--- | :--- | :--- |
| `patients` | One record per patient encounter | `patient_id` (PK), `token_no` (UK), `uhid`, `full_name`, `age`, `gender`, `emergency_level`, `status`, `arrival_time`, `assigned_bay`, `news2_score` |
| `treatment_records`| Clinical notes and consultation outcomes | `record_id` (PK), `patient_id` (FK), `doctor_id` (FK), `treatment_notes`, `diagnosis`, `medications`, `disposition`, `start_time`, `end_time` |
| `doctors` | Registry of emergency doctors | `doctor_id` (PK), `full_name`, `specialty`, `status`, `active_patients_count` |
| `clinical_orders` | Laboratory, radiology, and ECG orders | `order_id` (PK), `patient_id` (FK), `order_type`, `order_name`, `priority`, `status`, `result_value`, `abnormal_flag`, `ordered_at` |
| `users` | Authenticated staff login accounts | `user_id` (PK), `username` (UK), `password_hash`, `full_name`, `role`, `is_active`, `created_at` |
| `audit_logs` | Immutable audit trail for clinical actions | `log_id` (PK), `action_type`, `performed_by`, `patient_token`, `previous_state`, `new_state`, `details`, `logged_at` |

---

## 🧪 Testing & Verification

The project is backed by **37 automated JUnit 5 tests** checking all critical domain rules on every build, complemented by **15 manual test scenarios**:

### Automated Test Suite:

| Test Class | Tests | What is Checked |
| :--- | :---: | :--- |
| `PatientPriorityComparatorTest` | 4 | Critical before High, High before Medium, promoted patient prioritized, FIFO arrival tie-breaking |
| `PriorityQueueOrderingTest` | 3 | Min-heap ordering integrity, correct patient extraction, re-ordering after urgency adjustment |
| `PriorityQueueEscalationTest` | 2 | Medium promoted after 45 min wait, Low promoted after 90 min wait |
| `JavaTriageRuleEngineTest` | 6 | Critical triggers: low SpO2 (<90%), high BP (>200), tachycardia (>130 bpm), hypothermia (<95°F), bradypnea (<8/min) |
| `NEWS2ServiceTest` | 2 | Physiological score calculation for severely ill vs. stable patients |
| `PatientStateMachineTest` | 3 | Legal state transitions permitted; invalid state jumps throw `InvalidStateTransitionException` |
| `AIAgentOrchestratorTest` | 2 | Clinical advisory evaluation and safety override logic |
| `DoctorRoutingServiceTest` | 3 | Specialty doctor matching, busy doctor fallback, and general physician routing |
| `DatabaseQueueIntegrationTest` | 1 | In-memory heap queue state matches MySQL database table state |
| `JdbcPersistenceIntegrationTest` | 1 | SQL persistence, parameter binding, and retrieval using PreparedStatements |
| `TokenGeneratorTest` | 2 | Daily sequential token formatting (`TK-YYYY-XXXX`) and unique UHID generation |
| `ValidationUtilTest` | 7 | Boundary validation for patient names, ages, phone numbers, and vital signs |
| `ScreenCaptureTest` | 1 | UI rendering and lifecycle execution across all screen layouts |
| **Total** | **37** | **All 37 Tests Passed Cleanly** |

---

## 💻 System Requirements & Dependencies

### Minimum Requirements:
* **Operating System:** Windows 10/11 (64-bit), Linux, or macOS.
* **Java Development Kit:** JDK 17 (LTS), 64-bit (OpenJDK Temurin or Oracle JDK).
* **Database:** MySQL Community Server 8.0 or 8.4 LTS.
* **Build Tool:** Apache Maven 3.8 or higher.
* **Memory & Storage:** 4 GB RAM (8 GB recommended), 500 MB free disk space.

### Core Dependencies:
* `org.openjfx:javafx-controls:21.0.2` & `javafx-fxml:21.0.2`: GUI runtime components.
* `com.mysql:mysql-connector-j:8.4.0`: JDBC connectivity driver.
* `org.mindrot:jbcrypt:0.4`: Salted password hashing.
* `com.google.zxing:core:3.5.3` & `javase:3.5.3`: Barcode and QR code generation.
* `org.apache.pdfbox:pdfbox:3.0.2`: PDF clinical discharge document creation.
* `org.junit.jupiter:junit-jupiter:5.10.2`: Unit testing framework.

---

## 🚀 Setup & Execution Guide

### 1. Database Setup
Ensure MySQL Server is running locally on port `3306`. Execute the schema and sample data scripts:
```bash
mysql -u root -p < database/schema.sql
mysql -u root -p < database/sample-data.sql
```

Verify your credentials in `src/main/java/com/hospital/priority/util/DBConnection.java`:
```java
private static final String URL = "jdbc:mysql://localhost:3306/hospital_emergency_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
private static final String USER = "root";
private static final String PASSWORD = "your_mysql_password"; // Set to your password (default is empty "")
```

### 2. Build and Test
Verify the code and execute all automated tests:
```bash
mvn clean test
```

### 3. Launch the Application
Start the desktop application using Maven:
```bash
mvn javafx:run
```
*(Or double-click `start-app.bat` on Windows)*

### 4. Create Standalone Windows Executable (Optional)
Run the automated packaging script to build a self-contained runtime:
```bash
build-exe.bat
```

---

## 🔑 Default Demonstration Accounts

| Role | Username | Password | Default Workspace |
| :--- | :--- | :--- | :--- |
| **Emergency Doctor** | `dr.mehta` | `doctor123` | Doctor Workspace, SOAP Notes, Orders, Repeat Vitals, Discharge |
| **Hospital Administrator** | `admin` | `admin123` | Command Center, User Management, Reports, Audit Trail |
| **Triage Nurse** | `nurse.priya` | `nurse123` | Registration Wizard, Triage Assessment, Queue Management |
| **Receptionist** | `reception` | `recep123` | Patient Intake, Queue Board, Ticket Printing |

---

## 📄 References & Standards

1. Royal College of Physicians, *"National Early Warning Score (NEWS) 2: Standardising the assessment of acute-illness severity in the NHS,"* Report of a working party, London: RCP, 2017.
2. K. Mackway-Jones, J. Marsden and J. Windle (Eds.), *Emergency Triage: Manchester Triage Group*, 3rd ed., Wiley-Blackwell, 2014.
3. N. Gilboy, P. Tanabe, D. Travers and A. M. Rosenau, *Emergency Severity Index (ESI): A Triage Tool for Emergency Department Care*, Version 4, Implementation Handbook, AHRQ Publication No. 12-0014, 2012.
4. M. Benson, K. L. Koenig and C. H. Schultz, *"Disaster triage: START, then SAVE,"* Prehospital and Disaster Medicine, vol. 11, no. 2, pp. 117-124, 1996.
5. T. H. Cormen, C. E. Leiserson, R. L. Rivest and C. Stein, *Introduction to Algorithms*, MIT Press (Chapters on Binary Heaps and Priority Queues).
6. E. Gamma, R. Helm, R. Johnson and J. Vlissides, *Design Patterns: Elements of Reusable Object-Oriented Software*, Addison-Wesley, 1994.
7. Oracle Corporation, *Java Platform, Standard Edition 17 API Documentation (`java.util.PriorityQueue`)*.
8. OpenJFX Project, *JavaFX 21 Documentation*.
