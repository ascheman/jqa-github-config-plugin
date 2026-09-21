# jQAssistant GitHub Config Plugin — Claude session notes

This file orients a fresh Claude Code session for this repository.
Read it first; it is short on purpose.

## What this plugin is

A jQAssistant scanner plugin that parses **GitHub configuration files** found
under a repository's `.github/` directory and projects them into the jQAssistant
Neo4j store, where they can be queried with Cypher and constrained by
jQAssistant rules.

Sibling plugins by the same author live in `../jqa-ansible-plugin`,
`../jqa-groovy-plugin`, `../jqa-hcl-plugin` — same coding patterns and POM
shape are used here.

**Scope (today)**: GitHub Actions workflow YAML files
(`.github/workflows/*.yml`, `*.yaml`).

**Scope (future, deliberately reserved)**: other GitHub config files —
`dependabot.yml`, `CODEOWNERS`, issue / PR templates, `FUNDING.yml`, etc.
Hence the broader plugin name (`github-config`, not `gh-workflow`) and the
shared root label `:GitHub` on every node.

This plugin does **not** scan Git history, commits, issues, PRs, or anything
that lives in the GitHub API — that is a separate plugin's job.

## Module / package layout

```
net.aschemann.jqassistant.plugin.github.config
├── api.model/                      shared base
│   └── GitHubDescriptor            common label :GitHub
├── workflow.api.model/             workflow-domain descriptors
│   ├── WorkflowDescriptor          :GitHub:Workflow (also :File)
│   ├── TriggerDescriptor           :GitHub:Trigger
│   ├── JobDescriptor               :GitHub:Job
│   ├── StepDescriptor              :GitHub:Step
│   ├── ActionDescriptor            :GitHub:Action
│   ├── ScriptDescriptor            :GitHub:Script (a `run:` block)
│   ├── StrategyDescriptor          :GitHub:Strategy
│   ├── MatrixDimensionDescriptor   :GitHub:MatrixDimension
│   ├── MatrixCombinationDescriptor :GitHub:MatrixCombination (include / exclude)
│   ├── InputDescriptor             :GitHub:Input
│   ├── OutputDescriptor            :GitHub:Output
│   ├── SecretDescriptor            :GitHub:Secret
│   ├── EnvDescriptor               :GitHub:EnvironmentVariable
│   ├── WithDescriptor              :GitHub:WithParameter
│   ├── PermissionDescriptor        :GitHub:Permission
│   ├── ConcurrencyDescriptor       :GitHub:Concurrency
│   ├── ReusableWorkflowReferenceDescriptor   :GitHub:ReusableWorkflowReference
│   └── ServiceDescriptor           :GitHub:Service
└── impl.scanner/
    ├── WorkflowScannerPlugin       AbstractScannerPlugin entry point
    └── WorkflowParser              YAML-tree → descriptor mapping
```

When adding the next domain (e.g. Dependabot), put it under a new sibling
package `github.config.dependabot.api.model.*` and a new scanner under
`impl.scanner/`. Reuse `GitHubDescriptor` as the parent label.

## Relationships (current model)

- `(Workflow)-[:HAS_TRIGGER|HAS_JOB|HAS_INPUT|HAS_OUTPUT|HAS_SECRET|HAS_ENV|HAS_PERMISSION|HAS_CONCURRENCY]->(...)`
- `(Job)-[:HAS_STEP]->(Step)` — **order is carried in `Step.index`, not by relationship order** (XO does not preserve list order; tests must sort by `index`).
- `(Job)-[:NEEDS]->(Job)`, `(Job)-[:HAS_STRATEGY|HAS_ENV|HAS_PERMISSION|HAS_OUTPUT|HAS_SERVICE]->(...)`
- `(Job)-[:USES_REUSABLE_WORKFLOW]->(ReusableWorkflowReference)`, with `HAS_WITH` / `PASSES_SECRET`
- `(Step)-[:USES_ACTION]->(Action)`, `(Step)-[:RUNS_SCRIPT]->(Script)`, `(Step)-[:HAS_WITH|HAS_ENV]->(...)`
- `(Strategy)-[:HAS_MATRIX_DIMENSION|HAS_MATRIX_INCLUDE|HAS_MATRIX_EXCLUDE]->(...)`

## Build / test

Use the Maven wrapper for reproducibility:

```
./mvnw -B test
./mvnw -B install
```

Targets jQA `2.9.1` on Java 17. The pom imports `com.buschmais.jqassistant:bom`
for dependency management; the YAML library is `org.snakeyaml:snakeyaml-engine`
(YAML 1.2). Tests use `com.buschmais.jqassistant.core:test`
(`com.buschmais.jqassistant.core.test.plugin.AbstractPluginIT`); the embedded
store is Neo4j 5 in-memory, so no `add-opens` flags are needed.

## Important non-obvious gotchas

1. **Step / job order is in the `index` property**, not in XO list order.
   Tests must sort by `index` before doing positional assertions.

2. **`uses:` parsing has three kinds.**
   - `actions/checkout@v4` → `kind=marketplace`, `owner=actions`, `repoOrName=checkout`, `ref=v4`
   - `./.github/actions/foo` → `kind=local`, `repoOrName=<path>`
   - `docker://image:tag` → `kind=docker`, `repoOrName=<image:tag>`

   For job-level `uses:` (reusable workflow), parsing fills
   `owner` / `repo` / `path` / `ref` on `ReusableWorkflowReferenceDescriptor`.

## Conventions

- Java 17 source level.
- XO descriptor interfaces only — no concrete classes.
- Add new descriptors in `*.api.model.*` and register them in
  `src/main/resources/META-INF/jqassistant-plugin.xml` (the plugin loader
  ignores classes that are not registered there).
- The plugin manifest uses schema `http://schema.jqassistant.org/plugin/v2.4`;
  the `id` attribute regex is `[a-z][a-z0-9\.]+` — no hyphens (this plugin
  uses `jqa.plugin.githubconfig`).
- Coerce all YAML scalar values to `String` via `WorkflowParser.asString()`
  (numbers, booleans, etc. become their `toString()` form). Don't store
  raw `Object` in descriptors.

## How to extend (cookbook)

- **New workflow concept**: add a descriptor under
  `workflow.api.model`, register it in `jqassistant-plugin.xml`, parse it
  in `WorkflowParser`.
- **New GitHub-config domain** (e.g. CODEOWNERS): create
  `github.config.<domain>.api.model.*`, add a scanner plugin under
  `impl.scanner/<Domain>ScannerPlugin.java`, register both, and reuse
  `GitHubDescriptor` as the shared label root.

## Useful queries (Cypher)

```cypher
// Every distinct action used across all workflows
MATCH (:Workflow)-[:HAS_JOB]->(:Job)-[:HAS_STEP]->(:Step)-[:USES_ACTION]->(a:Action)
RETURN a.uses, count(*) AS occurrences ORDER BY occurrences DESC;

// Jobs that run a matrix
MATCH (j:Job)-[:HAS_STRATEGY]->(:Strategy)-[:HAS_MATRIX_DIMENSION]->(d)
RETURN j.id, collect(d.name);

// Reusable workflow callers
MATCH (j:Job)-[:USES_REUSABLE_WORKFLOW]->(r)
RETURN j.id, r.uses;
```
