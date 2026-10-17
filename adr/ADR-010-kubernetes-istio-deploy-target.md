# ADR-010: Kubernetes + Istio Mesh Deploy Target (amends Docker Compose rule)

**Status**: Accepted
**Date**: 2026-10-16
**Authors**: Ken Semerkant + agent brainstorming session
**Related**: ADR-007, ADR-009, platform/

## Context
16 services need mTLS, retries, traffic shifting for strangler cutovers,
and horizontal scale. Library-based resilience per service duplicates
policy; the team's global Docker Compose default cannot express mesh
semantics. D8 chose a service mesh, which implies Kubernetes.

## Decision
Kubernetes + Istio (Linkerd acceptable fallback) is the deploy target —
**scoped amendment** to the global Docker Compose rule for this program.
Mesh-wide STRICT PeerAuthentication (verified: plaintext rejected,
mTLS-only calls succeed). Dev profile: kind cluster (Docker Desktop)
running the same stack; Compose remains valid for out-of-cluster test
suites only.

## Rationale
1. mTLS + AuthorizationPolicy per service with zero service code.
2. DestinationRule retries/timeouts centralize resilience policy.
3. Gateway route flips are the strangler cutover + rollback mechanism
   (ADR-012), expressible only with an edge router over K8s services.

## Consequences
**Positive:** uniform security posture; cutover/rollback = route change.
**Negative:** platform learning curve (PROJECT-PLAN R3); heavier local dev.

## Alternatives Considered
- **Docker Compose + Resilience4j:** contradicts chosen mesh (D8).
- **Linkerd:** lighter; Istio chosen for AuthorizationPolicy expressiveness.

## References
platform/kind/, platform/bootstrap/10-verify-mtls.sh
