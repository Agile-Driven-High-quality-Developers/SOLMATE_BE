#!/bin/bash

DOCKER_USERNAME=$1
IMAGE_TAG=$2

CURRENT_PORT=$(sudo nginx -T 2>/dev/null | grep 'proxy_pass' | grep -o '808[0-1]' | head -1)

# 최초 배포 시 CURRENT_PORT 가 비어있으면 8080으로 시작
if [ -z "$CURRENT_PORT" ]; then
  echo "최초 배포 - 8080으로 시작"
  NEW_PORT=8080
  OLD_CONTAINER=""
  NEW_CONTAINER=app-8080
elif [ "$CURRENT_PORT" == "8080" ]; then
  NEW_PORT=8081
  OLD_CONTAINER=app-8080
  NEW_CONTAINER=app-8081
else
  NEW_PORT=8080
  OLD_CONTAINER=app-8081
  NEW_CONTAINER=app-8080
fi

echo "현재 포트: $CURRENT_PORT → 새 포트: $NEW_PORT"

# 새 이미지 pull
DOCKER_USERNAME=$DOCKER_USERNAME IMAGE_TAG=$IMAGE_TAG \
  docker compose pull $NEW_CONTAINER

# 새 컨테이너 실행
DOCKER_USERNAME=$DOCKER_USERNAME IMAGE_TAG=$IMAGE_TAG \
  docker compose up -d $NEW_CONTAINER

# 헬스체크 (최대 60초 대기)
echo "헬스체크 중..."
for i in $(seq 1 24); do
  sleep 5
  RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:$NEW_PORT/actuator/health)
  if [ "$RESPONSE" == "200" ]; then
    echo "헬스체크 통과!"
    break
  fi
  echo "대기 중... ($i/12)"
  if [ $i -eq 24 ]; then
    echo "헬스체크 실패 - 배포 중단"
    docker compose stop $NEW_CONTAINER
    exit 1
  fi
done

# Nginx 트래픽 전환
sudo sed -i "s/proxy_pass http:\/\/localhost:[0-9]*/proxy_pass http:\/\/localhost:$NEW_PORT/" /etc/nginx/sites-available/default
sudo nginx -s reload
echo "Nginx → $NEW_PORT 전환 완료"

# 구 컨테이너 종료 (최초 배포 시엔 건너뜀)
if [ -n "$OLD_CONTAINER" ]; then
  docker compose stop $OLD_CONTAINER
  echo "$OLD_CONTAINER 종료 완료"
fi

# 안 쓰는 이미지 정리 (최근 3개는 유지)
docker images "$DOCKER_USERNAME/solmate-be" --format "{{.ID}}" | tail -n +4 | xargs -r docker rmi -f
echo "사용하지 않는 이미지 정리 완료 (최근 3개 유지)"