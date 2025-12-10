.PHONY: help build test test-server test-app up lint lint-server lint-app format format-server format-app rpm deb clean

SERVER_DIR = server
APP_DIR = app
GIT_VERSION := $(shell git describe --tags --always --dirty 2>/dev/null || echo "0.0.0")
VERSION ?= $(shell echo "$(GIT_VERSION)" | sed 's/^v//; s/-/./g')
DEB_VERSION := $(shell echo "$(VERSION)" | sed 's/^\([0-9]\)/\1/; t; s/^/0.0.0+git./')

help: ## Show available commands
	@echo "LaNotifica - Available commands:"
	@echo ""
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-10s\033[0m %s\n", $$1, $$2}'
	@echo ""

build: ## Build the binary
	cd $(SERVER_DIR) && go build -ldflags "-s -w" -o ../bin/lanotifica ./cmd/lanotifica

test: test-server test-app ## Run all tests

test-server: ## Run server tests
	cd $(SERVER_DIR) && go mod download && go test -v ./...

test-app: ## Run Android app tests
	cd $(APP_DIR) && ./gradlew test --quiet

lint: lint-server lint-app ## Run all linters

lint-server: ## Run server linter
	@which golangci-lint > /dev/null || go install github.com/golangci/golangci-lint/cmd/golangci-lint@latest
	cd $(SERVER_DIR) && golangci-lint run

lint-app: ## Run Android app linter
	cd $(APP_DIR) && ./gradlew lint

format: format-server format-app ## Format all code

format-server: ## Format Go code
	cd $(SERVER_DIR) && go fmt ./...

format-app: ## Format Kotlin code
	cd $(APP_DIR) && ./gradlew spotlessApply

up: ## Start dev server with hot reload
	cd $(SERVER_DIR) && air

rpm: build ## Build RPM package
	mkdir -p ~/rpmbuild/{SOURCES,SPECS,BUILD,RPMS,SRPMS}
	tar --transform "s,^,lanotifica-$(VERSION)/," \
	    -czf ~/rpmbuild/SOURCES/lanotifica-$(VERSION).tar.gz \
	    bin/ packaging/ LICENSE
	rpmbuild -bb --define "version $(VERSION)" packaging/rpm/lanotifica.spec

deb: build ## Build DEB package
	rm -rf /tmp/lanotifica-deb
	mkdir -p dist
	mkdir -p /tmp/lanotifica-deb/usr/bin
	mkdir -p /tmp/lanotifica-deb/usr/lib/systemd/user
	mkdir -p /tmp/lanotifica-deb/usr/share/doc/lanotifica
	mkdir -p /tmp/lanotifica-deb/DEBIAN
	cp bin/lanotifica /tmp/lanotifica-deb/usr/bin/
	cp packaging/lanotifica.service /tmp/lanotifica-deb/usr/lib/systemd/user/
	cp LICENSE /tmp/lanotifica-deb/usr/share/doc/lanotifica/copyright
	sed 's/$${VERSION}/$(DEB_VERSION)/' packaging/deb/DEBIAN/control > /tmp/lanotifica-deb/DEBIAN/control
	cp packaging/deb/DEBIAN/postinst /tmp/lanotifica-deb/DEBIAN/
	chmod 755 /tmp/lanotifica-deb/DEBIAN/postinst
	dpkg-deb --root-owner-group --build /tmp/lanotifica-deb dist/lanotifica_$(DEB_VERSION)_amd64.deb

clean: ## Remove build artifacts
	rm -rf bin/ dist/
