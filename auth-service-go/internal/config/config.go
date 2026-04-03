package config

import (
	"fmt"
	"os"
	"strconv"

	"gopkg.in/yaml.v3"
)

type Config struct {
	Server struct {
		Port int `yaml:"port"`
	} `yaml:"server"`
	Etcd struct {
		Endpoints []string `yaml:"endpoints"`
	} `yaml:"etcd"`
	Redis struct {
		Addr string `yaml:"addr"`
		DB   int    `yaml:"db"`
	} `yaml:"redis"`
	Session struct {
		TTLSeconds int `yaml:"ttl_seconds"`
	} `yaml:"session"`
}

func Load(path string) (Config, error) {
	cfg := Config{}
	b, err := os.ReadFile(path)
	if err != nil {
		return cfg, err
	}
	if err := yaml.Unmarshal(b, &cfg); err != nil {
		return cfg, err
	}

	if p := os.Getenv("AUTH_PORT"); p != "" {
		port, err := strconv.Atoi(p)
		if err != nil {
			return cfg, fmt.Errorf("invalid AUTH_PORT: %w", err)
		}
		cfg.Server.Port = port
	}
	if e := os.Getenv("ETCD_ENDPOINTS"); e != "" {
		cfg.Etcd.Endpoints = []string{e}
	}
	if a := os.Getenv("REDIS_ADDR"); a != "" {
		cfg.Redis.Addr = a
	}
	if t := os.Getenv("SESSION_TTL_SECONDS"); t != "" {
		ttl, err := strconv.Atoi(t)
		if err != nil {
			return cfg, fmt.Errorf("invalid SESSION_TTL_SECONDS: %w", err)
		}
		cfg.Session.TTLSeconds = ttl
	}

	return cfg, nil
}
