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

var (
	iconCache           = icon.NewCache()
	activeNotifications = make(map[string]uint32) // key → D-Bus notification ID
	notificationsMutex  sync.RWMutex
)

// Send sends a desktop notification using the provided request data.
func Send(req *Request) error {
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

	id, err := ntf.Show()
	if err != nil {
		return fmt.Errorf("showing notification: %w", err)
	}

	// Store the notification ID if key is provided
	if req.Key != "" {
		notificationsMutex.Lock()
		activeNotifications[req.Key] = id
		notificationsMutex.Unlock()
	}

	return nil
}

// Dismiss closes an active notification by its key.
func Dismiss(key string) error {
	notificationsMutex.Lock()
	defer notificationsMutex.Unlock()

	id, exists := activeNotifications[key]
	if !exists {
		return nil // Notification already dismissed or never shown
	}

	delete(activeNotifications, key)
	return notify.CloseNotification(id)
}
