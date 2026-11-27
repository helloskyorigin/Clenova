# Private Groq relay (development)

Run Node 20+ behind an HTTPS reverse proxy. Configure environment secrets on the server only:

- GROQ_API_KEY: secret from your Groq account
- GROQ_MODEL: a currently available Groq model supporting JSON object mode
- AGENT_RELAY_TOKEN: a private random bearer token of at least 32 characters (not a Groq key)
- PORT: optional, default 8080; the process binds localhost

Run: node server/relay.mjs
Test: node server/relay.test.mjs

Use Agent menu > Connect AI (private test) in the app. Enter your HTTPS origin ending in /agent and private relay token. Credentials are held in memory only. Cloud chat is disabled by default and sends the current typed request and at most three previous user-typed messages from the same conversation, with explicit opt-in; no assistant scan results, file lists or full conversation history are sent. The backend filters prior text for plausible phone-related context. The server does not log conversation contents. Groq's own processing/retention terms apply. Do not enter sensitive information.

This is a private developer relay, not production public authentication. Before public release use per-user or device-attested short-lived authentication, durable rate limiting, quotas, monitoring, privacy/retention review and abuse testing. Scope checks plus prompt and output validation reduce unrelated responses but cannot guarantee perfect semantic filtering. Model output is advisory: no model tool execution. The Android user must tap a review action and separately approve actual cleanup.

No deployment or Groq key is supplied by this repository. Local supported commands work without the relay.
