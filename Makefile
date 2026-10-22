.DEFAULT_GOAL := help

# Image and Registry configuration
APP_NAME    ?= blood-broker
VERSION     ?= latest
AWS_REGION  ?= eu-north-1
ECR_REGISTRY ?= 192905952216.dkr.ecr.$(AWS_REGION).amazonaws.com
IMAGE_URI   ?= $(ECR_REGISTRY)/$(APP_NAME):$(VERSION)

.PHONY: help clean compile test package build run docker-build docker-run docker-stop docker-logs ecr-login docker-push ci

help: ## Show available commands
	@echo "======================================================================"
	@echo " Blood Donation Cloud Network — Development & CI/CD Makefile"
	@echo "======================================================================"
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-18s\033[0m %s\n", $$1, $$2}'

## --- Local Build & Test ---
clean: ## Clean build artifacts and target folder
	mvn clean

compile: ## Compile source code
	mvn compile

test: ## Run unit and integration tests with coverage
	mvn test

package: ## Build executable Spring Boot JAR (skipping tests)
	mvn package -DskipTests

build: ## Full clean build with test execution and coverage verification
	mvn clean verify

run: ## Run application locally with Spring Boot
	mvn spring-boot:run

## --- Docker & Containers ---
docker-build: ## Build local Docker container image
	docker build -t $(APP_NAME):$(VERSION) .

docker-run: ## Start application container in background using docker-compose
	docker compose up -d

docker-stop: ## Stop and remove application container
	docker compose down

docker-logs: ## Follow application container logs
	docker compose logs -f

## --- AWS ECR & Deployment ---
ecr-login: ## Authenticate local Docker daemon with AWS ECR
	aws ecr get-login-password --region $(AWS_REGION) | docker login --username AWS --password-stdin $(ECR_REGISTRY)

docker-push: docker-build ## Tag and push image to AWS ECR
	docker tag $(APP_NAME):$(VERSION) $(IMAGE_URI)
	docker push $(IMAGE_URI)

## --- CI Pipeline ---
ci: build docker-build ## Run full local CI check (tests + JAR build + Docker build)

