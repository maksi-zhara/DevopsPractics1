package app

import (
	"auth-service-go/internal/config"
	"auth-service-go/internal/server"
	"context"
	"log"
	"net/http"
	"time"

	"github.com/redis/go-redis/v9"
	clientv3 "go.etcd.io/etcd/client/v3"
)

func Run(cfg config.Config) error {
	etcdClient, err := clientv3.New(clientv3.Config{Endpoints: cfg.Etcd.Endpoints, DialTimeout: 5 * time.Second})
	if err != nil {
		return err
	}
	defer etcdClient.Close()

	rdb := redis.NewClient(&redis.Options{Addr: cfg.Redis.Addr, DB: cfg.Redis.DB})
	if err := rdb.Ping(context.Background()).Err(); err != nil {
		return err
	}

	httpServer := server.New(etcdClient, rdb, time.Duration(cfg.Session.TTLSeconds)*time.Second)
	addr := server.ListenAddr(cfg.Server.Port)
	log.Printf("auth service started at %s", addr)
	return http.ListenAndServe(addr, httpServer.Handler())
}
