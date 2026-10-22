#!/bin/bash
# Deploys one image tag on the EC2 host (root is needed for the Docker socket):
#   sudo /opt/events-manager/deploy.sh <image-tag>
# Secrets come from Parameter Store through the instance role and end up in .env; they are never baked into the image.
set -euo pipefail

REGION="eu-north-1"
APP_DIR="/opt/events-manager"
PARAM_PATH="/events-manager/prod"
REPO_NAME="events-manager-api"
TAG="${1:?usage: deploy.sh <image-tag>}"

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
[ -s "$TMP_ENV" ] || { echo "no parameters found under ${PARAM_PATH}" >&2; exit 1; }
echo "APP_IMAGE=${REGISTRY}/${REPO_NAME}:${TAG}" >> "$TMP_ENV"
mv "$TMP_ENV" "${APP_DIR}/.env"

echo "==> Starting the stack"
cd "$APP_DIR"
docker compose -f docker-compose.prod.yml pull
docker compose -f docker-compose.prod.yml up -d --wait --wait-timeout 180 --remove-orphans
docker compose -f docker-compose.prod.yml ps
