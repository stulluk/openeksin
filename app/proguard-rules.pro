# Jsoup keeps reflection-free; no special rules needed for okhttp3 4.x.
# Keep model classes (parsed via scraper, but no reflection serialization here).
-dontwarn org.jsoup.**
