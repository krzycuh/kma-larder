import type { Pantry } from '../types/pantry';

/**
 * The pantry the user switched to in this session, or – when there is none or it is gone (left or deleted,
 * another account on this device) – their default one. The API marks exactly one pantry as the default.
 */
export function pickCurrentPantry(pantries: Pantry[], storedId: string | null): Pantry | null {
  return (
    pantries.find((pantry) => pantry.id === storedId) ??
    pantries.find((pantry) => pantry.isDefault) ??
    pantries[0] ??
    null
  );
}
