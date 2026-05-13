package ch.ethz.seb.sebserver.webservice.weblayer.api;

import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.model.user.UserFeatures;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.FeatureService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("${sebserver.webservice.api.admin.endpoint}")
public class SEBVersionController {

    private final FeatureService featureService;

    public SEBVersionController(final FeatureService featureService) {
        this.featureService = featureService;
    }

    @RequestMapping(
            path = API.SEB_VERSION_PAGE_ENDPOINT,
            method = RequestMethod.GET,
            produces = MediaType.TEXT_HTML_VALUE)
    public String sebVersionInfoPage(
            @RequestParam(name = API.SEB_VERSION_SELECTED_EXAM, required = true) final String examName,
            @RequestParam(name = API.SEB_VERSION, required = true) final String currentSEBVersion,
            @RequestParam(name = API.SEB_VERSION_RESTRICTION, required = true) final String restriction,
            @RequestParam(name = API.SEB_DOWNLOAD_LINK, required = true) final String download) {

        if (!featureService.isEnabledByConfig(UserFeatures.Feature.SEB_CLIENT_VERSION_RESTRICTION_REDIRECT)) {
            return "";
        }

        try {
            return String.format(HTML_PAGE, examName, currentSEBVersion, restriction, download);
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    private static final String HTML_PAGE = """
            <!DOCTYPE html>
            <html lang="en">
            <head>
              <meta charset="UTF-8" />
              <meta name="viewport" content="width=device-width, initial-scale=1.0" />
              <title>SEB Version Restriction</title>
              <link href="https://fonts.googleapis.com/css2?family=IBM+Plex+Mono:wght@400;600&family=IBM+Plex+Sans:wght@300;400;500;600&display=swap" rel="stylesheet" />
              <style>
                :root {
                  --bg: #0d1117;
                  --surface: #161b22;
                  --border: #21262d;
                  --accent: #f0883e;
                  --accent-dim: rgba(240, 136, 62, 0.12);
                  --danger: #f85149;
                  --danger-dim: rgba(248, 81, 73, 0.1);
                  --text-primary: #e6edf3;
                  --text-secondary: #8b949e;
                  --text-muted: #484f58;
                  --link: #58a6ff;
                  --success: #3fb950;
                }
            
                *, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }
            
                body {
                  font-family: 'IBM Plex Sans', sans-serif;
                  background: var(--bg);
                  color: var(--text-primary);
                  min-height: 100vh;
                  display: flex;
                  align-items: center;
                  justify-content: center;
                  padding: 2rem;
                  overflow-x: hidden;
                }
            
                /* Subtle grid background */
                body::before {
                  content: '';
                  position: fixed;
                  inset: 0;
                  background-image:
                    linear-gradient(rgba(240,136,62,0.03) 1px, transparent 1px),
                    linear-gradient(90deg, rgba(240,136,62,0.03) 1px, transparent 1px);
                  background-size: 40px 40px;
                  pointer-events: none;
                  z-index: 0;
                }
            
                .container {
                  position: relative;
                  z-index: 1;
                  width: 100%%;
                  max-width: 680px;
                }
            
                /* Header strip */
                .header-strip {
                  display: flex;
                  align-items: center;
                  gap: 0.75rem;
                  margin-bottom: 2rem;
                  opacity: 0;
                  animation: fadeUp 0.5s ease forwards 0.1s;
                }
            
                .header-strip .dot {
                  width: 10px; height: 10px;
                  border-radius: 50%%;
                  background: var(--danger);
                  box-shadow: 0 0 8px var(--danger);
                  animation: pulse 2s ease-in-out infinite;
                }
            
                @keyframes pulse {
                  0%%, 100%% { opacity: 1; }
                  50%% { opacity: 0.4; }
                }
            
                .header-strip span {
                  font-family: 'IBM Plex Mono', monospace;
                  font-size: 0.72rem;
                  color: var(--text-muted);
                  letter-spacing: 0.1em;
                  text-transform: uppercase;
                }
            
                /* Main card */
                .card {
                  background: var(--surface);
                  border: 1px solid var(--border);
                  border-radius: 12px;
                  overflow: hidden;
                  opacity: 0;
                  animation: fadeUp 0.5s ease forwards 0.2s;
                }
            
                /* Warning banner at top of card */
                .card-banner {
                  background: var(--danger-dim);
                  border-bottom: 1px solid rgba(248, 81, 73, 0.2);
                  padding: 1rem 1.75rem;
                  display: flex;
                  align-items: center;
                  gap: 0.75rem;
                }
            
                .card-banner svg {
                  flex-shrink: 0;
                  color: var(--danger);
                }
            
                .card-banner-text {
                  font-family: 'IBM Plex Mono', monospace;
                  font-size: 0.75rem;
                  color: var(--danger);
                  letter-spacing: 0.05em;
                }
            
                .card-body {
                  padding: 2rem 1.75rem;
                }
            
                h1 {
                  font-size: 1.6rem;
                  font-weight: 600;
                  color: var(--text-primary);
                  line-height: 1.2;
                  margin-bottom: 0.75rem;
                }
            
                .subtitle {
                  font-size: 0.9rem;
                  font-weight: 300;
                  color: var(--text-secondary);
                  line-height: 1.65;
                  margin-bottom: 2rem;
                  padding-bottom: 2rem;
                  border-bottom: 1px solid var(--border);
                }
            
                /* Info rows */
                .info-grid {
                  display: flex;
                  flex-direction: column;
                  gap: 0;
                  margin-bottom: 2rem;
                }
            
                .info-row {
                  display: flex;
                  justify-content: space-between;
                  align-items: flex-start;
                  gap: 1rem;
                  padding: 0.9rem 0;
                  border-bottom: 1px solid var(--border);
                  opacity: 0;
                  animation: fadeUp 0.4s ease forwards;
                }
            
                .info-row:nth-child(1) { animation-delay: 0.35s; }
                .info-row:nth-child(2) { animation-delay: 0.45s; }
                .info-row:nth-child(3) { animation-delay: 0.55s; }
                .info-row:last-child { border-bottom: none; }
            
                .info-label {
                  font-size: 0.8rem;
                  color: var(--text-muted);
                  text-transform: uppercase;
                  letter-spacing: 0.07em;
                  font-family: 'IBM Plex Mono', monospace;
                  flex-shrink: 0;
                  padding-top: 2px;
                }
            
                .info-value {
                  font-size: 0.92rem;
                  color: var(--text-primary);
                  text-align: right;
                  font-weight: 500;
                }
            
                .info-value.version {
                  font-family: 'IBM Plex Mono', monospace;
                  font-size: 0.85rem;
                  background: var(--accent-dim);
                  color: var(--accent);
                  padding: 0.2rem 0.55rem;
                  border-radius: 4px;
                  border: 1px solid rgba(240,136,62,0.2);
                }
            
                .info-value.version-bad {
                  font-family: 'IBM Plex Mono', monospace;
                  font-size: 0.85rem;
                  background: var(--danger-dim);
                  color: var(--danger);
                  padding: 0.2rem 0.55rem;
                  border-radius: 4px;
                  border: 1px solid rgba(248,81,73,0.2);
                }
            
                /* Download button */
                .download-section {
                  opacity: 0;
                  animation: fadeUp 0.4s ease forwards 0.65s;
                }
            
                .download-btn {
                  display: flex;
                  align-items: center;
                  justify-content: center;
                  gap: 0.6rem;
                  width: 100%%;
                  padding: 0.9rem 1.5rem;
                  background: var(--accent);
                  color: #0d1117;
                  font-family: 'IBM Plex Sans', sans-serif;
                  font-weight: 600;
                  font-size: 0.9rem;
                  text-decoration: none;
                  border-radius: 8px;
                  letter-spacing: 0.02em;
                  transition: opacity 0.15s ease, transform 0.15s ease, box-shadow 0.15s ease;
                  box-shadow: 0 0 0 0 rgba(240,136,62,0);
                }
            
                .download-btn:hover {
                  opacity: 0.9;
                  transform: translateY(-1px);
                  box-shadow: 0 4px 20px rgba(240,136,62,0.3);
                }
            
                .download-btn:active {
                  transform: translateY(0);
                }
            
                .download-btn svg {
                  flex-shrink: 0;
                }
            
                /* No-param fallback */
                .fallback-notice {
                  font-family: 'IBM Plex Mono', monospace;
                  font-size: 0.75rem;
                  color: var(--text-muted);
                  margin-top: 1.25rem;
                  text-align: center;
                }
            
                @keyframes fadeUp {
                  from { opacity: 0; transform: translateY(10px); }
                  to   { opacity: 1; transform: translateY(0); }
                }
            
                @media (max-width: 480px) {
                  .info-row { flex-direction: column; gap: 0.3rem; }
                  .info-value { text-align: left; }
                }
              </style>
            </head>
            <body>
              <div class="container">
                <div class="header-strip">
                  <div class="dot"></div>
                  <span>Safe Exam Browser — Access Restricted</span>
                </div>
            
                <div class="card">
                  <div class="card-banner">
                    <svg width="16" height="16" viewBox="0 0 16 16" fill="none" xmlns="http://www.w3.org/2000/svg">
                      <path d="M8 1.5L14.5 13H1.5L8 1.5Z" stroke="currentColor" stroke-width="1.5" stroke-linejoin="round"/>
                      <path d="M8 6V9" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
                      <circle cx="8" cy="11" r="0.75" fill="currentColor"/>
                    </svg>
                    <span class="card-banner-text">VERSION_INCOMPATIBLE — Upgrade required to proceed</span>
                  </div>
            
                  <div class="card-body">
                    <h1>SEB Version Restriction</h1>
                    <p class="subtitle">
                      Your SEB Version is insufficient for the Exam you want to apply.
                      Please download and install the newest SEB Version for your device.
                    </p>
            
                    <div class="info-grid" id="info-grid">
                      <div class="info-row">
                        <span class="info-label">Selected Exam</span>
                        <span class="info-value" id="val-exam">%s</span>
                      </div>
                      <div class="info-row">
                        <span class="info-label">Current SEB Version</span>
                        <span class="info-value version-bad" id="val-current">%s</span>
                      </div>
                      <div class="info-row">
                        <span class="info-label">Exam Restricted to Version(s)</span>
                        <span class="info-value version" id="val-required">%s</span>
                      </div>
                    </div>
            
                    <div class="download-section">
                      <a href="%s" id="download-link" class="download-btn" target="_blank" rel="noopener noreferrer">
                        <svg width="16" height="16" viewBox="0 0 16 16" fill="none" xmlns="http://www.w3.org/2000/svg">
                          <path d="M8 2V10M8 10L5 7M8 10L11 7" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
                          <path d="M2 12H14" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
                        </svg>
                        Download Latest SEB Version
                      </a>
                      <p class="fallback-notice" id="fallback-notice" style="display:none;">
                        No download URL provided via query parameter.
                      </p>
                    </div>
                  </div>
                </div>
              </div>
            
            </body>
            </html>""";
}
