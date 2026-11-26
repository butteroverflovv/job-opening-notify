#!/usr/bin/env python3
"""Compile the production collector and run Java fixture tests without Android SDK."""
import pathlib, shutil, subprocess, tempfile
root = pathlib.Path(__file__).resolve().parents[1]
java = shutil.which('java')
if not java:
    raise SystemExit('Java 17 or newer is required')
with tempfile.TemporaryDirectory(prefix='history-core-') as output:
    sources = sorted((root / 'app/src/main/java/kr/personal/historyjobs/core').glob('*.java'))
    subprocess.run([java, 'com.sun.tools.javac.Main', '-source', '17', '-target', '17', '-encoding', 'UTF-8', '-d', output, *map(str, sources), str(root / 'tests/CoreTests.java')], check=True)
    subprocess.run([java, '-cp', output, 'CoreTests'], check=True)
