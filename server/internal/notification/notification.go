// Package notification provides functionality to send desktop notifications via D-Bus.
package notification

import (
	"fmt"

	"github.com/TheCreeper/go-notify"
	"github.com/alessandrolattao/la-notify/internal/icon"
)

// Request represents a notification request from the client.
type Request struct {
	AppName     string `json:"app_name"`
	PackageName string `json:"package_name"`
	Title       string `json:"title"`
	Message     string `json:"message"`
}

var iconCache = icon.NewCache()

// Send sends a desktop notification using the provided request data.
func Send(req Request) error {
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

	_, err := ntf.Show()
	if err != nil {
		return fmt.Errorf("showing notification: %w", err)
	}

	return nil
}
