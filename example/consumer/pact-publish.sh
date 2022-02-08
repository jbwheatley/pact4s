#!/bin/bash

#This is an example of how you might publish pacts to a remote broker using the Pact Foundation's pact-cli

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"
echo "Running pact-publish script at $SCRIPT_DIR"

PACT_DIR=../resources
LATEST_COMMIT=$(git rev-parse --short HEAD)

echo Found the following pact files:
for file in ${PACT_DIR}/*; do
    echo "$(basename "$file")"
done

docker run --rm \
  -v ${PACT_DIR}:/pacts \
  pactfoundation/pact-cli \
  publish /pacts \
  --broker-base-url=$BROKER_URL \
  --consumer-app-version=$LATEST_COMMIT \
  --tag=$ENV \
  --branch=master \