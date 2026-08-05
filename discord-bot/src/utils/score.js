'use strict';

/**
 * Parse "24/12/5" or "24-12-5" or "24 12 5" into { kills, deaths, assists }.
 */
function parseKda(raw) {
  const cleaned = String(raw).trim().replace(/,/g, ' ');
  const parts = cleaned.split(/[/\-\s|]+/).filter(Boolean);
  if (parts.length !== 3) return null;
  const nums = parts.map((p) => Number.parseInt(p, 10));
  if (nums.some((n) => !Number.isFinite(n) || n < 0 || n > 999)) return null;
  return { kills: nums[0], deaths: nums[1], assists: nums[2] };
}

function parseScore(raw) {
  const n = Number.parseInt(String(raw).trim(), 10);
  if (!Number.isFinite(n) || n < 0 || n > 99) return null;
  return n;
}

/**
 * Validate competitive / OT style scores.
 * Regulation typically ends 13+ with 2-round lead, OT continues until +2.
 * We only enforce non-negative integers and reject obvious nonsense (both 0 optional ok for forfeit).
 */
function validateMatchScores(score1, score2) {
  if (score1 === score2) {
    return 'Le score ne peut pas être une égalité — en compétitif, l\'OT continue jusqu\'à +2 manches.';
  }
  return null;
}

module.exports = { parseKda, parseScore, validateMatchScores };
