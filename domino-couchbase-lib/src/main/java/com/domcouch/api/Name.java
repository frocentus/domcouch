package com.domcouch.api;

/**
 * Represents a Domino/Notes hierarchical name. Mirrors lotus.domino.Name.
 * <p>
 * Supports both canonical ("CN=John Smith/OU=Dev/O=Acme/C=US") and
 * abbreviated ("John Smith/Dev/Acme/US") formats. Components are indexed
 * from the left (most specific) to right (most general).
 */
public interface Name {

    // ---- full-name formats ----

    /**
     * @return canonical form: "CN=John Smith/OU=Dev/O=Acme/C=US"
     */
    String getCanonical();

    /**
     * @return abbreviated form: "John Smith/Dev/Acme/US"
     */
    String getAbbreviated();

    // ---- individual components ----

    /**
     * @return the Common Name, e.g. "John Smith"
     */
    String getCommon();

    /**
     * @return the Organization, e.g. "Acme"
     */
    String getOrganization();

    /**
     * @return Organizational Unit 1 (first after CN), or "" if none
     */
    String getOrgUnit1();

    /**
     * @return Organizational Unit 2, or "" if none
     */
    String getOrgUnit2();

    /**
     * @return Organizational Unit 3, or "" if none
     */
    String getOrgUnit3();

    /**
     * @return Organizational Unit 4, or "" if none
     */
    String getOrgUnit4();

    /**
     * @return the Country component, e.g. "US", or "" if none
     */
    String getCountry();

    /**
     * @return true if the name contains hierarchical components (OU/O/C)
     */
    boolean isHierarchical();

    // ---- extended components (optional) ----

    /**
     * @return the Given Name (first name), or "" if not available
     */
    String getGiven();

    /**
     * @return the Surname (last name), or "" if not available
     */
    String getSurname();

    /**
     * @return the Initials, or "" if not available
     */
    String getInitials();

    /**
     * @return the Generation qualifier (e.g. "Jr", "III"), or "" if not available
     */
    String getGeneration();

    /**
     * @return the ADMD component, or "" if not available
     */
    String getADMD();

    /**
     * @return the PRMD component, or "" if not available
     */
    String getPRMD();

    // ---- internet / RFC addresses ----

    /**
     * @return RFC 821 address (e.g. "john.smith@acme.com")
     */
    String getAddr821();

    /**
     * @return RFC 822 address (e.g. "John Smith <john.smith@acme.com>")
     */
    String getAddr822();

    /**
     * @return the local part of the RFC 822 address
     */
    String getAddr822LocalPart();

    /**
     * @return the phrase part of the RFC 822 address
     */
    String getAddr822Phrase();

    // ---- language ----

    /**
     * @return the language code (e.g. "en"), or "" if not set
     */
    String getLanguage();
}
