package handler

import (
	"bytes"
	"context"
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"testing"

	"github.com/alessandrolattao/lanotifica/internal/notification"
)

func TestNotification_Success(t *testing.T) {
	t.Parallel()

	body := notification.Request{
		AppName: "TestApp",
		Title:   "Test Title",
		Message: "Test message",
	}
	jsonBody, err := json.Marshal(body)
	if err != nil {
		t.Fatalf("Failed to marshal request: %v", err)
	}

	req := httptest.NewRequestWithContext(context.Background(), http.MethodPost, "/notification", bytes.NewReader(jsonBody))
	req.Header.Set("Content-Type", "application/json")

	rr := httptest.NewRecorder()
	Notification(rr, req)

	if rr.Code != http.StatusOK && rr.Code != http.StatusInternalServerError {
		t.Errorf("Expected status 200 or 500, got %d", rr.Code)
	}
}

func TestNotification_MethodNotAllowed(t *testing.T) {
	t.Parallel()

	req := httptest.NewRequestWithContext(context.Background(), http.MethodGet, "/notification", http.NoBody)
	rr := httptest.NewRecorder()

	Notification(rr, req)

	if rr.Code != http.StatusMethodNotAllowed {
		t.Errorf("Expected status 405, got %d", rr.Code)
	}
}

func TestNotification_InvalidJSON(t *testing.T) {
	t.Parallel()

	req := httptest.NewRequestWithContext(context.Background(), http.MethodPost, "/notification", bytes.NewReader([]byte("invalid json")))
	req.Header.Set("Content-Type", "application/json")

	rr := httptest.NewRecorder()
	Notification(rr, req)

	if rr.Code != http.StatusBadRequest {
		t.Errorf("Expected status 400, got %d", rr.Code)
	}
}

func TestNotification_MissingMessage(t *testing.T) {
	t.Parallel()

	body := notification.Request{
		Title: "Only title",
	}
	jsonBody, err := json.Marshal(body)
	if err != nil {
		t.Fatalf("Failed to marshal request: %v", err)
	}

	req := httptest.NewRequestWithContext(context.Background(), http.MethodPost, "/notification", bytes.NewReader(jsonBody))
	req.Header.Set("Content-Type", "application/json")

	rr := httptest.NewRecorder()
	Notification(rr, req)

	if rr.Code != http.StatusBadRequest {
		t.Errorf("Expected status 400, got %d", rr.Code)
	}
}

func TestNotification_WithPackageName(t *testing.T) {
	t.Parallel()

	body := notification.Request{
		AppName:     "WhatsApp",
		PackageName: "com.whatsapp",
		Title:       "Mario",
		Message:     "Ciao!",
	}
	jsonBody, err := json.Marshal(body)
	if err != nil {
		t.Fatalf("Failed to marshal request: %v", err)
	}

	req := httptest.NewRequestWithContext(context.Background(), http.MethodPost, "/notification", bytes.NewReader(jsonBody))
	req.Header.Set("Content-Type", "application/json")

	rr := httptest.NewRecorder()
	Notification(rr, req)

	if rr.Code != http.StatusOK && rr.Code != http.StatusInternalServerError {
		t.Errorf("Expected status 200 or 500, got %d", rr.Code)
	}
}

// Health handler tests

func TestHealth_Success(t *testing.T) {
	t.Parallel()

	req := httptest.NewRequestWithContext(context.Background(), http.MethodGet, "/health", http.NoBody)
	rr := httptest.NewRecorder()

	Health(rr, req)

	if rr.Code != http.StatusOK {
		t.Errorf("Expected status 200, got %d", rr.Code)
	}

	contentType := rr.Header().Get("Content-Type")
	if contentType != "application/json" {
		t.Errorf("Expected Content-Type application/json, got %s", contentType)
	}

	var response HealthResponse
	if err := json.Unmarshal(rr.Body.Bytes(), &response); err != nil {
		t.Fatalf("Failed to unmarshal response: %v", err)
	}

	if response.Status != "ok" {
		t.Errorf("Expected status 'ok', got '%s'", response.Status)
	}
}

func TestHealth_MethodNotAllowed(t *testing.T) {
	t.Parallel()

	methods := []string{http.MethodPost, http.MethodPut, http.MethodDelete, http.MethodPatch}

	for _, method := range methods {
		t.Run(method, func(t *testing.T) {
			req := httptest.NewRequestWithContext(context.Background(), method, "/health", http.NoBody)
			rr := httptest.NewRecorder()

			Health(rr, req)

			if rr.Code != http.StatusMethodNotAllowed {
				t.Errorf("Expected status 405 for %s, got %d", method, rr.Code)
			}
		})
	}
}

// Home handler tests

func TestHomeHandler_Success(t *testing.T) {
	t.Parallel()

	handler := HomeHandler("test-secret", "test-fingerprint", "test")

	req := httptest.NewRequestWithContext(context.Background(), http.MethodGet, "/", http.NoBody)
	rr := httptest.NewRecorder()

	handler(rr, req)

	if rr.Code != http.StatusOK {
		t.Errorf("Expected status 200, got %d", rr.Code)
	}

	contentType := rr.Header().Get("Content-Type")
	if contentType != "text/html; charset=utf-8" {
		t.Errorf("Expected Content-Type text/html, got %s", contentType)
	}
}

func TestHomeHandler_ContainsQRCode(t *testing.T) {
	t.Parallel()

	handler := HomeHandler("test-secret", "test-fingerprint", "test")

	req := httptest.NewRequestWithContext(context.Background(), http.MethodGet, "/", http.NoBody)
	rr := httptest.NewRecorder()

	handler(rr, req)

	body := rr.Body.String()

	// Check for QR code base64 image
	if !bytes.Contains([]byte(body), []byte("data:image/png;base64,")) {
		t.Error("Expected response to contain base64 QR code image")
	}

	// Check for essential HTML elements
	if !bytes.Contains([]byte(body), []byte("LaNotifica")) {
		t.Error("Expected response to contain 'LaNotifica' title")
	}
}

func TestHomeHandler_NotFoundForOtherPaths(t *testing.T) {
	t.Parallel()

	handler := HomeHandler("test-secret", "test-fingerprint", "test")

	paths := []string{"/other", "/api", "/test"}

	for _, path := range paths {
		t.Run(path, func(t *testing.T) {
			req := httptest.NewRequestWithContext(context.Background(), http.MethodGet, path, http.NoBody)
			rr := httptest.NewRecorder()

			handler(rr, req)

			if rr.Code != http.StatusNotFound {
				t.Errorf("Expected status 404 for path %s, got %d", path, rr.Code)
			}
		})
	}
}

func TestFaviconHandler(t *testing.T) {
	t.Parallel()

	handler := FaviconHandler()

	req := httptest.NewRequestWithContext(context.Background(), http.MethodGet, "/favicon.png", http.NoBody)
	rr := httptest.NewRecorder()

	handler(rr, req)

	if rr.Code != http.StatusOK {
		t.Errorf("Expected status 200, got %d", rr.Code)
	}

	contentType := rr.Header().Get("Content-Type")
	if contentType != "image/png" {
		t.Errorf("Expected Content-Type image/png, got %s", contentType)
	}

	cacheControl := rr.Header().Get("Cache-Control")
	if cacheControl != "public, max-age=86400" {
		t.Errorf("Expected Cache-Control header, got %s", cacheControl)
	}

	if rr.Body.Len() == 0 {
		t.Error("Expected non-empty body")
	}
}
