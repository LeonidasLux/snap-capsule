// 端到端：点「近一周」tab，断言首屏出现昨晚(5.5h前,#204)的卡，证明窗口已拓宽。
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
let seq = 0; const pend = new Map(); const exc = [];
ws.onmessage = (e) => {
  const m = JSON.parse(e.data);
  if (m.id && pend.has(m.id)) { pend.get(m.id)(m); pend.delete(m.id); }
  else if (m.method === "Runtime.exceptionThrown") exc.push(m.params.exceptionDetails?.text || "x");
};
const cmd = (method, params = {}) => new Promise((res) => { const id = ++seq; pend.set(id, res); ws.send(JSON.stringify({ id, method, params })); });
const evalJs = async (expr) => (await cmd("Runtime.evaluate", { expression: expr, returnByValue: true, awaitPromise: true })).result?.result?.value;
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
const findText = (s) => evalJs(`(() => { for (const el of document.querySelectorAll('*')) { if (el.children.length === 0 && (el.textContent||'').trim().includes(${JSON.stringify(s)})) { const r = el.getBoundingClientRect(); if (r.top >= 0) return { x: r.left + r.width/2, y: r.top + r.height/2 }; } } return null; })()`);
const click = async (x, y) => { await cmd("Input.dispatchMouseEvent", { type: "mousePressed", x, y, button: "left", clickCount: 1 }); await sleep(50); await cmd("Input.dispatchMouseEvent", { type: "mouseReleased", x, y, button: "left", clickCount: 1 }); };
await cmd("Runtime.enable"); await cmd("Page.enable");

// 若已有 50 条数据则跳过载入，否则走设置载入
const has = await evalJs(`!!localStorage['snap_capsules'] && JSON.parse(localStorage['snap_capsules']).capsules.length === 50`);
if (!has) {
  let p = null;
  for (let i = 0; i < 40 && !p; i++) { p = await findText("⚙️"); if (!p) await sleep(500); }
  await click(p.x, p.y); await sleep(800);
  p = null;
  for (let i = 0; i < 40 && !p; i++) { p = await findText("载入示例数据"); if (!p) await sleep(500); }
  await click(p.x, p.y); await sleep(1000);
  await evalJs(`location.reload()`); await sleep(2500);
}

// 点「近一周」tab（顶部文字，取最靠上的匹配）
let tab = null;
for (let i = 0; i < 40 && !tab; i++) { tab = await findText("近一周"); if (!tab) await sleep(500); }
await click(tab.x, tab.y);
await sleep(1200);

const bodyText = await evalJs(`document.body.innerText`);
const inWeekKeys = ["多肉", "灰度方案", "话剧", "爬山", "换盆"];
const shown = inWeekKeys.filter((k) => bodyText.includes(k));
console.log("首屏命中昨晚/更早卡:", shown.length ? shown.join(" / ") : "无");
console.log("含 #204(5.5h前):", bodyText.includes("多肉"));
console.log("body 前 300 字:\n", bodyText.slice(0, 300).replace(/\n+/g, " | "));
console.log("JS 异常:", exc.length ? exc.slice(0, 3) : "无");
process.exit(0);
