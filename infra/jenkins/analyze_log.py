post {
    failure {
        script {
            // 1. 로그 추출 (현재 워크스페이스에 생성)
            def logFile = "${env.WORKSPACE}/extracted_log.txt"
            sh "tail -n 100 /var/jenkins_home/jobs/${env.JOB_NAME}/builds/${env.BUILD_NUMBER}/log > ${logFile}"

            // 2. 파이썬 실행 (출력값을 직접 받지 않고 파일로 저장하는 방식이 더 안전함)
            def resultFile = "${env.WORKSPACE}/analysis_result.json"
            sh "python3 infra/jenkins/analyze_log.py ${logFile} > ${resultFile}"
            
            // 3. 파일 읽기 및 파싱
            def resultText = readFile(file: resultFile).trim()
            if (resultText) {
                def result = new groovy.json.JsonSlurper().parseText(resultText)
                
                // 4. Mattermost 전송
                def payload = """
                {
                    "text": "### 🚨 [${result.team}] 빌드 실패 발생\\n**Job:** ${env.JOB_NAME}\\n**에러 요약:**\\n```\\n${result.summary}\\n```\\n[로그 보기](${env.BUILD_URL}console)"
                }
                """
                sh "curl -sS -X POST -H 'Content-Type: application/json' -d '${payload}' ${MM_WEBHOOK}"
            } else {
                echo "분석 결과가 비어있습니다."
            }
        }
    }
}