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

CLAIM_SCOPE = "visible-rendering-world-canvas-pixel-sampling"
CLAIM_SCOPE_DETAIL = "target-scoped-raw-pixel-observation-only"
UNSUPPORTED_CLAIMS = [
    "world-canvas-pixel-correctness",
    "full-visible-rendering-correctness",
    "rendered-world-correctness",
    "world-execution",
]


def write_json(path: Path, payload: dict[str, Any]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(payload, indent=2, sort_keys=True) + "\n", encoding="utf-8")


def blocker_payload(blocker: str, blocker_detail: str) -> dict[str, Any]:
    return {
        "schemaVersion": 1,
        "status": "blocked",
        "blocker": blocker,
        "blockerDetail": blocker_detail,
        "claimScope": CLAIM_SCOPE,
        "claimScopeDetail": CLAIM_SCOPE_DETAIL,
        "renderedWorldPixelsObserved": False,
        "visibleRenderingCorrectnessEstablished": False,
        "sampleCount": 0,
        "samples": [],
        "unsupportedClaims": UNSUPPORTED_CLAIMS,
    }


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
        try:
            if raw_channel.endswith("%"):
                channels.append(round(float(raw_channel[:-1]) * 255 / 100))
            else:
                channels.append(normalize_channel(round(float(raw_channel))))
        except ValueError:
            return None
    if len(channels) == 3:
        channels.append(255)
    if len(channels) != 4:
        return None
    if any(channel < 0 or channel > 255 for channel in channels):
        return None
    return channels


def missing_sampling_tools() -> list[str]:
    return [tool for tool in ("xwd", "convert") if shutil.which(tool) is None]


def capture_root_image() -> bytes:
    xwd = subprocess.run(
        ["xwd", "-root", "-silent"],
        check=False,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
    )
    if xwd.returncode != 0:
        raise RuntimeError((xwd.stderr or b"xwd capture failed").decode("utf-8", errors="replace").strip())
    return xwd.stdout


def extract_pixel(root_image: bytes, point: dict[str, int | str]) -> list[int]:
    x = int(point["x"])
    y = int(point["y"])
    convert = subprocess.run(
        ["convert", "xwd:-", "-crop", f"1x1+{x}+{y}", "txt:-"],
        input=root_image,
        check=False,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
    )
    if convert.returncode != 0:
        raise RuntimeError(
            (convert.stderr or b"convert pixel extraction failed")
            .decode("utf-8", errors="replace")
            .strip()
        )
    rgba = parse_rgba(convert.stdout.decode("utf-8", errors="replace"))
    if rgba is None:
        raise RuntimeError("convert output did not contain a parseable RGBA pixel")
    return rgba


def read_target(
    path: Path,
) -> tuple[dict[str, Any] | None, dict[str, int] | None, dict[str, Any] | None]:
    try:
        target = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as exc:
        return None, None, blocker_payload("target-json-unreadable", str(exc))
    if not isinstance(target, dict):
        return None, None, blocker_payload("target-json-invalid", "Target JSON must be an object.")
    extents = validated_extents(target)
    if extents is None:
        return None, None, blocker_payload(
            "target-geometry-invalid",
            "Target screenExtents must be positive screen coordinates.",
        )
    return target, extents, None


def collect_samples(extents: dict[str, int]) -> list[dict[str, Any]]:
    missing_tools = missing_sampling_tools()
    if missing_tools:
        tool_list = " and ".join(missing_tools)
        verb = "are" if len(missing_tools) > 1 else "is"
        raise RuntimeError(
            f"{tool_list} {verb} required for target-scoped raw pixel sampling"
        )

    root_image = capture_root_image()
    samples: list[dict[str, Any]] = []
    for point in sample_points(extents):
        samples.append(
            {
                "name": str(point["name"]),
                "point": {"x": int(point["x"]), "y": int(point["y"])},
                "rgba": extract_pixel(root_image, point),
                "checked": True,
            }
        )
    return samples


def observation_payload(target: dict[str, Any], samples: list[dict[str, Any]]) -> dict[str, Any]:
    return {
        "schemaVersion": 1,
        "status": "observed",
        "blocker": "none",
        "blockerDetail": "",
        "claimScope": CLAIM_SCOPE,
        "claimScopeDetail": CLAIM_SCOPE_DETAIL,
        "renderedWorldPixelsObserved": True,
        "visibleRenderingCorrectnessEstablished": False,
        "samplingMethod": "xwd-convert-target-scoped-raw-rgba",
        "sampleCount": len(samples),
        "samples": samples,
        "worldCanvasPixelTarget": target,
        "unsupportedClaims": UNSUPPORTED_CLAIMS,
    }


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--target-json", required=True)
    parser.add_argument("--output", required=True)
    args = parser.parse_args()

    output_path = Path(args.output)
    target, extents, blocker = read_target(Path(args.target_json))
    if blocker is not None:
        write_json(output_path, blocker)
        return 0
    if target is None or extents is None:
        write_json(output_path, blocker_payload("target-json-invalid", "Target JSON must be an object."))
        return 0

    try:
        samples = collect_samples(extents)
    except RuntimeError as exc:
        write_json(output_path, blocker_payload("pixel-sampling-failed", str(exc)))
        return 0

    write_json(output_path, observation_payload(target, samples))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
