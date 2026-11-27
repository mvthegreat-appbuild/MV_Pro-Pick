import urllib.request, json, re, html, datetime, os, time, asyncio

CHANNEL = "stockpro_online"
PAGES = 6            # each page is about 20 posts
KEEP_DAYS = 45
OUT = "positional_trades.json"
MAP_FILE = "symbol_map.json"          # remembers name -> NSE symbol
OVERRIDE_FILE = "symbol_overrides.json"  # optional: you can add fixes here by hand
QUOTE_URL = "https://mcp.nseindia.in/cmmkt/mcp"          # live price
HIST_URL = "https://mcp.nseindia.in/bhavcopy/cm/mcp"       # daily history + symbol search
IST = datetime.timezone(datetime.timedelta(hours=5, minutes=30))
# names whose NSE symbol cannot be guessed from the name (add your own in symbol_overrides.json)
SEED = {"tata motors": "TMPV", "hindustan aeronautics": "HAL", "mahindra & mahindra": "M&M",
        "bosch": "BOSCHLTD", "divi's laboratories": "DIVISLAB", "balkrishna": "BALKRISIND",
        "vodafone idea": "IDEA", "page industries": "PAGEIND", "prestige estates": "PRESTIGE",
        "sun tv": "SUNTV", "hindalco": "HINDALCO", "bank of baroda": "BANKBARODA"}
ENRICH_KEYS = ["symbol_nse", "cmp", "cmp_time", "status", "status_text", "targets_hit", "entry_time",
               "hit_time", "sl_time", "realised_pct", "pnl_pct", "upside_left_pct", "to_entry_pct",
               "seen_high", "seen_low", "seen_close"]
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


# ---------------- prices and status (NSE data) ----------------
def load_json(path, default):
    try:
        return json.load(open(path, encoding="utf-8"))
    except Exception:
        return default


def fnum(x):
    try:
        v = float(str(x).replace(",", ""))
        return v if v > 0 else None
    except Exception:
        return None


def parse_result(result):
    if result.is_error:
        raise RuntimeError("NSE request failed")
    text = next((p.text for p in result.content if hasattr(p, "text")), "")
    obj, _ = json.JSONDecoder().raw_decode(text.lstrip())
    return obj


async def call(session, tool, args):
    return parse_result(await session.call_tool(tool, args))


async def resolve(hist, t, over, smap):
    """Find the NSE symbol for a trade. Returns '' when it cannot be matched safely."""
    key = t["name"].lower()
    if key in over:
        return over[key]
    if key in smap:
        return smap[key]
    words = re.sub(r"[^A-Za-z0-9 ]", " ", t["name"]).split()
    target = "".join(words).lower()
    queries = []
    if t["symbol"]:
        queries.append(t["symbol"])
    if words:
        queries += [words[0], "".join(words[:2]), "".join(words), t["name"]]
    seen = set()
    for q in queries:
        if q.lower() in seen:
            continue
        seen.add(q.lower())
        try:
            res = await call(hist, "search_symbols", {"query": q})
        except Exception:
            continue
        syms = [r.get("symbol", "") for r in res.get("results", [])]
        if t["symbol"] and t["symbol"].upper() in syms:
            return t["symbol"].upper()
        pref = [s for s in syms if s and target.startswith(s.lower())]
        if pref:
            return max(pref, key=len)
        await asyncio.sleep(0.3)
    return ""


def build_bars(daily, quote, t, now_dt, prev):
    """Daily bars after the post, plus a live bar for today and a tracking bar for the post day."""
    post_dt = datetime.datetime.fromisoformat(t["date"]).astimezone(IST)
    post_date, today = post_dt.date().isoformat(), now_dt.date().isoformat()
    pre_open = post_dt.time() < datetime.time(9, 15)
    after_close = post_dt.time() > datetime.time(15, 30)
    bars = []
    for d in daily:
        ds = d["date"]
        if ds < post_date or (ds == post_date and not pre_open):
            continue                 # bar of the post day also holds moves before the post
        bars.append({"d": ds, "h": d["high"], "l": d["low"], "c": d["close"], "src": "day"})
    have = {d["date"] for d in daily}
    if quote and today > post_date and today not in have:
        bars.append({"d": today, "h": quote["high"], "l": quote["low"], "c": quote["ltp"], "src": "live"})
    # live checks on the post day itself (the 15-minute runs are saved in seen_high / seen_low)
    sh, sl_, sc = prev.get("seen_high"), prev.get("seen_low"), prev.get("seen_close")
    if quote and today == post_date and not after_close and not pre_open:
        p = quote["ltp"]
        sh, sl_, sc = max(sh or p, p), min(sl_ or p, p), p
    t["seen_high"], t["seen_low"], t["seen_close"] = sh, sl_, sc
    if sh and not pre_open and not after_close:
        bars.append({"d": post_date, "h": sh, "l": sl_, "c": sc, "src": "track"})
    return sorted(bars, key=lambda b: b["d"])


def evaluate(t, bars, cmp, now_dt, prev):
    """Entry, target and stop-loss status from daily highs and lows after the post."""
    try:
        e, sl = float(t["entry"]), float(t["sl"])
        T = [float(x) for x in t["targets"]]
    except Exception:
        return {"status": "unknown", "status_text": "Check levels"}
    if t.get("check_levels") or not T:
        return {"status": "unknown", "status_text": "Check levels"}
    buy = t["side"] == "BUY"
    reach = (lambda b, v: b["h"] >= v) if buy else (lambda b, v: b["l"] <= v)
    stopped = (lambda b: b["l"] <= sl) if buy else (lambda b: b["h"] >= sl)
    closed_stop = (lambda b: b["c"] <= sl) if buy else (lambda b: b["c"] >= sl)
    now_iso = now_dt.isoformat()
    when = lambda b, old: b["d"] if b["src"] == "day" else (old or now_iso)
    entry_t = hit_t = sl_t = None
    entered = False
    hits = 0
    for b in bars:
        if not entered:
            if not reach(b, e):
                continue
            entered, entry_t = True, when(b, prev.get("entry_time"))
            while hits < len(T) and reach(b, T[hits]):
                hits += 1
                hit_t = when(b, prev.get("hit_time"))
            if hits == 0 and closed_stop(b):      # entry day: only a closing stop counts
                sl_t = when(b, prev.get("sl_time"))
                break
            continue
        if stopped(b):                             # later days: stop first if both touched
            if hits == 0:
                sl_t = when(b, prev.get("sl_time"))
            break
        while hits < len(T) and reach(b, T[hits]):
            hits += 1
            hit_t = when(b, prev.get("hit_time"))
    out = {"targets_hit": hits, "entry_time": entry_t, "hit_time": hit_t, "sl_time": sl_t,
           "realised_pct": None, "pnl_pct": None, "upside_left_pct": None, "to_entry_pct": None}
    sg = 1 if buy else -1
    nxt = T[hits] if hits < len(T) else None
    if cmp and nxt:
        out["upside_left_pct"] = round(sg * (nxt - cmp) / cmp * 100, 2)
    if hits > 0:
        out["status"] = "target"
        out["status_text"] = ("Target hit" if len(T) == 1 else f"T{hits} hit") if hits < len(T) or len(T) == 1 else "All targets hit"
        out["realised_pct"] = round(sg * (T[hits - 1] - e) / e * 100, 2)
        if hits == len(T):
            out["upside_left_pct"] = 0.0
    elif sl_t:
        out["status"], out["status_text"] = "sl", "Stop loss hit"
        out["realised_pct"] = round(sg * (sl - e) / e * 100, 2)
    elif entered:
        out["status"], out["status_text"] = "active", "Active"
        if cmp:
            out["pnl_pct"] = round(sg * (cmp - e) / e * 100, 2)
    else:
        post = datetime.datetime.fromisoformat(t["date"])
        age = (now_dt - post).total_seconds() / 86400
        out["status"], out["status_text"] = ("missed", "Entry not triggered") if age > 10 else ("waiting", "Waiting for entry")
        if cmp:
            out["to_entry_pct"] = round(sg * (e - cmp) / cmp * 100, 2)
    return out


async def run_enrich(trades, prev_all, hist, qs, now_dt=None):
    now_dt = now_dt or datetime.datetime.now(IST)
    smap = load_json(MAP_FILE, {})
    over = dict(SEED)
    over.update({k.lower(): str(v).upper() for k, v in load_json(OVERRIDE_FILE, {}).items()})
    daily_c, quote_c = {}, {}
    ok = fail = 0
    for t in trades:
        prev = prev_all.get(t["id"], {})
        try:
            sym = await resolve(hist, t, over, smap)
            if not sym:
                print("symbol not found:", t["name"], "-> add it to symbol_overrides.json")
                raise RuntimeError("symbol not found")
            smap[t["name"].lower()] = sym
            if sym not in daily_c:
                daily_c[sym] = (await call(hist, "get_stock_history", {"symbol": sym, "months": 3, "endDate": "today"})).get("data", [])
                try:
                    q = (await call(qs, "cm_get_stock_quote", {"symbol": sym})).get("stock", {})
                    ltp = fnum(q.get("lastTradedPrice"))
                    quote_c[sym] = {"ltp": ltp, "open": fnum(q.get("open")) or ltp,
                                    "high": fnum(q.get("high")) or ltp, "low": fnum(q.get("low")) or ltp} if ltp else None
                except Exception as e:
                    print("live price failed:", sym, e)
                    quote_c[sym] = None
                await asyncio.sleep(0.5)
            daily, quote = daily_c[sym], quote_c[sym]
            if not daily and not quote:
                raise RuntimeError("no price data")
            cmp = quote["ltp"] if quote else fnum(daily[-1]["close"])
            cmp_time = now_dt.isoformat() if quote else (daily[-1]["date"] if daily else "")
            bars = build_bars(daily, quote, t, now_dt, prev)
            t.update(evaluate(t, bars, cmp, now_dt, prev))
            t["symbol_nse"], t["cmp"], t["cmp_time"] = sym, cmp, cmp_time
            ok += 1
        except Exception as e:
            fail += 1
            print("price failed:", t["name"], "->", e)
            for k in ENRICH_KEYS:
                if k in prev and k not in t:
                    t[k] = prev[k]
            if "status" not in t:
                t["status"], t["status_text"] = "unknown", "Price unavailable"
    json.dump(smap, open(MAP_FILE, "w", encoding="utf-8"), indent=1, sort_keys=True)
    print("prices ok:", ok, "| failed:", fail)


async def enrich(trades, prev_all):
    from mcp import ClientSession
    from mcp.client.streamable_http import streamable_http_client
    try:
        async with streamable_http_client(HIST_URL, terminate_on_close=False) as (r1, w1):
            async with ClientSession(r1, w1) as hist:
                await hist.initialize()
                async with streamable_http_client(QUOTE_URL, terminate_on_close=False) as (r2, w2):
                    async with ClientSession(r2, w2) as qs:
                        await qs.initialize()
                        await run_enrich(trades, prev_all, hist, qs)
    except Exception as e:
        print("NSE connection failed:", e)
        for t in trades:
            prev = prev_all.get(t["id"], {})
            for k in ENRICH_KEYS:
                if k in prev and k not in t:
                    t[k] = prev[k]
            t.setdefault("status", "unknown")
            t.setdefault("status_text", "Price unavailable")


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
    asyncio.run(enrich(final, prev_all))
    json.dump({"updated": datetime.datetime.now(datetime.timezone.utc).isoformat(),
               "channel": CHANNEL, "trades": final},
              open(OUT, "w", encoding="utf-8"), ensure_ascii=False, indent=1)
    print("posts scanned:", scanned, "| positional trades kept:", len(final))


if __name__ == "__main__":
    main()
