const L = {
  bn: { title: "ℹ️ ডেভেলপার পরিচিতি", app: "অ্যাপ", name: "নাম", contact: "যোগাযোগ", notSet: "(সেট করা হয়নি)" },
  en: { title: "ℹ️ About the developer", app: "App", name: "Name", contact: "Contact", notSet: "(not set)" },
};

export async function sendAbout(env, chatId, lang) {
  const s = L[lang] || L.bn;
  const caption = [
    s.title,
    "",
    `${s.app}: MR-SHADOW-DEVICE-INFO`,
    `${s.name}: ${env.ABOUT_NAME || s.notSet}`,
    `${s.contact}: ${env.ABOUT_CONTACT || s.notSet}`,
  ].join("\n");

  const api = `https://api.telegram.org/bot${env.BOT_TOKEN}`;
  const post = (method, body) =>
    fetch(`${api}/${method}`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body),
    });

  // Try with photo first; if the photo link fails, fall back to text only
  if (env.ABOUT_PHOTO_URL) {
    const res = await post("sendPhoto", { chat_id: chatId, photo: env.ABOUT_PHOTO_URL, caption });
    if (res.ok) return;
  }
  await post("sendMessage", { chat_id: chatId, text: caption });
}
