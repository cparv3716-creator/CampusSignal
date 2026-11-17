const QUOTED_REPLY =
  /^(?:>+|On .+ wrote:|From:\s.+|-----Original Message-----)/i;
const PROMOTIONAL_FOOTER =
  /^(?:unsubscribe|manage (?:your )?preferences|email preferences|view (?:this )?in browser|privacy policy|all rights reserved|follow us(?:\s+on)?)[\s.!:|•-]*$/i;
const URL_PATTERN = /^(https?):\/\/[^\s<>'"`]+$/i;

export function safeHttpUrl(value) {
  const candidate = String(value || "").replace(/[),.;:!?]+$/, "");
  if (!URL_PATTERN.test(candidate)) return null;
  try {
    const parsed = new URL(candidate);
    return parsed.protocol === "https:" || parsed.protocol === "http:"
      ? parsed.href
      : null;
  } catch {
    return null;
  }
}

export function splitEmailBody(value) {
  const lines = String(value || "")
    .replace(/\r\n/g, "\n")
    .split("\n");
  const quotedAt = lines.findIndex((line) => QUOTED_REPLY.test(line.trim()));
  const visible = quotedAt < 0 ? lines : lines.slice(0, quotedAt);
  const quoted = quotedAt < 0 ? [] : lines.slice(quotedAt);
  const footerAt = visible.findIndex(
    (line, index) => index > 0 && PROMOTIONAL_FOOTER.test(line.trim()),
  );
  return {
    message: (footerAt < 0 ? visible : visible.slice(0, footerAt))
      .join("\n")
      .trim(),
    footer: (footerAt < 0 ? [] : visible.slice(footerAt)).join("\n").trim(),
    quoted: quoted.join("\n").trim(),
  };
}
