package main

import (
	"log"
	"os"

	"auth-service-go/internal/app"
	"auth-service-go/internal/config"
)

func main() {
	cfgPath := os.Getenv("AUTH_CONFIG")
	if cfgPath == "" {
		cfgPath = "config.yml"
	}

	cfg, err := config.Load(cfgPath)
	if err != nil {
		log.Fatalf("failed to load config: %v", err)
	}

	if err := app.Run(cfg); err != nil {
		log.Fatalf("auth service error: %v", err)
	}
}
