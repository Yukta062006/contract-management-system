# Internship Technical Interview & Viva Guide

**Project**: Contract Management Backend System  
**Organization**: Seventh Ray Consulting and Technology Private Limited  
**Author**: Internship Candidate  

---

## 1. Project Introduction

The **Contract Management Backend System** is an enterprise-grade RESTful application developed using **Java 21** and **Spring Boot 3.3.5**. It enables organizations to streamline the ingestion, lifecycle approval, and textual interrogation of contractual documents (specifically PDF and DOCX formats).

### Key Business Problems Solved:
1. **Document Storage & Metadata Association**: Securely persists binary documents on the local filesystem while linking their metadata (file name, type, storage path, upload timestamp) in a relational MySQL database.
2. **Automated Content Extraction**: Automatically extracts plain text from PDF documents using Apache PDFBox 3.x and from DOCX documents using Apache POI 5.x (including tables and structured clauses).
3. **Strict Lifecycle Governance**: Enforces a non-reversible state machine: `DRAFT -> REVIEW -> APPROVED`. Invalid status jumps, repeats, or retrospective edits on approved contracts are rejected.
4. **Keyword-Based Question Answering (Q&A)**: Enables users to query contracts in natural English without sending proprietary data to paid external AI APIs. It utilizes keyword extraction, stop-word removal, domain synonym expansion, and line-scoring algorithms.

---

## 2. Overall Architecture

The application adopts a **Layered Architecture** adhering to Separation of Concerns:

```
[ Client / Postman ]
        │
        ▼ (HTTP REST Requests)
[ Controller Layer ]     com.example.contract.controller
        │                - Exposes endpoints, handles HTTP status codes & Bean validation.
        ▼
[ Service Layer ]        com.example.contract.service
        │                - Contains business logic, file storage, parsing, Q&A, and transaction boundaries.
        ├── FileStorageService (Local filesystem IO)
        ├── DocumentParserService (Apache PDFBox & Apache POI)
        └── QuestionAnswerService (NLP keyword matching & synonym scoring)
        │
        ▼
[ Repository Layer ]     com.example.contract.repository
        │                - Spring Data JPA repositories communicating with Hibernate ORM.
        ▼
[ Database Layer ]       MySQL 8 (Runtime) / H2 In-Memory (Automated Testing)
```

---

## 3. End-to-End Execution Flow

### Scenario A: Contract Upload (`POST /api/contracts`)
1. **Client Submission**: Client sends a `multipart/form-data` request with a file attached under the key `file` and an optional `description` parameter.
2. **Controller Validation**: `ContractController` receives `MultipartFile` and the `description` string.
3. **Format & Integrity Check**: `ContractService` validates that the file is non-empty, checks that the extension is `.pdf` or `.docx`, and verifies there is no directory traversal sequence in the filename.
4. **Filesystem Storage**: `FileStorageService` generates a unique name using `UUID.randomUUID()` and saves the binary file into the local `uploads/` folder.
5. **Text Extraction**: `DocumentParserService` routes the file to either Apache PDFBox (if PDF) or Apache POI (if DOCX). Paragraphs and tables are extracted and normalized. If the file is unreadable, empty, or corrupted, the stored file is deleted from disk and a `DocumentParsingException` (HTTP 422) is raised.
6. **Entity Persistence**: A `Contract` entity is constructed with initial status `ContractStatus.DRAFT`, user-provided `contractDescription`, and saved via `ContractRepository.save()`.
7. **Response**: The persisted entity is converted to a `ContractResponse` DTO and returned with HTTP `201 Created`.

### Scenario B: Status Transition (`PUT /api/contracts/{id}/status`)
1. Client sends JSON payload: `{"status": "REVIEW"}`.
2. `ContractController` validates the payload with `@Valid`.
3. `ContractService` fetches the contract by ID from MySQL or throws `ContractNotFoundException` (HTTP 404).
4. `contract.getStatus().validateTransition(targetStatus)` is invoked:
   - If current is `DRAFT` and target is `REVIEW` $\rightarrow$ Allowed.
   - If current is `REVIEW` and target is `APPROVED` $\rightarrow$ Allowed.
   - Any other combination (skipping from `DRAFT` to `APPROVED`, repeating status, or modifying `APPROVED`) throws `InvalidStatusTransitionException` (HTTP 409).
5. The updated entity is saved in MySQL and returned as a `ContractResponse`.

### Scenario C: Asking a Question (`POST /api/contracts/{id}/questions`)
1. Client sends JSON payload: `{"question": "What is the notice period?"}`.
2. `ContractService` retrieves the contract and retrieves `extractedText`.
3. `KeywordQuestionAnswerService.answerQuestion()` executes:
   - Removes stop-words (*what, is, the*) leaving `["notice", "period"]`.
   - Expands terms with the synonym dictionary (includes *notice period*, *resignation*).
   - Loops over all document lines, calculating a relevance score. Lines matching labels like `Notice Period:` receive high boost.
   - Extracts the answer value (`30 days`).
   - If score $\ge 3$, returns `found: true` with the answer. If no match exceeds threshold, returns `found: false` with `"Information not found in the contract."`

---

## 4. Why We Chose These Technologies

### Why Java 21?
- Modern Long-Term Support (LTS) release of the Java platform.
- Provides improved pattern matching, switch expressions, and virtual threads capabilities.
- Strong static typing and memory management ideal for processing large document streams.

### Why Spring Boot 3.x?
- Rapid bootstrapping with convention-over-configuration.
- Out-of-the-box embedded Tomcat server eliminating the need for standalone servlet containers.
- First-class dependency injection, transaction management, and centralized exception handling.

### Why Spring Data JPA & Hibernate?
- Eliminates repetitive JDBC boilerplate code.
- Auto-generates CRUD queries at runtime from repository interfaces.
- Manages dirty checking, entity lifecycle callbacks (`@PrePersist`), and schema updates (`ddl-auto=update`).

### Why MySQL 8?
- Robust, reliable, open-source relational database.
- Supports `LONGTEXT` storage capable of holding multi-megabyte extracted text strings.
- Standard in production enterprise environments.

### Why H2 Database for Testing?
- Runs completely in-memory, zero installation needed.
- Allows test suites to run in CI/CD pipelines or clean developer machines without requiring a running MySQL server.

### Why Apache PDFBox 3.x & Apache POI 5.x?
- Industry standards for programmatic document parsing in the Java ecosystem.
- PDFBox 3.x introduced simplified thread-safe loading (`Loader.loadPDF`) and better memory handling.
- Apache POI provides full access to the Office Open XML standard for extracting paragraphs and nested table cells.

---

## 5. Architectural Deep Dives & Important Concepts

### Why No Lombok?
Lombok uses annotation processing to generate byte-code at compile time. In this project, writing explicit constructors, getters, and setters:
- Avoids annotation-processor conflicts across various IDEs or new JDK versions.
- Makes code 100% transparent and beginner-friendly for code reviewers and interviewers.
- Ensures full control over encapsulation and debugging.

### How Does Constructor-Based Dependency Injection Work?
Instead of field injection (`@Autowired private MyService myService;`), we use constructor injection:
```java
public ContractService(ContractRepository contractRepository, ...) {
    this.contractRepository = contractRepository;
}
```
**Benefits**:
1. Dependencies are marked `final`, ensuring immutability.
2. Prevents `NullPointerException` since objects cannot be instantiated without required dependencies.
3. Simplifies unit testing because mocks can be passed directly via constructor without reflection frameworks.

### How is Path Traversal Prevented?
Attackers can upload a file named `../../Windows/System32/evil.exe` to write outside the upload directory. We prevent this by:
1. Sanitizing the path using Spring's `StringUtils.cleanPath()`.
2. Rejecting any file containing `..`, `/`, or `\`.
3. Resolving the destination path and asserting: `targetPath.startsWith(uploadDirectory)`.

### How Does the Fallback File Cleanup Work?
When an uploaded file is saved to the disk, document parsing occurs immediately after. If parsing fails (due to a corrupt PDF or password protection), keeping the file would leak storage. Our service catches `DocumentParsingException`, invokes `fileStorageService.deleteFile(path)`, and then rethrows the exception, guaranteeing the filesystem remains clean.

---

## 6. 35 Technical Interview Questions and Answers

### Section 1: Java & Core Concepts

#### Q1: What are the benefits of using Java 21 in this project?
**Answer**: Java 21 is a Long Term Support release. It offers language features such as enhanced pattern matching for switch expressions (which we used in `ContractStatus.canTransitionTo`), improved garbage collection algorithms, and performance enhancements.

#### Q2: What is the difference between an Exception and an Error in Java?
**Answer**: Both inherit from `Throwable`. `Error` (e.g., `OutOfMemoryError`, `StackOverflowError`) indicates catastrophic system conditions that applications should not attempt to handle. `Exception` indicates recoverable conditions that can be handled using `try-catch` blocks.

#### Q3: Why did you use unchecked exceptions (`RuntimeException`) instead of checked exceptions?
**Answer**: Custom exceptions like `ContractNotFoundException` and `DocumentParsingException` extend `RuntimeException`. In modern Spring applications, unchecked exceptions allow transactions to roll back automatically (by default `@Transactional` rolls back on `RuntimeException`) and prevent verbose `throws` clauses in method signatures across layers.

#### Q4: Why are strings immutable in Java?
**Answer**: Security, thread-safety, caching (string pool), and hash code stability. Because strings cannot change after creation, passing file paths and SQL parameters is safe from concurrent alteration.

#### Q5: How did you implement encapsulation in the `Contract` entity?
**Answer**: All fields (`id`, `fileName`, `filePath`, etc.) are declared `private`. Access is restricted to explicit public getters and setters. Status transitions are guarded by validation methods.

---

### Section 2: Spring Boot & Framework Architecture

#### Q6: What does `@SpringBootApplication` do behind the scenes?
**Answer**: It is a composite meta-annotation that includes:
1. `@SpringBootConfiguration`: Registers the class as a configuration source.
2. `@EnableAutoConfiguration`: Automatically configures beans based on classpath dependencies (e.g. setting up Tomcat and DataSource).
3. `@ComponentScan`: Scans the current package (`com.example.contract`) and all sub-packages for `@Component`, `@Service`, `@Repository`, and `@RestController`.

#### Q7: What is Dependency Injection (DI) and Inversion of Control (IoC)?
**Answer**: IoC is a design principle where the control of object creation and lifecycle is inverted from the developer's code to a container/framework. Dependency Injection is the mechanism used to implement IoC: the Spring container creates beans and injects them into dependent classes (e.g. injecting `ContractRepository` into `ContractService`).

#### Q8: Why prefer constructor injection over field injection (`@Autowired`)?
**Answer**: Constructor injection makes dependencies explicit, allows fields to be declared `final` (immutable), prevents partial instantiation in unit tests, and avoids circular dependency traps.

#### Q9: What is the difference between `@Component`, `@Service`, `@Repository`, and `@Controller`?
**Answer**: `@Component` is the generic stereotype for any Spring-managed bean. `@Service`, `@Repository`, and `@Controller` are specialized stereotypes:
- `@Service`: Denotes business logic layer beans.
- `@Repository`: Denotes persistence layer beans and translates database exceptions into Spring's `DataAccessException` hierarchy.
- `@Controller` / `@RestController`: Handles HTTP requests and renders responses / JSON.

#### Q10: How does Spring Boot handle multipart file uploads?
**Answer**: Spring Boot integrates `StandardServletMultipartResolver`. When a `multipart/form-data` request arrives, it parses the boundary delimiters, extracts form fields and files, and wraps uploaded binaries into `MultipartFile` instances passed to controller methods.

#### Q11: Where are file upload limits configured?
**Answer**: In `application.properties`:
```properties
spring.servlet.multipart.max-file-size=15MB
spring.servlet.multipart.max-request-size=20MB
```
If an upload exceeds 15MB, Spring throws `MaxUploadSizeExceededException`, which our `GlobalExceptionHandler` converts to HTTP `413 Payload Too Large`.

---

### Section 3: REST API & Exception Handling

#### Q12: What HTTP status codes are used in this system and why?
**Answer**:
- `200 OK`: Successful retrieval or update (`GET`, `PUT`, Q&A).
- `201 Created`: Successful contract upload creation (`POST /api/contracts`).
- `400 Bad Request`: Validation failure (empty file, invalid JSON, unsupported format).
- `404 Not Found`: Contract ID does not exist in MySQL.
- `409 Conflict`: Disallowed status transition (e.g. DRAFT $\rightarrow$ APPROVED).
- `413 Payload Too Large`: Uploaded file exceeds 15MB.
- `422 Unprocessable Entity`: Document cannot be parsed or contains zero readable text.
- `500 Internal Server Error`: Unhandled server exception.

#### Q13: How does `@RestControllerAdvice` work?
**Answer**: It acts as an interceptor around all `@RestController` beans using Spring AOP (Aspect-Oriented Programming). When any controller throws an exception, Spring catches it and invokes the matching `@ExceptionHandler` method, returning a structured `ErrorResponse` DTO instead of an ugly HTML error page or stack trace.

#### Q14: Why is it bad practice to expose database entities directly in API responses?
**Answer**:
1. **Security**: Entities might contain sensitive internal fields.
2. **Coupling**: Changing database column names or schemas breaks external clients.
3. **Performance**: Exposing full entities can cause accidental N+1 queries or serialize huge columns (e.g. `extracted_text` in list responses). DTOs like `ContractSummaryResponse` ensure only required data is transmitted.

#### Q15: How does Bean Validation work in the project?
**Answer**: We use Jakarta Bean Validation annotations such as `@NotNull` on `StatusUpdateRequest` and `@NotBlank` on `QuestionRequest`. In the controller, the `@Valid` annotation triggers validation; if violated, `MethodArgumentNotValidException` is caught by `GlobalExceptionHandler` and returned as HTTP 400.

---

### Section 4: Database & Hibernate / JPA

#### Q16: What is the difference between JPA and Hibernate?
**Answer**: JPA (Jakarta Persistence API) is a specification/interface defining standards for Object-Relational Mapping (ORM) in Java. Hibernate is the actual concrete implementation of that specification.

#### Q17: What does `@Enumerated(EnumType.STRING)` do?
**Answer**: By default, JPA maps enums to ordinal integers (0, 1, 2). If you reorder enum values or add a new one, existing database records become corrupted. `EnumType.STRING` stores the literal name (`"DRAFT"`, `"REVIEW"`), preserving data integrity.

#### Q18: What is the purpose of `@Lob` and `columnDefinition = "LONGTEXT"` on `extractedText`?
**Answer**: Standard VARCHAR or TEXT columns in MySQL have a maximum length of 65,535 bytes (~64 KB). Many legal agreements are hundreds of pages long. Using `@Lob` with `LONGTEXT` allows storing strings up to 4 GB.

#### Q19: What is `@Transactional` and how does dirty checking work?
**Answer**: `@Transactional` binds method execution to a database transaction. When reading entities within a transaction, Hibernate keeps a snapshot in its Persistence Context (first-level cache). If entity state changes before commit, Hibernate automatically detects changes ("dirty checking") and generates SQL `UPDATE` statements without needing manual SQL.

#### Q20: Why did you use H2 in automated tests instead of MySQL?
**Answer**: To make the project completely portable. Tests run seamlessly without requiring interviewers or CI/CD pipelines to have a running MySQL instance, credentials, or network access.

---

### Section 5: Document Parsing (PDFBox & Apache POI)

#### Q21: How does Apache PDFBox 3.x differ from version 2.x?
**Answer**: In PDFBox 2.x, documents were loaded via `PDDocument.load(File)`. In PDFBox 3.x, loading was redesigned using the `Loader` class (`Loader.loadPDF(File)`) to improve memory management, thread safety, and resource cleanup.

#### Q22: What happens if a password-protected PDF is uploaded?
**Answer**: Our `DocumentParserService` checks `document.isEncrypted()`. If true, it immediately throws `DocumentParsingException("Password-protected or encrypted PDFs are not supported.")` which translates to HTTP 422.

#### Q23: How did you extract table contents in DOCX files?
**Answer**: In Apache POI, paragraphs alone do not capture table structures. We iterated over `document.getTables()`, then iterated over each row and cell, appending cell text delimited by `|` to maintain the tabular key-value relationship (e.g. `Salary | Rs. 50,000 per month`).

#### Q24: What is text normalization and why is it needed?
**Answer**: Different operating systems use different newline characters (`\r\n` on Windows, `\n` on Linux/Mac). Normalization unifies line breaks to `\n`, cleans excess blank lines, and trims whitespace so subsequent keyword matching and line parsing behave consistently.

---

### Section 6: Question Answering (Q&A) Engine

#### Q25: How does the keyword-based Q&A engine work without external AI?
**Answer**: It uses an information retrieval scoring pipeline:
1. **Normalization**: Converts the question to lowercase and strips punctuation.
2. **Stop-word filtering**: Discards non-informative words (*what, is, the, where, when*).
3. **Synonym expansion**: Checks a dictionary mapping concepts (e.g. *salary* expands to *pay, compensation, ctc, stipend*).
4. **Scoring**: Evaluates each document line. Lines containing structured labels like `Salary:` or `Notice Period:` get +15 bonus points; multi-word phrase matches get +8; keyword hits get +3.
5. **Extraction**: If a line is formatted as `Key: Value`, only the `Value` portion is extracted.
6. **Threshold verification**: If the maximum score is below 3, the service returns `found: false` rather than guessing.

#### Q26: Why is hallucination prevention critical in contract management?
**Answer**: In legal and financial systems, incorrect answers (e.g., claiming a notice period is 15 days when it is 30 days) create contractual liabilities. A deterministic scoring engine ensures that if information is absent, the system explicitly reports `found: false`.

#### Q27: How would you scale this Q&A engine for thousands of large documents?
**Answer**:
1. Store text chunks in a dedicated search engine like Elasticsearch or Apache Solr using BM25 scoring.
2. Alternatively, compute vector embeddings (using a local embedding model like sentence-transformers) and store them in a vector database (e.g., pgvector, Milvus) for semantic similarity search.

---

### Section 7: Status Workflow & State Machine

#### Q28: How is the contract status workflow designed?
**Answer**: The lifecycle follows: `DRAFT -> REVIEW -> APPROVED`.
Transitions are validated inside the `ContractStatus` enum itself via `canTransitionTo()` and `validateTransition()`.

#### Q29: What happens if an API user tries to move directly from DRAFT to APPROVED?
**Answer**: `ContractStatus.validateTransition(APPROVED)` checks `canTransitionTo(APPROVED)`. Because `DRAFT` can only transition to `REVIEW`, it throws `InvalidStatusTransitionException`, which `GlobalExceptionHandler` converts to an HTTP `409 Conflict` response with an explanatory message.

#### Q30: Can an APPROVED contract be moved back to REVIEW or DRAFT?
**Answer**: No. `ContractStatus.APPROVED` permits no transitions. Attempting any transition throws an exception explaining that approved contracts are immutable.

---

### Section 8: Testing, Security & Production Readiness

#### Q31: What levels of testing were implemented?
**Answer**:
- **Unit Tests**:
  - `ContractStatusTest`: Tests valid and invalid transitions, repeats, skips, and post-approval immutability.
  - `KeywordQuestionAnswerServiceTest`: Tests exact questions, synonyms, unknown questions, and edge cases.
  - `DocumentParserServiceTest`: Tests valid PDF, valid DOCX, corrupted PDF, and empty files.
  - `ContractServiceTest`: Uses Mockito to verify service logic and file cleanup on parsing failure.
- **Web Integration Tests**:
  - `ContractControllerTest`: Uses `@WebMvcTest` and `MockMvc` to verify HTTP status codes, routing, and JSON serialization.

#### Q32: What is Mockito and how does `@MockBean` work?
**Answer**: Mockito creates simulated mock objects that mimic the behavior of real dependencies. `@MockBean` adds Mockito mocks to the Spring `ApplicationContext`, allowing controllers to be tested in isolation from databases or file systems.

#### Q33: How does the application protect against malicious file uploads?
**Answer**:
1. Validates extensions (`.pdf` and `.docx` only).
2. Generates random UUID filenames to avoid overwriting existing files.
3. Checks for directory traversal patterns (`..`).
4. Rejects empty files and files exceeding 15MB.

#### Q34: How are database credentials securely passed in production?
**Answer**: Using Spring Boot property placeholders with environment variable fallbacks:
`spring.datasource.username=${DB_USERNAME:root}`
In staging or production environments, Docker or Kubernetes injects environment variables without hardcoding passwords in source files.

#### Q35: If you had another sprint, what improvements would you implement?
**Answer**:
1. Add OCR support (using Tesseract via Tess4J) for scanned image-only PDFs.
2. Integrate Spring Security with JWT tokens for role-based access control (e.g., only Managers can approve contracts).
3. Introduce full-text search indexing with Elasticsearch.
4. Add audit logging (`created_by`, `last_modified_by`) using Spring Data JPA Auditing.
