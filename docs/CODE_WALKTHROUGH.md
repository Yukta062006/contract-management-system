# Contract Management Backend System - Detailed Code Walkthrough

This document provides a class-by-class walkthrough of the **Contract Management Backend System**, designed for **Seventh Ray Consulting and Technology Private Limited**. It details each class's responsibility, key annotations, internal logic, and interactions with other system components.

---

## 1. Architectural Overview & Request Lifecycle

The application adheres to a clean layered architecture:

```
[Postman / Client]
        │  (HTTP / JSON or multipart/form-data)
        ▼
[ContractController]  <-- Input validation, Swagger docs, HTTP response mapping
        │
        ▼
[ContractService]     <-- Business logic, orchestration, transaction management
   ┌────┼───────────────────────────┬──────────────────────────┐
   ▼    ▼                           ▼                          ▼
[FileStorageService]  [DocumentParserService]  [QuestionAnswerService]  [ContractRepository]
(Saves to uploads/)   (PDFBox 3.x / POI 5.x)   (Keyword & Synonym Q&A)   (Spring Data JPA)
                                                                               │
                                                                               ▼
                                                                        [MySQL / contracts]
```

When an exception occurs anywhere in the stack, `GlobalExceptionHandler` intercepts it, maps it to an HTTP status code, and produces a standardized `ErrorResponse` JSON.

---

## 2. Core Domain & Entity

### `com.example.contract.entity.Contract`
- **Responsibility**: Represents a row in the `contracts` table. It holds metadata (`id`, `fileName`, `filePath`, `fileType`, `status`, `uploadedAt`), user-provided description (`contractDescription`), and the full extracted text (`extractedText`).
- **Key Annotations**:
  - `@Entity`: Marks this class as a JPA entity managed by Hibernate.
  - `@Table(name = "contracts")`: Maps the entity to the `contracts` table in MySQL.
  - `@Id` and `@GeneratedValue(strategy = GenerationType.IDENTITY)`: Designates `id` as the primary key generated using MySQL's `AUTO_INCREMENT`.
  - `@Enumerated(EnumType.STRING)`: Stores the enum value as a readable string (`DRAFT`, `REVIEW`, `APPROVED`) rather than an ordinal integer in the database.
  - `@Column(name = "contract_description", columnDefinition = "TEXT")`: Stores meaningful user-entered descriptions in MySQL.
  - `@Lob` & `@Column(name = "extracted_text", columnDefinition = "LONGTEXT")`: Specifies that `extracted_text` can store extensive text content (up to 4 GB in MySQL), preventing truncation issues with long contracts.
  - `@PrePersist`: Lifecycle callback that automatically populates `uploadedAt` with `LocalDateTime.now()` and defaults `status` to `DRAFT` if null before the entity is first persisted.
- **Interactions**: Managed by `ContractRepository`; created and updated by `ContractService`; mapped to DTOs for client responses.

---

## 3. Workflow & Business Rules

### `com.example.contract.enums.ContractStatus`
- **Responsibility**: Encapsulates the contract lifecycle states and enforces transition business rules.
- **Values**: `DRAFT`, `REVIEW`, `APPROVED`.
- **Key Methods**:
  - `canTransitionTo(ContractStatus targetStatus)`: Uses a modern Java `switch` expression to verify whether a requested transition is valid.
    - `DRAFT -> REVIEW` (allowed)
    - `REVIEW -> APPROVED` (allowed)
    - `APPROVED -> any` (disallowed)
  - `validateTransition(ContractStatus targetStatus)`: Enforces business constraints:
    - Rejects null target status.
    - Rejects repeated updates (`DRAFT -> DRAFT`, `REVIEW -> REVIEW`).
    - Rejects modifications once `APPROVED`.
    - Throws `InvalidStatusTransitionException` with an informative error message if any rule is violated.
- **Why this design?**: Keeps business logic strictly inside domain boundaries instead of scattering validation across controllers or multiple service methods.

---

## 4. Repository Layer

### `com.example.contract.repository.ContractRepository`
- **Responsibility**: Provides CRUD database operations for `Contract` entities.
- **Key Annotations**:
  - `@Repository`: Spring component annotation indicating database abstraction.
  - Extends `JpaRepository<Contract, Long>`: Inherits out-of-the-box methods such as `save()`, `findById()`, `findAll()`, `delete()`, and pagination support without writing custom SQL.
- **Interactions**: Injected into `ContractService`.

---

## 5. Service Layer

### `com.example.contract.service.FileStorageService`
- **Responsibility**: Manages the local filesystem directory where uploaded documents are preserved.
- **Key Logic**:
  - **Path Traversal Protection**: Inspects file names using `StringUtils.cleanPath()` and verifies that names do not contain relative path sequences (`..` or directory separators).
  - **Unique Filename Generation**: Prefixes every file with `UUID.randomUUID().toString() + "_"` to prevent accidental overwrites when users upload documents with identical names.
  - **Bounds Verification**: Ensures the resolved `Path` is located inside the configured root upload directory.
  - **Cleanup Method (`deleteFile`)**: Safely removes a file if parsing or database persistence fails, ensuring the filesystem does not accumulate orphaned files.
- **Key Annotations**:
  - `@Service`: Registers the class as a Spring-managed service bean.
  - `@Value("${file.upload-dir:uploads}")`: Reads the configured storage directory from `application.properties`, falling back to `uploads`.
  - `@PostConstruct`: Automatically creates the target directory on application bootstrap if it does not yet exist.

### `com.example.contract.service.DocumentParserService`
- **Responsibility**: Inspects and extracts textual content from PDF and DOCX files.
- **Technologies Used**:
  - **Apache PDFBox 3.x**:
    - Uses `Loader.loadPDF(File)` to load the document safely into memory.
    - Checks `document.isEncrypted()` to detect password protection.
    - Employs `PDFTextStripper` with `setSortByPosition(true)` to extract text in visual reading order.
  - **Apache POI 5.x**:
    - Reads DOCX files using `XWPFDocument`.
    - Iterates through paragraphs (`document.getParagraphs()`) to collect standard text.
    - Iterates through tables (`document.getTables()`), extracting cell contents row-by-row and separating them with `|` delimiters so tabular terms (e.g. `Salary | Rs. 50,000 per month`) are retained accurately.
- **Common Logic**:
  - `normalizeText(String raw)`: Replaces CRLF line breaks with standard Unix `\n`, cleans trailing spaces, and eliminates consecutive blank lines.
  - Validates that extracted text is not empty or blank; otherwise, throws `DocumentParsingException`.

### `com.example.contract.service.QuestionAnswerService` (Interface)
- **Responsibility**: Defines the contract for natural language document querying:
  `QuestionResponse answerQuestion(Long contractId, String extractedText, String question);`

### `com.example.contract.service.KeywordQuestionAnswerService` (Implementation)
- **Responsibility**: Answers natural language questions using a keyword extraction and synonym expansion algorithm without relying on paid or external AI APIs.
- **Pipeline Details**:
  1. **Preprocessing**: Normalizes questions to lowercase, removes punctuation, and filters out common stop words (e.g. *what, is, the, when, where, of, in*).
  2. **Synonym Expansion**: Checks an internal dictionary mapping canonical employment terms to common synonyms:
     - *Salary* $\rightarrow$ pay, compensation, remuneration, ctc, stipend, wage
     - *Notice Period* $\rightarrow$ notice, resignation
     - *Joining Date* $\rightarrow$ start date, commencement date, date of joining
     - *Work Location* $\rightarrow$ location, workplace, office, job location
     - *Employee Name* $\rightarrow$ candidate name, name, employee
  3. **Candidate Segmentation**: Splits contract text into lines and candidate sentences.
  4. **Scoring Engine**:
     - *Label Matches*: Lines structured as `Label: Value` (e.g., `Salary: Rs. 50,000 per month`) receive a heavy score boost (+15 points for exact match, +10 for partial match).
     - *Phrase Matches*: Multi-word concepts receive +8 points.
     - *Keyword Matches*: Individual matching terms receive +3 points.
  5. **Value Extraction (`formatAnswer`)**:
     - If the best line contains a colon `:` or hyphen `-`, the specific value part is extracted (e.g. `Rs. 50,000 per month`).
     - If it is a table cell line (`Salary | Rs. 50,000 per month`), the second column value is returned.
  6. **Threshold Guard**: If the top score is below `MIN_CONFIDENCE_SCORE` (3), the service returns `found: false` with `"Information not found in the contract."` to ensure answers are never fabricated.

### `com.example.contract.service.ContractService`
- **Responsibility**: The primary orchestrator handling business operations across file storage, document parsing, database updates, and Q&A.
- **Key Methods**:
  - `uploadContract(MultipartFile file)`: Validates file extension, stores it via `FileStorageService`, extracts text via `DocumentParserService`, cleans up on failure, saves the entity with `DRAFT` status, and returns a `ContractResponse`.
  - `getAllContracts()`: Reads all entities from `ContractRepository` and maps them to `ContractSummaryResponse` (omitting extracted text).
  - `getContractById(Long id)`: Finds entity by ID, throws `ContractNotFoundException(id)` if absent, and returns `ContractResponse`.
  - `updateContractStatus(Long id, ContractStatus newStatus)`: Calls `contract.getStatus().validateTransition(newStatus)`, persists changes, and returns the updated response.
  - `askQuestion(Long id, String question)`: Passes contract text to `QuestionAnswerService` and returns the resulting `QuestionResponse`.
- **Key Annotations**:
  - `@Service`: Managed service bean.
  - `@Transactional`: Wraps write operations in a database transaction.
  - `@Transactional(readOnly = true)`: Optimizes read operations by avoiding Hibernate dirty checking.

---

## 6. Controller & Presentation Layer

### `com.example.contract.controller.ContractController`
- **Responsibility**: Exposes the REST API endpoints, parses HTTP request parameters and bodies, performs validation, and maps service responses to HTTP status codes.
- **Endpoints**:
  - `POST /api/contracts` $\rightarrow$ Accepts `multipart/form-data` with parameter `file`. Returns HTTP `201 Created`.
  - `GET /api/contracts` $\rightarrow$ Returns HTTP `200 OK` with `List<ContractSummaryResponse>`.
  - `GET /api/contracts/{id}` $\rightarrow$ Returns HTTP `200 OK` with `ContractResponse` (or 404).
  - `PUT /api/contracts/{id}/status` $\rightarrow$ Accepts JSON `StatusUpdateRequest`. Returns HTTP `200 OK` (or 409).
  - `POST /api/contracts/{id}/questions` $\rightarrow$ Accepts JSON `QuestionRequest`. Returns HTTP `200 OK` with `QuestionResponse`.
- **Key Annotations**:
  - `@RestController`: Marks this class as a REST controller where method return values are automatically serialized to JSON.
  - `@RequestMapping("/api/contracts")`: Base path for all endpoints.
  - `@Tag(name = "Contract Management API")`: Groups endpoints in Swagger UI.
  - `@Operation`, `@ApiResponse`, `@Parameter`: Swagger annotations detailing request/response formats.
  - `@Valid`: Triggers Bean Validation on request bodies before the method executes.

---

## 7. Data Transfer Objects (DTOs)

All DTOs are plain Java objects without Lombok, featuring explicit constructors, getters, and setters:

| DTO Class | Primary Purpose | Fields |
|---|---|---|
| `ContractResponse` | Detailed contract data returned on upload, get-by-id, and status update | `id`, `fileName`, `fileType`, `status`, `extractedText`, `contractDescription`, `uploadedAt` |
| `ContractSummaryResponse` | Lightweight response for contract listing (avoids high network payload) | `id`, `fileName`, `fileType`, `status`, `contractDescription`, `uploadedAt` |
| `StatusUpdateRequest` | Request payload for status transition | `status` (`@NotNull`) |
| `QuestionRequest` | Request payload for querying contract content | `question` (`@NotBlank`) |
| `QuestionResponse` | Result of a contract question query | `contractId`, `question`, `answer`, `found` |
| `ErrorResponse` | Consistent error structure returned across all failures | `timestamp`, `status`, `error`, `message`, `path` |

---

## 8. Exception Handling

### `com.example.contract.exception.GlobalExceptionHandler`
- **Responsibility**: Centralized interceptor that catches application and framework exceptions, maps them to standard HTTP status codes, and formats them into an `ErrorResponse`.
- **Key Annotations**:
  - `@RestControllerAdvice`: Automatically intercepts exceptions thrown by any `@RestController`.
  - `@ExceptionHandler(...)`: Declares which exception type a method handles.
- **Exception to HTTP Status Mapping**:
  - `ContractNotFoundException` $\rightarrow$ `404 NOT FOUND`
  - `InvalidFileException` $\rightarrow$ `400 BAD REQUEST`
  - `InvalidStatusTransitionException` $\rightarrow$ `409 CONFLICT`
  - `DocumentParsingException` $\rightarrow$ `422 UNPROCESSABLE ENTITY`
  - `MaxUploadSizeExceededException` $\rightarrow$ `413 PAYLOAD TOO LARGE`
  - `MethodArgumentNotValidException` $\rightarrow$ `400 BAD REQUEST` (collects field error messages)
  - `HttpMessageNotReadableException` $\rightarrow$ `400 BAD REQUEST` (catches malformed JSON)
  - `Exception` (catch-all) $\rightarrow$ `500 INTERNAL SERVER ERROR` (returns generic message, hides stack trace)

---

## 9. Configuration

### `com.example.contract.config.OpenApiConfig`
- **Responsibility**: Configures the OpenAPI 3.0 specification details (title, description, version, contact, license) displayed on the Swagger UI page (`/swagger-ui.html`).
- **Key Annotations**:
  - `@Configuration`: Indicates a Spring configuration class providing `@Bean` definitions.

---

## 10. Application Entry Point

### `com.example.contract.ContractApplication`
- **Responsibility**: Boots the Spring application context, starts the embedded Tomcat server, scans packages starting from `com.example.contract`, and configures Spring Data JPA repositories.
- **Key Annotation**:
  - `@SpringBootApplication`: Meta-annotation combining `@Configuration`, `@EnableAutoConfiguration`, and `@ComponentScan`.
