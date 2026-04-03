FROM golang:1.22-alpine AS builder

WORKDIR /app

COPY go.mod go.sum ./
RUN go mod download

COPY . .

RUN CGO_ENABLED=0 GOOS=linux go build -o auth-service ./cmd/auth-service

FROM alpine:3.19

WORKDIR /app

COPY --from=builder /app/auth-service .
COPY --from=builder /app/config.yml .

EXPOSE 8081

CMD ["./auth-service"]

