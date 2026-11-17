export const OPPORTUNITY_CATEGORIES = new Set([
  "CAREER",
  "ACADEMIC_RESEARCH",
  "RECOGNITION_ACHIEVEMENT",
  "CALL_FOR_PARTICIPATION",
  "EVENT_ACTIVITY",
]);

export function isOpportunity(itemOrCategory) {
  const category =
    typeof itemOrCategory === "string"
      ? itemOrCategory
      : itemOrCategory?.emailCategory;
  return OPPORTUNITY_CATEGORIES.has(category);
}

export function selectOpportunities(feed) {
  return (feed || []).filter(isOpportunity);
}

export function filterOpportunities(feed, { query = "", category = "" } = {}) {
  const normalizedQuery = query.trim().toLowerCase();
  return selectOpportunities(feed)
    .filter((item) => !category || item.emailCategory === category)
    .filter((item) =>
      `${item.emailSubject || ""} ${item.rankingExplanation || ""}`
        .toLowerCase()
        .includes(normalizedQuery),
    );
}
