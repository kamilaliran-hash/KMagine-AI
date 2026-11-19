const { contextBridge, ipcRenderer } = require("electron");

contextBridge.exposeInMainWorld("nativeBridge", {
  platform: "desktop",
  post: (req) => ipcRenderer.invoke("http-post", req),
  getDataUrl: (url) => ipcRenderer.invoke("get-data-url", url),
});
