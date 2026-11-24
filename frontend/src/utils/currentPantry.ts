import type { Pantry } from '../types/pantry';

/**
 * The pantry the user last picked, or – when there is none or it is gone (left the pantry,
 * another account on this device) – the first one; the API lists the user's own pantry first.
 */
export function pickCurrentPantry(pantries: Pantry[], storedId: string | null): Pantry | null {
  return pantries.find((pantry) => pantry.id === storedId) ?? pantries[0] ?? null;
}
