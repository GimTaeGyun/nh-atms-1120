# ~/deploy/deploy-war.local.sh : 포트 없이 재기동 확인 (서버에서 직접 고칠 때, 반입본 deploy-war.sh 에 한 줄로 읽힘)
JL="${JEUS_LOG_DIR:-/WASlogs/jeusATMSlogs/nbatmsltws01/app}"
JS="${JEUS_SRV_LOG_DIR:-/WASlogs/jeusATMSlogs/nbatmsltws01}"
DENY_MENUS="3 17 18"
proc_up() { ps -u jeusatms -o args= 2>/dev/null | grep -Eq -- "obj_name=$(cfg SERVER "$1")( |$)"; }
server_up() { proc_up "$1"; }
health_code() { if proc_up "$1"; then echo 200; else echo 000; fi; }
fsize() { wc -c 2>/dev/null < "$1" || echo 0; }
newpart() { local o="$2"; [[ $(fsize "$1") -lt $o ]] && o=0; tail -c +$(( o + 1 )) "$1" 2>/dev/null || true; }
show_logs() {
  local f; for f in "${JL}/$1.log" "${JS}/$(cfg SERVER "$1")/JeusServer.log"; do
    echo "----- $f (끝 40줄)"; tail -n 40 "$f" 2>/dev/null || echo "(없음 또는 읽기 불가)"
  done; echo "-----"
}
jeus_menu() {
  local n; for n in "$(cfg STOP_MENU "$m")" "$(cfg BOOT_MENU "$m")"; do
    [[ "$n" =~ ^[0-9]{1,2}$ ]] || die "메뉴 번호 오류: $n"
    [[ " ${DENY_MENUS} " == *" $n "* ]] && die "쓰지 않는 메뉴: $n"
  done
  [[ "${JEUS_RSTART}" -nt "${JEUS_RSTART%/*}/jeusmenu.c" ]] || die "jeusRstart 재컴파일 전 (jeusmenu.c 가 더 새로움) → 메뉴 안 넣음"
  grep -aq -- " $(cfg SERVER "$m")'" "${JEUS_RSTART}" || die "jeusRstart 에 $(cfg SERVER "$m") 없음 → 메뉴 안 넣음"
  OFF=$(fsize "${JL}/${m}.log"); OFF2=$(fsize "${JS}/$(cfg SERVER "$m")/JeusServer.log")
  log "jeusRstart 메뉴 $1"
  printf '%s\n' "$1" | "${JEUS_RSTART}" 9>&- || true
}
wait_ready() {
  local i r
  [[ " ${HEALTH_SKIP} " == *" $1 "* ]] && return 0
  for (( i = 0; i <= BOOT_WAIT; i += 10 )); do
    r=$( { newpart "${JL}/$1.log" "${OFF:-0}"; newpart "${JS}/$(cfg SERVER "$1")/JeusServer.log" "${OFF2:-0}"; } | grep -oE 'APPLICATION FAILED TO START|Application run failed|Started Atms[A-Za-z]+Application' | tail -1 || true)
    case "$r" in Started*) log "기동 확인: $1"; return 0 ;; ?*) log "기동 실패: $1 ($r)"; show_logs "$1"; return 1 ;; esac
    sleep 10
  done
  log "기동 확인 실패 (${BOOT_WAIT}초): $1 (앱 로그·JEUS 서버 로그에 Started 없음)"; show_logs "$1"; return 1
}
