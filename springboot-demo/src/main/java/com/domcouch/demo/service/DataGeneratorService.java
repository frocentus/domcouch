package com.domcouch.demo.service;

import com.domcouch.demo.model.Person;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Generates realistic fake person data — no external dependencies.
 * Produces 20 attributes per person.
 */
@Service
public class DataGeneratorService {

    private static final Logger log = LoggerFactory.getLogger(DataGeneratorService.class);
    private final Random rng = new SecureRandom();

    // --- name pools ---

    private static final String[] FIRST_NAMES = {
            "James","Mary","John","Patricia","Robert","Jennifer","Michael","Linda",
            "William","Elizabeth","David","Barbara","Richard","Susan","Joseph","Jessica",
            "Thomas","Sarah","Christopher","Karen","Charles","Lisa","Daniel","Nancy",
            "Matthew","Betty","Anthony","Margaret","Mark","Sandra","Donald","Ashley",
            "Steven","Kimberly","Paul","Emily","Andrew","Donna","Joshua","Michelle",
            "Kenneth","Carol","Kevin","Amanda","Brian","Dorothy","George","Melissa",
            "Timothy","Deborah","Ronald","Stephanie","Edward","Rebecca","Jason","Sharon",
            "Jeffrey","Laura","Ryan","Cynthia","Jacob","Kathleen","Gary","Amy",
            "Nicholas","Angela","Eric","Shirley","Jonathan","Anna","Stephen","Brenda",
            "Larry","Pamela","Justin","Emma","Scott","Nicole","Brandon","Helen",
            "Benjamin","Samantha","Samuel","Katherine","Raymond","Christine","Gregory","Debra",
            "Frank","Rachel","Alexander","Carolyn","Patrick","Janet","Jack","Catherine",
            "Dennis","Maria","Jerry","Heather","Tyler","Diane","Aaron","Ruth"
    };

    private static final String[] LAST_NAMES = {
            "Smith","Johnson","Williams","Brown","Jones","Garcia","Miller","Davis",
            "Rodriguez","Martinez","Hernandez","Lopez","Gonzalez","Wilson","Anderson","Thomas",
            "Taylor","Moore","Jackson","Martin","Lee","Perez","Thompson","White",
            "Harris","Sanchez","Clark","Ramirez","Lewis","Robinson","Walker","Young",
            "Allen","King","Wright","Scott","Torres","Nguyen","Hill","Flores",
            "Green","Adams","Nelson","Baker","Hall","Rivera","Campbell","Mitchell",
            "Carter","Roberts","Gomez","Phillips","Evans","Turner","Diaz","Parker",
            "Cruz","Edwards","Collins","Reyes","Stewart","Morris","Morales","Murphy",
            "Cook","Rogers","Gutierrez","Ortiz","Morgan","Cooper","Peterson","Bailey",
            "Reed","Kelly","Howard","Ramos","Kim","Cox","Ward","Richardson"
    };

    private static final String[] CITIES = {
            "New York","Los Angeles","Chicago","Houston","Phoenix","Philadelphia",
            "San Antonio","San Diego","Dallas","San Jose","Austin","Jacksonville",
            "Fort Worth","Columbus","Charlotte","Indianapolis","San Francisco","Seattle",
            "Denver","Washington","Boston","Nashville","Portland","Oklahoma City",
            "Las Vegas","Baltimore","Louisville","Milwaukee","Albuquerque","Tucson"
    };

    private static final String[] STATES = {
            "AL","AK","AZ","AR","CA","CO","CT","DE","FL","GA","HI","ID","IL",
            "IN","IA","KS","KY","LA","ME","MD","MA","MI","MN","MS","MO","MT",
            "NE","NV","NH","NJ","NM","NY","NC","ND","OH","OK","OR","PA","RI",
            "SC","SD","TN","TX","UT","VT","VA","WA","WV","WI","WY"
    };

    private static final String[] COMPANIES = {
            "Acme Corp","Globex Industries","Initech Solutions","Umbrella Holdings",
            "Stark Enterprises","Wayne Industries","Oscorp Dynamics","Massive Dynamic",
            "Cyberdyne Systems","Aperture Science","Weyland-Yutani","Tyrell Corporation",
            "Soylent Corp","Dunder Mifflin","Sterling Cooper","Pied Piper Technologies",
            "Hooli Inc","E Corp","Gringotts Financial","Wonka Industries"
    };

    private static final String[] DEPARTMENTS = {
            "Engineering","Sales","Marketing","Human Resources","Finance","Operations",
            "Research & Development","Customer Support","Legal","Product Management",
            "Information Technology","Quality Assurance","Supply Chain","Administration",
            "Data Science","Design","Security","Compliance","Training","Strategy"
    };

    private static final String[] OCCUPATIONS = {
            "Software Engineer","Project Manager","Data Analyst","Accountant",
            "Marketing Specialist","Sales Representative","HR Coordinator",
            "Operations Manager","Product Designer","Financial Advisor",
            "Systems Administrator","Research Scientist","Quality Engineer",
            "Business Analyst","Technical Writer","DevOps Engineer",
            "UX Designer","Database Administrator","Network Engineer",
            "Customer Success Manager"
    };

    private static final String[] STREETS = {
            "Main St","Oak Ave","Maple Dr","Cedar Ln","Pine Rd","Elm St",
            "Washington Blvd","Park Ave","Lake Dr","Hill Rd","Forest Way",
            "River Rd","Spring St","Meadow Ln","Valley Dr","Highland Ave",
            "Sunset Blvd","Willow Way","Cherry Ln","Birch St"
    };

    private static final String[] MARITAL_STATUSES = {
            "Single","Married","Divorced","Widowed","Separated"
    };

    private static final String[] GENDERS = {"Male","Female"};

    /**
     * Generate a batch of fake persons.
     *
     * @param count number of persons to generate
     * @return list of Person objects
     */
    public List<Person> generatePersons(int count) {
        long start = System.currentTimeMillis();
        List<Person> persons = new ArrayList<>(count);

        for (int i = 0; i < count; i++) {
            persons.add(generateOne());
        }

        long elapsed = System.currentTimeMillis() - start;
        log.info("Generated {} fake persons in {}ms", count, elapsed);
        return persons;
    }

    private Person generateOne() {
        String firstName = pick(FIRST_NAMES);
        String lastName = pick(LAST_NAMES);
        String gender = pick(GENDERS);
        String company = pick(COMPANIES);
        String department = pick(DEPARTMENTS);
        String city = pick(CITIES);
        String state = pick(STATES);

        LocalDate dateOfBirth = randomDate(1955, 2005);
        LocalDate hireDate = randomDate(2010, 2025);

        return new Person(
                firstName,
                lastName,
                generateEmail(firstName, lastName, company),
                generatePhone(),
                randomInt(100, 9999) + " " + pick(STREETS),
                city,
                state,
                String.format("%05d", randomInt(1000, 99999)),
                "USA",
                dateOfBirth,
                gender,
                pick(OCCUPATIONS),
                company,
                department,
                "EMP-" + String.format("%06d", randomInt(1, 999999)),
                Math.round((30000 + rng.nextDouble() * 170000) * 100.0) / 100.0,
                hireDate,
                pick(FIRST_NAMES) + " " + pick(LAST_NAMES),
                String.format("%03d-%02d-%04d", randomInt(100, 999), randomInt(10, 99), randomInt(1000, 9999)),
                pick(MARITAL_STATUSES)
        );
    }

    // --- helpers ---

    private String pick(String[] pool) {
        return pool[rng.nextInt(pool.length)];
    }

    private int randomInt(int min, int max) {
        return min + rng.nextInt(max - min + 1);
    }

    private String generateEmail(String first, String last, String company) {
        String domain = company.toLowerCase()
                .replaceAll("[^a-z0-9]", "")
                .replaceAll("corporation", "corp")
                .replaceAll("industries", "ind")
                .replaceAll("technologies", "tech")
                + ".com";
        if (domain.length() > 30) domain = domain.substring(0, 30);
        return (first.charAt(0) + last).toLowerCase() + randomInt(1, 999) + "@" + domain;
    }

    private String generatePhone() {
        return String.format("(%03d) %03d-%04d",
                randomInt(200, 999), randomInt(200, 999), randomInt(1000, 9999));
    }

    private LocalDate randomDate(int fromYear, int toYear) {
        LocalDate start = LocalDate.of(fromYear, 1, 1);
        LocalDate end = LocalDate.of(toYear, 12, 31);
        long days = ChronoUnit.DAYS.between(start, end);
        return start.plusDays(rng.nextLong(days + 1));
    }
}
