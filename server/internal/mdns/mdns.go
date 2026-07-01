// Package mdns provides mDNS service registration for local network discovery.
package mdns

import (
	"errors"
	"fmt"
	"log"
	"net"
	"strconv"
	"strings"

	"github.com/hashicorp/mdns"
)

var errNoUsableIP = errors.New("no usable non-loopback IPv4 address found")

// Server wraps the mDNS server.
type Server struct {
	server *mdns.Server
}

// Start registers the LaNotifica service via mDNS.
// The service will be discoverable as "lanotifica.local" on the local network.
func Start(port string) (*Server, error) {
	portNum, err := parsePort(port)
	if err != nil {
		return nil, fmt.Errorf("parsing port: %w", err)
	}

	info := []string{"LaNotifica notification forwarder"}

	advertisedIP, err := advertisedIPv4()
	if err != nil {
		return nil, fmt.Errorf("determining advertised LAN address: %w", err)
	}

	service, err := mdns.NewMDNSService(
		"lanotifica",           // Instance name
		"_lanotifica._tcp",     // Service type
		"",                     // Domain (empty = .local)
		"",                     // Host name (empty = use system hostname)
		portNum,                // Port
		[]net.IP{advertisedIP}, // Explicit reachable LAN address
		info,                   // TXT records
	)
	if err != nil {
		return nil, fmt.Errorf("creating mDNS service: %w", err)
	}

	server, err := mdns.NewServer(&mdns.Config{Zone: service})
	if err != nil {
		return nil, fmt.Errorf("starting mDNS server: %w", err)
	}

	log.Printf("mDNS: advertising lanotifica at %s:%d via %s", service.HostName, portNum, advertisedIP)

	return &Server{server: server}, nil
}

// Stop shuts down the mDNS server.
func (s *Server) Stop() error {
	if s.server != nil {
		return s.server.Shutdown()
	}
	return nil
}

func parsePort(port string) (int, error) {
	p := strings.TrimPrefix(port, ":")
	return strconv.Atoi(p)
}

// advertisedIPv4 returns the local IPv4 address the kernel would use for mDNS
// traffic. Dialing UDP does not send any packet — it just asks the kernel which
// source address it would pick for that destination.
func advertisedIPv4() (net.IP, error) {
	if ip := routedIPv4(); ip != nil {
		return ip, nil
	}
	if ip := firstUsableIfaceIPv4(); ip != nil {
		return ip, nil
	}
	return nil, errNoUsableIP
}

// routedIPv4 asks the kernel which source IP it would use to reach the mDNS
// multicast group. No packet is sent.
func routedIPv4() net.IP {
	conn, err := net.DialUDP("udp4", nil, &net.UDPAddr{IP: net.IPv4(224, 0, 0, 251), Port: 5353})
	if err != nil {
		return nil
	}
	defer func() { _ = conn.Close() }()

	addr, ok := conn.LocalAddr().(*net.UDPAddr)
	if ok && isUsableIPv4(addr.IP) {
		return addr.IP.To4()
	}
	return nil
}

// firstUsableIfaceIPv4 scans network interfaces and returns the first usable
// non-loopback IPv4 address on a multicast-capable interface.
func firstUsableIfaceIPv4() net.IP {
	ifaces, err := net.Interfaces()
	if err != nil {
		return nil
	}
	for _, iface := range ifaces {
		if iface.Flags&net.FlagUp == 0 ||
			iface.Flags&net.FlagLoopback != 0 ||
			iface.Flags&net.FlagMulticast == 0 {
			continue
		}
		if ip := firstUsableIPv4OnIface(iface); ip != nil {
			return ip
		}
	}
	return nil
}

func firstUsableIPv4OnIface(iface net.Interface) net.IP {
	addrs, err := iface.Addrs()
	if err != nil {
		return nil
	}
	for _, addr := range addrs {
		var ip net.IP
		switch v := addr.(type) {
		case *net.IPNet:
			ip = v.IP
		case *net.IPAddr:
			ip = v.IP
		}
		if isUsableIPv4(ip) {
			return ip.To4()
		}
	}
	return nil
}

func isUsableIPv4(ip net.IP) bool {
	return ip != nil &&
		ip.To4() != nil &&
		!ip.IsLoopback() &&
		!ip.IsUnspecified() &&
		!ip.IsMulticast() &&
		!ip.IsLinkLocalUnicast()
}
