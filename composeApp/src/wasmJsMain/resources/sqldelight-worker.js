// SQLDelight Web Worker - Production Version
const SQL_JS_VERSION = "1.10.3";
const SQL_JS_BASE = `https://cdn.jsdelivr.net/npm/sql.js@${SQL_JS_VERSION}/dist`;

importScripts(`${SQL_JS_BASE}/sql-wasm.js`);

let db = null;

const dbReady = (async () => {
  try {
    const SQL = await initSqlJs({ locateFile: () => `${SQL_JS_BASE}/sql-wasm.wasm` });
    db = new SQL.Database();
  } catch (e) {
    // Silence non-critical logs in production, but keep errors for debugging
    console.error("Worker: Fatal error loading database engine", e);
  }
})();

self.onmessage = async (event) => {
  const data = event.data;
  if (!data || !data.action) return;

  try {
    await dbReady;
    switch (data.action) {
      case "exec": {
        const results = db.exec(data.sql, data.params);
        const response = results[0] ?? { values: [] };
        return self.postMessage({ id: data.id, results: response });
      }
      case "begin_transaction":
        db.exec("BEGIN TRANSACTION;");
        return self.postMessage({ id: data.id });
      case "end_transaction":
        db.exec("COMMIT;");
        return self.postMessage({ id: data.id });
      case "rollback_transaction":
        db.exec("ROLLBACK;");
        return self.postMessage({ id: data.id });
      default:
        throw new Error(`Unsupported action: ${data.action}`);
    }
  } catch (err) {
    return self.postMessage({
      id: data.id,
      error: { message: String(err.message || err) }
    });
  }
};
