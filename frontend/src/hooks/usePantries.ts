import { useSyncExternalStore } from 'react';
import { useQuery } from '@tanstack/react-query';
import { pantriesApi } from '../api/pantries';
import { pickCurrentPantry } from '../utils/currentPantry';

/**
 * Pantries of the current user: their own one first, then household pantries they were invited to.
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

function readStoredId(): string | null {
  try {
    return localStorage.getItem(STORAGE_KEY);
  } catch {
    return null;
  }
}

function subscribe(listener: () => void) {
  listeners.add(listener);
  window.addEventListener('storage', listener);
  return () => {
    listeners.delete(listener);
    window.removeEventListener('storage', listener);
  };
}

export function setCurrentPantryId(id: string) {
  try {
    localStorage.setItem(STORAGE_KEY, id);
  } catch {
    // Private mode etc.: the choice just won't survive a reload
  }
  listeners.forEach((listener) => listener());
}

/**
 * The pantry every stock page works on. Remembered per device; shared by all pages.
 */
export function useCurrentPantry() {
  const query = usePantries();
  const storedId = useSyncExternalStore(subscribe, readStoredId, () => null);
  const pantries = query.data ?? [];
  return { ...query, pantries, pantry: pickCurrentPantry(pantries, storedId), setPantry: setCurrentPantryId };
}
