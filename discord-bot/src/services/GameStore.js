'use strict';

const fs = require('node:fs');
const path = require('node:path');

const DATA_DIR = path.join(__dirname, '..', 'data');
const STORE_PATH = path.join(DATA_DIR, 'games.json');

/**
 * Persistent store for active / finished custom games.
 * Kept simple on purpose: one JSON file, no external DB.
 */
class GameStore {
  constructor() {
    this.games = new Map();
    this._load();
  }

  _load() {
    try {
      if (!fs.existsSync(DATA_DIR)) {
        fs.mkdirSync(DATA_DIR, { recursive: true });
      }
      if (!fs.existsSync(STORE_PATH)) {
        this._save();
        return;
      }
      const raw = fs.readFileSync(STORE_PATH, 'utf8');
      const parsed = JSON.parse(raw);
      const list = Array.isArray(parsed.games) ? parsed.games : [];
      for (const game of list) {
        this.games.set(game.id, game);
      }
    } catch (err) {
      console.error('[GameStore] Impossible de charger games.json:', err);
      this.games = new Map();
    }
  }

  _save() {
    if (!fs.existsSync(DATA_DIR)) {
      fs.mkdirSync(DATA_DIR, { recursive: true });
    }
    const payload = {
      games: [...this.games.values()],
    };
    fs.writeFileSync(STORE_PATH, JSON.stringify(payload, null, 2), 'utf8');
  }

  get(id) {
    return this.games.get(id) || null;
  }

  getByMessageId(messageId) {
    for (const game of this.games.values()) {
      if (game.messageId === messageId) return game;
    }
    return null;
  }

  /** Active (non-finished) games only. */
  listActive() {
    return [...this.games.values()].filter((g) => g.status !== 'finished' && g.status !== 'cancelled');
  }

  upsert(game) {
    this.games.set(game.id, game);
    this._save();
    return game;
  }

  delete(id) {
    const ok = this.games.delete(id);
    if (ok) this._save();
    return ok;
  }
}

module.exports = { GameStore };
