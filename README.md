# Contract Management Backend System

An enterprise-ready backend RESTful application built with **Java 21**, **Spring Boot 3.3.5**, and **MySQL 8** for managing contract documents, enforcing state-machine approval workflows, and querying contract contents using deterministic keyword-based Information Retrieval.

Developed for the internship technical evaluation at **Seventh Ray Consulting and Technology Private Limited**.

---

## Table of Contents

1. [Project Introduction](#1-project-introduction)
2. [Project Objectives](#2-project-objectives)
3. [Technology Stack](#3-technology-stack)
4. [Application Architecture](#4-application-architecture)
5. [Folder Structure](#5-folder-structure)
6. [Database Setup (MySQL)](#6-database-setup-mysql)
7. [Database Configuration](#7-database-configuration)
8. [How to Run the Application](#8-how-to-run-the-application)
9. [How to Run Automated Tests](#9-how-to-run-automated-tests)
10. [Swagger / OpenAPI Documentation](#10-swagger--openapi-documentation)
11. [REST API Specifications](#11-rest-api-specifications)
12. [Sample API Requests and Responses](#12-sample-api-requests-and-responses)
13. [Contract Status Workflow](#13-contract-status-workflow)
14. [Document Parsing Engine](#14-document-parsing-engine)
15. [Keyword-Based Question Answering (Q&A)](#15-keyword-based-question-answering-qa)
16. [Known Limitations](#16-known-limitations)
17. [Possible Future Improvements](#17-possible-future-improvements)

---

## 1. Project Introduction

Legal and commercial agreements are typically stored as unstructured documents (PDF or DOCX). Manually verifying key terms (such as notice periods, salary figures, and joining dates) and tracking document review statuses across teams is time-consuming and prone to human error.

The **Contract Management Backend System** provides a reliable, secure, and developer-friendly REST backend that automates the lifecycle of contractual documents:
- Secure ingestion of PDF and DOCX files.
- Automated text extraction preserving table hierarchies and document clauses.
- Strict enforcement of approval state transitions (`DRAFT -> REVIEW -> APPROVED`).
- Zero-cost, privacy-preserving Q&A using NLP keyword search and synonym matching without external or paid AI APIs.

---

## 2. Project Objectives

- **Document Ingestion**: Accept PDF and DOCX documents with robust filename sanitization, file-type verification, and directory traversal defense.
- **Persistent Storage**: Save uploaded binaries into a structured local directory (`uploads/`) with metadata and extracted text stored in MySQL.
- **Workflow State Machine**: Strictly enforce non-reversible contract lifecycle progression (`DRAFT -> REVIEW -> APPROVED`).
- **Text Retrieval & Q&A**: Provide factual answers to questions regarding employee name, salary, notice period, location, and dates directly from document content without hallucinating.
- **Clean Architecture & Code Quality**: No Lombok (manual constructors, getters, setters), constructor-based dependency injection, centralized exception handling, and 100% automated test coverage.

---

## 3. Technology Stack

| Technology | Version | Purpose |
|---|---|---|
| **Java** | 21 (LTS) | Core programming language |
| **Spring Boot** | 3.3.5 | Application framework & dependency management |
| **Spring Web (MVC)** | 3.3.5 | REST API development with embedded Apache Tomcat |
| **Spring Data JPA** | 3.3.5 | Object-Relational Mapping (ORM) and repository abstractions |
| **Hibernate** | 6.5.x | JPA implementation for database operations |
| **MySQL** | 8.x | Production relational database |
| **H2 Database** | 2.2.x | In-memory database for automated testing |
| **Apache PDFBox** | 3.0.3 | Robust text extraction from PDF documents |
| **Apache POI** | 5.3.0 | Text and table extraction from DOCX documents |
| **Spring Validation** | 3.3.5 | Bean validation (`@Valid`, `@NotNull`, `@NotBlank`) |
| **Springdoc OpenAPI** | 2.6.0 | Interactive Swagger documentation UI |
| **JUnit 5 & Mockito** | 5.x | Unit and integration testing |
| **Maven Wrapper** | 3.9.9 | Cross-platform build automation |

### Technology Restrictions Met
- ❌ **No Lombok**: All constructors, getters, setters, and `toString` methods are written manually.
- ❌ **No Docker**: Runs natively using Java and local MySQL.
- ❌ **No Microservices**: Built as a cohesive, single-module layered monolith.
- ❌ **No Spring Security**: Kept simple and accessible without authentication walls.
- ❌ **No Paid External AI APIs**: Q&A operates via internal keyword & synonym scoring.
- ✔️ **Constructor-Based DI**: Clean, immutable dependency injection throughout.
- ✔️ **Maven Wrapper**: `mvnw` and `mvnw.cmd` included for zero-install builds.

---

## 4. Application Architecture

The system follows a classic **Layered Architecture**:

```
[Postman / Frontend Client]
          │
          ▼  HTTP (JSON / multipart)
   [ContractController]         <--- REST API Endpoints & Request Validation
          │
          ▼
   [ContractService]            <--- Business Logic & Transaction Management
   ┌──────┼────────────────────────────┬────────────────────────────┐
   ▼      ▼                            ▼                            ▼
[FileStorage]  [DocumentParser]   [QuestionAnswer]            [ContractRepository]
(uploads/ dir) (PDFBox 3 & POI 5) (Keyword & Synonym Engine)  (Spring Data JPA)
                                                                    │
                                                                    ▼
                                                            [MySQL / contracts]
```

### Data Flow Overview:
1. **Controller Layer**: Handles HTTP requests, triggers Bean Validation, and maps responses to DTOs.
2. **Service Layer**: Orchestrates file storage, text extraction, state transitions, and Q&A scoring.
3. **Repository Layer**: Interacts with MySQL via Hibernate ORM.
4. **Global Exception Handler**: Intercepts all custom and framework exceptions, returning uniform `ErrorResponse` objects.

---

## 5. Folder Structure

```
InternshipAssignment/
├── .mvn/
│   └── wrapper/
│       ├── maven-wrapper.jar
│       └── maven-wrapper.properties
├── docs/
│   ├── CODE_WALKTHROUGH.md          <-- Class-by-class technical breakdown
│   └── INTERVIEW_GUIDE.md           <-- Beginner guide & 35 interview Q&A
├── postman/
│   └── ContractManagement.postman_collection.json  <-- Complete Postman collection
├── samples/
│   ├── sample_corrupted.pdf         <-- Negative test sample (corrupted PDF)
│   ├── sample_employment_agreement.docx  <-- Valid DOCX contract sample
│   ├── sample_employment_agreement.pdf   <-- Valid PDF contract sample
│   └── sample_unsupported.txt       <-- Negative test sample (unsupported format)
├── src/
│   ├── main/
│   │   ├── java/com/example/contract/
│   │   │   ├── config/
│   │   │   │   └── OpenApiConfig.java
│   │   │   ├── controller/
│   │   │   │   └── ContractController.java
│   │   │   ├── dto/
│   │   │   │   ├── ContractResponse.java
│   │   │   │   ├── ContractSummaryResponse.java
│   │   │   │   ├── ErrorResponse.java
│   │   │   │   ├── QuestionRequest.java
│   │   │   │   ├── QuestionResponse.java
│   │   │   │   └── StatusUpdateRequest.java
│   │   │   ├── entity/
│   │   │   │   └── Contract.java
│   │   │   ├── enums/
│   │   │   │   └── ContractStatus.java
│   │   │   ├── exception/
│   │   │   │   ├── ContractNotFoundException.java
│   │   │   │   ├── DocumentParsingException.java
│   │   │   │   ├── GlobalExceptionHandler.java
│   │   │   │   ├── InvalidFileException.java
│   │   │   │   └── InvalidStatusTransitionException.java
│   │   │   ├── repository/
│   │   │   │   └── ContractRepository.java
│   │   │   ├── service/
│   │   │   │   ├── ContractService.java
│   │   │   │   ├── DocumentParserService.java
│   │   │   │   ├── FileStorageService.java
│   │   │   │   ├── KeywordQuestionAnswerService.java
│   │   │   │   └── QuestionAnswerService.java
│   │   │   └── ContractApplication.java
│   │   └── resources/
│   │       └── application.properties
│   └── test/
│       ├── java/com/example/contract/
│       │   ├── controller/
│       │   │   └── ContractControllerTest.java
│       │   ├── enums/
│       │   │   └── ContractStatusTest.java
│       │   ├── service/
│       │   │   ├── ContractServiceTest.java
│       │   │   ├── DocumentParserServiceTest.java
│       │   │   └── KeywordQuestionAnswerServiceTest.java
│       │   └── util/
│       │       ├── SampleDataGenerator.java
│       │       └── SampleFilesGenerationTest.java
│       └── resources/
│           ├── mockito-extensions/
│           │   └── org.mockito.plugins.MockMaker
│           └── application.properties
├── .gitignore
├── mvnw
├── mvnw.cmd
├── pom.xml
└── README.md
```

---

## 6. Database Setup (MySQL)

### Step 1: Open MySQL CLI or MySQL Workbench
Connect to your local MySQL server as `root` (or administrative user):
```bash
mysql -u root -p
```

### Step 2: Create the Database
```sql
CREATE DATABASE IF NOT EXISTS contract_db;
USE contract_db;
```

### Step 3: Schema Creation
Hibernate automatically creates and manages the `contracts` table on application startup via `spring.jpa.hibernate.ddl-auto=update`. However, if you wish to inspect or create the table manually, the schema definition is:

```sql
CREATE TABLE IF NOT EXISTS contracts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(255) NOT NULL,
    file_type VARCHAR(10) NOT NULL,
    status VARCHAR(20) NOT NULL,
    contract_description TEXT,
    extracted_text LONGTEXT,
    uploaded_at DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

#### Safe Migration for Existing Databases:
If you already have existing records in your MySQL table, execute this SQL command in MySQL Workbench to add the column without losing any data:
```sql
ALTER TABLE contracts ADD COLUMN contract_description TEXT NULL;
```
*(Hibernate will also automatically execute this alter command when `spring.jpa.hibernate.ddl-auto=update` is active).*

---

## 7. Database Configuration

The application reads its database credentials from environment variables with sensible local defaults in `src/main/resources/application.properties`:

```properties
# MySQL Database Connection with Fallback Defaults
spring.datasource.url=${DB_URL:jdbc:mysql://localhost:3306/contract_db?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true}
spring.datasource.username=${DB_USERNAME:root}
spring.datasource.password=${DB_PASSWORD:root}
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# Hibernate DDL and SQL Properties
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect

# File Upload Thresholds
spring.servlet.multipart.max-file-size=15MB
spring.servlet.multipart.max-request-size=20MB
file.upload-dir=uploads
```

### Setting Custom Credentials via Environment Variables (Optional)
On Windows (PowerShell):
```powershell
$env:DB_URL="jdbc:mysql://localhost:3306/contract_db"
$env:DB_USERNAME="my_user"
$env:DB_PASSWORD="my_password"
```

On Linux / macOS:
```bash
export DB_URL="jdbc:mysql://localhost:3306/contract_db"
export DB_USERNAME="my_user"
export DB_PASSWORD="my_password"
```

---

## 8. How to Run the Application

### Prerequisites:
- **Java Development Kit (JDK)**: Java 21 or Java 17.
- **MySQL Server 8.x**: Running locally on port `3306`.

### Option A: Using Maven Wrapper (Windows PowerShell / CMD)
```powershell
.\mvnw.cmd spring-boot:run
```

### Option B: Using Maven Wrapper (Linux / macOS / Git Bash)
```bash
./mvnw spring-boot:run
```

### Option C: Package as JAR and Run
```bash
./mvnw clean package -DskipTests
java -jar target/contract-management-0.0.1-SNAPSHOT.jar
```

Once running, Tomcat starts on port `8080`:
```
Started ContractApplication in 2.5 seconds (process running for 3.1)
```

---

## 9. How to Run Automated Tests

Automated tests run against an **in-memory H2 database** (`contract_test_db`). You do **not** need MySQL running to execute tests.

### Run All 44 Automated Tests:
```powershell
# Windows
.\mvnw.cmd test

# Linux / macOS
./mvnw test
```

### Run Specific Test Classes:
```powershell
# Status workflow tests
.\mvnw.cmd test -Dtest=ContractStatusTest

# PDF and DOCX document parser tests
.\mvnw.cmd test -Dtest=DocumentParserServiceTest

# Keyword Q&A matching tests
.\mvnw.cmd test -Dtest=KeywordQuestionAnswerServiceTest

# Service layer unit tests (Mockito)
.\mvnw.cmd test -Dtest=ContractServiceTest

# REST API controller integration tests (MockMvc)
.\mvnw.cmd test -Dtest=ContractControllerTest
```

### Expected Output:
```
[INFO] Results:
[INFO] 
[INFO] Tests run: 44, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```

---

## 10. Swagger / OpenAPI Documentation

Once the application is running, open your web browser and navigate to:

- **Swagger UI**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **OpenAPI JSON Spec**: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

You can view all endpoint contracts, schemas, and execute live API calls directly from the interactive Swagger UI interface.

---

## 11. REST API Specifications

The application exposes 5 core REST endpoints:

| # | Method | Endpoint | Description | Content-Type | Success Code |
|---|---|---|---|---|---|
| 1 | `POST` | `/api/contracts` | Upload PDF or DOCX contract document | `multipart/form-data` | `201 Created` |
| 2 | `GET` | `/api/contracts` | List all uploaded contracts (summaries) | `application/json` | `200 OK` |
| 3 | `GET` | `/api/contracts/{id}` | Retrieve complete contract details with extracted text | `application/json` | `200 OK` |
| 4 | `PUT` | `/api/contracts/{id}/status` | Update contract status (`DRAFT -> REVIEW -> APPROVED`) | `application/json` | `200 OK` |
| 5 | `POST` | `/api/contracts/{id}/questions` | Ask question about contract contents | `application/json` | `200 OK` |

---

## 12. Sample API Requests and Responses

### API 1: Upload Contract
**Request:**
- **URL**: `POST http://localhost:8080/api/contracts`
- **Body**: `multipart/form-data`
  - `file`: Select `samples/sample_employment_agreement.pdf`
  - `description`: *(Optional)* `Employment agreement for Rahul Sharma covering 30-day notice and Rs. 50,000 monthly CTC`

**Response (`201 Created`):**
```json
{
  "id": 1,
  "fileName": "sample_employment_agreement.pdf",
  "fileType": "PDF",
  "status": "DRAFT",
  "contractDescription": "Employment agreement for Rahul Sharma covering 30-day notice and Rs. 50,000 monthly CTC",
  "extractedText": "EMPLOYMENT AGREEMENT\n\nThis Employment Agreement is entered into by and between Seventh Ray Consulting and Technology Private Limited and the Employee.\n\n1. Employee Name: Rahul Sharma\n2. Role: Software Engineer\n3. Joining Date: 01 January 2026\n4. Working Location: Pune\n5. Salary: Rs. 50,000 per month\n6. Probation: 3 months\n7. Notice Period: 30 days\n8. Leave: 18 days of paid annual leave per calendar year...",
  "uploadedAt": "2026-11-20T18:00:00.123456"
}
```

---

### API 2: Get All Contracts
**Request:**
- **URL**: `GET http://localhost:8080/api/contracts`

**Response (`200 OK`):**
```json
[
  {
    "id": 1,
    "fileName": "sample_employment_agreement.pdf",
    "fileType": "PDF",
    "status": "DRAFT",
    "contractDescription": "Employment agreement for Rahul Sharma covering 30-day notice and Rs. 50,000 monthly CTC",
    "uploadedAt": "2026-11-20T18:00:00.123456"
  }
]
```
*(Notice that `extractedText` is excluded from the summary list response for network efficiency, while `contractDescription` is included).*

---

### API 3: Get Contract by ID
**Request:**
- **URL**: `GET http://localhost:8080/api/contracts/1`

**Response (`200 OK`):**
```json
{
  "id": 1,
  "fileName": "sample_employment_agreement.pdf",
  "fileType": "PDF",
  "status": "DRAFT",
  "contractDescription": "Employment agreement for Rahul Sharma covering 30-day notice and Rs. 50,000 monthly CTC",
  "extractedText": "EMPLOYMENT AGREEMENT\n...",
  "uploadedAt": "2026-11-20T18:00:00.123456"
}
```

**Non-Existent ID Response (`404 Not Found`):**
```json
{
  "timestamp": "2026-11-19T19:46:12.345678",
  "status": 404,
  "error": "Not Found",
  "message": "Contract with ID 999 was not found.",
  "path": "/api/contracts/999"
}
```

---

### API 4: Update Contract Status
**Request:**
- **URL**: `PUT http://localhost:8080/api/contracts/1/status`
- **Headers**: `Content-Type: application/json`
- **Body**:
```json
{
  "status": "REVIEW"
}
```

**Response (`200 OK`):**
```json
{
  "id": 1,
  "fileName": "sample_employment_agreement.pdf",
  "fileType": "PDF",
  "status": "REVIEW",
  "extractedText": "EMPLOYMENT AGREEMENT...",
  "uploadedAt": "2026-11-19T19:45:00.123456"
}
```

**Invalid Transition Error (`409 Conflict`):**
If you try to jump directly from `DRAFT` to `APPROVED` or move backward:
```json
{
  "timestamp": "2026-11-19T19:47:00.123456",
  "status": 409,
  "error": "Conflict",
  "message": "Invalid status transition from DRAFT to APPROVED. Allowed sequence is strictly: DRAFT -> REVIEW -> APPROVED.",
  "path": "/api/contracts/1/status"
}
```

---

### API 5: Ask Questions About a Contract

#### Example 5.1: Notice Period
**Request:**
- **URL**: `POST http://localhost:8080/api/contracts/1/questions`
- **Body**:
```json
{
  "question": "What is the notice period?"
}
```
**Response (`200 OK`):**
```json
{
  "contractId": 1,
  "question": "What is the notice period?",
  "answer": "30 days",
  "found": true
}
```

#### Example 5.2: Salary
**Request:**
- **Body**: `{"question": "What is the salary?"}`
**Response (`200 OK`):**
```json
{
  "contractId": 1,
  "question": "What is the salary?",
  "answer": "Rs. 50,000 per month",
  "found": true
}
```

#### Example 5.3: Work Location
**Request:**
- **Body**: `{"question": "Where is the work location?"}`
**Response (`200 OK`):**
```json
{
  "contractId": 1,
  "question": "Where is the work location?",
  "answer": "Pune",
  "found": true
}
```

#### Example 5.4: Unmentioned Topic (Bonus Policy)
**Request:**
- **Body**: `{"question": "What is the bonus policy?"}`
**Response (`200 OK`):**
```json
{
  "contractId": 1,
  "question": "What is the bonus policy?",
  "answer": "Information not found in the contract.",
  "found": false
}
```

---

## 13. Contract Status Workflow

The application implements a strict finite state machine defined inside `ContractStatus.java`:

```
┌─────────┐         Valid         ┌──────────┐         Valid         ┌────────────┐
│  DRAFT  │ ────────────────────> │  REVIEW  │ ────────────────────> │  APPROVED  │
└─────────┘                       └──────────┘                       └────────────┘
     │                                  │                                  │
     │ Disallowed (409)                 │ Disallowed (409)                 │ Disallowed (409)
     ├─ DRAFT -> APPROVED (skip)        ├─ REVIEW -> DRAFT (backward)      ├─ APPROVED -> REVIEW (backward)
     └─ DRAFT -> DRAFT (repeat)         └─ REVIEW -> REVIEW (repeat)       ├─ APPROVED -> DRAFT (backward)
                                                                           └─ APPROVED -> APPROVED (repeat)
```

### Transition Validation Rules:
1. `DRAFT -> REVIEW`: Allowed.
2. `REVIEW -> APPROVED`: Allowed.
3. `DRAFT -> APPROVED`: Rejected (HTTP 409 Conflict - cannot skip steps).
4. `REVIEW -> DRAFT`: Rejected (HTTP 409 Conflict - cannot move backward).
5. `APPROVED -> any`: Rejected (HTTP 409 Conflict - approved contracts are immutable).
6. Repeated status updates (e.g. `REVIEW -> REVIEW`): Rejected.

---

## 14. Document Parsing Engine

Document parsing is handled by `DocumentParserService`:

### PDF Parsing with Apache PDFBox 3.x
- Employs `Loader.loadPDF(File)` for safe memory allocation.
- Inspects `document.isEncrypted()`; encrypted PDFs are rejected immediately with HTTP 422.
- Uses `PDFTextStripper` with `setSortByPosition(true)` to extract characters according to physical page coordinates, ensuring columns and headings are preserved.

### DOCX Parsing with Apache POI 5.x
- Reads documents using `XWPFDocument`.
- Paragraphs are extracted sequentially.
- Tables are extracted row by row and cell by cell with `|` delimiters (e.g., `Salary | Rs. 50,000 per month`), preserving tabular key-value relationships.

### Text Normalization & Cleanup
- Unifies carriage returns (`\r\n` $\rightarrow$ `\n`).
- Strips excess whitespace and collapses multiple blank lines.
- Checks if the document contains non-blank text. If empty or unreadable, throws `DocumentParsingException` and triggers `FileStorageService.deleteFile()` to keep disk storage clean.

---

## 15. Keyword-Based Question Answering (Q&A)

The Q&A engine (`KeywordQuestionAnswerService`) answers factual queries without expensive external AI APIs using a 6-stage algorithmic pipeline:

1. **Query Preprocessing**:
   - Converts the question to lowercase and strips non-alphanumeric punctuation.
   - Filters out generic stop words (*what, is, the, when, where, of, at, in*).
2. **Domain Synonym Expansion**:
   - Expands extracted keywords using a built-in synonym map:
     - `salary`: *pay, compensation, remuneration, ctc, stipend, wage*
     - `notice period`: *notice, resignation*
     - `joining date`: *start date, commencement date, date of joining*
     - `work location`: *location, workplace, office, job location*
     - `employee name`: *candidate name, name, employee*
3. **Document Line Segmentation**:
   - Divides the extracted contract text into distinct lines and sentences.
4. **Relevance Scoring Engine**:
   - **Label Matches**: If a line begins with `Label: Value` (e.g. `Notice Period: 30 days`), matching the label yields **+15 points** (exact) or **+10 points** (partial).
   - **Multi-Word Phrases**: Phrases like `"notice period"` or `"working location"` yield **+8 points**.
   - **Individual Keywords**: Each hit adds **+3 points**.
5. **Precise Answer Extraction**:
   - For key-value patterns (`Key: Value`, `Key - Value`, or `Key | Value`), the service isolates and returns the clean **Value** (e.g. `Rs. 50,000 per month`).
   - For unstructured sentences, the most relevant sentence is returned.
6. **Hallucination Prevention**:
   - If the highest score is below a confidence score threshold of 3, the engine returns `found: false` with `"Information not found in the contract."`

---

## 16. Known Limitations

1. **Scanned Images (OCR)**: Scanned PDFs that contain image-only bitmaps (without embedded font text) cannot be read by Apache PDFBox without an OCR engine like Tesseract.
2. **Complex Semantic Inferences**: Because it is a deterministic keyword & synonym engine, questions requiring multi-hop logical deduction (e.g. *"If I earn 50k and receive a 10% raise, what is my new salary?"*) cannot be calculated.
3. **Authentication**: The system currently runs without user login or authentication, as per project guidelines.

---

## 17. Possible Future Improvements

1. **Optical Character Recognition (OCR)**: Integrate Tesseract via Tess4J to process scanned physical contracts.
2. **JWT Authentication & RBAC**: Add Spring Security to restrict status approval (`REVIEW -> APPROVED`) to users with `ROLE_MANAGER`.
3. **Audit Trail**: Use Spring Data JPA Auditing (`@CreatedBy`, `@LastModifiedDate`) to track who changed a contract's status.
4. **Full-Text Search Indexing**: Implement Elasticsearch or PostgreSQL full-text search with BM25 ranking for multi-gigabyte document archives.
5. **Semantic Embeddings**: Integrate an offline local vector model (such as ONNX Runtime with BERT embeddings) for vector similarity search.
