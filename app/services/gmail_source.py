import base64
from datetime import datetime, timezone
from email.header import decode_header, make_header
from email.message import Message
from email.utils import getaddresses, parsedate_to_datetime
from html.parser import HTMLParser
from typing import Any

from app.schemas.email import CanonicalEmail


class GmailEmailSource:
    """Convert one Gmail API message into the canonical ingestion contract."""

    def __init__(self, message: dict[str, Any]) -> None:
        self.message = message

    def fetch(self) -> CanonicalEmail:
        message_id = self.message.get("id")
        if not message_id:
            raise ValueError("Gmail message is missing its id")

        payload = self.message.get("payload") or {}
        headers = _headers_by_name(payload.get("headers", []))

        return CanonicalEmail(
            external_message_id=f"gmail:{message_id}",
            sender=_sender_from_header(headers.get("from")),
            recipients=_recipients_from_headers(headers),
            subject=_decode_header_value(headers.get("subject")) or "(no subject)",
            body_text=_extract_body(payload),
            received_at=_received_at(headers.get("date"), self.message.get("internalDate")),
            source="gmail",
        )


def _headers_by_name(headers: list[dict[str, str]]) -> dict[str, str]:
    result: dict[str, str] = {}
    for header in headers:
        name = header.get("name", "").lower()
        if name and name not in result:
            result[name] = header.get("value", "")
    return result


def _decode_header_value(value: str | None) -> str:
    if not value:
        return ""
    try:
        return str(make_header(decode_header(value))).strip()
    except (LookupError, UnicodeError):
        return value.strip()


def _sender_from_header(value: str | None) -> str:
    addresses = getaddresses([value]) if value else []
    for _, address in addresses:
        if address:
            return address
    return "unknown"


def _recipients_from_headers(headers: dict[str, str]) -> list[str]:
    values = [
        headers[name]
        for name in ("to", "cc", "bcc")
        if headers.get(name)
    ]
    recipients: list[str] = []
    for _, address in getaddresses(values):
        if address and address not in recipients:
            recipients.append(address)
    return recipients


def _received_at(date_header: str | None, internal_date: Any) -> datetime:
    if date_header:
        try:
            parsed = parsedate_to_datetime(date_header)
            if parsed is not None:
                if parsed.tzinfo is None:
                    parsed = parsed.replace(tzinfo=timezone.utc)
                return parsed.astimezone(timezone.utc)
        except (TypeError, ValueError, OverflowError):
            pass

    if internal_date is None:
        raise ValueError("Gmail message is missing both Date and internalDate")

    try:
        milliseconds = int(internal_date)
    except (TypeError, ValueError) as exc:
        raise ValueError("Gmail message has an invalid internalDate") from exc
    return datetime.fromtimestamp(milliseconds / 1000, tz=timezone.utc)


def _extract_body(payload: dict[str, Any]) -> str:
    plain_parts: list[str] = []
    html_parts: list[str] = []
    _collect_text_parts(payload, plain_parts, html_parts)

    if plain_parts:
        return "\n\n".join(part for part in plain_parts if part).strip()
    if html_parts:
        return "\n\n".join(_html_to_text(part) for part in html_parts).strip()
    return ""


def _collect_text_parts(
    part: dict[str, Any],
    plain_parts: list[str],
    html_parts: list[str],
) -> None:
    if part.get("filename") or (part.get("body") or {}).get("attachmentId"):
        return

    mime_type = (part.get("mimeType") or "").lower()
    data = (part.get("body") or {}).get("data")
    if data and mime_type in {"text/plain", "text/html"}:
        decoded = _decode_body_data(data, _charset_for_part(part))
        if mime_type == "text/plain":
            plain_parts.append(decoded)
        else:
            html_parts.append(decoded)

    for child in part.get("parts") or []:
        _collect_text_parts(child, plain_parts, html_parts)


def _charset_for_part(part: dict[str, Any]) -> str:
    content_type = next(
        (
            header.get("value", "")
            for header in part.get("headers", [])
            if header.get("name", "").lower() == "content-type"
        ),
        "",
    )
    message = Message()
    if content_type:
        message["content-type"] = content_type
    return message.get_content_charset() or "utf-8"


def _decode_body_data(data: str, charset: str) -> str:
    padding = "=" * (-len(data) % 4)
    try:
        decoded = base64.urlsafe_b64decode(data + padding)
    except (ValueError, TypeError) as exc:
        raise ValueError("Gmail message body contains invalid Base64URL data") from exc
    try:
        return decoded.decode(charset, errors="replace")
    except LookupError:
        return decoded.decode("utf-8", errors="replace")


class _HTMLTextExtractor(HTMLParser):
    block_tags = {
        "br",
        "div",
        "p",
        "li",
        "tr",
        "h1",
        "h2",
        "h3",
        "h4",
        "h5",
        "h6",
    }

    def __init__(self) -> None:
        super().__init__(convert_charrefs=True)
        self.fragments: list[str] = []
        self.ignored_depth = 0

    def handle_starttag(self, tag: str, attrs: list[tuple[str, str | None]]) -> None:
        del attrs
        if tag in {"script", "style"}:
            self.ignored_depth += 1
        elif tag in self.block_tags:
            self.fragments.append("\n")

    def handle_endtag(self, tag: str) -> None:
        if tag in {"script", "style"} and self.ignored_depth:
            self.ignored_depth -= 1
        elif tag in self.block_tags:
            self.fragments.append("\n")

    def handle_data(self, data: str) -> None:
        if not self.ignored_depth:
            self.fragments.append(data)


def _html_to_text(html: str) -> str:
    parser = _HTMLTextExtractor()
    parser.feed(html)
    lines = (" ".join(line.split()) for line in "".join(parser.fragments).splitlines())
    return "\n".join(line for line in lines if line)
