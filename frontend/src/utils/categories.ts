import type { ProductCategory } from '../types/stock';

/** One pictogram per category, a quick visual cue in lists */
export const CATEGORY_ICONS: Record<ProductCategory, string> = {
  PRESERVES: '🫙',
  HONEY: '🍯',
  FLOUR_GRAINS: '🌾',
  PASTA_RICE: '🍝',
  CANNED: '🥫',
  BEVERAGES: '💧',
  OILS_SAUCES: '🫒',
  SWEETS_SNACKS: '🍫',
  SPICES_BAKING: '🧂',
  DRIED: '🥜',
  FROZEN: '🧊',
  OTHER: '📦',
};
