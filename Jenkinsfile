pipeline {
    agent any // Pipeline'ın herhangi bir uygun Jenkins agent'ında çalışmasını sağlar

    environment {
        // Jenkins'te oluşturduğun Docker Hub kimlik bilgisinin ID'si
        DOCKERHUB_CREDENTIALS_ID = '61611616' // SENİN BELİRLEDİĞİN ID

        // Docker imajının tam adı (Docker Hub kullanıcı adın / imaj adın)
        DOCKER_IMAGE_NAME      = "mehmettalha/kuberjet" // SENİN DOCKER IMAJ ADIN

        // Dinamik olarak oluşturulacak imaj etiketi (build numarası)
        // Bu değişken script içinde tanımlanacak ve kullanılacak
    }

    stages {
        stage('Git Checkout') { // 1. Aşama: Projeyi GitHub'dan çek
            steps {
                // GitHub depon ve ana branch'in (eğer farklıysa değiştir, örn: 'master')
                git url: 'https://github.com/talhabektas/kubernetesJenkins.git', branch: 'master'
                script {
                    echo "Proje başarıyla klonlandı."
                }
            }
        }

        stage('Build Project (JAR)') { // 2. Aşama: Maven ile projeyi derle ve JAR oluştur
            steps {
                // Windows'ta bat ve mvnw.cmd kullanıyoruz
                bat './mvnw.cmd clean package -DskipTests'
                script {
                    echo "Proje başarıyla derlendi ve JAR dosyası oluşturuldu."
                }
            }
        }

        stage('Build Docker Image') { // 3. Aşama: Docker imajını oluştur
            steps {
                script {
                    // Jenkins'in BUILD_NUMBER'ını etiket olarak kullanalım.
                    // Eğer BUILD_NUMBER yoksa (örneğin lokal testlerde veya ilk çalıştırmada), 'latest' kullan.
                    def imageTag = env.BUILD_NUMBER ?: "latest"
                    env.IMAGE_FULL_NAME_WITH_TAG = "${env.DOCKER_IMAGE_NAME}:${imageTag}" // Tam imaj adını bir sonraki aşamalar için sakla

                    // Docker build komutu. '.' Dockerfile'ın mevcut dizinde olduğunu belirtir.
                    sh "docker build -t ${env.IMAGE_FULL_NAME_WITH_TAG} ."
                    echo "Docker imajı başarıyla oluşturuldu: ${env.IMAGE_FULL_NAME_WITH_TAG}"
                }
            }
        }

        stage('Login to Docker Hub') { // 4. Aşama: Docker Hub'a giriş yap
            steps {
                // 'DOCKERHUB_CREDENTIALS_ID' ile Jenkins'te kaydettiğin kimlik bilgilerini kullan.
                // usernameVariable ve passwordVariable, Docker Hub kullanıcı adı ve şifresini tutacak geçici değişkenlerdir.
                withCredentials([usernamePassword(credentialsId: env.DOCKERHUB_CREDENTIALS_ID, usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
                    sh "echo ${DOCKER_PASS} | docker login -u ${DOCKER_USER} --password-stdin"
                    echo "Docker Hub'a başarıyla giriş yapıldı."
                }
            }
        }

        stage('Push Docker Image') { // 5. Aşama: Docker imajını Docker Hub'a gönder
            steps {
                script {
                    // Bir önceki aşamada tanımladığımız tam imaj adını kullanarak push et.
                    sh "docker push ${env.IMAGE_FULL_NAME_WITH_TAG}"
                    echo "Docker imajı başarıyla Docker Hub'a gönderildi: ${env.IMAGE_FULL_NAME_WITH_TAG}"
                }
            }
        }

        stage('Deploy to Kubernetes') { // 6. ve 7. Aşamalar: Kubernetes'e deploy et
            steps {
                // Bu komutların çalışması için Jenkins'in çalıştığı ortamda kubectl'in kurulu
                // ve Minikube'e erişecek şekilde yapılandırılmış olması gerekir.
                // Jenkins ve Minikube aynı makinede çalışıyorsa ve kubectl PATH'de ise genellikle sorun olmaz.
                sh 'kubectl apply -f deployment.yaml'
                sh 'kubectl apply -f service.yaml'
                script {
                    echo "Kubernetes'e başarıyla deploy edildi."
                    // Opsiyonel: Minikube'de servisin URL'sini almak ve loglamak için.
                    // Bu komut bazen etkileşimli olabilir veya farklı bir çıktı verebilir, duruma göre ayarlanmalı.
                    // def serviceURL = sh(script: "minikube service kuberjet-service --url", returnStdout: true).trim()
                    // echo "Uygulama URL'si (Minikube): ${serviceURL}"
                }
            }
        }
    }

    post { // Pipeline bittikten sonra her zaman çalışacak bölüm
        always {
            echo 'Pipeline tamamlandı.'
            // cleanWs() // Jenkins çalışma alanını temizlemek için (isteğe bağlı, yorumu kaldırılabilir)
        }
        success {
            echo 'Pipeline BAŞARIYLA tamamlandı!'
        }
        failure {
            echo 'Pipeline BAŞARISIZ oldu. Lütfen Jenkins loglarını kontrol et.'
        }
    }
}