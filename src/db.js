const sqlite3 = require('sqlite3').verbose();
const fs = require('fs');
const path = require('path');

const DB_PATH = process.env.DB_PATH || path.join(process.cwd(), 'data', 'sgv.db');

function ensureDataDir() {
  const dir = path.dirname(DB_PATH);
  if (!fs.existsSync(dir)) fs.mkdirSync(dir, { recursive: true });
}

function openDb() {
  ensureDataDir();
  return new sqlite3.Database(DB_PATH);
}

async function init() {
  const db = openDb();
  const schemaPath = path.join(__dirname, '..', 'migrations', 'schema.sql');
  if (!fs.existsSync(schemaPath)) throw new Error('schema.sql not found');
  const sql = fs.readFileSync(schemaPath, 'utf8');
  return new Promise((resolve, reject) => {
    db.exec(sql, (err) => {
      db.close();
      if (err) return reject(err);
      resolve();
    });
  });
}

module.exports = { init, openDb };
