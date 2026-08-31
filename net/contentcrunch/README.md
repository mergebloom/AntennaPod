# :net:contentcrunch

Content Crunch mobile API client, secure authentication state, episode matching, segment cache, and skip decisions. The client accepts only HTTPS server URLs. Authentication persists the access token and the HttpOnly refresh cookie in encrypted preferences.

Summary availability is fetched in batches with `POST /api/v1/mobile/content-crunch/availability`. The request is `{"episodes":[{"feedUrl":"…","guid":"…","audioUrl":"…"}],"summaryConfig":{"sizeWords":300,"style":"concise"}}`; `guid` and `audioUrl` are omitted when absent. The response is `{"data":{"available":[<episode key>, ...]}}`, containing only keys with completed summaries compatible with the requested configuration.
