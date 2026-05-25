package com.domcouch.impl;

import com.domcouch.api.ACL;
import com.domcouch.api.ACLEntry;

import java.util.ArrayList;
import java.util.List;

/**
 * Couchbase-backed ACLEntry. Stored as part of the ACL document.
 */
public class CouchbaseACLEntry implements ACLEntry {

    private String name;
    private int level;
    private int userType;
    private int privileges;
    private final List<String> roles;

    public CouchbaseACLEntry(String name, int level, int userType) {
        this.name = name;
        this.level = clampLevel(level);
        this.userType = userType;
        this.privileges = ACLEntry.defaultPrivilegesForLevel(this.level);
        this.roles = new ArrayList<>();
    }

    @Override
    public String getName() { return name; }

    @Override
    public void setName(String name) { this.name = name; }

    @Override
    public int getLevel() { return level; }

    @Override
    public void setLevel(int level) {
        int newLevel = clampLevel(level);
        if (this.level != newLevel) {
            this.level = newLevel;
            this.privileges = ACLEntry.defaultPrivilegesForLevel(this.level);
        }
    }

    @Override
    public int getPrivileges() { return privileges; }

    @Override
    public void setPrivileges(int privileges) { this.privileges = privileges; }

    @Override
    public int getUserType() { return userType; }

    @Override
    public void setUserType(int type) { this.userType = type; }

    @Override
    public boolean isPerson() { return userType == TYPE_PERSON; }

    @Override
    public boolean isServer() { return userType == TYPE_SERVER; }

    @Override
    public boolean isGroup() {
        return userType == TYPE_PERSON_GROUP
            || userType == TYPE_SERVER_GROUP
            || userType == TYPE_MIXED_GROUP;
    }

    @Override
    public List<String> getRoles() { return List.copyOf(roles); }

    @Override
    public boolean isRoleEnabled(String role) { return roles.contains(role); }

    @Override
    public void enableRole(String role) {
        if (!roles.contains(role)) roles.add(role);
    }

    @Override
    public void disableRole(String role) { roles.remove(role); }

    private static int clampLevel(int level) {
        if (level < ACL.LEVEL_NOACCESS) return ACL.LEVEL_NOACCESS;
        if (level > ACL.LEVEL_MANAGER) return ACL.LEVEL_MANAGER;
        return level;
    }

    @Override
    public String toString() {
        return getLevelName() + ": " + name + (roles.isEmpty() ? "" : " [" + String.join(", ", roles) + "]");
    }
}
