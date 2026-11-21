import json, re, os, asyncio, datetime
import fetch_stockpro as sp

CHANNEL = "https://t.me/s/motilaloswalofficial"
PAGES = 6
KEEP_DAYS = 45
OUT = "motilal_trades.json"
NUM = sp.NUM


def parse_call(text):
    """Read BUY/SELL + CMP + SL + TGT posts. Anything else is skipped."""
    lines = [l.strip() for l in text.split("\n") if l.strip()]
    body = "\n".join(lines)
    m = re.search(r"(?im)^(BUY|SELL)\s+([A-Za-z0-9&\-\. ]{2,40})$", body)
    if not m:
        return None
    side, name = m.group(1).upper(), m.group(2).strip().upper()
    c = re.search(r"(?im)^CMP\s*[:\-]?\s*" + NUM, body)
    s = re.search(r"(?im)^(?:SL|STOP\s*LOSS)\s*[:\-]?\s*" + NUM, body)
    t = re.search(r"(?im)^(?:TGT|TARGETS?)\s*[:\-]?\s*([^\n]+)", body)
    if not (c and s and t):
        return None
    entry, sl = sp.num(c.group(1)), sp.num(s.group(1))
    tl = re.sub(r"\([^)]*\)", "", t.group(1))
    tl = re.sub(r"\d+(?:\.\d+)?\s*%", "", tl)
    targets = [sp.num(x) for x in re.findall(r"\d[\d,]*(?:\.\d+)?", tl)]
    targets = [x for x in targets if x]
    if not entry or not sl or not targets:
        return None
    kind = lines[0] if not re.match(r"(?i)^(BUY|SELL)\s", lines[0]) and len(lines[0]) < 40 else ""
    idea = " ".join(l[1:].strip() for l in lines if l.startswith("-"))[:220]
    ok = (sl < entry < min(targets)) if side == "BUY" else (max(targets) < entry < sl)
    return {"name": name, "symbol": name if " " not in name else "", "side": side,
            "entry": sp.fmt(entry), "sl": sp.fmt(sl), "targets": [sp.fmt(x) for x in targets],
            "target_note": "", "period": kind, "check_levels": not ok,
            "entry_label": "Entry · CMP at call", "idea": idea}


def evaluate_mo(t, bars, cmp, now_dt, prev):
    """Entry is the CMP at call time, so the trade is active from the post."""
    try:
        e, sl = float(t["entry"]), float(t["sl"])
        T = [float(x) for x in t["targets"]]
    except Exception:
        return {"status": "unknown", "status_text": "Check levels"}
    if t.get("check_levels") or not T:
        return {"status": "unknown", "status_text": "Check levels"}
    buy = t["side"] == "BUY"
    sg = 1 if buy else -1
    reach = (lambda b, v: b["h"] >= v) if buy else (lambda b, v: b["l"] <= v)
    stopped = (lambda b: b["l"] <= sl) if buy else (lambda b: b["h"] >= sl)
    now_iso = now_dt.isoformat()
    when = lambda b, old: b["d"] if b["src"] == "day" else (old or now_iso)
    hits, hit_t, sl_t = 0, None, None
    for b in bars:
        if stopped(b):
            if hits == 0:
                sl_t = when(b, prev.get("sl_time"))
            break
        while hits < len(T) and reach(b, T[hits]):
            hits += 1
            hit_t = when(b, prev.get("hit_time"))
    out = {"targets_hit": hits, "entry_time": t["date"], "hit_time": hit_t, "sl_time": sl_t,
           "realised_pct": None, "pnl_pct": None, "upside_left_pct": None, "to_entry_pct": None}
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
    else:
        out["status"], out["status_text"] = "active", "Active"
        if cmp:
            out["pnl_pct"] = round(sg * (cmp - e) / e * 100, 2)
    return out


def main():
    if CHANNEL.startswith("PASTE"):
        raise SystemExit("Set CHANNEL in fetch_motilal.py first")
    trades, before, scanned = {}, None, 0
    for _ in range(PAGES):
        url = f"https://t.me/s/{CHANNEL}" + (f"?before={before}" if before else "")
        try:
            posts = sp.posts_from_page(sp.get(url))
        except Exception as e:
            print("page failed:", e)
            break
        if not posts:
            break
        scanned += len(posts)
        for pid, date, text in posts:
            t = parse_call(text)
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
    old.update(trades)
    cutoff = (datetime.datetime.now(datetime.timezone.utc) - datetime.timedelta(days=KEEP_DAYS)).isoformat()
    final = sorted((t for t in old.values() if not t.get("date") or t["date"] >= cutoff),
                   key=lambda t: t["id"], reverse=True)
    if not final and scanned == 0:
        print("nothing read; keeping old file")
        return
    sp.evaluate = evaluate_mo
    asyncio.run(sp.enrich(final, prev_all))
    json.dump({"updated": datetime.datetime.now(datetime.timezone.utc).isoformat(),
               "channel": CHANNEL, "trades": final},
              open(OUT, "w", encoding="utf-8"), ensure_ascii=False, indent=1)
    print("posts scanned:", scanned, "| calls kept:", len(final))


if __name__ == "__main__":
    main()
