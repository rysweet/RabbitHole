#!/usr/bin/env python3
"""Sample raw pixels inside one validated world-canvas screen target."""

from __future__ import annotations

import argparse
import json
import math
import re
import shutil
import subprocess
from pathlib import Path
from typing import Any


def write_json(path: Path, payload: dict[str, Any]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(payload, indent=2, sort_keys=True) + "\n", encoding="utf-8")


def is_number(value: Any) -> bool:
    return isinstance(value, (int, float)) and not isinstance(value, bool) and math.isfinite(value)


def validated_extents(target: dict[str, Any]) -> dict[str, int] | None:
    extents = target.get("screenExtents")
    if not isinstance(extents, dict) or extents.get("coordinateType") != "screen":
        return None
    for key in ("x", "y", "width", "height"):
        if not is_number(extents.get(key)):
            return None
    x = int(extents["x"])
    y = int(extents["y"])
    width = int(extents["width"])
    height = int(extents["height"])
    if width <= 0 or height <= 0:
        return None
    return {"x": x, "y": y, "width": width, "height": height}


def sample_points(extents: dict[str, int]) -> list[dict[str, int | str]]:
    x = extents["x"]
    y = extents["y"]
    width = extents["width"]
    height = extents["height"]
    left = x + (1 if width > 2 else 0)
    right = x + width - (2 if width > 2 else 1)
    top = y + (1 if height > 2 else 0)
    bottom = y + height - (2 if height > 2 else 1)
    center = {"name": "center", "x": x + width // 2, "y": y + height // 2}
    points = [
        center,
        {"name": "upper-left-inset", "x": left, "y": top},
        {"name": "lower-right-inset", "x": right, "y": bottom},
    ]
    unique: list[dict[str, int | str]] = []
    seen: set[tuple[int, int]] = set()
    for point in points:
        key = (int(point["x"]), int(point["y"]))
        if key not in seen:
            seen.add(key)
            unique.append(point)
    return unique


def normalize_channel(value: int) -> int:
    if value <= 255:
        return value
    return max(0, min(255, round(value / 257)))


def parse_rgba(text: str) -> list[int] | None:
    match = re.search(r"\(([^)]+)\)", text)
    if not match:
        match = re.search(r"rgba?\(([^)]+)\)", text, flags=re.IGNORECASE)
    if not match:
        return None
    channels: list[int] = []
    for raw_channel in match.group(1).split(","):
        raw_channel = raw_channel.strip()
        if raw_channel.endswith("%"):
            channels.append(round(float(raw_channel[:-1]) * 255 / 100))
        else:
            channels.append(normalize_channel(round(float(raw_channel))))
    if len(channels) == 3:
        channels.append(255)
    if len(channels) != 4:
        return None
    if any(channel < 0 or channel > 255 for channel in channels):
        return None
    return channels


def sample_pixel(point: dict[str, int | str]) -> list[int]:
    if shutil.which("xwd") is None or shutil.which("convert") is None:
        raise RuntimeError("xwd and convert are required for target-scoped raw pixel sampling")
    x = int(point["x"])
    y = int(point["y"])
    xwd = subprocess.run(
        ["xwd", "-root", "-silent"],
        check=False,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
    )
    if xwd.returncode != 0:
        raise RuntimeError((xwd.stderr or b"xwd capture failed").decode("utf-8", errors="replace").strip())
    convert = subprocess.run(
        ["convert", "xwd:-", "-crop", f"1x1+{x}+{y}", "txt:-"],
        input=xwd.stdout,
        check=False,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        text=True,
    )
    if convert.returncode != 0:
        raise RuntimeError((convert.stderr or "convert pixel extraction failed").strip())
    rgba = parse_rgba(convert.stdout)
    if rgba is None:
        raise RuntimeError("convert output did not contain a parseable RGBA pixel")
    return rgba


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--target-json", required=True)
    parser.add_argument("--output", required=True)
    args = parser.parse_args()

    output_path = Path(args.output)
    try:
        target = json.loads(Path(args.target_json).read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as exc:
        write_json(output_path, {"schemaVersion": 1, "status": "blocked", "blocker": "target-json-unreadable", "blockerDetail": str(exc)})
        return 0
    if not isinstance(target, dict):
        write_json(output_path, {"schemaVersion": 1, "status": "blocked", "blocker": "target-json-invalid", "blockerDetail": "Target JSON must be an object."})
        return 0
    extents = validated_extents(target)
    if extents is None:
        write_json(output_path, {"schemaVersion": 1, "status": "blocked", "blocker": "target-geometry-invalid", "blockerDetail": "Target screenExtents must be positive screen coordinates."})
        return 0

    samples: list[dict[str, Any]] = []
    try:
        for point in sample_points(extents):
            rgba = sample_pixel(point)
            samples.append(
                {
                    "name": str(point["name"]),
                    "point": {"x": int(point["x"]), "y": int(point["y"])},
                    "rgba": rgba,
                    "checked": True,
                }
            )
    except RuntimeError as exc:
        write_json(output_path, {"schemaVersion": 1, "status": "blocked", "blocker": "pixel-sampling-failed", "blockerDetail": str(exc)})
        return 0

    write_json(
        output_path,
        {
            "schemaVersion": 1,
            "status": "observed",
            "samplingMethod": "xwd-convert-target-scoped-raw-rgba",
            "sampleCount": len(samples),
            "samples": samples,
        },
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
