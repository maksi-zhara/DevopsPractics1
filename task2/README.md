# Local run

## With Docker Compose

```bash
docker compose up --build
```

Open `http://localhost:8081`

## Services

- `frontend/` stores static `html + js`
- `backend` in `backend/` exposes Flask API on `/api/hello`
- `frontend` is a separate container and reads `API_BASE_URL` at runtime
- `backend` allows browser access from the separate frontend container during local development
- in Kubernetes, `Ingress` can route `/` to frontend and `/api` to backend, so frontend can use `/api`

## API check

```bash
curl http://localhost:8080/api/hello
```

Expected response:

```json
{"message":"Hello Lev","name":"Lev","source":"environment variable NAME"}
```

## Backend only

```bash
docker build -t hello-flask-backend ./backend
docker run --rm -p 8080:8080 -e NAME=Lev hello-flask-backend
```

## Frontend only

```bash
docker build -t hello-frontend ./frontend
docker run --rm -p 8081:80 -e API_BASE_URL=http://localhost:8080/api hello-frontend
```

## Kubernetes deploy

Manifests are stored in `k8s/` and use:

- `Namespace` for isolation from other tasks on the same cluster
- `ConfigMap` for `NAME` and `API_BASE_URL`
- `Deployment` for `backend` and `frontend`
- `Service` for internal routing
- `Ingress` for `/` -> frontend and `/api` -> backend

Before apply, replace placeholder images in `k8s/02-backend.yaml` and `k8s/03-frontend.yaml`:

- `docker.io/your-dockerhub-user/hello-backend:latest`
- `docker.io/your-dockerhub-user/hello-frontend:latest`

Apply manifests:

```bash
kubectl apply -f k8s/00-namespace.yaml
kubectl apply -f k8s/
```

Check resources:

```bash
kubectl get all -n task2
kubectl get ingress -n task2
```

If the cluster does not already have an ingress controller, install one first. Example for `ingress-nginx`:

```bash
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/main/deploy/static/provider/cloud/deploy.yaml
```

Then test:

```bash
kubectl describe ingress hello-app -n task2
curl http://<INGRESS-IP>/
curl http://<INGRESS-IP>/api/hello
```
