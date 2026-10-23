package com.myapp.kafka;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * GET /kafka/console
 *
 * A form that POSTs to KafkaProduceServlet ("/kafka/produce") and a table
 * that polls KafkaMessagesServlet ("/kafka/messages") every couple seconds.
 * Produce and consume happen through the broker, not through each other -
 * this page just makes that visible by showing both sides at once.
 */
@WebServlet("/kafka/console")
public class KafkaConsoleServlet extends HttpServlet {

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
            <title>FinanceBudge - Kafka Console</title>
            <style>
                body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Arial, sans-serif;
                       max-width: 760px; margin: 2rem auto; padding: 0 1rem; }
                section { border: 1px solid #ccc; border-radius: 8px; padding: 1.25rem 1.5rem; margin-bottom: 1.5rem; }
                label { display: block; font-size: 0.8rem; color: #555; margin-bottom: 0.3rem; }
                .field { margin-bottom: 1rem; }
                input[type="text"] { width: 100%; padding: 0.5rem; box-sizing: border-box; }
                .row { display: flex; gap: 1rem; }
                .row .field { flex: 1; }
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
            <h1>FinanceBudge Kafka Console</h1>
            <p>Produce a record &rarr; it lands on the broker's "app-events" topic &rarr; a separate background
            consumer thread (started when this webapp deployed) picks it up independently and it shows up below.
            Neither side calls the other directly.</p>

            <section>
                <h2>Produce &rarr; POST /kafka/produce</h2>
                <form id="produce-form">
                    <div class="row">
                        <div class="field">
                            <label for="key">Key</label>
                            <input type="text" id="key" name="key" placeholder="user-42" required>
                        </div>
                        <div class="field">
                            <label for="value">Value</label>
                            <input type="text" id="value" name="value" placeholder="signed up" required>
                        </div>
                    </div>
                    <button type="submit">Publish</button>
                </form>
                <div id="produce-status" class="status"></div>
            </section>

            <section>
                <h2>Consumed &larr; GET /kafka/messages</h2>
                <p class="hint">Auto-refreshes every 2s. Most recent first.</p>
                <div id="results"></div>
            </section>

            <script>
                const produceForm = document.getElementById('produce-form');
                const produceStatus = document.getElementById('produce-status');
                const resultsEl = document.getElementById('results');

                function showStatus(el, message, isError) {
                    el.textContent = message;
                    el.classList.remove('ok', 'error');
                    el.classList.add(isError ? 'error' : 'ok');
                }

                produceForm.addEventListener('submit', async (event) => {
                    event.preventDefault();
                    const button = produceForm.querySelector('button');
                    button.disabled = true;
                    const payload = {
                        key: document.getElementById('key').value,
                        value: document.getElementById('value').value
                    };
                    try {
                        const resp = await fetch('produce', {
                            method: 'POST',
                            headers: { 'Content-Type': 'application/json' },
                            body: JSON.stringify(payload)
                        });
                        const text = await resp.text();
                        if (!resp.ok) throw new Error('HTTP ' + resp.status + ': ' + text);
                        const sent = JSON.parse(text);
                        showStatus(produceStatus, 'Sent to partition ' + sent.partition + ', offset ' + sent.offset +
                            ' (waiting for the consumer to pick it up...)', false);
                        document.getElementById('value').value = '';
                    } catch (err) {
                        showStatus(produceStatus, 'Failed to publish: ' + err.message, true);
                    } finally {
                        button.disabled = false;
                    }
                });

                async function refresh() {
                    try {
                        const resp = await fetch('messages');
                        const hits = await resp.json();
                        renderResults(hits);
                    } catch (err) {
                        // transient - leave the last good render in place
                    }
                }

                function renderResults(hits) {
                    if (!hits || hits.length === 0) {
                        resultsEl.innerHTML = '<p>No records consumed yet.</p>';
                        return;
                    }
                    const rows = hits.map((hit) => `
                        <tr>
                            <td>${escapeHtml(hit.timestamp || '')}</td>
                            <td>${escapeHtml(hit.key || '')}</td>
                            <td>${escapeHtml(hit.value || '')}</td>
                            <td>${escapeHtml(hit.partition)}</td>
                            <td>${escapeHtml(hit.offset)}</td>
                        </tr>
                    `).join('');
                    resultsEl.innerHTML = `
                        <table>
                            <thead><tr><th>Consumed at</th><th>Key</th><th>Value</th><th>Partition</th><th>Offset</th></tr></thead>
                            <tbody>${rows}</tbody>
                        </table>
                    `;
                }

                function escapeHtml(value) {
                    return String(value).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
                }

                refresh();
                setInterval(refresh, 2000);
            </script>
            </body>
            </html>
            """;
}
