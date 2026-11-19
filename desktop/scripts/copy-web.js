// Copies the shared web app (../../web) into desktop/web so Electron can package it.
const fs = require("fs");
const path = require("path");

const src = path.join(__dirname, "..", "..", "web");
const dest = path.join(__dirname, "..", "web");
fs.rmSync(dest, { recursive: true, force: true });
fs.cpSync(src, dest, { recursive: true });
console.log("Copied web app to", dest);
