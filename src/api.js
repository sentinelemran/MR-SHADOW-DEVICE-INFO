const JSON_HEADERS = { "Content-Type": "application/json; charset=utf-8" };
const reply = (obj, status = 200) =>
  new Response(JSON.stringify(obj), { status, headers: JSON_HEADERS });

const bnDigits = "০১২৩৪৫৬৭৮৯";
const digits = (s) =>
  String(s ?? "")
    .replace(/[০-৯]/g, (d) => bnDigits.indexOf(d))
    .replace(/\D/g, "");
const last10 = (s) => {
  const d = digits(s);
  return d.length >= 10 ? d.slice(-10) : null;
};

export async function handleApi(request, env, url) {
  if (url.pathname !== "/api/status") return reply({ ok: false, error: "not_found" }, 404);
  if (request.method !== "POST") return reply({ ok: false, error: "method" }, 405);

  let body;
  try {
    body = await request.json();
  } catch {
    return reply({ ok: false, error: "invalid" }, 400);
  }

  const jobNo = Number(digits(body.job_no));
  const phone = last10(body.phone);
  if (!Number.isInteger(jobNo) || jobNo < 1 || jobNo > 1e9 || !phone) {
    return reply({ ok: false, error: "invalid" }, 400);
  }

  const key = env.SUPABASE_KEY;
  const headers = { apikey: key };
  if (key.startsWith("eyJ")) headers.Authorization = `Bearer ${key}`;
  const base = env.SUPABASE_URL.replace(/\/$/, "");

  let rows;
  try {
    const res = await fetch(
      `${base}/rest/v1/jobs?job_no=eq.${jobNo}&select=job_no,phone_brand,phone_model,status,job_date,total_amount,paid_amount,due_amount,clients(phone)`,
      { headers }
    );
    if (!res.ok) throw new Error("db");
    rows = await res.json();
  } catch {
    return reply({ ok: false, error: "server" }, 500);
  }

  // The phone number must match the client's number on file
  const j = rows[0];
  const clientPhone = j && j.clients ? last10(j.clients.phone) : null;
  if (!j || !clientPhone || clientPhone !== phone) {
    return reply({ ok: false, error: "not_found" }, 404);
  }

  return reply({
    ok: true,
    job: {
      job_no: j.job_no,
      device: [j.phone_brand, j.phone_model].filter(Boolean).join(" ") || "-",
      status: j.status,
      date: j.job_date,
      total: Number(j.total_amount),
      paid: Number(j.paid_amount),
      due: Number(j.due_amount),
    },
  });
}
