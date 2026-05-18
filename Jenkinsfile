pipeline {
    agent any

    environment {
        PATH                   = "/usr/bin:/bin:/usr/sbin:/sbin:/usr/local/bin:/opt/homebrew/bin"
        
        DOCKER_HUB_CREDENTIALS = 'teedy2026' 
        DOCKER_IMAGE           = 'manfred622/teedy' 
        DOCKER_TAG             = "${env.BUILD_NUMBER}" 
    }

    stages {
        // 1. 代码拉取与 Maven 编译打包
        stage('Build') {
            steps {
                checkout scmGit(
                    branches: [[name: '*/master']],
                    extensions: [],
                    userRemoteConfigs: [[url: 'https://github.com/ManfredShao/Teedy.git']]
                )
                sh 'mvn -B -DskipTests clean package'
            }
        }

        // 2. 构建 Docker 镜像
        stage('Building image') {
            steps {
                sh 'docker build -t ${DOCKER_IMAGE}:${DOCKER_TAG} .'
            }
        }

        // 3. 推送 Docker 镜像到 Docker Hub
        stage('Upload image') {
            steps {
                // 【核心修改】绕过 docker.withRegistry 插件，改用标准的 withCredentials 提取账号密码
                withCredentials([usernamePassword(credentialsId: "${env.DOCKER_HUB_CREDENTIALS}", usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
                    sh '''
                        # 使用管道方式安全登录 Docker Hub，避免密码暴露在历史记录里
                        echo "$DOCKER_PASS" | docker login -u "$DOCKER_USER" --password-stdin
                        
                        # 推送当前构建号的版本
                        docker push ${DOCKER_IMAGE}:${DOCKER_TAG}
                        
                        # 打上 latest 标签并推送
                        docker tag ${DOCKER_IMAGE}:${DOCKER_TAG} ${DOCKER_IMAGE}:latest
                        docker push ${DOCKER_IMAGE}:latest
                        
                        # 登录完成后顺手登出，保持环境干净
                        docker logout
                    '''
                }
            }
        }

        // 4. 在当前节点运行/更新容器
        stage('Run containers') {
            steps {
                // 【核心修改】同样换成纯 sh 脚本运行，完全避开插件限制
                sh '''
                    docker stop teedy-container-8081 || true
                    docker rm teedy-container-8081 || true
                    docker run --name teedy-container-8081 -d -p 8081:8080 ${DOCKER_IMAGE}:${DOCKER_TAG}
                    docker ps --filter "name=teedy-container"
                '''
            }
        }
    }

    post {
        always {
            // 构建完成后自动清理无标签的临时镜像，省下 Mac 的磁盘空间
            sh 'docker image prune -f || true'
        }
    }
}