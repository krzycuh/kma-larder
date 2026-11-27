export type PantryRole = 'OWNER' | 'MEMBER';

export interface Pantry {
  id: string;
  name: string;
  ownerId: string;
  createdAt: string;
  /** Role of the current user in this pantry */
  myRole: PantryRole;
  memberCount: number;
  /** The pantry the current user's pages open on; exactly one of their pantries has it */
  isDefault: boolean;
}

export interface PantryMember {
  userId: string;
  name: string;
  email: string;
  role: PantryRole;
}
