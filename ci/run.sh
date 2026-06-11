#!/usr/bin/env bash
set -euo pipefail

CI_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
FQE_ROOT="$(cd "$CI_DIR/.." && pwd)"

github_repo_git_url() {
  local spec="${1:?repo required}"
  if [[ "$spec" == https://* ]]; then
    echo "$spec"
    return 0
  fi
  echo "https://github.com/${spec}.git"
}

_semver_strip() {
  echo "${1#v}"
}

_semver_gt() {
  local a b a1 a2 a3 b1 b2 b3
  a="$(_semver_strip "$1")"
  b="$(_semver_strip "$2")"
  IFS=. read -r a1 a2 a3 <<< "$a"
  IFS=. read -r b1 b2 b3 <<< "$b"
  a1=${a1:-0}
  a2=${a2:-0}
  a3=${a3:-0}
  b1=${b1:-0}
  b2=${b2:-0}
  b3=${b3:-0}
  (( a1 > b1 )) && return 0
  (( a1 < b1 )) && return 1
  (( a2 > b2 )) && return 0
  (( a2 < b2 )) && return 1
  (( a3 > b3 )) && return 0
  return 1
}

resolve_latest_semver_release_tag() {
  local repo_spec="${1:?owner/name or git URL required}"
  local git_url best tag

  git_url="$(github_repo_git_url "$repo_spec")"
  best=""
  while IFS= read -r tag; do
    [[ -z "$tag" ]] && continue
    if [[ -z "$best" ]] || _semver_gt "$tag" "$best"; then
      best="$tag"
    fi
  done < <(
    git ls-remote --tags "$git_url" \
      | awk -F/ '{print $NF}' \
      | sed 's/\^{}//' \
      | grep -E '^v?[0-9]+\.[0-9]+\.[0-9]+$'
  )

  if [[ -z "$best" ]]; then
    echo "error: no semver release tags found on ${git_url}" >&2
    return 1
  fi
  echo "$best"
}

resolve_github_release_ref() {
  local repo_spec="${1:?repo required}"
  local pinned="${2:-}"
  if [[ -n "$pinned" ]]; then
    echo "$pinned"
    return 0
  fi
  resolve_latest_semver_release_tag "$repo_spec"
}

resolve_modpack_tag() {
  resolve_github_release_ref \
    "${MODPACK_REPO:-https://github.com/TerraFirmaGreg-Team/Modpack-Modern.git}" \
    "${MODPACK_TAG:-}"
}

load_config() {
  local env_file="${CI_BUILD_ENV:-$CI_DIR/build.env}"
  if [[ ! -f "$env_file" ]]; then
    echo "::error::Missing CI config: $env_file" >&2
    exit 1
  fi

  set -a
  # shellcheck disable=SC1090
  source "$env_file"
  set +a

  local ws="${GITHUB_WORKSPACE:-$FQE_ROOT}"
  EXPORT_ROOT="${ws}/${EXPORT_ROOT_DIR:-export}"
  EXPORT_QUEST="${EXPORT_ROOT}/${QUEST_SUBDIR:-quest-export}"
  RUNNER_HOME="${RUNNER_HOME:-${HOME:-/home/runner}}"

  export RUNNER_HOME JAVA_VERSION
  export MC_VERSION MC_ASSET_INDEX FORGE_BUILD
  export HMC_REPO HMC_VERSION MODPACK_DIR MODPACK_REPO MODPACK_TAG
  export EXPORT_WARMUP_TICKS EXPORT_WORLD_DELAY_TICKS EXPORT_TIMEOUT_SECONDS
  export EXPORT_ROOT EXPORT_QUEST EXPORT_ROOT_DIR QUEST_SUBDIR
  export EXPORT_ARTIFACT_NAME="${EXPORT_ARTIFACT_NAME:-quest-export}"
  export FQE_JAR_PATH

  if [[ -n "${GITHUB_ENV:-}" ]]; then
    {
      printf 'RUNNER_HOME=%s\n' "$RUNNER_HOME"
      printf 'JAVA_VERSION=%s\n' "$JAVA_VERSION"
      printf 'MC_VERSION=%s\n' "$MC_VERSION"
      printf 'MC_ASSET_INDEX=%s\n' "$MC_ASSET_INDEX"
      printf 'FORGE_BUILD=%s\n' "$FORGE_BUILD"
      printf 'HMC_REPO=%s\n' "${HMC_REPO:-3arthqu4ke/headlessmc}"
      printf 'HMC_VERSION=%s\n' "${HMC_VERSION:-}"
      printf 'MODPACK_DIR=%s\n' "$MODPACK_DIR"
      printf 'MODPACK_REPO=%s\n' "$MODPACK_REPO"
      printf 'MODPACK_TAG=%s\n' "${MODPACK_TAG:-}"
      printf 'EXPORT_WARMUP_TICKS=%s\n' "$EXPORT_WARMUP_TICKS"
      printf 'EXPORT_WORLD_DELAY_TICKS=%s\n' "$EXPORT_WORLD_DELAY_TICKS"
      printf 'EXPORT_TIMEOUT_SECONDS=%s\n' "$EXPORT_TIMEOUT_SECONDS"
      printf 'EXPORT_ROOT_DIR=%s\n' "${EXPORT_ROOT_DIR:-export}"
      printf 'QUEST_SUBDIR=%s\n' "${QUEST_SUBDIR:-quest-export}"
      printf 'EXPORT_ROOT=%s\n' "$EXPORT_ROOT"
      printf 'EXPORT_QUEST=%s\n' "$EXPORT_QUEST"
      printf 'EXPORT_ARTIFACT_NAME=%s\n' "${EXPORT_ARTIFACT_NAME:-quest-export}"
      printf 'FQE_JAR_PATH=%s\n' "${FQE_JAR_PATH:-}"
    } >> "$GITHUB_ENV"
  fi
}

bundle_id_for_run() {
  local sha tag
  sha="${GITHUB_SHA:-$(git -C "$FQE_ROOT" rev-parse HEAD)}"
  sha="${sha:0:7}"
  tag="${MODPACK_TAG:-$(resolve_modpack_tag)}"
  tag="${tag#v}"
  printf 'fqe-dev-%s-%s' "$tag" "$sha"
}

checkout_modpack() {
  local mp="${MODPACK_DIR:-$FQE_ROOT/Modpack-Modern}"
  local repo="${MODPACK_REPO:-https://github.com/TerraFirmaGreg-Team/Modpack-Modern.git}"
  local tag

  if [[ -n "${MODPACK_TAG:-}" ]]; then
    tag="$MODPACK_TAG"
    echo "Using MODPACK_TAG override: $tag"
  else
    tag="$(resolve_modpack_tag)"
    if [[ -z "$tag" ]]; then
      echo "::error::No semver release tags found on ${MODPACK_REPO:-Modpack-Modern}" >&2
      exit 1
    fi
    echo "Latest release tag: $tag"
  fi

  cd "$FQE_ROOT"
  if [[ -e "$mp/.git" ]]; then
    local current
    current="$(git -C "$mp" describe --tags --exact-match 2>/dev/null || true)"
    if [[ "$current" == "$tag" ]]; then
      echo "Modpack-Modern already at $tag"
    else
      echo "Replacing $mp (was ${current:-unknown}) with shallow clone @ $tag ..."
      rm -rf "$mp"
      git clone --depth 1 --branch "$tag" "$repo" "$mp"
    fi
  else
    echo "Shallow cloning Modpack-Modern @ $tag into $mp ..."
    git clone --depth 1 --branch "$tag" "$repo" "$mp"
  fi

  cd "$mp"
  git describe --tags --exact-match 2>/dev/null || git describe --tags --always

  export MODPACK_TAG="$tag"
  if [[ -n "${GITHUB_OUTPUT:-}" ]]; then
    echo "modpack_tag=$tag" >> "$GITHUB_OUTPUT"
  fi
}

prepare_export() {
  load_config
  checkout_modpack
  export BUNDLE_ID="$(bundle_id_for_run)"
  if [[ -n "${GITHUB_ENV:-}" ]]; then
    printf 'BUNDLE_ID=%s\n' "$BUNDLE_ID" >> "$GITHUB_ENV"
  fi
  if [[ -n "${GITHUB_OUTPUT:-}" ]]; then
    echo "bundle_id=${BUNDLE_ID}" >> "$GITHUB_OUTPUT"
  fi
  echo "Export bundle_id=${BUNDLE_ID} modpack_tag=${MODPACK_TAG}"
}

export_languages() {
  local lang_cfg="$FQE_ROOT/ci/language.json"
  if [[ ! -f "$lang_cfg" ]]; then
    echo "::error::Missing $lang_cfg" >&2
    exit 1
  fi

  local csv
  csv="$(node -e "
const fs=require('fs');
const cfg=JSON.parse(fs.readFileSync(process.argv[1],'utf8'));
const arr=Array.isArray(cfg.enabledLocales)?cfg.enabledLocales:[];
const norm=[...new Set(arr.map(s=>String(s||'').trim().toLowerCase().replace(/-/g,'_')).filter(Boolean))];
process.stdout.write((norm.length?norm:['en_us','zh_cn']).join(','));
" "$lang_cfg")"

  if [[ -n "${GITHUB_OUTPUT:-}" ]]; then
    {
      echo "export_languages<<EOF"
      echo "$csv"
      echo "EOF"
    } >> "$GITHUB_OUTPUT"
  fi
  echo "Export languages (ci/language.json): ${csv}"
}

resolve_fqe_jar() {
  if [[ -n "${FQE_JAR_PATH:-}" && -f "$FQE_JAR_PATH" ]]; then
    echo "$FQE_JAR_PATH"
    return 0
  fi
  local jar
  jar="$(ls -t "$FQE_ROOT"/build/libs/ftb-quest-export-*.jar 2>/dev/null | head -1 || true)"
  if [[ -n "$jar" && -f "$jar" ]]; then
    echo "$jar"
    return 0
  fi
  echo "::error::No ftb-quest-export jar — set FQE_JAR_PATH or run ./gradlew jar first" >&2
  return 1
}

install_local_export_mod() {
  local jar mp
  jar="$(resolve_fqe_jar)" || exit 1
  mp="${MODPACK_DIR:-$FQE_ROOT/Modpack-Modern}"

  mkdir -p "$mp/mods"
  find "$mp/mods" -maxdepth 1 -name 'ftb-quest-export*.jar' -delete
  find "$mp/mods" -maxdepth 1 -name 'ftb-quest-forge*.jar' -delete
  find "$mp/mods" -maxdepth 1 -name 'ftbquest*.jar' -delete
  find "$mp/mods" -maxdepth 1 -name 'minecraft-web-export*.jar' -delete
  cp -v "$jar" "$mp/mods/"
  echo "Installed local ftb-quest-export: $jar"
}

install_display_deps() {
  if command -v xvfb-run >/dev/null 2>&1; then
    return 0
  fi
  sudo DEBIAN_FRONTEND=noninteractive apt-get update
  sudo DEBIAN_FRONTEND=noninteractive apt-get install -y \
    xvfb x11-xserver-utils \
    libgl1 libgl1-mesa-dri \
    libopenal1
}

setup_hmc() {
  local hmc_ver mc_ver forge mp mp_abs launcher

  hmc_ver="${HMC_VERSION:?HMC_VERSION required}"
  mc_ver="${MC_VERSION:?MC_VERSION required}"
  forge="${FORGE_BUILD:?FORGE_BUILD required}"
  mp="${MODPACK_DIR:-Modpack-Modern}"
  mp_abs="$(cd "$FQE_ROOT/$mp" && pwd)"
  launcher="headlessmc-launcher-${hmc_ver}.jar"

  cd "$FQE_ROOT"
  if [[ ! -f "$launcher" ]]; then
    gh release download "$hmc_ver" \
      --repo "${HMC_REPO:-3arthqu4ke/headlessmc}" \
      --pattern "$launcher" \
      --clobber
  fi

  mkdir -p HeadlessMC
  cat > HeadlessMC/config.properties <<EOF
hmc.java.versions=$JAVA_HOME/bin/java
hmc.gamedir=$mp_abs
hmc.offline=true
hmc.rethrow.launch.exceptions=true
hmc.exit.on.failed.command=true
EOF

  if [[ ! -f "$HOME/.minecraft/versions/$mc_ver/$mc_ver.json" ]]; then
    java -jar "$launcher" --command "download $mc_ver"
  fi
  if ! ls "$HOME/.minecraft/versions" 2>/dev/null | grep -q "$forge"; then
    java -jar "$launcher" --command "forge $mc_ver --uid $forge"
  fi
}

prepare_game() {
  install_display_deps
  install_local_export_mod
  setup_hmc
}

# Chapter quest icon atlases + global UI atlas (replaces legacy assets/icons/items/).
verify_quest_icon_atlases() {
  local quest="${1:?quest-export root required}"

  if [[ -f "$quest/assets/icons/items/manifest.json" ]] \
      || find "$quest/assets/icons/items" -name '*.png' 2>/dev/null | grep -q .; then
    echo "::error::Legacy per-item icons under $quest/assets/icons/items — re-export with current ftb-quest-export" >&2
    return 1
  fi

  if [[ ! -f "$quest/quests/global-atlas.png" ]]; then
    echo "::error::Missing $quest/quests/global-atlas.png" >&2
    return 1
  fi

  if [[ ! -f "$quest/quests/index.json" ]]; then
    echo "::error::Missing $quest/quests/index.json" >&2
    return 1
  fi

  local chapter_atlas_count
  chapter_atlas_count="$(find "$quest/quests/chapters" -maxdepth 1 -name '*.png' 2>/dev/null | wc -l | tr -d ' ')"
  if [[ "$chapter_atlas_count" -lt 1 ]]; then
    echo "::error::No chapter icon atlases under $quest/quests/chapters/*.png" >&2
    return 1
  fi

  python3 - "$quest" "$chapter_atlas_count" <<'PY'
import json
import sys
from pathlib import Path

quest = Path(sys.argv[1])
chapter_atlas_count = int(sys.argv[2])

index = json.loads((quest / "quests/index.json").read_text(encoding="utf-8"))
global_atlas = index.get("globalAtlas")
if not global_atlas:
    raise SystemExit("::error::quests/index.json missing globalAtlas")

for key in ("src", "width", "height", "missingIconId", "sprites"):
    if key not in global_atlas:
        raise SystemExit(f"::error::globalAtlas missing {key}")

missing_id = global_atlas["missingIconId"]
if missing_id != "fqe:missing_icon":
    raise SystemExit(f"::error::globalAtlas.missingIconId must be fqe:missing_icon (got: {missing_id})")

sprites = global_atlas["sprites"]
if missing_id not in sprites:
    raise SystemExit(f"::error::globalAtlas.sprites missing {missing_id}")

rect = sprites[missing_id]
if rect.get("w") != 16 or rect.get("h") != 16:
    raise SystemExit(f"::error::{missing_id} must be 16x16 in global atlas index")

atlas_png = quest / global_atlas["src"]
if not atlas_png.is_file():
    raise SystemExit(f"::error::global atlas file missing: {atlas_png}")

chapters = index.get("chapters") or []
if not chapters:
    raise SystemExit("::error::index.json has no chapters")

with_icon = [c for c in chapters if c.get("icon") and (c.get("iconDisplay") or {}).get("spriteId")]
if not with_icon:
    raise SystemExit("::error::index chapters missing iconDisplay for sidebar icons")

sample_filename = with_icon[0]["filename"]
expected_sprite = f"chapter:{sample_filename}"
if with_icon[0]["iconDisplay"]["spriteId"] != expected_sprite:
    raise SystemExit(
        f"::error::chapter iconDisplay.spriteId must be chapter:{{filename}} (got: {with_icon[0]['iconDisplay']['spriteId']})"
    )
if expected_sprite not in sprites:
    raise SystemExit(f"::error::globalAtlas.sprites missing {expected_sprite}")

chapters_dir = quest / "quests/chapters"
chapter_jsons = sorted(chapters_dir.glob("*.json"))
if not chapter_jsons:
    raise SystemExit(f"::error::No chapter JSON under {chapters_dir}")

sample = json.loads(chapter_jsons[0].read_text(encoding="utf-8"))
for key in ("iconAtlases", "iconSprites"):
    if key not in sample:
        raise SystemExit(f"::error::{chapter_jsons[0].name} missing {key}")

quests = sample.get("quests") or []
if quests and not (quests[0].get("iconDisplay") or {}).get("spriteId"):
    raise SystemExit(f"::error::{chapter_jsons[0].name} quests[0] missing iconDisplay.spriteId")

manifest = json.loads((quest / "manifest.json").read_text(encoding="utf-8"))
cia = manifest.get("chapterIconAtlases") or {}
sprites_packed = int(cia.get("spritesPacked") or 0)

print(
    f"quest icons: global-atlas ({len(sprites)} sprites, {len(with_icon)} chapter icons) + "
    f"{chapter_atlas_count} chapter quest atlas PNG(s), {sprites_packed} quest sprites packed"
)
PY
}

verify_quest_export() {
  local quest="${EXPORT_QUEST:?EXPORT_QUEST required}"

  for f in manifest.json meta.json; do
    if [[ ! -f "$quest/$f" ]]; then
      echo "::error::Missing $quest/$f"
      exit 1
    fi
  done

  local exporter
  exporter=$(python3 -c "import json; print(json.load(open('$quest/manifest.json')).get('exporter',''))")
  if [[ "$exporter" != "ftb-quest-export" ]]; then
    echo "::error::manifest.exporter must be ftb-quest-export (got: $exporter)"
    exit 1
  fi

  for d in assets lang quests extras; do
    if [[ ! -d "$quest/$d" ]]; then
      echo "::error::Missing directory $quest/$d"
      exit 1
    fi
  done

  verify_quest_icon_atlases "$quest"

  echo "quest-export OK: $quest"
  du -sh "$quest" "$quest/assets" "$quest/lang" "$quest/quests" "$quest/quests/chapters" 2>/dev/null || true
}

launch_export() {
  local mp hmc_ver launcher

  mp="${MODPACK_DIR:-$FQE_ROOT/Modpack-Modern}"
  hmc_ver="${HMC_VERSION:?HMC_VERSION required}"
  launcher="headlessmc-launcher-${hmc_ver}.jar"

  mkdir -p "$mp/config" "$mp/saves" "${EXPORT_ROOT:?EXPORT_ROOT required}"
  cp -f "$CI_DIR/config/export-fml.toml" "$mp/config/fml.toml"
  cp -f "$CI_DIR/config/export-forge-client.toml" "$mp/config/forge-client.toml"
  cat > "$mp/options.txt" <<EOF
onboardAccessibility:false
pauseOnLostFocus:false
EOF

  cd "$FQE_ROOT"
  xvfb-run --server-args="-screen 0 1280x720x24" -a java \
    -Dhmc.check.xvfb=true \
    -jar "$launcher" \
    --command "launch .*forge.* -regex --jvm \"${FQE_JVM_FLAGS:?FQE_JVM_FLAGS required}\""

  verify_quest_export
}

package_export() {
  load_config
  local bundle_id="${BUNDLE_ID:?BUNDLE_ID required — run prepare-export first}"
  local archive="$FQE_ROOT/quest-export-${bundle_id}.tar.gz"

  tar -czf "$archive" -C "$EXPORT_ROOT" quest-export
  ls -lh "$archive"

  if [[ -n "${GITHUB_OUTPUT:-}" ]]; then
    echo "archive_path=${archive}" >> "$GITHUB_OUTPUT"
    echo "archive_name=quest-export-${bundle_id}.tar.gz" >> "$GITHUB_OUTPUT"
  fi
}

collect_export_debug() {
  load_config

  local mp="${MODPACK_DIR:-$FQE_ROOT/Modpack-Modern}"
  local out="$FQE_ROOT/ci-debug"
  local quest="${EXPORT_QUEST:?EXPORT_QUEST required}"

  rm -rf "$out"
  mkdir -p "$out"

  if [[ -d "$mp/logs" ]]; then
    mkdir -p "$out/modpack/logs"
    for f in "$mp/logs"/*; do
      [[ -f "$f" ]] || continue
      local base
      base=$(basename "$f")
      if [[ "$base" == latest.log ]] || [[ $(stat -c%s "$f" 2>/dev/null || stat -f%z "$f") -lt 5242880 ]]; then
        cp -a "$f" "$out/modpack/logs/"
      fi
    done
  fi

  if [[ -d "$mp/crash-reports" ]]; then
    cp -a "$mp/crash-reports" "$out/modpack/"
  fi

  if [[ -f "$quest/manifest.json" ]]; then
    mkdir -p "$out/export/quest-export"
    cp "$quest/manifest.json" "$out/export/quest-export/"
    [[ -f "$quest/meta.json" ]] && cp "$quest/meta.json" "$out/export/quest-export/"
    du -sh "$quest" > "$out/export/quest-export-size.txt" 2>/dev/null || true
    find "$quest" -type f 2>/dev/null | head -200 > "$out/export/quest-export-file-sample.txt" || true
  fi

  local jar
  if jar="$(resolve_fqe_jar 2>/dev/null)"; then
    mkdir -p "$out/build"
    ls -lh "$jar" > "$out/build/fqe-jar.txt" 2>/dev/null || true
  fi

  if [[ -z "$(find "$out" -type f 2>/dev/null | head -1)" ]]; then
    echo "no debug files collected" > "$out/README.txt"
  fi

  echo "debug files under $out:"
  find "$out" -type f | head -50
}

usage() {
  cat <<'EOF'
Usage: bash ci/run.sh <command>

  env                 load ci/build.env into GITHUB_ENV
  prepare-export      modpack checkout + bundle id
  export-languages    read ci/language.json → export_languages output
  prepare-game        xvfb + local FQE jar + HeadlessMC
  launch-export       headless forge export (needs FQE_JVM_FLAGS)
  package-export      tar quest-export/ → quest-export-<bundle_id>.tar.gz
  collect-export-debug
EOF
}

if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
  cmd="${1:-}"
  if [[ -z "$cmd" ]]; then
    usage >&2
    exit 1
  fi
  shift

  case "$cmd" in
    env) load_config "$@" ;;
    prepare-export) prepare_export "$@" ;;
    export-languages) export_languages "$@" ;;
    prepare-game) prepare_game "$@" ;;
    launch-export) launch_export "$@" ;;
    package-export) package_export "$@" ;;
    collect-export-debug) collect_export_debug "$@" ;;
    -h|--help|help) usage ;;
    *)
      echo "::error::Unknown command: $cmd" >&2
      usage >&2
      exit 1
      ;;
  esac
fi
