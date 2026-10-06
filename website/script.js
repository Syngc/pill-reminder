// Spanish is written in the HTML; English is swapped in here. The choice is remembered per browser.
const TEXT = {
  es: {
    title: "MedRing · Recordatorios que suenan y hablan",
    description: "App para Android que ayuda a las personas mayores a tomar sus medicinas a tiempo: suena como una alarma y lee las indicaciones en voz alta.",
    skip: "Ir al contenido",
    name: "MedRing",
    eyebrow: "Para personas mayores y sus familias",
    headline: "Sus medicinas, a tiempo.",
    lead: "Un familiar toma una foto de la receta y la app lee cada medicina, su dosis y sus horas. A la hora de cada toma, el teléfono suena aunque esté bloqueado, lee las indicaciones en voz alta y la persona confirma con un solo botón grande.",
    download: "Descargar para Android",
    downloadNote: "Gratis · Android 8.0 o más reciente · Archivo APK",
    phoneLabel: "Pantalla de alarma de la app: 8:00 p. m., Es hora de tu medicina, Losartán 50 mg, 1 tableta, botón Ya me la tomé",
    pm: "p. m.",
    mockTitle: "Es hora de tu medicina",
    mockDose: "1 tableta",
    mockButton: "YA ME LA TOMÉ",
    howTitle: "Cómo funciona",
    step1n: "Paso 1", step1t: "Foto de la receta",
    step1d: "Un familiar fotografía la receta y la inteligencia artificial lee las medicinas, dosis y horarios.",
    step2n: "Paso 2", step2t: "La familia revisa",
    step2d: "Ninguna alarma se activa hasta que alguien compara cada dato con el papel y lo confirma.",
    step3n: "Paso 3", step3t: "Suena a la hora",
    step3d: "Alarma en pantalla completa, voz en español o inglés y un solo botón grande para confirmar.",
    featuresTitle: "Pensada para leerse fácil",
    f1: "Letra grande y alto contraste",
    f2: "Español e inglés",
    f3: "Funciona con el teléfono bloqueado",
    f4: "Repite el mensaje cuando lo pidas",
    f5: "Solo repite lo que dice la receta",
    installTitle: "Cómo instalarla",
    i1: "Toca «Descargar para Android» desde el teléfono.",
    i2: "Abre el archivo descargado. Si Android lo pide, permite instalar apps desde ese navegador («Instalar apps desconocidas»).",
    i3: "Abre MedRing y permite las notificaciones y la alarma en pantalla completa.",
    i4: "Toca «Probar alarma» para escuchar cómo suena.",
    privacyTitle: "Tu información",
    privacy: "Las medicinas se guardan solo en tu teléfono. La foto de la receta se envía de forma segura para leerla y no se guarda en nuestro servidor.",
    disclaimer: "Esta app solo repite lo que dice la receta. No da consejos médicos. Ante cualquier duda, consulta a tu médico o farmacéutico.",
    credits: "Hecha para el ML Empowerment Build Challenge. Tipografía Atkinson Hyperlegible Next (licencia SIL Open Font).",
  },
  en: {
    title: "MedRing · Reminders that ring and speak",
    description: "Android app that helps older adults take their medicines on time: it rings like an alarm and reads the instructions out loud.",
    skip: "Skip to content",
    name: "MedRing",
    eyebrow: "For older adults and their families",
    headline: "Their medicines, on time.",
    lead: "A family member takes a photo of the prescription, and the app reads each medicine, its dose and its times. At every dose, the phone rings even when locked, reads the instructions out loud, and the person confirms with one big button.",
    download: "Download for Android",
    downloadNote: "Free · Android 8.0 or newer · APK file",
    phoneLabel: "The app's alarm screen: 8:00 PM, Time for your medicine, Losartan 50 mg, 1 tablet, I took it button",
    pm: "PM",
    mockTitle: "Time for your medicine",
    mockDose: "1 tablet",
    mockButton: "I TOOK IT",
    howTitle: "How it works",
    step1n: "Step 1", step1t: "Photo of the prescription",
    step1d: "A family member photographs the prescription and AI reads the medicines, doses and times.",
    step2n: "Step 2", step2t: "The family checks",
    step2d: "No alarm is turned on until someone compares every detail with the paper and confirms it.",
    step3n: "Step 3", step3t: "It rings on time",
    step3d: "A full-screen alarm, a voice in English or Spanish, and one big button to confirm.",
    featuresTitle: "Made to be easy to read",
    f1: "Large text and high contrast",
    f2: "English and Spanish",
    f3: "Works with the phone locked",
    f4: "Repeats the message when asked",
    f5: "Only repeats what the prescription says",
    installTitle: "How to install it",
    i1: "Tap “Download for Android” on the phone.",
    i2: "Open the downloaded file. If Android asks, allow installing apps from that browser (“Install unknown apps”).",
    i3: "Open MedRing and allow notifications and the full-screen alarm.",
    i4: "Tap “Test alarm” to hear how it sounds.",
    privacyTitle: "Your information",
    privacy: "Medicines are stored only on your phone. The prescription photo is sent securely to be read and is not stored on our server.",
    disclaimer: "This app only repeats what the prescription says. It does not give medical advice. If you have any questions, ask your doctor or pharmacist.",
    credits: "Made for the ML Empowerment Build Challenge. Typeface: Atkinson Hyperlegible Next (SIL Open Font License).",
  },
};

function storedLanguage() {
  try {
    return localStorage.getItem("language");
  } catch {
    return null;
  }
}

function apply(language) {
  const text = TEXT[language];
  document.documentElement.lang = language;
  document.title = text.title;
  document.querySelector('meta[name="description"]').setAttribute("content", text.description);
  document.querySelectorAll("[data-i18n]").forEach((el) => {
    el.textContent = text[el.dataset.i18n];
  });
  document.querySelectorAll("[data-i18n-label]").forEach((el) => {
    el.setAttribute("aria-label", text[el.dataset.i18nLabel]);
  });
  document.querySelectorAll("[data-lang]").forEach((button) => {
    button.setAttribute("aria-pressed", String(button.dataset.lang === language));
  });
}

document.querySelectorAll("[data-lang]").forEach((button) => {
  button.addEventListener("click", () => {
    const language = button.dataset.lang;
    try {
      localStorage.setItem("language", language);
    } catch {
      // Private browsing: the choice just isn't remembered.
    }
    apply(language);
  });
});

// Saved choice first, then the browser's language; Spanish otherwise, like the app.
// A link can pick the language (?lang=en); then the saved choice; then the browser's language;
// Spanish otherwise, like the app.
const linked = new URLSearchParams(location.search).get("lang");
const saved = storedLanguage();
const browser = (navigator.language || "").toLowerCase().startsWith("en") ? "en" : "es";
apply([linked, saved, browser].find((language) => TEXT[language]) || "es");
