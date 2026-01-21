export const POI_CATEGORIES = [
  "Nature",
  "History & Culture",
  "Adventure",
  "Relaxation",
  "Food & Culinary",
  "Shopping",
] as const;

export type PoiCategoryLabel = (typeof POI_CATEGORIES)[number];

const POI_CATEGORY_BY_KEY: Record<string, PoiCategoryLabel> = {
  NATURE: "Nature",
  HISTORY_AND_CULTURE: "History & Culture",
  ADVENTURE: "Adventure",
  RELAXATION: "Relaxation",
  FOOD_AND_CULINARY: "Food & Culinary",
  SHOPPING: "Shopping",
};

const POI_CATEGORY_BY_LABEL = POI_CATEGORIES.reduce(
  (acc, label) => {
    acc[label.toLowerCase()] = label;
    return acc;
  },
  {} as Record<string, PoiCategoryLabel>
);

export function normalizePoiCategory(value: string): PoiCategoryLabel | null {
  const trimmed = value.trim();
  if (!trimmed) return null;

  const labelMatch = POI_CATEGORY_BY_LABEL[trimmed.toLowerCase()];
  if (labelMatch) return labelMatch;

  const key = trimmed
    .toUpperCase()
    .replace(/&/g, "AND")
    .replace(/\s+/g, "_")
    .replace(/_+/g, "_");

  return POI_CATEGORY_BY_KEY[key] ?? null;
}

export function normalizePoiCategories(values: unknown): PoiCategoryLabel[] {
  if (!Array.isArray(values)) return [];

  const normalized: PoiCategoryLabel[] = [];
  for (const entry of values) {
    if (typeof entry !== "string") continue;
    const label = normalizePoiCategory(entry);
    if (label && !normalized.includes(label)) normalized.push(label);
  }

  return normalized;
}
