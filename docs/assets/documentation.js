(() => {
  const article = document.getElementById("documentation-content");
  // Allow keyboard readers to scroll long examples and tables independently.
  if (article) {
    article.querySelectorAll("pre, table").forEach((element) => {
      element.tabIndex = 0;
    });
  }
  const contents = document.getElementById("contents-list");
  const disclosure = document.querySelector(".page-contents");
  if (!article || !contents || !disclosure) {
    return;
  }

  // Keep the Markdown renderer's IDs, including IDs used by existing shared links.
  const headings = Array.from(article.querySelectorAll("h2[id], h3[id]"));
  if (headings.length === 0) {
    document.querySelector(".contents-panel").hidden = true;
    document.getElementById("main").classList.add("documentation-layout-without-contents");
    return;
  }

  const links = headings.map((heading) => {
    const item = document.createElement("li");
    item.className = `contents-depth-${heading.tagName.slice(1)}`;
    const link = document.createElement("a");
    link.href = `#${encodeURIComponent(heading.id)}`;
    link.textContent = heading.textContent;
    item.append(link);
    return { item, link };
  });
  contents.replaceChildren(...links.map(({ item }) => item));

  // Only change the disclosure when crossing the layout breakpoint. Readers can
  // still open or close it themselves while they remain in the same layout.
  const narrowLayout = window.matchMedia("(max-width: 56rem)");
  const updateDisclosure = () => {
    disclosure.open = !narrowLayout.matches;
  };
  updateDisclosure();
  narrowLayout.addEventListener("change", updateDisclosure);

  let updateQueued = false;
  const updateCurrentSection = () => {
    let current = 0;
    for (let index = 0; index < headings.length; index += 1) {
      if (headings[index].getBoundingClientRect().top <= 120) {
        current = index;
      }
    }
    links.forEach(({ link }, index) => {
      if (index === current) {
        link.setAttribute("aria-current", "location");
      } else {
        link.removeAttribute("aria-current");
      }
    });
    updateQueued = false;
  };
  const queueCurrentSection = () => {
    if (!updateQueued) {
      updateQueued = true;
      window.requestAnimationFrame(updateCurrentSection);
    }
  };
  window.addEventListener("scroll", queueCurrentSection, { passive: true });
  window.addEventListener("resize", queueCurrentSection);
  updateCurrentSection();
})();
