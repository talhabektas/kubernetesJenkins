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

        stage('Login to Docker Hub') {
                            steps {
                                withCredentials([usernamePassword(credentialsId: env.DOCKERHUB_CREDENTIALS_ID, usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
                                    script {
                                        echo "Attempting Docker login for user: ${DOCKER_USER}"
                                        // DOCKER_PASS değişkeninin içeriğini (token) maskeleyerek loglayalım (ilk birkaç karakteri)
                                        if (env.DOCKER_PASS != null && env.DOCKER_PASS.length() > 5) {
                                            echo "DOCKER_PASS (token) starts with: ${DOCKER_PASS.substring(0,5)}*****"
                                        } else {
                                            echo "DOCKER_PASS (token) is short or null."
                                        }

                                        // Önceki bat komutunu yorum satırı yapalım:
                                        // bat "echo ${DOCKER_PASS} | docker login -u ${DOCKER_USER} --password-stdin"

                                        // Yeni deneme: Şifreyi (token'ı) doğrudan -p parametresiyle vermek
                                        // Çift tırnaklara dikkat et, özellikle Windows'ta önemli olabilir.
                                        // DOCKER_USER ve DOCKER_PASS değişkenlerinin doğru geldiğinden emin olmak için de logladık.
                                        bat "docker login -u \"${DOCKER_USER}\" -p \"${DOCKER_PASS}\""

                                        echo "Docker Hub login command executed." // Bu satır, komutun çalıştırıldığını teyit eder.
                                    }
                                }
                            }
                            post {
                                success {
                                    echo "Docker Hub'a başarıyla giriş yapıldı."
                                }
                                failure {
                                    echo "Docker Hub'a giriş BAŞARISIZ OLDU."
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