import { describe, expect, it } from 'vitest';
import { pickCurrentPantry } from '../currentPantry';
import type { Pantry } from '../../types/pantry';

const pantry = (id: string, myRole: Pantry['myRole']): Pantry => ({
  id,
  name: id,
  ownerId: 'o',
  createdAt: '2026-01-01T00:00:00',
  myRole,
  memberCount: 1,
});

describe('pickCurrentPantry', () => {
  const own = pantry('own', 'OWNER');
  const family = pantry('family', 'MEMBER');

  it('keeps the pantry the user picked', () => {
    expect(pickCurrentPantry([own, family], 'family')).toBe(family);
  });

  it('falls back to the first (own) pantry when nothing or an unknown pantry is stored', () => {
    expect(pickCurrentPantry([own, family], null)).toBe(own);
    expect(pickCurrentPantry([own, family], 'left-long-ago')).toBe(own);
  });

  it('returns null without pantries', () => {
    expect(pickCurrentPantry([], 'own')).toBeNull();
  });
});
