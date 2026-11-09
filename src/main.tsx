import { useEffect, useState, type FormEvent } from 'react';
import { createRoot } from 'react-dom/client';
import { Capacitor } from '@capacitor/core';
import { languages, t, type Label, type Language } from './i18n';
import { sip, type Account, type Status } from './sip';
import license from '../LICENSE?raw';
import { version } from '../package.json';
import './style.css';

type LanguagePreference = Language | 'system';
type ThemePreference = 'system' | 'light' | 'dark';

function savedTheme(): ThemePreference {
  const value = localStorage.getItem('themePreference');
  return value === 'light' || value === 'dark' ? value : 'system';
}

const systemDark = window.matchMedia('(prefers-color-scheme: dark)');
const isDark = (theme: ThemePreference) => theme === 'dark' || (theme === 'system' && systemDark.matches);
document.documentElement.dataset.theme = isDark(savedTheme()) ? 'dark' : 'light';

function systemLanguage(): Language {
  for (const locale of navigator.languages) {
    const code = locale.split('-')[0].toLowerCase();
    if (Object.hasOwn(languages, code)) return code as Language;
  }
  return 'en';
}

function App() {
  const [theme, setTheme] = useState<ThemePreference>(savedTheme);
  const [system, setSystem] = useState(systemLanguage);
  const [preference, setPreference] = useState<LanguagePreference>(() => {
    const value = localStorage.getItem('languagePreference');
    return value && Object.hasOwn(languages, value) ? value as Language : 'system';
  });
  const lang = preference === 'system' ? system : preference;
  const [tab, setTab] = useState<'account' | 'phone' | 'about'>('account');
  const [status, setStatus] = useState<Status>({});
  const [host, setHost] = useState('');
  const [extension, setExtension] = useState('');
  const [password, setPassword] = useState('');
  const [savePassword, setSavePassword] = useState(true);
  const [port, setPort] = useState(5061);
  const [mode, setMode] = useState<Account['mode']>('sdes');
  const [md5, setMd5] = useState(false);
  const [certificate, setCertificate] = useState('');
  const [peer, setPeer] = useState('');
  const [notice, setNotice] = useState<Label>();
  const [busy, setBusy] = useState(false);
  const l = (label: Label) => t(lang, label);
  const account = status.account;
  const online = status.registration === 'Ok' || status.registration === 'Refreshing';
  const running = !!status.registration;
  const active = status.call && !['idle', 'failed'].includes(status.call);

  useEffect(() => {
    localStorage.setItem('languagePreference', preference);
    document.documentElement.lang = lang;
  }, [lang, preference]);

  useEffect(() => {
    localStorage.setItem('themePreference', theme);
    const applyTheme = () => {
      const dark = isDark(theme);
      document.documentElement.dataset.theme = dark ? 'dark' : 'light';
      if (Capacitor.isNativePlatform()) void sip.setAppearance({ dark }).catch(() => {});
    };
    applyTheme();
    systemDark.addEventListener('change', applyTheme);
    return () => systemDark.removeEventListener('change', applyTheme);
  }, [theme]);

  useEffect(() => {
    const refresh = () => setSystem(systemLanguage());
    window.addEventListener('languagechange', refresh);
    return () => window.removeEventListener('languagechange', refresh);
  }, []);

  useEffect(() => {
    if (!Capacitor.isNativePlatform()) return;
    let stopped = false;
    let previousCall: Status['call'];
    const poll = async () => {
      try {
        const value = await sip.status();
        if (stopped) return;
        setStatus(value);
        if (value.call === 'incoming' && previousCall !== 'incoming') setTab('phone');
        previousCall = value.call;
      } catch { /* The native bridge can be unavailable during teardown. */ }
    };
    void sip.status().then(({ account }) => {
      if (stopped || !account) return;
      setHost(account.host);
      setExtension(account.extension);
      setPort(account.port);
      setMode(account.mode);
      setMd5(account.md5);
      setSavePassword(account.savePassword ?? true);
      setCertificate(account.certificate ?? '');
      setTab('phone');
    }).catch(() => {});
    void poll();
    const timer = window.setInterval(poll, 500);
    return () => { stopped = true; window.clearInterval(timer); };
  }, []);

  async function action(task: () => Promise<void>) {
    setBusy(true);
    setNotice(undefined);
    try { await task(); }
    catch (error) {
      const code = error instanceof Error ? error.message : '';
      const known = ['notifications', 'microphone', 'offline', 'invalidCertificate'];
      setNotice(known.includes(code) ? code as Label : 'error');
    } finally { setBusy(false); }
  }

  function connect(event: FormEvent) {
    event.preventDefault();
    void action(async () => {
      const endpoint = { host: host.trim().toLowerCase(), port };
      let trusted = certificate.trim();
      if (!trusted) {
        const discovered = await sip.discoverCertificate(endpoint);
        if (discovered.certificate && discovered.fingerprint) {
          if (!window.confirm(`${l('trustCertificate')}\n\n${discovered.fingerprint}`)) return;
          trusted = discovered.certificate;
          setCertificate(trusted);
        }
      }
      await sip.connect({ account: { ...endpoint, extension, password, savePassword, mode, md5, certificate: trusted, language: preference } });
      setPassword('');
      setTab('phone');
    });
  }

  const canReusePassword = !!account?.hasSavedPassword && host.trim().toLowerCase() === account.host
    && extension === account.extension && port === account.port;
  const registration = online ? 'online' : status.registration === 'Failed' ? 'registrationFailed'
    : running ? 'progress' : 'offline';

  return <main className={tab === 'phone' ? 'phone-page' : undefined}>
    <header>
      <div><h1>Minghe</h1><p className={online ? 'online' : 'muted'} role="status">{l(registration)}{registration === 'registrationFailed' && !!status.registrationCode && ` (SIP ${status.registrationCode})`}</p></div>
      <select aria-label="Language · 语言" value={preference} onChange={event => setPreference(event.target.value as LanguagePreference)}>
        <option value="system">{l('systemLanguage')}</option>
        {Object.entries(languages).map(([code, name]) => <option value={code} key={code}>{name}</option>)}
      </select>
    </header>
    <nav>
      {(['account', 'phone', 'about'] as const).map(value =>
        <button key={value} aria-current={tab === value ? 'page' : undefined} onClick={() => { setTab(value); setNotice(undefined); }}>
          {l(value)}
        </button>)}
    </nav>
    {notice && <p className="notice" role="status">{l(notice)}</p>}
    {tab === 'account' && <form className="panel" onSubmit={connect}>
      <h2>{l('account')}</h2>
      <label>{l('server')}<input required value={host} placeholder="sip.example.org" autoCapitalize="none" spellCheck={false}
        onChange={event => setHost(event.target.value)} /></label>
      <label>{l('extension')}<input required inputMode="numeric" pattern="[1-9][0-9]{0,9}" value={extension}
        onChange={event => setExtension(event.target.value)} /></label>
      <label>{l('password')}<input required={!canReusePassword} placeholder={canReusePassword ? l('savedPassword') : undefined} type="password" maxLength={512} value={password}
        onChange={event => setPassword(event.target.value)} /></label>
      <label className="check"><input type="checkbox" checked={savePassword} onChange={event => setSavePassword(event.target.checked)} />{l('savePassword')}</label>
      <details>
        <summary>{l('advanced')}</summary>
        <button type="button" onClick={() => void action(() => sip.batterySettings())}>{l('battery')}</button>
        <label>{l('port')}<input required type="number" min="1" max="65535" value={port}
          onChange={event => setPort(event.target.valueAsNumber)} /></label>
        <label>{l('mode')}<select value={mode} onChange={event => setMode(event.target.value as Account['mode'])}>
          <option value="sdes">{l('sdes')}</option><option value="zrtp">{l('zrtp')}</option>
        </select></label>
        <label>{l('certificate')}<textarea rows={3} maxLength={16384} value={certificate} autoCapitalize="none" spellCheck={false}
          onChange={event => setCertificate(event.target.value)} /></label>
        <label className="check"><input type="checkbox" checked={md5} onChange={event => setMd5(event.target.checked)} />{l('md5')}</label>
      </details>
      <div className="actions">
        <button className="primary" disabled={busy || !!active}>{l('connect')}</button>
        {running && <button type="button" disabled={busy} onClick={() => void action(() => sip.disconnect())}>{l('disconnect')}</button>}
      </div>
    </form>}
    {tab === 'phone' && <section className="panel phone-panel">
      <div className="dial-section">
        <h2>{l('phone')}</h2>
        {status.call && status.call !== 'idle' && <p role="status">{l(status.call)} {status.peer}{status.call === 'failed' && !!status.callCode && ` (SIP ${status.callCode})`}</p>}
        {active ? <>
          <div className="actions">
            {status.call === 'incoming' && <button className="primary" disabled={busy} onClick={() => void action(() => sip.answer())}>{l('answer')}</button>}
            <button className="danger" disabled={busy} onClick={() => void action(() => sip.hangup())}>
              {l(status.call === 'incoming' ? 'decline' : 'hangup')}
            </button>
          </div>
          {status.sas && <div className="sas">
            <p>{l('sas')}</p><output>{status.sas}</output>
            <button disabled={status.verified || busy} onClick={() => void action(() => sip.verify())}>
              {l(status.verified ? 'verified' : 'verify')}
            </button>
          </div>}
        </> : <form onSubmit={event => { event.preventDefault(); void action(() => sip.dial({ peer })); }}>
          <input className="dial-number" aria-label={l('destination')} placeholder={l('destination')} required
            inputMode="none" pattern="[1-9][0-9]{0,9}" maxLength={10} value={peer}
            onChange={event => setPeer(event.target.value.replace(/\D/g, ''))} />
          <div className="keypad">
            {'123456789*0#'.split('').map(digit => <button key={digit} type="button"
              disabled={!/^[0-9]$/.test(digit) || peer.length >= 10} onClick={() => setPeer(value => value + digit)}>
              {digit}
            </button>)}
          </div>
          <div className="dial-actions">
            <button type="button" aria-label={l('backspace')} disabled={!peer} onClick={() => setPeer(value => value.slice(0, -1))}>⌫</button>
            <button className="primary" disabled={!online || busy || !/^[1-9][0-9]{0,9}$/.test(peer)}>{l('dial')}</button>
          </div>
        </form>}
      </div>
      <div className="history-section">
        <h3>{l('history')}</h3>
        {status.history?.length ? <ul className="call-history">{status.history.map((entry, index) =>
          <li key={entry.time + ':' + index}><button onClick={() => setPeer(entry.peer)} disabled={!!active || !/^[1-9][0-9]{0,9}$/.test(entry.peer)}>
            <span aria-hidden="true">{entry.incoming ? '↙' : '↗'}</span>
            <span><strong>{entry.peer || '—'}</strong><small>
              {l(entry.incoming ? 'incoming' : 'outgoing')} · {l(entry.answered ? 'answered' : 'unanswered')}
            </small></span>
            <time dateTime={new Date(entry.time).toISOString()}>{new Date(entry.time).toLocaleString(lang)}</time>
          </button></li>)}</ul> : <p className="muted">{l('noCalls')}</p>}
      </div>
    </section>}
    {tab === 'about' && <section className="panel">
      <h2>{l('about')}</h2>
      <label>{l('appearance')}
        <select value={theme} onChange={event => setTheme(event.target.value as ThemePreference)}>
          <option value="system">{l('systemTheme')}</option>
          <option value="light">{l('lightTheme')}</option>
          <option value="dark">{l('darkTheme')}</option>
        </select>
      </label>
      <p>Minghe v{version}</p>
      <p>{l('author')} · 619dev</p>
      <p>{l('website')} · <a href="https://minghe.mobile" target="_blank" rel="noopener noreferrer">minghe.mobile</a></p>
      <details>
        <summary>GNU Affero General Public License v3.0</summary>
        <pre className="license">{license}</pre>
      </details>
    </section>}
  </main>;
}

createRoot(document.getElementById('root')!).render(<App />);
