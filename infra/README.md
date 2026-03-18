## 🚀 Jenkins & ngrok CI/CD 가이드
이 문서는 Docker를 활용하여 젠킨스와 ngrok 터널을 구축하고, GitLab Webhook을 통해 자동 빌드 환경을 설정하는 방법을 설명합니다.

### 1. 로컬(Local) 환경 실행 방법
- 사전 준비
    - docker-compose.yml이 있는 위치에 .env 파일을 생성하고 본인의 ngrok 토큰을 입력합니다.

    ```
    NGROK_AUTHTOKEN=여러분의_ngrok_토큰
    ```

- 실행 순서
    1. 컨테이너 실행: `docker-compose up -d`
    2. 외부 접속 주소(URL) 확인: http://localhost:4040에 접속하여 Forwarding 주소(https://xxxx.ngrok-free.app)를 복사
    3. GitLab Webhook 등록: 등록되있는 Webhooks에 주소 변경

### 2. EC2 서버 이전 시 작업 가이드
EC2로 환경을 옮긴 후에는 서버 환경(IP/인증)이 변하므로 아래 4가지 작업을 반드시 수행해야 합니다.

- ① 파일 클론 및 환경 변수 설정
    - EC2 서버에 접속하여 프로젝트를 git clone 받습니다.
    - 중요: 깃랩에 올리지 않은 .env 파일을 EC2 서버에서 다시 생성하여 토큰을 넣습니다.
- ② Jenkins 초기 세팅 (Credentials)
    - 서버를 새로 띄우면 내부 금고가 비어있으므로 자격 증명을 다시 등록해야 합니다.
    - gitlab_auth: GitLab 계정 ID / Password(또는 Access Token)
    - mattermost-webhook-url: Mattermost 웹훅 전체 주소 (Secret text 타입)
- ③ Jenkins 아이템 생성 및 토큰 발급
    - 새로운 Pipeline 아이템을 생성합니다.
    - Pipeline script from SCM을 선택하고 GitLab Repository 주소를 입력합니다.
    - Build Triggers에서 Secret Token을 새로 Generate 합니다. (이때 발급된 토큰이 로컬과 다르므로 주의!)
- ④ GitLab Webhook URL 업데이트
    - 주소 변경:
        - 만약 EC2에서도 ngrok을 쓴다면: 새로운 ngrok 주소로 변경
        - 만약 EC2 공인 IP를 직접 쓴다면: http://[EC2-공인-IP]:9090/project/[아이템이름]