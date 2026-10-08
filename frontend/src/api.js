let csrf;
export async function api(path, options = {}) {
  const method = options.method || "GET";
  if (method !== "GET") {
    const r = await fetch("/api/auth/csrf", { credentials: "same-origin" });
    if (!r.ok)
      throw new Error("Não foi possível iniciar a sessão de segurança.");
    csrf = (await r.json()).token;
  }
  const res = await fetch("/api" + path, {
    ...options,
    method,
    credentials: "same-origin",
    headers: {
      ...(options.body ? { "Content-Type": "application/json" } : {}),
      ...(method !== "GET" ? { "X-XSRF-TOKEN": csrf } : {}),
      ...options.headers,
    },
    body: options.body ? JSON.stringify(options.body) : undefined,
  });
  if (!res.ok) {
    let data;
    try {
      data = await res.json();
    } catch {
      data = { message: "Não foi possível acessar o servidor." };
    }
    const e = new Error(data.message || "Operação recusada.");
    e.status = res.status;
    if (res.status === 403) csrf = undefined;
    throw e;
  }
  if (path === "/auth/login" || path === "/auth/logout") csrf = undefined;
  const text = await res.text();
  return text ? JSON.parse(text) : null;
}
export const send = (path, body, method = "POST") =>
  api(path, { method, body });
