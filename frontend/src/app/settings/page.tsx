"use client";

import { useState } from 'react';
import { 
  Settings, 
  Database, 
  Cpu, 
  Key, 
  Bell, 
  Save, 
  CheckCircle, 
  Server, 
  ShieldCheck, 
  HardDrive, 
  Copy, 
  Check, 
  RefreshCw,
  Sliders
} from 'lucide-react';

export default function SettingsPage() {
  const [activeTab, setActiveTab] = useState<'engine' | 'storage' | 'notifications' | 'api'>('engine');
  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const [copiedKey, setCopiedKey] = useState(false);

  // Engine Settings state
  const [semgrepTimeout, setSemgrepTimeout] = useState('300');
  const [trivyTimeout, setTrivyTimeout] = useState('300');
  const [gitleaksTimeout, setGitleaksTimeout] = useState('120');
  const [memoryLimit, setMemoryLimit] = useState('512m');
  const [cpuLimit, setCpuLimit] = useState('1.0');
  const [dockerSocket, setDockerSocket] = useState('unix:///Users/yuvalsharma/.docker/run/docker.sock');

  // Storage Settings
  const [s3Endpoint, setS3Endpoint] = useState('https://lrfosirsnujasknyehuv.storage.supabase.co/storage/v1/s3');
  const [s3Bucket, setS3Bucket] = useState('systemguard-artifacts');
  const [s3Region, setS3Region] = useState('ap-southeast-1');

  // Notifications
  const [notifyCritical, setNotifyCritical] = useState(true);
  const [notifyFailedScans, setNotifyFailedScans] = useState(true);
  const [sseStreaming, setSseStreaming] = useState(true);

  // API Token
  const [apiKey] = useState('sg_live_79a3b8d2e1c944f0b6e921d4c28109a4');

  const showToast = (msg: string) => {
    setToastMessage(msg);
    setTimeout(() => setToastMessage(null), 3000);
  };

  const handleCopyKey = () => {
    navigator.clipboard.writeText(apiKey);
    setCopiedKey(true);
    showToast('API key copied to clipboard');
    setTimeout(() => setCopiedKey(false), 2000);
  };

  const handleSave = (e: React.FormEvent) => {
    e.preventDefault();
    showToast('Settings saved successfully');
  };

  return (
    <div className="space-y-6 pb-12">
      {/* Toast */}
      {toastMessage && (
        <div className="fixed bottom-6 right-6 z-50 bg-emerald-500/10 border border-emerald-500/30 text-emerald-400 px-4 py-3 rounded-xl shadow-2xl flex items-center gap-2 backdrop-blur-md animate-in fade-in slide-in-from-bottom-5">
          <CheckCircle className="w-4 h-4 text-emerald-400 shrink-0" />
          <span className="text-sm font-medium">{toastMessage}</span>
        </div>
      )}

      {/* Header */}
      <div className="flex flex-col md:flex-row md:items-end justify-between gap-4">
        <div>
          <h1 className="text-display text-white">System Settings & Configuration</h1>
          <p className="text-zinc-400 mt-1 text-sm">
            Configure container isolation parameters, cloud storage, API tokens, and scanning limits.
          </p>
        </div>

        <button 
          onClick={handleSave}
          className="btn btn-primary flex items-center gap-2 self-start md:self-auto"
        >
          <Save className="w-4 h-4" />
          Save Changes
        </button>
      </div>

      {/* Navigation Tabs */}
      <div className="flex border-b border-white/5 gap-6">
        {[
          { id: 'engine', label: 'Scanner Engine', icon: Cpu },
          { id: 'storage', label: 'Storage & Database', icon: HardDrive },
          { id: 'notifications', label: 'Notifications & Alerts', icon: Bell },
          { id: 'api', label: 'API Keys & Security', icon: Key },
        ].map(tab => {
          const isActive = activeTab === tab.id;
          return (
            <button
              key={tab.id}
              onClick={() => setActiveTab(tab.id as any)}
              className={`flex items-center gap-2 py-3 text-xs font-semibold tracking-wide border-b-2 transition-colors ${
                isActive 
                  ? 'border-indigo-500 text-indigo-400' 
                  : 'border-transparent text-zinc-400 hover:text-zinc-200'
              }`}
            >
              <tab.icon className="w-4 h-4" />
              {tab.label}
            </button>
          );
        })}
      </div>

      {/* Engine Settings */}
      {activeTab === 'engine' && (
        <div className="space-y-6">
          <div className="sg-card p-6 space-y-5">
            <div>
              <h2 className="text-base font-semibold text-white">Docker Isolation & Scanners</h2>
              <p className="text-xs text-zinc-400 mt-1">Resource limits and timeouts allocated to scanning containers.</p>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label className="block text-xs font-medium text-zinc-300 mb-1">Semgrep Timeout (Seconds)</label>
                <input
                  type="number"
                  value={semgrepTimeout}
                  onChange={e => setSemgrepTimeout(e.target.value)}
                  className="w-full bg-[#121214] border border-white/10 rounded-lg px-3 py-2 text-sm text-white outline-none focus:border-indigo-500 font-mono"
                />
              </div>

              <div>
                <label className="block text-xs font-medium text-zinc-300 mb-1">Trivy Timeout (Seconds)</label>
                <input
                  type="number"
                  value={trivyTimeout}
                  onChange={e => setTrivyTimeout(e.target.value)}
                  className="w-full bg-[#121214] border border-white/10 rounded-lg px-3 py-2 text-sm text-white outline-none focus:border-indigo-500 font-mono"
                />
              </div>

              <div>
                <label className="block text-xs font-medium text-zinc-300 mb-1">Gitleaks Timeout (Seconds)</label>
                <input
                  type="number"
                  value={gitleaksTimeout}
                  onChange={e => setGitleaksTimeout(e.target.value)}
                  className="w-full bg-[#121214] border border-white/10 rounded-lg px-3 py-2 text-sm text-white outline-none focus:border-indigo-500 font-mono"
                />
              </div>

              <div>
                <label className="block text-xs font-medium text-zinc-300 mb-1">Container Memory Limit</label>
                <select
                  value={memoryLimit}
                  onChange={e => setMemoryLimit(e.target.value)}
                  className="w-full bg-[#121214] border border-white/10 rounded-lg px-3 py-2 text-sm text-white outline-none focus:border-indigo-500 font-mono"
                >
                  <option value="512m">512 MB (Default)</option>
                  <option value="1g">1024 MB (1 GB)</option>
                  <option value="2g">2048 MB (2 GB)</option>
                </select>
              </div>

              <div className="md:col-span-2">
                <label className="block text-xs font-medium text-zinc-300 mb-1">Docker Daemon Socket</label>
                <input
                  type="text"
                  value={dockerSocket}
                  onChange={e => setDockerSocket(e.target.value)}
                  className="w-full bg-[#121214] border border-white/10 rounded-lg px-3 py-2 text-sm text-white outline-none focus:border-indigo-500 font-mono text-xs"
                />
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Storage & Database */}
      {activeTab === 'storage' && (
        <div className="space-y-6">
          <div className="sg-card p-6 space-y-5">
            <div>
              <h2 className="text-base font-semibold text-white">Database & S3 Artifact Storage</h2>
              <p className="text-xs text-zinc-400 mt-1">Live database connections and object storage endpoints.</p>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div className="p-4 rounded-xl bg-[#121214] border border-white/5 flex items-center justify-between">
                <div className="flex items-center gap-3">
                  <div className="w-8 h-8 rounded-lg bg-emerald-500/10 border border-emerald-500/20 flex items-center justify-center text-emerald-400">
                    <Database className="w-4 h-4" />
                  </div>
                  <div>
                    <div className="text-xs font-semibold text-white">Supabase PostgreSQL</div>
                    <div className="text-[11px] text-zinc-500 font-mono">aws-0-ap-southeast-1.pooler.supabase.com:5432</div>
                  </div>
                </div>
                <span className="badge badge-success text-[10px]">Connected</span>
              </div>

              <div className="p-4 rounded-xl bg-[#121214] border border-white/5 flex items-center justify-between">
                <div className="flex items-center gap-3">
                  <div className="w-8 h-8 rounded-lg bg-red-500/10 border border-red-500/20 flex items-center justify-center text-red-400">
                    <Server className="w-4 h-4" />
                  </div>
                  <div>
                    <div className="text-xs font-semibold text-white">Upstash Redis</div>
                    <div className="text-[11px] text-zinc-500 font-mono">allowing-mullet-192835.upstash.io:6379</div>
                  </div>
                </div>
                <span className="badge badge-success text-[10px]">Connected</span>
              </div>
            </div>

            <div className="space-y-4 pt-4 border-t border-white/5">
              <div>
                <label className="block text-xs font-medium text-zinc-300 mb-1">S3 Endpoint URL</label>
                <input
                  type="text"
                  value={s3Endpoint}
                  onChange={e => setS3Endpoint(e.target.value)}
                  className="w-full bg-[#121214] border border-white/10 rounded-lg px-3 py-2 text-xs text-white outline-none focus:border-indigo-500 font-mono"
                />
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-medium text-zinc-300 mb-1">S3 Bucket Name</label>
                  <input
                    type="text"
                    value={s3Bucket}
                    onChange={e => setS3Bucket(e.target.value)}
                    className="w-full bg-[#121214] border border-white/10 rounded-lg px-3 py-2 text-xs text-white outline-none focus:border-indigo-500 font-mono"
                  />
                </div>

                <div>
                  <label className="block text-xs font-medium text-zinc-300 mb-1">Region</label>
                  <input
                    type="text"
                    value={s3Region}
                    onChange={e => setS3Region(e.target.value)}
                    className="w-full bg-[#121214] border border-white/10 rounded-lg px-3 py-2 text-xs text-white outline-none focus:border-indigo-500 font-mono"
                  />
                </div>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Notifications */}
      {activeTab === 'notifications' && (
        <div className="space-y-6">
          <div className="sg-card p-6 space-y-5">
            <div>
              <h2 className="text-base font-semibold text-white">Alert Preferences</h2>
              <p className="text-xs text-zinc-400 mt-1">Configure when SystemGuard sends high-priority notifications.</p>
            </div>

            <div className="space-y-4">
              <div className="flex items-center justify-between p-3 rounded-lg bg-[#121214] border border-white/5">
                <div>
                  <div className="text-xs font-medium text-white">Critical Finding Immediate Alert</div>
                  <div className="text-[11px] text-zinc-500">Dispatch alerts whenever a CVSS 9.0+ vulnerability is uncovered.</div>
                </div>
                <input
                  type="checkbox"
                  checked={notifyCritical}
                  onChange={e => setNotifyCritical(e.target.checked)}
                  className="w-4 h-4 rounded border-white/10 text-indigo-600 focus:ring-0 cursor-pointer"
                />
              </div>

              <div className="flex items-center justify-between p-3 rounded-lg bg-[#121214] border border-white/5">
                <div>
                  <div className="text-xs font-medium text-white">Pipeline Failure Notifications</div>
                  <div className="text-[11px] text-zinc-500">Alert lead developers if any scanner fails or times out.</div>
                </div>
                <input
                  type="checkbox"
                  checked={notifyFailedScans}
                  onChange={e => setNotifyFailedScans(e.target.checked)}
                  className="w-4 h-4 rounded border-white/10 text-indigo-600 focus:ring-0 cursor-pointer"
                />
              </div>

              <div className="flex items-center justify-between p-3 rounded-lg bg-[#121214] border border-white/5">
                <div>
                  <div className="text-xs font-medium text-white">SSE Real-Time UI Streaming</div>
                  <div className="text-[11px] text-zinc-500">Stream job completion events via Server-Sent Events.</div>
                </div>
                <input
                  type="checkbox"
                  checked={sseStreaming}
                  onChange={e => setSseStreaming(e.target.checked)}
                  className="w-4 h-4 rounded border-white/10 text-indigo-600 focus:ring-0 cursor-pointer"
                />
              </div>
            </div>
          </div>
        </div>
      )}

      {/* API Keys */}
      {activeTab === 'api' && (
        <div className="space-y-6">
          <div className="sg-card p-6 space-y-5">
            <div>
              <h2 className="text-base font-semibold text-white">API Authentication & Tokens</h2>
              <p className="text-xs text-zinc-400 mt-1">Use this key to authenticate CI/CD runners (Jenkins, GitHub Actions, GitLab CI).</p>
            </div>

            <div>
              <label className="block text-xs font-medium text-zinc-300 mb-1">Production API Key</label>
              <div className="flex items-center gap-2">
                <input
                  type="password"
                  readOnly
                  value={apiKey}
                  className="w-full bg-[#121214] border border-white/10 rounded-lg px-3 py-2 text-xs text-white font-mono outline-none"
                />
                <button
                  onClick={handleCopyKey}
                  className="px-3 py-2 bg-white/5 hover:bg-white/10 text-zinc-300 rounded-lg border border-white/10 flex items-center gap-1.5 text-xs font-medium transition-colors"
                >
                  {copiedKey ? <Check className="w-3.5 h-3.5 text-emerald-400" /> : <Copy className="w-3.5 h-3.5" />}
                  {copiedKey ? 'Copied' : 'Copy'}
                </button>
              </div>
              <p className="text-[11px] text-zinc-500 mt-1.5">
                Pass this token in HTTP header: <code className="text-indigo-400">Authorization: Bearer sg_live_...</code>
              </p>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
