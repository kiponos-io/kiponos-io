#!/usr/bin/env python3
"""Scaffold a Super Pattern example: Java (Gradle) + Python + README stubs.

Usage:
  python3 scripts/scaffold_super_pattern_example.py pattern-chain-live-fraud
"""
from __future__ import annotations

import argparse
import shutil
import textwrap
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA_ROOT = ROOT / "examples" / "java"
PY_ROOT = ROOT / "examples" / "python"
TEMPLATE = JAVA_ROOT / "pattern-strategy-live-router"

BUILD_GRADLE = '''plugins {{
    id 'application'
    id 'java'
}}

java {{
    toolchain {{
        languageVersion = JavaLanguageVersion.of(17)
    }}
}}

group = 'io.kiponos.examples'
version = '1.0.0'

application {{
    mainClass.set('{main_class}')
}}

def localEnv = file("${{rootDir}}/kiponos.local.env")
def kiponosId = 'REPLACE_WITH_KIPONOS_ID_FROM_ACCOUNT'
def kiponosAccess = 'REPLACE_WITH_KIPONOS_ACCESS_FROM_ACCOUNT'
def kiponosProfile = "['my-app']['v1.0.0']['dev']['base']"

if (localEnv.exists()) {{
    localEnv.eachLine {{ line ->
        def t = line.trim()
        if (!t || t.startsWith('#')) {{
            return
        }}
        def i = t.indexOf('=')
        if (i < 1) {{
            return
        }}
        def k = t.substring(0, i).trim()
        def v = t.substring(i + 1).trim()
        if ((v.startsWith('"') && v.endsWith('"')) || (v.startsWith("'") && v.endsWith("'"))) {{
            v = v.substring(1, v.length() - 1)
        }}
        if (k == 'KIPONOS_ID') {{
            kiponosId = v
        }} else if (k == 'KIPONOS_ACCESS') {{
            kiponosAccess = v
        }} else if (k == 'KIPONOS') {{
            kiponosProfile = v
        }}
    }}
}} else {{
    if (System.getenv('KIPONOS_ID')) {{
        kiponosId = System.getenv('KIPONOS_ID')
    }}
    if (System.getenv('KIPONOS_ACCESS')) {{
        kiponosAccess = System.getenv('KIPONOS_ACCESS')
    }}
    if (System.getenv('KIPONOS')) {{
        kiponosProfile = System.getenv('KIPONOS')
    }}
}}

tasks.withType(JavaExec).configureEach {{
    environment 'KIPONOS_ID', kiponosId
    environment 'KIPONOS_ACCESS', kiponosAccess
    systemProperty 'kiponos', kiponosProfile
    systemProperty 'io.kiponos.host', 'kiponos.io'
}}

tasks.withType(Test).configureEach {{
    environment 'KIPONOS_ID', kiponosId
    environment 'KIPONOS_ACCESS', kiponosAccess
    systemProperty 'kiponos', kiponosProfile
    systemProperty 'io.kiponos.host', 'kiponos.io'
    if (kiponosId.startsWith('REPLACE_WITH_')) {{
        systemProperty 'kiponos.golden.skip', 'true'
    }}
}}

repositories {{
    mavenLocal()
    mavenCentral()
}}

dependencies {{
    implementation 'io.kiponos:sdk-boot-3:4.4.0.250319'

    testImplementation platform('org.junit:junit-bom:5.10.2')
    testImplementation 'org.junit.jupiter:junit-jupiter'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
}}

test {{
    useJUnitPlatform()
    testLogging {{
        events 'passed', 'skipped', 'failed'
        showStandardStreams = true
    }}
}}
'''

ENV_EXAMPLE = textwrap.dedent(
    """\
    # Copy to kiponos.local.env (gitignored) and fill from https://kiponos.io → Connect
    KIPONOS_ID=REPLACE_WITH_KIPONOS_ID_FROM_ACCOUNT
    KIPONOS_ACCESS=REPLACE_WITH_KIPONOS_ACCESS_FROM_ACCOUNT
    KIPONOS=['my-app']['v1.0.0']['dev']['base']
    """
)


def ensure_java_shell(example_id: str) -> Path:
    dest = JAVA_ROOT / example_id
    dest.mkdir(parents=True, exist_ok=True)
    if not (dest / "gradlew").exists():
        shutil.copytree(TEMPLATE / "gradle", dest / "gradle", dirs_exist_ok=True)
        shutil.copy2(TEMPLATE / "gradlew", dest / "gradlew")
        shutil.copy2(TEMPLATE / "gradlew.bat", dest / "gradlew.bat")
        (dest / "gradlew").chmod(0o755)
    (dest / "settings.gradle").write_text(f'rootProject.name = "{example_id}"\n')
    (dest / "kiponos.local.env.example").write_text(ENV_EXAMPLE)
    return dest


def write_build_gradle(dest: Path, main_class: str) -> None:
    (dest / "build.gradle").write_text(BUILD_GRADLE.format(main_class=main_class))


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("example_id")
    ap.add_argument("--main-class", required=True)
    args = ap.parse_args()
    dest = ensure_java_shell(args.example_id)
    write_build_gradle(dest, args.main_class)
    print(f"scaffolded {dest}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
