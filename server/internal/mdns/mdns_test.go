package mdns

import (
	"testing"
)

func TestParsePort_Valid(t *testing.T) {
	t.Parallel()

	testCases := []struct {
		name     string
		input    string
		expected int
	}{
		{"with colon", ":19420", 19420},
		{"without colon", "19420", 19420},
		{"different port", ":8080", 8080},
		{"standard https", ":443", 443},
	}

	for _, tc := range testCases {
		t.Run(tc.name, func(t *testing.T) {
			result, err := parsePort(tc.input)
			if err != nil {
				t.Fatalf("parsePort(%q) returned error: %v", tc.input, err)
			}
			if result != tc.expected {
				t.Errorf("parsePort(%q) = %d, expected %d", tc.input, result, tc.expected)
			}
		})
	}
}

func TestParsePort_Invalid(t *testing.T) {
	t.Parallel()

	testCases := []struct {
		name  string
		input string
	}{
		{"empty", ""},
		{"only colon", ":"},
		{"non-numeric", "abc"},
		{"colon with non-numeric", ":abc"},
		{"float", ":19420.5"},
	}

	for _, tc := range testCases {
		t.Run(tc.name, func(t *testing.T) {
			_, err := parsePort(tc.input)
			if err == nil {
				t.Errorf("parsePort(%q) expected error, got nil", tc.input)
			}
		})
	}
}

func TestServer_Stop_Nil(t *testing.T) {
	t.Parallel()

	// Test that Stop doesn't panic when server is nil
	s := &Server{server: nil}
	err := s.Stop()
	if err != nil {
		t.Errorf("Stop() on nil server returned error: %v", err)
	}
}
