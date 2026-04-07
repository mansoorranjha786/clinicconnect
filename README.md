# ClinicConnect: Pediatric Immunization Eligibility Engine
### Starlight Pediatrics — Automated Catch-Up Vaccination Scheduler

---

## 🏥 What Does This System Do?

This system is a **middleware service** designed to automate the tracking of pediatric immunizations. It helps your front desk team proactively identify children who are missing vaccinations, calculating exactly how overdue they are and when they can next be scheduled for a catch-up appointment.

Every morning, the engine:
1. **Loads Patient Records**: Analyzes your internal database of pediatric patients.
2. **Syncs with the CDC**: Programmatically retrieves the latest vaccine requirements (MMR, DTaP, etc.) from public health data sources.
3. **Runs Eligibility Logic**: Compares each child's current age and history against the recommended schedule.
4. **Generates a Gap Report**: Produces a "Daily Clinic Action List" sorted by urgency.

---

## 🚀 How to Run (Non-Technical Guide)

### Prerequisites
- **Java 21**: Ensure you have a modern Java version installed ([Download here](https://adoptium.net/)).
- **Internet Access**: Required for the system to fetch the live CDC schedule.

### Step 1 — Start the Engine
If you are using **IntelliJ IDEA**, simply open the project and click **Run** (the green arrow) on the `ClinicconnectApplication` file.

Alternatively, use the terminal:
```bash
./mvnw spring-boot:run
```

### Step 2 — View the Daily Action List
Once the engine is running, open your web browser or use a tool like Postman to view the results:

**Daily Action List (All Gaps):**
`http://localhost:8080/api/v1/immunization/daily-report`

---

## 📋 Understanding the "Gap Report"
The report is provided in **JSON format** (a standard data format). Here is how to read the most important fields:

- **`urgencyLevel`**:
    - `CRITICAL`: Child is significantly overdue (365+ days) or within 30 days of the maximum age for the vaccine. Call immediately.
    - `HIGH`: Overdue by 180-365 days. Schedule this week.
    - `MEDIUM`: Overdue by 90-180 days.
    - `LOW`: Overdue by less than 90 days.
- **`daysOverdue`**: The number of days passed since the vaccine was first recommended.
- **`nextEligibleDate`**: The earliest date the child can safely receive the next dose (respecting medical wait times between doses).
- **`actionRequired`**: A human-readable instruction for the front desk staff.

---

## 🧠 Logic Guide: How Eligibility is Calculated

The ClinicConnect engine follows a strict multi-step validation process for every patient and every required vaccine dose:

### 1. Age-Based Eligibility
The system checks the patient's current age against the CDC's **Minimum Age** requirements.
*   *Example*: If a vaccine requires the child to be at least 12 months old, a 10-month-old will not be flagged as "missing" even if they haven't had it yet.

### 2. Interval-Based Eligibility
For multi-dose series (like HepB or DTaP), the system checks the time elapsed since the **previous dose**.
*   *Constraint*: The engine respects the `minimumIntervalFromPreviousDose` (e.g., 28 days between Dose 1 and Dose 2).
*   *Result*: A child might be "overdue" based on age, but "not yet eligible" if the minimum interval hasn't passed since their last shot.

### 3. Maximum Age Cutoff
Some pediatric vaccines (like certain formulations of Hib or Rotavirus) cannot be administered after a specific age.
*   The engine automatically stops flagging these once the `maximumAge` is reached to prevent clinical errors.

### 4. Urgency Calculation
Urgency is determined by the `daysOverdue` (Current Date - Recommended Date):
| Urgency | Criteria |
| :--- | :--- |
| **CRITICAL** | Over 1 year (365 days) overdue **OR** within 30 days of the vaccine's Maximum Age. |
| **HIGH** | 180 to 364 days overdue. |
| **MEDIUM** | 90 to 179 days overdue. |
| **LOW** | 0 to 89 days overdue. |

---

## 📮 Postman Integration
A pre-configured Postman collection is available in the root directory: `ClinicConnect.postman_collection.json`.

**How to use:**
1. Open Postman.
2. Click **Import**.
3. Drag and drop the `ClinicConnect.postman_collection.json` file.
4. You will see a folder named **ClinicConnect API** with all available endpoints.

---

## 🛠️ Technical Deliverables

- **Codebase**: Fully modular Java 21 / Spring Boot project.
- **Documentation**: Logic documentation is embedded in this README and also available in the source code comments.
- **Public Repository**: https://github.com/mansoorranjha786/clinicconnect.git

---

## ✅ Acceptance Criteria Met
- **Overdue Scenarios**: Correctly identifies 18-month-old missing HepB, 5-year-old missing MMR, and 7-year-old missing DTaP.
- **Resiliency**: Implements Circuit Breakers and Fallback mechanisms for API stability.
- **Testing**: Over 70% coverage of the eligibility logic engine.