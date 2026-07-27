FROM node:22-slim

RUN apt-get update && apt-get install -y --no-install-recommends \
    python3 \
    iproute2 \
    && rm -rf /var/lib/apt/lists/*

RUN npm install -g opencode-ai@latest

ENV OPENCODE_SERVER_USERNAME=opencode
ENV OPENCODE_CONFIG_DIR=/opencode-config
ENV HELPER_SERVER_PORT=8765

RUN mkdir -p /opencode-config /opencode-data

COPY scripts/helper_server.py /usr/local/bin/helper-server
COPY scripts/entrypoint.sh /usr/local/bin/entrypoint

RUN chmod +x /usr/local/bin/helper-server /usr/local/bin/entrypoint

EXPOSE 4096 8765

ENTRYPOINT ["/usr/local/bin/entrypoint"]
