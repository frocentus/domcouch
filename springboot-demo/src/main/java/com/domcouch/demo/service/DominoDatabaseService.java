package com.domcouch.demo.service;

import com.domcouch.api.*;
import com.domcouch.demo.model.Person;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

/**
 * Service that encapsulates the Domino-style API.
 * Stores Person records as Documents and provides lookup Views.
 */
@Service
public class DominoDatabaseService {

    private static final Logger log = LoggerFactory.getLogger(DominoDatabaseService.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;

    private final Database database;
    private final DataGeneratorService generator;

    // View names
    public static final String VIEW_ALL = "AllPersons";
    public static final String VIEW_BY_LASTNAME = "ByLastName";
    public static final String VIEW_BY_DEPARTMENT = "ByDepartment";
    public static final String VIEW_BY_COMPANY = "ByCompany";
    public static final String VIEW_BY_SALARY_RANGE = "BySalaryRange";
    public static final String VIEW_BY_CITY = "ByCity";
    public static final String VIEW_HIGH_EARNERS = "HighEarners";

    public DominoDatabaseService(Database database, DataGeneratorService generator) {
        this.database = database;
        this.generator = generator;
    }

    /**
     * Initialize the database: generate and persist 10,000 fake persons if empty,
     * then create all views.
     */
    public void initialize() throws NotesException {
        int existingCount = database.getDocumentCount();
        if (existingCount > 0) {
            log.info("Database already contains {} documents — skipping generation.", existingCount);
        } else {
            log.info("Generating 10,000 fake persons...");
            List<Person> persons = generator.generatePersons(10_000);
            bulkInsert(persons);
            log.info("Successfully inserted {} documents.", persons.size());
        }
        createAllViews();
    }

    /**
     * Re-initialize: clear and re-populate the database.
     */
    public void reinitialize() throws NotesException {
        log.warn("Clearing all documents...");
        int removed = 0;
        View allView = database.getView(VIEW_ALL);
        ViewEntryCollection entries = allView.getAllEntries();
        for (ViewEntry entry : entries) {
            Document doc = entry.getDocument();
            if (doc != null) {
                doc.remove();
                removed++;
            }
        }
        log.info("Removed {} documents.", removed);

        List<Person> persons = generator.generatePersons(10_000);
        bulkInsert(persons);
        log.info("Re-inserted {} documents.", persons.size());
        createAllViews();
    }

    /**
     * Store a single Person as a Domino-style Document.
     */
    public Document storePerson(Person person) throws NotesException {
        Document doc = database.createDocument();
        doc.replaceItemValue("Form", "Person");
        doc.replaceItemValue("FirstName", person.getFirstName());
        doc.replaceItemValue("LastName", person.getLastName());
        doc.replaceItemValue("Email", person.getEmail());
        doc.replaceItemValue("Phone", person.getPhone());
        doc.replaceItemValue("Street", person.getStreet());
        doc.replaceItemValue("City", person.getCity());
        doc.replaceItemValue("State", person.getState());
        doc.replaceItemValue("ZipCode", person.getZipCode());
        doc.replaceItemValue("Country", person.getCountry());
        doc.replaceItemValue("DateOfBirth", person.getDateOfBirth().format(DATE_FMT));
        doc.replaceItemValue("Gender", person.getGender());
        doc.replaceItemValue("Occupation", person.getOccupation());
        doc.replaceItemValue("Company", person.getCompany());
        doc.replaceItemValue("Department", person.getDepartment());
        doc.replaceItemValue("EmployeeId", person.getEmployeeId());
        doc.replaceItemValue("Salary", person.getSalary());
        doc.replaceItemValue("HireDate", person.getHireDate().format(DATE_FMT));
        doc.replaceItemValue("ManagerName", person.getManagerName());
        doc.replaceItemValue("SSN", person.getSsn());
        doc.replaceItemValue("MaritalStatus", person.getMaritalStatus());
        doc.save();
        return doc;
    }

    /**
     * Retrieve a Person by UNID.
     */
    public Person getPersonByUNID(String unid) {
        Document doc = database.getDocumentByUNID(unid);
        return doc != null ? documentToPerson(doc) : null;
    }

    /**
     * Lookup persons by the given view and key.
     */
    public List<Person> lookupByView(String viewName, String key) throws NotesException {
        List<Person> results = new ArrayList<>();
        View view = database.getView(viewName);
        ViewEntryCollection entries;

        if (key != null && !key.isEmpty()) {
            entries = view.getAllEntriesByKey(key);
        } else {
            entries = view.getAllEntries();
        }

        for (ViewEntry entry : entries) {
            Document doc = entry.getDocument();
            if (doc != null) {
                results.add(documentToPerson(doc));
            }
        }
        return results;
    }

    /**
     * Full-text search across all documents.
     */
    public List<Person> search(String query) throws NotesException {
        return search(query, 500);
    }

    public List<Person> search(String query, int maxDocs) throws NotesException {
        List<Person> results = new ArrayList<>();
        DocumentCollection docs = database.FTSearch(query, maxDocs);
        for (Document doc : docs) {
            results.add(documentToPerson(doc));
        }
        return results;
    }

    /**
     * Get document count.
     */
    public int getDocumentCount() {
        return database.getDocumentCount();
    }

    public String getDatabaseTitle() {
        return database.getTitle();
    }

    // ---- bulk insert ----

    private void bulkInsert(List<Person> persons) throws NotesException {
        int batchSize = 500;
        int total = persons.size();

        for (int i = 0; i < total; i += batchSize) {
            int end = Math.min(i + batchSize, total);
            for (int j = i; j < end; j++) {
                storePerson(persons.get(j));
            }
            log.info("Inserted {}/{} persons", end, total);
        }
    }

    // ---- view creation ----

    private void createAllViews() {
        log.info("Creating views...");

        database.createView(VIEW_ALL,
                "Form = 'Person'");

        database.createView(VIEW_BY_LASTNAME,
                "Form = 'Person' AND LastName IS NOT MISSING",
                "LastName");

        database.createView(VIEW_BY_DEPARTMENT,
                "Form = 'Person' AND Department IS NOT MISSING",
                "Department");

        database.createView(VIEW_BY_COMPANY,
                "Form = 'Person' AND Company IS NOT MISSING",
                "Company");

        database.createView(VIEW_BY_SALARY_RANGE,
                "Form = 'Person' AND Salary IS NOT MISSING",
                "Salary");

        database.createView(VIEW_BY_CITY,
                "Form = 'Person' AND City IS NOT MISSING",
                "City");

        database.createView(VIEW_HIGH_EARNERS,
                "Form = 'Person' AND Salary > 100000",
                "Salary");

        log.info("Views created: {}, {}, {}, {}, {}, {}, {}",
                VIEW_ALL, VIEW_BY_LASTNAME, VIEW_BY_DEPARTMENT, VIEW_BY_COMPANY,
                VIEW_BY_SALARY_RANGE, VIEW_BY_CITY, VIEW_HIGH_EARNERS);
    }

    // ---- document-to-person mapping ----

    private Person documentToPerson(Document doc) {
        if (doc == null) return null;
        Person p = new Person();
        p.setFirstName(getItemString(doc, "FirstName"));
        p.setLastName(getItemString(doc, "LastName"));
        p.setEmail(getItemString(doc, "Email"));
        p.setPhone(getItemString(doc, "Phone"));
        p.setStreet(getItemString(doc, "Street"));
        p.setCity(getItemString(doc, "City"));
        p.setState(getItemString(doc, "State"));
        p.setZipCode(getItemString(doc, "ZipCode"));
        p.setCountry(getItemString(doc, "Country"));

        String dob = getItemString(doc, "DateOfBirth");
        p.setDateOfBirth(dob != null && !dob.isEmpty() ? java.time.LocalDate.parse(dob) : null);

        p.setGender(getItemString(doc, "Gender"));
        p.setOccupation(getItemString(doc, "Occupation"));
        p.setCompany(getItemString(doc, "Company"));
        p.setDepartment(getItemString(doc, "Department"));
        p.setEmployeeId(getItemString(doc, "EmployeeId"));

        Item salaryItem = doc.getFirstItem("Salary");
        p.setSalary(salaryItem != null ? salaryItem.getValueDouble() : 0.0);

        String hire = getItemString(doc, "HireDate");
        p.setHireDate(hire != null && !hire.isEmpty() ? java.time.LocalDate.parse(hire) : null);

        p.setManagerName(getItemString(doc, "ManagerName"));
        p.setSsn(getItemString(doc, "SSN"));
        p.setMaritalStatus(getItemString(doc, "MaritalStatus"));
        return p;
    }

    private String getItemString(Document doc, String name) {
        Item item = doc.getFirstItem(name);
        return item != null ? item.getValueString() : null;
    }
}
