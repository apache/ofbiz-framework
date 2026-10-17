# Gate CG-3 — Phase 3: product-svc + inventory-svc

**Type:** phase completion gate
**Objective:** 
**Depends on:** all phase 3 stories

## Checklist (all must pass, evidence linked)
- [ ] Catalog browse + price lookup via new APIs in SPA
- [ ] Stock reserve/release saga works; oversell impossible under concurrency (100 parallel reserves, 0 over-reserved)
- [ ] search-svc indexes products on change events; search p99 < 100ms @ 100k docs
- [ ] CDC cutover + consistency delta 0 for product/shipment slices; rollback drill done
