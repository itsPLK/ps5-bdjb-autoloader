FROM maven:3.9.6-eclipse-temurin-11

# Install xorriso for ISO creation
RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential \
    git \
    pkg-config \
    libbsd-dev \
    && rm -rf /var/lib/apt/lists/*

# Build makefs for UDF 2.50 support (from bdj-sdk tools)
RUN git clone --depth 1 --recurse-submodules https://github.com/john-tornblom/bdj-sdk /tmp/bdj-sdk \
    && cd /tmp/bdj-sdk/host/src/makefs_termux \
    && make \
    && cp makefs /usr/local/bin/ \
    && rm -rf /tmp/bdj-sdk

WORKDIR /workspace
