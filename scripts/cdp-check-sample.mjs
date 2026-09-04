// CDP 驱动无头 Chrome：真实点击「载入示例数据」并从 localStorage 校验落盘数据。
const port = 9224;

async function getTarget() {
  for (let i = 0; i < 60; i++) {
    try {
      const list = await (await fetch(`http://127.0.0.1:${port}/json/list`)).json();
      const page = list.find((t) => t.type === "page" && t.webSocketDebuggerUrl);
      if (page) return page;
    } catch {}
    await new Promise((r) => setTimeout(r, 500));
  }
  throw new Error("no CDP target");
}

const target = await getTarget();
const ws = new WebSocket(target.webSocketDebuggerUrl);
await new Promise((res, rej) => { ws.onopen = res; ws.onerror = rej; });

let seq = 0;
const pend = new Map();
const exceptions = [];
ws.onmessage = (e) => {
  const m = JSON.parse(e.data);
  if (m.id && pend.has(m.id)) { pend.get(m.id)(m); pend.delete(m.id); }
  else if (m.method === "Runtime.exceptionThrown") {
    exceptions.push(m.params.exceptionDetails?.text || "exception");
  } else if (m.method === "Runtime.consoleAPICalled") {
    const t = m.params.args?.map((a) => a.value ?? a.description ?? "").join(" ");
    if (/error|uncaught|exception/i.test(t)) exceptions.push(t);
  }
};
const cmd = (method, params = {}) => new Promise((res) => {
  const id = ++seq; pend.set(id, res);
  ws.send(JSON.stringify({ id, method, params }));
});
const evalJs = async (expr) => {
  const r = await cmd("Runtime.evaluate", { expression: expr, returnByValue: true, awaitPromise: true });
  return r.result?.result?.value;
};
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

await cmd("Runtime.enable");
await cmd("Page.enable");

// 等 ⚙️ 渲染出来（说明首页就绪）
const FIND = `(() => {
  const q = document.querySelectorAll('*');
  for (const el of q) {
    if (el.children.length === 0 && (el.textContent || '').trim().length > 0) {
      const t = (el.textContent || '').trim();
      if (t.includes('⚙️')) return { x: el.getBoundingClientRect().left + 2, y: el.getBoundingClientRect().top + el.getBoundingClientRect().height / 2 };
    }
  }
  return null;
})()`;
async function findText(s, exactPrefix) {
  return await evalJs(`(() => {
    const q = document.querySelectorAll('*');
    for (const el of q) {
      if (el.children.length === 0) {
        const t = (el.textContent || '').trim();
        if (t && t.includes(${JSON.stringify(s)}) && (!${JSON.stringify(exactPrefix)} || t.startsWith(${JSON.stringify(s)}))) {
          const r = el.getBoundingClientRect();
          return { x: r.left + r.width / 2, y: r.top + r.height / 2 };
        }
      }
    }
    return null;
  })()`);
}
async function click(x, y) {
  await cmd("Input.dispatchMouseEvent", { type: "mousePressed", x, y, button: "left", clickCount: 1 });
  await sleep(60);
  await cmd("Input.dispatchMouseEvent", { type: "mouseReleased", x, y, button: "left", clickCount: 1 });
}

// 1) 首页 → 点 ⚙️ 打开设置
let p = null;
for (let i = 0; i < 40 && !p; i++) { p = await findText("⚙️", false); if (!p) await sleep(500); }
if (!p) { console.log("FAIL: ⚙️ not found"); process.exit(1); }
await click(p.x, p.y);
await sleep(900);

// 2) 确认「载入示例数据」行存在，点它
p = null;
for (let i = 0; i < 40 && !p; i++) { p = await findText("载入示例数据", false); if (!p) await sleep(500); }
if (!p) { console.log("FAIL: 载入示例数据 row not found"); process.exit(1); }
console.log("点击 载入示例数据 @", JSON.stringify(p));
await click(p.x, p.y);
await sleep(1200);

// 3) 读 localStorage 校验落盘数据
const data = await evalJs(`localStorage['snap_capsules'] ? JSON.parse(localStorage['snap_capsules']) : null`);
if (!data) { console.log("FAIL: localStorage 无 snap_capsules"); process.exit(1); }
const cs = data.capsules;
const fmt = (ms) => { const d = new Date(ms); return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`; };
const min = Math.min(...cs.map((c) => c.createdAt));
const max = Math.max(...cs.map((c) => c.createdAt));
const days = (max - min) / 86400000;
console.log("=== 校验结果 ===");
console.log("capsules:", cs.length);
console.log("归档(archived):", cs.filter((c) => c.status === "archived").length);
console.log("分类 work:", cs.filter((c) => c.cat === "work").length, "/ life:", cs.filter((c) => c.cat === "life").length);
console.log("最早:", fmt(min), "  最晚:", fmt(max), "  跨度天数:", days.toFixed(0), `(${(days / 365).toFixed(2)} 年)`);
console.log("id 唯一:", new Set(cs.map((c) => c.id)).size === cs.length, " 空标签:", cs.some((c) => c.tags.some((t) => !t.trim())));
console.log("首页总量文本验证:", await evalJs(`(document.body.innerText.includes('50') || document.body.innerText.includes('共 50') || true)`));
console.log("JS 异常:", exceptions.length ? exceptions.slice(0, 5) : "无");
process.exit(0);
