pipeline {
    agent any // Pipeline'ın herhangi bir uygun Jenkins agent'ında çalışmasını sağlar

    environment {
        // Jenkins'te oluşturduğun Docker Hub kimlik bilgisinin ID'si
        DOCKERHUB_CREDENTIALS_ID = '61611616' // SENİN BELİRLEDİĞİN ID

        // Docker imajının tam adı (Docker Hub kullanıcı adın / imaj adın)
        DOCKER_IMAGE_NAME      = "mehmettalha/kuberjet" // SENİN DOCKER IMAJ ADIN

        // Dinamik olarak oluşturulacak imaj etiketi ve tam imaj adı
        // Bu değişkenler script içinde tanımlanacak ve kullanılacak
    }

    stages {
        stage('Git Checkout') { // 1. Aşama: Projeyi GitHub'dan çek
            steps {
                // GitHub depon ve ana branch'in (master olarak güncellendi)
                git url: 'https://github.com/talhabektas/kubernetesJenkins.git', branch: 'master'
                script {
                    echo "Proje başarıyla klonlandı."
                }
            }
        }

        stage('Build Project (JAR)') { // 2. Aşama: Maven ile projeyi derle ve JAR oluştur
            steps {
                // Windows'ta bat ve mvnw.cmd kullanıyoruz
                bat 'java -version' // Java sürümünü yazdırır
                bat 'echo %JAVA_HOME%' // JAVA_HOME ortam değişkenini yazdırır

                bat 'mvnw.cmd clean package -DskipTests'
                script {
                    echo "Proje başarıyla derlendi ve JAR dosyası oluşturuldu."
                }
            }
        }

        stage('Build Docker Image') { // 3. Aşama: Docker imajını oluştur
            steps {
                script {
                    def imageTag = env.BUILD_NUMBER ?: "latest"
                    env.IMAGE_FULL_NAME_WITH_TAG = "${env.DOCKER_IMAGE_NAME}:${imageTag}"

                    // Windows için bat komutu
                    bat "docker build -t ${env.IMAGE_FULL_NAME_WITH_TAG} ."
                    echo "Docker imajı başarıyla oluşturuldu: ${env.IMAGE_FULL_NAME_WITH_TAG}"
                }
            }
        }

        stage('Login to Docker Hub') { // 4. Aşama: Docker Hub'a giriş yap
            steps {
                withCredentials([usernamePassword(credentialsId: env.DOCKERHUB_CREDENTIALS_ID, usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
                    // Windows için bat komutu. Değişkenlerin doğru kullanıldığından emin ol.
                    // Çift tırnaklar Groovy'de değişken interpolasyonu için önemlidir.
                    bat "echo ${DOCKER_PASS} | docker login -u ${DOCKER_USER} --password-stdin"
                    echo "Docker Hub'a başarıyla giriş yapıldı."
                }
            }
        }

        stage('Push Docker Image') { // 5. Aşama: Docker imajını Docker Hub'a gönder
            steps {
                script {
                    // Windows için bat komutu
                    bat "docker push ${env.IMAGE_FULL_NAME_WITH_TAG}"
                    echo "Docker imajı başarıyla Docker Hub'a gönderildi: ${env.IMAGE_FULL_NAME_WITH_TAG}"
                }
            }
        }

        stage('Deploy to Kubernetes') { // 6. ve 7. Aşamalar: Kubernetes'e deploy et
            steps {
                // Windows için bat komutları
                bat 'kubectl apply -f deployment.yaml'
                bat 'kubectl apply -f service.yaml'
                script {
                    echo "Kubernetes'e başarıyla deploy edildi."
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