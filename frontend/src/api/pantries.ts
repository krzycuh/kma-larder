import apiClient from './client';
import type { Pantry, PantryMember } from '../types/pantry';

export const pantriesApi = {
  list: async (): Promise<Pantry[]> => {
    const response = await apiClient.get<Pantry[]>('/pantries');
    return response.data;
  },

  create: async (name: string): Promise<Pantry> => {
    const response = await apiClient.post<Pantry>('/pantries', { name });
    return response.data;
  },

  /** Owner only: removes the pantry with its products, batches and history, for every member. */
  remove: async (pantryId: string): Promise<void> => {
    await apiClient.delete(`/pantries/${pantryId}`);
  },

  /** The pantry the pages open on, for the current user only. */
  setDefault: async (pantryId: string): Promise<void> => {
    await apiClient.put(`/pantries/${pantryId}/default`);
  },

  rename: async (pantryId: string, name: string): Promise<Pantry> => {
    const response = await apiClient.patch<Pantry>(`/pantries/${pantryId}`, { name });
    return response.data;
  },

  members: async (pantryId: string): Promise<PantryMember[]> => {
    const response = await apiClient.get<PantryMember[]>(`/pantries/${pantryId}/members`);
    return response.data;
  },

  /** Owner only: adds an existing user (by e-mail) as a member. */
  invite: async (pantryId: string, email: string): Promise<PantryMember> => {
    const response = await apiClient.post<PantryMember>(`/pantries/${pantryId}/members`, { email });
    return response.data;
  },

  /** Owner removes a member, or a member leaves (userId = own id). */
  removeMember: async (pantryId: string, userId: string): Promise<void> => {
    await apiClient.delete(`/pantries/${pantryId}/members/${userId}`);
  },
};
