import test from "node:test";
import assert from "node:assert/strict";
import { filterOpportunities, selectOpportunities } from "./opportunities.js";

const item = (emailCategory, emailSubject = emailCategory) => ({
  emailCategory,
  emailSubject,
  rankingExplanation: "",
});
const demo = [
  item("CAREER", "Summer Internship"),
  item("ACADEMIC_RESEARCH", "Research Assistant"),
  item("EVENT_ACTIVITY", "Art Evening"),
];

test("one shared selector counts all three qualifying demo opportunities", () => {
  assert.equal(selectOpportunities(demo).length, 3);
});

test("the same selector returns two qualifying items without hardcoded counts", () => {
  assert.equal(selectOpportunities(demo.slice(0, 2)).length, 2);
});

test("nonqualifying messages do not increase the opportunity total", () => {
  assert.equal(
    selectOpportunities([
      ...demo,
      item("ADMINISTRATIVE_NOTICE", "Library notice"),
    ]).length,
    3,
  );
});

test("search and temporary type filters change shown results but not the shared total", () => {
  assert.equal(selectOpportunities(demo).length, 3);
  assert.equal(filterOpportunities(demo, { query: "research" }).length, 1);
  assert.equal(filterOpportunities(demo, { category: "CAREER" }).length, 1);
  assert.equal(selectOpportunities(demo).length, 3);
});

test("a Demo Mode to live workspace switch recalculates from the active feed", () => {
  const live = [
    item("CAREER", "Live internship"),
    item("ADMINISTRATIVE_NOTICE", "Live notice"),
  ];
  assert.equal(selectOpportunities(demo).length, 3);
  assert.equal(selectOpportunities(live).length, 1);
});
