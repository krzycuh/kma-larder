import { useSyncExternalStore } from 'react';
import { useQuery } from '@tanstack/react-query';
import { pantriesApi } from '../api/pantries';
import { pickCurrentPantry } from '../utils/currentPantry';

/**
 * Pantries of the current user (owned and joined), the default one first. May be empty: a new account has none.
 */
export function usePantries() {
  return useQuery({
    queryKey: ['pantries'],
    queryFn: pantriesApi.list,
    staleTime: 5 * 60_000,
  });
}

const STORAGE_KEY = 'currentPantryId';
const listeners = new Set<() => void>();

// The switcher's choice lasts for the session (this tab / the opened app): every fresh start lands on
// the default pantry, so nobody keeps adding to a pantry they switched to days ago.
function readStoredId(): string | null {
  try {
    return sessionStorage.getItem(STORAGE_KEY);
  } catch {
    return null;
  }
}

function subscribe(listener: () => void) {
  listeners.add(listener);
  return () => {
    listeners.delete(listener);
  };
}

/** Switches the current pantry; `null` goes back to the default one. */
export function setCurrentPantryId(id: string | null) {
  try {
    if (id === null) sessionStorage.removeItem(STORAGE_KEY);
    else sessionStorage.setItem(STORAGE_KEY, id);
  } catch {
    // Private mode etc.: the choice just won't survive a reload
  }
  listeners.forEach((listener) => listener());
}

/**
 * The pantry every stock page works on: the one switched to in this session, else the user's default.
 */
export function useCurrentPantry() {
  const query = usePantries();
  const storedId = useSyncExternalStore(subscribe, readStoredId, () => null);
  const pantries = query.data ?? [];
  return { ...query, pantries, pantry: pickCurrentPantry(pantries, storedId), setPantry: setCurrentPantryId };
}
