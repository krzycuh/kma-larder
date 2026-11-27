import { describe, expect, it } from 'vitest';
import { pickCurrentPantry } from '../currentPantry';
import type { Pantry } from '../../types/pantry';

const pantry = (id: string, isDefault = false): Pantry => ({
  id,
  name: id,
  ownerId: 'o',
  createdAt: '2026-01-01T00:00:00',
  myRole: 'MEMBER',
  memberCount: 1,
  isDefault,
});

describe('pickCurrentPantry', () => {
  const allotment = pantry('allotment');
  const family = pantry('family', true);

  it('keeps the pantry the user switched to', () => {
    expect(pickCurrentPantry([allotment, family], 'allotment')).toBe(allotment);
  });

  it('falls back to the default pantry when nothing or an unknown pantry is stored', () => {
    expect(pickCurrentPantry([allotment, family], null)).toBe(family);
    expect(pickCurrentPantry([allotment, family], 'left-long-ago')).toBe(family);
  });

  it('takes the first pantry when none is marked as the default', () => {
    expect(pickCurrentPantry([allotment, pantry('other')], null)).toBe(allotment);
  });

  it('returns null without pantries', () => {
    expect(pickCurrentPantry([], 'allotment')).toBeNull();
  });
});
