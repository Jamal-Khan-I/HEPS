# Hospital Emergency Priority System (HEPS)

[![Java 17](https://img.shields.io/badge/Java-17%20LTS-007396?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/17/)
[![JavaFX 21](https://img.shields.io/badge/JavaFX-21.0.2-FF6F00?style=for-the-badge&logo=java&logoColor=white)](https://openjfx.io/)
[![MySQL 8](https://img.shields.io/badge/MySQL-8.4%20LTS-4479A1?style=for-the-badge&logo=mysql&logoColor=white)](https://www.mysql.com/)
[![Maven 3](https://img.shields.io/badge/Apache%20Maven-3.9-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white)](https://maven.apache.org/)
[![JUnit 5](https://img.shields.io/badge/Tests-37%20Passed-25A162?style=for-the-badge&logo=junit5&logoColor=white)](https://junit.org/junit5/)
[![License MIT](https://img.shields.io/badge/License-MIT-blue.svg?style=for-the-badge)](LICENSE)

A high-performance JavaFX desktop application for hospital emergency departments. HEPS replaces conventional manual and First-Come, First-Served (FIFO) waiting lines with an intelligent triage engine, an in-memory priority queue (min-heap), and physician clinical workstations.

---

## ⚡ Key Features

### 🚨 Clinical Triage & Urgency Stratification
* **4-Tier Urgency Classification:** Automatically stratifies patients into standardized urgency levels:
  * **Critical (Level 1):** Immediate resuscitation (Cardiac arrest, severe trauma, SpO2 < 90%).
  * **High (Level 2):** Care within 15 minutes (Fractures, severe asthma, systolic BP > 200).
  * **Medium (Level 3):** Care within 60 minutes (Lacerations, acute abdominal pain).
  * **Low (Level 4):** Care within 120 minutes (Minor scrapes, mild cold symptoms).
* **Clinical Safety Override:** Immediate escalation to Critical if physiological vitals cross high-risk safety thresholds regardless of the stated complaint.
* **Early Warning Score (NEWS2):** Automated physiological calculation across respiration, oxygen saturation, blood pressure, pulse, temperature, and alertness.

### 🧠 In-Memory Priority Queue & Anti-Starvation
* **Min-Heap Algorithm:** Backed by `java.util.PriorityQueue<Patient>` with $O(\log n)$ insertion/polling and $O(1)$ lookup for the next highest-urgency patient.
* **3-Tier Custom Comparator:** Sorts primarily by clinical urgency level, secondarily by anti-starvation escalation status, and tertiarily by arrival timestamp (FIFO within the same urgency tier).
* **Automated Anti-Starvation Daemon:** Background timer running every 15 seconds scans waiting durations:
  * Promotes **Medium** patients waiting > 45 minutes to **High**.
  * Promotes **Low** patients waiting > 90 minutes to **Medium**.
  * Re-sorts the priority queue automatically upon any promotion.

### 🩺 Doctor Clinical Workspace
* **Patient Journey Stepper:** 5-step circular journey tracker (`Registered` → `Triaged` → `Assigned` → `Treatment` → `Disposition`).
* **Demographic Banner:** Real-time patient identity, age, gender, assigned bay, on-duty physician, active allergies, and elapsed treatment duration.
* **Vitals Trend Analysis:** Dual-metric visual line chart tracking oxygen saturation (SpO2) and heart rate alongside a 2x2 physiological metric grid.
* **Structured SOAP Clinical Notes:** Tabbed clinical encounter documentation (*Subjective*, *Objective*, *Assessment*, *Plan*) with required-field validation and live character counters.
* **Serial Vitals & NEWS2 Deterioration Watchdog:** Null-safe serial vitals input with live keystroke NEWS2 calculation and automated deterioration warning alerts.

### 🧪 Diagnostic Orders & Laboratory Results
* **STAT Quick-Order Chips:** Rapid ordering for critical tests (`Troponin I`, `12-Lead ECG`, `Chest X-Ray`, `CBC STAT`, `BMP`, `Aspirin 300mg`).
* **Result Inspector:** Structured display of lab and imaging results with abnormal value flags.

### 🏥 Bay Allocation & Occupancy Board
* **12 Dedicated Clinical Bays:** Real-time tracking across Resuscitation (Resus), Acute, and Sub-Acute zones.
* **Turnaround Management:** Visual bed status indicators (`Available`, `Occupied`, `Cleaning`) with a 15-minute bed sanitization timer.

### 📋 Disposition, Discharge & Documentation
* **Automated PDF Discharge Summaries:** Compiles consultation notes, diagnoses, medications, and follow-up plans into printable PDF documents using Apache PDFBox.
* **Digital Triage Tickets:** Generates thermal triage tickets with barcodes and 2D QR codes using Google ZXing for bedside patient verification.
* **Mass Casualty Incident (MCI) Mode:** One-click activation of the Simple Triage and Rapid Treatment (START) protocol with international color tags (Red, Yellow, Green, Black).

### 🔐 Security, RBAC & Audit Trail
* **Role-Based Access Control:** Dedicated interfaces tailored for 4 staff roles: Hospital Administrator, Emergency Physician, Triage Nurse, and Receptionist.
* **Password Hashing:** Salted BCrypt password encryption for all staff accounts.
* **Immutable Audit Trail:** Comprehensive event logging capturing user IDs, timestamps, patient tokens, and previous/new clinical states.

### 📈 Reports & SLA Compliance Audits
* **Acuity Distribution:** Live pie chart breaking down patient urgency mix.
* **Hourly Patient Volume:** Bar chart analyzing peak arrival hours with full time-axis labeling.
* **SLA Benchmark Table:** Target vs. actual wait time compliance audit by acuity category.

---

## 🛠️ Technology Stack

| Layer | Component | Version | Description |
| :--- | :--- | :--- | :--- |
| **Language** | Java (OpenJDK Temurin) | 17 LTS | Core business logic, OOP models, Collections |
| **UI Framework** | JavaFX / OpenJFX | 21.0.2 | Hardware-accelerated desktop views and controllers |
| **UI Definition** | FXML & Vanilla CSS | JavaFX 21 | Clean view layouts with zero inline styles |
| **Database** | MySQL Server | 8.4 LTS | Relational storage with foreign key constraints |
| **Driver** | MySQL Connector/J | 8.4.0 | JDBC connection handling and PreparedStatements |
| **Security** | jBCrypt | 0.4 | Salted BCrypt credential hashing |
| **PDF Generation** | Apache PDFBox | 3.0.2 | Programmatic clinical discharge summary generation |
| **QR & Barcodes** | Google ZXing | 3.5.3 | 2D QR code encoding on triage tickets |
| **Build Tool** | Apache Maven | 3.9+ | Dependency management and shaded JAR packaging |
| **Testing** | JUnit 5 (Jupiter) | 5.10.2 | Automated unit and integration test suites |

---

## 🏛️ System Architecture

HEPS uses a **Layered Clean Architecture** enforcing separation of concerns:

```
├── Presentation Layer     JavaFX FXML screens, custom CSS, and event controllers
│       ▼
├── Business Logic Layer   QueueService, NEWS2Service, JavaTriageRuleEngine,
│                          PatientStateMachine, DoctorRoutingService, BayService
│       ▼
├── Data Access Layer      DAO Interfaces (PatientDao, DoctorDao, OrderDao, etc.)
│                          and implementations using JDBC PreparedStatements
│       ▼
└── Persistence Layer      MySQL 8 relational database (hospital_emergency_db)
```

### OOP Principles & Patterns Applied:
* **Encapsulation:** Domain models (`Patient`, `Vitals`, `Doctor`, `Bay`) guard state and validate physiological ranges via accessors.
* **Inheritance & Polymorphism:** Custom exception hierarchy and polymorphic sorting via `Comparator<Patient>`.
* **Abstraction:** Services call DAO interfaces without exposure to SQL syntax or database connection logic.
* **Design Patterns:**
  * **Singleton:** Centralized coordinators (`QueueService`, `UserSession`, `DBConnection`, `BayService`).
  * **State Pattern:** `PatientStateMachine` guarantees valid clinical progressions and rejects illegal status transitions.
  * **Strategy Pattern:** `PatientPriorityComparator` decouples priority calculation from queue storage.
  * **DAO Pattern:** Decouples persistence from domain models.
  * **Observer Pattern:** JavaFX `ObservableList` bindings keep UI tables synchronized with model updates.

---

## 🚀 Getting Started

### Prerequisites
* **Java Development Kit (JDK):** Version 17 LTS (OpenJDK Temurin or Oracle JDK 17+).
* **Apache Maven:** Version 3.8 or higher.
* **MySQL Server:** Version 8.0 or 8.4 running locally on port `3306`.

---

### 1. Database Setup
Ensure MySQL is running, then load the schema and sample data:
```bash
mysql -u root -p < database/schema.sql
mysql -u root -p < database/sample-data.sql
```

Verify connection parameters in `src/main/java/com/hospital/priority/util/DBConnection.java`:
```java
private static final String URL = "jdbc:mysql://localhost:3306/hospital_emergency_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
private static final String USER = "root";
private static final String PASSWORD = "your_mysql_password"; // Set your password (default is empty "")
```

---

### 2. Build and Test
Run Maven to compile the application and execute all 37 automated tests:
```bash
mvn clean test
```

---

### 3. Run the Application
Launch the desktop client via Maven:
```bash
mvn javafx:run
```
*(On Windows, you can also double-click `start-app.bat`)*

---

### 4. Build Standalone Package (Optional)
To package an executable standalone distribution:
```bash
build-exe.bat
```

---

## 🔑 Default User Accounts

| Role | Username | Password | Default Interface |
| :--- | :--- | :--- | :--- |
| **Emergency Doctor** | `dr.mehta` | `doctor123` | Doctor Workspace, SOAP Notes, Orders, Repeat Vitals, Discharge |
| **Administrator** | `admin` | `admin123` | Command Center, User Management, Reports, Audit Trail |
| **Triage Nurse** | `nurse.priya` | `nurse123` | Registration Wizard, Triage Assessment, Priority Queue |
| **Receptionist** | `reception` | `recep123` | Patient Intake, Queue Board, Ticket Printing |

---

## 🧪 Test Coverage

The project includes **37 automated JUnit 5 tests** checking core domain rules on every build:

* `PatientPriorityComparatorTest` (4 tests) — Priority tier ranking, escalation priority, arrival FIFO tie-breaking.
* `PriorityQueueOrderingTest` (3 tests) — Heap structure integrity, top-patient retrieval, re-sorting.
* `PriorityQueueEscalationTest` (2 tests) — Anti-starvation 45m and 90m promotion triggers.
* `JavaTriageRuleEngineTest` (6 tests) — SpO2, blood pressure, heart rate, temperature, and respiration triggers.
* `NEWS2ServiceTest` (2 tests) — NEWS2 score computation and risk band classification.
* `PatientStateMachineTest` (3 tests) — Valid state transitions and rejection of illegal state jumps.
* `DoctorRoutingServiceTest` (3 tests) — Specialty matching, busy fallback, and workload balancing.
* `DatabaseQueueIntegrationTest` (1 test) — In-memory heap queue synchronization with MySQL.
* `JdbcPersistenceIntegrationTest` (1 test) — CRUD persistence via PreparedStatements.
* `TokenGeneratorTest` (2 tests) — Sequential daily token formatting and unique UHID generation.
* `ValidationUtilTest` (7 tests) — Boundary validation for demographics, phone numbers, and vitals.
* `ScreenCaptureTest` (1 test) — FXML loading and UI rendering validation across all screens.

---

## 📄 License

This project is licensed under the [MIT License](LICENSE).
