const defaultProject = {
  "index.html": `<!doctype html>
<html>
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>My Vibe Site</title>
    <link rel="stylesheet" href="style.css" />
  </head>
  <body>
    <main>
      <h1>Welcome to your vibe-coded website ✨</h1>
      <p>Edit any file, add new files, and preview instantly.</p>
      <button id="cta">Click me</button>
    </main>
    <script src="script.js"></script>
  </body>
</html>`,
  "style.css": `body { font-family: system-ui, sans-serif; margin: 0; background: #f5f7ff; color: #1f2748; }
main { max-width: 720px; margin: 8vh auto; padding: 2rem; background: white; border-radius: 16px; box-shadow: 0 10px 25px rgba(0,0,0,.08); }
h1 { margin-top: 0; }
button { border: 0; border-radius: 10px; padding: 0.7rem 1.1rem; background: #4e6bff; color: white; cursor: pointer; }`,
  "script.js": `document.getElementById('cta')?.addEventListener('click', () => {
  alert('Your generated site is interactive!');
});`
};

const fileList = document.getElementById("fileList");
const codeEditor = document.getElementById("codeEditor");
const previewFrame = document.getElementById("previewFrame");
const publishOutput = document.getElementById("publishOutput");
const promptInput = document.getElementById("promptInput");

let project = loadProject();
let selectedFile = "index.html";

function loadProject() {
  const stored = localStorage.getItem("vibe_project");
  if (!stored) return { ...defaultProject };

  try {
    return JSON.parse(stored);
  } catch {
    return { ...defaultProject };
  }
}

function saveProject() {
  localStorage.setItem("vibe_project", JSON.stringify(project));
}

function renderFileList() {
  fileList.innerHTML = "";

  Object.keys(project).forEach((filename) => {
    const item = document.createElement("li");
    item.textContent = filename;
    item.className = filename === selectedFile ? "active" : "";
    item.addEventListener("click", () => {
      selectedFile = filename;
      codeEditor.value = project[filename];
      renderFileList();
    });
    fileList.appendChild(item);
  });

  if (!project[selectedFile]) {
    selectedFile = Object.keys(project)[0];
  }

  codeEditor.value = project[selectedFile] || "";
}

function buildPreviewDocument() {
  const html = project["index.html"] || "<h1>Create index.html to preview your website.</h1>";
  const css = project["style.css"] || "";
  const js = project["script.js"] || "";

  if (html.includes("</head>")) {
    return html.replace("</head>", `<style>${css}</style></head>`).replace("</body>", `<script>${js}\/script></body>`);
  }

  return `<!doctype html><html><head><style>${css}</style></head><body>${html}<script>${js}\/script></body></html>`;
}

function updatePreview() {
  previewFrame.srcdoc = buildPreviewDocument();
}

document.getElementById("previewBtn").addEventListener("click", updatePreview);

codeEditor.addEventListener("input", () => {
  project[selectedFile] = codeEditor.value;
  saveProject();
});

document.getElementById("newFileBtn").addEventListener("click", () => {
  const name = prompt("New file name (e.g. about.html, app.js):");
  if (!name || project[name]) return;
  project[name] = "";
  selectedFile = name;
  saveProject();
  renderFileList();
});

document.getElementById("deleteFileBtn").addEventListener("click", () => {
  if (Object.keys(project).length <= 1) {
    alert("You need at least one file in the project.");
    return;
  }

  if (!confirm(`Delete ${selectedFile}?`)) return;
  delete project[selectedFile];
  selectedFile = Object.keys(project)[0];
  saveProject();
  renderFileList();
  updatePreview();
});

document.getElementById("generateBtn").addEventListener("click", () => {
  const prompt = promptInput.value.trim();
  const promptText = prompt || "a modern startup";

  project["index.html"] = `<!doctype html>
<html>
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>${promptText}</title>
    <link rel="stylesheet" href="style.css" />
  </head>
  <body>
    <section class="hero">
      <h1>${promptText}</h1>
      <p>Generated instantly in VibeBuilder AI. Customize every file with no limits.</p>
      <button>Get Started</button>
    </section>
    <script src="script.js"></script>
  </body>
</html>`;

  project["style.css"] = `:root { color-scheme: light; }
body { margin: 0; font-family: Inter, Arial, sans-serif; background: linear-gradient(130deg, #eef2ff, #ffffff); color: #222; }
.hero { min-height: 100vh; display: grid; place-content: center; text-align: center; padding: 2rem; }
h1 { font-size: clamp(2rem, 5vw, 4rem); margin: 0; }
p { max-width: 55ch; margin: 1rem auto; color: #475569; }
button { border: none; padding: 0.8rem 1.2rem; border-radius: 999px; background: #4f46e5; color: #fff; font-weight: 700; }`;

  project["script.js"] = `document.querySelector('button')?.addEventListener('click', () => {
  alert('Welcome! Keep editing to make this site yours.');
});`;

  selectedFile = "index.html";
  saveProject();
  renderFileList();
  updatePreview();
});

document.getElementById("publishBtn").addEventListener("click", () => {
  const publishId = crypto.randomUUID();
  localStorage.setItem(`published_${publishId}`, JSON.stringify(project));

  const link = `${location.origin}${location.pathname}?published=${publishId}`;
  const template = document.getElementById("publishedTemplate").content.cloneNode(true);
  const anchor = template.getElementById("publishedLink");
  anchor.href = link;
  anchor.textContent = link;

  publishOutput.innerHTML = "";
  publishOutput.classList.remove("hidden");
  publishOutput.appendChild(template);
});

function loadPublishedSiteIfPresent() {
  const params = new URLSearchParams(location.search);
  const publishId = params.get("published");
  if (!publishId) return;

  const published = localStorage.getItem(`published_${publishId}`);
  if (!published) return;

  try {
    const publishedProject = JSON.parse(published);
    document.body.innerHTML = "";
    document.write((publishedProject["index.html"] || "")
      .replace("</head>", `<style>${publishedProject["style.css"] || ""}</style></head>`)
      .replace("</body>", `<script>${publishedProject["script.js"] || ""}\\/script></body>`));
    document.close();
  } catch {
    console.error("Invalid published site.");
  }
}

loadPublishedSiteIfPresent();
renderFileList();
updatePreview();
