.PHONY: help build test up lint rpm deb clean

SERVER_DIR = server
GIT_VERSION := $(shell git describe --tags --always --dirty 2>/dev/null || echo "0.0.0")
VERSION ?= $(shell echo "$(GIT_VERSION)" | sed 's/^v//; s/-/./g')
DEB_VERSION := $(shell echo "$(VERSION)" | sed 's/^\([0-9]\)/\1/; t; s/^/0.0.0+git./')

help: ## Show available commands
	@echo "LA-notify - Available commands:"
	@echo ""
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-10s\033[0m %s\n", $$1, $$2}'
	@echo ""

build: ## Build the binary
	cd $(SERVER_DIR) && go build -ldflags "-s -w" -o ../bin/la-notify ./cmd/la-notify

test: ## Run tests
	cd $(SERVER_DIR) && go test -v ./...

lint: ## Run linter
	cd $(SERVER_DIR) && golangci-lint run

up: ## Start dev server with hot reload
	cd $(SERVER_DIR) && air

rpm: build ## Build RPM package
	mkdir -p ~/rpmbuild/{SOURCES,SPECS,BUILD,RPMS,SRPMS}
	tar --transform "s,^,la-notify-$(VERSION)/," \
	    -czf ~/rpmbuild/SOURCES/la-notify-$(VERSION).tar.gz \
	    bin/ packaging/ LICENSE
	rpmbuild -bb --define "version $(VERSION)" packaging/rpm/la-notify.spec

deb: build ## Build DEB package
	rm -rf /tmp/la-notify-deb
	mkdir -p dist
	mkdir -p /tmp/la-notify-deb/usr/bin
	mkdir -p /tmp/la-notify-deb/usr/lib/systemd/user
	mkdir -p /tmp/la-notify-deb/usr/share/doc/la-notify
	mkdir -p /tmp/la-notify-deb/DEBIAN
	cp bin/la-notify /tmp/la-notify-deb/usr/bin/
	cp packaging/la-notify.service /tmp/la-notify-deb/usr/lib/systemd/user/
	cp LICENSE /tmp/la-notify-deb/usr/share/doc/la-notify/copyright
	sed 's/$${VERSION}/$(DEB_VERSION)/' packaging/deb/DEBIAN/control > /tmp/la-notify-deb/DEBIAN/control
	cp packaging/deb/DEBIAN/postinst /tmp/la-notify-deb/DEBIAN/
	chmod 755 /tmp/la-notify-deb/DEBIAN/postinst
	dpkg-deb --root-owner-group --build /tmp/la-notify-deb dist/la-notify_$(DEB_VERSION)_amd64.deb

clean: ## Remove build artifacts
	rm -rf bin/ dist/
