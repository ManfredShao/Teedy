pipeline {
    agent any

    environment {
        // Jenkins 凭据配置：Docker Hub 的凭据 ID
        DOCKER_HUB_CREDENTIALS = 'teedy2026' 
        
        // Docker Hub 仓库名称：格式为 '用户名/仓库名'
        DOCKER_IMAGE           = 'manfred622/teedy' 
        
        // 镜像标签：使用 Jenkins 当前构建号作为 Tag
        DOCKER_TAG             = "${env.BUILD_NUMBER}" 
    }

    stages {
        // 1. 代码拉取与 Maven 编译打包
        stage('Build') {
            steps {
                // 从源码仓库拉取 master 分支
                checkout scmGit(
                    branches: [[name: '*/master']],
                    extensions: [],
                    userRemoteConfigs: [[url: 'https://github.com/ManfredShao/Teedy.git']]
                )
                // 执行 Maven 编译打包，跳过测试
                sh '/opt/homebrew/bin/mvn -B -DskipTests clean package'
            }
        }

        // 2. 构建 Docker 镜像
        stage('Building image') {
            steps {
                script {
                    // 默认 Dockerfile 位于项目根目录
                    docker.build("${env.DOCKER_IMAGE}:${env.DOCKER_TAG}")
                }
            }
        }

        // 3. 推送 Docker 镜像到 Docker Hub
        stage('Upload image') {
            steps {
                script {
                    // 登录 Docker Hub 注册表
                    docker.withRegistry('https://registry.hub.docker.com', DOCKER_HUB_CREDENTIALS) {
                        // 推送带构建号标签的镜像
                        docker.image("${env.DOCKER_IMAGE}:${env.DOCKER_TAG}").push()
                        // 顺便推送一个名为 latest 的标签（可选）
                        docker.image("${env.DOCKER_IMAGE}:${env.DOCKER_TAG}").push('latest')
                    }
                }
            }
        }

        // 4. 在当前节点运行/更新容器
        stage('Run containers') {
            steps {
                script {
                    // 如果已有同名容器在运行，先停止并删除
                    sh 'docker stop teedy-container-8081 || true'
                    sh 'docker rm teedy-container-8081 || true'
                    
                    // 运行新容器，将容器内的 8080 端口映射到宿主机的 8081 端口
                    docker.image("${env.DOCKER_IMAGE}:${env.DOCKER_TAG}").run('--name teedy-container-8081 -d -p 8081:8080')
                    
                    // 打印当前含有 teedy-container 关键字的容器列表（可选）
                    sh 'docker ps --filter "name=teedy-container"'
                }
            }
        }
    }
}