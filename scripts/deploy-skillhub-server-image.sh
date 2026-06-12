#!/usr/bin/env bash

set -euo pipefail

usage() {
  cat <<'EOF'
Usage:
  deploy-skillhub-server-image.sh <full-skillhub-server-image>

Example:
  deploy-skillhub-server-image.sh tyhub.tuyoo.com/iflytek_skillhub/skillhub-server:hunterdock-20260612-1500

Environment overrides:
  SKILLHUB_EXPECT_HOST   Expected hostname guard.
  SKILLHUB_EXPECT_USER   Expected Unix user guard.
  SKILLHUB_RUNTIME_DIR   Runtime directory containing .env.release and compose.release.yml.
  SKILLHUB_ENV_FILE      Release env file name.
  SKILLHUB_COMPOSE_FILE  Compose file name.
EOF
}

if [[ "${1:-}" == "-h" || "${1:-}" == "--help" ]]; then
  usage
  exit 0
fi

if [[ $# -ne 1 ]]; then
  usage >&2
  exit 1
fi

image="$1"
expected_host="${SKILLHUB_EXPECT_HOST:-sa201-cn-beijing-pt82-opsre-single-test-0004-172-16-1-9}"
expected_user="${SKILLHUB_EXPECT_USER:-tywork}"
runtime_dir="${SKILLHUB_RUNTIME_DIR:-/home/tywork/skillhub}"
env_file="${SKILLHUB_ENV_FILE:-.env.release}"
compose_file="${SKILLHUB_COMPOSE_FILE:-compose.release.yml}"

if [[ ! "${image}" =~ ^[^[:space:]]+/skillhub-server:[A-Za-z0-9._-]+$ ]]; then
  echo "Invalid SkillHub server image: ${image}" >&2
  echo "Expected a full image with tag, for example registry/org/skillhub-server:tag." >&2
  exit 1
fi

actual_host="$(hostname)"
if [[ -n "${expected_host}" && "${actual_host}" != "${expected_host}" ]]; then
  echo "Refusing to deploy on unexpected host: ${actual_host}; expected: ${expected_host}" >&2
  exit 1
fi

actual_user="$(id -un)"
if [[ -n "${expected_user}" && "${actual_user}" != "${expected_user}" ]]; then
  echo "Refusing to deploy as unexpected user: ${actual_user}; expected: ${expected_user}" >&2
  exit 1
fi

cd "${runtime_dir}"

test -f "${env_file}"
test -f "${compose_file}"

backup_file="${env_file}.bak.$(date +%Y%m%d%H%M%S)"
cp "${env_file}" "${backup_file}"

set_env_value() {
  key="$1"
  value="$2"
  tmp="${env_file}.tmp.$$"

  if grep -q "^${key}=" "${env_file}"; then
    sed "s|^${key}=.*|${key}=${value}|" "${env_file}" > "${tmp}"
  else
    cp "${env_file}" "${tmp}"
    printf '\n%s=%s\n' "${key}" "${value}" >> "${tmp}"
  fi

  mv "${tmp}" "${env_file}"
}

get_env_value() {
  key="$1"
  default_value="${2:-}"
  value="$(grep -E "^${key}=" "${env_file}" | tail -n 1 | cut -d= -f2- || true)"

  if [[ -n "${value}" ]]; then
    printf '%s' "${value}"
  else
    printf '%s' "${default_value}"
  fi
}

restore_env_on_error=1
restore_env() {
  if [[ "${restore_env_on_error}" -eq 1 ]]; then
    cp "${backup_file}" "${env_file}"
    echo "Restored ${env_file} from ${backup_file}" >&2
  fi
}
trap restore_env ERR

set_env_value "SKILLHUB_SERVER_IMAGE" "${image}"

docker pull "${image}"
docker compose --env-file "${env_file}" -f "${compose_file}" config >/dev/null

restore_env_on_error=0
trap - ERR

docker compose --env-file "${env_file}" -f "${compose_file}" up -d --no-build
docker compose --env-file "${env_file}" -f "${compose_file}" ps

server_container="$(docker compose --env-file "${env_file}" -f "${compose_file}" ps -q server)"
if [[ -z "${server_container}" ]]; then
  echo "Cannot find running server container." >&2
  exit 1
fi

actual_image="$(docker inspect "${server_container}" --format '{{.Config.Image}}')"
if [[ "${actual_image}" != "${image}" ]]; then
  echo "Server container image mismatch." >&2
  echo "Expected: ${image}" >&2
  echo "Actual:   ${actual_image}" >&2
  exit 1
fi

api_port="$(get_env_value "API_PORT" "8080")"
curl -fsS "http://127.0.0.1:${api_port}/actuator/health"
printf '\nDeployed SkillHub server image: %s\n' "${image}"
printf 'Env backup: %s\n' "${backup_file}"
