package com.myapp.logging;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * GET /console
 *
 * Serves the log console page: a form that POSTs to LogIngestServlet ("/logs")
 * and a search bar that GETs LogSearchServlet ("/logs/search"). The page itself
 * is just a servlet writing HTML, same as HelloServlet - no static files.
 */
@WebServlet("/console")
public class LogConsoleServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("text/html");
        response.getWriter().println(PAGE);
    }

    private static final String PAGE = """
            <!DOCTYPE html>
            <html lang="en">
            <head>
            <meta charset="UTF-8">
            <title>FinanceBudge - Log Console</title>
            <style>
                body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Arial, sans-serif;
                       max-width: 760px; margin: 2rem auto; padding: 0 1rem; }
                section { border: 1px solid #ccc; border-radius: 8px; padding: 1.25rem 1.5rem; margin-bottom: 1.5rem; }
                label { display: block; font-size: 0.8rem; color: #555; margin-bottom: 0.3rem; }
                .field { margin-bottom: 1rem; }
                select, input[type="text"], textarea { width: 100%; padding: 0.5rem; box-sizing: border-box; }
                textarea { min-height: 4.5rem; }
                .row { display: flex; gap: 1rem; }
                .row .field { flex: 1; }
                .search-bar { display: flex; gap: 0.6rem; }
                .search-bar input { flex: 1; }
                button { padding: 0.5rem 1rem; cursor: pointer; }
                table { width: 100%; border-collapse: collapse; margin-top: 1rem; font-size: 0.85rem; }
                th, td { text-align: left; padding: 0.4rem; border-bottom: 1px solid #ddd; }
                .status { margin-top: 0.75rem; font-family: monospace; white-space: pre-wrap; }
                .status.error { color: #a11515; }
                .status.ok { color: #166534; }
            </style>
            </head>
            <body>
            <h1>FinanceBudge Log Console</h1>
            <p>Writes to logs/application.log. Logstash tails that file and ships new entries into OpenSearch ("app-logs") so they show up in the search below.</p>

            <section>
                <h2>Write a log entry &rarr; POST /logs</h2>
                <form id="log-form">
                    <div class="row">
                        <div class="field">
                            <label for="level">Level</label>
                            <select id="level" name="level">
                                <option value="DEBUG">DEBUG</option>
                                <option value="INFO" selected>INFO</option>
                                <option value="WARN">WARN</option>
                                <option value="ERROR">ERROR</option>
                            </select>
                        </div>
                        <div class="field">
                            <label for="source">Source</label>
                            <input type="text" id="source" name="source" placeholder="checkout-service" required>
                        </div>
                    </div>
                    <div class="field">
                        <label for="message">Message</label>
                        <textarea id="message" name="message" placeholder="Something happened..." required></textarea>
                    </div>
                    <button type="submit">Submit log</button>
                </form>
                <div id="log-status" class="status"></div>
            </section>

            <section>
                <h2>Search logs &rarr; GET /logs/search?q=...</h2>
                <form id="search-form" class="search-bar">
                    <input type="text" id="query" name="q" placeholder="Search message text, e.g. checkout" required>
                    <button type="submit">Search</button>
                </form>
                <div id="search-status" class="status"></div>
                <div id="results"></div>
            </section>

            <script>
                const logForm = document.getElementById('log-form');
                const logStatus = document.getElementById('log-status');
                const searchForm = document.getElementById('search-form');
                const searchStatus = document.getElementById('search-status');
                const resultsEl = document.getElementById('results');

                function showStatus(el, message, isError) {
                    el.textContent = message;
                    el.classList.remove('ok', 'error');
                    el.classList.add(isError ? 'error' : 'ok');
                }

                logForm.addEventListener('submit', async (event) => {
                    event.preventDefault();
                    const button = logForm.querySelector('button');
                    button.disabled = true;
                    const payload = {
                        level: document.getElementById('level').value,
                        source: document.getElementById('source').value,
                        message: document.getElementById('message').value
                    };
                    try {
                        const resp = await fetch('logs', {
                            method: 'POST',
                            headers: { 'Content-Type': 'application/json' },
                            body: JSON.stringify(payload)
                        });
                        const text = await resp.text();
                        if (!resp.ok) throw new Error('HTTP ' + resp.status + ': ' + text);
                        showStatus(logStatus, 'Written to log file -> ' + text.trim() + ' (Logstash will index it shortly)', false);
                        document.getElementById('message').value = '';
                    } catch (err) {
                        showStatus(logStatus, 'Failed to submit log: ' + err.message, true);
                    } finally {
                        button.disabled = false;
                    }
                });

                searchForm.addEventListener('submit', async (event) => {
                    event.preventDefault();
                    const button = searchForm.querySelector('button');
                    button.disabled = true;
                    resultsEl.innerHTML = '';
                    const q = document.getElementById('query').value;
                    try {
                        const resp = await fetch('logs/search?q=' + encodeURIComponent(q));
                        const text = await resp.text();
                        if (!resp.ok) throw new Error('HTTP ' + resp.status + ': ' + text);
                        searchStatus.textContent = '';
                        renderResults(JSON.parse(text));
                    } catch (err) {
                        showStatus(searchStatus, 'Search failed: ' + err.message, true);
                    } finally {
                        button.disabled = false;
                    }
                });

                function renderResults(hits) {
                    if (!hits || hits.length === 0) {
                        resultsEl.innerHTML = '<p>No matching log entries.</p>';
                        return;
                    }
                    const rows = hits.map((hit) => `
                        <tr>
                            <td>${escapeHtml(hit.timestamp || '')}</td>
                            <td>${escapeHtml(hit.level || '')}</td>
                            <td>${escapeHtml(hit.message || '')}</td>
                            <td>${escapeHtml(hit.source || '')}</td>
                        </tr>
                    `).join('');
                    resultsEl.innerHTML = `
                        <table>
                            <thead><tr><th>Timestamp</th><th>Level</th><th>Message</th><th>Source</th></tr></thead>
                            <tbody>${rows}</tbody>
                        </table>
                    `;
                }

                function escapeHtml(value) {
                    return String(value).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
                }
            </script>
            </body>
            </html>
            """;
}
