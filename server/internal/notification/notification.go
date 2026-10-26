// Package notification provides functionality to send desktop notifications via D-Bus.
package notification

import (
	"fmt"
	"sync"

	"github.com/TheCreeper/go-notify"
	"github.com/alessandrolattao/lanotifica/internal/icon"
)

// Request represents a notification request from the client.
type Request struct {
	Key         string `json:"key"`
	AppName     string `json:"app_name"`
	PackageName string `json:"package_name"`
	Title       string `json:"title"`
	Message     string `json:"message"`
	Urgency     int    `json:"urgency"`    // 0=low, 1=normal, 2=critical
	TimeoutMs   int32  `json:"timeout_ms"` // -1=default, 0=never, >0=milliseconds
}

// DismissRequest represents a request to dismiss a notification.
type DismissRequest struct {
	Key string `json:"key"`
}

// notifier tracks the D-Bus notifications it shows, so a request with a known key replaces its notification.
type notifier struct {
	active  map[string]uint32 // key → D-Bus notification ID
	show    func(notify.Notification) (uint32, error)
	closeID func(uint32) error
	mu      sync.Mutex
}

var (
	iconCache       = icon.NewCache()
	defaultNotifier = &notifier{
		active:  make(map[string]uint32),
		show:    notify.Notification.Show,
		closeID: notify.CloseNotification,
	}
)

// Send sends a desktop notification using the provided request data.
// If a notification with the same key already exists, it is updated in place.
func Send(req *Request) error {
	return defaultNotifier.send(req)
}

// Dismiss closes an active notification by its key.
func Dismiss(key string) error {
	return defaultNotifier.dismiss(key)
}

func (n *notifier) send(req *Request) error {
	title := req.Title
	if title == "" {
		title = "Notification"
	}

	ntf := notify.NewNotification(title, req.Message)
	if req.AppName != "" {
		ntf.AppName = req.AppName
	}
	ntf.AppIcon = "preferences-system-notifications"
	ntf.Hints = make(map[string]any)

	if iconPath := iconCache.GetIconPath(req.PackageName); iconPath != "" {
		ntf.Hints[notify.HintImagePath] = "file://" + iconPath
	}

	// Set urgency hint (0=low, 1=normal, 2=critical)
	if req.Urgency >= 0 && req.Urgency <= 2 {
		ntf.Hints[notify.HintUrgency] = byte(req.Urgency)
	}

	// Set timeout (-1=default, 0=never, >0=milliseconds)
	switch {
	case req.TimeoutMs > 0:
		ntf.Timeout = req.TimeoutMs
	case req.TimeoutMs == 0:
		ntf.Timeout = notify.ExpiresNever
	}
	// else: use default (ExpiresDefault = -1)

	// Reuse existing D-Bus ID so the notification updates in place
	// instead of spawning a new one (handles progress updates, message stacking, etc.)
	// The lock spans the D-Bus call: two requests with the same key would otherwise
	// both find no ID and show two notifications instead of replacing one
	n.mu.Lock()
	defer n.mu.Unlock()

	if req.Key != "" {
		ntf.ReplacesID = n.active[req.Key]
	}

	id, err := n.show(ntf)
	if err != nil {
		return fmt.Errorf("showing notification: %w", err)
	}

	if req.Key != "" {
		n.active[req.Key] = id
	}

	return nil
}

func (n *notifier) dismiss(key string) error {
	n.mu.Lock()
	defer n.mu.Unlock()

	id, exists := n.active[key]
	if !exists {
		return nil // Notification already dismissed or never shown
	}

	delete(n.active, key)
	if err := n.closeID(id); err != nil {
		return fmt.Errorf("closing notification: %w", err)
	}
	return nil
}
