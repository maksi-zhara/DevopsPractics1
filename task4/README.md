# Local run

## With Docker Compose

```bash
docker compose up --build
```

Open `http://localhost:8081`

## Services

- `frontend/` stores static `html + js`
- `backend` in `backend/` exposes Flask API on `/api/people`
- `frontend` is a separate container and reads `API_BASE_URL` at runtime
- `backend` stores and reads data from PostgreSQL
- in Kubernetes, `Gateway API` routes `/` to frontend and `/api` to backend

## API check

```bash
curl http://localhost:8080/api/people
```

Expected response:

```json
{
  "message":"People loaded from PostgreSQL",
  "count":3,
  "people":[
    {"id":1,"first_name":"Lev","last_name":"Sidorov","middle_name":"Petrovich","age":23}
  ]
}
```

## Helm layout

Helm charts are stored in `charts/`:

- `charts/platform` for cluster-level components
- `charts/task4` for the application stack

## Platform chart

`charts/platform` is intended to manage:

- `cert-manager` through Helm dependency
- `CloudNativePG` operator through Helm dependency
- `NGINX Gateway Fabric` through Helm dependency
- `Gateway API` as Helm-managed CRDs

The chart now vendors the official `Gateway API` standard CRDs in a local dependency chart `gateway-api-crds`.

By default, all platform Helm dependencies are installed into the same Helm release namespace, for example `platform-system`.

## Application chart

`charts/task4` now manages:

- `backend` Deployment and Service
- `frontend` Deployment and Service
- `CloudNativePG Cluster` instead of manually deployed PostgreSQL
- `Gateway` and `HTTPRoute`
- `cert-manager` issuers and certificates for self-signed TLS
- app `ConfigMap` and database bootstrap `Secret`

The backend connects to the `CloudNativePG` read-write service `<cluster-name>-rw`.

## Kubernetes deploy

Push these application images to Docker Hub before applying:

- `docker.io/moz1h1gh/task4-backend:latest`
- `docker.io/moz1h1gh/task4-frontend:latest`

Build and push images:

```bash
docker login
docker build -t moz1h1gh/task4-backend:latest ./backend
docker build -t moz1h1gh/task4-frontend:latest ./frontend
docker push moz1h1gh/task4-backend:latest
docker push moz1h1gh/task4-frontend:latest
```

Install chart dependencies first:

```bash
helm dependency update charts/platform
```

Create the application namespace before installing `platform`, because `NGINX Gateway Fabric` is configured to watch only `task4`:

```bash
kubectl create namespace task4
```

Install platform components.

This environment required `hostNetwork` for the `cert-manager` webhook and the `CloudNativePG` webhook, otherwise the Kubernetes API server could not reach them through pod networking:

```bash
helm upgrade --install platform charts/platform \
  -n platform-system \
  --create-namespace \
  --set nginxgatewayfabric.nginxGateway.gatewayClassName=task4-nginx \
  --set certmanager.startupapicheck.enabled=false \
  --set certmanager.webhook.hostNetwork=true \
  --set certmanager.webhook.securePort=10260 \
  --set cloudnativepg.hostNetwork=true \
  --set cloudnativepg.webhook.port=9444
```

Because this cluster has no `StorageClass`, create a static `PersistentVolume` for `CloudNativePG`:

```bash
sudo mkdir -p /data/task4-cnpg
sudo chmod 777 /data/task4-cnpg
```

```bash
cat <<'EOF' | kubectl apply -f -
apiVersion: v1
kind: PersistentVolume
metadata:
  name: task4-db-pv
spec:
  capacity:
    storage: 1Gi
  accessModes:
    - ReadWriteOnce
  persistentVolumeReclaimPolicy: Retain
  storageClassName: ""
  hostPath:
    path: /data/task4-cnpg
EOF
```

Install application chart:

```bash
helm upgrade --install task4 charts/task4 \
  -n task4 \
  --create-namespace \
  --set gateway.gatewayClassName=task4-nginx
```

Render manifests locally for review:

```bash
helm template task4 charts/task4 -n task4
```

Check resources:

```bash
kubectl get all -n task4
kubectl get cluster -n task4
kubectl get gateway,httproute -n task4
```

Check that `CloudNativePG` and the app are up:

```bash
kubectl get pods -n task4 -w
kubectl get clusters.postgresql.cnpg.io -n task4
kubectl logs deployment/task4-task4-backend -n task4
```

Check platform components:

```bash
kubectl get crd gateways.gateway.networking.k8s.io
kubectl get deploy -n platform-system
kubectl get gatewayclass
```

Check database storage:

```bash
kubectl get pv
kubectl get pvc -n task4
```

Check certificates:

```bash
kubectl get issuer,certificate -n task4
kubectl get secret task4-gateway-tls -n task4
```

Find the NodePort gateway service:

```bash
kubectl get svc -n task4
kubectl get gateway -n task4
kubectl describe gateway task4-task4-gateway -n task4
```

Get node addresses:

```bash
kubectl get nodes -o wide
```

Example from this environment:

- `NODE-IP`: `192.168.56.11`
- `HTTP NodePort`: `30441`
- `HTTPS NodePort`: `31073`

Then test:

```bash
curl -I -H 'Host: task4.local' http://192.168.56.11:30441/
curl -I -H 'Host: task4.local' http://192.168.56.11:30441/api/people
curl -k --resolve task4.local:31073:192.168.56.11 https://task4.local:31073/
curl -k --resolve task4.local:31073:192.168.56.11 https://task4.local:31073/api/people
curl -k -X POST --resolve task4.local:31073:192.168.56.11 https://task4.local:31073/api/people/random
```

The HTTP checks return `301 Moved Permanently` by design because the chart redirects HTTP traffic to HTTPS.
