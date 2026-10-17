# Gate CG-5 — Phase 5: billing-svc + finance-svc

**Type:** phase completion gate
**Objective:** 
**Depends on:** all phase 5 stories

## Checklist (all must pass, evidence linked)
- [ ] SME-approved entity split (FUTURESTATE-LOWLEVEL §2.1) frozen in a migration mapping doc
- [ ] Invoice issue→settle e2e; payment gateway adapter (test gateway) auth/capture/void
- [ ] GL auto-posting from billing events; trial balance equals monolith trial balance on migrated dataset
- [ ] notification-svc sends invoice emails; PCI scope documented as confined to billing-svc
