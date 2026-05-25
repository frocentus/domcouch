package com.domcouch.api;

import java.util.List;

/**
 * Represents a Database Access Control List. Mirrors lotus.domino.ACL.
 * <p>
 * Stored as a document with {@code _type = "domcouch.acl"} in the database scope.
 */
public interface ACL {

    // ---- access levels ----

    /** No access — user cannot see or open the database */
    int LEVEL_NOACCESS  = 0;
    /** Can create documents but not read (needs PRIV_CREATE_DOCS) */
    int LEVEL_DEPOSITOR = 1;
    /** Can read documents */
    int LEVEL_READER    = 2;
    /** Can create and edit own documents (needs PRIV_CREATE_DOCS to create) */
    int LEVEL_AUTHOR    = 3;
    /** Can edit all documents */
    int LEVEL_EDITOR    = 4;
    /** Can modify design elements and create full-text index */
    int LEVEL_DESIGNER  = 5;
    /** Full control — modify ACL, encrypt, delete database, replication settings */
    int LEVEL_MANAGER   = 6;

    // ---- access level privileges (flags) ----

    /** Create documents (required for Author + Depositor level) */
    int PRIV_CREATE_DOCS              = 1;
    /** Delete documents */
    int PRIV_DELETE_DOCS              = 2;
    /** Create personal agents */
    int PRIV_CREATE_PERSONAL_AGENT    = 4;
    /** Create personal folders and views */
    int PRIV_CREATE_PERSONAL_FOLDER   = 8;
    /** Create shared folders and views */
    int PRIV_CREATE_SHARED_FOLDER     = 16;
    /** Create LotusScript / Java agents */
    int PRIV_CREATE_LS_JAVA_AGENT     = 32;
    /** Read public documents (even at No Access level) */
    int PRIV_READ_PUBLIC_DOCS         = 64;
    /** Write public documents (even at No Access level) */
    int PRIV_WRITE_PUBLIC_DOCS        = 128;
    /** Replicate or copy documents */
    int PRIV_REPLICATE_COPY           = 256;

    // ---- entries ----

    /**
     * @return the ACL entry for the given name, or null if not found
     */
    ACLEntry getEntry(String name);

    /**
     * Create or overwrite an ACL entry.
     * @param name user, group, or server name
     * @param level access level (LEVEL_* constants)
     * @return the created entry
     */
    ACLEntry createACLEntry(String name, int level);

    /**
     * Remove an ACL entry.
     * @param name the entry name to remove
     */
    void removeACLEntry(String name);

    /**
     * @return all ACL entries
     */
    List<ACLEntry> getEntries();

    // ---- roles ----

    /**
     * @return all defined roles
     */
    List<String> getRoles();

    /**
     * Add a role definition.
     * @param name role name
     */
    void addRole(String name);

    /**
     * Remove a role and revoke it from all entries.
     * @param role role name
     */
    void removeRole(String role);

    /**
     * Rename an existing role.
     * @param oldName current name
     * @param newName new name
     */
    void renameRole(String oldName, String newName);

    // ---- settings ----

    /**
     * @return the default access level for users not explicitly listed
     */
    int getDefaultLevel();

    /**
     * Set the default access level.
     * @param level LEVEL_* constant
     */
    void setDefaultLevel(int level);

    /**
     * @return true if uniform access is enabled (consistent enforcement)
     */
    boolean isUniformAccess();

    /**
     * Enable or disable uniform access.
     */
    void setUniformAccess(boolean flag);

    /**
     * @return true if the current user has administrator access to edit the ACL
     */
    boolean isAdminNames();

    /**
     * @return true if consistent (uniform) access is enforced across all replicas.
     *         Mirrors lotus.domino.ACL.isConsistentACL().
     */
    boolean isConsistentACL();

    /**
     * Enable or disable consistent ACL enforcement.
     */
    void setConsistentACL(boolean flag);

    /**
     * @return the maximum internet access level (LEVEL_* constant).
     *         For Couchbase, this is always LEVEL_MANAGER.
     */
    int getInternetLevel();

    /**
     * Set the maximum internet access level.
     */
    void setInternetLevel(int level);

    /**
     * @return true if administrators can read/edit all documents
     *         regardless of Reader/Author fields.
     */
    boolean isAdminReaderAuthor();

    /**
     * Enable or disable admin reader-author override.
     */
    void setAdminReaderAuthor(boolean flag);

    /**
     * Persist the ACL to Couchbase.
     */
    void save();
}
