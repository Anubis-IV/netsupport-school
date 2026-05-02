# NetSupport School

A lightweight, LAN-based classroom management and exam delivery system. A tutor runs a server on their machine, discovers students on the local network, locks/unlocks their screens, pushes timed multiple-choice exams to them, watches answers arrive in real time, and downloads a PDF results report — all without any cloud dependency.

---

## 1. The Idea

NetSupport School solves a specific problem in computer-lab classrooms: a teacher needs to distribute an exam to every student's PC at the same time, prevent students from navigating away, collect answers automatically when time runs out, and view a graded results sheet — without installing heavyweight proctoring software or relying on an internet connection.

The flow is simple:

1. The **tutor** starts the backend server on the classroom's teacher PC.
2. The **student app** (pre-installed on each student PC) listens silently in the background.
3. The tutor clicks **Scan Students** in the browser-based Tutor Dashboard — this broadcasts a UDP discovery packet across the LAN.
4. Every student PC picks up the broadcast, connects back to the server over WebSocket, and registers itself.
5. The tutor can now **lock screens**, **unlock screens**, **request name entry**, and **start a timed exam** — all pushed in real time to every connected student.
6. As students select answers the tutor sees live score updates. When time expires (or the tutor stops the exam) answers are submitted automatically.
7. The tutor opens the **Exam Results** tab and downloads a colour-coded **PDF report**.

---

## 2. Architecture & Technology

### System Overview

```
┌─────────────────────────────────────────────────┐
│                  Tutor Machine                  │
│                                                 │
│  ┌──────────────┐     ┌────────────────────┐    │
│  │  Tutor UI    │────▶│  Backend Server   │    │
│  │ (index.html) │ WS  │  Spring Boot :8080 │    │
│  └──────────────┘     │  MySQL (Docker)    │    │
│                       └────────┬───────────┘    │
│                                │ UDP broadcast  │
└────────────────────────────────┼────────────────┘
                                 │ :9999/UDP
          ┌──────────────────────┼──────────────────────┐
          │                      │                      │
   ┌──────▼──────┐       ┌───────▼─────┐       ┌───────▼─────┐
   │ Student PC 1│       │ Student PC 2│       │ Student PC N│
   │ JavaFX App  │       │ JavaFX App  │       │ JavaFX App  │
   └─────────────┘       └─────────────┘       └─────────────┘
```

### Components

| Component | Technology | Role |
|---|---|---|
| **Backend Server** | Spring Boot 4, Java 21, Spring WebSocket, Spring Data JPA, MySQL, iTextPDF | Central hub: REST API, WebSocket broker, UDP broadcaster, PDF generation |
| **Tutor UI** | Vanilla HTML / CSS / JavaScript | Browser-based dashboard for the teacher; communicates via WebSocket and REST |
| **Designer UI** | Vanilla HTML / CSS / JavaScript | Separate browser tool for creating and editing exam question banks |
| **Student App** | JavaFX 21, Java 21, Java `HttpClient` WebSocket | Fullscreen client installed on student PCs; renders the lock screen and exam UI |

### Key Design Decisions

**UDP Discovery.** The backend broadcasts a `TUTOR_HERE` JSON packet (containing its own IP and port) to every broadcast address on all active LAN interfaces. The student app listens on UDP port 9999 and opens a WebSocket connection as soon as it receives this packet. This means students need no manual configuration — they connect automatically when the tutor scans.

**WebSocket Messaging.** All real-time control (lock, unlock, start exam, stop exam, answer submission, live score updates) flows over a single persistent WebSocket connection per client. Messages are typed JSON objects (`BaseMessage` with Jackson polymorphic dispatch), making it easy to add new message types without breaking existing ones.

**Persistence.** Exam definitions (questions, choices, correct answers) and student results are stored in MySQL via JPA/Hibernate. The schema is managed automatically by Hibernate's `ddl-auto: update`. Results survive a server restart.

**Live Answer Tracking.** Each time a student selects a choice, a `SUBMIT_ANSWERS` message with trigger `ANSWER_CHANGE` is sent to the server, which scores it and broadcasts a `STUDENT_SUBMITTED` update to all tutor sessions. This gives the tutor a live progress bar per student without waiting for final submission.

**Auto-Stop.** When all registered students have submitted (time-ended or tutor-stopped), the server automatically sends a `STOP_EXAM` to all remaining students and an `EXAM_COMPLETED` event to the tutor, cleanly closing the exam session.

---

## 3. Installation Guide

### Prerequisites

| Requirement | Where needed |
|---|---|
| Windows PC (student machines) | Student App |
| Docker Desktop installed | Backend Server (tutor machine) |
| A modern web browser (Chrome, Edge, Firefox) | Tutor UI & Designer UI |
| All machines on the same LAN / Wi-Fi subnet | Everywhere |

---

### 3a. Student App (Student PCs)

The student app is pre-built and distributed as a single JAR file. No JDK installation or compilation is required.

**Step 1 — Download the JAR**

Download `student-app.zip` from Google Drive:

> **[https://drive.google.com/file/d/11vnBZ4Tvl_cWavCquJeWahMOvqCk_SAL/view?usp=sharing](https://drive.google.com/file/d/11vnBZ4Tvl_cWavCquJeWahMOvqCk_SAL/view?usp=sharing)**

 Save the file anywhere on the student PC (e.g. `C:\NetSupport\student-app.zip`) then unzip the folder and run `app.exe`.

**Step 2 — Open UDP port 9999 in Windows Firewall**

The app listens for the tutor's discovery broadcast on UDP port 9999. Windows Firewall blocks this by default.

1. Open **Windows Defender Firewall with Advanced Security** (search for it in the Start menu).
2. Click **Inbound Rules** → **New Rule…**
3. Select **Port** → **Next**.
4. Select **UDP**, enter `9999` in *Specific local ports* → **Next**.
5. Select **Allow the connection** → **Next**.
6. Check all three profiles (Domain, Private, Public) → **Next**.
7. Give it a name such as `NetSupport UDP Discovery` → **Finish**.

**Step 3 — Run the app**

Double-click the `app.exe` file then the app starts silently (no window). It listens in the background and will show a fullscreen lock screen only when the tutor sends a command.


---

### 3b. Backend Server (Tutor Machine)

The server runs as a Docker container alongside a MySQL database. Docker Compose manages both services.

**Step 1 — Install Docker Desktop**

Download and install Docker Desktop for Windows from [https://www.docker.com/products/docker-desktop](https://www.docker.com/products/docker-desktop). Ensure it is running before proceeding.

**Step 2 — Create the Compose file**

Create a new file named `docker-compose.yml` anywhere on the tutor machine and paste the following content:

```yaml
services:
  mysql:
    image: mysql:8.3
    container_name: netsupport-mysql
    restart: always
    environment:
      MYSQL_DATABASE: netsupport_school
      MYSQL_USER: mahmoud
      MYSQL_PASSWORD: 12345678
      MYSQL_ROOT_PASSWORD: root
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost", "-u", "mahmoud", "-p12345678"]
      interval: 10s
      timeout: 5s
      retries: 5

  backend:
    image: mahmoudelbaz/netsupport-school-backend:latest
    container_name: netsupport-backend
    depends_on:
      mysql:
        condition: service_healthy
    ports:
      - "8080:8080"
      - "9999:9999/udp"   # CRITICAL: Required for student UDP discovery
    environment:
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/netsupport_school?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
      SPRING_DATASOURCE_USERNAME: mahmoud
      SPRING_DATASOURCE_PASSWORD: 12345678
      SPRING_PROFILES_ACTIVE: dev

volumes:
  mysql_data:
```

> **Important:** Port `9999/udp` must be mapped so that UDP discovery broadcasts from the backend reach the student PCs. Without this mapping, the Scan Students feature will not work.

**Step 3 — Start the server**

Open a terminal in the directory containing `docker-compose.yml` and run:

```bash
docker compose up -d
```

Docker will pull the backend image (on first run), start MySQL, wait for it to become healthy, then start the backend. This typically takes 30–60 seconds on first launch.

**Step 4 — Verify**

Open a browser and navigate to:

```
http://localhost:8080/api/exams
```

You should see a JSON response like `{"exams":[]}`. The server is ready.

**To stop the server:**

```bash
docker compose down
```

Data is preserved in the `mysql_data` Docker volume across restarts.

---

### 3c. Tutor UI (Teacher's Browser)

No installation is required. The Tutor UI is a static HTML application.

**Step 1 — Get the files**

Clone or download the repository from GitHub. The Tutor UI files are located in the `tutor-ui/` directory:

```
tutor-ui/
├── index.html
├── style.css
└── app.js
```

**Step 2 — Open the dashboard**

Simply open `tutor-ui/index.html` in any modern web browser (Chrome or Edge recommended). No web server is needed — the file can be opened directly from disk.

**Step 3 — Connect to the backend**

1. In the sidebar, confirm the **Server URL** field shows the correct address. If opening on the same machine as Docker, `localhost:8080` is correct. If on a different machine, replace it with the tutor PC's LAN IP (e.g. `192.168.1.10:8080`).
2. Click **Connect**. The status dot in the sidebar will turn green.

**Step 4 — Scan for students**

Once connected, click **Scan Students**. The backend broadcasts a UDP discovery packet. Any student PC running the student app on the same network will connect within a few seconds and appear as a card in the dashboard.

---

### 3d. Designer UI (Optional — Exam Creation)

A separate exam designer is available in the `designer-ui/` directory of the repository. Open `designer-ui/index.html` in a browser the same way as the Tutor UI. Point it to the same backend URL to create, edit, and delete exam question banks before a session.

---

## Typical Session Workflow

1. Start the Docker backend on the tutor machine (`docker compose up -d`).
2. Open `tutor-ui/index.html` in the browser and connect.
3. (Optional) Open `designer-ui/index.html` to create exams if none exist yet.
4. Ensure student PCs have `student-app.jar` running and UDP port 9999 is open.
5. Click **Scan Students** — students appear in the dashboard automatically.
6. Use **Lock All** to take control of student screens before the exam begins.
7. Click **Start Exam**, choose an exam and the target students, then click **Launch Exam**.
8. Monitor live answer progress on each student card.
9. When the exam ends (timer or manual stop), navigate to **Exam Results**, select the exam, and click **PDF Report** to download the graded report.

---

## License

MIT License — Copyright (c) 2026 Anubis IV. See `LICENSE` for full terms.
