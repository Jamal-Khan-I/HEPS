# Hospital Emergency Priority System — Team Presentation Guide

**Course:** CSE203 — Object-Oriented Programming using Java  
**Presentation File:** [`HEPS_Conceptual_Presentation.pptx`](file:///e:/Antigravity%20IDE/hospital-emergency-priority-system/docs/HEPS_Conceptual_Presentation.pptx)  
**Total Duration:** 15 Minutes (~3 min per member) + 5 Minutes Q&A

---

## Team Roster & Speaking Order

| Member | Topic | Time Slot |
| :--- | :--- | :--- |
| **Member 1** | The Problem & Motivation (Slides 1–4) | 0:00 – 3:00 |
| **Member 2** | Our Solution & Patient Journey (Slides 5–6) | 3:00 – 6:00 |
| **Member 3** | Smart Queue & Priority Logic (Slides 7–8) | 6:00 – 9:00 |
| **Member 4** | Database, Security & Architecture (Slides 9–12) | 9:00 – 12:00 |
| **Member 5** | Testing, Team Work & Live Demo (Slides 13–17) | 12:00 – 15:00 |

---

## Slide-by-Slide Speaker Notes

### Slide 1 — Title Slide
**Speaker:** Member 1

> "Good morning. We are presenting our mini project: the Hospital Emergency Priority System. It's a desktop application that helps hospital staff manage emergency patients by automatically prioritizing them based on how sick they are — so the most critical patient is always treated first."

---

### Slide 2 — Agenda
**Speaker:** Member 1

> "Our presentation has five parts: I'll explain the problem, Member 2 will show our solution, Member 3 will explain how the smart queue works, Member 4 covers data storage and security, and Member 5 will give a live demo."

---

### Slide 3 — The Problem
**Speaker:** Member 1

> "In busy emergency rooms, patients arrive with very different conditions — some have minor cuts, others are having heart attacks. If we use a simple first-come-first-served system, a critically ill patient might have to wait behind someone with a cold. That delay can be life-threatening. Staff also struggle with paper-based tracking — it's error-prone and there's no reliable history."

---

### Slide 4 — Priority Levels
**Speaker:** Member 1

> "Our system classifies every patient into one of four levels: Critical for life-threatening cases, High for urgent but stable conditions, Medium for patients who need attention soon, and Low for minor issues. This is based on real hospital triage systems."

**Handoff:** "I'll now hand over to Member 2 to show how our system solves this problem."

---

### Slide 5 — Our Solution
**Speaker:** Member 2

> "Our system, HEPS, is a desktop application with six key capabilities: patient registration with automatic token generation, a live dashboard showing waiting counts and critical cases, a smart queue that auto-sorts by urgency, a complete treatment workflow for doctors, search and history for finding any patient record, and staff roles so each person sees only what they need."

---

### Slide 6 — Patient Journey
**Speaker:** Member 2

> "Let me walk you through how a patient moves through the system. Step 1: The receptionist registers the patient and assigns a priority level. Step 2: A unique token like TK-2026-0001 is generated. Step 3: The system automatically places the patient at the right position in the queue based on urgency. Step 4: The doctor sees who's next and begins treatment. Step 5: After treatment, the doctor adds clinical notes and marks it complete. Every status change is permanently saved."

**Handoff:** "Member 3 will now explain how the smart queue actually works."

---

### Slide 7 — How the Smart Queue Works
**Speaker:** Member 3

> "This is the heart of our project. Let me show you with an example. Four patients arrive: Patient A with a mild cold at 2 PM, Patient B with a sprained ankle at 2:05, Patient C with chest pain at 2:10, and Patient D with a deep cut at 2:15. In a normal queue, they'd be treated in arrival order — meaning the chest pain patient waits behind two non-urgent patients. But our smart queue automatically reorders them: Patient C goes to position 1 because they're Critical, then D who is High, then B who is Medium, and finally A who is Low."

> "How does it decide? Three simple rules: First, compare urgency — Critical always beats everything else. Second, if two patients have the same urgency, the one who arrived earlier goes first. Third, if everything matches, the smaller token number wins."

---

### Slide 8 — Normal vs Smart Queue
**Speaker:** Member 3

> "This slide shows the difference clearly. On the left: a normal queue where Patient C with chest pain is stuck at position 3. On the right: our smart queue where Patient C is automatically moved to position 1. This is the core value of our project — the system makes the right decision automatically."

**Handoff:** "Member 4 will now explain how we store and protect all this patient data."

---

### Slide 9 — How We Built It
**Speaker:** Member 4

> "We built HEPS using four main technologies: Java 17 as our core programming language, JavaFX for the desktop user interface, MySQL 8 as our database to store all records permanently, and JDBC as the bridge between Java and MySQL. The flow is simple: staff interacts with the screen, the application logic processes the request, the smart queue handles prioritization, and the database stores everything."

---

### Slide 10 — Design Principles
**Speaker:** Member 4

> "We followed four key principles: Encapsulation — patient data is protected and can only be accessed through controlled methods. Abstraction — complex database operations are hidden behind simple interfaces. Polymorphism — the same code handles different priority levels with different visual styles automatically. And Error Handling — if something goes wrong, the user gets a clear message instead of a crash."

---

### Slide 11 — What Data We Store
**Speaker:** Member 4

> "We have three database tables: Patients table stores the token, name, age, symptoms, priority level, and current status. Treatment Records stores which doctor treated them, when treatment started and ended, and clinical notes. And the Staff Users table manages login credentials with encrypted passwords and role assignments."

---

### Slide 12 — Data Safety
**Speaker:** Member 4

> "Patient data is sensitive, so we built in six layers of protection: encrypted passwords, protection against database attacks, input validation on every field, permanent storage that survives restarts, external configuration so credentials aren't in the code, and graceful error handling if the database goes offline."

**Handoff:** "Member 5 will now show our test results and give a live demonstration."

---

### Slide 13 — Testing
**Speaker:** Member 5

> "We wrote 19 automated tests — and all of them pass. We tested that Critical patients always sort before others, that tie-breaking works correctly, that 1,000 generated tokens are all unique with zero duplicates, that invalid inputs are properly rejected, and that patients loaded from the database maintain the correct priority order."

---

### Slide 14 — Team Contributions
**Speaker:** Member 5

> "Every team member contributed to the project. Member 1 handled research and requirements, Member 2 designed the application architecture, Member 3 implemented the priority queue logic, Member 4 built the database and security layer, and Member 5 designed the UI and wrote tests. All our work is tracked on GitHub with individual commits."

---

### Slide 15 — Live Demo
**Speaker:** Member 5

> "Now let me demonstrate the system. I'll open the app, register three patients with different priority levels, show how the queue auto-sorts them, begin treatment on the top patient, complete the treatment with doctor notes, and search for the patient record. Watch how the Critical patient automatically goes to position 1, how the color badges make scanning easy, and how the dashboard numbers update instantly."

*(Switch to the running application and demonstrate)*

---

### Slide 16 — Key Takeaways
**Speaker:** Member 5

> "To summarize: We solved a real hospital problem — automated emergency patient prioritization. Our smart queue always treats the sickest patient first. Patient data is secure and permanently stored. The complete workflow from registration to discharge is tracked. And we built it as a team using Git, with clean architecture and 19 passing tests."

---

### Slide 17 — Thank You
**Speaker:** All Members

> "Thank you for your time. We're happy to answer any questions."

---

## Tips for Each Presenter

1. **Speak slowly and clearly** — you have 3 minutes, that's plenty of time.
2. **Don't read from the slides** — use the speaker notes above as your guide.
3. **Point to visuals** — gesture at the cards/diagrams on screen as you talk.
4. **Practice the handoff** — smooth transitions between members look professional.
5. **During Q&A** — the person with the most relevant expertise should answer.

## Common Questions to Prepare For

| Question | Who Answers | Quick Answer |
| :--- | :--- | :--- |
| "Why not use a normal queue?" | Member 3 | "Normal queues use first-come-first-served, which can delay critical patients. Our priority queue ensures urgency decides the order." |
| "How do you handle same-priority patients?" | Member 3 | "The patient who arrived earlier goes first — then by token number." |
| "Is the data saved permanently?" | Member 4 | "Yes, in MySQL. Even if the app restarts, all records are preserved." |
| "How do you prevent data attacks?" | Member 4 | "All database queries use parameterized statements, which block SQL injection." |
| "What OOP concepts did you use?" | Member 2/4 | "Encapsulation, Abstraction, Polymorphism, and Custom Exception Handling." |
| "How many tests did you write?" | Member 5 | "19 automated tests, all passing — covering priority sorting, validation, and database sync." |
