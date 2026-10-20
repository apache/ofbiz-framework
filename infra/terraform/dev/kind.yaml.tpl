# Rendered by terraform (infra/terraform/dev/main.tf) — placeholders
# __CLUSTER_NAME__ / __WORKER_REPLICAS__. Keep node/port layout in sync
# with platform/kind/cluster.yaml.
kind: Cluster
apiVersion: kind.x-k8s.io/v1alpha4
name: __CLUSTER_NAME__
nodes:
  - role: control-plane
    kubeadmConfigPatches:
      - |
        kind: InitConfiguration
        nodeRegistration:
          kubeletExtraArgs:
                node-labels: "ingress-ready=true"
    # Ingress ports for istio-ingressgateway (host 80/443 → node 80/443).
    extraPortMappings:
      - containerPort: 80
        hostPort: 80
        protocol: TCP
      - containerPort: 443
        hostPort: 443
        protocol: TCP
  - role: worker
    replicas: __WORKER_REPLICAS__
