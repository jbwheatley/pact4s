#!/bin/bash
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"
echo "Running publish_pacts script at $SCRIPT_DIR"

export PACT_BROKER_BASE_URL="https://test.pact.dius.com.au"
export PACT_BROKER_USERNAME="dXfltyFMgNOFZAxr8io9wJ37iUpY42M"
export PACT_BROKER_PASSWORD="O5AIZWxelWbLvqMd8PkAVycBJh2Psyg1"

docker run --rm \
 -w ${SCRIPT_DIR} \
 -v ${SCRIPT_DIR}:${SCRIPT_DIR} \
 -e PACT_BROKER_BASE_URL \
 -e PACT_BROKER_USERNAME \
 -e PACT_BROKER_PASSWORD \
  pactfoundation/pact-cli \
  publish \
  ${SCRIPT_DIR} \
  --consumer-app-version fake-git-sha-for-demo-$(date +%s) \
  --branch main

docker run --rm \
 -w "${SCRIPT_DIR}/feature" \
 -v "${SCRIPT_DIR}/feature":"${SCRIPT_DIR}/feature" \
 -e PACT_BROKER_BASE_URL \
 -e PACT_BROKER_USERNAME \
 -e PACT_BROKER_PASSWORD \
  pactfoundation/pact-cli \
  publish \
  "${SCRIPT_DIR}/feature" \
  --consumer-app-version fake-git-sha-for-demo-$(date +%s) \
  --branch feat/x
