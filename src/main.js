import bot from "./index.js";
import { handleApi } from "./api.js";

export default {
  async fetch(request, env, ctx) {
    const url = new URL(request.url);
    if (url.pathname.startsWith("/api/")) return handleApi(request, env, url);
    return bot.fetch(request, env, ctx);
  },
};
