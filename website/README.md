# Website

A one-page landing site for the app: what it does, how it works, how to install it, and a download button. Plain HTML, CSS and a little JavaScript, with no build step.

- **Look:** the same colors as the app (light and dark, following the visitor's system setting) and the same typeface, Atkinson Hyperlegible Next. The font is served from `fonts/` with its license (`fonts/OFL.txt`), so visitors' browsers don't contact a third-party font service.
- **Languages:** Spanish is written in `index.html`; `script.js` swaps in English. The language comes from `?lang=es` / `?lang=en` in the link, then the visitor's last choice, then their browser's language.

## Preview locally

```bash
cd website
python3 -m http.server 8000
# open http://localhost:8000
```

## Download link

The button points to:

```
https://github.com/Syngc/pill-reminder/releases/latest/download/MisMedicinas.apk
```

GitHub serves this from the newest release, as long as that release has a file named exactly `MisMedicinas.apk`. To publish a new version:

1. In the repo on GitHub: **Releases → Draft a new release**.
2. Create a tag such as `v0.6.1`, and give the release a title.
3. Attach the APK, renamed to `MisMedicinas.apk`.
4. Publish. The website's button now downloads this version.

## Hosting

Published with GitHub Pages at **https://syngc.github.io/pill-reminder/** by `.github/workflows/pages.yml`, which redeploys whenever something in `website/` changes on `main`. It can also be run by hand from the repo's **Actions** tab (**Website → Run workflow**).

One-time setup: in the repo on GitHub, **Settings → Pages → Build and deployment → Source: GitHub Actions**.
