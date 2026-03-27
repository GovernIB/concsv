/// <reference types="vite/client" />

interface ImportMetaEnv {
	BASE_URL: string;
	readonly VITE_API_URL: string;
	readonly VITE_APP_VERSION: string;
	readonly VITE_API_BASE_URL: string;
	readonly VITE_API_SUFFIX: string;
	readonly VITE_PREVIEW_ENABLED: boolean;
	readonly VITE_RECAPTCHA_ENABLED: boolean;
	readonly VITE_RECAPTCHA_SITE_KEY: string;
}

interface ImportMeta {
	readonly env: ImportMetaEnv;
}