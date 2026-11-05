#!/bin/bash
# Deploys one service's image tag on the EC2 host (root is needed for the Docker socket):
#   sudo /opt/events-manager/deploy.sh <service> <image-tag>
# <service> is events-manager (this repository's pipeline) or events-notification (the notification service's).
# Secrets come from Parameter Store through the instance role and end up in .env; they are never baked into the image.
# .env is rebuilt on every deploy, so the image of the other service is carried over from the previous .env: each
# pipeline changes only its own service. The consumer profile is turned on once the host knows the consumer's image.
set -euo pipefail

REGION="eu-north-1"
APP_DIR="/opt/events-manager"
PARAM_PATH="/events-manager/prod"
USAGE="usage: deploy.sh <events-manager|events-notification> <image-tag>"
SERVICE="${1:?$USAGE}"
TAG="${2:?$USAGE}"

case "$SERVICE" in
  events-manager) IMAGE_VAR="APP_IMAGE" REPO_NAME="events-manager-api" ;;
  events-notification) IMAGE_VAR="NOTIFICATION_IMAGE" REPO_NAME="events-notification-service" ;;
  *) echo "$USAGE" >&2; exit 1 ;;
esac

ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
REGISTRY="${ACCOUNT_ID}.dkr.ecr.${REGION}.amazonaws.com"

echo "==> ECR login"
aws ecr get-login-password --region "$REGION" | docker login --username AWS --password-stdin "$REGISTRY"

echo "==> Reading secrets from ${PARAM_PATH}"
umask 077
TMP_ENV=$(mktemp "${APP_DIR}/.env.XXXXXX")
trap 'rm -f "$TMP_ENV"' EXIT
aws ssm get-parameters-by-path --region "$REGION" --path "$PARAM_PATH" --with-decryption \
  --query "Parameters[].[Name,Value]" --output text \
  | while IFS=$'\t' read -r name value; do echo "${name##*/}=${value}"; done > "$TMP_ENV"
[[ -s "$TMP_ENV" ]] || { echo "no parameters found under ${PARAM_PATH}" >&2; exit 1; }

# The image this deploy sets, then the other service's image as the previous deploy left it (if any).
echo "${IMAGE_VAR}=${REGISTRY}/${REPO_NAME}:${TAG}" >> "$TMP_ENV"
if [[ -f "${APP_DIR}/.env" ]]; then
  grep -E '^(APP_IMAGE|NOTIFICATION_IMAGE)=' "${APP_DIR}/.env" | grep -v "^${IMAGE_VAR}=" >> "$TMP_ENV" || true
fi
grep -q '^APP_IMAGE=' "$TMP_ENV" || { echo "deploy events-manager first: the host has no API image yet" >&2; exit 1; }
if grep -q '^NOTIFICATION_IMAGE=' "$TMP_ENV"; then
  echo "COMPOSE_PROFILES=consumer" >> "$TMP_ENV"
fi
mv "$TMP_ENV" "${APP_DIR}/.env"

echo "==> Starting the stack (${SERVICE} at ${TAG})"
cd "$APP_DIR"
docker compose -f docker-compose.prod.yml pull --quiet
docker compose -f docker-compose.prod.yml up -d --wait --wait-timeout 180 --remove-orphans
docker compose -f docker-compose.prod.yml ps
