package com.domcouch.impl;

import com.couchbase.client.java.Collection;
import com.couchbase.client.java.json.JsonObject;
import com.couchbase.client.java.kv.GetOptions;
import com.couchbase.client.java.kv.GetResult;
import com.couchbase.client.java.kv.UpsertOptions;
import com.couchbase.client.java.codec.RawJsonTranscoder;
import com.domcouch.api.ACL;
import com.domcouch.api.ACLEntry;

import java.util.*;

/**
 * Couchbase-backed ACL. Stored as a document with key {@code "acl"} and
 * {@code _type = "domcouch.acl"} in the database's documents collection.
 */
public class CouchbaseACL implements ACL {

    private static final String ACL_DOC_ID = "acl";
    private static final String ACL_TYPE = "domcouch.acl";

    private final Collection collection;
    private final Map<String, ACLEntry> entries;
    private final List<String> roles;
    private int defaultLevel;
    private boolean uniformAccess;
    private boolean consistentACL;
    private int internetLevel;
    private boolean adminReaderAuthor;

    /**
     * Load or create the ACL document.
     */
    public CouchbaseACL(Collection collection) {
        this.collection = collection;
        this.entries = new LinkedHashMap<>();
        this.roles = new ArrayList<>();
        this.defaultLevel = LEVEL_READER;
        this.uniformAccess = false;
        this.consistentACL = false;
        this.internetLevel = LEVEL_MANAGER;
        this.adminReaderAuthor = false;
        load();
    }

    // ---- entries ----

    @Override
    public ACLEntry getEntry(String name) {
        return entries.get(name.toLowerCase());
    }

    @Override
    public ACLEntry createACLEntry(String name, int level) {
        CouchbaseACLEntry entry = new CouchbaseACLEntry(name, level, ACLEntry.TYPE_UNSPECIFIED);
        entries.put(name.toLowerCase(), entry);
        return entry;
    }

    @Override
    public void removeACLEntry(String name) {
        entries.remove(name.toLowerCase());
    }

    @Override
    public List<ACLEntry> getEntries() {
        return List.copyOf(entries.values());
    }

    // ---- roles ----

    @Override
    public List<String> getRoles() {
        return List.copyOf(roles);
    }

    @Override
    public void addRole(String name) {
        if (!roles.contains(name)) roles.add(name);
    }

    @Override
    public void removeRole(String role) {
        roles.remove(role);
        for (ACLEntry e : entries.values()) {
            e.disableRole(role);
        }
    }

    @Override
    public void renameRole(String oldName, String newName) {
        int idx = roles.indexOf(oldName);
        if (idx >= 0) roles.set(idx, newName);
        for (ACLEntry e : entries.values()) {
            if (e.isRoleEnabled(oldName)) {
                e.disableRole(oldName);
                e.enableRole(newName);
            }
        }
    }

    // ---- settings ----

    @Override
    public int getDefaultLevel() { return defaultLevel; }

    @Override
    public void setDefaultLevel(int level) {
        this.defaultLevel = clampLevel(level);
    }

    @Override
    public boolean isUniformAccess() { return uniformAccess; }

    @Override
    public void setUniformAccess(boolean flag) { this.uniformAccess = flag; }

    @Override
    public boolean isAdminNames() {
        return true;
    }

    @Override
    public boolean isConsistentACL() { return consistentACL; }

    @Override
    public void setConsistentACL(boolean flag) { this.consistentACL = flag; }

    @Override
    public int getInternetLevel() { return internetLevel; }

    @Override
    public void setInternetLevel(int level) {
        this.internetLevel = Math.min(level, LEVEL_MANAGER);
    }

    @Override
    public boolean isAdminReaderAuthor() { return adminReaderAuthor; }

    @Override
    public void setAdminReaderAuthor(boolean flag) { this.adminReaderAuthor = flag; }

    // ---- persistence ----

    @Override
    public void save() {
        JsonObject doc = JsonObject.create()
                .put("_type", ACL_TYPE)
                .put("defaultLevel", defaultLevel)
                .put("uniformAccess", uniformAccess)
                .put("consistentACL", consistentACL)
                .put("internetLevel", (long) internetLevel)
                .put("adminReaderAuthor", adminReaderAuthor);

        JsonObject rolesArr = JsonObject.create();
        for (String r : roles) rolesArr.put(r, true);
        doc.put("roles", rolesArr);

        JsonObject entriesObj = JsonObject.create();
        for (Map.Entry<String, ACLEntry> e : entries.entrySet()) {
            ACLEntry ae = e.getValue();
            JsonObject entryJson = JsonObject.create()
                    .put("level", ae.getLevel())
                    .put("userType", ae.getUserType())
                    .put("privileges", (long) ae.getPrivileges());
            JsonObject entryRoles = JsonObject.create();
            for (String r : ae.getRoles()) entryRoles.put(r, true);
            entryJson.put("roles", entryRoles);
            entriesObj.put(ae.getName(), entryJson);
        }
        doc.put("entries", entriesObj);

        collection.upsert(ACL_DOC_ID, doc, UpsertOptions.upsertOptions()
                .expiry(java.time.Duration.ofDays(365)));
    }

    /**
     * Load the ACL from Couchbase. If no ACL document exists, starts with defaults.
     */
    private void load() {
        try {
            GetResult gr = collection.get(ACL_DOC_ID,
                    GetOptions.getOptions().transcoder(RawJsonTranscoder.INSTANCE));
            if (gr == null) return;
            JsonObject doc;
            try {
                doc = JsonObject.fromJson(gr.contentAs(String.class));
            } catch (Exception e) {
                return;
            }
            if (doc == null) return;
            if (!ACL_TYPE.equals(doc.getString("_type"))) return;

            Long dl = doc.getLong("defaultLevel");
            defaultLevel = dl != null ? clampLevel(dl.intValue()) : defaultLevel;
            Boolean ua = doc.getBoolean("uniformAccess");
            uniformAccess = ua != null && ua;
            Boolean ca = doc.getBoolean("consistentACL");
            consistentACL = ca != null && ca;
            Long il = doc.getLong("internetLevel");
            if (il != null) internetLevel = il.intValue();
            Boolean ara = doc.getBoolean("adminReaderAuthor");
            adminReaderAuthor = ara != null && ara;

            JsonObject rolesObj = doc.getObject("roles");
            if (rolesObj != null) {
                for (String name : rolesObj.getNames()) {
                    Boolean v = rolesObj.getBoolean(name);
                    if (v != null && v) roles.add(name);
                }
            }

            JsonObject entriesObj = doc.getObject("entries");
            if (entriesObj != null) {
                for (String name : entriesObj.getNames()) {
                    JsonObject entryJson = entriesObj.getObject(name);
                    if (entryJson == null) continue;
                    Long lev = entryJson.getLong("level");
                    int level = lev != null ? clampLevel(lev.intValue()) : LEVEL_READER;
                    Long ut = entryJson.getLong("userType");
                    int userType = ut != null ? ut.intValue() : ACLEntry.TYPE_UNSPECIFIED;

                    CouchbaseACLEntry entry = new CouchbaseACLEntry(name, level, userType);
                    Long epriv = entryJson.getLong("privileges");
                    if (epriv != null) entry.setPrivileges(epriv.intValue());
                    JsonObject eroles = entryJson.getObject("roles");
                    if (eroles != null) {
                        for (String r : eroles.getNames()) {
                            Boolean rv = eroles.getBoolean(r);
                            if (rv != null && rv) entry.enableRole(r);
                        }
                    }
                    entries.put(name.toLowerCase(), entry);
                }
            }
        } catch (Exception e) {
            // No ACL document yet — use defaults
        }
    }

    private static int clampLevel(Object val) {
        if (val instanceof Number n) {
            int lvl = n.intValue();
            if (lvl < LEVEL_NOACCESS) return LEVEL_NOACCESS;
            if (lvl > LEVEL_MANAGER) return LEVEL_MANAGER;
            return lvl;
        }
        return LEVEL_READER;
    }

    @Override
    public String toString() {
        return "ACL[" + entries.size() + " entries, " + roles.size()
                + " roles, default=" + ACLEntry.class.getSimpleName() + "]";
    }
}
