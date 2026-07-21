# CONTRIBUTING

## Development server

Start the opencode server for local API development:

```bash
OPENCODE_SERVER_PASSWORD=mysecret opencode serve --hostname 0.0.0.0 --port 4096
```

The server listens on `http://0.0.0.0:4096` with HTTP Basic Auth:
- Username: `opencode`
- Password: `mysecret`

For WebSocket/SSE connections, append `?auth_token=base64(opencode:mysecret)` to the URL.
