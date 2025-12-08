interface ImportMetaEnv {
  readonly VITE_API_BASE_URL?: string;
  readonly VITE_SPOTIFY_AUTH_URL?: string;
  readonly VITE_GOOGLE_AUTH_URL?: string;
  readonly VITE_MAP_STYLE?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
