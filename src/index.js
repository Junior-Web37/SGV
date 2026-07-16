const express = require('express');
const path = require('path');
const fs = require('fs');
require('dotenv').config();

const db = require('./db');

const app = express();
app.use(express.json());

const PORT = process.env.PORT || 3000;

app.get('/health', (req, res) => res.json({ ok: true, env: process.env.NODE_ENV || 'dev' }));

app.post('/init-db', async (req, res) => {
  try {
    await db.init();
    return res.json({ ok: true, message: 'DB initialized' });
  } catch (err) {
    console.error(err);
    return res.status(500).json({ ok: false, error: String(err) });
  }
});

app.listen(PORT, () => console.log(`SGV API listening on port ${PORT}`));
