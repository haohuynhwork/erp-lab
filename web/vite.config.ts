import tailwindcss from "@tailwindcss/vite";
import { tanstackRouter } from "@tanstack/router-plugin/vite";
import react from "@vitejs/plugin-react";
import { defineConfig, loadEnv } from "vite";

// WEB_URL / API_URL come from the root .env; their paths are the web and api prefixes.
const root = new URL("..", import.meta.url).pathname;

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, root, "");
  const webUrl = new URL(env.WEB_URL || "http://localhost:5173/web");
  const apiUrl = new URL(env.API_URL || "http://localhost:8080/api");
  const base = `${webUrl.pathname.replace(/\/+$/, "")}/`;
  const apiPath = apiUrl.pathname.replace(/\/+$/, "") || "/";

  return {
    base,
    envDir: root,
    define: {
      "import.meta.env.WEB_URL": JSON.stringify(webUrl.href),
      "import.meta.env.API_URL": JSON.stringify(apiUrl.href),
    },
    plugins: [tanstackRouter({ target: "react", autoCodeSplitting: true }), react(), tailwindcss()],
    resolve: { alias: { "@": new URL("./src", import.meta.url).pathname } },
    server: {
      port: Number(webUrl.port) || 5173,
      strictPort: true,
      // Dev: the api runs on its own port; proxy its path so calls work same-origin too.
      proxy: { [apiPath]: { target: apiUrl.origin, ws: true } },
    },
  };
});
