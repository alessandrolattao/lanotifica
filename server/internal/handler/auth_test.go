package handler

import (
	"context"
	"net/http"
	"net/http/httptest"
	"testing"
)

const testSecret = "test-secret-token"

func TestAuthMiddleware_ValidToken(t *testing.T) {
	t.Parallel()

	nextCalled := false

	next := func(w http.ResponseWriter, _ *http.Request) {
		nextCalled = true
		w.WriteHeader(http.StatusOK)
	}

	handler := AuthMiddleware(testSecret, next)

	req := httptest.NewRequestWithContext(context.Background(), http.MethodGet, "/test", http.NoBody)
	req.Header.Set("Authorization", "Bearer "+testSecret)

	rr := httptest.NewRecorder()
	handler(rr, req)

	if !nextCalled {
		t.Error("Expected next handler to be called")
	}
	if rr.Code != http.StatusOK {
		t.Errorf("Expected status 200, got %d", rr.Code)
	}
}

func TestAuthMiddleware_MissingHeader(t *testing.T) {
	t.Parallel()

	nextCalled := false

	next := func(_ http.ResponseWriter, _ *http.Request) {
		nextCalled = true
	}

	handler := AuthMiddleware(testSecret, next)

	req := httptest.NewRequestWithContext(context.Background(), http.MethodGet, "/test", http.NoBody)
	// No Authorization header

	rr := httptest.NewRecorder()
	handler(rr, req)

	if nextCalled {
		t.Error("Expected next handler NOT to be called")
	}
	if rr.Code != http.StatusUnauthorized {
		t.Errorf("Expected status 401, got %d", rr.Code)
	}
}

func TestAuthMiddleware_InvalidFormat(t *testing.T) {
	t.Parallel()

	nextCalled := false

	next := func(_ http.ResponseWriter, _ *http.Request) {
		nextCalled = true
	}

	handler := AuthMiddleware(testSecret, next)

	testCases := []struct {
		name   string
		header string
	}{
		{"Basic auth", "Basic " + testSecret},
		{"No space", "Bearer" + testSecret},
		{"Only token", testSecret},
		{"Empty bearer", "Bearer"},
	}

	for _, tc := range testCases {
		t.Run(tc.name, func(t *testing.T) {
			req := httptest.NewRequestWithContext(context.Background(), http.MethodGet, "/test", http.NoBody)
			req.Header.Set("Authorization", tc.header)

			rr := httptest.NewRecorder()
			handler(rr, req)

			if nextCalled {
				t.Error("Expected next handler NOT to be called")
			}
			if rr.Code != http.StatusUnauthorized {
				t.Errorf("Expected status 401, got %d", rr.Code)
			}
		})
	}
}

func TestAuthMiddleware_WrongToken(t *testing.T) {
	t.Parallel()

	nextCalled := false

	next := func(_ http.ResponseWriter, _ *http.Request) {
		nextCalled = true
	}

	handler := AuthMiddleware(testSecret, next)

	req := httptest.NewRequestWithContext(context.Background(), http.MethodGet, "/test", http.NoBody)
	req.Header.Set("Authorization", "Bearer wrong-token")

	rr := httptest.NewRecorder()
	handler(rr, req)

	if nextCalled {
		t.Error("Expected next handler NOT to be called")
	}
	if rr.Code != http.StatusUnauthorized {
		t.Errorf("Expected status 401, got %d", rr.Code)
	}
}

func TestAuthMiddleware_EmptyToken(t *testing.T) {
	t.Parallel()

	nextCalled := false

	next := func(_ http.ResponseWriter, _ *http.Request) {
		nextCalled = true
	}

	handler := AuthMiddleware(testSecret, next)

	req := httptest.NewRequestWithContext(context.Background(), http.MethodGet, "/test", http.NoBody)
	req.Header.Set("Authorization", "Bearer ")

	rr := httptest.NewRecorder()
	handler(rr, req)

	if nextCalled {
		t.Error("Expected next handler NOT to be called")
	}
	if rr.Code != http.StatusUnauthorized {
		t.Errorf("Expected status 401, got %d", rr.Code)
	}
}
