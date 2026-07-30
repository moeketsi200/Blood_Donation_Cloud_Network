# 🐳 Blood Integration Broker — ECR Push & ECS Fargate Deployment (Step 3)

## 1. Overview

In Step 3, you package your Spring Boot application as a Docker container, push the image to **Amazon ECR (Elastic Container Registry)**, and configure an **Amazon ECS (Elastic Container Service) Fargate Task** to run your broker in the cloud.

---

## 2. Resources Created in this Step

| Resource | AWS Service | Detail / URI |
| :--- | :--- | :--- |
| **ECR Repository** | Amazon ECR | `192905952216.dkr.ecr.eu-north-1.amazonaws.com/blood-broker` |
| **Task Definition** | Amazon ECS | `ecs-task-def.json` (Fargate, 0.5 vCPU, 1GB RAM) |
| **Container Image** | Docker | `blood-broker:latest` |

---

## 3. Step-by-Step Execution Guide

### Part A — Build, Tag, and Push Docker Image to ECR

1. **Authenticate Docker with AWS ECR:**
   ```bash
   ./aws/dist/aws ecr get-login-password --region eu-north-1 | docker login --username AWS --password-stdin 192905952216.dkr.ecr.eu-north-1.amazonaws.com
   ```

2. **Tag local Docker image for ECR:**
   ```bash
   docker tag blood-broker:latest 192905952216.dkr.ecr.eu-north-1.amazonaws.com/blood-broker:latest
   ```

3. **Push image to ECR:**
   ```bash
   docker push 192905952216.dkr.ecr.eu-north-1.amazonaws.com/blood-broker:latest
   ```

---

### Part B — Register the ECS Task Definition

1. **Verify `ecs-task-def.json` in your project root:**
   - Image: `192905952216.dkr.ecr.eu-north-1.amazonaws.com/blood-broker:latest`
   - CPU: `512` (0.5 vCPU)
   - Memory: `1024` (1 GB RAM)
   - Port: `8080`

2. **Register the task definition in AWS:**
   ```bash
   ./aws/dist/aws ecs register-task-definition --cli-input-json file://ecs-task-def.json --region eu-north-1
   ```

---

### Part C — Run as a Fargate Service in AWS Console

1. Open **AWS Console → Amazon ECS → Clusters**.
2. Click **Create Cluster** (Name: `blood-broker-cluster`, Infrastructure: **AWS Fargate**).
3. Under Services, click **Create**.
4. Select Family: `blood-broker-task`.
5. Launch type: **FARGATE**.
6. Set Desired Tasks: `1`.
7. Click **Create** — AWS will automatically pull your Docker container from ECR and start polling your SQS queue in the cloud!
