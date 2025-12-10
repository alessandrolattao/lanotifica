// Package main is the entry point for the LaNotifica server.
package main

import (
	"crypto/tls"
	"log"
	"net/http"
	"os"
	"os/signal"
	"syscall"
	"time"

	"github.com/alessandrolattao/lanotifica/internal/cert"
	"github.com/alessandrolattao/lanotifica/internal/config"
	"github.com/alessandrolattao/lanotifica/internal/handler"
	"github.com/alessandrolattao/lanotifica/internal/mdns"
)

func main() {
	cfg, err := config.Load()
	if err != nil {
		log.Fatalf("Failed to load config: %v", err)
	}

	log.Printf("Config loaded from %s", config.Path())

	// Load or generate TLS certificate
	certificate, err := cert.LoadOrCreate(cert.ConfigDir())
	if err != nil {
		log.Fatalf("Failed to load/create certificate: %v", err)
	}

	log.Printf("Certificate fingerprint: %s", certificate.Fingerprint)

	// Start mDNS server
	mdnsServer, err := mdns.Start(cfg.Port)
	if err != nil {
		log.Printf("Warning: mDNS failed to start: %v", err)
		log.Printf("The server will still work, but you'll need to use IP address instead of lanotifica.local")
	}

	mux := http.NewServeMux()
	mux.HandleFunc("/", handler.HomeHandler(cfg.Secret, certificate.Fingerprint))
	mux.HandleFunc("/favicon.png", handler.FaviconHandler())
	mux.HandleFunc("/health", handler.Health)
	mux.HandleFunc("/notification", handler.AuthMiddleware(cfg.Secret, handler.Notification))
	mux.HandleFunc("/notification/dismiss", handler.AuthMiddleware(cfg.Secret, handler.DismissNotification))

	server := &http.Server{
		Addr:              cfg.Port,
		Handler:           mux,
		ReadHeaderTimeout: 5 * time.Second,
		ReadTimeout:       time.Duration(cfg.ReadTimeout) * time.Second,
		WriteTimeout:      time.Duration(cfg.WriteTimeout) * time.Second,
		IdleTimeout:       time.Duration(cfg.IdleTimeout) * time.Second,
		TLSConfig: &tls.Config{
			Certificates: []tls.Certificate{certificate.TLSCert},
			MinVersion:   tls.VersionTLS12,
		},
	}

	// Handle graceful shutdown
	go func() {
		sigChan := make(chan os.Signal, 1)
		signal.Notify(sigChan, syscall.SIGINT, syscall.SIGTERM)
		<-sigChan

		log.Println("Shutting down...")
		if mdnsServer != nil {
			_ = mdnsServer.Stop()
		}
		os.Exit(0)
	}()

	log.Printf("LaNotifica server started on port %s", cfg.Port)
	log.Printf("Open https://localhost%s in your browser to see the QR code", cfg.Port)

	if err := server.ListenAndServeTLS("", ""); err != nil {
		log.Fatalf("Failed to start server: %v", err)
	}
}
