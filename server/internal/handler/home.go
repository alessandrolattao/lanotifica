package handler

import (
	"crypto/rand"
	_ "embed"
	"encoding/base64"
	"fmt"
	"html/template"
	"log"
	"net/http"
	"regexp"
	"sync"
	"time"

	"github.com/skip2/go-qrcode"
	"golang.org/x/crypto/bcrypt"
)

//go:embed lanotifica.png
var logoPNG []byte

var (
	sessions      = make(map[string]time.Time)
	sessionsMu    sync.RWMutex
	validPINRegex = regexp.MustCompile(`^\d{6}$`)
)

const (
	sessionCookieName = "ln_session"
	sessionDuration   = 30 * 24 * time.Hour
)

func newSession() string {
	b := make([]byte, 32)
	if _, err := rand.Read(b); err != nil {
		log.Printf("Failed to generate session token: %v", err)
		return ""
	}
	token := base64.URLEncoding.EncodeToString(b)
	sessionsMu.Lock()
	sessions[token] = time.Now().Add(sessionDuration)
	sessionsMu.Unlock()
	return token
}

func hasValidSession(r *http.Request) bool {
	cookie, err := r.Cookie(sessionCookieName)
	if err != nil {
		return false
	}
	sessionsMu.RLock()
	expiry, ok := sessions[cookie.Value]
	sessionsMu.RUnlock()
	return ok && time.Now().Before(expiry)
}

func setSessionCookie(w http.ResponseWriter, token string) {
	http.SetCookie(w, &http.Cookie{
		Name:     sessionCookieName,
		Value:    token,
		Path:     "/",
		MaxAge:   int(sessionDuration.Seconds()),
		Secure:   true,
		HttpOnly: true,
		SameSite: http.SameSiteStrictMode,
	})
}

var sharedStyle = `
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
            background: #0a0a0a;
            min-height: 100vh;
            display: flex;
            justify-content: center;
            align-items: center;
            color: #fafafa;
        }
        .card {
            background: #111;
            border: 1px solid #222;
            border-radius: 16px;
            padding: 48px;
            width: 100%;
            max-width: 400px;
            text-align: center;
        }
        h1 { font-size: 1.5rem; font-weight: 600; margin-bottom: 8px; }
        .sub { color: #666; font-size: 14px; margin-bottom: 32px; }
        input[type=text], input[type=password] {
            width: 100%;
            background: #0a0a0a;
            border: 1px solid #333;
            border-radius: 8px;
            color: #fff;
            font-size: 2rem;
            letter-spacing: 0.5rem;
            padding: 12px;
            text-align: center;
            outline: none;
            margin-bottom: 16px;
        }
        input:focus { border-color: #555; }
        button {
            width: 100%;
            background: #fff;
            color: #000;
            border: none;
            border-radius: 8px;
            padding: 12px;
            font-size: 15px;
            font-weight: 600;
            cursor: pointer;
        }
        button:hover { background: #e5e5e5; }
        .err { color: #f87171; font-size: 13px; margin-bottom: 16px; }
    </style>`

var setupTemplate = template.Must(template.New("setup").Parse(`<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>LaNotifica — Set PIN</title>
    <link rel="icon" type="image/png" href="/favicon.png">
` + sharedStyle + `
</head>
<body>
    <div class="card">
        <h1>Set a PIN</h1>
        <p class="sub">Choose a 6-digit PIN to protect this page</p>
        {{if .Error}}<p class="err">{{.Error}}</p>{{end}}
        <form method="POST" action="/">
            <input type="hidden" name="action" value="setup">
            <input type="password" name="pin" maxlength="6" placeholder="------" autofocus inputmode="numeric" pattern="\d{6}">
            <button type="submit">Set PIN</button>
        </form>
    </div>
</body>
</html>`))

var loginTemplate = template.Must(template.New("login").Parse(`<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>LaNotifica — Login</title>
    <link rel="icon" type="image/png" href="/favicon.png">
` + sharedStyle + `
</head>
<body>
    <div class="card">
        <h1>LaNotifica</h1>
        <p class="sub">Enter your PIN to access this page</p>
        {{if .Error}}<p class="err">{{.Error}}</p>{{end}}
        <form method="POST" action="/">
            <input type="hidden" name="action" value="login">
            <input type="password" name="pin" maxlength="6" placeholder="------" autofocus inputmode="numeric" pattern="\d{6}">
            <button type="submit">Login</button>
        </form>
    </div>
</body>
</html>`))

var homeTemplate = template.Must(template.New("home").Parse(`<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>LaNotifica</title>
    <link rel="icon" type="image/png" href="/favicon.png">
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600&display=swap" rel="stylesheet">
    <style>
        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }

        body {
            font-family: 'Inter', -apple-system, BlinkMacSystemFont, sans-serif;
            background: #0a0a0a;
            min-height: 100vh;
            display: flex;
            justify-content: center;
            align-items: center;
            color: #fafafa;
            line-height: 1.6;
        }

        .container {
            display: grid;
            grid-template-columns: auto 1fr;
            gap: 80px;
            max-width: 900px;
            padding: 60px;
            align-items: center;
        }

        .qr-section {
            display: flex;
            flex-direction: column;
            align-items: center;
        }

        .qr-wrapper {
            background: #fff;
            padding: 20px;
            border-radius: 24px;
            box-shadow: 0 0 0 1px rgba(255,255,255,0.1), 0 25px 50px -12px rgba(0,0,0,0.5);
        }

        .qr-wrapper img {
            display: block;
            width: 280px;
            height: 280px;
        }

        .qr-hint {
            margin-top: 20px;
            font-size: 13px;
            color: #666;
            text-align: center;
        }

        .content {
            max-width: 400px;
        }

        .header {
            display: flex;
            align-items: center;
            gap: 16px;
            margin-bottom: 12px;
        }

        .header img {
            width: 128px;
            height: 128px;
            border-radius: 20px;
        }

        h1 {
            font-size: 3rem;
            font-weight: 600;
            letter-spacing: -0.03em;
            background: linear-gradient(135deg, #fff 0%, #999 100%);
            -webkit-background-clip: text;
            -webkit-text-fill-color: transparent;
            background-clip: text;
        }

        .tagline {
            font-size: 1.1rem;
            color: #666;
            margin-bottom: 48px;
            font-weight: 300;
        }

        .steps {
            list-style: none;
            counter-reset: step;
        }

        .steps li {
            counter-increment: step;
            display: flex;
            align-items: flex-start;
            gap: 16px;
            margin-bottom: 20px;
            font-size: 15px;
            color: #a1a1a1;
        }

        .steps li::before {
            content: counter(step);
            flex-shrink: 0;
            width: 28px;
            height: 28px;
            background: #1a1a1a;
            border: 1px solid #333;
            border-radius: 50%;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 12px;
            font-weight: 500;
            color: #fff;
        }

        .steps strong {
            color: #fff;
            font-weight: 500;
        }

        .steps a {
            color: #60a5fa;
            text-decoration: none;
        }

        .steps a:hover {
            text-decoration: underline;
        }

        .note {
            margin-top: 40px;
            padding: 16px 20px;
            background: #111;
            border-radius: 12px;
            font-size: 13px;
            color: #555;
            border: 1px solid #222;
        }
    </style>
</head>
<body>
    <div class="container">
        <div class="qr-section">
            <div class="qr-wrapper">
                <img src="data:image/png;base64,{{.QRCode}}" alt="QR Code">
            </div>
            <p class="qr-hint">Scan with LaNotifica app</p>
        </div>

        <div class="content">
            <div class="header">
                <img src="data:image/png;base64,{{.Logo}}" alt="LaNotifica">
                <h1>LaNotifica</h1>
            </div>
            <p class="tagline">Forward Android notifications to your Linux desktop</p>

            <ol class="steps">
                <li><span>Install <strong>LaNotifica</strong> from <a href="https://play.google.com/store/apps/details?id=com.alessandrolattao.lanotifica" target="_blank">Google Play</a></span></li>
                <li><span>Open the app and tap <strong>Scan QR Code</strong></span></li>
                <li><span>Grant <strong>Notification Access</strong> permission</span></li>
                <li><span>Disable <strong>Battery Optimization</strong></span></li>
                <li><span>Enable <strong>Forward Notifications</strong></span></li>
            </ol>

            <p class="note">
                The QR contains your auth token and certificate fingerprint.
                Server discovery happens automatically via mDNS.
                <br><br>
                <span style="color: #777;">Version {{.Version}}</span>
            </p>
        </div>
    </div>
</body>
</html>`))

// FaviconHandler returns a handler that serves the favicon.
func FaviconHandler() http.HandlerFunc {
	return func(w http.ResponseWriter, _ *http.Request) {
		w.Header().Set("Content-Type", "image/png")
		w.Header().Set("Cache-Control", "public, max-age=86400")
		_, _ = w.Write(logoPNG)
	}
}

// HomeHandler returns a handler that displays the home page with QR code,
// protected by a 6-digit PIN set on first access.
func HomeHandler(
	secret, certFingerprint, version string,
	getPINHash func() string,
	savePin func(string) error,
) http.HandlerFunc {
	qrData := fmt.Sprintf("%s|%s", secret, certFingerprint)
	qr, err := qrcode.Encode(qrData, qrcode.Medium, 256)
	if err != nil {
		log.Printf("Failed to generate QR code: %v", err)
	}
	qrBase64 := base64.StdEncoding.EncodeToString(qr)
	logoBase64 := base64.StdEncoding.EncodeToString(logoPNG)

	return func(w http.ResponseWriter, r *http.Request) {
		if r.URL.Path != "/" {
			http.NotFound(w, r)
			return
		}

		pinHash := getPINHash()

		if pinHash == "" {
			handlePINSetup(w, r, savePin)
			return
		}

		if !hasValidSession(r) {
			handlePINLogin(w, r, pinHash)
			return
		}

		w.Header().Set("Content-Type", "text/html; charset=utf-8")
		w.Header().Set("Cache-Control", "no-store, private")
		if err := homeTemplate.Execute(w, map[string]string{
			"QRCode":  qrBase64,
			"Logo":    logoBase64,
			"Version": version,
		}); err != nil {
			log.Printf("Failed to render home page: %v", err)
			http.Error(w, "Internal server error", http.StatusInternalServerError)
		}
	}
}

func handlePINSetup(w http.ResponseWriter, r *http.Request, savePin func(string) error) {
	if r.Method == http.MethodGet {
		renderSetup(w, "")
		return
	}

	if r.Method != http.MethodPost {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	r.Body = http.MaxBytesReader(w, r.Body, 1<<10)
	if err := r.ParseForm(); err != nil {
		renderSetup(w, "Invalid request")
		return
	}

	pin := r.FormValue("pin")
	if !validPINRegex.MatchString(pin) {
		renderSetup(w, "PIN must be exactly 6 digits")
		return
	}

	hash, err := bcrypt.GenerateFromPassword([]byte(pin), bcrypt.DefaultCost)
	if err != nil {
		log.Printf("Failed to hash PIN: %v", err)
		renderSetup(w, "Internal error, please try again")
		return
	}

	if err := savePin(string(hash)); err != nil {
		log.Printf("Failed to save PIN: %v", err)
		renderSetup(w, "Failed to save PIN, please try again")
		return
	}

	token := newSession()
	if token == "" {
		renderSetup(w, "Internal error, please try again")
		return
	}
	setSessionCookie(w, token)
	http.Redirect(w, r, "/", http.StatusSeeOther)
}

func handlePINLogin(w http.ResponseWriter, r *http.Request, pinHash string) {
	if r.Method == http.MethodGet {
		renderLogin(w, "")
		return
	}

	if r.Method != http.MethodPost {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	r.Body = http.MaxBytesReader(w, r.Body, 1<<10)
	if err := r.ParseForm(); err != nil {
		renderLogin(w, "Invalid request")
		return
	}

	pin := r.FormValue("pin")
	if err := bcrypt.CompareHashAndPassword([]byte(pinHash), []byte(pin)); err != nil {
		renderLogin(w, "Wrong PIN")
		return
	}

	token := newSession()
	if token == "" {
		renderLogin(w, "Internal error, please try again")
		return
	}
	setSessionCookie(w, token)
	http.Redirect(w, r, "/", http.StatusSeeOther)
}

func renderSetup(w http.ResponseWriter, errMsg string) {
	w.Header().Set("Content-Type", "text/html; charset=utf-8")
	w.Header().Set("Cache-Control", "no-store, private")
	if err := setupTemplate.Execute(w, map[string]string{"Error": errMsg}); err != nil {
		log.Printf("Failed to render setup page: %v", err)
		http.Error(w, "Internal server error", http.StatusInternalServerError)
	}
}

func renderLogin(w http.ResponseWriter, errMsg string) {
	w.Header().Set("Content-Type", "text/html; charset=utf-8")
	w.Header().Set("Cache-Control", "no-store, private")
	if err := loginTemplate.Execute(w, map[string]string{"Error": errMsg}); err != nil {
		log.Printf("Failed to render login page: %v", err)
		http.Error(w, "Internal server error", http.StatusInternalServerError)
	}
}
