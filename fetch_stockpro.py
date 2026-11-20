import urllib.request, urllib.parse, json, re, html, datetime, os, time

CHANNEL = "stockpro_online"
PAGES = 6            # each page is about 20 posts
KEEP_DAYS = 45
OUT = "positional_trades.json"
MAP_FILE = "symbol_map.json"          # remembers name -> Yahoo symbol
OVERRIDE_FILE = "symbol_overrides.json"  # optional: you can add fixes here by hand
ENRICH_KEYS = ["symbol_yf", "cmp", "cmp_time", "status", "status_text", "targets_hit", "entry_time",
               "hit_time", "sl_time", "realised_pct", "pnl_pct", "upside_left_pct", "to_entry_pct"]
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


# ---------------- prices and status (Yahoo Finance, NSE symbols end with .NS) ----------------
def yget(url):
    last = None
    for _ in range(2):
        try:
            return json.loads(get(url))
        except Exception as e:
            last = e
            time.sleep(1.5)
    raise last


def load_json(path, default):
    try:
        return json.load(open(path, encoding="utf-8"))
    except Exception:
        return default


def chart(symbol):
    url = ("https://query1.finance.yahoo.com/v8/finance/chart/" + urllib.parse.quote(symbol) +
           "?interval=15m&range=60d&includePrePost=false")
    r = yget(url)["chart"]["result"][0]
    q = r["indicators"]["quote"][0]
    ts = r.get("timestamp") or []
    candles = [{"t": ts[i], "h": q["high"][i], "l": q["low"][i]}
               for i in range(len(ts)) if q["high"][i] is not None and q["low"][i] is not None]
    meta = r.get("meta", {})
    return candles, meta.get("regularMarketPrice"), meta.get("regularMarketTime")


def search_symbol(name):
    for q in (name, name + " NSE"):
        url = ("https://query2.finance.yahoo.com/v1/finance/search?q=" + urllib.parse.quote(q) +
               "&quotesCount=8&newsCount=0&listsCount=0")
        try:
            for it in yget(url).get("quotes", []):
                s = it.get("symbol", "")
                if s.endswith(".NS") and it.get("quoteType", "EQUITY") == "EQUITY":
                    return s
        except Exception as e:
            print("search failed:", name, e)
        time.sleep(0.4)
    return ""


def evaluate(t, candles, cmp, post_ts, now_ts):
    """Work out entry / target / stop-loss status from 15-minute candles after the post."""
    try:
        e, sl = float(t["entry"]), float(t["sl"])
        T = [float(x) for x in t["targets"]]
    except Exception:
        return {"status": "unknown", "status_text": "Check levels"}
    if t.get("check_levels") or not post_ts or not T:
        return {"status": "unknown", "status_text": "Check levels"}
    buy = t["side"] == "BUY"
    reach = (lambda c, v: c["h"] >= v) if buy else (lambda c, v: c["l"] <= v)
    stopped = (lambda c: c["l"] <= sl) if buy else (lambda c: c["h"] >= sl)
    entered_i = entry_ts = sl_ts = hit_ts = None
    hits = 0
    for i, c in enumerate(candles):
        if c["t"] < post_ts:
            continue
        if entered_i is None:
            if not reach(c, e):
                continue
            entered_i, entry_ts = i, c["t"]
        if i > entered_i and stopped(c):
            sl_ts = c["t"]
            break
        while hits < len(T) and reach(c, T[hits]):
            hits += 1
            hit_ts = c["t"]
    out = {"targets_hit": hits, "entry_time": entry_ts, "hit_time": hit_ts, "sl_time": sl_ts,
           "realised_pct": None, "pnl_pct": None, "upside_left_pct": None, "to_entry_pct": None}
    sgn = 1 if buy else -1
    nxt = T[hits] if hits < len(T) else None
    if cmp and nxt:
        out["upside_left_pct"] = round(sgn * (nxt - cmp) / cmp * 100, 2)
    if hits > 0:
        out["status"] = "target"
        out["status_text"] = ("Target hit" if len(T) == 1 else f"T{hits} hit") if hits < len(T) or len(T) == 1 else "All targets hit"
        out["realised_pct"] = round(sgn * (T[hits - 1] - e) / e * 100, 2)
        if hits == len(T):
            out["upside_left_pct"] = 0.0
    elif sl_ts:
        out["status"], out["status_text"] = "sl", "Stop loss hit"
        out["realised_pct"] = round(sgn * (sl - e) / e * 100, 2)
    elif entered_i is not None:
        out["status"], out["status_text"] = "active", "Active"
        if cmp:
            out["pnl_pct"] = round(sgn * (cmp - e) / e * 100, 2)
    else:
        age_days = (now_ts - post_ts) / 86400
        if age_days > 10:
            out["status"], out["status_text"] = "missed", "Entry not triggered"
        else:
            out["status"], out["status_text"] = "waiting", "Waiting for entry"
        if cmp:
            out["to_entry_pct"] = round(sgn * (e - cmp) / cmp * 100, 2)
    return out


def iso(ts):
    return datetime.datetime.fromtimestamp(ts, datetime.timezone.utc).isoformat() if ts else ""


def enrich(trades, prev_all):
    smap = load_json(MAP_FILE, {})
    over = {k.lower(): v for k, v in load_json(OVERRIDE_FILE, {}).items()}
    charts, now_ts = {}, time.time()
    ok = fail = 0
    for t in trades:
        key = t["name"].lower()
        sym = over.get(key) or smap.get(key, "")
        try:
            if not sym and t["symbol"]:
                try:
                    chart(t["symbol"] + ".NS")
                    sym = t["symbol"] + ".NS"
                except Exception:
                    sym = ""
            if not sym:
                sym = search_symbol(t["name"])
            if not sym:
                raise RuntimeError("symbol not found")
            smap[key] = sym
            if sym not in charts:
                charts[sym] = chart(sym)
                time.sleep(0.4)
            candles, cmp, cmp_t = charts[sym]
            try:
                post_ts = datetime.datetime.fromisoformat(t["date"]).timestamp()
            except Exception:
                post_ts = None
            res = evaluate(t, candles, cmp, post_ts, now_ts)
            for k in ("entry_time", "hit_time", "sl_time"):
                if k in res:
                    res[k] = iso(res[k])
            t.update(res)
            t["symbol_yf"], t["cmp"], t["cmp_time"] = sym, cmp, iso(cmp_t)
            ok += 1
        except Exception as e:
            fail += 1
            print("price failed:", t["name"], "->", e)
            prev = prev_all.get(t["id"], {})
            for k in ENRICH_KEYS:
                if k in prev and k not in t:
                    t[k] = prev[k]
            if "status" not in t:
                t["status"], t["status_text"] = "unknown", "Price unavailable"
    json.dump(smap, open(MAP_FILE, "w", encoding="utf-8"), indent=1, sort_keys=True)
    print("prices ok:", ok, "| failed:", fail)


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
    prev_all = dict(old)
    old.update(trades)                      # newest read wins (handles edited posts)
    cutoff = (datetime.datetime.now(datetime.timezone.utc) - datetime.timedelta(days=KEEP_DAYS)).isoformat()
    final = sorted((t for t in old.values() if not t.get("date") or t["date"] >= cutoff),
                   key=lambda t: t["id"], reverse=True)
    if not final and scanned == 0:
        print("nothing read; keeping old file")
        return
    enrich(final, prev_all)
    json.dump({"updated": datetime.datetime.now(datetime.timezone.utc).isoformat(),
               "channel": CHANNEL, "trades": final},
              open(OUT, "w", encoding="utf-8"), ensure_ascii=False, indent=1)
    print("posts scanned:", scanned, "| positional trades kept:", len(final))


if __name__ == "__main__":
    main()
