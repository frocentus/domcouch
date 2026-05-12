# DomCouch — HCL Domino 14.5 Java API on Couchbase

> **"Write once against the Domino API, run forever on Couchbase."**

## Architecture

```
┌─────────────────────────────┐
│   Spring Boot Demo App      │
│   (REST API + Data Gen)     │
└──────────┬──────────────────┘
           │ uses
┌──────────▼──────────────────┐
│  domino-couchbase-lib       │
│  com.domcouch.api.*         │
│  (Session → DB → Doc → View)│
│  com.domcouch.formula.*     │
│  (Lexer → Parser → Eval)    │
└──────────┬──────────────────┘
           │ Couchbase Java SDK
┌──────────▼──────────────────┐
│  Couchbase 7.x (Docker)     │
│  Bucket: domcouch           │
│  Scope: contacts            │
│  Collection: documents      │
└─────────────────────────────┘
```

## Quick Start

### 1. Start Couchbase

```bash
docker compose up -d
```

Wait ~60s for the Couchbase cluster to initialize. Check the web console at
http://localhost:8091 (user: `Administrator`, password: `password`).

### 2. Build & test

```bash
mvn clean package -DskipTests
mvn test -pl domino-couchbase-lib   # 264 tests
```

### 3. Run the Spring Boot app

```bash
mvn -pl springboot-demo spring-boot:run
```

On first start, the app generates **10,000 fake persons** (20 attributes each)
and creates 7 N1QL-backed views.

## Key Features

### Domino API Emulation

- **Session** — connect to cluster, open databases (bucket-per-DB or scope-per-DB)
- **Database** — createDocument, getDocumentByUNID, getView, createView, FTSearch, search
- **Document** — getFirstItem, replaceItemValue, save, remove, copyToDatabase, hierarchy
- **View** — N1QL-backed: getAllEntries, getAllEntriesByKey, FTSearch, getEntryCount
- **Item** — multi-type: TEXT, NUMBERS, DATETIMES, AUTHORS, READERS, RICHTEXT
- **DateTime** — getLocalTime, toJavaDate, timeDifference, adjustDay

### Document-Level Security

Reader and Author fields with Domino-compatible semantics:
- **Reader fields**: documents invisible to unauthorized users (all read paths filtered)
- **Author fields**: `save()` and `remove()` enforce edit permissions (`NotesException 4010`)
- Centralized `canRead()` check shared by Document and View implementations

### Formula Engine

Full Lexer → Parser → Evaluator pipeline supporting Domino's formula language:
- **25+ @Functions**: `@Trim`, `@UpperCase`, `@If`, `@Do`, `@Return`, `@Elements`, etc.
- **Query translation**: `toN1ql()` — selection formulas → N1QL WHERE clauses
- **Computed evaluation**: `evaluate()` — computed fields against document contexts
- **Compiled caching**: `compileFormula()` → 16× speedup for batch processing
- **264 unit tests** with 97 real-world Domino formula examples

```java
// Compile once
CompiledFormula fullName = translator.compile("FirstName + \" \" + LastName");

// Evaluate against 10,000 documents — no re-parsing
for (Document doc : documents) {
    DocumentFormulaContext ctx = new DocumentFormulaContext(doc);
    String name = (String) translator.evaluate(fullName, ctx);
}
```

Full language spec: `docs/formula-language-architecture.md`

## REST Endpoints

| Method | Endpoint                               | Description               |
| ------ | -------------------------------------- | ------------------------- |
| GET    | `/api/persons/info`                    | Database info + doc count |
| GET    | `/api/persons/view/{viewName}?key=...` | Lookup by view            |
| GET    | `/api/persons/search?q=...`            | Full-text search          |
| GET    | `/api/persons/{unid}`                  | Get person by UNID        |
| POST   | `/api/persons/admin/reinitialize`      | Clear + repopulate        |

### Available Views

| View            | Purpose              |
| --------------- | -------------------- |
| `AllPersons`    | All documents        |
| `ByLastName`    | Indexed by last name |
| `ByDepartment`  | Group by department  |
| `ByCompany`     | Group by company     |
| `BySalaryRange` | Salary-based queries |
| `ByCity`        | Geographic lookup    |
| `HighEarners`   | Salary > $100K       |

## Project Layout

```
domcouch/
├── AGENTS.md                         (Architecture decisions + conventions)
├── README.md
├── pom.xml                           (parent Maven POM, Spring Boot 3.4.3)
├── docker-compose.yml                (Couchbase 7.x)
├── docs/
│   ├── api-coverage.md               (Domino API compatibility matrix)
│   └── formula-language-architecture.md  (Complete formula language spec)
├── domino-couchbase-lib/
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/domcouch/
│       │   ├── api/                  (Interfaces — the Domino contract)
│       │   ├── impl/                 (Couchbase-backed implementations)
│       │   └── formula/              (Formula engine: Lexer, Parser, Evaluator)
│       └── test/java/com/domcouch/
│           └── formula/              (264 unit tests)
└── springboot-demo/                  (REST app + data generator)
    └── src/main/java/com/domcouch/demo/
```

## Documentation

| Document | Purpose |
|----------|---------|
| `AGENTS.md` | Architecture decisions, security model, code conventions, decision log |
| `docs/api-coverage.md` | Domino API compatibility matrix + domcouch extensions |
| `docs/formula-language-architecture.md` | Complete formula language grammar, AST design, @Function catalog |
