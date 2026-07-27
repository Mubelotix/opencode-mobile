#!/usr/bin/env python3
import http.server
import json
import os
import re
import subprocess
import urllib.parse

PORT = int(os.environ.get("HELPER_SERVER_PORT", "8765"))
SCAN_RANGE = range(3000, 65536)


def get_listening_ports():
    ports = set()
    try:
        result = subprocess.run(
            ["ss", "-tlnp"], capture_output=True, text=True, timeout=5
        )
        for line in result.stdout.splitlines():
            m = re.search(r":(\d+)\s", line)
            if m:
                p = int(m.group(1))
                if p != PORT:
                    ports.add(p)
    except Exception:
        pass

    if not ports:
        try:
            result = subprocess.run(
                ["ss", "-tln"], capture_output=True, text=True, timeout=5
            )
            for line in result.stdout.splitlines():
                m = re.search(r":(\d+)\s", line)
                if m:
                    p = int(m.group(1))
                    if p != PORT:
                        ports.add(p)
        except Exception:
            pass

    if not ports:
        try:
            result = subprocess.run(
                ["netstat", "-tln"], capture_output=True, text=True, timeout=5
            )
            for line in result.stdout.splitlines():
                m = re.search(r":(\d+)\s", line)
                if m:
                    p = int(m.group(1))
                    if p != PORT:
                        ports.add(p)
        except Exception:
            pass

    return sorted(ports)


def send_json(handler, data, status=200):
    handler.send_response(status)
    handler.send_header("Content-Type", "application/json")
    handler.send_header("Access-Control-Allow-Origin", "*")
    handler.end_headers()
    handler.wfile.write(json.dumps(data).encode())


def sanitize_path(path):
    if not path or not path.startswith("/"):
        return None
    resolved = os.path.normpath(path)
    if not resolved.startswith("/"):
        return None
    if ".." in path.split("/"):
        return None
    return resolved


class Handler(http.server.BaseHTTPRequestHandler):
    def do_GET(self):
        parsed = urllib.parse.urlparse(self.path)
        if parsed.path == "/" or parsed.path == "/ports":
            ports = get_listening_ports()
            send_json(self, {"ports": ports})
        else:
            send_json(self, {"error": "not found"}, 404)

    def do_POST(self):
        parsed = urllib.parse.urlparse(self.path)
        if parsed.path == "/fs/mkdir":
            content_length = int(self.headers.get("Content-Length", 0))
            if content_length == 0:
                send_json(self, {"ok": False, "error": "missing body"}, 400)
                return

            raw = self.rfile.read(content_length)
            try:
                body = json.loads(raw)
            except json.JSONDecodeError:
                send_json(self, {"ok": False, "error": "invalid json"}, 400)
                return

            path = body.get("path", "")
            sanitized = sanitize_path(path)
            if sanitized is None:
                send_json(self, {"ok": False, "error": "invalid path"}, 400)
                return

            try:
                os.makedirs(sanitized, exist_ok=True)
                send_json(self, {"ok": True})
            except OSError as e:
                send_json(self, {"ok": False, "error": str(e)}, 500)
        else:
            send_json(self, {"error": "not found"}, 404)

    def do_OPTIONS(self):
        self.send_response(204)
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
        self.send_header("Access-Control-Allow-Headers", "Content-Type, Authorization")
        self.end_headers()

    def log_message(self, format, *args):
        pass


if __name__ == "__main__":
    server = http.server.HTTPServer(("0.0.0.0", PORT), Handler)
    print(f"Helper server listening on 0.0.0.0:{PORT}")
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        server.shutdown()
