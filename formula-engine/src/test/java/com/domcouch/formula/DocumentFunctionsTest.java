package com.domcouch.formula;

import org.junit.jupiter.api.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Document Functions")
class DocumentFunctionsTest extends BaseFormulaTest {

    @Nested @DisplayName("@DocFields")
    class DocFieldsTests {
        @Test @DisplayName("returns list") void returnsList() { assertTrue(eval("@DocFields") instanceof List); }
    }

    @Nested @DisplayName("@DocLength")
    class DocLengthTests {
        @Test @DisplayName("returns number") void returnsNumber() { assertEquals(0.0, eval("@DocLength")); }
    }

    @Nested @DisplayName("@DocLock")
    class DocLockTests {
        @Test @DisplayName("LOCKINGENABLED") void lockingEnabled() { assertEquals(0.0, eval("@DocLock([LOCKINGENABLED])")); }
        @Test @DisplayName("STATUS") void status() { assertEquals("", eval("@DocLock([STATUS])")); }
        @Test @DisplayName("LOCK") void lock() { assertEquals(1.0, eval("@DocLock([LOCK])")); }
    }

    @Nested @DisplayName("@DocumentUniqueID")
    class DocumentUniqueIdTests {
        @Test @DisplayName("returns string") void returnsUnid() { assertEquals("", eval("@DocumentUniqueID")); }
    }

    @Nested @DisplayName("Document lifecycle")
    class DocLifecycleTests {
        @Test @DisplayName("@DeleteDocument") void del() { assertEquals(1.0, eval("@DeleteDocument")); }
        @Test @DisplayName("@UndeleteDocument") void undel() { assertEquals(1.0, eval("@UndeleteDocument")); }
        @Test @DisplayName("@HardDeleteDocument") void hardDel() { assertEquals(1.0, eval("@HardDeleteDocument")); }
        @Test @DisplayName("@DocCommittedLength") void committed() { assertEquals(0.0, eval("@DocCommittedLength")); }
    }

    @Nested @DisplayName("Folder and misc ops")
    class FolderTests {
        @Test @DisplayName("@AddToFolder") void add() { assertEquals(1.0, eval("@AddToFolder(\"folder\")")); }
        @Test @DisplayName("@WhichFolders") void which() { assertEquals(List.of(), eval("@WhichFolders")); }
        @Test @DisplayName("@Narrow/@Wide") void narrowWide() { assertEquals(1.0, eval("@Narrow")); assertEquals(1.0, eval("@Wide")); }
        @Test @DisplayName("@GetField") void getField() { vars.put("TestField", "hello"); assertEquals("hello", eval("@GetField(\"TestField\")")); }
    }

    @Nested @DisplayName("@DeleteField")
    class DeleteFieldDirectTests {
        @Test @DisplayName("function exists") void exists() { assertEquals(0.0, eval("@IsError(@DeleteField(\"Field\"))")); }
    }
}
