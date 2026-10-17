# ADR-008: Object Storage — S3 API Contract, Garage for Local Profile

**Status**: Accepted
**Date**: 2026-10-17
**Authors**: Ken Semerkant + agent (Phase 0 execution)
**Related**: ADR-007, platform/storage/

## Context
Content and audit services need object storage; the target is cloud-native,
so storage must be S3-API. For local development the default choice, MinIO,
is no longer pullable: MinIO removed its Docker Hub images in 2025 and the
Bitnami mirrors were swept in the same registry purge. Attempted pulls of
minio/minio (Docker Hub), quay.io/minio/minio, bitnami/minio all failed.

## Decision
Standardize on the **S3 API as the only storage contract** services may code
against (no vendor SDK lock-in). Local profile runs **Garage v2.1.0**
(S3-compatible, lightweight, active on Docker Hub); production swaps
endpoint + credentials to cloud S3 with zero code change. Buckets:
ofbiz-content, ofbiz-audit.

## Rationale
1. Registry reality: MinIO unobtainable; blocking local dev on it is waste.
2. Garage satisfies the same contract (verified: put/get roundtrip green).
3. The contract — not the engine — is the architectural decision.

## Consequences
**Positive:** cloud-native ready; local dev unblocked; documented swap path.
**Negative:** Garage operational quirks (layout bootstrap, key secret shown
once at creation) live in platform/storage/20-verify.sh.

## Alternatives Considered
- **SeaweedFS / Zenko:** viable; Garage verified first, kept.
- **Vendoring MinIO image:** private registry overhead for local dev only.

## References
platform/storage/garage.yaml, platform/storage/20-verify.sh
