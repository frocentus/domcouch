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

    // ---- wildcard ----

    /**
     * Return true if this entry is a wildcard pattern.
     * Example: entry name "*&#47;West/Acme" matches any user in West/Acme.
     */
    default boolean isWildcard() {
        return getName().contains("*");
    }

    /**
     * Test whether a hierarchical name matches this entry's wildcard.
     */
    default boolean matchesWildcard(String userName) {
        if (!isWildcard() || userName == null) return false;
        String[] pp = getName().split("/");
        String[] up = userName.split("/");
        if (pp.length != up.length) return false;
        for (int i = 0; i < pp.length; i++) {
            if (pp[i].equals("*")) continue;
            if (!pp[i].equalsIgnoreCase(up[i])) return false;
        }
        return true;
    }

    // ---- privileges (per-entry, with defaults by level) ----

    /**
     * @return the privilege bitmask for this entry (PRIV_* flags from ACL).
     *         Defaults are set when the level is assigned.
     */
    int getPrivileges();

    /**
     * Set the full privilege bitmask.
     */
    void setPrivileges(int privileges);

    /**
     * @return true if this entry has the specified privilege.
     */
    default boolean isPrivilegeEnabled(int privilege) {
        return (getPrivileges() & privilege) != 0;
    }

    /**
     * Enable a privilege for this entry.
     */
    default void enablePrivilege(int privilege) {
        setPrivileges(getPrivileges() | privilege);
    }

    /**
     * Disable a privilege for this entry.
     */
    default void disablePrivilege(int privilege) {
        setPrivileges(getPrivileges() & ~privilege);
    }

    /**
     * @return the default privilege bitmask for a given access level.
     */
    static int defaultPrivilegesForLevel(int level) {
        return switch (level) {
            case ACL.LEVEL_MANAGER  -> ACL.PRIV_CREATE_DOCS | ACL.PRIV_CREATE_PERSONAL_AGENT
                    | ACL.PRIV_CREATE_PERSONAL_FOLDER | ACL.PRIV_CREATE_SHARED_FOLDER
                    | ACL.PRIV_CREATE_LS_JAVA_AGENT | ACL.PRIV_READ_PUBLIC_DOCS
                    | ACL.PRIV_WRITE_PUBLIC_DOCS;
            case ACL.LEVEL_DESIGNER -> ACL.PRIV_CREATE_DOCS | ACL.PRIV_CREATE_PERSONAL_AGENT
                    | ACL.PRIV_CREATE_PERSONAL_FOLDER | ACL.PRIV_CREATE_SHARED_FOLDER
                    | ACL.PRIV_READ_PUBLIC_DOCS | ACL.PRIV_WRITE_PUBLIC_DOCS;
            case ACL.LEVEL_EDITOR   -> ACL.PRIV_CREATE_DOCS | ACL.PRIV_READ_PUBLIC_DOCS
                    | ACL.PRIV_WRITE_PUBLIC_DOCS;
            case ACL.LEVEL_AUTHOR   -> ACL.PRIV_READ_PUBLIC_DOCS;
            case ACL.LEVEL_READER   -> ACL.PRIV_READ_PUBLIC_DOCS;
            case ACL.LEVEL_DEPOSITOR -> ACL.PRIV_CREATE_DOCS;
            default -> 0; // No Access: no default privileges
        };
    }

    // ---- convenience ----

    /**
     * @return true if this entry can create documents (level ≥ Author + CREATE_DOCS priv).
     */
    default boolean canCreateDocuments() {
        return getLevel() >= ACL.LEVEL_EDITOR
            || (getLevel() >= ACL.LEVEL_AUTHOR && isPrivilegeEnabled(ACL.PRIV_CREATE_DOCS));
    }

    /**
     * @return true if this entry can delete documents (needs DELETE_DOCS priv).
     */
    default boolean canDeleteDocuments() {
        return isPrivilegeEnabled(ACL.PRIV_DELETE_DOCS);
    }

    /**
     * @return true if this entry can create personal agents.
     */
    default boolean canCreatePersonalAgent() {
        return isPrivilegeEnabled(ACL.PRIV_CREATE_PERSONAL_AGENT);
    }

    /**
     * @return true if this entry can create personal folders/views.
     */
    default boolean canCreatePersonalFolderView() {
        return isPrivilegeEnabled(ACL.PRIV_CREATE_PERSONAL_FOLDER);
    }

    /**
     * @return true if this entry can create shared folders/views.
     */
    default boolean canCreateSharedFolderView() {
        return isPrivilegeEnabled(ACL.PRIV_CREATE_SHARED_FOLDER);
    }

    /**
     * @return true if this entry can create LotusScript/Java agents.
     */
    default boolean canCreateLSOrJavaAgent() {
        return isPrivilegeEnabled(ACL.PRIV_CREATE_LS_JAVA_AGENT);
    }

    /**
     * @return true if this entry can replicate or copy documents.
     */
    default boolean canReplicateOrCopyDocuments() {
        return isPrivilegeEnabled(ACL.PRIV_REPLICATE_COPY);
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
