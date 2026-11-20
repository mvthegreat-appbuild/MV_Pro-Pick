import urllib.request, json, re, html, datetime, os

CHANNEL = "stockpro_online"
PAGES = 6            # each page is about 20 posts
KEEP_DAYS = 45
OUT = "positional_trades.json"
UA = {"User-Agent": "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 Chrome/120 Mobile Safari/537.36"}
NUM = r"([\d,]+(?:\.\d+)?)"


def get(url):
    req = urllib.request.Request(url, headers=UA)
    return urllib.request.urlopen(req, timeout=30).read().decode("utf-8", "ignore")


def to_text(fragment):
    t = re.sub(r"(?i)<br\s*/?>", "\n", fragment)
    t = re.sub(r"(?s)<[^>]+>", "", t)
    return html.unescape(t).replace("\xa0", " ")


def posts_from_page(page):
    """Return list of (id, iso_date, text) from one t.me/s page."""
    marks = list(re.finditer(r'data-post="[^"/]+/(\d+)"', page))
    out = []
    for i, m in enumerate(marks):
        chunk = page[m.end(): marks[i + 1].start() if i + 1 < len(marks) else len(page)]
        tm = re.search(r'<div class="tgme_widget_message_text[^"]*"[^>]*>(.*?)</div>', chunk, re.S)
        dm = re.search(r'<time[^>]*datetime="([^"]+)"', chunk)
        if tm:
            out.append((int(m.group(1)), dm.group(1) if dm else "", to_text(tm.group(1))))
    return out


def num(s):
    try:
        return float(str(s).replace(",", ""))
    except Exception:
        return None


def fmt(x):
    return str(int(x)) if float(x).is_integer() else str(round(x, 2))


def parse_positional(text):
    """Return a trade dict only for POSITIONAL TRADE posts that have entry, SL and targets."""
    lines = [l.strip() for l in text.split("\n") if l.strip()]
    if not lines or not any(re.match(r"(?i)^positional\s+trade", l) for l in lines[:3]):
        return None
    hi = next(i for i, l in enumerate(lines[:3]) if re.match(r"(?i)^positional\s+trade", l))
    if hi + 1 >= len(lines):
        return None
    head = lines[hi + 1]
    sym = re.search(r"\(([A-Za-z0-9&\-]+)\)", head)
    name = re.sub(r"\s*\([^)]*\)\s*", " ", head).strip()
    body = "\n".join(lines[hi + 1:])

    side, entry = "BUY", None
    m = re.search(r"(?i)\babove\b\s*[:\-]?\s*" + NUM, body)
    if m:
        entry = num(m.group(1))
    else:
        m = re.search(r"(?i)\bbelow\b\s*[:\-]?\s*" + NUM, body)
        if m:
            side, entry = "SELL", num(m.group(1))
    m = re.search(r"(?i)(?:\bSL\b|stop\s*-?\s*loss)\s*[:\-]?\s*" + NUM, body)
    sl = num(m.group(1)) if m else None
    m = re.search(r"(?i)targets?\s*[:\-]?\s*([^\n]+)", body)
    tline = m.group(1) if m else ""
    nums = [num(x) for x in re.findall(r"\d[\d,]*(?:\.\d+)?", tline)]
    nums = [n for n in nums if n]
    if entry is None or sl is None or not nums:
        return None
    if re.search(r"(?i)\b(points?|pts)\b", tline):      # targets given as points from entry
        sign = 1 if side == "BUY" else -1
        targets = [entry + sign * n for n in nums]
        target_note = "points from entry"
    else:
        targets = nums
        target_note = ""
    m = re.search(r"(?i)\bhold\b\s+([^\n.]+)", body)
    period = ("Hold " + m.group(1).strip()) if m else ""
    ok = (sl < entry < min(targets)) if side == "BUY" else (max(targets) < entry < sl)
    return {"name": name, "symbol": sym.group(1).upper() if sym else "", "side": side,
            "entry": fmt(entry), "sl": fmt(sl), "targets": [fmt(t) for t in targets],
            "target_note": target_note, "period": period, "check_levels": not ok}


def main():
    trades, before, scanned = {}, None, 0
    for _ in range(PAGES):
        url = f"https://t.me/s/{CHANNEL}" + (f"?before={before}" if before else "")
        try:
            posts = posts_from_page(get(url))
        except Exception as e:
            print("page failed:", e)
            break
        if not posts:
            break
        scanned += len(posts)
        for pid, date, text in posts:
            t = parse_positional(text)
            if t:
                t.update({"id": pid, "date": date, "link": f"https://t.me/{CHANNEL}/{pid}"})
                trades[pid] = t
        before = min(p[0] for p in posts)
    old = {}
    if os.path.exists(OUT):
        try:
            old = {t["id"]: t for t in json.load(open(OUT, encoding="utf-8")).get("trades", [])}
        except Exception:
            old = {}
    old.update(trades)                      # newest read wins (handles edited posts)
    cutoff = (datetime.datetime.now(datetime.timezone.utc) - datetime.timedelta(days=KEEP_DAYS)).isoformat()
    final = sorted((t for t in old.values() if not t.get("date") or t["date"] >= cutoff),
                   key=lambda t: t["id"], reverse=True)
    if not final and scanned == 0:
        print("nothing read; keeping old file")
        return
    json.dump({"updated": datetime.datetime.now(datetime.timezone.utc).isoformat(),
               "channel": CHANNEL, "trades": final},
              open(OUT, "w", encoding="utf-8"), ensure_ascii=False, indent=1)
    print("posts scanned:", scanned, "| positional trades kept:", len(final))


if __name__ == "__main__":
    main()
