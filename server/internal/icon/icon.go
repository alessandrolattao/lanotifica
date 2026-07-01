// Package icon provides functionality to fetch and cache Android app icons from the Play Store.
package icon

import (
	"context"
	"errors"
	"fmt"
	"io"
	"net/http"
	"os"
	"path/filepath"
	"regexp"
	"strings"
	"time"
)

var (
	// ErrPlayStoreNotFound is returned when the app is not found on Play Store.
	ErrPlayStoreNotFound = errors.New("app not found on Play Store")
	// ErrNoIconFound is returned when no icon URL is found in the Play Store page.
	ErrNoIconFound = errors.New("no icon found in Play Store page")
	// ErrInvalidPackageName is returned when the package name fails validation.
	ErrInvalidPackageName = errors.New("invalid package name")
)

var (
	httpClient = &http.Client{
		Timeout: 10 * time.Second,
	}

	validPackageName = regexp.MustCompile(`^[a-zA-Z][a-zA-Z0-9_]*(\.[a-zA-Z][a-zA-Z0-9_]*)+$`)
	iconURLPattern   = regexp.MustCompile(`https://play-lh\.googleusercontent\.com/[^"'\s]+`)
)

const iconURLPrefix = "https://play-lh.googleusercontent.com/"

// Cache manages the icon cache directory.
type Cache struct {
	dir string
}

// NewCache creates a new icon cache using XDG_CACHE_HOME/lanotifica/icons.
func NewCache() *Cache {
	xdgCache := os.Getenv("XDG_CACHE_HOME")
	if xdgCache == "" {
		home, _ := os.UserHomeDir()
		xdgCache = filepath.Join(home, ".cache")
	}
	return &Cache{
		dir: filepath.Join(xdgCache, "lanotifica", "icons"),
	}
}

// GetIconPath returns the cached icon path for the given package name.
// If the icon is not in cache, it downloads it from Play Store first.
// Returns empty string if the icon cannot be obtained.
func (c *Cache) GetIconPath(packageName string) string {
	if packageName == "" {
		return ""
	}

	if !validPackageName.MatchString(packageName) {
		return ""
	}

	iconPath := c.buildIconPath(packageName)

	if c.existsInCache(iconPath) {
		return iconPath
	}

	if err := c.downloadAndCache(packageName, iconPath); err != nil {
		return ""
	}

	return iconPath
}

func (c *Cache) buildIconPath(packageName string) string {
	return filepath.Join(c.dir, packageName+".png")
}

func (c *Cache) existsInCache(iconPath string) bool {
	_, err := os.Stat(iconPath)
	return err == nil
}

func (c *Cache) downloadAndCache(packageName, iconPath string) error {
	ctx, cancel := context.WithTimeout(context.Background(), 30*time.Second)
	defer cancel()

	iconURL, err := c.fetchIconURLFromPlayStore(ctx, packageName)
	if err != nil {
		return err
	}

	return c.saveIconToCache(ctx, iconURL, iconPath)
}

func (c *Cache) fetchIconURLFromPlayStore(ctx context.Context, packageName string) (string, error) {
	playStoreURL := "https://play.google.com/store/apps/details?id=" + packageName

	req, err := http.NewRequestWithContext(ctx, http.MethodGet, playStoreURL, http.NoBody)
	if err != nil {
		return "", fmt.Errorf("creating request: %w", err)
	}

	resp, err := httpClient.Do(req)
	if err != nil {
		return "", fmt.Errorf("fetching Play Store page: %w", err)
	}
	defer func() { _ = resp.Body.Close() }()

	if resp.StatusCode == http.StatusNotFound {
		return "", ErrPlayStoreNotFound
	}
	if resp.StatusCode != http.StatusOK {
		return "", fmt.Errorf("%w: status %d", ErrPlayStoreNotFound, resp.StatusCode)
	}

	body, err := io.ReadAll(resp.Body)
	if err != nil {
		return "", fmt.Errorf("reading response body: %w", err)
	}

	matches := iconURLPattern.FindAllString(string(body), -1)
	if len(matches) == 0 {
		return "", ErrNoIconFound
	}

	iconURL := matches[0]
	if !strings.HasPrefix(iconURL, iconURLPrefix) {
		return "", ErrNoIconFound
	}

	return iconURL, nil
}

func (c *Cache) saveIconToCache(ctx context.Context, iconURL, iconPath string) error {
	iconReq, err := http.NewRequestWithContext(ctx, http.MethodGet, iconURL, http.NoBody)
	if err != nil {
		return fmt.Errorf("creating icon request: %w", err)
	}

	iconResp, err := httpClient.Do(iconReq)
	if err != nil {
		return fmt.Errorf("downloading icon: %w", err)
	}
	defer func() { _ = iconResp.Body.Close() }()

	if mkdirErr := os.MkdirAll(c.dir, 0o750); mkdirErr != nil {
		return fmt.Errorf("creating cache dir: %w", mkdirErr)
	}

	tmp, err := os.CreateTemp(c.dir, "icon-*.png.tmp")
	if err != nil {
		return fmt.Errorf("creating temp file: %w", err)
	}
	tmpPath := tmp.Name()

	if _, err := io.Copy(tmp, iconResp.Body); err != nil {
		_ = tmp.Close()
		_ = os.Remove(tmpPath)
		return fmt.Errorf("saving icon to file: %w", err)
	}

	if err := tmp.Close(); err != nil {
		_ = os.Remove(tmpPath)
		return fmt.Errorf("closing temp file: %w", err)
	}

	if err := os.Rename(tmpPath, iconPath); err != nil {
		_ = os.Remove(tmpPath)
		return fmt.Errorf("renaming temp file: %w", err)
	}

	return nil
}
