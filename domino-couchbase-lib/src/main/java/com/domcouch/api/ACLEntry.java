package com.domcouch.api;

import java.util.List;

/**
 * A single entry in a Database ACL. Mirrors lotus.domino.ACLEntry.
 * <p>
 * Each entry associates a name (user, group, or server) with an access level
 * and optional role assignments.
 */
public interface ACLEntry {

    /** Person name */
    int TYPE_PERSON       = 0;
    /** Server name */
    int TYPE_SERVER       = 1;
    /** Mixed group */
    int TYPE_MIXED_GROUP  = 2;
    /** Person group */
    int TYPE_PERSON_GROUP = 3;
    /** Server group */
    int TYPE_SERVER_GROUP = 4;
    /** Unspecified / default */
    int TYPE_UNSPECIFIED  = -1;

    // ---- basic properties ----

    /**
     * @return the name (hierarchical or flat) this entry applies to
     */
    String getName();

    /**
     * Set a new name for this entry.
     */
    void setName(String name);

    /**
     * @return the access level (ACL.LEVEL_* constant)
     */
    int getLevel();

    /**
     * Set the access level.
     * @param level ACL.LEVEL_* constant
     */
    void setLevel(int level);

    // ---- user type ----

    /**
     * @return TYPE_* constant indicating person, server, or group
     */
    int getUserType();

    /**
     * Set the user type.
     * @param type TYPE_* constant or TYPE_UNSPECIFIED
     */
    void setUserType(int type);

    /**
     * @return true if this entry is for a person (not group/server)
     */
    boolean isPerson();

    /**
     * @return true if this entry is for a server
     */
    boolean isServer();

    /**
     * @return true if this entry is for a group
     */
    boolean isGroup();

    // ---- roles ----

    /**
     * @return list of roles enabled for this entry
     */
    List<String> getRoles();

    /**
     * @param role role name
     * @return true if the specified role is enabled
     */
    boolean isRoleEnabled(String role);

    /**
     * Enable a role for this entry.
     * @param role role name
     */
    void enableRole(String role);

    /**
     * Disable a role for this entry.
     * @param role role name
     */
    void disableRole(String role);

    // ---- permissions (bonus / not in lotus.domino) ----

    /**
     * @return true if this entry can create documents
     */
    default boolean canCreateDocuments() {
        return getLevel() >= ACL.LEVEL_AUTHOR;
    }

    /**
     * @return true if this entry can delete documents
     */
    default boolean canDeleteDocuments() {
        return getLevel() >= ACL.LEVEL_EDITOR;
    }

    /**
     * @return true if this entry can create personal agents/views
     */
    default boolean canCreatePersonalAgent() {
        return getLevel() >= ACL.LEVEL_EDITOR;
    }

    /**
     * @return true if this entry can create LotusScript/Java agents
     */
    default boolean canCreateLSOrJavaAgent() {
        return getLevel() >= ACL.LEVEL_DESIGNER;
    }

    /**
     * @return a human-readable level name (e.g., "Manager", "Author")
     */
    default String getLevelName() {
        return switch (getLevel()) {
            case ACL.LEVEL_NOACCESS  -> "No Access";
            case ACL.LEVEL_DEPOSITOR -> "Depositor";
            case ACL.LEVEL_READER    -> "Reader";
            case ACL.LEVEL_AUTHOR    -> "Author";
            case ACL.LEVEL_EDITOR    -> "Editor";
            case ACL.LEVEL_DESIGNER  -> "Designer";
            case ACL.LEVEL_MANAGER   -> "Manager";
            default -> "Unknown (" + getLevel() + ")";
        };
    }
}
