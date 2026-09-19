import test from 'node:test';
import assert from 'node:assert/strict';
import { calculateDisputeAmounts, creatorShareCap } from './disputes.js';

test('full refund consumes pending escrow without crediting creator', () => {
  assert.deepEqual(calculateDisputeAmounts(100000, 100000, 100000), {
    toCreator: 0,
    pendingAfter: 0,
    creatorFromPending: 0,
    customerFromPending: 100000,
  });
});

test('partial dispute splits pending escrow correctly', () => {
  assert.deepEqual(calculateDisputeAmounts(100000, 100000, 25000), {
    toCreator: 75000,
    pendingAfter: 0,
    creatorFromPending: 75000,
    customerFromPending: 25000,
  });
});

test('legacy insufficient pending balance never mints creator funds', () => {
  assert.deepEqual(calculateDisputeAmounts(100000, 30000, 0), {
    toCreator: 100000,
    pendingAfter: 0,
    creatorFromPending: 30000,
    customerFromPending: 0,
  });
});

test('creatorShareCap uses the net escrow.creatorShare, not the gross amount', () => {
  const escrow = { amount: 100000, platformFee: 10000, creatorShare: 90000 };
  assert.equal(creatorShareCap(escrow, 100000), 90000);
});

test('creatorShareCap falls back to the gross amount for legacy escrow docs without creatorShare', () => {
  assert.equal(creatorShareCap(null, 100000), 100000);
  assert.equal(creatorShareCap({ amount: 100000 }, 100000), 100000);
});

test('release_full on a disputed booking never pays the creator the platform commission', () => {
  // gross 100000, platform fee 10000, creator's net share 90000.
  const shareCap = creatorShareCap({ creatorShare: 90000 }, 100000);
  const { toCreator } = calculateDisputeAmounts(shareCap, 90000, 0);
  assert.equal(toCreator, 90000); // NOT 100000 — the platform's 10000 stays out of the creator's pocket.
});

test('refund beyond the creator share is absorbed by the platform, never negative for the creator', () => {
  // gross 100000, creatorShare 90000, admin refunds 95000 to the customer.
  const shareCap = creatorShareCap({ creatorShare: 90000 }, 100000);
  const { toCreator } = calculateDisputeAmounts(shareCap, 90000, 95000);
  const platformPortion = Math.max(0, 100000 - 95000 - toCreator);
  assert.equal(toCreator, 0);
  assert.equal(platformPortion, 5000); // platform keeps only what's left after covering the refund shortfall.
});
