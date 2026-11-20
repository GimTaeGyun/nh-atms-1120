// =====================================================================
// 백엔드 WAR 빌드·배포 — Jenkins(ws01 컨테이너) : bootWar -Pjeus → WAR 6개 + sha256 보관 → (선택) ws01 배포 폴더 교체
//
//  잡 만들기: 새 Item → Pipeline, 이름 nh-atms-backend (폴더 없이 최상위)
//            → Pipeline 칸 "Pipeline script" 에 이 파일 내용을 붙여 넣는다
//            → 처음 한 번 "Build with Parameters" 로 실행해야 매개변수·트리거가 등록된다
//  자동 빌드: 프론트 잡과 같은 Gitea 웹훅 1개(notifyCommit, url=GITEA_REPO)를 함께 쓴다.
//            소스 받기에 PathRestriction(Backend/.*) 을 걸어, Backend/ 아래가 바뀐 push 에만 빌드된다
//  필요한 Credentials (Jenkins 관리 → Credentials → System → Global)
//    gitea-jenkins-bot : Username with password  (jenkins-bot / Gitea 토큰)        소스 받기
//    ws01-deploy-key   : SSH Username with private key (atmswas / 배포 전용 키)  ws01 WAR 배포 (배포 MODE 에만 필요)
//  캐시: offline-seed 의 Gradle 캐시가 /cache/gradle 에 마운트돼 있고 GRADLE_USER_HOME=/cache/gradle
//        (DEV_SERVER_SETUP.md 9-5). 의존성이 바뀌어 "Could not resolve" 가 나면 캐시 재반입
//  구성 (2026-10-26): JEUS 도메인 1개 + 매니지드 서버 6개 (모듈마다 JVM·포트 하나, 컨텍스트 '/')
//        atms-web 8080 · journal 8081 · batch 8082 · file 8083 · if-internal 8084 · if-external 8085 (16.88.122.113)
//  배포: Jenkins 컨테이너 → ws01 호스트 SSH (WS01_SSH). ws01 의 authorized_keys 에서 이 키는
//        command="/home/atmswas/atms/deploy/jeus/deploy-war.sh" 로 고정 → deploy/rollback/status/restart 만 가능
//        (deploy/jeus/install-deploy-war.sh 로 등록). 교체 뒤 RESTART=true 면 deploy-war.sh restart 가
//        교체한 모듈의 서버만 하나씩 jeusRstart 로 재기동 → 기동 확인 → 실패한 모듈은 자동 롤백·재기동
//        서버의 재기동 메뉴가 ws01 ~/deploy/deploy-war.env 에 없으면 그 모듈은 교체만 하고 UNSTABLE(노란색)
//        → WAS 담당에 재기동 요청 (deploy-war.sh 종료코드 4)
//        결과 색 (2026-11-10): 초록 = 교체 + 재기동 + 기동 확인(로그 Started)까지 성공일 때만.
//          노랑 = 재기동 메뉴 없음(4) 또는 RESTART 꺼서 교체만 함(5) — 서버에 반영 안 됨.
//          빨강 = 빌드·교체·재기동·기동 확인 실패 (기동 실패면 앱 로그·JEUS 서버 로그 끝 40줄이 콘솔에 찍힘)
//          초록이 아니면 콘솔 끝에 ws01 status(모듈별 프로세스·마지막 기동 로그·최근 기록)를 붙인다
//  문서: docs/BACKEND_CICD_PLAN.md
//
//  MODE
//    build-deploy (기본, 웹훅도 이것 — 2026-11-10 사용자 결정: push 하면 자동 빌드+배포)
//                                     : 빌드 → MODULE(all 이면 6개) WAR 를 ws01 배포 폴더(catms·cajou·…)에 교체 → (RESTART) 재기동·확인
//    build-only                       : 빌드·보관만 (배포하지 않음)
//    redeploy                         : REDEPLOY_BUILD 번 빌드의 보관 WAR 를 다시 교체 → (RESTART) 재기동·확인
//    rollback                         : MODULE 하나를 ws01 의 직전 백업으로 (all 불가) → (RESTART) 재기동·확인
//    restart                          : 교체 없이 재기동·확인만 (MODULE=all 이면 6개 서버를 하나씩)
//    status                           : ws01 의 모듈별 기동 확인 응답 코드·최근 수신 WAR·백업 목록
// =====================================================================
pipeline {
  agent any

  options {
    timestamps()
    disableConcurrentBuilds()
    timeout(time: 120, unit: 'MINUTES')  // 6개 서버를 하나씩 재기동·기동 확인 (2026-11-10 자동 배포로 60 → 120)
    buildDiscarder(logRotator(numToKeepStr: '30', artifactNumToKeepStr: '10'))
  }

  // 일정 없는 pollSCM = 웹훅(notifyCommit)이 올 때만 새 커밋 확인. Backend/ 밖만 바뀐 push 는 빌드 안 함
  triggers { pollSCM('') }

  parameters {
    choice(name: 'MODE', choices: ['build-deploy', 'build-only', 'redeploy', 'rollback', 'restart', 'status'], description: '기본 build-deploy (웹훅 push 빌드도 빌드+배포+재기동, 2026-11-10). 배포 없이 빌드만: build-only. 설명은 스크립트 맨 위')
    choice(name: 'MODULE', choices: ['auto', 'all', 'atms-web', 'atms-journal', 'atms-batch', 'atms-file', 'atms-if-internal', 'atms-if-external'], description: '기본 auto = 직전 성공 빌드 이후 바뀐 모듈만 (atms-common·루트 빌드 파일이 바뀌면 6개 전부). all = 6개. rollback·restart 는 하나 또는 all')
    booleanParam(name: 'RESTART', defaultValue: true, description: 'build-deploy·redeploy·rollback 뒤 그 모듈의 서버만 재기동 + 기동 확인 (실패 시 자동 롤백)')
    string(name: 'REDEPLOY_BUILD', defaultValue: '', description: 'MODE=redeploy 일 때 다시 배포할 이 잡의 빌드 번호')
  }

  environment {
    GITEA_REPO  = 'http://atms-gitea:3000/NH-ATMS/nh-atms.git'  // 프론트 잡·웹훅 url= 과 글자까지 같아야 함
    SRC_BRANCH  = 'main'
    BACK_DIR    = 'Backend/nh-atms-api'
    WAR_VERSION = '0.0.1'
    WS01_SSH    = 'atmswas@host.containers.internal'  // 컨테이너에서 ws01 호스트 sshd. 안 되면 BACKEND_CICD_PLAN 6-3 대로 바꿈
    GRADLE_USER_HOME = '/cache/gradle'  // offline-seed 캐시 마운트 위치 (jenkins.env 에 없어도 동작하게 고정, 2026-10-23)
    TZ          = 'Asia/Seoul'
  }

  stages {
    // 모든 MODE 에서 소스를 받는다: 비교 기준(마지막 빌드의 커밋)이 끊기지 않게 (프론트 잡과 같은 이유)
    stage('소스') {
      steps {
        script {
          def scmVars = checkout([$class: 'GitSCM',
            branches: [[name: "*/${env.SRC_BRANCH}"]],
            userRemoteConfigs: [[url: env.GITEA_REPO, credentialsId: 'gitea-jenkins-bot']],
            extensions: [[$class: 'PathRestriction', includedRegions: 'Backend/.*', excludedRegions: '']]])
          // 대상 모듈 (2026-11-10): auto = 직전 성공 빌드 커밋 이후 Backend/nh-atms-api 에서 바뀐 모듈 폴더만.
          //   atms-common·루트 build.gradle·settings.gradle·gradle/ 등 모듈 폴더 밖이 바뀌면 6개 전부 (공통을 함께 씀)
          def ALL = 'atms-web atms-journal atms-batch atms-file atms-if-internal atms-if-external'
          def base = scmVars.GIT_PREVIOUS_SUCCESSFUL_COMMIT ?: ''
          if (params.MODULE != 'auto') {
            env.TARGETS = (params.MODULE == 'all') ? ALL : params.MODULE
          } else if (!(params.MODE in ['build-deploy', 'build-only'])) {
            env.TARGETS = ALL
          } else if (!base) {
            env.TARGETS = ALL
          } else {
            def mods = []
            def every = false
            sh(returnStdout: true, script: "git diff --name-only ${base} HEAD -- Backend/nh-atms-api").trim().split('\n').each { f ->
              def p = f.split('/')
              if (!f) { return }
              if (p.size() > 3 && ALL.split(' ').contains(p[2])) { mods << p[2] } else { every = true }
            }
            env.TARGETS = every ? ALL : mods.unique().join(' ')
          }
          echo "대상 모듈: ${env.TARGETS ?: '(없음 — 바뀐 모듈 없음, 빌드·배포 생략)'}  (비교 기준 ${base ?: '없음'})"
        }
      }
    }

    stage('빌드') {
      when { expression { (params.MODE == 'build-only' || params.MODE == 'build-deploy') && env.TARGETS } }
      steps {
        dir(env.BACK_DIR) {
          sh '''
            set -eu
            java -version
            echo "GRADLE_USER_HOME=${GRADLE_USER_HOME:-미설정}"
            # 캐시가 없으면 gradlew 가 인터넷에서 배포판을 받으려다 UnknownHostException 으로 실패 → 원인을 먼저 알려 줌
            ls -d "${GRADLE_USER_HOME}"/wrapper/dists/gradle-8.14.4-bin >/dev/null 2>&1 \
              || { echo "Gradle 캐시 없음: ${GRADLE_USER_HOME}/wrapper/dists/gradle-8.14.4-bin → DEV_SERVER_SETUP 9-5 (컨테이너에 -v .../cache/gradle:/cache/gradle 마운트 확인)"; exit 1; }
            TASK=""; for m in ${TARGETS}; do TASK="${TASK} :${m}:bootWar"; done
            # gradle.properties 의 -Xmx2g·parallel 은 컨테이너(2g)에 과함 → 명령줄 -D 로 덮어씀
            sh ./gradlew ${TASK} -Pjeus --offline --no-daemon --max-workers=1 \
              -Dorg.gradle.parallel=false \
              -Dorg.gradle.jvmargs="-Xmx768m -Dfile.encoding=UTF-8" \
              -Porg.gradle.java.installations.auto-download=false \
              -Porg.gradle.java.installations.paths="${JAVA_HOME:-/opt/java/openjdk}"
          '''
        }
      }
    }

    stage('묶기') {
      when { expression { (params.MODE == 'build-only' || params.MODE == 'build-deploy') && env.TARGETS } }
      steps {
        sh '''
          set -eu
          STAMP="$(date +%Y%m%d)-${BUILD_NUMBER}"
          rm -rf out && mkdir out
          for w in "${BACK_DIR}"/atms-*/build/libs/*.war; do
            [ -f "$w" ] || continue
            case "$w" in *-plain.war) continue ;; esac
            m="$(basename "$(dirname "$(dirname "$(dirname "$w")")")")"
            case " ${TARGETS} " in *" ${m} "*) ;; *) continue ;; esac
            n="${m}-${WAR_VERSION}-${STAMP}.war"
            cp "$w" "out/${n}"
            (cd out && sha256sum "${n}" > "${n}.sha256")
          done
          ls out/*.war >/dev/null
          echo "${STAMP}" > out/VERSION
          git rev-parse HEAD > out/COMMIT
          ls -l out
        '''
        archiveArtifacts artifacts: 'out/*', fingerprint: true
        script { currentBuild.description = readFile('out/VERSION').trim() + ' ' + env.TARGETS }
      }
    }

    stage('배포') {
      when { expression { params.MODE != 'build-only' && (params.MODE != 'build-deploy' || env.TARGETS) } }
      steps {
        script {
          if (params.MODE == 'rollback' && params.MODULE in ['all', 'auto']) { error 'rollback 은 MODULE 을 하나 고르세요' }
          if (params.MODE == 'restart' && params.MODULE == 'auto') { error 'restart 는 MODULE 을 all 또는 하나로 고르세요' }
          if (params.MODE == 'redeploy') {
            if (!(params.REDEPLOY_BUILD ==~ /\d+/)) { error 'REDEPLOY_BUILD 에 빌드 번호(숫자)를 넣으세요' }
            sh '''
              set -eu
              SRC="${JENKINS_HOME}/jobs/${JOB_NAME}/builds/${REDEPLOY_BUILD}/archive/out"
              test -d "${SRC}" || { echo "빌드 #${REDEPLOY_BUILD} 의 보관 파일 없음 (최근 10개 빌드만 보관)"; exit 1; }
              rm -rf out && cp -r "${SRC}" out
            '''
            currentBuild.description = 'redeploy ' + readFile('out/VERSION').trim() + ' ' + params.MODULE
          }
        }
        script {
          // 종료코드 4 = 재기동 메뉴가 없는 모듈이 있음(교체는 됨) → UNSTABLE. 그 밖의 0 이 아닌 값은 실패
          int rc = 0
          sshagent(credentials: ['ws01-deploy-key']) {
            rc = sh(returnStatus: true, script: '''
            set -eu
            SSH="ssh -o BatchMode=yes -o StrictHostKeyChecking=yes -o ConnectTimeout=10 -o ServerAliveInterval=30"
            case "${MODE}" in
              status)
                if [ "${MODULE}" = "all" ] || [ "${MODULE}" = "auto" ]; then ${SSH} "${WS01_SSH}" status; else ${SSH} "${WS01_SSH}" "status ${MODULE}"; fi ;;
              restart)
                if [ "${MODULE}" = "all" ]; then ${SSH} "${WS01_SSH}" restart; else ${SSH} "${WS01_SSH}" "restart ${MODULE}"; fi ;;
              rollback)
                ${SSH} "${WS01_SSH}" "rollback ${MODULE}"
                if [ "${RESTART}" = "true" ]; then ${SSH} "${WS01_SSH}" "restart ${MODULE}"; fi ;;
              *)
                COMMIT="$(cat out/COMMIT 2>/dev/null || echo -)"
                n=0; MODS=""
                for w in out/*.war; do
                  [ -f "$w" ] || continue
                  f="$(basename "$w")"
                  case " ${TARGETS} " in *" ${f%%-[0-9]*} "*) ;; *) continue ;; esac
                  SHA="$(cut -d' ' -f1 "$w.sha256")"
                  echo "=== ${f}"
                  ${SSH} "${WS01_SSH}" "deploy ${f} ${SHA} ${COMMIT}" < "$w"
                  n=$((n + 1)); MODS="${MODS} ${f%%-[0-9]*}"
                done
                [ "$n" -gt 0 ] || { echo "배포할 WAR 없음 (MODULE=${MODULE})"; exit 1; }
                if [ "${RESTART}" = "true" ]; then
                  ${SSH} "${WS01_SSH}" "restart --rollback${MODS}"
                else
                  echo "WAR ${n}개 교체 완료 (재기동 안 함 — 서버에는 아직 반영 안 됨). 반영: MODE=restart"
                  exit 5
                fi
                ;;
            esac
            ''')
          }
          // 성공이 아니면 ws01 의 모듈별 상태(프로세스·마지막 기동 로그·재기동 메뉴·최근 기록)를 콘솔 끝에 붙인다 (2026-11-10)
          //   재기동 실패 원인(앱 로그·JEUS 서버 로그 끝 40줄)은 그 위 restart 출력에 이미 찍혀 있다
          if (rc != 0 && params.MODE != 'status') {
            sshagent(credentials: ['ws01-deploy-key']) {
              sh(returnStatus: true, script: 'echo "===== ws01 상태 (실패·미반영 진단)"; ssh -o BatchMode=yes -o StrictHostKeyChecking=yes -o ConnectTimeout=10 "${WS01_SSH}" status')
            }
          }
          if (rc == 4) {
            unstable('재기동 메뉴가 없는 모듈이 있음 — 파일은 교체됨. WAS 담당에 그 서버 재기동 요청 (콘솔의 "WAS 담당 재기동 필요" 줄)')
          } else if (rc == 5) {
            unstable('WAR 교체만 함 (RESTART 꺼짐) — 서버에는 아직 반영 안 됨. MODE=restart 로 반영')
          } else if (rc != 0) {
            error("ws01 deploy-war.sh 실패 (종료코드 ${rc}) — 콘솔 확인")
          }
        }
      }
    }
  }
}
