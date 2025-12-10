.PHONY: test up down lint

SERVER_DIR = server

test:
	cd $(SERVER_DIR) && go test -v ./...

lint:
	cd $(SERVER_DIR) && golangci-lint run

up:
	cd $(SERVER_DIR) && air

down:
	@pkill -f la-notify || echo "Server not running"
