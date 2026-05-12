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

### 2. Build the project

```bash
mvn clean package -DskipTests
```

### 3. Run the Spring Boot app

```bash
mvn -pl springboot-demo spring-boot:run
```

On first start, the app generates **10,000 fake persons** (20 attributes each)
and creates 7 N1QL-backed views.

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

### Example Queries

```bash
# Find by last name
curl "http://localhost:8080/api/persons/view/ByLastName?key=Smith"

# Full-text search
curl "http://localhost:8080/api/persons/search?q=Engineer"

# Get database stats
curl "http://localhost:8080/api/persons/info"
```

## Person Attributes (20 fields)

| #   | Field         | Type   |
| --- | ------------- | ------ |
| 1   | FirstName     | Text   |
| 2   | LastName      | Text   |
| 3   | Email         | Text   |
| 4   | Phone         | Text   |
| 5   | Street        | Text   |
| 6   | City          | Text   |
| 7   | State         | Text   |
| 8   | ZipCode       | Text   |
| 9   | Country       | Text   |
| 10  | DateOfBirth   | Date   |
| 11  | Gender        | Text   |
| 12  | Occupation    | Text   |
| 13  | Company       | Text   |
| 14  | Department    | Text   |
| 15  | EmployeeId    | Text   |
| 16  | Salary        | Number |
| 17  | HireDate      | Date   |
| 18  | ManagerName   | Text   |
| 19  | SSN           | Text   |
| 20  | MaritalStatus | Text   |

## Domino API Coverage

The library emulates the core `lotus.domino` data-layer API:

- **Session** — connect to cluster, open databases
- **Database** — createDocument, getDocumentByUNID, getView, createView, FTSearch, search
- **Document** — getFirstItem, replaceItemValue, save, remove, getUniversalID, hasItem
- **View** — getAllEntries, getAllEntriesByKey, getEntryByKey, FTSearch, getEntryCount
- **ViewEntry** — getColumnValues, getDocument, getUniversalID
- **ViewEntryCollection** — getFirstEntry, getNextEntry, getNthEntry, iterator
- **Item** — getValueString, getValueInt, getValueDouble, getValueDateTime
- **DateTime** — getLocalTime, toJavaDate, timeDifference, adjustDay
- **NotesException** — Domino-style error codes

## Project Layout

```
domcouch/
├── docker-compose.yml
├── pom.xml                          (parent Maven POM)
├── domino-couchbase-lib/            (API + Couchbase impl)
│   └── src/main/java/com/domcouch/
│       ├── api/                     (Interfaces: Session, Database, Document, View, ...)
│       └── impl/                    (CouchbaseSession, CouchbaseDatabase, ...)
└── springboot-demo/                 (REST app + data generator)
    └── src/main/java/com/domcouch/demo/
        ├── DomcouchDemoApplication.java
        ├── DatabaseInitializer.java
        ├── config/
        │   └── DomcouchConfig.java
        ├── controller/
        │   └── PersonController.java
        ├── model/
        │   └── Person.java
        └── service/
            ├── DataGeneratorService.java
            └── DominoDatabaseService.java
```
