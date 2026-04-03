package server

import (
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"net/http"
	"time"

	"github.com/google/uuid"
	"github.com/redis/go-redis/v9"
	clientv3 "go.etcd.io/etcd/client/v3"
)

type Server struct {
	etcd       *clientv3.Client
	redis      *redis.Client
	sessionTTL time.Duration
}

type credentials struct {
	Login    string `json:"login"`
	Password string `json:"password"`
}

func New(etcd *clientv3.Client, redis *redis.Client, sessionTTL time.Duration) *Server {
	return &Server{etcd: etcd, redis: redis, sessionTTL: sessionTTL}
}

func (s *Server) Handler() http.Handler {
	mux := http.NewServeMux()
	mux.HandleFunc("/health", s.health)
	mux.HandleFunc("/register", s.register)
	mux.HandleFunc("/login", s.login)
	return mux
}

func (s *Server) health(w http.ResponseWriter, _ *http.Request) {
	w.WriteHeader(http.StatusOK)
	_, _ = w.Write([]byte("ok"))
}

func decodeCreds(r *http.Request) (credentials, error) {
	var c credentials
	if err := json.NewDecoder(r.Body).Decode(&c); err != nil {
		return c, err
	}
	if c.Login == "" || c.Password == "" {
		return c, errors.New("login and password are required")
	}
	return c, nil
}

func (s *Server) register(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
		return
	}
	creds, err := decodeCreds(r)
	if err != nil {
		http.Error(w, err.Error(), http.StatusBadRequest)
		return
	}

	ctx, cancel := context.WithTimeout(r.Context(), 3*time.Second)
	defer cancel()

	key := "users/" + creds.Login
	resp, err := s.etcd.Get(ctx, key)
	if err != nil {
		http.Error(w, "etcd unavailable", http.StatusServiceUnavailable)
		return
	}
	if len(resp.Kvs) > 0 {
		http.Error(w, "user already exists", http.StatusConflict)
		return
	}
	if _, err = s.etcd.Put(ctx, key, creds.Password); err != nil {
		http.Error(w, "failed to save user", http.StatusInternalServerError)
		return
	}
	w.WriteHeader(http.StatusCreated)
}

func (s *Server) login(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
		return
	}
	creds, err := decodeCreds(r)
	if err != nil {
		http.Error(w, err.Error(), http.StatusBadRequest)
		return
	}

	ctx, cancel := context.WithTimeout(r.Context(), 3*time.Second)
	defer cancel()

	resp, err := s.etcd.Get(ctx, "users/"+creds.Login)
	if err != nil || len(resp.Kvs) == 0 {
		http.Error(w, "invalid credentials", http.StatusUnauthorized)
		return
	}
	if string(resp.Kvs[0].Value) != creds.Password {
		http.Error(w, "invalid credentials", http.StatusUnauthorized)
		return
	}

	token := uuid.NewString()
	if err := s.redis.Set(ctx, "session:"+token, creds.Login, s.sessionTTL).Err(); err != nil {
		http.Error(w, "failed to create session", http.StatusInternalServerError)
		return
	}

	w.Header().Set("Content-Type", "application/json")
	_ = json.NewEncoder(w).Encode(map[string]string{"token": token})
}

func ListenAddr(port int) string {
	return fmt.Sprintf(":%d", port)
}
