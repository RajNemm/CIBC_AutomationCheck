pipeline {
    agent any

    parameters {
        choice(name: 'TEST_SUITE', choices: ['smoke', 'regression', 'secupi', 'all'],
            description: 'Select a test suite; all runs smoke, regression, then SecuPI')
    }

    options {
        timestamps()
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Smoke Tests') {
            when {
                expression { params.TEST_SUITE in ['smoke', 'all'] }
            }
            steps {
                script {
                    if (isUnix()) {
                        sh 'mvn -B -Dheadless=true -Dtestng.suite=src/test/resources/testng-smoke.xml clean test'
                    } else {
                        bat 'mvn -B -Dheadless=true -Dtestng.suite=src/test/resources/testng-smoke.xml clean test'
                    }
                }
            }
        }

        stage('Regression Tests') {
            when {
                expression { params.TEST_SUITE in ['regression', 'all'] }
            }
            steps {
                script {
                    if (isUnix()) {
                        sh 'mvn -B -Dheadless=true -Dtestng.suite=src/test/resources/testng-regression.xml test'
                    } else {
                        bat 'mvn -B -Dheadless=true -Dtestng.suite=src/test/resources/testng-regression.xml test'
                    }
                }
            }
        }

        stage('SecuPI Oracle Tests') {
            when {
                expression { params.TEST_SUITE in ['secupi', 'all'] }
            }
            steps {
                withCredentials([
                    file(credentialsId: 'secupi-agent-jar', variable: 'SECUPI_AGENT_JAR'),
                    file(credentialsId: 'secupi-test-config', variable: 'SECUPI_CONFIG_FILE'),
                    usernamePassword(credentialsId: 'oracle-regular-user',
                        usernameVariable: 'SECUPI_DATABASE_REGULAR_USERNAME',
                        passwordVariable: 'SECUPI_DATABASE_REGULAR_PASSWORD'),
                    usernamePassword(credentialsId: 'oracle-privileged-user',
                        usernameVariable: 'SECUPI_DATABASE_PRIVILEGED_USERNAME',
                        passwordVariable: 'SECUPI_DATABASE_PRIVILEGED_PASSWORD')
                ]) {
                    script {
                        if (isUnix()) {
                            sh 'mvn -B -Psecupi -Dsecupi.agent.jar="$SECUPI_AGENT_JAR" -Dsecupi.config.file="$SECUPI_CONFIG_FILE" test'
                        } else {
                            bat 'mvn -B -Psecupi -Dsecupi.agent.jar="%SECUPI_AGENT_JAR%" -Dsecupi.config.file="%SECUPI_CONFIG_FILE%" test'
                        }
                    }
                }
            }
        }
    }

    post {
        always {
            junit testResults: '**/target/surefire-reports/TEST-*.xml', allowEmptyResults: true
            archiveArtifacts artifacts: '**/target/surefire-reports/**', allowEmptyArchive: true
        }
    }
}
