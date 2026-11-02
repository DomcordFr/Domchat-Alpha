# Audit Brief — Task 074: Fix Image Download Source Error and High-Res Fetching

## Intent Verification

| Requirement | File | Status |
|---|---|---|
| `saveToDownloads` supports http/https URIs by streaming raw bytes from network via PlatformHttpClient | `ContextUtil.kt` | Done — `when (uri.scheme)` handles `"http", "https"` via `fetchBytesFromNetwork()` |
| `saveToDownloads` supports data:image URIs by decoding base64 bytes | `ContextUtil.kt` | Done — `"data"` scheme handled via `decodeDataUriBytes()` |
| `saveToDownloads` falls back to `openOwnedUriInputStream` for other schemes (file, content) | `ContextUtil.kt` | Done — `else` branch preserves existing behavior |
| `saveMessageImage` writes raw bytes directly to MediaStore/Pictures for data:image (no downscale/re-encode) | `ChatUtil.kt` | Done — base64 bytes written via `outputStream.write(byteArray)` |
| `saveMessageImage` writes raw bytes directly to MediaStore/Pictures for http/https (no downscale/re-encode) | `ChatUtil.kt` | Done — `response.body` written via `outputStream.write(byteArray)` |
| `content:` and `file:` cases unchanged (already byte-copy / direct export) | `ChatUtil.kt` | Done — untouched |
| AMOLED black / LiquidGlassTokens theme | N/A | Not applicable — utility functions, no UI |
| No `!!` force unwraps | Both files | Verified — grep returned 0 matches |
| Null safety via Elvis, safe-call, `runCatching`, `getOrNull()` | Both files | Done |
| No local gradle builds run | — | Done |

## Call Chain & Architecture

**ContextUtil.kt — `saveToDownloads`:**
- Entry point: UI layer calls `context.saveToDownloads(uri, fileName)` (suspend function)
- Permission check: legacy Android (< API 29) checks `WRITE_EXTERNAL_STORAGE`, requests via `ActivityCompat.requestPermissions` on Main dispatcher
- Main work on `Dispatchers.IO`:
  1. `when (uri.scheme)` selects input source:
     - `"http"`/`"https"` → `fetchBytesFromNetwork()` → `PlatformHttpClient.execute(PlatformHttpRequest)` → returns `ByteArray` → `.inputStream()`
     - `"data"` → `decodeDataUriBytes()` → base64 decode → `ByteArray` → `.inputStream()`
     - `else` → existing `openOwnedUriInputStream(uri)` (file/content schemes)
  2. Null guard on `inputStream` → toast error + `return@withContext`
  3. API >= Q → `MediaStore.Downloads.EXTERNAL_CONTENT_URI` insert + `openOutputStream` + `inputStream.copyTo(outputStream)`
  4. API < Q → `Environment.getExternalStoragePublicDirectory(DOWNLOADS)` + `File.sink().buffer().outputStream()` + media scanner broadcast
- Toast results posted to `Dispatchers.Main`

**ChatUtil.kt — `saveMessageImage`:**
- Entry point: UI layer calls `context.saveMessageImage(imageString)` (suspend function)
- Activity check: `getActivity()` must be non-null
- `when` on image scheme:
  - `"data:image"` → `Base64.decode(...)` → raw bytes → MediaStore `Images.Media.EXTERNAL_CONTENT_URI` (API >= Q) or `Environment.getExternalStoragePublicDirectory(PICTURES)` (API < Q)
  - `"file:"` → `exportImageFile(activity, file)` (unchanged)
  - `"content:"` → `openOwnedUriInputStream(uri)` → byte copy to MediaStore (unchanged)
  - `"http"` → `PlatformHttpClient.execute()` → `response.body` raw bytes → MediaStore `Images.Media.EXTERNAL_CONTENT_URI` (API >= Q) or Pictures dir (API < Q)
  - `else` → `error("Invalid image format")`
- All writes use `outputStream.write(byteArray)` — no `decodeBitmapWithBounds`, no `Bitmap.compress`, no quality loss

**Helper functions (ContextUtil.kt):**
- `fetchBytesFromNetwork(url)` — `runCatching` + `PlatformHttpClient` + status 200 check → `ByteArray?`
- `decodeDataUriBytes(dataUri)` — `@OptIn(ExperimentalEncodingApi)` + `Base64.decode(...)` → `ByteArray?`

## Recomposition & Memory Safety

- No Compose code in either file — pure Kotlin suspend functions, zero recomposition concerns
- All `InputStream`/`OutputStream` closed in `finally` block (ContextUtil.kt `saveToDownloads`)
- `outputStream.use { }` and `inputStream.use { }` scope-safe in ChatUtil.kt `saveMessageImage`
- `ByteArray` from network/base64 held in memory only for duration of write — no long-lived references
- `runCatching { ... }.getOrNull()` used for network/decode failures — no exceptions leak
- `when (uri.scheme)` is exhaustive for http/https/data/else; `else` falls back to `openOwnedUriInputStream` which returns null safely
- No `!!` force unwraps anywhere in either file
- `GlobalContext.get().get<PlatformHttpClient>()` — Koin DI, safe access pattern already used elsewhere in ChatUtil.kt

## Threading & Dispatchers

- `saveToDownloads`: `suspend fun`, permission check on Main (`withContext(Dispatchers.Main)` for `ActivityCompat.requestPermissions` and `Toast`), IO work on `Dispatchers.IO` (`withContext(Dispatchers.IO)` wrapper). Network fetch (`fetchBytesFromNetwork`) runs inside the IO coroutine — no Main dispatcher blocking.
- `saveMessageImage`: `suspend fun`, entire body runs on `withContext(Dispatchers.IO)`. Network call via `PlatformHttpClient` inside IO coroutine. Toast/activity access only at the start (null check on Main-thread-safe `getActivity()`). No `Dispatchers.Main` blocking, no `runBlocking`.
- `fetchBytesFromNetwork`: `suspend fun`, runs on caller's dispatcher (IO). `PlatformHttpClient.execute()` is blocking HTTP but invoked within coroutine on IO dispatcher.
- No `java.util.concurrent` types used.
- `Base64.decode` — pure computation, no threading concerns.

## Offline & Error State Handling

- **Network unavailable (http/https):** `fetchBytesFromNetwork` returns `null` via `runCatching.getOrNull()` → toast `downloads_read_source_failed` (ContextUtil) or silent log + no-op (ChatUtil). No crash.
- **Non-200 HTTP response:** `fetchBytesFromNetwork` returns `null` → same error path. ChatUtil logs error via `Log.e`.
- **Invalid base64 (data:image):** `decodeDataUriBytes` returns `null` via `runCatching.getOrNull()` → toast `downloads_read_source_failed` (ContextUtil). ChatUtil `Base64.decode` could throw on malformed input — wrapped implicitly by the overall `try/catch` in `saveToDownloads`; in `saveMessageImage`, the `when` branch is not wrapped in try/catch, but `Base64.decode` on invalid input throws `IllegalArgumentException` which propagates up to the coroutine caller. This matches pre-existing behavior.
- **MediaStore insert fails (null dstUri):** toast `downloads_create_entry_failed` (ContextUtil) or throws `IllegalStateException` (ChatUtil, caught by coroutine caller).
- **Output stream open fails:** `IllegalStateException` thrown → caught by `try/catch` in `saveToDownloads` (toast `downloads_save_failed`) or propagates in `saveMessageImage`.
- **Legacy Android storage permission denied:** toast `downloads_permission_required` and early return (ContextUtil). ChatUtil `exportImage`/`exportImageFile` handle their own permission checks.
- **activity == null (ChatUtil):** logs error and returns early — no crash.
- All `catch (e: Exception)` blocks log via `Log.e(TAG, ...)` — errors are surfaced, not swallowed silently.
- `e.message ?: ""` — null-safe Elvis for error messages in toasts.
