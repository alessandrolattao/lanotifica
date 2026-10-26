package notification

import (
	"slices"
	"sync"
	"sync/atomic"
	"testing"
	"time"

	"github.com/TheCreeper/go-notify"
)

// fakeBus mimics a notification daemon: it keeps the replaced ID or hands out a new one.
type fakeBus struct {
	replaces []uint32
	mu       sync.Mutex
	nextID   uint32
}

func (b *fakeBus) show(replacesID uint32) uint32 {
	b.mu.Lock()
	defer b.mu.Unlock()

	b.replaces = append(b.replaces, replacesID)
	if replacesID != 0 {
		return replacesID
	}
	b.nextID++
	return b.nextID
}

func TestSend_SameKeyConcurrently(t *testing.T) {
	t.Parallel()

	bus := &fakeBus{}
	firstShowing := make(chan struct{})
	secondShowing := make(chan struct{})
	var calls atomic.Int32
	n := &notifier{
		active: make(map[string]uint32),
		show: func(ntf notify.Notification) (uint32, error) {
			switch calls.Add(1) {
			case 1:
				close(firstShowing)
				// Give the second request time to reach D-Bus while the first one is in flight
				select {
				case <-secondShowing:
				case <-time.After(100 * time.Millisecond):
				}
			case 2:
				close(secondShowing)
			}
			return bus.show(ntf.ReplacesID), nil
		},
		closeID: func(uint32) error { return nil },
	}

	const key = "0|com.whatsapp|1|null|10100"
	errs := make(chan error, 2)
	go func() { errs <- n.send(&Request{Key: key, Message: "first"}) }()
	<-firstShowing
	go func() { errs <- n.send(&Request{Key: key, Message: "second"}) }()

	for range 2 {
		if err := <-errs; err != nil {
			t.Fatalf("send: %v", err)
		}
	}

	// The second request must replace the first notification, not show a new one
	if want := []uint32{0, 1}; !slices.Equal(bus.replaces, want) {
		t.Errorf("ReplacesID per D-Bus call = %v, want %v", bus.replaces, want)
	}
}
