#!/bin/bash
set -e

# Ensure we are in the project root
cd "$(dirname "$0")"

echo "Starting PS5 JAR Loader Docker Builder..."

# Build the docker image if needed
docker-compose build builder

# Run the build process
docker-compose run --rm --remove-orphans builder

echo ""
echo "Done! If successful, ps5-bdjb-autoloader.iso should be in the current directory."
