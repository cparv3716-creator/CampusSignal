import test from "node:test";
import assert from "node:assert/strict";
import { safeHttpUrl, splitEmailBody } from "./emailReader.js";

test("keeps a plain-text email readable and preserves long links", () => {
  const body =
    "Hello students,\n\nRegister at https://example.edu/a/very/long/link?ref=campussignal today.";
  assert.deepEqual(splitEmailBody(body), {
    message: body,
    footer: "",
    quoted: "",
  });
  assert.equal(
    safeHttpUrl("https://example.edu/a/very/long/link?ref=campussignal"),
    "https://example.edu/a/very/long/link?ref=campussignal",
  );
});

test("collapses only reliable quoted replies and promotional footer markers", () => {
  const parts = splitEmailBody(
    "New announcement.\n\nUnsubscribe\nManage preferences\n\nOn Mon, A wrote:\n> Earlier reply",
  );
  assert.equal(parts.message, "New announcement.");
  assert.match(parts.footer, /Unsubscribe/);
  assert.match(parts.quoted, /Earlier reply/);
});

test("permits only http and https links", () => {
  assert.equal(safeHttpUrl("javascript:alert(1)"), null);
  assert.equal(safeHttpUrl("data:text/html,test"), null);
  assert.equal(
    safeHttpUrl("https://safe.example/path."),
    "https://safe.example/path",
  );
});

test("leaves unsafe HTML as inert plain text", () => {
  const body = "<img src=x onerror=alert(1)>\n<script>alert(1)</script>";
  assert.equal(splitEmailBody(body).message, body);
});
