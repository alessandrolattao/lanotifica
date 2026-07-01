// Package config provides configuration management for LaNotifica.
package config

import (
	"crypto/rand"
	"encoding/hex"
	"encoding/json"
	"fmt"
	"os"
	"path/filepath"
)

// DefaultPort is the default port for the LaNotifica server.
const DefaultPort = ":19420"

// Config represents the application configuration.
type Config struct {
	Port            string `json:"port"`
	Secret          string `json:"secret"`
	ReadTimeout     int    `json:"read_timeout_seconds"`
	WriteTimeout    int    `json:"write_timeout_seconds"`
	IdleTimeout     int    `json:"idle_timeout_seconds"`
	IconCacheMaxAge int    `json:"icon_cache_max_age_days"`
}

// DefaultConfig returns the default configuration.
func DefaultConfig() Config {
	return Config{
		Port:            DefaultPort,
		Secret:          generateSecret(),
		ReadTimeout:     10,
		WriteTimeout:    10,
		IdleTimeout:     120,
		IconCacheMaxAge: 180,
	}
}

func generateSecret() string {
	bytes := make([]byte, 32)
	if _, err := rand.Read(bytes); err != nil {
		panic(fmt.Sprintf("failed to generate secret: %v", err))
	}
	return hex.EncodeToString(bytes)
}

var configDir string
var configPath string

func init() {
	// Follow XDG Base Directory Specification.
	// Use $XDG_CONFIG_HOME/lanotifica or ~/.config/lanotifica.
	xdgConfig := os.Getenv("XDG_CONFIG_HOME")
	if xdgConfig == "" {
		home, _ := os.UserHomeDir()
		xdgConfig = filepath.Join(home, ".config")
	}
	configDir = filepath.Join(xdgConfig, "lanotifica")
	configPath = filepath.Join(configDir, "config.json")
}

// Load loads the configuration from the config file.
// If the file doesn't exist, it creates a default one.
func Load() (Config, error) {
	if _, err := os.Stat(configPath); os.IsNotExist(err) {
		if err := createDefault(); err != nil {
			return Config{}, fmt.Errorf("creating default config: %w", err)
		}
	}

	data, err := os.ReadFile(configPath) //nolint:gosec // configPath is controlled internally
	if err != nil {
		return Config{}, fmt.Errorf("reading config file: %w", err)
	}

	var cfg Config
	if err := json.Unmarshal(data, &cfg); err != nil {
		return Config{}, fmt.Errorf("parsing config file: %w", err)
	}

	return cfg, nil
}

func createDefault() error {
	if err := os.MkdirAll(configDir, 0o750); err != nil {
		return fmt.Errorf("creating config directory: %w", err)
	}

	cfg := DefaultConfig()
	data, err := json.MarshalIndent(cfg, "", "  ") //nolint:gosec // config serialization is intentional
	if err != nil {
		return fmt.Errorf("marshaling default config: %w", err)
	}

	if err := os.WriteFile(configPath, data, 0o600); err != nil {
		return fmt.Errorf("writing config file: %w", err)
	}

	return nil
}

// Path returns the path to the config file.
func Path() string {
	return configPath
}
