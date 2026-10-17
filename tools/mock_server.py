"""Local mock update server for TiebaLite in-app update acceptance (P2 reachability).

Serves both endpoints the app uses:
  GET /repos/min09577/TiebaLite/releases/latest   (stable channel, single object)
  GET /repos/min09577/TiebaLite/releases?per_page=10  (ai channel, array)
  GET /release-*.apk                              (fake apk payload)

Usage (emulator/MuMu):
  python mock_server.py 8000
  adb reverse tcp:8000 tcp:8000   # 设备 localhost:8000 -> 宿主 8000（配 gradle 默认 mock 根）
  # 或模拟器直连宿主别名: mockRoot 传 http://10.0.2.2:8000/（需 debug 构建的明文放行）
  adb shell am start -n com.huanchengfly.tieba.post/.MainActivityV2 \
      --ez tieba.update.mock true [--es tieba.update.mockRoot http://10.0.2.2:8000/]
"""
import json
import os
import sys
from http.server import BaseHTTPRequestHandler, HTTPServer

ROOT = os.path.dirname(os.path.abspath(__file__))
RELEASE = json.load(open(os.path.join(ROOT, "mock_release.json"), encoding="utf-8"))
FAKE_APK = b"PK\x03\x04mock-apk-payload-for-reachability-test-only"


class Handler(BaseHTTPRequestHandler):
    def _send(self, code, body, ctype):
        self.send_response(code)
        self.send_header("Content-Type", ctype)
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def do_GET(self):
        print(f"[mock] GET {self.path}", flush=True)
        # 归一化：容忍尾斜杠根产生的 // 与带 /repos/<owner>/<repo> 前缀的完整 API 根两种传法
        path = self.path.split("?")[0]
        while "//" in path:
            path = path.replace("//", "/")
        if path.startswith("/repos/min09577/TiebaLite"):
            path = path[len("/repos/min09577/TiebaLite"):]
        if path in ("/releases/latest", ""):
            self._send(200, json.dumps(RELEASE).encode("utf-8"), "application/json")
        elif path == "/releases":
            self._send(200, json.dumps([RELEASE]).encode("utf-8"), "application/json")
        elif path.startswith("/release-"):
            self._send(200, FAKE_APK, "application/vnd.android.package-archive")
        else:
            self._send(404, b"not found", "text/plain")

    def log_message(self, *args):
        pass


if __name__ == "__main__":
    port = int(sys.argv[1]) if len(sys.argv) > 1 else 8000
    HTTPServer(("127.0.0.1", port), Handler).serve_forever()
