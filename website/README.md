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

The button points to the APK attached to the current release:

```
https://github.com/Syngc/pill-reminder/releases/download/v0.6.1/MisMedicinas-0.6.1.apk
```

This link is tied to one version. For each new release:

1. In the repo on GitHub: **Releases → Draft a new release**, create a tag such as `v0.6.2`, attach the APK (for example `MisMedicinas-0.6.2.apk`) and publish.
2. Update the `href` of the download button in `index.html` to the new file's link, then push. The site redeploys on its own.

To never edit the site again, attach the APK under the same name in every release, for example `MisMedicinas.apk`, and use `https://github.com/Syngc/pill-reminder/releases/latest/download/MisMedicinas.apk`, which always serves the newest release.

## Hosting

Published with GitHub Pages at **https://syngc.github.io/pill-reminder/** by `.github/workflows/pages.yml`, which redeploys whenever something in `website/` changes on `main`. It can also be run by hand from the repo's **Actions** tab (**Website → Run workflow**).

One-time setup: in the repo on GitHub, **Settings → Pages → Build and deployment → Source: GitHub Actions**.
