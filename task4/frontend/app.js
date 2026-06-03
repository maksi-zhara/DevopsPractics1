function renderPeople(people) {
  const list = document.getElementById("people-list");

  if (!people.length) {
    list.innerHTML = "<li>No records found</li>";
    return;
  }

  list.innerHTML = "";

  for (const person of people) {
    const item = document.createElement("li");
    item.className = "person-card";
    item.innerHTML = `
      <strong>${person.last_name} ${person.first_name} ${person.middle_name}</strong>
      <span>${person.age} years old</span>
    `;
    list.appendChild(item);
  }
}

async function loadPeople() {
  const title = document.getElementById("title");
  const endpoint = document.getElementById("endpoint");
  const apiBaseUrl = window.APP_CONFIG?.API_BASE_URL || "/api";
  const peopleUrl = `${apiBaseUrl}/people`;
  endpoint.textContent = peopleUrl;

  try {
    const response = await fetch(peopleUrl);
    if (!response.ok) {
      throw new Error(`HTTP ${response.status}`);
    }

    const data = await response.json();
    title.textContent = `${data.message} (${data.count})`;
    renderPeople(data.people || []);
  } catch (error) {
    title.textContent = "Backend unavailable";
    console.error("Failed to load people:", error);
  }
}

async function addRandomPerson() {
  const button = document.getElementById("add-person");
  const apiBaseUrl = window.APP_CONFIG?.API_BASE_URL || "/api";
  const randomPersonUrl = `${apiBaseUrl}/people/random`;

  button.disabled = true;
  button.textContent = "Adding...";

  try {
    const response = await fetch(randomPersonUrl, { method: "POST" });
    if (!response.ok) {
      throw new Error(`HTTP ${response.status}`);
    }

    await loadPeople();
  } catch (error) {
    console.error("Failed to add random person:", error);
  } finally {
    button.disabled = false;
    button.textContent = "Add Random Person";
  }
}

document.getElementById("add-person").addEventListener("click", addRandomPerson);
loadPeople();
