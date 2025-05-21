# Spring Boot & Kubernetes CI/CD Project with Jenkins

This project demonstrates a complete CI/CD (Continuous Integration/Continuous Deployment) pipeline for a simple Spring Boot web application. The pipeline is managed by Jenkins and involves building the application, containerizing it with Docker, pushing the image to Docker Hub, and finally deploying it to a Kubernetes cluster (running on Docker Desktop).

## Project Overview

The core components and workflow are as follows:

1.  **Spring Boot Application:** A simple "Hello World" style web application with a single REST endpoint (`/merhaba`).
2.  **Docker:** The Spring Boot application is packaged into a Docker image using a `Dockerfile`.
3.  **Docker Hub:** The built Docker image is pushed to Docker Hub as a public repository.
4.  **Kubernetes (via Docker Desktop):** The application is deployed to a local Kubernetes cluster provided by Docker Desktop.
    * A `deployment.yaml` file defines how the application should run (image, replicas, ports).
    * A `service.yaml` file exposes the application externally using a `NodePort`.
5.  **Jenkins:** A Jenkins pipeline (`Jenkinsfile`) automates the entire process:
    * Checks out the source code from this GitHub repository.
    * Builds the Spring Boot application using Maven, producing a JAR file.
    * Builds a Docker image from the JAR file.
    * Logs into Docker Hub using stored credentials.
    * Pushes the Docker image to Docker Hub, tagging it with `:latest` and the Jenkins build number.
    * Deploys the application to the Kubernetes cluster by applying the `deployment.yaml` and `service.yaml` files.
6.  **Webhook (GitHub & ngrok):** The Jenkins pipeline is automatically triggered by `git push` events to the `master` branch of this repository, facilitated by an ngrok tunnel for local Jenkins instances.

## Technologies Used

* **Java 17**
* **Spring Boot 3.x** (with Spring Web)
* **Maven** (for building the Spring Boot application)
* **Docker** (for containerization)
* **Docker Hub** (as a container registry)
* **Kubernetes** (orchestration, running via Docker Desktop)
* **Jenkins** (CI/CD automation)
* **ngrok** (for exposing local Jenkins to GitHub webhooks)
* **Git & GitHub** (version control and SCM)

## Project Structure

```
.
├── .mvn/                          # Maven wrapper files
├── src/
│   ├── main/
│   │   ├── java/com/example/jenkkuber/  # Spring Boot application source code
│   │   │   └── JenkkuberApplication.java
│   │   │   └── HelloController.java
│   │   └── resources/
│   │       └── application.properties   # Spring Boot configuration (e.g., server.port=8061)
│   └── test/
│       └── java/com/example/jenkkuber/
│           └── JenkkuberApplicationTests.java
├── Dockerfile                     # Defines how to build the Docker image
├── Jenkinsfile                    # Defines the Jenkins CI/CD pipeline
├── deployment.yaml                # Kubernetes Deployment configuration
├── mvnw                           # Maven wrapper script (Linux/macOS)
├── mvnw.cmd                       # Maven wrapper script (Windows)
├── pom.xml                        # Maven project configuration
└── README.md                      # This file
└── service.yaml                   # Kubernetes Service configuration
```

## Prerequisites

Before you can run this project and pipeline, ensure you have the following installed and configured:

1.  **Git:** For cloning the repository.
2.  **Java Development Kit (JDK):** Version 17 or compatible.
3.  **Maven:** For building the Spring Boot application (or use the included Maven wrapper).
4.  **Docker Desktop:** Installed and running, with Kubernetes enabled in its settings.
5.  **Jenkins:**
    * Installed and running (locally or on a server).
    * Necessary plugins installed: `Pipeline`, `Git plugin`, `Docker Pipeline`, `Kubernetes CLI plugin` (or ensure `kubectl` is in Jenkins agent's PATH).
    * **Docker Hub Credentials:** Configured in Jenkins (e.g., with ID `61611616` or your chosen ID) for pushing images.
    * **Kubernetes `kubeconfig` Credential:** Configured in Jenkins as a "Secret file" (e.g., with ID `kubeconfig-dockerdesktop`) to allow Jenkins to deploy to your Kubernetes cluster.
6.  **ngrok (Optional but Recommended for Local Jenkins):** If your Jenkins instance is running locally and you want to use GitHub webhooks for automatic triggering.

## Setup and Usage

### 1. Clone the Repository

```bash
git clone https://github.com/talhabektas/kubernetesJenkins.git
cd kubernetesJenkins
```

### 2. Configure Jenkins

**Create Docker Hub Credential:**

- Go to Jenkins -> Manage Jenkins -> Credentials -> System -> Global credentials.
- Add a new "Username with password" credential.
- Username: Your Docker Hub username (e.g., mehmettalha).
- Password: Your Docker Hub Access Token (recommended) or password.
- ID: A unique ID (e.g., 61611616).
- Update the DOCKERHUB_CREDENTIALS_ID in the Jenkinsfile with this ID.

**Create Kubernetes kubeconfig Credential:**

- Go to Jenkins -> Manage Jenkins -> Credentials -> System -> Global credentials.
- Add a new "Secret file" credential.
- Upload your kubeconfig file (usually found at ~/.kube/config or C:\Users\YourUser\.kube\config for Docker Desktop Kubernetes).
- ID: A unique ID (e.g., kubeconfig-dockerdesktop).
- Update the credentialsId for the kubeconfig file in the Deploy to Kubernetes stage of your Jenkinsfile if you used a different ID.

**Create a New Pipeline Job in Jenkins:**

- Click on "New Item".
- Enter a name (e.g., kuberjet-pipeline).
- Select "Pipeline" and click "OK".
- In the configuration, under the "Pipeline" section:
  - Definition: Select "Pipeline script from SCM".
  - SCM: Select "Git".
  - Repository URL: https://github.com/talhabektas/kubernetesJenkins.git
  - Branch Specifier: */master (or your main branch).
  - Script Path: Jenkinsfile (this is the default).
- Save the pipeline job.

### 3. (Optional) Configure GitHub Webhook for Automatic Triggering

**Enable GitHub Hook Trigger in Jenkins:**

- In your Jenkins pipeline job configuration, go to "Build Triggers".
- Check "GitHub hook trigger for GITScm polling".
- Save.

**Set up ngrok (if Jenkins is local):**

- Download and run ngrok: `ngrok http 8080` (if your Jenkins is on port 8080).
- Note the public URL ngrok provides (e.g., https://your-unique-id.ngrok-free.app).

**Add Webhook in GitHub:**

- Go to your GitHub repository -> Settings -> Webhooks -> Add webhook.
- Payload URL: Your Jenkins URL + /github-webhook/ (e.g., https://your-unique-id.ngrok-free.app/github-webhook/).
- Content type: application/json.
- Which events?: "Just the push event."
- Ensure "Active" is checked.
- Add webhook.

### 4. Run the Pipeline

**Manually:** In Jenkins, open your pipeline job and click "Build Now".

**Automatically:** If you configured the webhook, push a change to your GitHub repository's master branch.

```bash
git commit -am "Test auto trigger"
git push origin master
```

The pipeline will execute all stages defined in the Jenkinsfile.

### 5. Accessing the Application

Once the pipeline completes successfully:

**Check Pod Status:**

```bash
kubectl get pods
```

You should see your kuberjet-deployment-... pod(s) in Running state and 1/1 READY.

**Get Service Information:**

```bash
kubectl get service kuberjet-service
```

Note the NodePort assigned (e.g., 80:NODE_PORT/TCP).

**Access in Browser:**
Open your browser and go to: http://localhost:NODE_PORT/hello (replace NODE_PORT with the actual port number). You should see the "Hello World" message from the Spring Boot application.

### 6. Scaling the Application

To scale your application to 2 replicas (as per project requirements):

```bash
kubectl scale deployment kuberjet-deployment --replicas=2
```

Verify with `kubectl get pods` that two pods are now running. The application should still be accessible via the same NodePort.

## Troubleshooting

**ImagePullBackOff:**
- Ensure the image name and tag in deployment.yaml (currently mehmettalha/kuberjet:latest) match an image that has been successfully pushed to Docker Hub by the Jenkins pipeline.
- The current Jenkinsfile tags and pushes both ${BUILD_NUMBER} and :latest.

**Jenkins kubectl Authentication Errors:**
- Verify the kubeconfig-dockerdesktop secret file credential in Jenkins is correctly set up with your active kubeconfig file.
- Ensure Docker Desktop and its Kubernetes cluster are running.

**Webhook Not Triggering:**
- Check the "Recent Deliveries" for your webhook in GitHub settings for errors.
- Ensure your ngrok tunnel (if used) is active and the Payload URL is correct.
- Verify "GitHub hook trigger for GITScm polling" is enabled in Jenkins job configuration.

## Conclusion

This project provides a foundational CI/CD pipeline, demonstrating key DevOps practices. It can be extended further with automated testing, different deployment strategies, monitoring, and more.
