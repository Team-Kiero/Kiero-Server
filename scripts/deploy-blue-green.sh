#!/bin/bash

###############################################################################
# Blue-Green 무중단 배포 스크립트
###############################################################################

set -e  # 에러 발생 시 스크립트 중단

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# 로그 함수
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 사용할 Docker Compose 파일 결정
COMPOSE_FILE=${COMPOSE_FILE:-docker-compose.dev.yml}
log_info "사용할 Compose 파일: $COMPOSE_FILE"

# 환경에 따른 컨테이너 이름 접두사 및 env 파일 결정
if [[ "$COMPOSE_FILE" == *"dev"* ]]; then
    CONTAINER_PREFIX="kiero-dev-app"
    ENV_FILE=".env.dev"
else
    CONTAINER_PREFIX="kiero-prod-app"
    ENV_FILE=".env.prod"
fi
log_info "컨테이너 이름 접두사: $CONTAINER_PREFIX"
log_info "환경변수 파일: $ENV_FILE"

# Docker Compose 명령어 (env-file 포함)
DOCKER_COMPOSE="docker compose -f $COMPOSE_FILE --env-file $ENV_FILE"

# 옵션 파싱
KEEP_OLD=false
while [[ "$#" -gt 0 ]]; do
    case $1 in
        --keep-old) KEEP_OLD=true ;;
        *) log_error "Unknown parameter: $1"; exit 1 ;;
    esac
    shift
done

# 프로젝트 루트 디렉토리로 이동
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$PROJECT_ROOT"

log_info "프로젝트 디렉토리: $PROJECT_ROOT"

log_info "redis, prometheus, grafana, nocodb 컨테이너를 띄웁니다."
$DOCKER_COMPOSE up -d redis prometheus grafana nocodb

# EC2 Nginx 설정 파일 경로
NGINX_CONF="/etc/nginx/sites-available/kiero"

# Nginx 설정 파일 존재 확인
if [ ! -f "$NGINX_CONF" ]; then
    log_error "EC2 Nginx 설정 파일을 찾을 수 없습니다: $NGINX_CONF"
    log_error "먼저 nginx/kiero.conf를 $NGINX_CONF 로 복사하세요."
    exit 1
fi

###############################################################################
# 1. 현재 활성 환경 확인
###############################################################################

log_info "현재 활성 환경 확인 중..."

# nginx.conf에서 ACTIVE_CONTAINER 주석이 있는 줄 찾기
ACTIVE_LINE=$(grep "ACTIVE_CONTAINER" "$NGINX_CONF")

if echo "$ACTIVE_LINE" | grep -q "localhost:8080"; then
    ACTIVE="blue"
    INACTIVE="green"
    INACTIVE_PORT=8081
elif echo "$ACTIVE_LINE" | grep -q "localhost:8081"; then
    ACTIVE="green"
    INACTIVE="blue"
    INACTIVE_PORT=8080
else
    log_error "활성 환경을 확인할 수 없습니다. $NGINX_CONF 를 확인하세요."
    exit 1
fi

log_info "현재 활성 환경: ${YELLOW}$ACTIVE${NC}"
log_info "새 버전 배포 대상: ${YELLOW}$INACTIVE${NC} (포트: $INACTIVE_PORT)"

###############################################################################
# 2. 새 이미지 Pull
###############################################################################

log_info "새 Docker 이미지 pulling..."

if [ "$INACTIVE" == "green" ]; then
    export GREEN_TAG="${GREEN_TAG:-latest}"
    log_info "Green 이미지 태그: $GREEN_TAG"
else
    export BLUE_TAG="${BLUE_TAG:-latest}"
    log_info "Blue 이미지 태그: $BLUE_TAG"
fi

$DOCKER_COMPOSE pull app-$INACTIVE

###############################################################################
# 3. 비활성 환경에 새 버전 배포
###############################################################################

log_info "$INACTIVE 환경에 새 버전 배포 중..."

# 기존 비활성 컨테이너 정지 및 제거
$DOCKER_COMPOSE stop app-$INACTIVE || true
$DOCKER_COMPOSE rm -f app-$INACTIVE || true

# 새 컨테이너 시작
$DOCKER_COMPOSE up -d app-$INACTIVE

log_info "$INACTIVE 컨테이너 시작 완료. Health check 대기 중..."

###############################################################################
# 4. Health Check
###############################################################################

HEALTH_CHECK_TIMEOUT=120
HEALTH_CHECK_INTERVAL=5
ELAPSED=0

while [ $ELAPSED -lt $HEALTH_CHECK_TIMEOUT ]; do
    HEALTH_STATUS=$(docker inspect --format='{{.State.Health.Status}}' "${CONTAINER_PREFIX}-${INACTIVE}" 2>/dev/null || echo "unknown")

    if [ "$HEALTH_STATUS" == "healthy" ]; then
        log_success "Health check 성공! (${ELAPSED}s)"
        break
    fi

    if [ "$HEALTH_STATUS" == "starting" ]; then
        log_info "애플리케이션 부팅 중... ($HEALTH_STATUS) (${ELAPSED}s / ${HEALTH_CHECK_TIMEOUT}s)"
    else
        log_warning "Health check 상태: $HEALTH_STATUS (${ELAPSED}s / ${HEALTH_CHECK_TIMEOUT}s)"
    fi

    log_info "Health check 상태: $HEALTH_STATUS (${ELAPSED}s / ${HEALTH_CHECK_TIMEOUT}s) - Target: ${CONTAINER_PREFIX}-${INACTIVE}"
    sleep $HEALTH_CHECK_INTERVAL
    ELAPSED=$((ELAPSED + HEALTH_CHECK_INTERVAL))
done

if [ "$HEALTH_STATUS" != "healthy" ]; then
    log_error "$INACTIVE 환경이 정상적으로 시작되지 않았습니다. 배포를 중단합니다."
    log_error "로그 확인: docker logs ${CONTAINER_PREFIX}-${INACTIVE}"| tail -n 100
    $DOCKER_COMPOSE stop app-$INACTIVE
    exit 1
fi

###############################################################################
# 5. Nginx 설정 변경 (트래픽 전환)
###############################################################################

log_warning "트래픽을 $ACTIVE -> $INACTIVE 로 전환합니다..."

# EC2 Nginx 설정 파일의 upstream 변경
if [ "$INACTIVE" == "green" ]; then
    # Blue(8080) -> Green(8081)로 전환
    sudo sed -i.bak "s/server localhost:8080;  # ACTIVE_CONTAINER/server localhost:8081;  # ACTIVE_CONTAINER/" "$NGINX_CONF"
else
    # Green(8081) -> Blue(8080)로 전환
    sudo sed -i.bak "s/server localhost:8081;  # ACTIVE_CONTAINER/server localhost:8080;  # ACTIVE_CONTAINER/" "$NGINX_CONF"
fi

# Nginx 설정 문법 확인
log_info "Nginx 설정 문법 확인 중..."
if ! sudo nginx -t; then
    log_error "Nginx 설정 파일에 오류가 있습니다. 변경을 되돌립니다."
    sudo mv "$NGINX_CONF.bak" "$NGINX_CONF"
    exit 1
fi

# Nginx reload
log_info "Nginx reload 중..."
sudo systemctl reload nginx

log_success "트래픽 전환 완료! 모든 요청이 이제 $INACTIVE 환경으로 전달됩니다."

###############################################################################
# 6. 이전 환경 정리
###############################################################################

if [ "$KEEP_OLD" == true ]; then
    log_warning "이전 버전($ACTIVE)을 유지합니다. 롤백이 필요하면 다시 스크립트를 실행하세요."
else
    log_info "10초 후 이전 버전($ACTIVE)을 정지합니다. (Ctrl+C로 취소 가능)"
    sleep 10

    $DOCKER_COMPOSE stop app-$ACTIVE
    log_success "이전 버전($ACTIVE) 정지 완료"
fi

###############################################################################
# 7. 배포 완료
###############################################################################

log_success "======================================"
log_success "Blue-Green 배포 완료!"
log_success "======================================"
log_info "활성 환경: $INACTIVE (포트: $INACTIVE_PORT)"
log_info "비활성 환경: $ACTIVE"
log_info ""
log_info "확인 명령어:"
log_info "  - 현재 상태: docker ps"
log_info "  - 로그 확인: docker logs ${CONTAINER_PREFIX}-${INACTIVE}"
log_info "  - Health check: curl http://localhost/health/$INACTIVE"
log_info "  - Nginx 설정: cat $NGINX_CONF | grep ACTIVE_CONTAINER"
log_info ""

if [ "$KEEP_OLD" == true ]; then
    log_warning "롤백 방법: 이전 버전($ACTIVE)이 아직 실행 중입니다."
    log_warning "문제 발생 시: ./scripts/deploy-blue-green.sh 를 다시 실행하면 $ACTIVE로 전환됩니다."
fi