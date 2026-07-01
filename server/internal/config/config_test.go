package config

import (
	"os"
	"testing"
)

func TestDefaultConfig(t *testing.T) {
	t.Parallel()

	cfg := DefaultConfig()

	if cfg.Port != DefaultPort {
		t.Errorf("Expected port :19420, got %s", cfg.Port)
	}
	if cfg.ReadTimeout != 10 {
		t.Errorf("Expected read timeout 10, got %d", cfg.ReadTimeout)
	}
	if cfg.WriteTimeout != 10 {
		t.Errorf("Expected write timeout 10, got %d", cfg.WriteTimeout)
	}
	if cfg.IdleTimeout != 120 {
		t.Errorf("Expected idle timeout 120, got %d", cfg.IdleTimeout)
	}
	if cfg.IconCacheMaxAge != 180 {
		t.Errorf("Expected icon cache max age 180, got %d", cfg.IconCacheMaxAge)
	}
}

func TestLoad_CreatesDefaultIfNotExists(t *testing.T) {
	setupTestConfig(t)

	cfg, err := Load()
	if err != nil {
		t.Fatalf("Failed to load config: %v", err)
	}

	// Check that file was created.
	if _, err := os.Stat(configPath); os.IsNotExist(err) {
		t.Error("Config file was not created")
	}

	// Check default values.
	if cfg.Port != DefaultPort {
		t.Errorf("Expected port :19420, got %s", cfg.Port)
	}
}

func TestLoad_ReadsExistingConfig(t *testing.T) {
	setupTestConfig(t)

	// Create a custom config.
	if err := os.MkdirAll(configDir, 0o750); err != nil {
		t.Fatalf("Failed to create config dir: %v", err)
	}

	customConfig := `{
  "port": ":9090",
  "read_timeout_seconds": 30,
  "write_timeout_seconds": 30,
  "idle_timeout_seconds": 300,
  "icon_cache_max_age_days": 60
}`
	if err := os.WriteFile(configPath, []byte(customConfig), 0o600); err != nil {
		t.Fatalf("Failed to write config: %v", err)
	}

	cfg, err := Load()
	if err != nil {
		t.Fatalf("Failed to load config: %v", err)
	}

	if cfg.Port != ":9090" {
		t.Errorf("Expected port :9090, got %s", cfg.Port)
	}
	if cfg.ReadTimeout != 30 {
		t.Errorf("Expected read timeout 30, got %d", cfg.ReadTimeout)
	}
}

func TestPath(t *testing.T) {
	t.Parallel()

	path := Path()
	if path == "" {
		t.Error("Config path should not be empty")
	}
}
