#!/usr/bin/env python3
import http.server
import json
import os
import re
import subprocess

PORT = int(os.environ.get("PORT_SCANNER_PORT", "8765"))
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


class PortHandler(http.server.BaseHTTPRequestHandler):
    def do_GET(self):
        ports = get_listening_ports()
        self.send_response(200)
        self.send_header("Content-Type", "application/json")
        self.send_header("Access-Control-Allow-Origin", "*")
        self.end_headers()
        self.wfile.write(json.dumps({"ports": ports}).encode())

    def log_message(self, format, *args):
        pass


if __name__ == "__main__":
    server = http.server.HTTPServer(("0.0.0.0", PORT), PortHandler)
    print(f"Port scanner listening on 0.0.0.0:{PORT}")
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        server.shutdown()
