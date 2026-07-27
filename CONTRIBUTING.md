# CONTRIBUTING

## Development server

### 1. Create OpenRouter provider config

Create `./.tmp/opencode-config/opencode.json`:
```json
{
  "provider": {
    "or": {
      "name": "OpenRouter",
      "npm": "@ai-sdk/openai-compatible",
      "options": {
        "baseURL": "https://openrouter.ai/api/v1",
        "apiKey": "sk-or-v1-..."
      },
      "models": {
        "google/gemini-3.6-flash": {
          "name": "Gemini 3.6 Flash"
        }
      }
    }
  }
}
```

### 2. Start the server

```bash
XDG_DATA_HOME=./.tmp OPENCODE_CONFIG_DIR=./.tmp/opencode-config OPENCODE_SERVER_PASSWORD=mysecret opencode serve --hostname 0.0.0.0 --port 4096
```

The server listens on `http://0.0.0.0:4096` with HTTP Basic Auth:
- Username: `opencode`
- Password: `mysecret`

### 3. Port Scanner (optional)

Run the port scanner on the same machine to enable the Browser tab in the app:

```bash
python3 scripts/port_scanner.py
```

The scanner listens on port 8765 by default. Override with `PORT_SCANNER_PORT`.

The scanner returns a JSON list of listening TCP ports on the machine. Configure its endpoint in the app's Settings under "Port Scanner".
