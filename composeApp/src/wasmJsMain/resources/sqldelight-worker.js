// SQLDelight Web Worker — Protocolo de diagnóstico y secuenciación corregido
const SQL_JS_VERSION = "1.10.3";
const SQL_JS_BASE = `https://cdn.jsdelivr.net/npm/sql.js@${SQL_JS_VERSION}/dist`;

console.log("Worker: Iniciando carga de SQL.js...");
importScripts(`${SQL_JS_BASE}/sql-wasm.js`);

let db = null;

const dbReady = (async () => {
  try {
    const SQL = await initSqlJs({ locateFile: () => `${SQL_JS_BASE}/sql-wasm.wasm` });
    db = new SQL.Database();
    console.log("Worker: SQL.js cargado y base de datos inicializada.");
  } catch (e) {
    console.error("Worker: ERROR inicializando SQL.js:", e);
  }
})();

self.onmessage = async (event) => {
  const data = event.data;
  if (!data || !data.action) return;

  try {
    // Bloqueante: Aseguramos que SQL.js esté listo antes de procesar CUALQUIER mensaje
    await dbReady;

    switch (data.action) {
      case "exec": {
        if (!data.sql) throw new Error("exec: Missing SQL");
        const results = db.exec(data.sql, data.params);

        // Log para trazar creación
        if (data.sql.includes("CREATE TABLE")) {
           const match = data.sql.match(/CREATE TABLE IF NOT EXISTS (\w+)/i);
           if (match) console.log(`Worker: Ejecutado CREATE TABLE -> ${match[1]}`);
        }

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
    console.error(`Worker: Error en acción [${data.action}]:`, err);
    return self.postMessage({
      id: data.id,
      error: { message: String(err.message || err) }
    });
  }
};
