
# 💻 Digital Complaint Management System

## 📘 Overview
The **Digital Complaint Management System (DCMS)** is a Java-based web application designed to streamline the process of submitting, managing, and resolving complaints within an institution.  
It provides dedicated dashboards for students, admins, and department staff — ensuring efficient handling, transparency, and accountability.

---

## 🚀 Features
- 🧑‍🎓 **Student Portal:** File and track complaints easily.  
- 🧑‍💼 **Admin Dashboard:** Manage users, assign complaints, and monitor resolution rates.  
- 🏢 **Department Dashboards:** Each department can view and resolve assigned complaints.  
- 📊 **Analytics:** View complaint statistics and status reports with charts.  
- 🔐 **Secure Login:** Authentication for students, departments, and admins.  
- ⚙️ **Auto Department Routing:** Complaints are automatically sent to the relevant department.

---

## 🧱 Technology Stack
| Component | Technology |
|------------|-------------|
| Frontend | HTML, CSS, JavaScript |
| Backend | Java 11 (Spring Boot) |
| Database | MySQL (via XAMPP Server) |
| Build Tool | Maven |
| IDE | IntelliJ IDEA / Eclipse |
| Connector | MySQL Connector (mysql-connector-j-9.4.0) |

---

## 🗂️ Project Folder Structure
\`\`\`
digital-complaint-system-full/
├── .vscode/
│   └── settings.json
├── backend/
│   ├── pom.xml
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   │   └── com/
│   │   │   │       └── dcs/
│   │   │   │           ├── DigitalComplaintSystemApplication.java
│   │   │   │           ├── config/
│   │   │   │           │   ├── DataInitializer.java
│   │   │   │           │   └── JdbcConnector.java
│   │   │   │           ├── controller/
│   │   │   │           │   ├── AdminController.java
│   │   │   │           │   ├── AuthController.java
│   │   │   │           │   ├── DepartmentController.java
│   │   │   │           │   ├── HomeController.java
│   │   │   │           │   └── StudentController.java
│   │   │   │           ├── model/
│   │   │   │           │   ├── Complaint.java
│   │   │   │           │   └── User.java
│   │   │   │           ├── repository/
│   │   │   │           │   ├── ComplaintRepository.java
│   │   │   │           │   └── UserRepository.java
│   │   │   │           └── service/
│   │   │   │               ├── ComplaintService.java
│   │   │   │               └── UserService.java
│   │   │   └── resources/
│   │   │       ├── application.properties
│   │   │       ├── database/
│   │   │       │   └── complaints.sql
│   │   │       ├── static/
│   │   │       │   ├── admin.html
│   │   │       │   ├── index.html
│   │   │       │   ├── login.html
│   │   │       │   ├── script.js
│   │   │       │   ├── student.html
│   │   │       │   └── styles.css
│   │   │       └── templates/
│   │   │           ├── admin_dashboard.html
│   │   │           ├── dept_dashboard.html
│   │   │           ├── login.html
│   │   │           ├── register.html
│   │   │           └── student_dashboard.html
│   │   └── test/
│   │       └── java/
│   └── target/
│       ├── digital-complaint-system-0.0.1-SNAPSHOT.jar
│       └── ...
└── database/
    └── (additional DB assets if any)
\`\`\`

---

## 🗃️ Database Details

**Database Name:** \`comp\`

### 1️⃣ \`users\`  
Stores information about all users (students, departments, admins).
| Field | Type | Description |
|--------|------|-------------|
| id | BIGINT | Primary Key |
| username | VARCHAR(50) | Unique username |
| email | VARCHAR(100) | User email |
| password | VARCHAR(255) | Encrypted password |
| role | ENUM('student','admin','department') | User role |
| department | VARCHAR(100) | Department name (if applicable) |

### 2️⃣ \`complaints\`  
Stores all complaints with their details and status.
| Field | Type | Description |
|--------|------|-------------|
| complaint_id | BIGINT | Primary Key |
| student_id | BIGINT | Foreign key to \`users(id)\` |
| department | VARCHAR(100) | Department assigned |
| subject | VARCHAR(100) | Complaint title |
| description | TEXT | Complaint details |
| status | ENUM('Pending','In Progress','Resolved') | Complaint status |
| date_created | DATETIME | Timestamp of complaint |
| last_updated | DATETIME | Last update time |

### 3️⃣ Department Tables  
Each department has its own complaint table for tracking progress.

#### \`academic_dept\`, \`facilities_dept\`, \`food_services_dept\`, \`technology_dept\`, \`transportation_dept\`, \`other_dept\`
| Field | Type | Description |
|--------|------|-------------|
| id | BIGINT | Primary Key |
| complaint_id | BIGINT | Foreign key to \`complaints(complaint_id)\` |
| issue | TEXT | Complaint details |
| status | ENUM('Pending','In Progress','Resolved') | Department’s resolution state |
| assigned_to | VARCHAR(100) | Department staff name |
| updated_at | DATETIME | Last update time |

---

## ⚙️ System Flow
1. **Student** logs in and submits a complaint.  
2. The system identifies the **relevant department** automatically.  
3. Complaint is stored in both:
   - \`complaints\` table  
   - Relevant department table  
4. **Department staff** view and update complaint status.  
5. **Admin** monitors all activity and generates reports.  
6. **Students** can track their complaint status in real-time.

---

## 🧰 Setup Instructions
1. **Clone Repository**
   \`\`\`
   git clone https://github.com/yourusername/digital-complaint-system-full.git
   \`\`\`

2. **Open in IDE**
   - Import the \`backend\` folder as a **Maven project** in IntelliJ or Eclipse.

3. **Configure MySQL**
   \`\`\`sql
   CREATE DATABASE comp;
   USE comp;
   SOURCE complaints.sql;
   \`\`\`

4. **Edit Configuration**
   - Open \`application.properties\`
   - Update database username/password:
     \`\`\`properties
     spring.datasource.url=jdbc:mysql://localhost:3306/comp
     spring.datasource.username=root
     spring.datasource.password=yourpassword
     \`\`\`

5. **Run Application**
   \`\`\`bash
   mvn spring-boot:run
   \`\`\`
   or run \`DigitalComplaintSystemApplication.java\` directly.

6. **Access the App**
   - Open browser → [http://localhost:8080](http://localhost:8080)

---

## 📈 Future Enhancements
- 📩 Email notifications for complaint updates  
- 📄 Admin report generation (PDF/Excel)  
- 🕒 Complaint priority and deadline system  
- 📱 Android/iOS mobile version  

---

## 🏁 License
This project is created for **academic purposes** and can be extended for educational or institutional use.
