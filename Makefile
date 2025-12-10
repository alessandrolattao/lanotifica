.PHONY: build test up down clean lint

SERVER_DIR = server

build:
	cd $(SERVER_DIR) && go build -o ../bin/la-notify ./cmd/la-notify

test:
	cd $(SERVER_DIR) && go test -v ./...

lint:
	cd $(SERVER_DIR) && golangci-lint run

up:
	cd $(SERVER_DIR) && air

down:
	@pkill -f la-notify || echo "Server not running"

clean: down
	rm -rf bin
