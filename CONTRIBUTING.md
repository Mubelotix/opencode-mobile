# CONTRIBUTING

## Development server

### 1. Create OpenRouter provider config

Create `./opencode-server-config/opencode.json`:
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
XDG_DATA_HOME=./opencode-server-data OPENCODE_CONFIG_DIR=./opencode-server-config OPENCODE_SERVER_PASSWORD=mysecret opencode serve --hostname 0.0.0.0 --port 4096
```

The server listens on `http://0.0.0.0:4096` with HTTP Basic Auth:
- Username: `opencode`
- Password: `mysecret`

### 3. Helper Server (optional)

Run the helper server on the same machine to enable the Browser tab and file system operations (new folder creation, etc.) in the app:

```bash
python3 scripts/helper_server.py
```

The server listens on port 8765 by default. Override with `HELPER_SERVER_PORT`.

Configure its endpoint in the app's Settings under "Helper Server".
