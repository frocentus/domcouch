package com.domcouch.demo.model;

import java.time.LocalDate;
import java.util.StringJoiner;

/**
 * Domain model for a Person record — 20 attributes.
 */
public class Person {

    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String street;
    private String city;
    private String state;
    private String zipCode;
    private String country;
    private LocalDate dateOfBirth;
    private String gender;
    private String occupation;
    private String company;
    private String department;
    private String employeeId;
    private double salary;
    private LocalDate hireDate;
    private String managerName;
    private String ssn;
    private String maritalStatus;

    // --- constructors ---

    public Person() {}

    public Person(String firstName, String lastName, String email, String phone,
                  String street, String city, String state, String zipCode,
                  String country, LocalDate dateOfBirth, String gender,
                  String occupation, String company, String department,
                  String employeeId, double salary, LocalDate hireDate,
                  String managerName, String ssn, String maritalStatus) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phone = phone;
        this.street = street;
        this.city = city;
        this.state = state;
        this.zipCode = zipCode;
        this.country = country;
        this.dateOfBirth = dateOfBirth;
        this.gender = gender;
        this.occupation = occupation;
        this.company = company;
        this.department = department;
        this.employeeId = employeeId;
        this.salary = salary;
        this.hireDate = hireDate;
        this.managerName = managerName;
        this.ssn = ssn;
        this.maritalStatus = maritalStatus;
    }

    // --- getters & setters ---

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getStreet() { return street; }
    public void setStreet(String street) { this.street = street; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getZipCode() { return zipCode; }
    public void setZipCode(String zipCode) { this.zipCode = zipCode; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getOccupation() { return occupation; }
    public void setOccupation(String occupation) { this.occupation = occupation; }

    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }

    public double getSalary() { return salary; }
    public void setSalary(double salary) { this.salary = salary; }

    public LocalDate getHireDate() { return hireDate; }
    public void setHireDate(LocalDate hireDate) { this.hireDate = hireDate; }

    public String getManagerName() { return managerName; }
    public void setManagerName(String managerName) { this.managerName = managerName; }

    public String getSsn() { return ssn; }
    public void setSsn(String ssn) { this.ssn = ssn; }

    public String getMaritalStatus() { return maritalStatus; }
    public void setMaritalStatus(String maritalStatus) { this.maritalStatus = maritalStatus; }

    @Override
    public String toString() {
        return new StringJoiner(", ", Person.class.getSimpleName() + "[", "]")
                .add("name='" + firstName + " " + lastName + "'")
                .add("email='" + email + "'")
                .add("company='" + company + "'")
                .add("department='" + department + "'")
                .add("salary=" + String.format("$%.0f", salary))
                .toString();
    }
}
