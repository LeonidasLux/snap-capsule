// 验证：刚载入 50 条示例数据后，按 HomeScreen 的 tab 规则统计各视图条数。
const port = 9224;

const target = await (async () => {
  for (let i = 0; i < 60; i++) {
    try {
      const list = await (await fetch(`http://127.0.0.1:${port}/json/list`)).json();
      const p = list.find((t) => t.type === "page" && t.webSocketDebuggerUrl);
      if (p) return p;
    } catch {}
    await new Promise((r) => setTimeout(r, 500));
  }
  throw new Error("no target");
})();

const ws = new WebSocket(target.webSocketDebuggerUrl);
await new Promise((res, rej) => { ws.onopen = res; ws.onerror = rej; });
let seq = 0; const pend = new Map();
ws.onmessage = (e) => { const m = JSON.parse(e.data); if (m.id && pend.has(m.id)) { pend.get(m.id)(m); pend.delete(m.id); } };
const cmd = (method, params = {}) => new Promise((res) => { const id = ++seq; pend.set(id, res); ws.send(JSON.stringify({ id, method, params })); });
const evalJs = async (expr) => (await cmd("Runtime.evaluate", { expression: expr, returnByValue: true, awaitPromise: true })).result?.result?.value;
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
const findText = (s) => evalJs(`(() => { for (const el of document.querySelectorAll('*')) { if (el.children.length === 0 && (el.textContent||'').trim().includes(${JSON.stringify(s)})) { const r = el.getBoundingClientRect(); return { x: r.left + r.width/2, y: r.top + r.height/2 }; } } return null; })()`);
const click = async (x, y) => { await cmd("Input.dispatchMouseEvent", { type: "mousePressed", x, y, button: "left", clickCount: 1 }); await sleep(50); await cmd("Input.dispatchMouseEvent", { type: "mouseReleased", x, y, button: "left", clickCount: 1 }); };

await cmd("Runtime.enable"); await cmd("Page.enable");

// 设置 → 载入示例数据
let p = null;
for (let i = 0; i < 40 && !p; i++) { p = await findText("⚙️"); if (!p) await sleep(500); }
await click(p.x, p.y); await sleep(800);
p = null;
for (let i = 0; i < 40 && !p; i++) { p = await findText("载入示例数据"); if (!p) await sleep(500); }
await click(p.x, p.y); await sleep(1200);

// 读取数据，并在 Node 侧复刻 TimeText 的窗口规则
const raw = await evalJs(`localStorage['snap_capsules']`);
const { capsules: cs } = JSON.parse(raw);

const now = Date.now();
const todayMid = new Date(now); todayMid.setHours(0, 0, 0, 0);
const weekMid = todayMid.getTime() - 6 * 86400000; // startOfDay(now - 6d) ≈ 今天往前第6天 00:00
const loadAgeMs = now - Math.max(...cs.map((c) => c.createdAt)); // 数据生成于多久前

const active = cs.filter((c) => c.status === "active");
const count = (pred) => active.filter(pred).length;
const res = {
  数据距今: `${(loadAgeMs / 1000).toFixed(0)}s`,
  todayMid: new Date(todayMid.getTime()).toISOString(),
  今天: count((c) => c.createdAt >= todayMid.getTime()),
  近一周_含今天: count((c) => c.createdAt >= weekMid),
  全部_active: active.length,
  归档: cs.length - active.length,
};
// 近一周窗口内逐条
const inWeek = active.filter((c) => c.createdAt >= weekMid).sort((a, b) => b.createdAt - a.createdAt);
const ago = (t) => { const h = (now - t) / 3600000; return h < 1 ? `${Math.round(h * 60)}min` : `${h.toFixed(1)}h`; };
console.log(JSON.stringify(res, null, 2));
console.log("近一周内逐条:", inWeek.map((c) => `#${c.id} ${ago(c.createdAt)}前`).join("  "));
process.exit(0);
