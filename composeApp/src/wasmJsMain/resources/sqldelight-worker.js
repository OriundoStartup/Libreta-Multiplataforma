// SQLDelight Web Worker — protocolo compatible con app.cash.sqldelight:web-worker-driver 2.3.x
// Carga sql.js desde jsDelivr (sirve con CORP cross-origin, satisface COEP require-corp).
// Persistencia: en memoria por sesión (igual que el comportamiento previo del DataSeeder).

const SQL_JS_VERSION = "1.10.3";
const SQL_JS_BASE = `https://cdn.jsdelivr.net/npm/sql.js@${SQL_JS_VERSION}/dist`;

importScripts(`${SQL_JS_BASE}/sql-wasm.js`);

let db = null;

const dbReady = (async () => {
  const SQL = await initSqlJs({ locateFile: () => `${SQL_JS_BASE}/sql-wasm.wasm` });
  db = new SQL.Database();
})();

self.onmessage = async (event) => {
  const data = event.data;
  try {
    await dbReady;
    switch (data && data.action) {
      case "exec": {
        if (!data.sql) throw new Error("exec: Missing query string");
        const results = db.exec(data.sql, data.params)[0] ?? { values: [] };
        return self.postMessage({ id: data.id, results });
      }
      case "begin_transaction":
        return self.postMessage({ id: data.id, results: db.exec("BEGIN TRANSACTION;") });
      case "end_transaction":
        return self.postMessage({ id: data.id, results: db.exec("END TRANSACTION;") });
      case "rollback_transaction":
        return self.postMessage({ id: data.id, results: db.exec("ROLLBACK TRANSACTION;") });
      default:
        throw new Error(`Unsupported action: ${data && data.action}`);
    }
  } catch (err) {
    return self.postMessage({
      id: data && data.id,
      error: { message: String((err && err.message) || err) }
    });
  }
};
