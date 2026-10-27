---
layout: default
title: Documentation
---

# snAp

<img width="244" height="244" alt="snAp" src="https://i.imgur.com/cBTLckS.png" />

Useful links:
* [User Guide](UserGuide.md)
* [Developer Guide](DeveloperGuide.md)
* [About Us](AboutUs.md)

## Documentation layout

The documentation uses a shared Jekyll layout for the header, navigation and footer. Edit the Markdown
pages directly, retaining their `layout: default` and `title` front matter.

Long guides show a contents list built from their rendered second- and third-level headings. The list stays
visible beside the guide on wide screens and can be expanded or collapsed on narrow screens. It uses the
actual heading IDs, so section links remain usable without JavaScript. Short pages omit the contents list
when there are no relevant headings.

## Publishing

GitHub Pages publishes the `docs` directory from the `master` branch. Pages renders the Jekyll layout and
Markdown after changes are merged. No npm build is required. Review the rendered pages before merging,
including the navigation, contents links, images and tables at wide and narrow widths.

For local preview, install Ruby and the [GitHub Pages dependencies](https://pages.github.com/versions/),
then run this command from the repository root:

```sh
jekyll serve --source docs --destination _temp/docs-preview --baseurl /tp
```

Open `http://127.0.0.1:4000/tp/`. The destination keeps generated files outside the published documentation.
