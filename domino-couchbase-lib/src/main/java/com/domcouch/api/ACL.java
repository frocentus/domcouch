package com.domcouch.api;

import java.util.List;

/**
 * Represents a Database Access Control List. Mirrors lotus.domino.ACL.
 * <p>
 * Stored as a document with {@code _type = "domcouch.acl"} in the database scope.
 */
public interface ACL {

    /** No access — user cannot see or open the database */
    int LEVEL_NOACCESS  = 0;
    /** Can create documents but not read */
    int LEVEL_DEPOSITOR = 1;
    /** Can read documents */
    int LEVEL_READER    = 2;
    /** Can create and edit own documents */
    int LEVEL_AUTHOR    = 3;
    /** Can edit all documents */
    int LEVEL_EDITOR    = 4;
    /** Can modify design elements */
    int LEVEL_DESIGNER  = 5;
    /** Full control */
    int LEVEL_MANAGER   = 6;

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
     * Persist the ACL to Couchbase.
     */
    void save();
}
