pipeline {
    agent any // Pipeline'ın herhangi bir uygun Jenkins agent'ında çalışmasını sağlar

    environment {
        DOCKERHUB_CREDENTIALS_ID = '61611616' // Docker Hub kimlik bilgisinin ID’si
        DOCKER_IMAGE_NAME = "mehmettalha/kuberjet" // Docker imaj adı (kullanıcı_adı/imaj_adı)
    }

    stages {
        stage('Git Checkout') { // 1. Aşama: GitHub’dan kodu al
            steps {
                git url: 'https://github.com/talhabektas/kubernetesJenkins.git', branch: 'master'
                script {
                    echo "Proje başarıyla klonlandı."
                }
            }
        }

        stage('Build Project (JAR)') { // 2. Aşama: Maven ile JAR dosyası oluştur
            steps {
                bat 'java -version'
                bat 'echo %JAVA_HOME%'
                bat 'mvnw.cmd clean package -DskipTests'
                script {
                    echo "Proje başarıyla derlendi ve JAR dosyası oluşturuldu."
                }
            }
        }

       stage('Build Docker Image') {
           steps {
               script {
                   def buildTag = env.BUILD_NUMBER ?: "dev" // Eğer build no yoksa 'dev' gibi bir tag kullan
                   env.IMAGE_WITH_BUILD_TAG = "${env.DOCKER_IMAGE_NAME}:${buildTag}"
                   env.IMAGE_WITH_LATEST_TAG = "${env.DOCKER_IMAGE_NAME}:latest"

                   // Önce build numarasıyla build et
                   bat "docker build -t ${env.IMAGE_WITH_BUILD_TAG} ."
                   echo "Docker imajı başarıyla oluşturuldu: ${env.IMAGE_WITH_BUILD_TAG}"

                   // Aynı imajı :latest olarak da etiketle
                   bat "docker tag ${env.IMAGE_WITH_BUILD_TAG} ${env.IMAGE_WITH_LATEST_TAG}"
                   echo "İmaj ayrıca etiketlendi: ${env.IMAGE_WITH_LATEST_TAG}"
               }
           }
       }

        stage('Login to Docker Hub') {
            steps {
                withCredentials([usernamePassword(credentialsId: env.DOCKERHUB_CREDENTIALS_ID, usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
                    script {
                        echo "Docker kullanıcısı: ${DOCKER_USER}"
                        if (env.DOCKER_PASS != null && env.DOCKER_PASS.length() > 5) {
                            echo "DOCKER_PASS ilk karakterleri: ${DOCKER_PASS.substring(0,5)}*****"
                        } else {
                            echo "DOCKER_PASS değeri geçersiz veya çok kısa."
                        }
                        bat "docker login -u \"${DOCKER_USER}\" -p \"${DOCKER_PASS}\""
                        echo "Docker Hub login komutu çalıştırıldı."
                    }
                }
            }
            post {
                success {
                    echo "Docker Hub’a başarıyla giriş yapıldı."
                }
                failure {
                    echo "Docker Hub’a giriş BAŞARISIZ OLDU."
                }
            }
        }

       stage('Push Docker Image') {
           steps {
               script {
                   // Önce build numaralı imajı push'la (opsiyonel ama iyi bir pratik)
                   bat "docker push ${env.IMAGE_WITH_BUILD_TAG}"
                   echo "Docker imajı başarıyla Docker Hub'a gönderildi: ${env.IMAGE_WITH_BUILD_TAG}"

                   // Sonra :latest etiketli imajı push'la
                   bat "docker push ${env.IMAGE_WITH_LATEST_TAG}"
                   echo "Docker imajı (:latest) başarıyla Docker Hub'a gönderildi: ${env.IMAGE_WITH_LATEST_TAG}"
               }
           }
       }

     stage('Deploy to Kubernetes') {
         steps {
             withCredentials([file(credentialsId: 'kubeconfig-dockerdesktop', variable: 'KUBECONFIG_FILE')]) {
                 script {
                     echo "Deploying application to Kubernetes using KUBECONFIG from credentials..."
                     echo "KUBECONFIG file path: ${KUBECONFIG_FILE}"
                     withEnv(["KUBECONFIG=${KUBECONFIG_FILE}"]) {
                         bat "kubectl config view"
                         bat "kubectl get nodes"
                         bat "kubectl apply -f deployment.yaml"
                         bat "kubectl apply -f service.yaml" // <-- BU SATIRI EKLEDİK!
                     }
                     echo "Application deployment and service commands executed."
                 }
             }
         }
         post {
             success {
                 echo "Uygulama ve servis Kubernetes'e başarıyla dağıtıldı." // Mesajı güncelledik
             }
             failure {
                 echo "Uygulama veya servis Kubernetes'e DAĞITILAMADI." // Mesajı güncelledik
                 withCredentials([file(credentialsId: 'kubeconfig-dockerdesktop', variable: 'KUBECONFIG_FILE_POST')]) {
                     withEnv(["KUBECONFIG=${KUBECONFIG_FILE_POST}"]) {
                         bat "kubectl get all --all-namespaces"
                         bat "kubectl describe pods"
                     }
                 }
             }
         }
     }
    }

    post {
        always {
            echo 'Pipeline tamamlandı.'
        }
        success {
            echo 'Pipeline BAŞARIYLA tamamlandı!'
        }
        failure {
            echo 'Pipeline BAŞARISIZ oldu. Jenkins loglarını kontrol edin.'
        }
    }
}
