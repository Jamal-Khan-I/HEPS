# Hospital Emergency Priority System (HEPS) — Team Presentation Guide

**Course:** CSE203 - Object-Oriented Programming using Java  
**Evaluation:** Mini-Project Presentation & Individual Viva (50 Marks)  
**File Generated:** [`docs/Hospital_Emergency_Priority_System_Presentation.pptx`](file:///e:/Antigravity%20IDE/hospital-emergency-priority-system/docs/Hospital_Emergency_Priority_System_Presentation.pptx)  
**Total Target Duration:** 15 Minutes (~3 Minutes per Team Member) + 5 Minutes Q&A

---

## Team Roster & Responsibility Matrix

| Member | Primary Presentation Responsibility | Key Technical Topics Covered | Target Time |
| :--- | :--- | :--- | :--- |
| **Member 1** | **Clinical Context & Problem Statement** | Emergency Department overload, why FIFO queues fail in healthcare, triage urgency scale, project objectives. | 0:00 – 3:00 |
| **Member 2** | **OOP Architecture & System Design** | Layered architecture (FXML -> Controller -> Service -> DAO -> MySQL), Encapsulation, Abstraction, Polymorphism, Custom Exceptions. | 3:00 – 6:00 |
| **Member 3** | **Data Structures & Priority Queue Algorithms** | Min-Heap binary tree mechanics, $O(1)$ vs $O(\log N)$ complexity, 3-tier `PatientPriorityComparator`, tie-breaking logic. | 6:00 – 9:00 |
| **Member 4** | **Database Design, Enterprise JDBC & Security** | MySQL relational schema, Foreign key cascades, PreparedStatements against SQL Injection, heap-to-DB synchronization. | 9:00 – 12:00 |
| **Member 5** | **UI Tour, Automated QA & Live Demo** | JavaFX workstation layout, 19 passing JUnit 5 tests, live workflow (Register -> Admit -> Discharge), project conclusion. | 12:00 – 15:00 |

---

## Slide-by-Slide Speaker Notes & Talk Tracks

### [Slide 1] Title Slide (All Members on Stage)
- **Speaker:** Member 1
- **Visual:** Project Title, Course Code (CSE203), Team Member Names.
- **Speaking Script:**
  > *"Good morning respected evaluators and panel members. We are Group [Number], presenting our Object-Oriented Programming mini-project: the **Hospital Emergency Priority System (HEPS)**. Our system is an event-driven clinical workstation designed to solve the critical challenge of emergency patient prioritization using Java 17, JavaFX 21, relational persistence in MySQL, and an algorithmic min-heap priority queue. I will begin by introducing the healthcare challenge, followed by my team members who will cover our OOP architecture, data structure implementation, database engineering, and a live demonstration."*

---

### [Slide 2] Presentation Agenda & Team Division
- **Speaker:** Member 1
- **Visual:** 5-member agenda breakdown cards.
- **Speaking Script:**
  > *"To give you a structured view of our defense today, we have divided the technical presentation into five distinct competencies: I will cover the clinical problem and triage requirements; Member 2 will explain our layered software architecture and OOP principles; Member 3 will walk through the internal binary heap mechanics and comparator algorithms; Member 4 will detail our MySQL relational persistence and security; and Member 5 will present our workstation UI, automated test suite, and live demo."*

---

### [Slide 3] The Healthcare Challenge: Emergency Room Overload
- **Speaker:** Member 1
- **Visual:** Emergency room surge problem card and 4-level triage acuity scale (`CRITICAL`, `HIGH`, `MEDIUM`, `LOW`).
- **Speaking Script:**
  > *"In a busy hospital emergency department, patient arrivals are unpredictable and continuous. Patients walk through the door with vastly differing acuities—from superficial abrasions to life-threatening acute respiratory failure or myocardial infarction. If reception staff rely on manual registers or whiteboards, they experience severe cognitive fatigue under pressure, leading to dangerous errors and information silos."*

---

### [Slide 4] Why FIFO Queues Fail & System Objectives
- **Speaker:** Member 1
- **Visual:** The FIFO trap vs. algorithmic priority queue; HEPS project objectives.
- **Speaking Script:**
  > *"In computer science, First-In, First-Out (FIFO) is standard for print spoolers and web servers. In healthcare, FIFO is medically fatal. A patient experiencing cardiac arrest who arrives at 14:45 cannot wait behind a patient with mild back pain who arrived at 14:00. Even a 5-minute delay in resuscitation can cause irreversible hypoxia. Therefore, our primary objective was to build an automated algorithmic triage queue where urgency dictates position, while arrival time acts solely as a fair tie-breaker within the same urgency tier."*
- **Handoff Line:**
  > *"I will now hand over to Member 2 to explain how we structured our software architecture using clean Object-Oriented principles."*

---

### [Slide 5] Layered Architecture & Separation of Concerns
- **Speaker:** Member 2
- **Visual:** 6-layer architecture flow diagram.
- **Speaking Script:**
  > *"Thank you, Member 1. To ensure long-term maintainability and strict separation of concerns, HEPS is structured into six decoupled layers. At the top, the **Presentation Layer** uses JavaFX FXML views styled with CSS, containing zero business logic. The **Controller Layer** captures UI events and invokes the **Service Layer**—specifically `PatientService` and `QueueService`. Beneath them, the **Domain & DSA Layer** encapsulates our core entities and priority comparator. Finally, our **DAO Layer** abstracts database operations, communicating via JDBC with **MySQL 8**."*

---

### [Slide 6] Core OOP Design Principles in Action
- **Speaker:** Member 2
- **Visual:** Four cards covering Encapsulation, Abstraction (DAO), Polymorphism, and Custom Exceptions.
- **Speaking Script:**
  > *"We applied all four pillars of OOP throughout the codebase:
  > 1. **Encapsulation:** Domain classes like `Patient`, `User`, and `TreatmentRecord` keep fields private, with controlled accessors enforcing business invariants.
  > 2. **Abstraction:** Through the Data Access Object pattern, `PatientDao` and `TreatmentRecordDao` define abstract interfaces. The services know nothing about SQL syntax.
  > 3. **Polymorphism:** We implement the `Comparator<Patient>` interface dynamically, and use generic JavaFX `TableCell` factories to render custom status pills and priority badges.
  > 4. **Exception Handling:** We designed a clean hierarchy with `InvalidPatientDataException` for validation boundaries and `DatabaseOperationException` for SQL failures, ensuring user-friendly error alerts without application crashes."*
- **Handoff Line:**
  > *"Member 3 will now take you into our core Data Structure and the internal mechanics of our PriorityQueue."*

---

### [Slide 7] Data Structures & Algorithms: Java PriorityQueue
- **Speaker:** Member 3
- **Visual:** Binary min-heap diagram and asymptotic time complexity comparison table.
- **Speaking Script:**
  > *"Thank you, Member 2. At the core of our triage engine is Java's `java.util.PriorityQueue<Patient>`, which is internally implemented as an array-backed binary min-heap. Unlike a sorted LinkedList which requires $O(N)$ linear time to insert a new patient, the binary heap maintains the heap invariant where every parent is smaller than or equal to its children. This gives us $O(\log N)$ logarithmic insertion via sift-up, $O(\log N)$ deletion via sift-down, and $O(1)$ constant-time retrieval of the queue head. For 1,000 waiting patients, an enqueue requires fewer than 10 comparison operations."*

---

### [Slide 8] Deterministic 3-Tier Priority Comparator Algorithm
- **Speaker:** Member 3
- **Visual:** `PatientPriorityComparator` decision tree breakdown (Tier 1, 2, 3).
- **Speaking Script:**
  > *"To govern the heap ordering, we authored a custom `PatientPriorityComparator`. The comparison follows a strict 3-tier cascade:
  > - **Tier 1:** Medical Emergency Level. `CRITICAL` (rank 1) has highest priority, followed by `HIGH` (2), `MEDIUM` (3), and `LOW` (4).
  > - **Tier 2:** Arrival Timestamp tie-breaker. If two patients have equal urgency, the one with the earlier arrival time is placed first—enforcing strict First-Come, First-Served fairness within the tier.
  > - **Tier 3:** Sequential Token Number tie-breaker. If two patients have identical timestamps to the millisecond, their unique token strings guarantee deterministic mathematical sorting.
  > Crucially, because direct iteration over Java's `PriorityQueue` only traverses the array in arbitrary heap order, `QueueService.getSortedSnapshot()` extracts elements into an immutable list sorted by this comparator before populating the JavaFX table."*
- **Handoff Line:**
  > *"Member 4 will now present our database schema and JDBC persistence engineering."*

---

### [Slide 9] Relational Database Design & Schema Architecture
- **Speaker:** Member 4
- **Visual:** MySQL tables (`patients`, `treatment_records`) with keys and indexes.
- **Speaking Script:**
  > *"Thank you, Member 3. To guarantee data durability and auditable patient history, HEPS uses a local relational MySQL database. Our schema comprises two core transactional tables:
  > - The `patients` table stores demographic records, triage urgency, arrival timestamps, and queue statuses (`WAITING`, `IN_TREATMENT`, `COMPLETED`).
  > - The `treatment_records` table holds physician consultation documentation, linking to `patients` via a foreign key with cascading referential integrity.
  > We indexed `status`, `emergency_level`, and `arrival_time` to ensure instant query performance even under high historical visit volumes."*

---

### [Slide 10] Enterprise JDBC Persistence & Security Measures
- **Speaker:** Member 4
- **Visual:** PreparedStatements, heap-to-DB sync algorithm, and try-with-resources resource management.
- **Speaking Script:**
  > *"On the persistence tier, we implemented enterprise security and synchronization best practices:
  > - **SQL Injection Prevention:** 100% of our SQL statements use parameterized `PreparedStatement` queries. User inputs are escaped by the MySQL JDBC driver as data literals.
  > - **Dual-Layer Synchronization:** On startup, `QueueService` queries MySQL for all waiting patients and reconstructs the in-memory `PriorityQueue` in $O(N \log N)$ time. Any status mutation is persisted in MySQL before reconciling the in-memory queue.
  > - **Resource Safety:** All database connections, statements, and result sets are wrapped in Java `try-with-resources` blocks to completely eliminate connection leaks.
  > - **Patient Privacy:** Phone numbers are masked in reception tables as `+91 98****001`, only exposed in clinical inspection drawers upon doctor authorization."*
- **Handoff Line:**
  > *"Member 5 will now demonstrate the clinical desktop workstation and our automated testing results."*

---

### [Slide 11] Emergency Department Staff Workstation UI
- **Speaker:** Member 5
- **Visual:** Full application workstation screenshot (`screen_full_app.png`).
- **Speaking Script:**
  > *"Thank you, Member 4. For our user interface, we moved away from generic student templates and designed an authentic clinical desktop workstation. Notice the calm color palette: soft cool grays, white panels, and restrained hospital blue, reserving red strictly for CRITICAL life-threats. The header prominently displays 'Emergency Department' with administrator identity and sign-out controls. The sidebar features custom SVG vector icons for our four core views: Overview, Register Patient, Emergency Queue, and Patient Records."*

---

### [Slide 12] Functional Screen Tour & Clinical Workflows
- **Speaker:** Member 5
- **Visual:** Three operational screen cards (Register, Queue, Records).
- **Speaking Script:**
  > *"Here you see our three primary operational views:
  > 1. **Register Patient:** A centered two-column card grouping personal details on the left and visit details on the right with priority guidance and automated token generation.
  > 2. **Emergency Queue:** A live worklist ordered by urgency. Clicking 'Begin Treatment' admits the queue head, updating the database and moving them into treatment bays.
  > 3. **Patient Records:** An auditable log with masked phone privacy, consistent status pills, and doctor discharge note recording."*

---

### [Slide 13] Automated Testing, Verification & Quality Assurance
- **Speaker:** Member 5
- **Visual:** JUnit 5 test breakdown (19 tests passing) and Maven build log.
- **Speaking Script:**
  > *"To ensure software correctness, we developed a comprehensive test suite of 19 automated JUnit 5 tests. Our tests validate:
  > - 3-tier comparator ordering and tie-breaking edge cases.
  > - PriorityQueue poll extraction and heap invariant maintenance.
  > - Full database bootstrapping and transactional JDBC lifecycles.
  > - Demographic boundary validation (age 0-125, valid phone numbers).
  > - Headless JavaFX screen capture tests verifying zero visual clipping.
  > Running `mvn clean test` compiles all classes and passes 19 out of 19 tests with zero failures."*

---

### [Slide 14] Individual Viva Voce Defense (All Members)
- **Speaker:** Member 5 transitions; all members ready.
- **Visual:** Five core technical defense questions and answers.
- **Speaking Script:**
  > *"We have also prepared for individual viva voce questions covering our technical design choices—from why we chose a binary heap over a sorted LinkedList, to how our DAO abstraction shields the application from persistence changes, to the common trap of iterating over Java's PriorityQueue."*

---

### [Slide 15] Project Achievements & Future Scope
- **Speaker:** Member 5
- **Visual:** Key achievements and future roadmap (HL7/FHIR, IoT vitals, multi-terminal WebSocket sync).
- **Speaking Script:**
  > *"In summary, HEPS successfully bridges theoretical computer science with real-world clinical application, meeting every rubric requirement of our OOP course. In future iterations, we plan to integrate HL7/FHIR standards for hospital EHR connectivity, IoT vitals monitor ingestion, and WebSocket-driven multi-terminal synchronization."*

---

### [Slide 16] Q&A & Thank You
- **Speaker:** Member 5 (All Members step forward)
- **Visual:** Thank You & Q&A invitation.
- **Speaking Script:**
  > *"Thank you for your time and attention. We now invite any questions or comments from the examination panel."*

---

## Examiner Viva Voce Preparation (Per Member)

| Member | Probable Viva Question | Precise Answer to Give |
| :--- | :--- | :--- |
| **Member 1** | *Why not just sort the patients by arrival time and let the doctor pick who looks sickest?* | "Manual sorting introduces clinical bias, cognitive fatigue under pressure, and dangerous delays. Algorithmic triage guarantees the most acute patient is placed at the queue head deterministically, upholding hospital patient-safety standards." |
| **Member 2** | *What is the advantage of using DAO interfaces instead of putting SQL in the controllers?* | "Separation of concerns and loose coupling. If the hospital switches databases from MySQL to Oracle or MongoDB, only the DAO implementation changes. Controllers and views remain 100% untouched." |
| **Member 3** | *If Java's PriorityQueue is sorted, why did you need `Collections.sort()` for the TableView?* | "`PriorityQueue.iterator()` traverses internal array elements in heap-tree order, not sorted order. Only sequential `poll()` yields sorted order. To display a sorted table without draining the queue, we take a snapshot list and sort it using our comparator." |
| **Member 4** | *How does your application prevent SQL Injection in the search bar?* | "We use `PreparedStatement` with parameterized placeholders (`?`). The JDBC driver treats user inputs strictly as literal values, completely neutralizing SQL injection attempts." |
| **Member 5** | *How does your UI prevent accidental double-admission or premature discharge?* | "The 'Complete Treatment' button is strictly disabled until an `IN_TREATMENT` patient is selected. Admitting the queue head prompts a confirmation dialog and atomically updates both MySQL and the in-memory queue." |
