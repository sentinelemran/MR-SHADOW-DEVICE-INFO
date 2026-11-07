// ---------- Messages (Bengali / English) ----------
const fmt = (c) => `#${c.client_no} ${c.name}${c.phone ? " · " + c.phone : ""}`;

const T = {
  bn: {
    help: `MR-SHADOW-DEVICE-INFO — অ্যাডমিন প্যানেল

👤 ক্লায়েন্ট
/client_add নাম | ফোন
/client_list
/client_find নাম

🌐 ভাষা
/lang bn অথবা /lang en

(কাজ ও রিপোর্টের কমান্ড শীঘ্রই যোগ হবে)`,
    unknown: "অজানা কমান্ড। /help দিন।",
    needName: "নাম দিন। উদাহরণ:\n/client_add রহিম উদ্দিন | 01XXXXXXXXX",
    added: (c) => `✅ ক্লায়েন্ট যোগ হয়েছে:\n${fmt(c)}`,
    empty: "এখনো কোনো ক্লায়েন্ট নেই।",
    listTitle: "সর্বশেষ ক্লায়েন্ট:",
    needQuery: "খুঁজতে নাম লিখুন। উদাহরণ:\n/client_find রহিম",
    notFound: "কাউকে পাওয়া যায়নি।",
    foundTitle: "পাওয়া গেছে:",
    langSet: "ভাষা: বাংলা ✅",
    langUsage: "ব্যবহার: /lang bn অথবা /lang en",
    error: "কিছু একটা সমস্যা হয়েছে। একটু পরে আবার চেষ্টা করুন।",
  },
  en: {
    help: `MR-SHADOW-DEVICE-INFO — Admin panel

👤 Clients
/client_add name | phone
/client_list
/client_find name

🌐 Language
/lang bn or /lang en

(Job and report commands coming soon)`,
    unknown: "Unknown command. Send /help.",
    needName: "Please give a name. Example:\n/client_add Rahim Uddin | 01XXXXXXXXX",
    added: (c) => `✅ Client added:\n${fmt(c)}`,
    empty: "No clients yet.",
    listTitle: "Latest clients:",
    needQuery: "Type a name to search. Example:\n/client_find Rahim",
    notFound: "No match found.",
    foundTitle: "Found:",
    langSet: "Language: English ✅",
    langUsage: "Usage: /lang bn or /lang en",
    error: "Something went wrong. Please try again shortly.",
  },
};

// ---------- Helpers ----------
async function reply(env, chatId, text) {
  await fetch(`https://api.telegram.org/bot${env.BOT_TOKEN}/sendMessage`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ chat_id: chatId, text }),
  });
}

async function db(env, path, init = {}) {
  const key = env.SUPABASE_KEY;
  const headers = { apikey: key, "Content-Type": "application/json", ...(init.headers || {}) };
  if (key.startsWith("eyJ")) headers.Authorization = `Bearer ${key}`;
  const base = env.SUPABASE_URL.replace(/\/$/, "");
  const res = await fetch(`${base}/rest/v1/${path}`, { ...init, headers });
  if (!res.ok) throw new Error(`DB ${res.status}`);
  const body = await res.text();
  return body ? JSON.parse(body) : null;
}

async function getLang(env) {
  try {
    const rows = await db(env, "bot_settings?key=eq.lang&select=value");
    return rows && rows[0] && rows[0].value === "en" ? "en" : "bn";
  } catch {
    return "bn";
  }
}

// ---------- Main ----------
export default {
  async fetch(request, env) {
    if (request.method !== "POST") return new Response("OK");

    if (request.headers.get("X-Telegram-Bot-Api-Secret-Token") !== env.WEBHOOK_SECRET) {
      return new Response("Forbidden", { status: 403 });
    }

    const update = await request.json();
    const msg = update.message;
    if (!msg || !msg.text) return new Response("OK");

    // Admin only: silently ignore everyone else
    if (String(msg.from.id) !== String(env.ADMIN_ID)) return new Response("OK");

    const chatId = msg.chat.id;
    const text = msg.text.trim();
    const first = text.split(/\s+/)[0];
    const cmd = first.split("@")[0].toLowerCase();
    const args = text.slice(first.length).trim();

    let lang = "bn";
    try {
      lang = await getLang(env);
      const t = T[lang];
      let out;

      switch (cmd) {
        case "/start":
        case "/help":
          out = t.help;
          break;

        case "/client_add": {
          const [nameRaw, phoneRaw] = args.split("|").map((s) => s.trim());
          if (!nameRaw) { out = t.needName; break; }
          const rows = await db(env, "clients?select=client_no,name,phone", {
            method: "POST",
            headers: { Prefer: "return=representation" },
            body: JSON.stringify({
              name: nameRaw.slice(0, 100),
              phone: phoneRaw ? phoneRaw.slice(0, 30) : null,
            }),
          });
          out = t.added(rows[0]);
          break;
        }

        case "/client_list": {
          const rows = await db(env, "clients?select=client_no,name,phone&order=client_no.desc&limit=10");
          out = rows.length ? `${t.listTitle}\n` + rows.map(fmt).join("\n") : t.empty;
          break;
        }

        case "/client_find": {
          if (!args) { out = t.needQuery; break; }
          const q = encodeURIComponent(args.slice(0, 60));
          const rows = await db(env, `clients?name=ilike.*${q}*&select=client_no,name,phone&order=client_no.desc&limit=10`);
          out = rows.length ? `${t.foundTitle}\n` + rows.map(fmt).join("\n") : t.notFound;
          break;
        }

        case "/lang": {
          const v = args.toLowerCase();
          if (v !== "bn" && v !== "en") { out = t.langUsage; break; }
          await db(env, "bot_settings?on_conflict=key", {
            method: "POST",
            headers: { Prefer: "resolution=merge-duplicates,return=minimal" },
            body: JSON.stringify({ key: "lang", value: v }),
          });
          out = T[v].langSet;
          break;
        }

        default:
          out = t.unknown;
      }

      await reply(env, chatId, out);
    } catch (e) {
      await reply(env, chatId, T[lang].error);
    }

    return new Response("OK");
  },
};
