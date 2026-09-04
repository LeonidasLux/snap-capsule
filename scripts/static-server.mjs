// 极简静态文件服务器：H5 预览用。用法: node scripts/static-server.mjs [端口] [目录]
import { createServer } from 'node:http';
import { readFile, stat } from 'node:fs/promises';
import { join, extname, normalize } from 'node:path';
import { fileURLToPath } from 'node:url';

const port = Number(process.argv[2] || 8080);
const root = normalize(join(fileURLToPath(import.meta.url), '..', '..', process.argv[3] || 'h5App/build/dist/js/productionExecutable'));

const MIME = {
  '.html': 'text/html; charset=utf-8', '.js': 'application/javascript; charset=utf-8',
  '.json': 'application/json; charset=utf-8', '.css': 'text/css; charset=utf-8',
  '.png': 'image/png', '.svg': 'image/svg+xml', '.ico': 'image/x-icon', '.map': 'application/json',
};

createServer(async (req, res) => {
  try {
    let p = decodeURIComponent((req.url || '/').split('?')[0]);
    if (p.endsWith('/')) p += 'index.html';
    const file = join(root, p);
    if (!file.startsWith(root)) { res.writeHead(403).end(); return; }
    const st = await stat(file);
    if (!st.isFile()) { res.writeHead(404).end('not found'); return; }
    const data = await readFile(file);
    res.writeHead(200, { 'Content-Type': MIME[extname(file)] || 'application/octet-stream' });
    res.end(data);
  } catch {
    res.writeHead(404).end('not found');
  }
}).listen(port, () => console.log(`static server: http://localhost:${port}/`));
