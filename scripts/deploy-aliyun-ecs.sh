#!/usr/bin/env bash
set -Eeuo pipefail

RAINIER_DIR="${RAINIER_DIR:-/opt/rainier}"
REPO_URL="${REPO_URL:-https://github.com/dogekul/rainier.git}"
BRANCH="${BRANCH:-main}"
PUBLIC_BASE_URL="${PUBLIC_BASE_URL:-http://8.166.121.138}"
ENV_FILE="${RAINIER_DIR}/.env"
COMPOSE_FILE="${RAINIER_DIR}/deploy/aliyun/docker-compose.ecs.yml"

log() {
  printf '\n[deploy] %s\n' "$*"
}

need_root() {
  if [ "$(id -u)" != "0" ]; then
    echo "Please run as root." >&2
    exit 1
  fi
}

rand_alnum() {
  local n="${1:-32}"
  openssl rand -hex "${n}" | cut -c "1-${n}"
}

install_packages() {
  log "Installing base packages if needed"
  if command -v apt-get >/dev/null 2>&1; then
    apt-get update
    apt-get install -y ca-certificates curl git openssl
  elif command -v dnf >/dev/null 2>&1; then
    dnf install -y ca-certificates curl git openssl
  elif command -v yum >/dev/null 2>&1; then
    yum install -y ca-certificates curl git openssl
  else
    echo "Unsupported Linux distribution: no apt-get/dnf/yum found." >&2
    exit 1
  fi
}

install_docker() {
  if ! command -v docker >/dev/null 2>&1; then
    log "Installing Docker"
    curl -fsSL https://get.docker.com | sh
  fi
  systemctl enable docker >/dev/null 2>&1 || true
  systemctl start docker
  if ! docker compose version >/dev/null 2>&1; then
    echo "Docker Compose plugin is not available after Docker install." >&2
    echo "Install docker compose plugin, then re-run this script." >&2
    exit 1
  fi
}

checkout_repo() {
  log "Checking out ${REPO_URL} (${BRANCH}) into ${RAINIER_DIR}"
  mkdir -p "${RAINIER_DIR}"
  if [ -d "${RAINIER_DIR}/.git" ]; then
    git -C "${RAINIER_DIR}" fetch origin "${BRANCH}"
    git -C "${RAINIER_DIR}" checkout "${BRANCH}"
    git -C "${RAINIER_DIR}" reset --hard "origin/${BRANCH}"
  else
    local saved_env=""
    if [ -f "${ENV_FILE}" ]; then
      saved_env="$(mktemp)"
      cp "${ENV_FILE}" "${saved_env}"
    fi
    find "${RAINIER_DIR}" -mindepth 1 -maxdepth 1 -exec rm -rf {} +
    git clone --branch "${BRANCH}" "${REPO_URL}" "${RAINIER_DIR}"
    if [ -n "${saved_env}" ]; then
      cp "${saved_env}" "${ENV_FILE}"
      rm -f "${saved_env}"
    fi
  fi
}

write_env() {
  if [ -f "${ENV_FILE}" ]; then
    log "Keeping existing ${ENV_FILE}"
    return
  fi

  log "Creating ${ENV_FILE} with generated secrets"
  cat >"${ENV_FILE}" <<EOF
RAINIER_MYSQL_DATABASE=rainier
RAINIER_MYSQL_USERNAME=rainier
RAINIER_MYSQL_PASSWORD=$(rand_alnum 32)
RAINIER_MYSQL_ROOT_PASSWORD=$(rand_alnum 32)
RAINIER_JWT_SECRET=$(rand_alnum 80)
RAINIER_DEFAULT_PASSWORD=$(rand_alnum 18)
RAINIER_GITLAB_WEBHOOK_SECRET=$(rand_alnum 40)
RAINIER_FRONTEND_BASE_URL=${PUBLIC_BASE_URL}
RAINIER_REAL_AUTH_ENABLED=false
EOF
  chmod 600 "${ENV_FILE}"
}

compose() {
  docker compose --env-file "${ENV_FILE}" -f "${COMPOSE_FILE}" "$@"
}

wait_for_health() {
  log "Waiting for backend health"
  local i
  for i in $(seq 1 60); do
    if curl -fsS http://127.0.0.1:8080/api/health | grep -q '"status":"UP"'; then
      return 0
    fi
    sleep 3
  done
  compose ps
  compose logs --tail=200 backend
  echo "Backend did not become healthy in time." >&2
  exit 1
}

seed_minimal_admin() {
  log "Ensuring minimal demo admin user exists"
  set -a
  # shellcheck disable=SC1090
  . "${ENV_FILE}"
  set +a
  compose exec -T mysql mysql -uroot -p"${RAINIER_MYSQL_ROOT_PASSWORD}" "${RAINIER_MYSQL_DATABASE}" <<'SQL'
SET @now := NOW(6);

INSERT INTO rainier_user
  (login_name, name, code, email_address, is_internal, enabled, ai_auth_level,
   create_by, create_time, update_by, update_time, del_flag)
SELECT 'alice', 'Alice', 'U001', 'alice@example.com', 1, 1, 'DEPTH',
       'system', @now, 'system', @now, 0
WHERE NOT EXISTS (
  SELECT 1 FROM rainier_user WHERE login_name = 'alice' AND del_flag = 0
);

INSERT INTO rainier_role
  (code, name, description, enabled, admin_access,
   create_by, create_time, update_by, update_time, del_flag)
SELECT 'PMO', 'PMO', 'Bootstrap PMO admin role', 1, 1,
       'system', @now, 'system', @now, 0
WHERE NOT EXISTS (
  SELECT 1 FROM rainier_role WHERE code = 'PMO' AND del_flag = 0
);

INSERT INTO rainier_user_role
  (user_id, role_id, project_id, create_by, create_time, update_by, update_time, del_flag)
SELECT u.id, r.id, NULL, 'system', @now, 'system', @now, 0
FROM rainier_user u
JOIN rainier_role r ON r.code = 'PMO' AND r.del_flag = 0
WHERE u.login_name = 'alice'
  AND u.del_flag = 0
  AND NOT EXISTS (
    SELECT 1
    FROM rainier_user_role ur
    WHERE ur.user_id = u.id
      AND ur.role_id = r.id
      AND ur.project_id IS NULL
      AND ur.del_flag = 0
  );
SQL
}

main() {
  need_root
  install_packages
  install_docker
  checkout_repo
  write_env

  log "Building and starting Rainier"
  compose up -d --build
  wait_for_health
  seed_minimal_admin

  log "Deployment complete"
  compose ps
  echo
  echo "URL: ${PUBLIC_BASE_URL}"
  echo "Login: alice / any password (demo auth mode)"
  echo "Generated secrets are stored at: ${ENV_FILE}"
}

main "$@"
