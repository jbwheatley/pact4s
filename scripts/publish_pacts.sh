SCRIPT_DIR="$( cd "$( dirname "$0" )" &> /dev/null && pwd )"
if [[ ${SCRIPT_DIR} != *scripts ]]; then
  SCRIPT_DIR="${SCRIPT_DIR}/scripts"
fi
echo "Running publish_pacts script at $SCRIPT_DIR"

export PACT_BROKER_BASE_URL="https://test.pactflow.io"
export PACT_BROKER_TOKEN="129cCdfCWhMzcC9pFwb4bw"

docker run --rm \
 -w ${SCRIPT_DIR} \
 -v ${SCRIPT_DIR}:${SCRIPT_DIR} \
 -e PACT_BROKER_BASE_URL \
 -e PACT_BROKER_TOKEN \
  pactfoundation/pact-cli \
  publish \
  ${SCRIPT_DIR} \
  --consumer-app-version fake-git-sha-for-demo-$(date +%s) \
  --branch main \
  --tag pact4s-test

docker run --rm \
 -w "${SCRIPT_DIR}/feature" \
 -v "${SCRIPT_DIR}/feature":"${SCRIPT_DIR}/feature" \
 -e PACT_BROKER_BASE_URL \
 -e PACT_BROKER_TOKEN \
  pactfoundation/pact-cli \
  publish \
  "${SCRIPT_DIR}/feature" \
  --consumer-app-version fake-git-sha-for-demo-$(date +%s) \
  --branch feat/x \
  --tag pact4s-test
