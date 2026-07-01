// Package cert handles TLS certificate generation and management.
package cert

import (
	"crypto/ecdsa"
	"crypto/elliptic"
	"crypto/rand"
	"crypto/sha256"
	"crypto/tls"
	"crypto/x509"
	"crypto/x509/pkix"
	"encoding/hex"
	"encoding/pem"
	"errors"
	"fmt"
	"math/big"
	"net"
	"os"
	"path/filepath"
	"strings"
	"time"
)

var errPEMDecode = errors.New("failed to decode PEM")

// Certificate holds the TLS certificate and its fingerprint.
type Certificate struct {
	TLSCert     tls.Certificate
	Fingerprint string
}

// LoadOrCreate loads existing certificates or creates new ones if they don't exist.
func LoadOrCreate(configDir string) (*Certificate, error) {
	certPath := filepath.Join(configDir, "cert.pem")
	keyPath := filepath.Join(configDir, "key.pem")

	if fileExists(certPath) && fileExists(keyPath) {
		return load(certPath, keyPath)
	}

	return create(certPath, keyPath)
}

func fileExists(path string) bool {
	_, err := os.Stat(path)
	return err == nil
}

func load(certPath, keyPath string) (*Certificate, error) {
	tlsCert, err := tls.LoadX509KeyPair(certPath, keyPath)
	if err != nil {
		return nil, fmt.Errorf("loading certificate: %w", err)
	}

	fingerprint, err := calculateFingerprint(certPath)
	if err != nil {
		return nil, fmt.Errorf("calculating fingerprint: %w", err)
	}

	return &Certificate{
		TLSCert:     tlsCert,
		Fingerprint: fingerprint,
	}, nil
}

func create(certPath, keyPath string) (*Certificate, error) {
	privateKey, err := ecdsa.GenerateKey(elliptic.P256(), rand.Reader)
	if err != nil {
		return nil, fmt.Errorf("generating private key: %w", err)
	}

	serialNumber, err := rand.Int(rand.Reader, new(big.Int).Lsh(big.NewInt(1), 128))
	if err != nil {
		return nil, fmt.Errorf("generating serial number: %w", err)
	}

	template := x509.Certificate{
		SerialNumber: serialNumber,
		Subject: pkix.Name{
			Organization: []string{"LaNotifica"},
			CommonName:   "LaNotifica Server",
		},
		NotBefore:             time.Now(),
		NotAfter:              time.Now().AddDate(10, 0, 0), // Valid for 10 years
		KeyUsage:              x509.KeyUsageKeyEncipherment | x509.KeyUsageDigitalSignature,
		ExtKeyUsage:           []x509.ExtKeyUsage{x509.ExtKeyUsageServerAuth},
		BasicConstraintsValid: true,
	}

	// Add all local IPs and common names
	template.IPAddresses = getLocalIPs()
	template.DNSNames = []string{"localhost", "lanotifica.local"}

	certDER, err := x509.CreateCertificate(rand.Reader, &template, &template, &privateKey.PublicKey, privateKey)
	if err != nil {
		return nil, fmt.Errorf("creating certificate: %w", err)
	}

	// Save certificate
	certFile, err := os.Create(certPath) //nolint:gosec // certPath is controlled internally
	if err != nil {
		return nil, fmt.Errorf("creating cert file: %w", err)
	}
	defer func() { _ = certFile.Close() }()

	if encodeErr := pem.Encode(certFile, &pem.Block{Type: "CERTIFICATE", Bytes: certDER}); encodeErr != nil {
		return nil, fmt.Errorf("encoding certificate: %w", encodeErr)
	}

	//nolint:gosec // keyPath is controlled internally
	keyFile, err := os.OpenFile(keyPath, os.O_WRONLY|os.O_CREATE|os.O_TRUNC, 0o600)
	if err != nil {
		return nil, fmt.Errorf("creating key file: %w", err)
	}
	defer func() { _ = keyFile.Close() }()

	keyDER, err := x509.MarshalECPrivateKey(privateKey)
	if err != nil {
		return nil, fmt.Errorf("marshaling private key: %w", err)
	}

	if err := pem.Encode(keyFile, &pem.Block{Type: "EC PRIVATE KEY", Bytes: keyDER}); err != nil {
		return nil, fmt.Errorf("encoding private key: %w", err)
	}

	// Load the newly created certificate
	return load(certPath, keyPath)
}

func calculateFingerprint(certPath string) (string, error) {
	//nolint:gosec // certPath is controlled internally
	certPEM, err := os.ReadFile(certPath)
	if err != nil {
		return "", fmt.Errorf("reading certificate: %w", err)
	}

	block, _ := pem.Decode(certPEM)
	if block == nil {
		return "", errPEMDecode
	}

	hash := sha256.Sum256(block.Bytes)
	return strings.ToUpper(hex.EncodeToString(hash[:])), nil
}

func getLocalIPs() []net.IP {
	var ips []net.IP
	ips = append(ips, net.ParseIP("127.0.0.1"))

	addrs, err := net.InterfaceAddrs()
	if err != nil {
		return ips
	}

	for _, addr := range addrs {
		if ipnet, ok := addr.(*net.IPNet); ok && !ipnet.IP.IsLoopback() {
			if ipnet.IP.To4() != nil {
				ips = append(ips, ipnet.IP)
			}
		}
	}

	return ips
}

// ConfigDir returns the config directory path.
func ConfigDir() string {
	xdgConfig := os.Getenv("XDG_CONFIG_HOME")
	if xdgConfig == "" {
		home, _ := os.UserHomeDir()
		xdgConfig = filepath.Join(home, ".config")
	}
	return filepath.Join(xdgConfig, "lanotifica")
}
