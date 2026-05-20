# Documentation Audit Report — domcouch

> **Audit date**: 2026-05-19  
> **Auditor**: AI documentation auditor  
> **Scope**: 9 markdown files across `docs/`, `README.md`, `AGENTS.md`

---

## (A) Concise Summary — Top 5 Recommended Actions

| # | Action | Effort | Impact |
|---|---|---|---|
| 1 | **Cross-reference architecture-review ↔ code-review-report** — they share resolved/status information. Add "Status: ✅ Fixed" to each finding in the code review report and link to architecture-review. | Low | Clarity |
| 2 | **Add a `docs/README.md` landing page** — a one-page index mapping each doc to its purpose and audience. New users have no entry point. | Low | Discoverability |
| 3 | **Merge couchbase8-knowledge.md into .skills/couchbase8/SKILL.md** — two files cover identical topics. Keep the skill file as canonical source, replace knowledge doc with a 5-line pointer. | Low | Eliminates duplication |
| 4 | **Extract "Known Limitations" from AGENTS.md into a standalone `docs/known-limitations.md`** — AGENTS.md is a decision log; limitations belong in a dedicated doc that's easy to find. Reference from AGENTS.md. | Low | Structure |
| 5 | **Add "Last Updated" dates to all docs** — only 4 of 9 files have visible dates. | Low | Freshness visibility |

---

## (B) Detailed Report — Document-by-Document

### Inventory

| # | File | Lines | Purpose |
|---|---|---|---|
| 1 | `README.md` | 372 | Project landing page: architecture, build, features, endpoints, test suite |
| 2 | `AGENTS.md` | 465 | Project decisions, conventions, architecture, decision log, known limitations |
| 3 | `docs/api-coverage.md` | 281 | Domino API compatibility matrix with domcouch status |
| 4 | `docs/architecture-review.md` | 219 | Gap analysis against ideal Domino facade architecture |
| 5 | `docs/code-review-report.md` | 185 | External code review findings with risk assessment |
| 6 | `docs/couchbase8-knowledge.md` | 412 | Couchbase 8 N1QL patterns, indexes, pitfalls, performance |
| 7 | `docs/formula-language-architecture.md` | 815 | Formula engine design: grammar, AST, evaluation, translation |
| 8 | `docs/function-catalog.md` | 324 | Per-function implementation status table |
| 9 | `docs/notes_formula_documentation.md` | 22,761 | HCL Domino official @Function reference (external reference) |
| — | `.skills/couchbase8/SKILL.md` | — | pi agent skill: same topic as #6 |

### Structure Check

| File | Logical Flow | Missing Sections | Notes |
|---|---|---|---|
| README.md | ✅ Good | Prerequisites (after Docker but before build) | — |
| AGENTS.md | ✅ Good | None | Section ordering slightly non-linear (known limitations appear in two places: §3.4 and §8) |
| api-coverage.md | ✅ Good | Examples column in ViewNavigator table | Tables are complete but don't link to implementation classes |
| architecture-review.md | ✅ Good | "Resolution Status" column to show fix dates | Many findings are resolved but doc doesn't show it |
| code-review-report.md | ✅ Good | "Status" column per finding | All findings resolved; doc shows none |
| couchbase8-knowledge.md | ⚠️ Fragmented with skill file | Cross-reference to .skills/couchbase8/SKILL.md | Added today |
| formula-language-architecture.md | ✅ Excellent | None | Best-structured doc in the set |
| function-catalog.md | ✅ Good | None | — |

### Overlap and Fragmentation

#### Major Duplication: Couchbase 8 Knowledge

| Location | Content |
|---|---|
| `docs/couchbase8-knowledge.md` (412 lines) | N1QL patterns, indexes, collections, consistency, EXPLAIN, performance |
| `.skills/couchbase8/SKILL.md` (278 lines) | Same topics, optimized for pi agent consumption |

**Recommendation**: Keep `.skills/couchbase8/SKILL.md` as canonical. Replace `docs/couchbase8-knowledge.md` with a 5-line file: `# Couchbase 8 Knowledge → see [.skills/couchbase8/SKILL.md](../.skills/couchbase8/SKILL.md)`. The skill file is the actively maintained, shorter, and more actionable version.

#### Duplication: Known Limitations

| Location | Content |
|---|---|
| `AGENTS.md` §3.4 | "Known Limitations" — 2 items (reader-filtered counts, formula translator trust) |
| `AGENTS.md` §8 | "Known Limitations" — 7 items (counts, translator, ACL, RichText, replication, @DbLookup, copyAllItems) |

**Recommendation**: Merge into one section. Move to a standalone `docs/known-limitations.md`. §3.4 items are security-specific and should move to §8. Reference from AGENTS.md with a 2-line pointer.

#### Fragmentation: Architecture Assessment

| Location | Content |
|---|---|
| `docs/architecture-review.md` §1-8 | Gap analysis vs ideal architecture |
| `docs/code-review-report.md` | Specific code flaws with fix recommendations |
| `AGENTS.md` §7 | Decision log (raw chronology) |

**Recommendation**: No merge needed — these serve different purposes. But add cross-references:
- Architecture review should reference code review for specific fixes
- Code review should state which findings are resolved and where

#### Fragmentation: Formula Documentation

| Location | Content |
|---|---|
| `docs/formula-language-architecture.md` | Engine design, grammar, evaluation rules |
| `docs/function-catalog.md` | Per-function status table |
| `docs/notes_formula_documentation.md` | HCL official @Function reference (22K lines) |

**Recommendation**: No merge — distinct purposes. Ensure formula-language-architecture §4 (catalog) cross-references function-catalog.md.

### Merge/Split Recommendations

| Action | Files | Rationale |
|---|---|---|
| **Merge** | couchbase8-knowledge.md → .skills/couchbase8/SKILL.md | Two files, identical scope, skill file is canonical. Replace knowledge doc with pointer. |
| **Split** | AGENTS.md §8 → docs/known-limitations.md | Limitations belong in dedicated doc for discoverability. AGENTS.md is getting long (465 lines). |
| **Merge** | AGENTS.md §3.4 → AGENTS.md §8 | Two known-limitations sections. Consolidate. |

### Single-Source Guidance

| Topic | Canonical Source | Why |
|---|---|---|
| Couchbase 8 patterns | `.skills/couchbase8/SKILL.md` | Updated most recently, optimized format |
| Architecture gaps | `docs/architecture-review.md` | Most comprehensive, structured by concern |
| Code flaws & fixes | `docs/code-review-report.md` | External audit, clear findings |
| API compatibility status | `docs/api-coverage.md` | Tabular format, easy to scan |
| Formula engine design | `docs/formula-language-architecture.md` | Authoritative design doc |
| Project conventions | `AGENTS.md` | Decision log + code conventions |
| User-facing features | `README.md` | Landing page |

### Consistency & Naming — Proposed Glossary

| Term | Definition | Notes |
|---|---|---|
| **Item** | A single named field in a Document (type + values) | Domino-compatible |
| **Multi-instance item** | Multiple Items with the same name (e.g., Body) | Stored as JSON array |
| **View** | A N1QL-backed query with optional key columns and display columns | Mirrors Domino View |
| **ViewNavigator** | Cursor over a View's entries, including category rows | In-memory or lazy variants |
| **Batch fetching** | N1QL USE KEYS with 100 IDs per query | Replaces N+1 KV reads |
| **Lazy loading** | Item deserialization deferred to first access | 70% memory reduction |
| **RawJsonTranscoder** | Couchbase SDK transcoder that avoids ClassCastException | Used in getDocumentByUNID |

### Navigation & Discoverability

**Recommended `docs/README.md` landing page:**

```markdown
# DomCouch Documentation

| Document | Audience | Purpose |
|---|---|---|
| [api-coverage.md](api-coverage.md) | Developers | Domino API compatibility status |
| [architecture-review.md](architecture-review.md) | Architects | Gap analysis vs ideal facade |
| [code-review-report.md](code-review-report.md) | Developers | Security/performance findings |
| [couchbase8-knowledge.md](couchbase8-knowledge.md) | Developers | → see .skills/couchbase8/SKILL.md |
| [formula-language-architecture.md](formula-language-architecture.md) | Engine devs | Formula engine design |
| [function-catalog.md](function-catalog.md) | Engine devs | Per-function status |
| [notes_formula_documentation.md](notes_formula_documentation.md) | Reference | HCL Domino @Function docs |
| [known-limitations.md](known-limitations.md) | Users | Current limitations |

Also: [README.md](../README.md) (project overview), [AGENTS.md](../AGENTS.md) (conventions + decisions)
```

---

## Actionable Edit Plan (Prioritized)

| # | Edit | Effort | Order |
|---|---|---|---|
| 1 | Add `docs/README.md` landing page | Low | 1st |
| 2 | Replace `couchbase8-knowledge.md` with pointer to `.skills/couchbase8/SKILL.md` | Low | 1st |
| 3 | Add "Status: ✅ Resolved" columns to `code-review-report.md` findings | Low | 2nd |
| 4 | Add cross-reference from `architecture-review.md` to `code-review-report.md` | Low | 2nd |
| 5 | Extract AGENTS.md §8 to `docs/known-limitations.md`, merge §3.4 into it | Low | 3rd |
| 6 | Add "Last Updated" date to `function-catalog.md`, `code-review-report.md` | Low | 3rd |
| 7 | Add formula-language-architecture.md §4 cross-reference to function-catalog.md | Low | 4th |
| 8 | Add "Examples" column to api-coverage.md ViewNavigator table | Low | 4th |

---

## Verification Checklist

- [ ] `docs/README.md` exists with landing page table
- [ ] `docs/couchbase8-knowledge.md` replaced with pointer to skill file
- [ ] `docs/code-review-report.md` has "Status" column showing ✅ for each finding
- [ ] `docs/architecture-review.md` links to code-review-report.md
- [ ] AGENTS.md §8 and §3.4 consolidated into `docs/known-limitations.md`
- [ ] All documents have "Last Updated" date visible
- [ ] `docs/formula-language-architecture.md` §4 links to `function-catalog.md`
- [ ] No broken cross-references after changes
