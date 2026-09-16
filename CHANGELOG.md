# 変更履歴

## fold8-support — 2026年9月16日（フォーク / fork）

Galaxy Z Fold8 SM-F9710 (Android 17 / One UI 9) support in this fork. English notes: see [README.md](README.md#galaxy-z-fold8-sm-f9710-this-fork) and [docs/fold8-sm-f9710.md](docs/fold8-sm-f9710.md).

- Allow SM-F9710 alongside SM-F966Z through `DeviceSupport`.
- Accept the Fold8 wallpaper angle log format in `AngleLog`, with unit tests.
- Accept the Fold8 stock cover image URI, which has no `.png` suffix.
- Move the Folduo home with `startActivityFromRecents` on Fold8, where `moveTaskToRootTask` into a HOME root is rejected; never move Samsung's launcher task.
- Add `tools/fold8-probe.sh`, a read-only ADB probe.
- 40 unit tests pass. Checked on one SM-F9710: fold transition, dual display handoff, and the Folduo home moving between displays.

## 0.1.21 — 2026年9月15日

- 内側の左画面が横へ伸びる補正を廃止。表示位置と幅を保ったまま、左端ほどぼける描画へ変更。
- 上下の角度補正を穏やかにし、折り目付近を読み取りやすいぼかしへ調整。
- 右側の鮮明な表示、前面の演出、両画面の切り替え、角度取得経路は維持。
- 単体試験35件、Fold7のGPU・プレビュー試験20件が成功。実際の開閉での最終的な見え方は確認待ち。

## 0.1.17 — 2026年9月14日

- 任意で選べるFolduoホームを追加。英語・日本語の表示、アプリ一覧、アイコンの入れ替えに対応。
- 内側ホームで選んだアプリだけを操作中の画面へ移し、ホームへの復帰と両画面間のホームの引き継ぎを修正。
- 開閉演出の投影・ぼかし計算と角度取得経路は維持。実際の開閉を含む最終確認は未完了。

## 0.1.16 — 2026年9月13日

- アプリ内にEnglish・日本語・端末設定に合わせる言語選択を追加。Androidのアプリ別言語設定と連動し、再起動後も維持。
- 設定、プレビュー、通知、内側の操作バー、センサー診断、停止・回復の案内を英語に対応。
- 言語変更時は通知と操作バーを更新し、角度の取得や画面制御を再起動しない。
- 開閉演出の計算、角度取得経路、パッケージ識別子、署名は維持。

## 0.1.15 — 2026年9月13日

- Folduoのアプリ、設定画面、通知、前面壁紙の設定補助を用意。
- `bunkaich/Folduo` にソース、配布物、英語・日本語の再現手順を用意。
