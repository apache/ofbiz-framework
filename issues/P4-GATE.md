# Gate CG-4 — Phase 4: order-svc

**Type:** phase completion gate
**Objective:** 
**Depends on:** all phase 4 stories

## Checklist (all must pass, evidence linked)
- [ ] Full order lifecycle (quote→order→approve→complete/cancel) via new stack in staging e2e test
- [ ] ECA-derived sagas: inventory reserve, invoice trigger, marketing attribution all fire on `order.order.placed`
- [ ] Monolith order writes disabled in staging; 30-day soak before prod cutover
- [ ] Consistency delta 0 vs monolith order tables; rollback drill done
