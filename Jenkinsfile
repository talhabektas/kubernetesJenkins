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

        stage('Deploy to Kubernetes') {
            steps {
                // KUBECONFIG_FILE adında bir değişkene, kimlik bilgisi olarak yüklediğimiz
                // dosyanın Jenkins workspace'indeki geçici yolunu atıyoruz.
                withCredentials([file(credentialsId: 'kubeconfig-dockerdesktop', variable: 'KUBECONFIG_FILE')]) {
                    script {
                        echo "Deploying application to Kubernetes using KUBECONFIG from credentials..."
                        echo "KUBECONFIG file path: ${KUBECONFIG_FILE}" // Dosya yolunu loglayalım

                        // Yöntem A: KUBECONFIG ortam değişkenini withEnv bloğu içinde ayarlamak
                        // Bu, kubectl'in otomatik olarak doğru config dosyasını kullanmasını sağlar.
                        withEnv(["KUBECONFIG=${KUBECONFIG_FILE}"]) {
                            bat "kubectl config view" // Hangi config'i gördüğünü kontrol etmek için
                            bat "kubectl get nodes"   // Bağlantıyı ve yetkiyi tekrar test etmek için
                            bat "kubectl apply -f deployment.yaml"
                        }

                        // Alternatif Yöntem B: --kubeconfig parametresini her kubectl komutuna eklemek
                        // Eğer yukarıdaki withEnv sorun çıkarırsa bu denenebilir.
                        // bat "kubectl --kubeconfig=\"${KUBECONFIG_FILE}\" config view"
                        // bat "kubectl --kubeconfig=\"${KUBECONFIG_FILE}\" get nodes"
                        // bat "kubectl --kubeconfig=\"${KUBECONFIG_FILE}\" apply -f deployment.yaml"

                        echo "Application deployment command executed."
                    }
                }
            }
            post {
                success {
                    echo "Uygulama Kubernetes'e başarıyla dağıtıldı."
                }
                failure {
                    echo "Uygulama Kubernetes'e DAĞITILAMADI."
                    // Hata durumunda daha fazla bilgi almak için buraya da withCredentials ekleyebiliriz
                    withCredentials([file(credentialsId: 'kubeconfig-dockerdesktop', variable: 'KUBECONFIG_FILE_POST')]) {
                        withEnv(["KUBECONFIG=${KUBECONFIG_FILE_POST}"]) {
                            bat "kubectl get all --all-namespaces"
                            bat "kubectl describe pods" // Hata veren podlar hakkında detaylı bilgi
                        }
                    }
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