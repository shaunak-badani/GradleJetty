package com.myapp.cassandra;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * GET /cassandra/console
 *
 * A form that POSTs to CassandraWriteServlet ("/cassandra/events") and a search box that GETs
 * CassandraQueryServlet ("/cassandra/events/search?source=..."). The search box only takes a
 * source, not a free-text query - that's deliberate, it's the partition key in action.
 */
@WebServlet("/cassandra/console")
public class CassandraConsoleServlet extends HttpServlet {

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
            <title>FinanceBudge - Cassandra Console</title>
            <style>
                body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Arial, sans-serif;
                       max-width: 760px; margin: 2rem auto; padding: 0 1rem; }
                section { border: 1px solid #ccc; border-radius: 8px; padding: 1.25rem 1.5rem; margin-bottom: 1.5rem; }
                label { display: block; font-size: 0.8rem; color: #555; margin-bottom: 0.3rem; }
                .field { margin-bottom: 1rem; }
                input[type="text"] { width: 100%; padding: 0.5rem; box-sizing: border-box; }
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
                .hint { color: #777; font-size: 0.85rem; }
            </style>
            </head>
            <body>
            <h1>FinanceBudge Cassandra Console</h1>
            <p>Rows are keyed by (source, created_at): "source" is the <b>partition key</b> (decides which node
            owns the row), "created_at" is a <b>clustering column</b> (sorts rows within a partition, newest
            first). You can only look up rows by source - that's a Cassandra table design choice, not a
            limitation of this page.</p>

            <section>
                <h2>Insert &rarr; POST /cassandra/events</h2>
                <form id="insert-form">
                    <div class="row">
                        <div class="field">
                            <label for="source">Source (partition key)</label>
                            <input type="text" id="source" name="source" placeholder="checkout-service" required>
                        </div>
                        <div class="field">
                            <label for="message">Message</label>
                            <input type="text" id="message" name="message" placeholder="order placed" required>
                        </div>
                    </div>
                    <button type="submit">Insert</button>
                </form>
                <div id="insert-status" class="status"></div>
            </section>

            <section>
                <h2>Query a partition &rarr; GET /cassandra/events/search?source=...</h2>
                <p class="hint">Returns every row for this source, newest first - no cross-partition filtering.</p>
                <form id="search-form" class="search-bar">
                    <input type="text" id="query" name="source" placeholder="checkout-service" required>
                    <button type="submit">Search</button>
                </form>
                <div id="search-status" class="status"></div>
                <div id="results"></div>
            </section>

            <script>
                const insertForm = document.getElementById('insert-form');
                const insertStatus = document.getElementById('insert-status');
                const searchForm = document.getElementById('search-form');
                const searchStatus = document.getElementById('search-status');
                const resultsEl = document.getElementById('results');

                function showStatus(el, message, isError) {
                    el.textContent = message;
                    el.classList.remove('ok', 'error');
                    el.classList.add(isError ? 'error' : 'ok');
                }

                insertForm.addEventListener('submit', async (event) => {
                    event.preventDefault();
                    const button = insertForm.querySelector('button');
                    button.disabled = true;
                    const payload = {
                        source: document.getElementById('source').value,
                        message: document.getElementById('message').value
                    };
                    try {
                        const resp = await fetch('events', {
                            method: 'POST',
                            headers: { 'Content-Type': 'application/json' },
                            body: JSON.stringify(payload)
                        });
                        const text = await resp.text();
                        if (!resp.ok) throw new Error('HTTP ' + resp.status + ': ' + text);
                        const inserted = JSON.parse(text);
                        showStatus(insertStatus, 'Inserted at ' + inserted.createdAt + ' under partition "' + inserted.source + '"', false);
                        document.getElementById('message').value = '';
                    } catch (err) {
                        showStatus(insertStatus, 'Failed to insert: ' + err.message, true);
                    } finally {
                        button.disabled = false;
                    }
                });

                searchForm.addEventListener('submit', async (event) => {
                    event.preventDefault();
                    const button = searchForm.querySelector('button');
                    button.disabled = true;
                    resultsEl.innerHTML = '';
                    const source = document.getElementById('query').value;
                    try {
                        const resp = await fetch('events/search?source=' + encodeURIComponent(source));
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
                        resultsEl.innerHTML = '<p>No rows for this partition.</p>';
                        return;
                    }
                    const rows = hits.map((hit) => `
                        <tr>
                            <td>${escapeHtml(hit.createdAt || '')}</td>
                            <td>${escapeHtml(hit.source || '')}</td>
                            <td>${escapeHtml(hit.message || '')}</td>
                        </tr>
                    `).join('');
                    resultsEl.innerHTML = `
                        <table>
                            <thead><tr><th>Created at</th><th>Source (partition)</th><th>Message</th></tr></thead>
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
