# demo-svc — scaffold reference service (P0-F4)

Proves the build conventions end-to-end: `ofbiz-msa.*` plugins, ArchUnit
layering (red-green), gRPC codegen, distroless image, shared Helm chart.

```
./gradlew build                                  # tests + ArchUnit + contractTest
docker build -t demo-svc:dev .                   # from this directory
../helm/10-install.sh dev                        # deploy to kind (ofbiz namespace)
../helm/20-verify.sh                             # HPA/PDB + zero-downtime rolling update gate
```

Module layout matches FUTURESTATE-LOWLEVEL §1 (`domain` ← `application` ←
`adapters/{rest,grpc}`). Real services copy this shape; ArchUnit rules live in
`platform/arch-rules`.
