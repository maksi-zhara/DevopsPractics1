async function loadGreeting() {
  const title = document.getElementById("title");
  const endpoint = document.getElementById("endpoint");
  const apiBaseUrl = window.APP_CONFIG?.API_BASE_URL || "/api";
  const helloUrl = `${apiBaseUrl}/hello`;
  endpoint.textContent = helloUrl;

  try {
    const response = await fetch(helloUrl);
    if (!response.ok) {
      throw new Error(`HTTP ${response.status}`);
    }

    const data = await response.json();
    title.textContent = data.message;
  } catch (error) {
    title.textContent = "Backend unavailable";
    console.error("Failed to load greeting:", error);
  }
}

loadGreeting();
