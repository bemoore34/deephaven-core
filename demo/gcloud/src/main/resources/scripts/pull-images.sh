# Compute some default variables
export VERSION="${VERSION:-$(echo "No VERSION set! Cannot continue!"; exit 99)}"
export TYPE="${TYPE:-python}"
export ZONE="${ZONE:-us-central1}"
export DH_SLACK_TOKEN="${DH_SLACK_TOKEN:-}"
export DH_ERROR_PREFIX="${DH_ERROR_PREFIX:-}"
export PROJECT_ID="${PROJECT_ID:-deephaven-oss}"
export REPO="${REPO:-${ZONE}-docker.pkg.dev/${PROJECT_ID}/deephaven}"
export DH_DIR="${DH_DIR:-/dh}"
# Create an .env file for docker-compose to read our variables from.
echo "
VERSION=$VERSION
REPO=$REPO
TYPE=$TYPE
DH_SLACK_TOKEN=$DH_SLACK_TOKEN
DH_ERROR_PREFIX=\"$DH_ERROR_PREFIX\"
DOMAIN=${DOMAIN:-${FIRST_DOMAIN:-demo.deephaven.app}}
" > "$DH_DIR/.env"

# Run docker-compose pull in a new shell, so it uses newly-created docker group
REPO_ROOT="$(echo "$REPO" | cut -d "/" -f1 )"
bash -c "cd $DH_DIR ; docker-compose pull" || {
    log "Unable to pull images; attempting to reauthenticate with gcloud"
    gcloud auth configure-docker "${REPO_ROOT}" -q
    bash -c "cd $DH_DIR ; docker-compose pull"
}