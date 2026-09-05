# cashier-app

Point-of-sale till for a Comic-Con booth. **Event: 31 October 2026.**

Single operator, single till, one laptop. Records sales, tracks stock, and
reconciles the cash tin against the opening float at close of day.

## Stack

Java 21 (LTS) + Spring Boot 4.1, packaged as a fat jar that also serves a Vite
React build from `src/main/resources/static/`. H2 in file mode. REST under
`/api`. One artifact, one process, one double-click.

Node 22 LTS. Built with the committed Maven Wrapper, so a clean checkout does
not need Maven installed.

## Running it

```
./mvnw clean package
java -jar target/cashier-app.jar
```

The live database, backups and logs live under `Desktop\comic-con_stuff\till\` - not
in this repo, and not beside the jar. See `config.example.properties`.

## Documentation

Project docs live one level up, outside this repo, in the Cowork folder:

| File                 | What it is                                               |
|----------------------|----------------------------------------------------------|
| `../build-plan.md`   | Phase-by-phase build order. **The document to follow.**  |
| `../brief.md`        | Settled brief: stack, scope, constraints, decisions log. |
| `../architecture.md` | Runtime shape, data model, build wiring, known traps.    |
| `../CLAUDE.md`       | Working rules for this project.                          |

`RUNBOOK.md` lands in Phase 6 - one page, printed, in the box with the cables.

## Branches

- `dev` - working branch. Commit and push every session, broken or not.
- `main` - protected. PR from `dev` only, and only on a green pipeline.

## Money

Every amount is stored as an integer in minor units. **No `double`, no
`float`, anywhere near a price.** Divide for display only; all arithmetic
happens on integers.

The exact unit - whole rupees or cents - is decided in Phase 2, before the
products table exists.
