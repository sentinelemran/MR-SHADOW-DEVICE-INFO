// ---------- Helpers: formatting ----------
const bnDigits = "০১২৩৪৫৬৭৮৯";
const norm = (s) => String(s).replace(/[০-৯]/g, (d) => bnDigits.indexOf(d));
const toNum = (s) => {
  if (s == null || s === "") return null;
  const n = Number(norm(s).replace(/,/g, ""));
  return Number.isFinite(n) ? n : null;
};
const toInt = (s) => {
  const n = toNum(s);
  return n != null && Number.isInteger(n) && n > 0 ? n : null;
};
const val = (s) => (!s || s === "-" ? null : s.slice(0, 100));
const money = (n) => "৳" + Number(n);
const round2 = (n) => Math.round(n * 100) / 100;
const todayDhaka = () => new Date(Date.now() + 6 * 3600 * 1000).toISOString().slice(0, 10);

const fmt = (c) => `#${c.client_no} ${c.name}${c.phone ? " · " + c.phone : ""}`;

const JOB_SEL =
  "job_no,phone_brand,phone_model,status,job_date,total_amount,paid_amount,due_amount,clients(client_no,name)";
const JOB_FULL =
  "job_no,phone_brand,phone_model,android_version,bootloader_status,imei,root_type,job_date,total_amount,paid_amount,due_amount,status,notes,clients(client_no,name,phone)";

const phoneName = (j) => [j.phone_brand, j.phone_model].filter(Boolean).join(" ") || "-";

const jobLine = (j, t) =>
  `#${j.job_no} · ${j.clients ? j.clients.name : "?"} · ${phoneName(j)} · ${t.st[j.status]} · ${j.job_date} · ${t.lbl.due} ${money(j.due_amount)}`;

const detail = (j, t) => {
  const L = t.lbl;
  const c = j.clients || {};
  return [
    `🔧 ${L.job} #${j.job_no}`,
    `${L.client}: #${c.client_no} ${c.name}${c.phone ? " · " + c.phone : ""}`,
    `${L.phone}: ${phoneName(j)}`,
    `Android: ${j.android_version || "-"}`,
    `${L.boot}: ${j.bootloader_status || "-"}`,
    `${L.root}: ${j.root_type || "-"}`,
    `IMEI: ${j.imei || "-"}`,
    `${L.date}: ${j.job_date}`,
    `${L.status}: ${t.st[j.status]}`,
    `${L.total}: ${money(j.total_amount)} | ${L.paid}: ${money(j.paid_amount)} | ${L.due}: ${money(j.due_amount)}`,
    j.notes ? `${L.notes}: ${j.notes}` : null,
  ]
    .filter(Boolean)
    .join("\n");
};

const statsText = (t, d) => {
  const S = t.sl;
  const L = t.lbl;
  return [
    S.title,
    "",
    `${S.clients}: ${d.clients}`,
    `${S.jobs}: ${d.jobs} (${S.today} ${d.today}, ${S.month} ${d.month})`,
    "",
    `${t.st.pending}: ${d.s.pending}`,
    `${t.st.in_progress}: ${d.s.in_progress}`,
    `${t.st.done}: ${d.s.done}`,
    `${t.st.cancelled}: ${d.s.cancelled}`,
    "",
    S.money,
    `${L.total}: ${money(d.total)} | ${L.paid}: ${money(d.paid)} | ${L.due}: ${money(round2(d.total - d.paid))}`,
    `${S.monthJobs}: ${L.total} ${money(d.mTotal)} | ${L.paid} ${money(d.mPaid)}`,
    "",
    `${S.topDue}:`,
    ...(d.top.length ? d.top.map((j) => jobLine(j, t)) : [S.none]),
  ].join("\n");
};

// ---------- Messages (Bengali / English) ----------
const T = {
  bn: {
    help: `MR-SHADOW-DEVICE-INFO — অ্যাডমিন প্যানেল

👤 ক্লায়েন্ট
/client_add নাম | ফোন
/client_list
/client_find নাম

🔧 কাজ
/job_new ক্লায়েন্ট# | ব্র্যান্ড | মডেল | Android | বুটলোডার | রুটের ধরন | মোট টাকা | IMEI | নোট
/job_list অথবা /job_list ক্লায়েন্ট#
/job_show কাজ#
/job_pay কাজ# | টাকা
/job_status কাজ# | pending/progress/done/cancel
/job_done কাজ#

📊 /stats

🌐 /lang bn অথবা /lang en

জানা না থাকলে ঘরে - দিন। IMEI ও নোট বাদ দেওয়া যায়।`,
    unknown: "অজানা কমান্ড। /help দিন।",
    error: "কিছু একটা সমস্যা হয়েছে। একটু পরে আবার চেষ্টা করুন।",
    needName: "নাম দিন। উদাহরণ:\n/client_add রহিম উদ্দিন | 01XXXXXXXXX",
    added: (c) => `✅ ক্লায়েন্ট যোগ হয়েছে:\n${fmt(c)}`,
    empty: "এখনো কোনো ক্লায়েন্ট নেই।",
    listTitle: "সর্বশেষ ক্লায়েন্ট:",
    needQuery: "খুঁজতে নাম লিখুন। উদাহরণ:\n/client_find রহিম",
    notFound: "কাউকে পাওয়া যায়নি।",
    foundTitle: "পাওয়া গেছে:",
    langSet: "ভাষা: বাংলা ✅",
    langUsage: "ব্যবহার: /lang bn অথবা /lang en",
    jobUsage:
      "ব্যবহার:\n/job_new ক্লায়েন্ট# | ব্র্যান্ড | মডেল | Android | বুটলোডার | রুটের ধরন | মোট টাকা | IMEI | নোট\n\nউদাহরণ:\n/job_new 1 | Samsung | A52 | 13 | unlocked | Magisk | 1500\n\nজানা না থাকলে ঘরে - দিন। IMEI ও নোট বাদ দেওয়া যায়।",
    noClient: "এই নম্বরের ক্লায়েন্ট নেই। /client_list দেখুন।",
    noJob: "এই নম্বরের কাজ নেই। /job_list দেখুন।",
    jobAdded: "✅ কাজ যোগ হয়েছে:",
    noJobs: "কোনো কাজ নেই।",
    jobListTitle: "সর্বশেষ কাজ:",
    needJobNo: "কাজের নম্বর দিন। উদাহরণ:\n/job_show 5",
    payUsage: "ব্যবহার: /job_pay কাজ# | টাকা\nউদাহরণ: /job_pay 5 | 500",
    overpay: (due) => `মোট টাকার চেয়ে বেশি হয়ে যাচ্ছে। বাকি আছে ${money(due)}।`,
    paid: (j) =>
      `✅ কাজ #${j.job_no}: পরিশোধ ${money(j.paid_amount)}, বাকি ${money(j.due_amount)}`,
    statusUsage:
      "ব্যবহার: /job_status কাজ# | অবস্থা\nঅবস্থা: pending, progress, done, cancel\nউদাহরণ: /job_status 5 | progress",
    statusSet: (j, t) => `✅ কাজ #${j.job_no}: ${t.st[j.status]}`,
    doneUsage: "ব্যবহার: /job_done কাজ#\nউদাহরণ: /job_done 5",
    st: { pending: "⏳ অপেক্ষমাণ", in_progress: "🛠 চলছে", done: "✅ সম্পন্ন", cancelled: "❌ বাতিল" },
    lbl: {
      job: "কাজ",
      client: "ক্লায়েন্ট",
      phone: "ফোন",
      boot: "বুটলোডার",
      root: "রুটের ধরন",
      date: "তারিখ",
      status: "অবস্থা",
      total: "মোট",
      paid: "পরিশোধ",
      due: "বাকি",
      notes: "নোট",
    },
    sl: {
      title: "📊 পরিসংখ্যান",
      clients: "👥 ক্লায়েন্ট",
      jobs: "🔧 মোট কাজ",
      today: "আজ",
      month: "এই মাসে",
      money: "💰 টাকা (বাতিল বাদে)",
      monthJobs: "এই মাসের কাজ",
      topDue: "সবচেয়ে বেশি বাকি",
      none: "নেই",
    },
  },
  en: {
    help: `MR-SHADOW-DEVICE-INFO — Admin panel

👤 Clients
/client_add name | phone
/client_list
/client_find name

🔧 Jobs
/job_new client# | brand | model | Android | bootloader | root type | total | IMEI | notes
/job_list or /job_list client#
/job_show job#
/job_pay job# | amount
/job_status job# | pending/progress/done/cancel
/job_done job#

📊 /stats

🌐 /lang bn or /lang en

Use - for unknown fields. IMEI and notes can be left out.`,
    unknown: "Unknown command. Send /help.",
    error: "Something went wrong. Please try again shortly.",
    needName: "Please give a name. Example:\n/client_add Rahim Uddin | 01XXXXXXXXX",
    added: (c) => `✅ Client added:\n${fmt(c)}`,
    empty: "No clients yet.",
    listTitle: "Latest clients:",
    needQuery: "Type a name to search. Example:\n/client_find Rahim",
    notFound: "No match found.",
    foundTitle: "Found:",
    langSet: "Language: English ✅",
    langUsage: "Usage: /lang bn or /lang en",
    jobUsage:
      "Usage:\n/job_new client# | brand | model | Android | bootloader | root type | total | IMEI | notes\n\nExample:\n/job_new 1 | Samsung | A52 | 13 | unlocked | Magisk | 1500\n\nUse - for unknown fields. IMEI and notes can be left out.",
    noClient: "No client with that number. See /client_list.",
    noJob: "No job with that number. See /job_list.",
    jobAdded: "✅ Job added:",
    noJobs: "No jobs yet.",
    jobListTitle: "Latest jobs:",
    needJobNo: "Give a job number. Example:\n/job_show 5",
    payUsage: "Usage: /job_pay job# | amount\nExample: /job_pay 5 | 500",
    overpay: (due) => `That is more than the total. Due is ${money(due)}.`,
    paid: (j) => `✅ Job #${j.job_no}: paid ${money(j.paid_amount)}, due ${money(j.due_amount)}`,
    statusUsage:
      "Usage: /job_status job# | status\nStatus: pending, progress, done, cancel\nExample: /job_status 5 | progress",
    statusSet: (j, t) => `✅ Job #${j.job_no}: ${t.st[j.status]}`,
    doneUsage: "Usage: /job_done job#\nExample: /job_done 5",
    st: { pending: "⏳ Pending", in_progress: "🛠 In progress", done: "✅ Done", cancelled: "❌ Cancelled" },
    lbl: {
      job: "Job",
      client: "Client",
      phone: "Phone",
      boot: "Bootloader",
      root: "Root type",
      date: "Date",
      status: "Status",
      total: "Total",
      paid: "Paid",
      due: "Due",
      notes: "Notes",
    },
    sl: {
      title: "📊 Statistics",
      clients: "👥 Clients",
      jobs: "🔧 Total jobs",
      today: "today",
      month: "this month",
      money: "💰 Money (excluding cancelled)",
      monthJobs: "This month's jobs",
      topDue: "Highest dues",
      none: "none",
    },
  },
};

const STATUS_MAP = {
  pending: "pending",
  progress: "in_progress",
  in_progress: "in_progress",
  done: "done",
  cancel: "cancelled",
  cancelled: "cancelled",
};

// ---------- Helpers: Telegram and database ----------
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

async function dbCount(env, path) {
  const key = env.SUPABASE_KEY;
  const headers = { apikey: key, Prefer: "count=exact", Range: "0-0" };
  if (key.startsWith("eyJ")) headers.Authorization = `Bearer ${key}`;
  const base = env.SUPABASE_URL.replace(/\/$/, "");
  const res = await fetch(`${base}/rest/v1/${path}`, { headers });
  if (res.status === 416) return 0;
  if (!res.ok) throw new Error(`DB ${res.status}`);
  const total = Number((res.headers.get("content-range") || "").split("/")[1]);
  return Number.isFinite(total) ? total : 0;
}

async function allJobs(env) {
  const all = [];
  for (let page = 0; page < 5; page++) {
    const rows = await db(
      env,
      `jobs?select=status,total_amount,paid_amount,job_date&order=job_no.asc&limit=1000&offset=${page * 1000}`
    );
    all.push(...rows);
    if (rows.length < 1000) break;
  }
  return all;
}

async function getLang(env) {
  try {
    const rows = await db(env, "bot_settings?key=eq.lang&select=value");
    return rows && rows[0] && rows[0].value === "en" ? "en" : "bn";
  } catch {
    return "bn";
  }
}

async function getJob(env, n) {
  const rows = await db(env, `jobs?job_no=eq.${n}&select=${JOB_FULL}`);
  return rows && rows[0] ? rows[0] : null;
}

async function setStatus(env, n, status) {
  const rows = await db(env, `jobs?job_no=eq.${n}&select=job_no,status`, {
    method: "PATCH",
    headers: { Prefer: "return=representation" },
    body: JSON.stringify({ status }),
  });
  return rows && rows[0] ? rows[0] : null;
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

        // ----- Clients -----
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

        // ----- Jobs -----
        case "/job_new": {
          const f = args.split("|").map((s) => s.trim());
          const clientNo = toInt(f[0]);
          const total = toNum(f[6]);
          if (f.length < 7 || clientNo == null || total == null || total < 0 || total > 1e9) {
            out = t.jobUsage;
            break;
          }
          const cl = await db(env, `clients?client_no=eq.${clientNo}&select=id`);
          if (!cl.length) { out = t.noClient; break; }
          const ins = await db(env, "jobs?select=job_no", {
            method: "POST",
            headers: { Prefer: "return=representation" },
            body: JSON.stringify({
              client_id: cl[0].id,
              phone_brand: val(f[1]),
              phone_model: val(f[2]),
              android_version: val(f[3]),
              bootloader_status: val(f[4]),
              root_type: val(f[5]),
              total_amount: total,
              imei: val(f[7]),
              notes: f[8] && f[8] !== "-" ? f[8].slice(0, 300) : null,
              job_date: todayDhaka(),
            }),
          });
          const job = await getJob(env, ins[0].job_no);
          out = `${t.jobAdded}\n${detail(job, t)}`;
          break;
        }

        case "/job_list": {
          let path = `jobs?select=${JOB_SEL}&order=job_no.desc&limit=10`;
          if (args) {
            const cn = toInt(args);
            if (cn == null) { out = t.jobUsage; break; }
            path = `jobs?select=${JOB_SEL.replace("clients(", "clients!inner(")}&clients.client_no=eq.${cn}&order=job_no.desc&limit=10`;
          }
          const rows = await db(env, path);
          out = rows.length ? `${t.jobListTitle}\n` + rows.map((j) => jobLine(j, t)).join("\n") : t.noJobs;
          break;
        }

        case "/job_show": {
          const n = toInt(args);
          if (n == null) { out = t.needJobNo; break; }
          const job = await getJob(env, n);
          out = job ? detail(job, t) : t.noJob;
          break;
        }

        case "/job_pay": {
          const [no, amt] = args.split("|").map((s) => s.trim());
          const n = toInt(no);
          const a = toNum(amt);
          if (n == null || a == null || a <= 0) { out = t.payUsage; break; }
          const rows = await db(env, `jobs?job_no=eq.${n}&select=total_amount,paid_amount,due_amount`);
          if (!rows.length) { out = t.noJob; break; }
          const newPaid = round2(Number(rows[0].paid_amount) + a);
          if (newPaid > Number(rows[0].total_amount)) { out = t.overpay(rows[0].due_amount); break; }
          const upd = await db(env, `jobs?job_no=eq.${n}&select=job_no,paid_amount,due_amount`, {
            method: "PATCH",
            headers: { Prefer: "return=representation" },
            body: JSON.stringify({ paid_amount: newPaid }),
          });
          out = t.paid(upd[0]);
          break;
        }

        case "/job_status": {
          const [no, st] = args.split("|").map((s) => s.trim());
          const n = toInt(no);
          const status = st ? STATUS_MAP[st.toLowerCase()] : null;
          if (n == null || !status) { out = t.statusUsage; break; }
          const j = await setStatus(env, n, status);
          out = j ? t.statusSet(j, t) : t.noJob;
          break;
        }

        case "/job_done": {
          const n = toInt(args);
          if (n == null) { out = t.doneUsage; break; }
          const j = await setStatus(env, n, "done");
          out = j ? t.statusSet(j, t) : t.noJob;
          break;
        }

        // ----- Stats -----
        case "/stats": {
          const [clients, jobs, top] = await Promise.all([
            dbCount(env, "clients?select=id"),
            allJobs(env),
            db(env, `jobs?select=${JOB_SEL}&status=neq.cancelled&due_amount=gt.0&order=due_amount.desc&limit=5`),
          ]);
          const today = todayDhaka();
          const month = today.slice(0, 7);
          const d = {
            clients, top, jobs: jobs.length,
            s: { pending: 0, in_progress: 0, done: 0, cancelled: 0 },
            today: 0, month: 0, total: 0, paid: 0, mTotal: 0, mPaid: 0,
          };
          for (const j of jobs) {
            if (d.s[j.status] !== undefined) d.s[j.status]++;
            if (j.job_date === today) d.today++;
            const inMonth = j.job_date.startsWith(month);
            if (inMonth) d.month++;
            if (j.status === "cancelled") continue;
            d.total += Number(j.total_amount);
            d.paid += Number(j.paid_amount);
            if (inMonth) {
              d.mTotal += Number(j.total_amount);
              d.mPaid += Number(j.paid_amount);
            }
          }
          d.total = round2(d.total);
          d.paid = round2(d.paid);
          d.mTotal = round2(d.mTotal);
          d.mPaid = round2(d.mPaid);
          out = statsText(t, d);
          break;
        }

        // ----- Language -----
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
