"""Check native UI at normal width and compact width with larger system text."""
from pathlib import Path
import subprocess

out = Path('ui-check')
out.mkdir(exist_ok=True)

def adb(*args):
    return subprocess.run(['adb', *args], check=True, text=True, capture_output=True).stdout

adb('install', '-r', 'app/build/outputs/apk/debug/app-debug.apk')
adb('install', '-r', 'app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk')
for name, size, density, scale in [('normal', '1080x2280', '420', '1.0'), ('compact-large-text', '960x1920', '480', '1.3')]:
    adb('shell', 'am', 'force-stop', 'kr.personal.historyjobs')
    adb('shell', 'wm', 'size', size)
    adb('shell', 'wm', 'density', density)
    adb('shell', 'settings', 'put', 'system', 'font_scale', scale)
    result = adb('shell', 'am', 'instrument', '-w', 'kr.personal.historyjobs.test/kr.personal.historyjobs.UiSmokeRunner')
    (out / f'{name}.txt').write_text(result)
    print(name, result, flush=True)
    adb('pull', '/sdcard/Android/data/kr.personal.historyjobs/files/ui-screenshots', str(out / name))
    if 'OK (1 test)' not in result:
        raise RuntimeError(f'{name}: UI check failed; see screenshots and log')
