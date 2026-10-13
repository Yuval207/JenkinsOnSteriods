"use client";

import { useState } from 'react';
import { 
  ShieldCheck, 
  ShieldAlert, 
  CheckCircle, 
  AlertTriangle, 
  Sliders, 
  Lock, 
  Plus, 
  FileCode, 
  Layers, 
  Save, 
  RefreshCw, 
  Check, 
  ToggleLeft, 
  ToggleRight,
  Info
} from 'lucide-react';

type Policy = {
  id: string;
  name: string;
  category: string;
  description: string;
  scanner: string;
  severityThreshold: 'CRITICAL' | 'HIGH' | 'MEDIUM' | 'LOW';
  action: 'BLOCK' | 'WARN' | 'AUDIT';
  enabled: boolean;
};

const initialPolicies: Policy[] = [
  {
    id: 'pol-1',
    name: 'Zero Tolerance for Hardcoded Secrets',
    category: 'Secret Detection',
    description: 'Block builds and alert DevSecOps if any API keys, SSH keys, or cloud credentials are detected in repo files.',
    scanner: 'Gitleaks',
    severityThreshold: 'CRITICAL',
    action: 'BLOCK',
    enabled: true,
  },
  {
    id: 'pol-2',
    name: 'Block Critical SAST Vulnerabilities',
    category: 'Code Security',
    description: 'Fail pipelines when static code analysis detects SQL Injection, Command Injection, or Insecure Deserialization.',
    scanner: 'Semgrep',
    severityThreshold: 'CRITICAL',
    action: 'BLOCK',
    enabled: true,
  },
  {
    id: 'pol-3',
    name: 'Severe Third-Party CVE Gatekeeper',
    category: 'Software Supply Chain',
    description: 'Reject artifacts containing known vulnerabilities with CVSS score >= 7.5 (High/Critical).',
    scanner: 'Trivy',
    severityThreshold: 'HIGH',
    action: 'BLOCK',
    enabled: true,
  },
  {
    id: 'pol-4',
    name: 'Non-Root Container Enforcement',
    category: 'Container Security',
    description: 'Audit Dockerfile and container configuration to ensure services do not execute with root UID.',
    scanner: 'Trivy Misconfig',
    severityThreshold: 'HIGH',
    action: 'WARN',
    enabled: true,
  },
  {
    id: 'pol-5',
    name: 'Restrict Copyleft Open Source Licenses',
    category: 'Compliance',
    description: 'Flag GPL-3.0, AGPL-3.0 dependencies embedded in production web application bundles.',
    scanner: 'Trivy',
    severityThreshold: 'MEDIUM',
    action: 'WARN',
    enabled: false,
  },
  {
    id: 'pol-6',
    name: '72-Hour Critical SLA Compliance',
    category: 'Governance',
    description: 'Alert engineering leads when critical vulnerabilities remain in OPEN state beyond 72 hours.',
    scanner: 'All Scanners',
    severityThreshold: 'CRITICAL',
    action: 'AUDIT',
    enabled: true,
  }
];

export default function PoliciesPage() {
  const [policies, setPolicies] = useState<Policy[]>(initialPolicies);
  const [filterCategory, setFilterCategory] = useState<string>('ALL');
  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const [showAddModal, setShowAddModal] = useState(false);
  const [newPolicy, setNewPolicy] = useState({
    name: '',
    category: 'Code Security',
    description: '',
    scanner: 'Semgrep',
    severityThreshold: 'HIGH' as const,
    action: 'BLOCK' as const,
  });

  const togglePolicy = (id: string) => {
    setPolicies(prev => prev.map(p => p.id === id ? { ...p, enabled: !p.enabled } : p));
    showToast('Policy status updated');
  };

  const updateAction = (id: string, action: 'BLOCK' | 'WARN' | 'AUDIT') => {
    setPolicies(prev => prev.map(p => p.id === id ? { ...p, action } : p));
    showToast('Enforcement action updated');
  };

  const showToast = (msg: string) => {
    setToastMessage(msg);
    setTimeout(() => setToastMessage(null), 3000);
  };

  const handleAddPolicy = (e: React.FormEvent) => {
    e.preventDefault();
    if (!newPolicy.name.trim()) return;

    const created: Policy = {
      id: `pol-${Date.now()}`,
      name: newPolicy.name,
      category: newPolicy.category,
      description: newPolicy.description || 'Custom security policy rule.',
      scanner: newPolicy.scanner,
      severityThreshold: newPolicy.severityThreshold,
      action: newPolicy.action,
      enabled: true,
    };

    setPolicies([created, ...policies]);
    setShowAddModal(false);
    setNewPolicy({
      name: '',
      category: 'Code Security',
      description: '',
      scanner: 'Semgrep',
      severityThreshold: 'HIGH',
      action: 'BLOCK',
    });
    showToast('New policy successfully created');
  };

  const categories = ['ALL', 'Secret Detection', 'Code Security', 'Software Supply Chain', 'Container Security', 'Compliance'];
  const filteredPolicies = policies.filter(p => filterCategory === 'ALL' || p.category === filterCategory);

  const activeCount = policies.filter(p => p.enabled).length;
  const blockingCount = policies.filter(p => p.enabled && p.action === 'BLOCK').length;

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
          <h1 className="text-display text-white">Security & Guardrail Policies</h1>
          <p className="text-zinc-400 mt-1 text-sm">
            Define automated security gates, blocking rules, and compliance standards for your CI/CD pipelines.
          </p>
        </div>

        <button 
          onClick={() => setShowAddModal(true)}
          className="btn btn-primary flex items-center gap-2 self-start md:self-auto"
        >
          <Plus className="w-4 h-4" />
          Add Custom Policy
        </button>
      </div>

      {/* Quick Metrics */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <div className="sg-card p-4 flex items-center gap-4">
          <div className="w-10 h-10 rounded-xl bg-indigo-500/10 border border-indigo-500/20 flex items-center justify-center text-indigo-400">
            <ShieldCheck className="w-5 h-5" />
          </div>
          <div>
            <div className="text-2xl font-bold text-white">{activeCount} / {policies.length}</div>
            <div className="text-xs text-zinc-400">Active Policies</div>
          </div>
        </div>

        <div className="sg-card p-4 flex items-center gap-4">
          <div className="w-10 h-10 rounded-xl bg-rose-500/10 border border-rose-500/20 flex items-center justify-center text-rose-400">
            <Lock className="w-5 h-5" />
          </div>
          <div>
            <div className="text-2xl font-bold text-white">{blockingCount}</div>
            <div className="text-xs text-zinc-400">Blocking CI/CD Gates</div>
          </div>
        </div>

        <div className="sg-card p-4 flex items-center gap-4">
          <div className="w-10 h-10 rounded-xl bg-emerald-500/10 border border-emerald-500/20 flex items-center justify-center text-emerald-400">
            <CheckCircle className="w-5 h-5" />
          </div>
          <div>
            <div className="text-2xl font-bold text-white">100%</div>
            <div className="text-xs text-zinc-400">Guardrail Engine Uptime</div>
          </div>
        </div>
      </div>

      {/* Filters */}
      <div className="flex gap-2 overflow-x-auto pb-1">
        {categories.map(cat => (
          <button
            key={cat}
            onClick={() => setFilterCategory(cat)}
            className={`px-3 py-1.5 rounded-lg text-xs font-medium transition-colors shrink-0 ${
              filterCategory === cat 
                ? 'bg-indigo-600 text-white shadow-md shadow-indigo-600/20' 
                : 'bg-[#18181b] text-zinc-400 hover:text-zinc-200 border border-white/5'
            }`}
          >
            {cat}
          </button>
        ))}
      </div>

      {/* Policy Cards List */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {filteredPolicies.map((pol) => (
          <div 
            key={pol.id}
            className={`sg-card p-5 transition-all flex flex-col justify-between ${
              pol.enabled ? 'border-white/10' : 'opacity-60 bg-[#121214]'
            }`}
          >
            <div>
              <div className="flex items-start justify-between gap-3 mb-2">
                <div>
                  <span className="px-2 py-0.5 rounded text-[10px] font-semibold uppercase tracking-wider bg-white/5 text-zinc-400 border border-white/5">
                    {pol.category}
                  </span>
                  <h3 className="text-base font-semibold text-white mt-2">{pol.name}</h3>
                </div>

                {/* Enable toggle */}
                <button 
                  onClick={() => togglePolicy(pol.id)}
                  className="text-zinc-400 hover:text-white transition-colors"
                  title={pol.enabled ? 'Click to disable' : 'Click to enable'}
                >
                  {pol.enabled ? (
                    <div className="w-10 h-5 bg-indigo-600 rounded-full flex items-center justify-end px-1 cursor-pointer transition-colors">
                      <div className="w-3.5 h-3.5 bg-white rounded-full shadow-md" />
                    </div>
                  ) : (
                    <div className="w-10 h-5 bg-zinc-700 rounded-full flex items-center justify-start px-1 cursor-pointer transition-colors">
                      <div className="w-3.5 h-3.5 bg-zinc-400 rounded-full shadow-md" />
                    </div>
                  )}
                </button>
              </div>

              <p className="text-xs text-zinc-400 leading-relaxed mb-4">
                {pol.description}
              </p>
            </div>

            <div className="pt-3 border-t border-white/5 flex flex-wrap items-center justify-between gap-2 text-xs">
              <div className="flex items-center gap-2">
                <span className="text-zinc-500">Scanner:</span>
                <span className="font-mono text-zinc-300 font-medium">{pol.scanner}</span>
              </div>

              <div className="flex items-center gap-2">
                <span className="text-zinc-500">Enforcement:</span>
                <div className="flex rounded-md overflow-hidden border border-white/10 bg-[#121214]">
                  <button
                    onClick={() => updateAction(pol.id, 'BLOCK')}
                    className={`px-2 py-0.5 text-[11px] font-semibold transition-colors ${
                      pol.action === 'BLOCK' ? 'bg-rose-500/20 text-rose-300' : 'text-zinc-500 hover:text-zinc-300'
                    }`}
                  >
                    Block
                  </button>
                  <button
                    onClick={() => updateAction(pol.id, 'WARN')}
                    className={`px-2 py-0.5 text-[11px] font-semibold transition-colors ${
                      pol.action === 'WARN' ? 'bg-amber-500/20 text-amber-300' : 'text-zinc-500 hover:text-zinc-300'
                    }`}
                  >
                    Warn
                  </button>
                  <button
                    onClick={() => updateAction(pol.id, 'AUDIT')}
                    className={`px-2 py-0.5 text-[11px] font-semibold transition-colors ${
                      pol.action === 'AUDIT' ? 'bg-indigo-500/20 text-indigo-300' : 'text-zinc-500 hover:text-zinc-300'
                    }`}
                  >
                    Audit
                  </button>
                </div>
              </div>
            </div>
          </div>
        ))}
      </div>

      {/* Add Policy Modal */}
      {showAddModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm animate-in fade-in">
          <div className="sg-card max-w-lg w-full p-6 border-white/10 shadow-2xl">
            <h2 className="text-lg font-bold text-white mb-1">Create Security Policy</h2>
            <p className="text-xs text-zinc-400 mb-4">Set up a customized gatekeeper rule across your scanning engines.</p>

            <form onSubmit={handleAddPolicy} className="space-y-4">
              <div>
                <label className="block text-xs font-medium text-zinc-300 mb-1">Policy Title</label>
                <input
                  type="text"
                  required
                  placeholder="e.g. Reject High Severity NPM Packages"
                  className="w-full bg-[#121214] border border-white/10 rounded-lg px-3 py-2 text-sm text-white placeholder:text-zinc-600 outline-none focus:border-indigo-500"
                  value={newPolicy.name}
                  onChange={e => setNewPolicy({ ...newPolicy, name: e.target.value })}
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-medium text-zinc-300 mb-1">Category</label>
                  <select
                    className="w-full bg-[#121214] border border-white/10 rounded-lg px-3 py-2 text-xs text-white outline-none focus:border-indigo-500"
                    value={newPolicy.category}
                    onChange={e => setNewPolicy({ ...newPolicy, category: e.target.value })}
                  >
                    <option value="Code Security">Code Security</option>
                    <option value="Secret Detection">Secret Detection</option>
                    <option value="Software Supply Chain">Software Supply Chain</option>
                    <option value="Container Security">Container Security</option>
                    <option value="Compliance">Compliance</option>
                  </select>
                </div>

                <div>
                  <label className="block text-xs font-medium text-zinc-300 mb-1">Target Scanner</label>
                  <select
                    className="w-full bg-[#121214] border border-white/10 rounded-lg px-3 py-2 text-xs text-white outline-none focus:border-indigo-500"
                    value={newPolicy.scanner}
                    onChange={e => setNewPolicy({ ...newPolicy, scanner: e.target.value })}
                  >
                    <option value="Semgrep">Semgrep (SAST)</option>
                    <option value="Gitleaks">Gitleaks (Secrets)</option>
                    <option value="Trivy">Trivy (SCA & Container)</option>
                    <option value="All Scanners">All Scanners</option>
                  </select>
                </div>
              </div>

              <div>
                <label className="block text-xs font-medium text-zinc-300 mb-1">Description</label>
                <textarea
                  rows={2}
                  placeholder="Describe what triggers this policy and what remediation is required..."
                  className="w-full bg-[#121214] border border-white/10 rounded-lg px-3 py-2 text-sm text-white placeholder:text-zinc-600 outline-none focus:border-indigo-500"
                  value={newPolicy.description}
                  onChange={e => setNewPolicy({ ...newPolicy, description: e.target.value })}
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-medium text-zinc-300 mb-1">Severity Trigger</label>
                  <select
                    className="w-full bg-[#121214] border border-white/10 rounded-lg px-3 py-2 text-xs text-white outline-none focus:border-indigo-500"
                    value={newPolicy.severityThreshold}
                    onChange={e => setNewPolicy({ ...newPolicy, severityThreshold: e.target.value as any })}
                  >
                    <option value="CRITICAL">Critical</option>
                    <option value="HIGH">High</option>
                    <option value="MEDIUM">Medium</option>
                    <option value="LOW">Low</option>
                  </select>
                </div>

                <div>
                  <label className="block text-xs font-medium text-zinc-300 mb-1">Enforcement Action</label>
                  <select
                    className="w-full bg-[#121214] border border-white/10 rounded-lg px-3 py-2 text-xs text-white outline-none focus:border-indigo-500"
                    value={newPolicy.action}
                    onChange={e => setNewPolicy({ ...newPolicy, action: e.target.value as any })}
                  >
                    <option value="BLOCK">Block Pipeline</option>
                    <option value="WARN">Warning Only</option>
                    <option value="AUDIT">Audit / Log</option>
                  </select>
                </div>
              </div>

              <div className="flex justify-end gap-2 pt-3">
                <button
                  type="button"
                  onClick={() => setShowAddModal(false)}
                  className="px-4 py-2 rounded-lg text-xs font-medium text-zinc-400 hover:text-white bg-white/5 hover:bg-white/10 transition-colors"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="btn btn-primary text-xs"
                >
                  Save Policy
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
