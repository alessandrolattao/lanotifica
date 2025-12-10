.PHONY: build test up down clean lint

build:
	go build -o bin/la-notify ./cmd/la-notify

test:
	go test -v ./...

lint:
	golangci-lint run

up:
	air

down:
	@pkill -f la-notify || echo "Server non in esecuzione"

clean: down
	rm -rf bin
