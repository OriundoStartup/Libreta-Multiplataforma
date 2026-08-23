// SQLDelight Web Worker — Protocolo de diagnóstico avanzado
const SQL_JS_VERSION = "1.10.3";
const SQL_JS_BASE = `https://cdn.jsdelivr.net/npm/sql.js@${SQL_JS_VERSION}/dist`;

console.log("Worker: Iniciando carga de scripts...");
importScripts(`${SQL_JS_BASE}/sql-wasm.js`);

let db = null;

const dbReady = (async () => {
  try {
    const SQL = await initSqlJs({ locateFile: () => `${SQL_JS_BASE}/sql-wasm.wasm` });
    db = new SQL.Database();
    console.log("Worker: SQL.js cargado y base de datos en memoria lista.");
  } catch (e) {
    console.error("Worker: ERROR CRÍTICO inicializando SQL.js:", e);
  }
})();

self.onmessage = async (event) => {
  const data = event.data;
  try {
    await dbReady;
    if (!data || !data.action) return;

    switch (data.action) {
      case "exec": {
        if (!data.sql) throw new Error("exec: Missing SQL query");

        // Log de ejecución
        if (data.sql.includes("CREATE TABLE")) {
           console.log(`Worker: Ejecutando DDL -> ${data.sql.substring(0, 100)}...`);
        }

        const results = db.exec(data.sql, data.params);

        // Confirmar creación de tabla específica
        if (data.sql.includes("CREATE TABLE")) {
           const tableName = data.sql.match(/CREATE TABLE IF NOT EXISTS (\w+)/i)?.[1] || "unknown";
           console.log(`Worker: TABLA CONFIRMADA -> ${tableName}`);
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
    console.error(`Worker: Error ejecutando [${data.action}]:`, err);
    return self.postMessage({
      id: data && data.id,
      error: { message: String(err.message || err) }
    });
  }
};
