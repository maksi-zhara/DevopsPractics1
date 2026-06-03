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

## Kubernetes deploy

Manifests are stored in `k8s/` and use:

- `Namespace` for isolation from other tasks on the same cluster
- `ConfigMap` for app and PostgreSQL connection settings
- `Secret` for PostgreSQL password
- `StatefulSet` for PostgreSQL with `volumeClaimTemplates`
- `Deployment` for `backend` and `frontend`
- `Service` for frontend, backend, and PostgreSQL
- `Gateway` and `HTTPRoute` instead of `Ingress`

Push these application images to Docker Hub before applying:

- `docker.io/moz1h1gh/task3-backend:latest`
- `docker.io/moz1h1gh/task3-frontend:latest`

Install Gateway API CRDs and NGINX Gateway Fabric `NodePort` controller first:

```bash
kubectl kustomize "https://github.com/nginx/nginx-gateway-fabric/config/crd/gateway-api/standard?ref=v2.6.0" | kubectl apply -f -
kubectl apply --server-side -f https://raw.githubusercontent.com/nginx/nginx-gateway-fabric/v2.6.0/deploy/crds.yaml
kubectl apply -f https://raw.githubusercontent.com/nginx/nginx-gateway-fabric/v2.6.0/deploy/nodeport/deploy.yaml
```

The default installation creates `GatewayClass` `nginx`.

Apply manifests:

```bash
kubectl apply -f k8s/
```

Because this cluster has no `StorageClass`, create the manual `PersistentVolume` too:

```bash
sudo mkdir -p /data/task3-postgres
sudo chmod 777 /data/task3-postgres
kubectl apply -f k8s/06-postgres-pv.yaml
kubectl apply -f k8s/
```

Check resources:

```bash
kubectl get all -n task3
kubectl get gateway,httproute -n task3
kubectl get pvc -n task3
```

Check that PostgreSQL is ready and the app is up:

```bash
kubectl get pods -n task3 -w
kubectl logs deployment/task3-backend -n task3
```

Find the NodePort gateway service:

```bash
kubectl get svc -n nginx-gateway
kubectl get gateway -n task3
kubectl describe gateway task3-gateway -n task3
```

Then test:

```bash
curl http://<NODE-IP>:<NODEPORT>/
curl http://<NODE-IP>:<NODEPORT>/api/people
curl -X POST http://<NODE-IP>:<NODEPORT>/api/people/random
```
