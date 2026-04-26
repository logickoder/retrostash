# Development Guidelines (Strict)

This file is the source of truth for day-to-day engineering standards in this repository.

## 1. Core Rule: No Behavior Change Without Tests

1. Every behavior change MUST include tests in the same pull request.
2. Bug fixes MUST include a regression test that fails before the fix and passes after.
3. New public APIs MUST include at least one usage-level test.
4. Refactors that alter control flow MUST include safety tests, even if behavior is intended to stay
   the same.
5. PRs that change logic without tests are not merge-ready.

## 2. Test Policy by Change Type

1. Core engine logic (`retrostash-core`): add/update unit tests in `commonTest`.
2. OkHttp adapter logic (`retrostash-okhttp`): add/update JVM unit tests in `src/jvmTest`.
3. Ktor plugin behavior (`retrostash-ktor`): add/update KMP/JVM tests for plugin metadata and
   interception behavior.
4. Annotation/metadata parsing changes: add parser/extractor tests for old and new annotation
   shapes.
5. Serialization/key-resolution changes: include nested body, escaped string, missing placeholder,
   and happy-path tests.

## 3. Definition of Done (DoD)

A change is done only when all are true:

1. Code compiles.
2. Relevant tests are present and pass.
3. Existing module checks pass.
4. Public API changes include docs or KDoc updates.
5. No known warnings are introduced without rationale.

## 4. Required Local Validation Before Merge

Run the minimum impacted checks plus the baseline suite:

1. `./gradlew :retrostash-core:jvmTest :retrostash-core:iosSimulatorArm64Test`
2. `./gradlew :retrostash-okhttp:jvmTest :retrostash-okhttp:assemble`
3. `./gradlew :retrostash-ktor:jvmTest :retrostash-ktor:iosSimulatorArm64Test`
4. `./gradlew :retrostash-annotations:assemble`

If a change is module-specific, run its focused tests first, then run affected integration modules.

## 5. API and Compatibility Rules

1. Preserve backward compatibility unless intentionally versioned as breaking.
2. If introducing a replacement API, keep compatibility aliases for at least one release cycle.
3. Document migration steps for any breaking changes.
4. Avoid transport-coupled behavior in core modules.

## 6. Code Review Rules

1. Keep PRs narrowly scoped.
2. Review for behavior regressions first, style second.
3. Reject "works locally" claims without test proof.
4. Any skipped test requirement must include explicit written justification.

## 7. Commit Hygiene

1. Use clear commit messages describing behavior impact.
2. Separate mechanical edits from behavioral changes when possible.
3. Do not mix unrelated refactors with feature work.

## 8. Documentation Rules

1. Update docs when public API or integration flow changes.
2. Keep examples aligned with current module names and APIs.
3. Never leave stale snippets that reference removed symbols.

## 9. Exceptions

Exceptions are rare and must include:

1. Reason tests are not added.
2. Risk assessment.
3. Follow-up issue with owner and due date.

Without all three, the exception is invalid.
