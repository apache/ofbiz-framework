# ADR-014: Edge Stack — Spring Auth Server, Spring Cloud Gateway, Angular 17+ SPA

**Status**: Accepted
**Date**: 2026-10-16
**Authors**: Ken Semerkant + agent brainstorming session
**Related**: ADR-007, ADR-010

## Context
The monolith's auth (userLogin + Security interface + JWTManager) and UI
(widget screens + FreeMarker themes) need target-state replacements
(D5, D6, D9). Headless APIs were chosen with an SPA frontend.

## Decision
- **identity-svc**: Spring Authorization Server (OIDC; auth-code+PKCE,
  client-credentials). Issues JWTs carrying party roles sourced from
  party-svc events. Credentials live only here.
- **gateway**: Spring Cloud Gateway — route table, JWKS validation,
  rate limiting (Redis), and the strangler default route (ADR-012).
- **SPA**: Angular 17+ (NOT AngularJS 1.x — EOL since 2022; confirmed
  correction during brainstorming). Nx workspace; consumes only gateway
  REST.

## Rationale
1. Standards-based OIDC avoids vendor lock-in; permission model maps
   cleanly from OFBiz roles.
2. Gateway is the single place strangler routing and rollback live.
3. One modern frontend stack; widget/theme knowledge does not carry over.

## Consequences
**Positive:** centralized authn; SPA replaces 6 theme stacks.
**Negative:** SPA is a real build stream (PROJECT-PLAN R7); token
revocation latency (5s SLA) must be engineered via events.

## Alternatives Considered
- **Keycloak:** faster start, another product to operate.
- **Server-rendered Spring UI:** contradicts headless decision (D5).

## References
FUTURESTATE-ARCHITECTURE.md §6.1-6.2, IMPLEMENTATION.md P1-F1/F2
