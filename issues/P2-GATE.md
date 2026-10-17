# Gate CG-2 — Phase 2: party-svc + CDC Bridge

**Type:** phase completion gate
**Objective:** first domain migrated; CDC interop pattern proven end-to-end (the template for all later phases).
**Depends on:** all phase 2 stories

## Checklist (all must pass, evidence linked)
- [ ] party-svc owns party schema (133 entities → JPA subset for live slices; historical archive documented)
- [ ] Debezium streams monolith party tables → `ofbiz.party.*` topics (lag < 5s at 100 msg/s test)
- [ ] Write cutover done in staging: gateway routes `/api/v1/parties/**` to party-svc; reverse-sync keeps monolith tables current; consistency job delta = 0 on a 10k-party dataset
- [ ] Rollback drill executed: route flip back, zero data loss
- [ ] SPA (skeleton) lists/searches parties via new API
