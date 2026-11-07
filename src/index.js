const HELP = `MR-SHADOW-DEVICE-INFO
বাংলা: আপনি অ্যাডমিন হিসেবে সংযুক্ত আছেন। কমান্ড শীঘ্রই যোগ হবে।
English: You are connected as admin. Commands will be added soon.

/start - শুরু / Start
/help - সাহায্য / Help`;

const UNKNOWN = `অজানা কমান্ড। /help দিন।
Unknown command. Send /help.`;

async function send(env, chatId, text) {
  await fetch(`https://api.telegram.org/bot${env.BOT_TOKEN}/sendMessage`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ chat_id: chatId, text }),
  });
}

export default {
  async fetch(request, env) {
    // TEMPORARY CHECK: shows only whether each secret exists (never the value)
    if (request.method !== "POST") {
      return new Response(
        `BOT_TOKEN: ${env.BOT_TOKEN ? "set" : "MISSING"}\n` +
        `ADMIN_ID: ${env.ADMIN_ID ? "set" : "MISSING"}\n` +
        `WEBHOOK_SECRET: ${env.WEBHOOK_SECRET ? "set" : "MISSING"}`
      );
    }

    if (request.headers.get("X-Telegram-Bot-Api-Secret-Token") !== env.WEBHOOK_SECRET) {
      return new Response("Forbidden", { status: 403 });
    }

    const update = await request.json();
    const msg = update.message;
    if (!msg || !msg.text) return new Response("OK");

    if (String(msg.from.id) !== String(env.ADMIN_ID)) return new Response("OK");

    const cmd = msg.text.trim().split(/\s+/)[0].split("@")[0].toLowerCase();
    const reply = cmd === "/start" || cmd === "/help" ? HELP : UNKNOWN;
    await send(env, msg.chat.id, reply);

    return new Response("OK");
  },
};
