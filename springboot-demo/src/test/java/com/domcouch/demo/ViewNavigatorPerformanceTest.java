package com.domcouch.demo;

import com.domcouch.api.*;
import com.domcouch.impl.CouchbaseLazyViewNavigator;
import com.domcouch.impl.CouchbaseSession;
import com.domcouch.impl.CouchbaseView;
import com.domcouch.impl.CouchbaseViewNavigator;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;

import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Performance and memory test for ViewNavigator categorization.
 * <p>
 * Requires a running Couchbase with the demo database populated:
 *   docker compose up -d
 *   mvn -pl springboot-demo spring-boot:run    (wait for init, then stop)
 * <p>
 * Run: mvn test -pl springboot-demo -Dtest=ViewNavigatorPerformanceTest
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ViewNavigatorPerformanceTest {

    private static Session session;
    private static Database db;

    @BeforeAll
    static void setUp() throws NotesException {
        session = CouchbaseSession.connect("couchbase://localhost", "Administrator", "password");
        db = session.getDatabase("domcouch", "contacts");
    }

    @AfterAll
    static void tearDown() {
        if (session != null) session.recycle();
    }

    @Test @Order(1) @DisplayName("Flat view — build time + memory for non-categorized view")
    void flatViewPerformance() throws Exception {
        View view = db.createView("perf_flat",
                null,  // no selection filter — include all documents
                List.of(ViewColumn.field("FullName", "LastName")));

        long buildMs = measureBuildTime(view);
        long memoryKB = measureMemory(view);

        System.out.printf("  [FLAT]  build: %d ms | entries: %d | memory: %d KB%n",
                buildMs, view.getEntryCount(), memoryKB);
        assertTrue(buildMs < 15_000, "Build should complete within 15 seconds");
    }

    @Test @Order(2) @DisplayName("Single-level category — build time + memory by Department")
    void singleCategoryPerformance() throws Exception {
        View view = db.createView("perf_cat_1",
                null,
                List.of("Department"),
                List.of(ViewColumn.field("Department", "Department"),
                        ViewColumn.field("FullName", "LastName")));

        long buildMs = measureBuildTime(view);
        long memoryKB = measureMemory(view);
        var nav = view.createViewNav();
        int catCount = countCategories(nav);
        int docCount = nav.getCount() - catCount;

        System.out.printf("  [CAT-1] build: %d ms | categories: %d | docs: %d | total: %d | memory: %d KB%n",
                buildMs, catCount, docCount, nav.getCount(), memoryKB);
        assertTrue(buildMs < 15_000, "Build should complete within 15 seconds");
        assertTrue(catCount > 0, "Should have at least one category");
    }

    @Test @Order(3) @DisplayName("Two-level category — build time + memory by Department → City")
    void twoLevelCategoryPerformance() throws Exception {
        View view = db.createView("perf_cat_2",
                null,
                List.of("Department", "City"),
                List.of(ViewColumn.field("Department", "Department"),
                        ViewColumn.field("City", "City"),
                        ViewColumn.field("FullName", "LastName")));

        long buildMs = measureBuildTime(view);
        long memoryKB = measureMemory(view);
        var nav = view.createViewNav();
        int total = nav.getCount();
        int cat1 = countCategoriesAtLevel(nav, 1);
        int cat2 = countCategoriesAtLevel(nav, 2);

        System.out.printf("  [CAT-2] build: %d ms | lvl1-cats: %d | lvl2-cats: %d | total: %d | memory: %d KB%n",
                buildMs, cat1, cat2, total, memoryKB);
        assertTrue(buildMs < 20_000, "Build should complete within 20 seconds");
        assertTrue(cat1 > 0, "Should have level-1 categories");
    }

    @Test @Order(4) @DisplayName("Navigation — getNth, getNext, getNextCategory benchmark")
    void navigationPerformance() throws Exception {
        View view = db.createView("perf_nav",
                null,
                List.of("Department"),
                List.of(ViewColumn.field("Department", "Department"),
                        ViewColumn.field("FullName", "LastName")));

        var nav = view.createViewNav();
        int total = nav.getCount();

        // O(1) getNth
        long t0 = System.nanoTime();
        int samples = Math.min(200, total);
        for (int pos = 1; pos <= samples; pos += Math.max(1, total / 200)) {
            ViewEntry e = nav.getNth(pos);
            assertNotNull(e, "getNth(" + pos + ") should not be null at " + pos + "/" + total);
        }
        long nthNs = (System.nanoTime() - t0) / Math.max(1, samples);

        // Sequential getNext walk
        t0 = System.nanoTime();
        nav.gotoFirst();
        ViewEntry e = nav.getCurrent();
        int walked = 0;
        while (e != null && walked < samples) {
            e = nav.getNext();
            walked++;
        }
        long nextNs = (System.nanoTime() - t0) / Math.max(1, walked);

        // getNextCategory skip
        t0 = System.nanoTime();
        nav.gotoFirst();
        int catSkips = 0;
        while (nav.getNextCategory() != null && catSkips < 100) catSkips++;
        long skipNs = (System.nanoTime() - t0) / Math.max(1, catSkips);

        System.out.printf("  [NAV]   getNth: %d ns | getNext: %d ns | cat-skip: %d ns | total: %d entries%n",
                nthNs, nextNs, skipNs, total);
        assertTrue(nthNs < 10_000, "getNth should be O(1) — sub-10μs");
    }

    @Test @Order(5) @DisplayName("Category hierarchy — child, parent, sibling, position string")
    void hierarchyNavigation() throws Exception {
        View view = db.createView("perf_hier",
                null,
                List.of("Department", "City"),
                List.of(ViewColumn.field("Department", "Department"),
                        ViewColumn.field("City", "City"),
                        ViewColumn.field("FullName", "LastName")));

        var nav = view.createViewNav();

        // Find first category
        ViewEntry cat = nav.getFirst();
        while (cat != null && !cat.isCategory()) cat = nav.getNext();
        assertNotNull(cat, "Should have at least one category");
        assertTrue(cat.isCategory());
        assertTrue(cat.getChildCount() > 0, "Category should have children");
        assertNotNull(cat.getPositionString(), "Should have position string");
        assertFalse(cat.getPositionString().isEmpty());

        System.out.printf("  [HIER]  first cat: pos=%s \"%s\" children=%d descendants=%d%n",
                cat.getPositionString(),
                cat.getColumnValues().isEmpty() ? "?" : cat.getColumnValues().get(0),
                cat.getChildCount(), cat.getDescendantCount());

        // Navigate to first child
        ViewEntry child = cat.getChild();
        assertNotNull(child, "Category should have a first child");
        System.out.printf("          child: pos=%s isCategory=%s%n",
                child.getPositionString(), child.isCategory());

        // Navigate parent
        ViewEntry parent = child.getParentEntry();
        assertNotNull(parent, "Child should have a parent");
        assertEquals(cat, parent, "Parent should equal the original category");

        // Next sibling
        ViewEntry sib = child.getNextSibling();
        if (sib != null) {
            System.out.printf("          sibling: pos=%s isCategory=%s%n",
                    sib.getPositionString(), sib.isCategory());
            assertEquals(sib.getParentEntry(), cat, "Sibling should have same parent");
        }
    }

    @Test @Order(6) @DisplayName("Sub-navigator — createViewNavFromCategory")
    void subNavigatorPerformance() throws Exception {
        View view = db.createView("perf_sub",
                null,
                List.of("Department"),
                List.of(ViewColumn.field("Department", "Department"),
                        ViewColumn.field("FullName", "LastName")));

        var fullNav = view.createViewNav();

        // Find a category with a reasonable number of children (skip empty-key categories)
        ViewEntry cat = fullNav.getFirst();
        while (cat != null && !cat.isCategory()) cat = fullNav.getNext();
        while (cat != null && cat.getChildCount() < 5 && cat.isCategory())
            cat = fullNav.getNextCategory();

        assertNotNull(cat);
        assertTrue(cat.getChildCount() >= 5,
                "Need a category with >= 5 entries for sub-nav test, got " + cat.getChildCount());

        // Create sub-navigator
        long t0 = System.nanoTime();
        var subNav = view.createViewNavFromChildren(cat);
        long buildNs = System.nanoTime() - t0;

        System.out.printf("  [SUB]   category: \"%s\" children=%d | sub-nav build: %d μs | sub-nav count: %d%n",
                cat.getColumnValues().get(0), cat.getChildCount(),
                buildNs / 1000, subNav.getCount());

        assertEquals(cat.getChildCount(), subNav.getCount(),
                "Sub-navigator should contain exactly the category's children");
    }

    @Test @Order(7) @DisplayName("Memory estimate — entries × bytes per entry")
    void memoryPerEntry() throws Exception {
        View view = db.createView("perf_mem",
                null,
                List.of("Department", "City"),
                List.of(ViewColumn.field("Department", "Department"),
                        ViewColumn.field("FullName", "LastName")));

        long beforeKB = measureMemory(view); // view fetch only, no nav
        var nav = view.createViewNav();
        long afterKB = measureMemoryNow();
        long navMemoryKB = afterKB - beforeKB;
        int total = nav.getCount();

        System.out.printf("  [MEM]   entries: %d | nav memory: %d KB | %d bytes/entry%n",
                total, navMemoryKB, total > 0 ? (navMemoryKB * 1024 / total) : 0);
        assertTrue(total > 0, "Should have entries");
    }

    // ---- helpers ----

        @Test @Order(8) @DisplayName("Lazy navigator — key-based pagination vs in-memory index")
    void lazyNavigatorComparison() throws Exception {
        View view = db.createView("perf_lazy",
                null,
                List.of("Department"),
                List.of(ViewColumn.field("Department", "Department"),
                        ViewColumn.field("FullName", "LastName")));

        // In-memory build time
        long t0 = System.nanoTime();
        var navMem = new CouchbaseViewNavigator((CouchbaseView) view, 64, 0);
        long buildMsMem = (System.nanoTime() - t0) / 1_000_000;
        int totalMem = navMem.getCount();

        // Lazy build time (constructor only — no full scan)
        t0 = System.nanoTime();
        var navLazy = ((CouchbaseView) view).createLazyViewNav();
        long buildMsLazy = (System.nanoTime() - t0) / 1_000_000;

        // First-page fetch (real work happens here)
        t0 = System.nanoTime();
        ViewEntry first = navLazy.getFirst();
        long firstMsLazy = (System.nanoTime() - t0) / 1_000_000;
        int totalLazy = navLazy.getCount();

        // Sequential walk: 100 entries
        t0 = System.nanoTime();
        ViewEntry e = first;
        int walked = 0;
        while (e != null && walked < 100) { e = navLazy.getNext(); walked++; }
        long walkNsLazy = (System.nanoTime() - t0) / walked;

        // In-memory walk for comparison
        t0 = System.nanoTime();
        navMem.gotoFirst();
        e = navMem.getCurrent();
        walked = 0;
        while (e != null && walked < 100) { e = navMem.getNext(); walked++; }
        long walkNsMem = (System.nanoTime() - t0) / walked;

        // getNth(5000)
        t0 = System.nanoTime();
        navLazy.getNth(5000);
        long nthMsLazy = (System.nanoTime() - t0) / 1_000_000;

        t0 = System.nanoTime();
        navMem.getNth(5000);
        long nthNsMem = (System.nanoTime() - t0);

        System.out.printf("  [LAZY]  build: %d ms vs %d ms | first page: %d ms%n", buildMsMem, buildMsLazy, firstMsLazy);
        System.out.printf("  [LAZY]  total: %d vs %d | walk: %d ns vs %d ns%n", totalMem, totalLazy, walkNsMem, walkNsLazy);
        System.out.printf("  [LAZY]  getNth(5000): %d ns vs %d ms%n", nthNsMem, nthMsLazy);

        assertNotNull(first);
        assertTrue(totalLazy > 0, "Should have entries");
        // Lazy nav counts documents only (categories are virtual); in-memory includes categories
        assertTrue(totalMem > totalLazy, "In-memory should have more entries (includes categories)");
    }

    // ---- helpers ----

    private long measureBuildTime(View view) {
        System.gc();
        long t0 = System.nanoTime();
        view.createViewNav();
        return (System.nanoTime() - t0) / 1_000_000;
    }

    private long measureMemory(View view) {
        System.gc();
        Runtime rt = Runtime.getRuntime();
        long before = rt.totalMemory() - rt.freeMemory();
        var nav = view.createViewNav();
        long after = rt.totalMemory() - rt.freeMemory();
        return (after - before) / 1024;
    }

    private long measureMemoryNow() {
        Runtime rt = Runtime.getRuntime();
        return (rt.totalMemory() - rt.freeMemory()) / 1024;
    }

    private int countCategories(ViewNavigator nav) {
        int count = 0;
        try {
            ViewEntry e = nav.getFirst();
            while (e != null) {
                if (e.isCategory()) count++;
                e = nav.getNext();
            }
        } catch (NotesException ignored) {}
        return count;
    }

    private int countCategoriesAtLevel(ViewNavigator nav, int level) {
        int count = 0;
        try {
            ViewEntry e = nav.getFirst();
            while (e != null) {
                if (e.isCategory() && e.getCategoryLevel() == level) count++;
                e = nav.getNext();
            }
        } catch (NotesException ignored) {}
        return count;
    }
}
