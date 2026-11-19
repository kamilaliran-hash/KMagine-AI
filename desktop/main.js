const { app, BrowserWindow, ipcMain, shell, Menu, protocol, net } = require("electron");
const path = require("path");
const { pathToFileURL } = require("url");

// Serve the app from app://imagine/ (a proper secure web address) instead of file://,
// so online services like Hugging Face and Puter accept requests from it.
protocol.registerSchemesAsPrivileged([
  { scheme: "app", privileges: { standard: true, secure: true, supportFetchAPI: true, corsEnabled: true, stream: true } },
]);
const WEB_ROOT = path.join(__dirname, "web");

function createWindow() {
  const win = new BrowserWindow({
    width: 1200,
    height: 860,
    minWidth: 420,
    minHeight: 600,
    backgroundColor: "#0a0a0b",
    title: "KMagine AI",
    autoHideMenuBar: true,
    webPreferences: {
      preload: path.join(__dirname, "preload.js"),
      contextIsolation: true,
      nodeIntegration: false,
    },
  });
  Menu.setApplicationMenu(null);
  win.loadURL("app://imagine/index.html");

  // Puter's sign-in opens a pop-up that must stay connected to the app;
  // every other link opens in the normal browser.
  win.webContents.setWindowOpenHandler(({ url }) => {
    let host = "";
    try { host = new URL(url).hostname; } catch (e) {}
    if (url === "about:blank" || host === "puter.com" || host.endsWith(".puter.com") || host.endsWith(".puter.site")) {
      return {
        action: "allow",
        overrideBrowserWindowOptions: { width: 520, height: 720, autoHideMenuBar: true, parent: win },
      };
    }
    if (/^https?:/.test(url)) shell.openExternal(url);
    return { action: "deny" };
  });
}

// Network calls run here (Node), so xAI isn't blocked by browser CORS rules.
ipcMain.handle("http-post", async (_e, { url, headers, body }) => {
  if (!/^https:\/\/api\.x\.ai\//.test(url)) throw new Error("Blocked URL");
  const r = await fetch(url, { method: "POST", headers, body });
  return { status: r.status, text: await r.text() };
});

ipcMain.handle("get-data-url", async (_e, url) => {
  if (!/^https:\/\//.test(url)) throw new Error("Blocked URL");
  const r = await fetch(url);
  if (!r.ok) throw new Error("HTTP " + r.status);
  const type = r.headers.get("content-type") || "image/jpeg";
  const buf = Buffer.from(await r.arrayBuffer());
  return "data:" + type + ";base64," + buf.toString("base64");
});

app.whenReady().then(() => {
  protocol.handle("app", (request) => {
    const rel = decodeURIComponent(new URL(request.url).pathname).replace(/^\/+/, "") || "index.html";
    const file = path.normalize(path.join(WEB_ROOT, rel));
    if (!file.startsWith(WEB_ROOT)) return new Response("Not found", { status: 404 });
    return net.fetch(pathToFileURL(file).toString());
  });
  createWindow();
  app.on("activate", () => {
    if (BrowserWindow.getAllWindows().length === 0) createWindow();
  });
});

app.on("window-all-closed", () => {
  if (process.platform !== "darwin") app.quit();
});
