import json
import csv


with open("data.json", "r") as f:
    records = json.load(f)

fields = [
    "voiceQuality", "audioIssues", "environment", "carrier",
    "networkGeneration", "signalStrength", "latitude", "longitude",
    "timestamp", "callDuration", "id", "created_at"
]


with open("output.csv", "w", newline="") as f:
    writer = csv.DictWriter(f, fieldnames=fields)
    writer.writeheader()

    for r in records:
        if r.get("carrier") is (None or ""):
            r["carrier"] = "JIO"
        r["audioIssues"] = ",".join(r["audioIssues"]) if r["audioIssues"] else ""
        writer.writerow({k: r.get(k) for k in fields})

print("CSV file created: output.csv")