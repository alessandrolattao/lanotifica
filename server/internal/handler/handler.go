// Package handler provides HTTP handlers for the notification server.
package handler

import (
	"encoding/json"
	"log"
	"net/http"

	"github.com/alessandrolattao/lanotifica/internal/notification"
)

// Notification handles POST requests to send notifications and DELETE to dismiss.
func Notification(w http.ResponseWriter, r *http.Request) {
	switch r.Method {
	case http.MethodPost:
		handleNotificationPost(w, r)
	case http.MethodDelete:
		handleNotificationDelete(w, r)
	default:
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
	}
}

func handleNotificationPost(w http.ResponseWriter, r *http.Request) {
	var req notification.Request
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		http.Error(w, "Invalid JSON body", http.StatusBadRequest)
		return
	}

	if req.Message == "" {
		http.Error(w, "Message is required", http.StatusBadRequest)
		return
	}

	if err := notification.Send(&req); err != nil {
		log.Printf("Failed to send notification: %v", err)
		http.Error(w, "Failed to send notification", http.StatusInternalServerError)
		return
	}

	w.WriteHeader(http.StatusOK)
	if err := json.NewEncoder(w).Encode(map[string]string{"status": "sent"}); err != nil {
		log.Printf("Error encoding response: %v", err)
	}
}

func handleNotificationDelete(w http.ResponseWriter, r *http.Request) {
	var req notification.DismissRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		http.Error(w, "Invalid JSON body", http.StatusBadRequest)
		return
	}

	if req.Key == "" {
		http.Error(w, "Key is required", http.StatusBadRequest)
		return
	}

	if err := notification.Dismiss(req.Key); err != nil {
		log.Printf("Failed to dismiss notification: %v", err)
		http.Error(w, "Failed to dismiss notification", http.StatusInternalServerError)
		return
	}

	w.WriteHeader(http.StatusOK)
	if err := json.NewEncoder(w).Encode(map[string]string{"status": "dismissed"}); err != nil {
		log.Printf("Error encoding response: %v", err)
	}
}
